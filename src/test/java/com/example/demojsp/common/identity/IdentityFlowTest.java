package com.example.demojsp.common.identity;

import com.example.demojsp.common.identity.config.IdentityProperties;
import com.example.demojsp.common.identity.domain.AuthMethod;
import com.example.demojsp.common.identity.domain.TermsDocument;
import com.example.demojsp.common.identity.provider.mock.MockIdentityProvider;
import com.example.demojsp.common.identity.sample.IdentitySampleController;
import com.example.demojsp.common.identity.service.IdentityProviderRegistry;
import com.example.demojsp.common.identity.service.IdentitySessionManager;
import com.example.demojsp.common.identity.service.IdentityVerificationService;
import com.example.demojsp.common.identity.service.TermsService;
import com.example.demojsp.common.identity.store.InMemoryIdentityTxStore;
import com.example.demojsp.common.identity.web.IdentityApiController;
import com.example.demojsp.common.identity.web.IdentityApiExceptionHandler;
import com.example.demojsp.common.identity.web.IdentityCallbackController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * 공통 본인인증 전체 흐름 테스트 (MockIdentityProvider 사용).
 *
 * <p>기존 테스트(@SpringBootTest)는 MySQL 이 떠 있어야 하지만, 이 테스트는 standalone MockMvc 로
 * 공통모듈 빈만 직접 조립하므로 DB 없이도 실행된다.
 *
 * <p>검증 시나리오: 시작 → 콜백 → 세션 반영 → 결과 조회(마스킹) → 업무 API 1회 사용 /
 * 콜백 재전송·세션 불일치·필수약관 미동의·외부 returnUrl·필수값 누락 차단
 */
class IdentityFlowTest {

    private static final String ALL_TERMS = "[\"NICE_PRIVACY\",\"NICE_NON_CREDIT\"]";

    private final ObjectMapper om = JsonMapper.builder().build();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.systemDefaultZone();
        IdentityProperties props = new IdentityProperties();
        props.getTerms().put(AuthMethod.NICE_MOBILE, List.of(
                new TermsDocument("NICE_PRIVACY", "개인정보 수집·이용 동의", true, "1.0", "/terms/1"),
                new TermsDocument("NICE_NON_CREDIT", "비여신금융거래 약관", true, "1.0", "/terms/2"),
                new TermsDocument("NICE_MARKETING", "마케팅 수신 동의", false, "1.0", "/terms/3")));

        IdentityProviderRegistry registry = new IdentityProviderRegistry(List.of(new MockIdentityProvider(clock)));
        IdentitySessionManager sessionManager = new IdentitySessionManager(props, clock);
        TermsService termsService = new TermsService(props);
        IdentityVerificationService service = new IdentityVerificationService(registry,
                new InMemoryIdentityTxStore(clock), sessionManager, termsService, props, clock);

        mvc = MockMvcBuilders.standaloneSetup(
                        new IdentityApiController(service, registry, termsService, sessionManager),
                        new IdentityCallbackController(service),
                        new IdentitySampleController(sessionManager))
                .setControllerAdvice(new IdentityApiExceptionHandler())
                .build();
    }

    private String startBody(String channel, String terms, String returnUrl) {
        return "{\"method\":\"NICE_MOBILE\",\"channel\":\"" + channel + "\",\"purpose\":\"SIGN_UP\","
                + "\"returnUrl\":\"" + returnUrl + "\",\"agreedTerms\":" + terms + "}";
    }

    /** 인증 시작 후 mock_tx(콜백 키) 반환 */
    private String start(MockHttpSession session, String channel) throws Exception {
        MvcResult r = mvc.perform(post("/api/common/identity/start").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(startBody(channel, ALL_TERMS, "/common/identity/sample")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        JsonNode body = om.readTree(r.getResponse().getContentAsString());
        return body.path("data").path("formParams").path("mock_tx").asString();
    }

    @Test
    @DisplayName("정상 흐름(팝업 채널): 콜백 → 마스킹 결과 → 업무 API 1회 사용 → 재전송 차단")
    void happyPathPopup() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String mockTx = start(session, "PC_WEB");

        mvc.perform(get("/common/identity/callback/nice_mobile").session(session).param("mock_tx", mockTx))
                .andExpect(view().name("common/identity/callback"))
                .andExpect(model().attribute("success", true))
                .andExpect(model().attribute("popup", true));

        // 결과 조회: 기존 MaskingUtil 규칙으로 마스킹, CI 미노출
        mvc.perform(get("/api/common/identity/result").session(session).param("purpose", "SIGN_UP"))
                .andExpect(jsonPath("$.data.name").value("홍*동"))
                .andExpect(jsonPath("$.data.mobileNo").value("010-****-5678"))
                .andExpect(jsonPath("$.data.ci").doesNotExist());

        // 업무 API: 1회 사용 후 소멸 → 두 번째는 IDV-301 (401)
        mvc.perform(post("/api/common/identity/sample/join").session(session))
                .andExpect(jsonPath("$.success").value(true));
        mvc.perform(post("/api/common/identity/sample/join").session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("IDV-301"));

        // 같은 콜백 재전송(새로고침/뒤로가기) → 거래 없음
        mvc.perform(get("/common/identity/callback/nice_mobile").session(session).param("mock_tx", mockTx))
                .andExpect(model().attribute("success", false))
                .andExpect(model().attribute("errorCode", "IDV-101"));
    }

    @Test
    @DisplayName("APP 채널: 콜백 후 returnUrl 로 이동 정보 전달")
    void appChannelReturnUrl() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String mockTx = start(session, "APP");
        mvc.perform(get("/common/identity/callback/nice_mobile").session(session).param("mock_tx", mockTx))
                .andExpect(model().attribute("popup", false))
                .andExpect(model().attribute("returnUrl", "/common/identity/sample"));
    }

    @Test
    @DisplayName("다른 브라우저(세션)로 콜백 → IDV-103")
    void sessionMismatch() throws Exception {
        String mockTx = start(new MockHttpSession(), "PC_WEB");
        mvc.perform(get("/common/identity/callback/nice_mobile").session(new MockHttpSession()).param("mock_tx", mockTx))
                .andExpect(model().attribute("success", false))
                .andExpect(model().attribute("errorCode", "IDV-103"));
    }

    @Test
    @DisplayName("필수약관 미동의 → IDV-002 (선택약관은 미동의 허용)")
    void requiredTermsNotAgreed() throws Exception {
        mvc.perform(post("/api/common/identity/start").session(new MockHttpSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(startBody("PC_WEB", "[\"NICE_PRIVACY\"]", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("IDV-002"));
    }

    @Test
    @DisplayName("외부 도메인 returnUrl(//evil.com) → IDV-003")
    void openRedirectBlocked() throws Exception {
        mvc.perform(post("/api/common/identity/start").session(new MockHttpSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(startBody("APP", ALL_TERMS, "//evil.com")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IDV-003"));
    }

    @Test
    @DisplayName("필수값 누락 / 잘못된 enum → IDV-004")
    void invalidRequest() throws Exception {
        mvc.perform(post("/api/common/identity/start").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IDV-004"));
        mvc.perform(post("/api/common/identity/start").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"UNKNOWN\",\"channel\":\"PC_WEB\",\"purpose\":\"SIGN_UP\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IDV-004"));
    }

    @Test
    @DisplayName("약관 목록 조회 - 비여신금융거래 약관 포함")
    void terms() throws Exception {
        mvc.perform(get("/api/common/identity/terms").param("method", "NICE_MOBILE"))
                .andExpect(jsonPath("$.data[1].title").value("비여신금융거래 약관"));
    }
}
