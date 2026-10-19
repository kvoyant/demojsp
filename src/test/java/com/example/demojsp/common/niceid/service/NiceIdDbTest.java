package com.example.demojsp.common.niceid.service;

import com.example.demojsp.common.consent.config.ConsentProperties;
import com.example.demojsp.common.consent.config.ConsentSeeder;
import com.example.demojsp.common.consent.dto.ConsentAgreeItemDto;
import com.example.demojsp.common.consent.dto.ConsentAgreeRequest;
import com.example.demojsp.common.consent.dto.ConsentListDto;
import com.example.demojsp.common.consent.mapper.ConsentMapper;
import com.example.demojsp.common.consent.service.ConsentServiceImpl;
import com.example.demojsp.common.consent.service.ConsentUser;
import com.example.demojsp.common.niceid.config.NiceIdProperties;
import com.example.demojsp.common.niceid.config.NiceIdSeeder;
import com.example.demojsp.common.niceid.controller.NiceIdApiController;
import com.example.demojsp.common.niceid.dto.IdvPurposeDto;
import com.example.demojsp.common.niceid.dto.IdvReqDto;
import com.example.demojsp.common.niceid.dto.IdvResult;
import com.example.demojsp.common.niceid.dto.IdvStartRequest;
import com.example.demojsp.common.niceid.dto.IdvStartResult;
import com.example.demojsp.common.niceid.exception.NiceIdException;
import com.example.demojsp.common.niceid.exception.NiceIdExceptionHandler;
import com.example.demojsp.common.niceid.gateway.MockNiceIdGateway;
import com.example.demojsp.common.niceid.mapper.NiceIdMapper;
import com.example.demojsp.common.store.CommonStoreInitializer;
import com.example.demojsp.common.store.CommonStoreProperties;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [공통#21] 휴대폰 본인인증 NICE ID - 실제 schema/data SQL + NiceIdMapper.xml + 공통#22 약관동의를 H2(MySQL 모드)에서 실행.
 * NICE 는 시뮬레이터(MockNiceIdGateway). 기준 시각 2026-10-20 10:00.
 */
class NiceIdDbTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 20, 10, 0);
    private static final ConsentUser A = new ConsentUser(null, "a".repeat(64), "10.0.0.1");
    private static final ConsentUser B = new ConsentUser(null, "b".repeat(64), "10.0.0.2");

    private JdbcTemplate jdbc;
    private ConsentServiceImpl consent;
    private NiceIdServiceImpl service;
    private NiceIdAdminServiceImpl admin;
    private NiceIdProperties props;
    private MockNiceIdGateway gateway;

    @BeforeEach
    void setUp() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:niceid" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        jdbc = new JdbcTemplate(ds);

        SqlSessionFactoryBean f = new SqlSessionFactoryBean();
        f.setDataSource(ds);
        f.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/common/*.xml"));
        org.apache.ibatis.session.Configuration cfg = new org.apache.ibatis.session.Configuration();
        cfg.setMapUnderscoreToCamelCase(true);
        f.setConfiguration(cfg);
        SqlSessionFactory factory = f.getObject();
        SqlSessionTemplate session = new SqlSessionTemplate(factory);

        List<PropertySource<?>> yaml = new YamlPropertySourceLoader().load("app", new ClassPathResource("application.yaml"));
        Binder binder = new Binder(ConfigurationPropertySources.from(yaml));
        props = binder.bind("common.niceid", NiceIdProperties.class).get();
        assertThat(props.getMaxRequestsPer10Min()).isEqualTo(10);       // yaml 키 바인딩 확인
        assertThat(props.isExposeTestCode()).isTrue();

        ConsentMapper consentMapper = session.getMapper(ConsentMapper.class);
        ConsentProperties cp = new ConsentProperties();
        consent = new ConsentServiceImpl(consentMapper, cp);
        NiceIdMapper mapper = session.getMapper(NiceIdMapper.class);
        gateway = new MockNiceIdGateway();
        service = new NiceIdServiceImpl(mapper, gateway, consent, props);
        admin = new NiceIdAdminServiceImpl(mapper, consentMapper);
        at(NOW);

        CommonStoreProperties sp = new CommonStoreProperties();
        sp.setInitSchema(true);
        sp.setSeedIfEmpty(true);
        assertThat(new CommonStoreInitializer(ds, sp, List.of(new ConsentSeeder(ds), new NiceIdSeeder(ds, mapper))).initialize()).isTrue();
    }

    private void at(LocalDateTime t) {
        Clock c = Clock.fixed(t.atZone(KST).toInstant(), KST);
        service.setClock(c);
        admin.setClock(c);
    }

    /** 화면처럼: 본인인증 약관(IDENTITY_NICE) 필수 전부 동의 → agreeNo */
    private long agree(ConsentUser u) {
        ConsentListDto l = consent.getTerms("IDENTITY_NICE");
        ConsentAgreeRequest req = new ConsentAgreeRequest();
        req.setChannel("APP");
        List<ConsentAgreeItemDto> items = new ArrayList<>();
        l.getRequired().forEach(t -> items.add(new ConsentAgreeItemDto(t.getTermsCd(), t.getTermsVer(), "Y")));
        req.setItems(items);
        return consent.agree("IDENTITY_NICE", req, u).getAgreeNo();
    }

    private static IdvStartRequest req(String purpose, Long agreeNo, String name, String birth, String phone) {
        IdvStartRequest r = new IdvStartRequest();
        r.setPurposeCd(purpose);
        r.setAgreeNo(agreeNo);
        r.setChannel("APP");
        r.setName(name);
        r.setBirth(birth);
        r.setPhone(phone);
        return r;
    }

    /** SMS 한 번에 끝까지 (시뮬레이터 인증번호 사용) */
    private IdvResult sms(String purpose, String name, String birth, String phone, ConsentUser u) {
        Long agreeNo = "PW_RESET".equals(purpose) ? null : agree(u);
        IdvStartResult s = service.startSms(req(purpose, agreeNo, name, birth, phone), u);
        return service.verifySms(s.getReqNo(), s.getTestCode(), u);
    }

    private static void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String code) {
        assertThatThrownBy(call).isInstanceOfSatisfying(NiceIdException.class,
                e -> assertThat(e.getErrorCode().getCode()).isEqualTo(code));
    }

    // ================================================================

    @Test
    @DisplayName("기동 초기화: 업무 정책 4건 + 유의고객 샘플 2명(없을 때만), 재실행해도 중복 없음")
    void seed() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM TB_CO_IDV_PURPOSE", Integer.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM TB_WATCHLIST", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT LENGTH(CI_VAL) FROM TB_WATCHLIST WHERE CUST_NM='김유의'", Integer.class)).isEqualTo(88);
        IdvPurposeDto p = service.purpose("SIGN_UP");
        assertThat(p.getConsentBizCd()).isEqualTo("IDENTITY_NICE");
        assertThat(p.getMinAge()).isNull();                                       // 검사 기준은 화면에 안 보낸다
    }

    @Test
    @DisplayName("SMS 통신사 미지정: 약관 → 인증번호 (오입력 1회 반영) → 성공 → 업무 서버 checkVerified (CI 88자)")
    void smsHappyPath() {
        long agreeNo = agree(A);
        IdvStartResult s = service.startSms(req("SIGN_UP", agreeNo, "홍길동", "19900101", "010-1234-5678"), A);
        assertThat(s.getPhoneMask()).isEqualTo("010-****-5678");
        assertThat(s.getTestCode()).hasSize(6);
        assertThat(s.getValidSec()).isEqualTo(180);

        String wrong = s.getTestCode().equals("000000") ? "111111" : "000000";
        assertCode(() -> service.verifySms(s.getReqNo(), wrong, A), "NID-007");
        assertThat(jdbc.queryForObject("SELECT TRY_CNT FROM TB_CO_IDV_REQ WHERE REQ_NO=?", Integer.class, s.getReqNo())).isEqualTo(1);
        assertCode(() -> service.verifySms(s.getReqNo(), s.getTestCode(), B), "NID-003");     // 남의 세션

        IdvResult r = service.verifySms(s.getReqNo(), s.getTestCode(), A);
        assertThat(r.getStatus()).isEqualTo("VERIFIED");
        assertThat(r.getAge()).isEqualTo(36);
        assertThat(r.getCellNoMask()).isEqualTo("010-****-5678");
        assertThat(r.getCi()).isNull();                                            // 화면 결과에는 CI 없음
        assertCode(() -> service.verifySms(s.getReqNo(), s.getTestCode(), A), "NID-004");     // 두 번 완료 불가

        IdvResult biz = service.checkVerified(s.getReqNo(), "SIGN_UP", A);
        assertThat(biz.getCi()).hasSize(88).isEqualTo(MockNiceIdGateway.identity("홍길동", "19900101", "01012345678").ci());
        assertThat(biz.getCellNo()).isEqualTo("01012345678");
        assertCode(() -> service.checkVerified(s.getReqNo(), "LOAN_APPLY", A), "NID-003");    // 다른 업무
        assertCode(() -> service.checkVerified(s.getReqNo(), "SIGN_UP", B), "NID-003");
        at(NOW.plusMinutes(31));
        assertCode(() -> service.checkVerified(s.getReqNo(), "SIGN_UP", A), "NID-302");       // 결과 사용시간 30분
    }

    @Test
    @DisplayName("약관 미동의 NID-101 / 입력값 NID-102 / 인증방식 NID-009 / 과다요청 NID-005")
    void guards() {
        assertCode(() -> service.startSms(req("SIGN_UP", null, "홍길동", "19900101", "01012345678"), A), "NID-101");
        long agreeNo = agree(A);
        assertCode(() -> service.startSms(req("SIGN_UP", agreeNo, "홍길동", "19900101", "01012345678"), B), "NID-101");  // 남의 약관동의
        assertCode(() -> service.startSms(req("SIGN_UP", agreeNo, "홍길동", "19901301", "01012345678"), A), "NID-102");
        assertCode(() -> service.startSms(req("SIGN_UP", agreeNo, "홍길동", "19900101", "0212345678"), A), "NID-102");
        assertCode(() -> service.startSms(req("SIGN_UP", agreeNo, "1", "19900101", "01012345678"), A), "NID-102");
        assertCode(() -> service.startMo(req("PW_RESET", null, "홍길동", "19900101", null), A), "NID-009");          // MO 미사용 업무
        assertCode(() -> service.startSms(req("NOPE", null, "홍길동", "19900101", "01012345678"), A), "NID-002");

        props.setMaxRequestsPer10Min(2);
        service.startSms(req("SIGN_UP", agreeNo, "홍길동", "19900101", "01012345678"), A);
        service.startSms(req("SIGN_UP", agreeNo, "홍길동", "19900101", "01012345678"), A);
        assertCode(() -> service.startSms(req("SIGN_UP", agreeNo, "홍길동", "19900101", "01012345678"), A), "NID-005");
    }

    @Test
    @DisplayName("오입력 최대 횟수(비밀번호 재설정 3회) → FAILED / 유효시간 지나면 EXPIRED")
    void tryAndExpire() {
        IdvStartResult s = service.startSms(req("PW_RESET", null, "홍길동", "19900101", "01012345678"), A);   // 약관 없는 업무
        String wrong = s.getTestCode().equals("000000") ? "111111" : "000000";
        assertCode(() -> service.verifySms(s.getReqNo(), wrong, A), "NID-007");
        assertCode(() -> service.verifySms(s.getReqNo(), wrong, A), "NID-007");
        assertCode(() -> service.verifySms(s.getReqNo(), wrong, A), "NID-008");
        assertThat(jdbc.queryForObject("SELECT STATUS || '/' || RESULT_CD FROM TB_CO_IDV_REQ WHERE REQ_NO=?", String.class, s.getReqNo()))
                .isEqualTo("FAILED/TRY_EXCEEDED");
        assertCode(() -> service.verifySms(s.getReqNo(), s.getTestCode(), A), "NID-004");

        IdvStartResult s2 = service.startSms(req("PW_RESET", null, "홍길동", "19900101", "01012345678"), A);
        at(NOW.plusSeconds(180));
        assertCode(() -> service.verifySms(s2.getReqNo(), s2.getTestCode(), A), "NID-006");
        assertThat(jdbc.queryForObject("SELECT STATUS FROM TB_CO_IDV_REQ WHERE REQ_NO=?", String.class, s2.getReqNo())).isEqualTo("EXPIRED");
    }

    @Test
    @DisplayName("주민번호 → CI 이후 체크: 유의고객(CI) · 미성년자(만 나이) · 외국인 - 업무 정책별로 다르게")
    void policyChecks() {
        IdvResult minor = sms("LOAN_APPLY", "김미성", "20100505", "01022223333", A);
        assertThat(minor.getStatus() + "/" + minor.getResultCd()).isEqualTo("BLOCKED/MINOR");
        assertThat(minor.getMessage()).contains("미성년자");
        assertThat(sms("SIGN_UP", "김미성", "20100505", "01022223333", A).getStatus()).isEqualTo("VERIFIED");   // 회원가입은 만 14세

        IdvResult foreigner = sms("LOAN_APPLY", "John Smith", "19850101", "01033334444", A);
        assertThat(foreigner.getResultCd()).isEqualTo("FOREIGNER");
        assertThat(foreigner.getNationalCd()).isEqualTo("F");
        assertThat(sms("SIGN_UP", "John Smith", "19850101", "01033334444", A).getStatus()).isEqualTo("VERIFIED");

        IdvResult watch = sms("SIGN_UP", "김유의", "19800101", "01099990000", A);
        assertThat(watch.getStatus() + "/" + watch.getResultCd()).isEqualTo("BLOCKED/WATCHLIST");
        assertThat(watch.getMessage()).doesNotContain("유의고객");                    // 차단 사유를 고객에게 그대로 노출하지 않음
        assertThat(sms("NON_CREDIT", "김유의", "19800101", "01099990000", A).getStatus()).isEqualTo("VERIFIED");   // 유의고객 검사 N

        jdbc.update("UPDATE TB_WATCHLIST SET BLOCK_YN='N' WHERE CUST_NM='김유의'");
        assertThat(sms("SIGN_UP", "김유의", "19800101", "01099990000", A).getStatus()).isEqualTo("VERIFIED");     // 차단 해제

        IdvResult blocked = minor;
        assertCode(() -> service.checkVerified(blocked.getReqNo(), "LOAN_APPLY", A), "NID-301");
    }

    @Test
    @DisplayName("MO 기기인증: 휴대폰번호 입력 없이 시작 → 문자 대기 → (시뮬레이터) 문자 발송 → 인증 성공, 번호는 MO 로 확인")
    void moFlow() {
        long agreeNo = agree(A);
        IdvStartResult s = service.startMo(req("SIGN_UP", agreeNo, "홍길동", "19900101", null), A);
        assertThat(s.getMoNumber()).isEqualTo("1600-1522");
        assertThat(s.getMoCode()).startsWith("PEPPER ");
        assertThat(s.getSmsUri()).startsWith("sms:16001522?body=PEPPER%20");
        assertThat(jdbc.queryForObject("SELECT PHONE_MASK FROM TB_CO_IDV_REQ WHERE REQ_NO=?", String.class, s.getReqNo())).isNull();

        assertThat(service.moStatus(s.getReqNo(), A).getStatus()).isEqualTo("REQUESTED");          // 아직 문자 안 옴
        service.demoMoSend(s.getReqNo(), "01055556666", A);
        IdvResult r = service.moStatus(s.getReqNo(), A);
        assertThat(r.getStatus()).isEqualTo("VERIFIED");
        assertThat(r.getCellNoMask()).isEqualTo("010-****-6666");
        assertThat(service.moStatus(s.getReqNo(), A).getStatus()).isEqualTo("VERIFIED");          // 다시 물어도 같은 결과

        IdvStartResult s2 = service.startMo(req("SIGN_UP", agreeNo, "홍길동", "19900101", null), A);
        at(NOW.plusMinutes(4));
        assertThat(service.moStatus(s2.getReqNo(), A).getStatus()).isEqualTo("EXPIRED");

        props.setDemoApiOpen(false);
        assertCode(() -> service.demoMoSend(s2.getReqNo(), "01055556666", A), "NID-901");
    }

    @Test
    @DisplayName("ADMIN: 정책 검증·저장 즉시 적용, 이력은 CI·휴대폰 마스킹")
    void admin() {
        IdvPurposeDto p = new IdvPurposeDto();
        p.setPurposeCd("OPEN_BANKING");
        p.setPurposeNm("오픈뱅킹 등록");
        p.setConsentBizCd("NOPE");
        p.setSmsYn("Y");
        p.setMoYn("N");
        p.setMinAge(19);
        p.setForeignerYn("N");
        p.setWatchCheckYn("Y");
        p.setCodeValidSec(180);
        p.setMaxTry(5);
        p.setResultValidMin(10);
        assertCode(() -> admin.savePurpose(p, "admin01"), "NID-102");                 // 없는 약관 업무코드
        p.setConsentBizCd("IDENTITY_NICE");
        p.setSmsYn("N");
        assertCode(() -> admin.savePurpose(p, "admin01"), "NID-102");                 // SMS·MO 둘 다 N
        p.setSmsYn("Y");
        p.setCodeValidSec(30);
        assertCode(() -> admin.savePurpose(p, "admin01"), "NID-102");
        p.setCodeValidSec(120);
        assertThat(admin.savePurpose(p, "admin01").getUpdBy()).isEqualTo("admin01");
        assertThat(service.purpose("OPEN_BANKING").getSmsYn()).isEqualTo("Y");

        sms("SIGN_UP", "홍길동", "19900101", "01012345678", A);
        List<IdvReqDto> list = admin.requests("SIGN_UP", "VERIFIED", 10);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getCiVal()).hasSize(11).endsWith("…");
        assertThat(list.get(0).getCellNo()).isEqualTo("010-****-5678");
        assertThat(list.get(0).getSessKey()).isNull();
        assertThat(list.get(0).getAgreeNo()).isNotNull();                            // 약관동의 번호로 증적 연결
    }

    @Test
    @DisplayName("API: 시작 응답 testCode(local) / 결과 JSON 에 CI 없음 / 오류코드")
    void api() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new NiceIdApiController(service))
                .setControllerAdvice(new NiceIdExceptionHandler()).build();
        MockHttpSession session = new MockHttpSession();
        ConsentUser u = new ConsentUser(null, ConsentServiceImplHash.sha(session.getId()), "127.0.0.1");
        long agreeNo = agree(u);

        mvc.perform(get("/api/common/niceid/purposes/LOAN_APPLY")).andExpect(jsonPath("$.data.moYn").value("Y"))
                .andExpect(jsonPath("$.data.foreignerYn").doesNotExist());
        MvcResult res = mvc.perform(post("/api/common/niceid/sms/request").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"purposeCd\":\"SIGN_UP\",\"agreeNo\":" + agreeNo + ",\"channel\":\"APP\",\"name\":\"홍길동\",\"birth\":\"19900101\",\"phone\":\"01012345678\"}"))
                .andExpect(jsonPath("$.success").value(true)).andReturn();
        String json = res.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        String reqNo = json.replaceAll(".*\"reqNo\":\"([0-9a-f]{32})\".*", "$1");
        String code = json.replaceAll(".*\"testCode\":\"(\\d{6})\".*", "$1");
        mvc.perform(post("/api/common/niceid/sms/verify").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reqNo\":\"" + reqNo + "\",\"code\":\"" + code + "\"}"))
                .andExpect(jsonPath("$.data.status").value("VERIFIED"))
                .andExpect(jsonPath("$.data.ci").doesNotExist())
                .andExpect(jsonPath("$.data.cellNo").doesNotExist());
        mvc.perform(post("/api/common/niceid/sms/request").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"purposeCd\":\"SIGN_UP\",\"channel\":\"APP\",\"name\":\"홍길동\",\"birth\":\"19900101\",\"phone\":\"01012345678\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("NID-101"));
    }

    /** ConsentUser.from(request) 와 같은 세션키 계산 (테스트에서 약관동의를 같은 세션으로 만들기 위해) */
    static final class ConsentServiceImplHash {
        static String sha(String v) {
            try {
                byte[] d = java.security.MessageDigest.getInstance("SHA-256").digest(v.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                return java.util.HexFormat.of().formatHex(d);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
