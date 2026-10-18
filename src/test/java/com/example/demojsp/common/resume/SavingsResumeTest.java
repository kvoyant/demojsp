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
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [공통#49] 저축 - 이어하기 (scope=DEPOSIT) 테스트. 기준시각 2026-10-20 10:00 KST.
 *
 * <p>customer01: 여신 대출(20분 전, 홈 최신) · 수신 파킹통장(2일 전) · 수신 정기예금(3일 전) · 여신 담보(5일 전)
 */
class SavingsResumeTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-20T01:00:00Z"), ZoneId.of("Asia/Seoul"));
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        ResumeProperties props = new ResumeProperties();
        props.setDemoApiOpen(true);
        props.getPartners().add(partner("OWN", "페퍼저축은행", null, true));
        props.getPartners().add(partner("PARTNER_A", "제휴플랫폼A", "/images/a.svg", false));
        props.getSeed().add(seed("LOAN", "PS_SMART", "페퍼스마트대출", "PARTNER_A", 20));
        props.getSeed().add(seed("DEPOSIT", "PS_PARKING4", "페퍼스 파킹통장4", "OWN", 2880));
        props.getSeed().add(seed("DEPOSIT", "PS_TIME", "페퍼스 정기예금", "OWN", 4000));
        props.getSeed().add(seed("LOAN", "PS_BIZ", "페퍼스 사업자담보대출", "OWN", 7200));
        ResumeService service = new ResumeService(new InMemoryResumeRepository(), new InMemoryPartnerRepository(props), props, CLOCK);
        ResumeRequestContext ctx = new ResumeRequestContext(props);
        mvc = MockMvcBuilders.standaloneSetup(new ResumeApiController(service, ctx), new ResumeDemoApiController(service, ctx))
                .setControllerAdvice(new ResumeApiExceptionHandler()).build();
    }

    private static ResumeProperties.PartnerSeed partner(String code, String name, String logo, boolean own) {
        ResumeProperties.PartnerSeed p = new ResumeProperties.PartnerSeed();
        p.setCode(code); p.setName(name); p.setLogoUrl(logo); p.setOwn(own);
        return p;
    }

    private static ResumeProperties.Seed seed(String type, String code, String name, String partner, long lastAgo) {
        ResumeProperties.Seed s = new ResumeProperties.Seed();
        s.setCustomerId("customer01");
        s.setProductType(type);
        s.setBizCode(type.equals("LOAN") ? "LOAN_APPLY" : "DEPOSIT_OPEN");
        s.setBizName(type.equals("LOAN") ? "대출신청" : "계좌개설");
        s.setProductCode(code); s.setProductName(name); s.setPartnerCode(partner);
        s.setStepName("본인인증"); s.setStepNo(2); s.setTotalSteps(4);
        s.setResumeUrl("/apply/" + code); s.setStartedMinutesAgo(lastAgo + 60); s.setLastActivityMinutesAgo(lastAgo);
        s.setExpireDays(30);
        return s;
    }

    @Test
    @DisplayName("기능1·2: 저축 탭 = 수신 이어하기 중 최신 1건 + 수신 건수 (홈 최신인 여신 건은 제외)")
    void depositLatestAndCount() throws Exception {
        mvc.perform(get("/api/common/resume").param("scope", "DEPOSIT").param("demoCustomer", "customer01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scope").value("DEPOSIT"))
                .andExpect(jsonPath("$.data.count").value(2))
                .andExpect(jsonPath("$.data.counts.ALL").value(4))
                .andExpect(jsonPath("$.data.latest.productName").value("페퍼스 파킹통장4"))
                .andExpect(jsonPath("$.data.latest.productType").value("DEPOSIT"));
        mvc.perform(get("/api/common/resume/list").param("scope", "DEPOSIT").param("demoCustomer", "customer01"))
                .andExpect(jsonPath("$.data[*].productName").value(contains("페퍼스 파킹통장4", "페퍼스 정기예금")))
                .andExpect(jsonPath("$.data[*].productType", everyItem(is("DEPOSIT"))));
    }

    @Test
    @DisplayName("기능3·5: 더보기 = 신청현황조회, 로고 = 당행(own) + 회사명 DB")
    void moreAndOwnLogo() throws Exception {
        mvc.perform(get("/api/common/resume").param("scope", "DEPOSIT").param("demoCustomer", "customer01"))
                .andExpect(jsonPath("$.data.moreUrl").value("/common/resume/status"))
                .andExpect(jsonPath("$.data.latest.own").value(true))
                .andExpect(jsonPath("$.data.latest.companyName").value("페퍼저축은행"));
    }

    @Test
    @DisplayName("기능4: [이어하기] → 해당 상품 신청 링크, 취소 후 다음 수신 건이 최신")
    void continueAndCancel() throws Exception {
        String body = mvc.perform(get("/api/common/resume").param("scope", "DEPOSIT").param("demoCustomer", "customer01"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long id = Long.parseLong(body.replaceAll(".*\"latest\":\\{\"id\":(\\d+).*", "$1"));
        mvc.perform(post("/api/common/resume/" + id + "/continue").param("demoCustomer", "customer01"))
                .andExpect(jsonPath("$.data.url").value("/apply/PS_PARKING4"));
        mvc.perform(post("/api/common/resume-demo/" + id + "/cancel").param("demoCustomer", "customer01")).andExpect(status().isOk());
        mvc.perform(get("/api/common/resume").param("scope", "DEPOSIT").param("demoCustomer", "customer01"))
                .andExpect(jsonPath("$.data.count").value(1))
                .andExpect(jsonPath("$.data.latest.productName").value("페퍼스 정기예금"));
    }

    @Test
    @DisplayName("정책2 수신상품만 취급·당행 로고: 수신 이어하기를 제휴사 코드로 등록하면 RSM-105")
    void depositOwnOnly() throws Exception {
        mvc.perform(post("/api/common/resume-demo/steps").param("demoCustomer", "customer01").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productType\":\"DEPOSIT\",\"bizCode\":\"DEPOSIT_OPEN\",\"bizName\":\"계좌개설\",\"productCode\":\"X\","
                                + "\"productName\":\"제휴 예금\",\"partnerCode\":\"PARTNER_A\",\"stepName\":\"약관\",\"stepNo\":1,\"totalSteps\":3,\"resumeUrl\":\"/x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RSM-105"));
    }

    @Test
    @DisplayName("화면: scope 별 기본 제목 (저축 탭 \"진행 중인 저축 가입\")")
    void titleByScope() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/static/js/common/resume-card.js")) {
            assertThat(in).isNotNull();
            String js = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(js).contains("DEPOSIT: '진행 중인 저축 가입'", "TITLES[s.scope]");
        }
    }
}
