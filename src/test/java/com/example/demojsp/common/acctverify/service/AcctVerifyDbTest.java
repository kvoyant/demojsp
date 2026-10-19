package com.example.demojsp.common.acctverify.service;

import com.example.demojsp.common.acctverify.config.AcctVerifyProperties;
import com.example.demojsp.common.acctverify.config.AcctVerifySeeder;
import com.example.demojsp.common.acctverify.controller.AcctVerifyApiController;
import com.example.demojsp.common.acctverify.dto.AvBizDto;
import com.example.demojsp.common.acctverify.dto.AvContextView;
import com.example.demojsp.common.acctverify.dto.AvReqDto;
import com.example.demojsp.common.acctverify.dto.AvRequest;
import com.example.demojsp.common.acctverify.dto.AvResult;
import com.example.demojsp.common.acctverify.dto.AvStartResult;
import com.example.demojsp.common.acctverify.exception.AcctVerifyException;
import com.example.demojsp.common.acctverify.exception.AcctVerifyExceptionHandler;
import com.example.demojsp.common.acctverify.gateway.MockFirmBankingGateway;
import com.example.demojsp.common.acctverify.mapper.AcctVerifyMapper;
import com.example.demojsp.common.consent.service.ConsentUser;
import com.example.demojsp.common.store.CommonStoreInitializer;
import com.example.demojsp.common.store.CommonStoreProperties;
import com.jayway.jsonpath.JsonPath;
import org.apache.ibatis.session.SqlSessionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [공통#25] 1원 계좌인증 - 실제 schema/data SQL + AcctVerifyMapper.xml 을 H2(MySQL 모드)에서 실행.
 * 펌뱅킹은 시뮬레이터(MockFirmBankingGateway: …0000 없는 계좌 / …9999 김타인 / …8888 송금 실패). 기준 시각 2026-10-20 10:00.
 */
class AcctVerifyDbTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 20, 10, 0);
    /** 대출 2건 보유 회원 (LN202610130001 KB 123402-04-567890, LN202610130004 카카오 3333-01-9876543) */
    private static final AcctCustomer HONG = new AcctCustomer("customer01", "홍길동", "a".repeat(64), "10.0.0.1");
    /** 비로그인 고객 (공통#21 본인인증으로 실명 확인된 경우) */
    private static final AcctCustomer GUEST = new AcctCustomer(null, "김신규", "b".repeat(64), "10.0.0.2");

    private JdbcTemplate jdbc;
    private AcctVerifyServiceImpl service;
    private AcctVerifyAdminServiceImpl admin;
    private AcctVerifyProperties props;

    @BeforeEach
    void setUp() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:acctverify" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        jdbc = new JdbcTemplate(ds);

        SqlSessionFactoryBean f = new SqlSessionFactoryBean();
        f.setDataSource(ds);
        f.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/common/*.xml"));
        org.apache.ibatis.session.Configuration cfg = new org.apache.ibatis.session.Configuration();
        cfg.setMapUnderscoreToCamelCase(true);
        f.setConfiguration(cfg);
        SqlSessionFactory factory = f.getObject();
        AcctVerifyMapper mapper = new SqlSessionTemplate(factory).getMapper(AcctVerifyMapper.class);

        List<PropertySource<?>> yaml = new YamlPropertySourceLoader().load("app", new ClassPathResource("application.yaml"));
        props = new Binder(ConfigurationPropertySources.from(yaml)).bind("common.acct-verify", AcctVerifyProperties.class).get();
        assertThat(props.getMaxRequestsPer10Min()).isEqualTo(5);        // yaml 키 바인딩 확인
        assertThat(props.isExposeTestCode()).isTrue();

        service = new AcctVerifyServiceImpl(mapper, new MockFirmBankingGateway(), props);
        admin = new AcctVerifyAdminServiceImpl(mapper);
        at(NOW);

        CommonStoreProperties sp = new CommonStoreProperties();
        sp.setInitSchema(true);
        sp.setSeedIfEmpty(true);
        assertThat(new CommonStoreInitializer(ds, sp, List.of(new AcctVerifySeeder(ds, mapper))).initialize()).isTrue();
    }

    private void at(LocalDateTime t) {
        Clock c = Clock.fixed(t.atZone(KST).toInstant(), KST);
        service.setClock(c);
        admin.setClock(c);
    }

    private static AvRequest prefill(String biz, String loanNo) {
        AvRequest r = new AvRequest();
        r.setBizCd(biz);
        r.setChannel("APP");
        r.setPrefillId(loanNo);
        return r;
    }

    private static AvRequest input(String biz, String bankCd, String acctNo, String modifiedYn) {
        AvRequest r = new AvRequest();
        r.setBizCd(biz);
        r.setChannel("APP");
        r.setBankCd(bankCd);
        r.setAcctNo(acctNo);
        r.setModifiedYn(modifiedYn);
        return r;
    }

    private static ConsentUser user(AcctCustomer c) {
        return new ConsentUser(c.custId(), c.sessKey(), c.clientIp());
    }

    private static String wrong(String code) {
        return code.equals("0000") ? "1111" : "0000";
    }

    private static void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String code) {
        assertThatThrownBy(call).isInstanceOfSatisfying(AcctVerifyException.class,
                e -> assertThat(e.getErrorCode().getCode()).isEqualTo(code));
    }

    private String row(String reqNo, String col) {
        return jdbc.queryForObject("SELECT " + col + " FROM TB_CO_ACCT_VERIFY WHERE REQ_NO = ?", String.class, reqNo);
    }

    // ================================================================

    @Test
    @DisplayName("기동 초기화: 업무 Context 4건 · 은행 21곳 · 대출계좌 4건 (비어 있을 때만), 재실행해도 중복 없음")
    void seed() throws Exception {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM TB_CO_ACCT_BIZ", Integer.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM TB_BANK_CODE", Integer.class)).isEqualTo(21);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM TB_LOAN_ACCOUNT", Integer.class)).isEqualTo(4);
        assertThat(admin.bizList()).extracting(AvBizDto::getScreenType).containsOnly("LOAN", "COMMON");
    }

    @Test
    @DisplayName("화면 분리: LOAN 은 대출 입출금계좌 자동완성(마스킹), COMMON · 비회원은 자동완성 없음")
    void contextSplit() {
        AvContextView loan = service.context("LOAN_DISBURSE", HONG);
        assertThat(loan.getScreenType()).isEqualTo("LOAN");
        assertThat(loan.getBanks()).hasSize(21);
        assertThat(loan.getPrefills()).hasSize(2);
        assertThat(loan.getPrefills().get(0).getAcctMask()).doesNotContain("4567").contains("*");   // 전체 번호를 화면에 보내지 않는다
        assertThat(loan.getPrefills()).extracting(p -> p.getBankNm()).contains("KB국민은행", "카카오뱅크");

        AvContextView common = service.context("AUTH_MEANS", HONG);
        assertThat(common.getScreenType()).isEqualTo("COMMON");
        assertThat(common.getPrefills()).isEmpty();
        assertThat(service.context("LOAN_DISBURSE", GUEST).getPrefills()).isEmpty();
        assertCode(() -> service.context("NO_SUCH", HONG), "AVF-002");
    }

    @Test
    @DisplayName("자동완성 계좌 → 1원 송금 → 입금자명 숫자 확인 → 업무 서버가 전체 계좌번호 수신")
    void prefillFlow() {
        AvStartResult s = service.request(prefill("LOAN_DISBURSE", "LN202610130001"), HONG);
        assertThat(s.getMemoPrefix()).isEqualTo("페퍼");
        assertThat(s.getTestCode()).matches("\\d{4}");
        assertThat(s.getHolderNm()).isEqualTo("홍길동");
        assertThat(row(s.getReqNo(), "CODE_HASH")).doesNotContain(s.getTestCode()).hasSize(64);     // 원문 저장 안 함

        AvResult r = service.verify(s.getReqNo(), s.getTestCode(), HONG);
        assertThat(r.getStatus()).isEqualTo(AvReqDto.VERIFIED);
        assertThat(r.getSource()).isEqualTo("PREFILL");
        assertThat(r.getAcctNo()).isNull();                                                        // 화면 응답에는 없음

        AvResult server = service.checkVerified(s.getReqNo(), "LOAN_DISBURSE", user(HONG));
        assertThat(server.getAcctNo()).isEqualTo("12340204567890");
        assertCode(() -> service.checkVerified(s.getReqNo(), "LOAN_REPAY", user(HONG)), "AVF-003");    // 다른 업무로 재사용 불가
        assertCode(() -> service.verify(s.getReqNo(), s.getTestCode(), HONG), "AVF-004");               // 두 번 완료 불가
    }

    @Test
    @DisplayName("[수정] 직접 입력은 MODIFIED_YN=Y, 남의 대출계좌 · 잘못된 계좌번호는 거절")
    void modify() {
        AvStartResult s = service.request(input("LOAN_DISBURSE", "088", "110-123-456789", "Y"), HONG);
        assertThat(row(s.getReqNo(), "SOURCE")).isEqualTo("INPUT");
        assertThat(row(s.getReqNo(), "MODIFIED_YN")).isEqualTo("Y");
        assertThat(row(s.getReqNo(), "BANK_NM")).isEqualTo("신한은행");

        assertCode(() -> service.request(prefill("LOAN_DISBURSE", "LN202610130002"), HONG), "AVF-103");   // 김상담의 대출
        assertCode(() -> service.request(prefill("AUTH_MEANS", "LN202610130001"), HONG), "AVF-103");      // COMMON 화면은 자동완성 불가
        assertCode(() -> service.request(input("AUTH_MEANS", "004", "12345", "N"), HONG), "AVF-102");
        assertCode(() -> service.request(input("AUTH_MEANS", "999", "1234567890", "N"), HONG), "AVF-102");
    }

    @Test
    @DisplayName("없는 계좌 · 타인 명의 · 송금 실패는 예외 + FAILED 이력")
    void failures() {
        assertCode(() -> service.request(input("AUTH_MEANS", "004", "1234567890000", "N"), GUEST), "AVF-104");
        assertCode(() -> service.request(input("AUTH_MEANS", "004", "1234567899999", "N"), GUEST), "AVF-105");
        assertCode(() -> service.request(input("AUTH_MEANS", "004", "1234567898888", "N"), GUEST), "AVF-201");
        assertThat(jdbc.queryForList("SELECT RESULT_CD FROM TB_CO_ACCT_VERIFY WHERE STATUS = 'FAILED' ORDER BY REQ_AT", String.class))
                .containsExactlyInAnyOrder("NO_ACCOUNT", "NOT_OWNER", "TRANSFER_FAIL");
    }

    @Test
    @DisplayName("오입력 횟수 (업무 Context 별: 인증수단 등록 3회) → FAILED TRY_EXCEEDED")
    void tries() {
        AvStartResult s = service.request(input("AUTH_MEANS", "090", "3333019876543", "N"), GUEST);
        assertThat(s.getMaxTry()).isEqualTo(3);
        String bad = wrong(s.getTestCode());
        assertCode(() -> service.verify(s.getReqNo(), bad, GUEST), "AVF-007");
        assertCode(() -> service.verify(s.getReqNo(), bad, GUEST), "AVF-007");
        assertCode(() -> service.verify(s.getReqNo(), bad, GUEST), "AVF-008");
        assertThat(row(s.getReqNo(), "STATUS")).isEqualTo("FAILED");
        assertThat(row(s.getReqNo(), "TRY_CNT")).isEqualTo("3");                                    // 예외에도 횟수는 저장됨
        assertCode(() -> service.verify(s.getReqNo(), s.getTestCode(), GUEST), "AVF-004");
        assertCode(() -> service.verify(s.getReqNo(), "12", GUEST), "AVF-004");
    }

    @Test
    @DisplayName("유효시간 · 결과 사용시간 · 남의 요청 · 과다 요청")
    void timeAndOwnership() {
        AvStartResult s1 = service.request(input("REFUND_ACCT", "004", "1234567890", "N"), GUEST);
        assertCode(() -> service.verify(s1.getReqNo(), s1.getTestCode(), HONG), "AVF-003");          // 다른 사람
        at(NOW.plusMinutes(11));
        assertCode(() -> service.verify(s1.getReqNo(), s1.getTestCode(), GUEST), "AVF-006");
        assertThat(row(s1.getReqNo(), "STATUS")).isEqualTo("EXPIRED");

        at(NOW.plusMinutes(12));
        AvStartResult s2 = service.request(input("AUTH_MEANS", "004", "1234567890", "N"), GUEST);
        service.verify(s2.getReqNo(), s2.getTestCode(), GUEST);
        assertThat(service.checkVerified(s2.getReqNo(), "AUTH_MEANS", user(GUEST)).getAcctNo()).isEqualTo("1234567890");
        at(NOW.plusMinutes(12 + 11));                                                              // AUTH_MEANS 결과 사용 10분
        assertCode(() -> service.checkVerified(s2.getReqNo(), "AUTH_MEANS", user(GUEST)), "AVF-302");

        for (int i = 0; i < 5; i++) service.request(input("AUTH_MEANS", "004", "1234567890", "N"), HONG);
        assertCode(() -> service.request(input("AUTH_MEANS", "004", "1234567890", "N"), HONG), "AVF-005");
    }

    @Test
    @DisplayName("어드민: 업무 Context 저장 검증 · 이력은 전체 계좌번호/해시 없이")
    void adminSave() {
        AvBizDto b = new AvBizDto();
        b.setBizCd("AUTO_TRANSFER");
        b.setBizNm("자동이체 출금계좌");
        b.setScreenType("COMMON");
        b.setPrefillYn("Y");
        b.setOwnerCheckYn("Y");
        b.setMemoPrefix("페퍼");
        b.setCodeValidMin(10);
        b.setMaxTry(5);
        b.setResultValidMin(30);
        assertCode(() -> admin.saveBiz(b, "admin01"), "AVF-401");          // COMMON + 자동완성
        b.setPrefillYn("N");
        b.setMemoPrefix("페퍼1");
        assertCode(() -> admin.saveBiz(b, "admin01"), "AVF-401");          // 접두어에 숫자 불가 (인증 숫자와 혼동)
        b.setMemoPrefix("페퍼");
        AvBizDto saved = admin.saveBiz(b, "admin01");
        assertThat(saved.getUpdBy()).isEqualTo("admin01");
        assertThat(service.context("AUTO_TRANSFER", GUEST).getScreenType()).isEqualTo("COMMON");    // 저장 즉시 적용

        service.request(prefill("LOAN_REPAY", "LN202610130004"), HONG);
        List<AvReqDto> list = admin.requests("LOAN_REPAY", null, 10);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getAcctNo()).isNull();
        assertThat(list.get(0).getCodeHash()).isNull();
        assertThat(list.get(0).getAcctMask()).contains("*");
    }

    @Test
    @DisplayName("API: context → request → verify (local demo 고객), 고객 미확인은 401 AVF-101")
    void api() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AcctVerifyApiController(service, new AcctCustomerResolver(null, props)))
                .setControllerAdvice(new AcctVerifyExceptionHandler()).build();
        MockHttpSession session = new MockHttpSession();
        String q = "?demoCustomer=customer01&demoName=홍길동";

        mvc.perform(get("/api/common/acct-verify/context/LOAN_DISBURSE"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("AVF-101"));
        mvc.perform(get("/api/common/acct-verify/context/LOAN_DISBURSE" + q).session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.screenType").value("LOAN"))
                .andExpect(jsonPath("$.data.prefills.length()").value(2));

        String res = mvc.perform(post("/api/common/acct-verify/request" + q).session(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bizCd\":\"LOAN_DISBURSE\",\"channel\":\"APP\",\"prefillId\":\"LN202610130004\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        String reqNo = JsonPath.read(res, "$.data.reqNo");
        String code = JsonPath.read(res, "$.data.testCode");
        mvc.perform(post("/api/common/acct-verify/verify" + q).session(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reqNo\":\"" + reqNo + "\",\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("VERIFIED"))
                .andExpect(jsonPath("$.data.bankNm").value("카카오뱅크"))
                .andExpect(jsonPath("$.data.acctNo").doesNotExist());
    }
}
