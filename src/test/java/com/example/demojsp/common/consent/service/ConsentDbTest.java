package com.example.demojsp.common.consent.service;

import com.example.demojsp.common.consent.config.ConsentProperties;
import com.example.demojsp.common.consent.config.ConsentSeeder;
import com.example.demojsp.common.consent.controller.ConsentAdminApiController;
import com.example.demojsp.common.consent.controller.ConsentAdminAuth;
import com.example.demojsp.common.consent.controller.ConsentApiController;
import com.example.demojsp.common.consent.dto.ConsentAgreeDto;
import com.example.demojsp.common.consent.dto.ConsentAgreeItemDto;
import com.example.demojsp.common.consent.dto.ConsentAgreeRequest;
import com.example.demojsp.common.consent.dto.ConsentBizDto;
import com.example.demojsp.common.consent.dto.ConsentBizTermsRequest;
import com.example.demojsp.common.consent.dto.ConsentListDto;
import com.example.demojsp.common.consent.dto.ConsentResult;
import com.example.demojsp.common.consent.dto.ConsentTermsDto;
import com.example.demojsp.common.consent.dto.ConsentTermsSaveRequest;
import com.example.demojsp.common.consent.exception.ConsentException;
import com.example.demojsp.common.consent.exception.ConsentExceptionHandler;
import com.example.demojsp.common.consent.mapper.ConsentMapper;
import com.example.demojsp.common.store.CommonStoreInitializer;
import com.example.demojsp.common.store.CommonStoreProperties;
import org.apache.ibatis.session.SqlSessionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [공통#22] 약관동의 (DB 방식) - 실제 schema-common-consent.sql + data-common-consent.sql + ConsentMapper.xml 을
 * H2(MySQL 호환 모드)에서 실행한다. 기준 시각 2026-10-19 10:00 (Asia/Seoul).
 */
class ConsentDbTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Instant NOW = LocalDate.of(2026, 10, 19).atTime(10, 0).atZone(KST).toInstant();
    private static final ConsentUser GUEST_A = new ConsentUser(null, "a".repeat(64), "10.0.0.1");
    private static final ConsentUser GUEST_B = new ConsentUser(null, "b".repeat(64), "10.0.0.2");

    private JdbcTemplate jdbc;
    private ConsentMapper mapper;
    private ConsentServiceImpl service;
    private ConsentAdminServiceImpl admin;
    private ConsentProperties props;
    private CommonStoreInitializer initializer;

    @BeforeEach
    void setUp() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:consent" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        jdbc = new JdbcTemplate(ds);

        SqlSessionFactoryBean f = new SqlSessionFactoryBean();
        f.setDataSource(ds);
        f.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/common/*.xml"));
        org.apache.ibatis.session.Configuration cfg = new org.apache.ibatis.session.Configuration();
        cfg.setMapUnderscoreToCamelCase(true);
        f.setConfiguration(cfg);
        SqlSessionFactory factory = f.getObject();
        mapper = new SqlSessionTemplate(factory).getMapper(ConsentMapper.class);

        props = new ConsentProperties();
        props.setAdminApiOpen(true);
        service = new ConsentServiceImpl(mapper, props);
        service.setClock(Clock.fixed(NOW, KST));
        admin = new ConsentAdminServiceImpl(mapper);
        admin.setClock(Clock.fixed(NOW, KST));

        CommonStoreProperties sp = new CommonStoreProperties();
        sp.setInitSchema(true);
        sp.setSeedIfEmpty(true);
        initializer = new CommonStoreInitializer(ds, sp, List.of(new ConsentSeeder(ds)));
        assertThat(initializer.initialize()).isTrue();
    }

    private int count(String table) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return n == null ? 0 : n;
    }

    /** 화면이 하는 것처럼: 목록을 받아서 필수는 전부, 선택은 지정한 것만 동의 */
    private ConsentAgreeRequest allRequired(ConsentListDto list, String... optionalAgreed) {
        ConsentAgreeRequest req = new ConsentAgreeRequest();
        req.setChannel("APP");
        List<ConsentAgreeItemDto> items = new ArrayList<>();
        list.getRequired().forEach(t -> items.add(new ConsentAgreeItemDto(t.getTermsCd(), t.getTermsVer(), "Y")));
        list.getOptional().forEach(t -> items.add(new ConsentAgreeItemDto(t.getTermsCd(), t.getTermsVer(),
                List.of(optionalAgreed).contains(t.getTermsCd()) ? "Y" : "N")));
        req.setItems(items);
        return req;
    }

    private static List<String> codes(List<ConsentTermsDto> list) {
        return list.stream().map(t -> t.getTermsCd() + ":v" + t.getTermsVer()).toList();
    }

    // ================================================================ 조회

    @Test
    @DisplayName("기동 초기화: 테이블 5개 생성 + 샘플 적재, 다시 초기화해도 중복 없음")
    void schemaAndSeed() {
        assertThat(count("TB_CO_TERMS_BIZ")).isEqualTo(5);
        assertThat(count("TB_CO_TERMS")).isEqualTo(15);
        assertThat(count("TB_CO_BIZ_TERMS")).isEqualTo(19);
        assertThat(initializer.initialize()).isTrue();
        assertThat(count("TB_CO_TERMS_BIZ")).isEqualTo(5);
        assertThat(count("TB_CO_TERMS")).isEqualTo(15);
        String ko = jdbc.queryForObject("SELECT TERMS_NM FROM TB_CO_TERMS WHERE TERMS_CD='SVC_USE' AND TERMS_VER=1", String.class);
        assertThat(ko).isEqualTo("전자금융거래 기본약관");
    }

    @Test
    @DisplayName("업무코드 → 1페이지 필수 / 2페이지 선택, 표시순서대로, 시행일 기준 현재 버전")
    void listByBizCode() {
        ConsentListDto l = service.getTerms("SIGN_UP");
        assertThat(l.getBizNm()).isEqualTo("회원가입");
        // PRIVACY_COLLECT 는 2026-07-01 시행 v2, SVC_USE 는 2027-01-01 시행 예정 v2 → 아직 v1
        assertThat(codes(l.getRequired())).containsExactly("SVC_USE:v1", "PRIVACY_COLLECT:v2", "UNIQUE_ID:v1");
        assertThat(codes(l.getOptional())).containsExactly("MARKETING_USE:v1", "AD_RECEIVE:v1");
        assertThat(l.getRequired()).allMatch(t -> t.getTermsCn() == null);         // 목록에는 전문 없음

        ConsentListDto future = service.getTerms("SIGN_UP", LocalDate.of(2027, 1, 1));
        assertThat(codes(future.getRequired())).startsWith("SVC_USE:v2");

        assertThat(service.getTerms("IDENTITY_NICE").getOptional()).isEmpty();      // 선택 없음 → 2페이지 생략
        assertThat(service.getTermsDetail("PRIVACY_COLLECT", 2).getTermsCn()).contains("법령상 의무 이행");

        assertThatThrownBy(() -> service.getTerms("NO_SUCH")).hasMessageContaining("업무코드");
        assertThatThrownBy(() -> service.getTermsDetail("SVC_USE", 99)).isInstanceOf(ConsentException.class);
    }

    // ================================================================ 동의

    @Test
    @DisplayName("동의 저장: 헤더 1 + 약관별 상세(버전·해시·열람), 보내지 않은 선택약관은 N")
    void agreeSaved() {
        ConsentListDto l = service.getTerms("SIGN_UP");
        ConsentAgreeRequest req = allRequired(l, "MARKETING_USE");
        req.getItems().get(0).setViewYn("Y");
        req.getItems().removeIf(i -> i.getTermsCd().equals("AD_RECEIVE"));          // 선택약관 안 보냄

        ConsentResult r = service.agree("SIGN_UP", req, GUEST_A);
        assertThat(r.getAgreeNo()).isPositive();
        assertThat(r.getAgreedTerms()).containsExactly("SVC_USE", "PRIVACY_COLLECT", "UNIQUE_ID", "MARKETING_USE");
        assertThat(r.getDeclinedTerms()).containsExactly("AD_RECEIVE");
        assertThat(r.isAgreed("MARKETING_USE")).isTrue();

        ConsentAgreeDto d = admin.agreeDetail(r.getAgreeNo());
        assertThat(d.getItems()).hasSize(5);
        ConsentAgreeItemDto svc = d.getItems().stream().filter(i -> i.getTermsCd().equals("SVC_USE")).findFirst().orElseThrow();
        assertThat(svc.getViewYn()).isEqualTo("Y");
        assertThat(svc.getTermsVer()).isEqualTo(1);
        // 해시 = 그 버전 전문의 SHA-256 → 원장에서 재계산해 동일성 입증
        String content = service.getTermsDetail("SVC_USE", 1).getTermsCn();
        assertThat(svc.getCnHash()).hasSize(64).isEqualTo(ConsentServiceImpl.contentHashOf(content));
        assertThat(service.getTermsDetail("SVC_USE", 1).getCnHash()).isEqualTo(svc.getCnHash());   // 원장에도 채워짐
        assertThat(d.getSessKey()).isNull();
    }

    @Test
    @DisplayName("서버 재검증: 필수 미동의 CNS-101 / 개정 CNS-102 / 다른 업무 약관 CNS-103 / 채널 오류 CNS-004 - 저장 안 됨")
    void agreeValidation() {
        ConsentListDto l = service.getTerms("SIGN_UP");

        ConsentAgreeRequest noRequired = allRequired(l);
        noRequired.getItems().get(1).setAgreeYn("N");
        assertCode(() -> service.agree("SIGN_UP", noRequired, GUEST_A), "CNS-101");

        ConsentAgreeRequest oldVer = allRequired(l);
        oldVer.getItems().stream().filter(i -> i.getTermsCd().equals("PRIVACY_COLLECT")).findFirst().orElseThrow().setTermsVer(1);
        assertCode(() -> service.agree("SIGN_UP", oldVer, GUEST_A), "CNS-102");

        ConsentAgreeRequest other = allRequired(l);
        other.getItems().add(new ConsentAgreeItemDto("LOAN_BASIC", 1, "Y"));
        assertCode(() -> service.agree("SIGN_UP", other, GUEST_A), "CNS-103");

        ConsentAgreeRequest badChannel = allRequired(l);
        badChannel.setChannel("ATM");
        assertCode(() -> service.agree("SIGN_UP", badChannel, GUEST_A), "CNS-004");

        assertThat(count("TB_CO_TERMS_AGREE")).isZero();
        assertThat(count("TB_CO_TERMS_AGREE_DTL")).isZero();
    }

    @Test
    @DisplayName("업무 서버 checkAgreed: 본인(세션/회원) · 같은 업무 · 유효시간")
    void checkAgreed() {
        ConsentResult guest = service.agree("SIGN_UP", allRequired(service.getTerms("SIGN_UP")), GUEST_A);
        assertThat(service.checkAgreed(guest.getAgreeNo(), "SIGN_UP", GUEST_A).getAgreedTerms()).hasSize(3);
        assertCode(() -> service.checkAgreed(guest.getAgreeNo(), "SIGN_UP", GUEST_B), "CNS-201");       // 다른 사람
        assertCode(() -> service.checkAgreed(guest.getAgreeNo(), "LOAN_APPLY", GUEST_A), "CNS-201");    // 다른 업무
        assertCode(() -> service.checkAgreed(999_999, "SIGN_UP", GUEST_A), "CNS-201");                 // 없는 번호

        ConsentUser member = new ConsentUser("customer01", "c".repeat(64), "10.0.0.3");
        ConsentResult loan = service.agree("LOAN_APPLY", allRequired(service.getTerms("LOAN_APPLY")), member);
        ConsentUser sameMemberOtherDevice = new ConsentUser("customer01", "d".repeat(64), "10.0.0.4");
        assertThat(service.checkAgreed(loan.getAgreeNo(), "LOAN_APPLY", sameMemberOtherDevice).getBizCd()).isEqualTo("LOAN_APPLY");

        service.setClock(Clock.fixed(NOW.plusSeconds(31 * 60), KST));                                  // 30분 초과
        assertCode(() -> service.checkAgreed(loan.getAgreeNo(), "LOAN_APPLY", member), "CNS-202");
    }

    // ================================================================ ADMIN

    @Test
    @DisplayName("ADMIN 약관 개정: 다음 버전 추가, 시행일부터 사용자 화면 반영, 이전 동의내역은 이전 버전 그대로")
    void reviseTerms() {
        ConsentResult before = service.agree("SIGN_UP", allRequired(service.getTerms("SIGN_UP")), GUEST_A);

        ConsentTermsSaveRequest bad = new ConsentTermsSaveRequest();
        bad.setTermsCd("PRIVACY_COLLECT");
        bad.setTermsNm("개인(신용)정보 수집·이용 동의");
        bad.setTermsCn("개정 문구");
        bad.setEffectDt(LocalDate.of(2026, 6, 30));                                                    // v2 시행일보다 이전
        assertCode(() -> admin.saveTerms(bad, "admin01"), "CNS-302");

        bad.setEffectDt(LocalDate.of(2026, 10, 19));
        ConsentTermsDto v3 = admin.saveTerms(bad, "admin01");
        assertThat(v3.getTermsVer()).isEqualTo(3);
        assertThat(v3.getCnHash()).isEqualTo(ConsentServiceImpl.contentHashOf("개정 문구"));
        assertThat(codes(service.getTerms("SIGN_UP").getRequired())).contains("PRIVACY_COLLECT:v3");
        assertThat(admin.termsVersions("PRIVACY_COLLECT")).extracting(ConsentTermsDto::getTermsVer).containsExactly(3, 2, 1);

        // 옛 화면(v2)으로 보내면 개정 안내
        ConsentAgreeRequest stale = allRequired(service.getTerms("SIGN_UP"));
        stale.getItems().stream().filter(i -> i.getTermsCd().equals("PRIVACY_COLLECT")).findFirst().orElseThrow().setTermsVer(2);
        assertCode(() -> service.agree("SIGN_UP", stale, GUEST_A), "CNS-102");

        // 증적: 개정 전 동의는 v2 그대로
        assertThat(admin.agreeDetail(before.getAgreeNo()).getItems())
                .filteredOn(i -> i.getTermsCd().equals("PRIVACY_COLLECT")).extracting(ConsentAgreeItemDto::getTermsVer).containsExactly(2);

        ConsentTermsSaveRequest newTerms = new ConsentTermsSaveRequest();
        newTerms.setTermsCd("CARD_BASIC");
        newTerms.setTermsNm("카드 기본약관");
        newTerms.setTermsCn("카드 문구");
        newTerms.setPdfUrl("javascript:alert(1)");
        newTerms.setEffectDt(LocalDate.of(2026, 10, 1));
        assertCode(() -> admin.saveTerms(newTerms, "admin01"), "CNS-301");
        newTerms.setPdfUrl("/pdf/card.pdf");
        assertThat(admin.saveTerms(newTerms, "admin01").getTermsVer()).isEqualTo(1);
        newTerms.setTermsCd("card basic");
        assertCode(() -> admin.saveTerms(newTerms, "admin01"), "CNS-301");
    }

    @Test
    @DisplayName("ADMIN 업무별 약관 저장 → 사용자 목록 즉시 반영 / 업무코드 등록·미사용")
    void bizTermsMapping() {
        admin.saveBizTerms("SIGN_UP", List.of(
                new ConsentBizTermsRequest("SVC_USE", "Y", 1, "Y"),
                new ConsentBizTermsRequest("MARKETING_USE", "Y", 2, "Y"),     // 선택 → 필수로
                new ConsentBizTermsRequest("UNIQUE_ID", "Y", 3, "N"),         // 사용 안 함
                new ConsentBizTermsRequest("AD_RECEIVE", "N", 1, "Y")), "admin01");
        ConsentListDto l = service.getTerms("SIGN_UP");
        assertThat(codes(l.getRequired())).containsExactly("SVC_USE:v1", "MARKETING_USE:v1");
        assertThat(codes(l.getOptional())).containsExactly("AD_RECEIVE:v1");
        assertThat(admin.bizTerms("SIGN_UP")).hasSize(4);

        assertCode(() -> admin.saveBizTerms("SIGN_UP", List.of(new ConsentBizTermsRequest("NOPE", "Y", 1, "Y")), "a"), "CNS-301");
        assertCode(() -> admin.saveBizTerms("SIGN_UP", List.of(
                new ConsentBizTermsRequest("SVC_USE", "Y", 1, "Y"), new ConsentBizTermsRequest("SVC_USE", "N", 2, "Y")), "a"), "CNS-301");
        assertThat(admin.bizTerms("SIGN_UP")).hasSize(4);                    // 실패 시 기존 유지 (검증 먼저)

        ConsentBizDto card = new ConsentBizDto();
        card.setBizCd("CARD_APPLY");
        card.setBizNm("카드신청");
        admin.saveBiz(card, "admin01");
        assertCode(() -> service.getTerms("CARD_APPLY"), "CNS-002");          // 연결된 약관 없음
        admin.saveBizTerms("CARD_APPLY", List.of(new ConsentBizTermsRequest("SVC_USE", "Y", 1, "Y")), "admin01");
        assertThat(service.getTerms("CARD_APPLY").getRequired()).hasSize(1);

        card.setUseYn("N");
        card.setBizNm("카드신청(중지)");
        assertThat(admin.saveBiz(card, "admin01").getBizNm()).isEqualTo("카드신청(중지)");
        assertCode(() -> service.getTerms("CARD_APPLY"), "CNS-001");
        assertThat(admin.bizList()).hasSize(6);
    }

    @Test
    @DisplayName("API: 목록 JSON(전문 제외) → 동의 → agreeNo / 오류는 success=false + CNS 코드")
    void api() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ConsentApiController(service),
                        new ConsentAdminApiController(admin, service, new ConsentAdminAuth(props)))
                .setControllerAdvice(new ConsentExceptionHandler()).build();
        MockHttpSession session = new MockHttpSession();

        mvc.perform(get("/api/common/consent/LOAN_APPLY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.required", hasSize(4)))
                .andExpect(jsonPath("$.data.optional", hasSize(1)))
                .andExpect(jsonPath("$.data.required[0].termsCd").value("LOAN_BASIC"))
                .andExpect(jsonPath("$.data.required[0].termsCn").doesNotExist());

        String body = "{\"channel\":\"PC_WEB\",\"items\":["
                + "{\"termsCd\":\"LOAN_BASIC\",\"termsVer\":1,\"agreeYn\":\"Y\"},"
                + "{\"termsCd\":\"PRIVACY_COLLECT\",\"termsVer\":2,\"agreeYn\":\"Y\",\"viewYn\":\"Y\"},"
                + "{\"termsCd\":\"CREDIT_INQUIRY\",\"termsVer\":1,\"agreeYn\":\"Y\"},"
                + "{\"termsCd\":\"PRIVACY_PROVIDE\",\"termsVer\":1,\"agreeYn\":\"Y\"}]}";
        mvc.perform(post("/api/common/consent/LOAN_APPLY/agree").session(session).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.agreeNo").isNumber())
                .andExpect(jsonPath("$.data.declinedTerms[0]").value("MARKETING_USE"));

        mvc.perform(post("/api/common/consent/LOAN_APPLY/agree").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channel\":\"APP\",\"items\":[{\"termsCd\":\"LOAN_BASIC\",\"termsVer\":1,\"agreeYn\":\"Y\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("CNS-101"));

        mvc.perform(get("/api/common/consent-admin/agreements").param("bizCd", "LOAN_APPLY"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].channel").value("PC_WEB"))
                .andExpect(jsonPath("$.data[0].agreeCnt").value(4))
                .andExpect(jsonPath("$.data[0].totalCnt").value(5));

        mvc.perform(get("/api/common/consent-admin/biz/SIGN_UP/preview").param("baseDt", "2027-01-01"))
                .andExpect(jsonPath("$.data.required[0].termsVer").value(2));

        props.setAdminApiOpen(false);
        mvc.perform(get("/api/common/consent-admin/biz")).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CNS-401"));
    }

    private static void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String code) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ConsentException.class,
                e -> assertThat(e.getErrorCode().getCode()).isEqualTo(code));
    }
}
