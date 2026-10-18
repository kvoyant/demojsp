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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [공통#50] 저축 - 이어하기 툴팁 (하단 '저축' 탭 숫자) 테스트. 기준시각 2026-10-20 10:00 KST.
 * customer01: 수신 2건 + 여신 1건 진행 중, 수신 1건 만료 / customer02: 여신만 1건.
 */
class SavingsTabTooltipTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-20T01:00:00Z"), ZoneId.of("Asia/Seoul"));
    private ResumeProperties props;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        props = new ResumeProperties();
        props.setDemoApiOpen(true);
        ResumeProperties.PartnerSeed own = new ResumeProperties.PartnerSeed();
        own.setCode("OWN"); own.setName("페퍼저축은행"); own.setOwn(true);
        props.getPartners().add(own);
        props.getSeed().add(seed("customer01", "DEPOSIT", "D1", 7));
        props.getSeed().add(seed("customer01", "DEPOSIT", "D2", 7));
        props.getSeed().add(seed("customer01", "DEPOSIT", "D_OLD", -1));     // 만료 → 건수 제외
        props.getSeed().add(seed("customer01", "LOAN", "L1", 7));
        props.getSeed().add(seed("customer02", "LOAN", "L2", 7));
        rebuild();
    }

    private void rebuild() {
        ResumeService service = new ResumeService(new InMemoryResumeRepository(), new InMemoryPartnerRepository(props), props, CLOCK);
        ResumeRequestContext ctx = new ResumeRequestContext(props);
        mvc = MockMvcBuilders.standaloneSetup(new ResumeApiController(service, ctx), new ResumeDemoApiController(service, ctx))
                .setControllerAdvice(new ResumeApiExceptionHandler()).build();
    }

    private static ResumeProperties.Seed seed(String customer, String type, String code, int expireDays) {
        ResumeProperties.Seed s = new ResumeProperties.Seed();
        s.setCustomerId(customer); s.setProductType(type);
        s.setBizCode(type.equals("LOAN") ? "LOAN_APPLY" : "DEPOSIT_OPEN"); s.setBizName("신청");
        s.setProductCode(code); s.setProductName(code); s.setPartnerCode("OWN");
        s.setStepName("단계"); s.setStepNo(1); s.setTotalSteps(3); s.setResumeUrl("/x/" + code);
        s.setStartedMinutesAgo(60); s.setLastActivityMinutesAgo(30); s.setExpireDays(expireDays);
        return s;
    }

    @Test
    @DisplayName("기능1: 저축 탭 숫자 = 진행 중 수신 이어하기 건수 (만료·여신 제외)")
    void depositCount() throws Exception {
        mvc.perform(get("/api/common/resume/counts").param("demoCustomer", "customer01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.DEPOSIT").value(2))
                .andExpect(jsonPath("$.data.LOAN").value(1))
                .andExpect(jsonPath("$.data.ALL").value(3));
    }

    @Test
    @DisplayName("기능2 미노출: 진행 중 계좌개설 없음 → 0 / 취소해서 잔여 0건 → 0")
    void hiddenWhenNoneOrCanceled() throws Exception {
        mvc.perform(get("/api/common/resume/counts").param("demoCustomer", "customer02"))
                .andExpect(jsonPath("$.data.DEPOSIT").value(0));
        String body = mvc.perform(get("/api/common/resume/list").param("scope", "DEPOSIT").param("demoCustomer", "customer01"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<String> ids = Pattern.compile("\"id\":(\\d+)").matcher(body).results().map(m -> m.group(1)).toList();
        assertThat(ids).hasSize(2);
        mvc.perform(post("/api/common/resume-demo/" + ids.get(0) + "/cancel").param("demoCustomer", "customer01"));
        mvc.perform(get("/api/common/resume/counts").param("demoCustomer", "customer01")).andExpect(jsonPath("$.data.DEPOSIT").value(1));
        mvc.perform(post("/api/common/resume-demo/" + ids.get(1) + "/cancel").param("demoCustomer", "customer01"));
        mvc.perform(get("/api/common/resume/counts").param("demoCustomer", "customer01"))
                .andExpect(jsonPath("$.data.DEPOSIT").value(0))
                .andExpect(jsonPath("$.data.LOAN").value(1));
    }

    @Test
    @DisplayName("정책: 비로그인(운영 설정)도 오류 없이 0건 → 탭바는 그대로, 툴팁만 미노출")
    void guestZeroNoError() throws Exception {
        props.setDemoApiOpen(false);
        rebuild();
        mvc.perform(get("/api/common/resume/counts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.DEPOSIT").value(0));
    }

    @Test
    @DisplayName("화면 계약: 저축 탭 배지·0건 숨김·탭 이동과 무관한 상시 표시·변경 이벤트 갱신·스크린리더 건수")
    void scriptContract() throws IOException {
        String js = resource("/static/js/common/bottom-nav.js");
        assertThat(js).contains("SAVINGS: 'DEPOSIT'", "tab.badge.hidden = n === 0", "'resume:changed'", "visibilitychange",
                "진행 중인 저축 가입", "aria-current", "/api/common/resume/counts").doesNotContain("innerHTML");
        assertThat(resource("/static/js/common/resume-card.js")).contains("new CustomEvent('resume:changed')");
    }

    private static String resource(String path) throws IOException {
        try (InputStream in = SavingsTabTooltipTest.class.getResourceAsStream(path)) {
            assertThat(in).as(path).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
