package com.example.demojsp.common.terms;

import com.example.demojsp.common.terms.config.TermsProperties;
import com.example.demojsp.common.terms.domain.AgreeRequest;
import com.example.demojsp.common.terms.domain.AgreeResult;
import com.example.demojsp.common.terms.sample.TermsSampleController;
import com.example.demojsp.common.terms.service.TermsAgreeService;
import com.example.demojsp.common.terms.service.TermsSessionManager;
import com.example.demojsp.common.terms.service.TermsSupport;
import com.example.demojsp.common.terms.store.InMemoryTermsAgreementRepository;
import com.example.demojsp.common.terms.store.InMemoryTermsRepository;
import com.example.demojsp.common.terms.web.TermsAdminApiController;
import com.example.demojsp.common.terms.web.TermsApiController;
import com.example.demojsp.common.terms.web.TermsApiExceptionHandler;
import com.example.demojsp.common.terms.web.TermsRequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [CF-BC-04] 약관동의 공통모듈 흐름 테스트 (standalone MockMvc - DB 불필요).
 *
 * <p>시나리오: 필수/선택 분리 조회 → 상세보기 → 동의(증적·세션) → 업무 API 1회 사용 /
 * 필수 미동의·개정(버전 불일치)·미등록 약관·미등록 업무·중복 버전·ADMIN 권한 차단 / 세션 만료 / 해시 정규화
 */
class TermsFlowTest {

    private static final String BIZ = "SIGN_UP";

    private TermsProperties props;
    private Clock clock;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        clock = Clock.systemDefaultZone();
        props = new TermsProperties();
        props.setAdminApiOpen(true);
        props.getBizNames().put(BIZ, "회원가입");
        props.getSeed().add(seed("SVC_USE", "전자금융거래 기본약관", true, 1));
        props.getSeed().add(seed("PRIVACY", "개인정보 수집·이용 동의", true, 2));
        props.getSeed().add(seed("MARKETING", "마케팅 활용 동의", false, 10));

        TermsAgreeService service = new TermsAgreeService(new InMemoryTermsRepository(props),
                new InMemoryTermsAgreementRepository(), props, clock);
        TermsSessionManager sessionManager = new TermsSessionManager(props, clock);
        TermsRequestContext context = new TermsRequestContext(props);

        mvc = MockMvcBuilders.standaloneSetup(
                        new TermsApiController(service, sessionManager, context),
                        new TermsAdminApiController(service, context),
                        new TermsSampleController(sessionManager))
                .setControllerAdvice(new TermsApiExceptionHandler())
                .build();
    }

    private static TermsProperties.Seed seed(String code, String title, boolean required, int sort) {
        TermsProperties.Seed s = new TermsProperties.Seed();
        s.setBizCode(BIZ);
        s.setCode(code);
        s.setTitle(title);
        s.setRequired(required);
        s.setVersion("1.0");
        s.setSortOrder(sort);
        s.setContent(title + " 전문입니다.");
        return s;
    }

    /** "CODE:VERSION:true" 형식 → 동의 요청 JSON */
    private static String agreeBody(String... items) {
        StringBuilder sb = new StringBuilder("{\"channel\":\"PC_WEB\",\"items\":[");
        for (int i = 0; i < items.length; i++) {
            String[] p = items[i].split(":");
            if (i > 0) sb.append(',');
            sb.append("{\"termsCode\":\"").append(p[0]).append("\",\"version\":\"").append(p[1])
                    .append("\",\"agreed\":").append(p[2]).append('}');
        }
        return sb.append("]}").toString();
    }

    @Test
    @DisplayName("업무코드 약관 조회 - 필수/선택 분리, 목록에는 전문 미포함")
    void listSplitsRequiredAndOptional() throws Exception {
        mvc.perform(get("/api/common/terms/" + BIZ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bizName").value("회원가입"))
                .andExpect(jsonPath("$.data.required", hasSize(2)))
                .andExpect(jsonPath("$.data.required[0].termsCode").value("SVC_USE"))
                .andExpect(jsonPath("$.data.optional", hasSize(1)))
                .andExpect(jsonPath("$.data.optional[0].termsCode").value("MARKETING"))
                .andExpect(jsonPath("$.data.required[0].content").doesNotExist());
    }

    @Test
    @DisplayName("약관 상세보기 - 전문 + SHA-256 해시")
    void detail() throws Exception {
        mvc.perform(get("/api/common/terms/" + BIZ + "/items/PRIVACY"))
                .andExpect(jsonPath("$.data.content").value("개인정보 수집·이용 동의 전문입니다."))
                .andExpect(jsonPath("$.data.contentHash").value(TermsSupport.sha256("개인정보 수집·이용 동의 전문입니다.")));
        mvc.perform(get("/api/common/terms/" + BIZ + "/items/NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRM-003"));
    }

    @Test
    @DisplayName("정상 동의 → 세션 반영 → 업무 API 1회 사용 → 이력(선택 미동의 N 포함) 저장")
    void happyPath() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/api/common/terms/" + BIZ + "/agree").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(agreeBody("SVC_USE:1.0:true", "PRIVACY:1.0:true", "MARKETING:1.0:false")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.agreedCodes", hasSize(2)))
                .andExpect(jsonPath("$.data.declinedCodes[0]").value("MARKETING"));

        mvc.perform(get("/api/common/terms/" + BIZ + "/status").session(session))
                .andExpect(jsonPath("$.data.bizCode").value(BIZ));

        mvc.perform(post("/api/common/terms-sample/submit").session(session).param("bizCode", BIZ))
                .andExpect(jsonPath("$.success").value(true));
        mvc.perform(post("/api/common/terms-sample/submit").session(session).param("bizCode", BIZ))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TRM-301"));

        mvc.perform(get("/api/common/terms-admin/agreements").param("bizCode", BIZ))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[?(@.termsCode=='MARKETING')].agreed").value(false))
                .andExpect(jsonPath("$.data[?(@.termsCode=='SVC_USE')].contentHash")
                        .value(TermsSupport.sha256("전자금융거래 기본약관 전문입니다.")))
                .andExpect(jsonPath("$.data[0].subjectId").value("GUEST"));
    }

    @Test
    @DisplayName("필수 약관 미동의/누락 → TRM-002, 세션 미반영")
    void requiredNotAgreed() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/api/common/terms/" + BIZ + "/agree").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(agreeBody("SVC_USE:1.0:true", "PRIVACY:1.0:false", "MARKETING:1.0:true")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TRM-002"));
        mvc.perform(post("/api/common/terms/" + BIZ + "/agree").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(agreeBody("SVC_USE:1.0:true")))
                .andExpect(jsonPath("$.code").value("TRM-002"));
        mvc.perform(get("/api/common/terms/" + BIZ + "/status").session(session))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("ADMIN 약관 개정(새 버전) → 이전 버전으로 동의 시 TRM-103, 목록은 새 버전")
    void revisedTermsRequireReAgree() throws Exception {
        mvc.perform(post("/api/common/terms-admin/terms").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bizCode\":\"SIGN_UP\",\"termsCode\":\"PRIVACY\",\"version\":\"1.1\","
                                + "\"title\":\"개인정보 수집·이용 동의\",\"content\":\"개정된 전문\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.required").value(true))   // 필수여부 미입력 → 기존 값 유지
                .andExpect(jsonPath("$.data.registeredBy").value("LOCAL_TEST"));

        mvc.perform(get("/api/common/terms/" + BIZ))
                .andExpect(jsonPath("$.data.required[1].version").value("1.1"));

        mvc.perform(post("/api/common/terms/" + BIZ + "/agree").contentType(MediaType.APPLICATION_JSON)
                        .content(agreeBody("SVC_USE:1.0:true", "PRIVACY:1.0:true")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TRM-103"));

        mvc.perform(post("/api/common/terms/" + BIZ + "/agree").contentType(MediaType.APPLICATION_JSON)
                        .content(agreeBody("SVC_USE:1.0:true", "PRIVACY:1.1:true")))
                .andExpect(status().isOk());

        // 버전 이력: 1.0 / 1.1 모두 보관
        mvc.perform(get("/api/common/terms-admin/" + BIZ + "/versions"))
                .andExpect(jsonPath("$.data", hasSize(4)));
        // 같은 버전 재등록 → TRM-201
        mvc.perform(post("/api/common/terms-admin/terms").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bizCode\":\"SIGN_UP\",\"termsCode\":\"PRIVACY\",\"version\":\"1.1\","
                                + "\"title\":\"x\",\"content\":\"x\"}"))
                .andExpect(jsonPath("$.code").value("TRM-201"));
    }

    @Test
    @DisplayName("미등록 약관 TRM-101 / 중복 TRM-102 / 미등록 업무 TRM-001 / 잘못된 JSON TRM-004")
    void invalidRequests() throws Exception {
        mvc.perform(post("/api/common/terms/" + BIZ + "/agree").contentType(MediaType.APPLICATION_JSON)
                        .content(agreeBody("SVC_USE:1.0:true", "PRIVACY:1.0:true", "HACK:1.0:true")))
                .andExpect(jsonPath("$.code").value("TRM-101"));
        mvc.perform(post("/api/common/terms/" + BIZ + "/agree").contentType(MediaType.APPLICATION_JSON)
                        .content(agreeBody("SVC_USE:1.0:true", "SVC_USE:1.0:true", "PRIVACY:1.0:true")))
                .andExpect(jsonPath("$.code").value("TRM-102"));
        mvc.perform(get("/api/common/terms/NO_SUCH_BIZ"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRM-001"));
        mvc.perform(post("/api/common/terms/" + BIZ + "/agree").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TRM-004"));
    }

    @Test
    @DisplayName("ADMIN API 닫힘(운영 설정) + 비로그인 → TRM-401(403)")
    void adminApiClosed() throws Exception {
        props.setAdminApiOpen(false);
        mvc.perform(get("/api/common/terms-admin/biz-codes"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TRM-401"));
        // 사용자 API 는 영향 없음
        mvc.perform(get("/api/common/terms/" + BIZ)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("동의 결과 세션 유효시간(agreement-ttl) 초과 → 미동의로 간주")
    void sessionResultExpires() {
        TermsAgreeService service = new TermsAgreeService(new InMemoryTermsRepository(props),
                new InMemoryTermsAgreementRepository(), props, clock);
        AgreeResult result = service.agree(BIZ, new AgreeRequest("APP", List.of(
                new AgreeRequest.Item("SVC_USE", "1.0", true),
                new AgreeRequest.Item("PRIVACY", "1.0", true))), "customer01", "127.0.0.1");
        assertThat(result.isAgreed("MARKETING")).isFalse();

        MockHttpSession session = new MockHttpSession();
        new TermsSessionManager(props, clock).save(session, result);
        assertThat(new TermsSessionManager(props, clock).find(session, BIZ)).isPresent();

        Clock later = Clock.offset(clock, props.getAgreementTtl().plus(Duration.ofSeconds(1)));
        assertThat(new TermsSessionManager(props, later).find(session, BIZ)).isEmpty();
    }

    @Test
    @DisplayName("해시 정규화 - 줄바꿈(CRLF/LF)·앞뒤 공백 차이는 같은 해시")
    void hashNormalization() {
        assertThat(TermsSupport.sha256("제1조\r\n목적  ")).isEqualTo(TermsSupport.sha256("제1조\n목적"));
        assertThat(TermsSupport.sha256("제1조\n목적")).hasSize(64).isNotEqualTo(TermsSupport.sha256("제1조\n목적2"));
    }
}
