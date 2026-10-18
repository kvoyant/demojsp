package com.example.demojsp.common.resume;

import com.example.demojsp.common.resume.config.ResumeProperties;
import com.example.demojsp.common.resume.service.ResumeService;
import com.example.demojsp.common.resume.store.InMemoryPartnerRepository;
import com.example.demojsp.common.resume.store.InMemoryResumeRepository;
import com.example.demojsp.common.resume.web.ResumeApiController;
import com.example.demojsp.common.resume.web.ResumeApiExceptionHandler;
import com.example.demojsp.common.resume.web.ResumeDemoApiController;
import com.example.demojsp.common.resume.web.ResumeRequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [CF-BC-13 / 공통#43] 홈 - 이어하기 흐름 테스트 (standalone MockMvc - DB 불필요).
 *
 * <p>기준시각 고정: 2026-10-20 10:00 (Asia/Seoul). seed (customer01):
 * <ul>
 *   <li>S1 페퍼스마트대출 (여신·제휴A) 20분 전 진행 ← 최신</li>
 *   <li>S2 페퍼스 파킹통장4 (수신·당행) 2일 전</li>
 *   <li>S3 페퍼스 사업자담보대출 (여신·당행) 5일 전</li>
 *   <li>S4 완료 건 (10분 전) / S5 만료 건 (5분 전 진행이지만 만료) → 미노출</li>
 * </ul>
 * customer02 = 수신 1건, customer03 = 없음.
 */
class ResumeFlowTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    /** 2026-10-20 10:00 KST */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-20T01:00:00Z"), KST);

    private ResumeProperties props;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        props = new ResumeProperties();
        props.setDemoApiOpen(true);
        props.getPartners().add(partner("OWN", "페퍼저축은행", null, true));
        props.getPartners().add(partner("PARTNER_A", "제휴플랫폼A", "/images/common/resume/logo-partner-sample.svg", false));
        props.getPartners().add(partner("PARTNER_B", "제휴플랫폼B", null, false));
        props.getSeed().add(seed("customer01", "LOAN", "PS_SMART", "페퍼스마트대출", "PARTNER_A", "서류제출", 3, 5, 1500, 20, 7, "IN_PROGRESS"));
        props.getSeed().add(seed("customer01", "DEPOSIT", "PS_PARKING4", "페퍼스 파킹통장4", "OWN", "본인인증", 2, 4, 3000, 2880, 30, "IN_PROGRESS"));
        props.getSeed().add(seed("customer01", "LOAN", "PS_BIZ_MORTGAGE", "페퍼스 사업자담보대출", "OWN", "담보정보입력", 2, 6, 8000, 7200, 30, "IN_PROGRESS"));
        props.getSeed().add(seed("customer01", "DEPOSIT", "PS_2030", "완료된 적금", "OWN", "완료", 4, 4, 600, 10, 30, "COMPLETED"));
        props.getSeed().add(seed("customer01", "LOAN", "PS_OLD", "만료된 대출", "PARTNER_B", "한도조회", 1, 5, 14400, 5, 7, "IN_PROGRESS"));
        props.getSeed().add(seed("customer02", "DEPOSIT", "PS_2030", "페퍼스 2030적금", "OWN", "약관동의", 1, 4, 90, 60, null, "IN_PROGRESS"));
        rebuild();
    }

    private void rebuild() {
        ResumeService service = new ResumeService(new InMemoryResumeRepository(), new InMemoryPartnerRepository(props), props, CLOCK);
        ResumeRequestContext context = new ResumeRequestContext(props);
        mvc = MockMvcBuilders.standaloneSetup(
                        new ResumeApiController(service, context),
                        new ResumeDemoApiController(service, context))
                .setControllerAdvice(new ResumeApiExceptionHandler())
                .build();
    }

    private static ResumeProperties.PartnerSeed partner(String code, String name, String logo, boolean own) {
        ResumeProperties.PartnerSeed p = new ResumeProperties.PartnerSeed();
        p.setCode(code);
        p.setName(name);
        p.setLogoUrl(logo);
        p.setOwn(own);
        return p;
    }

    private static ResumeProperties.Seed seed(String customer, String type, String productCode, String productName,
                                              String partner, String step, int stepNo, int total,
                                              long startedAgo, long lastAgo, Integer expireDays, String status) {
        ResumeProperties.Seed s = new ResumeProperties.Seed();
        s.setCustomerId(customer);
        s.setProductType(type);
        s.setBizCode(type.equals("LOAN") ? "LOAN_APPLY" : "DEPOSIT_OPEN");
        s.setBizName(type.equals("LOAN") ? "대출신청" : "계좌개설");
        s.setProductCode(productCode);
        s.setProductName(productName);
        s.setPartnerCode(partner);
        s.setStepName(step);
        s.setStepNo(stepNo);
        s.setTotalSteps(total);
        s.setResumeUrl("/common/resume/landing?biz=" + s.getBizCode() + "&product=" + productCode + "&step=" + stepNo);
        s.setStartedMinutesAgo(startedAgo);
        s.setLastActivityMinutesAgo(lastAgo);
        s.setExpireDays(expireDays);
        s.setStatus(status);
        return s;
    }

    private static String step(String productCode, String productName, String partner, int stepNo, String url) {
        return "{\"productType\":\"LOAN\",\"bizCode\":\"LOAN_APPLY\",\"bizName\":\"대출신청\",\"productCode\":\"" + productCode + "\","
                + "\"productName\":\"" + productName + "\",\"partnerCode\":\"" + partner + "\",\"stepName\":\"전자서명\","
                + "\"stepNo\":" + stepNo + ",\"totalSteps\":5,\"resumeUrl\":\"" + url + "\",\"expireDays\":7}";
    }

    @Test
    @DisplayName("기능1·2·3·5: 홈(ALL) - 진행 중 3건 숫자 + 최신 1건(마지막 진행일시) + 더보기 URL + 로고·회사명·상품명")
    void homeShowsLatestAndCount() throws Exception {
        mvc.perform(get("/api/common/resume").param("scope", "ALL").param("demoCustomer", "customer01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scope").value("ALL"))
                .andExpect(jsonPath("$.data.count").value(3))
                .andExpect(jsonPath("$.data.counts.ALL").value(3))
                .andExpect(jsonPath("$.data.counts.DEPOSIT").value(1))
                .andExpect(jsonPath("$.data.counts.LOAN").value(2))
                .andExpect(jsonPath("$.data.moreUrl").value("/common/resume/status"))
                .andExpect(jsonPath("$.data.baseAt").value("2026-10-20 10:00:00"))
                .andExpect(jsonPath("$.data.latest.productName").value("페퍼스마트대출"))
                .andExpect(jsonPath("$.data.latest.companyName").value("제휴플랫폼A"))
                .andExpect(jsonPath("$.data.latest.companyLogoUrl").value("/images/common/resume/logo-partner-sample.svg"))
                .andExpect(jsonPath("$.data.latest.own").value(false))
                .andExpect(jsonPath("$.data.latest.stepName").value("서류제출"))
                .andExpect(jsonPath("$.data.latest.stepNo").value(3))
                .andExpect(jsonPath("$.data.latest.totalSteps").value(5))
                // 내부 정보는 응답에 없음 (고객 ID, 이동 URL - 이동은 continue API 로만)
                .andExpect(jsonPath("$.data.latest.customerId").doesNotExist())
                .andExpect(jsonPath("$.data.latest.resumeUrl").doesNotExist());
    }

    @Test
    @DisplayName("완료·만료 건은 최근에 진행했어도 건수·최신 1건에서 제외")
    void completedAndExpiredExcluded() throws Exception {
        mvc.perform(get("/api/common/resume/list").param("demoCustomer", "customer01"))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[*].productName").value(contains("페퍼스마트대출", "페퍼스 파킹통장4", "페퍼스 사업자담보대출")));
    }

    @Test
    @DisplayName("scope 재사용: DEPOSIT(저축)=수신 1건·당행 로고 없음 / LOAN(대출)=여신 2건 / 잘못된 scope RSM-001")
    void scopeReuse() throws Exception {
        mvc.perform(get("/api/common/resume").param("scope", "deposit").param("demoCustomer", "customer01"))
                .andExpect(jsonPath("$.data.count").value(1))
                .andExpect(jsonPath("$.data.latest.productName").value("페퍼스 파킹통장4"))
                .andExpect(jsonPath("$.data.latest.companyName").value("페퍼저축은행"))
                .andExpect(jsonPath("$.data.latest.own").value(true))
                .andExpect(jsonPath("$.data.latest.companyLogoUrl").doesNotExist());
        mvc.perform(get("/api/common/resume").param("scope", "LOAN").param("demoCustomer", "customer01"))
                .andExpect(jsonPath("$.data.count").value(2))
                .andExpect(jsonPath("$.data.latest.productName").value("페퍼스마트대출"));
        mvc.perform(get("/api/common/resume").param("scope", "CARD").param("demoCustomer", "customer01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RSM-001"));
    }

    @Test
    @DisplayName("제공대상: 이어하기 없는 고객은 count=0, latest=null (카드 미노출) / 수신만 있으면 대출 탭 0건")
    void noTargetNoCard() throws Exception {
        mvc.perform(get("/api/common/resume").param("demoCustomer", "customer03"))
                .andExpect(jsonPath("$.data.count").value(0))
                .andExpect(jsonPath("$.data.latest").doesNotExist());
        mvc.perform(get("/api/common/resume").param("scope", "LOAN").param("demoCustomer", "customer02"))
                .andExpect(jsonPath("$.data.count").value(0))
                .andExpect(jsonPath("$.data.counts.ALL").value(1));
    }

    @Test
    @DisplayName("기능4: [이어하기] → 해당 상품 신청 링크 / 다른 고객 ID 는 RSM-003 / 취소 후에는 RSM-002")
    void continueResume() throws Exception {
        long id = latestId("customer01");
        mvc.perform(post("/api/common/resume/" + id + "/continue").param("demoCustomer", "customer01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").value("/common/resume/landing?biz=LOAN_APPLY&product=PS_SMART&step=3"));
        mvc.perform(post("/api/common/resume/" + id + "/continue").param("demoCustomer", "customer02"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RSM-003"));

        mvc.perform(post("/api/common/resume-demo/" + id + "/cancel").param("demoCustomer", "customer01"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/common/resume/" + id + "/continue").param("demoCustomer", "customer01"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RSM-002"));
        // 취소 후 최신 1건은 다음 건으로, 건수 2
        mvc.perform(get("/api/common/resume").param("demoCustomer", "customer01"))
                .andExpect(jsonPath("$.data.count").value(2))
                .andExpect(jsonPath("$.data.latest.productName").value("페퍼스 파킹통장4"));
    }

    @Test
    @DisplayName("단계 저장: 같은 업무·상품 진행 중이면 갱신(건수 유지, 최신으로 올라옴) / 새 상품이면 등록(건수 +1)")
    void saveStepUpsert() throws Exception {
        mvc.perform(post("/api/common/resume-demo/steps").param("demoCustomer", "customer01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(step("PS_BIZ_MORTGAGE", "페퍼스 사업자담보대출", "OWN", 4, "/loan/apply?step=4")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/common/resume").param("demoCustomer", "customer01"))
                .andExpect(jsonPath("$.data.count").value(3))
                .andExpect(jsonPath("$.data.latest.productName").value("페퍼스 사업자담보대출"))
                .andExpect(jsonPath("$.data.latest.stepNo").value(4))
                .andExpect(jsonPath("$.data.latest.lastActivityAt").value("2026-10-20 10:00"));

        mvc.perform(post("/api/common/resume-demo/steps").param("demoCustomer", "customer01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(step("PB_CREDIT", "제휴 신용대출", "PARTNER_B", 1, "/loan/apply?p=PB")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/common/resume").param("demoCustomer", "customer01"))
                .andExpect(jsonPath("$.data.count").value(4))
                .andExpect(jsonPath("$.data.counts.LOAN").value(3))
                .andExpect(jsonPath("$.data.latest.companyName").value("제휴플랫폼B"))
                .andExpect(jsonPath("$.data.latest.companyLogoUrl").doesNotExist())
                .andExpect(jsonPath("$.data.latest.daysLeft").value(7));
    }

    @Test
    @DisplayName("서버 검증: javascript:/외부(//) 링크 RSM-103, 미등록 회사코드 RSM-104, 필수 누락 RSM-101")
    void validation() throws Exception {
        mvc.perform(post("/api/common/resume-demo/steps").param("demoCustomer", "customer01")
                        .contentType(MediaType.APPLICATION_JSON).content(step("X1", "상품", "OWN", 1, "javascript:alert(1)")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("RSM-103"));
        mvc.perform(post("/api/common/resume-demo/steps").param("demoCustomer", "customer01")
                        .contentType(MediaType.APPLICATION_JSON).content(step("X1", "상품", "OWN", 1, "//evil.example.com/x")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("RSM-103"));
        mvc.perform(post("/api/common/resume-demo/steps").param("demoCustomer", "customer01")
                        .contentType(MediaType.APPLICATION_JSON).content(step("X1", "상품", "NO_SUCH", 1, "/loan")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("RSM-104"));
        mvc.perform(post("/api/common/resume-demo/steps").param("demoCustomer", "customer01")
                        .contentType(MediaType.APPLICATION_JSON).content(step("X1", "", "OWN", 1, "/loan")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("RSM-101"));
    }

    @Test
    @DisplayName("운영 설정(demo-api-open=false): 비로그인 RSM-401, 테스트 API RSM-402")
    void productionMode() throws Exception {
        props.setDemoApiOpen(false);
        rebuild();
        mvc.perform(get("/api/common/resume").param("demoCustomer", "customer01"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("RSM-401"));
        mvc.perform(post("/api/common/resume-demo/1/cancel"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("RSM-402"));
    }

    @Test
    @DisplayName("화면 계약: 카드 JS 가 상단 카드 구조(건수·더보기·로고·회사명·상품명·이어하기)와 0건 숨김을 갖춤")
    void cardScriptContract() throws IOException {
        String js = resource("/static/js/common/resume-card.js");
        assertThat(js).contains("ui-resume__count", "ui-resume__more", "ui-resume__logo", "ui-resume__company",
                "ui-resume__product", "이어하기", "/continue", "host.hidden = true", "textContent");
        assertThat(js).doesNotContain("innerHTML");
        String css = resource("/static/css/common/resume-card.css");
        assertThat(css).doesNotContain("position: fixed");       // 팝업이 아닌 화면 안 카드 (뒷 화면 가림 해소)
    }

    private long latestId(String customer) throws Exception {
        String body = mvc.perform(get("/api/common/resume").param("demoCustomer", customer))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return Long.parseLong(body.replaceAll(".*\"latest\":\\{\"id\":(\\d+).*", "$1"));
    }

    private static String resource(String path) throws IOException {
        try (InputStream in = ResumeFlowTest.class.getResourceAsStream(path)) {
            assertThat(in).as(path).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
