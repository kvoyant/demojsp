package com.example.demojsp.common.banner;

import com.example.demojsp.common.banner.config.BannerPopupProperties;
import com.example.demojsp.common.banner.service.BannerPopupService;
import com.example.demojsp.common.banner.service.BannerSupport;
import com.example.demojsp.common.banner.store.ConfigAccountHoldingChecker;
import com.example.demojsp.common.banner.store.InMemoryBannerPopupRepository;
import com.example.demojsp.common.banner.web.BannerApiExceptionHandler;
import com.example.demojsp.common.banner.web.BannerPopupApiController;
import com.example.demojsp.common.banner.web.BannerRequestContext;
import com.example.demojsp.common.banner.web.TopSlideBannerApiController;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [CF-BC-20 / 공통#44] 홈 상단슬라이드배너 테스트 (standalone MockMvc - DB 불필요).
 *
 * <p>기준시각 고정: 2026-10-20 10:00 (Asia/Seoul).
 * seed: 상단슬라이드 S1~S6 게시중(표시순서 3,1,2,5,4,6), S7 게시예정, S8 기간 종료(오늘 00:00 종료), S9 미사용 + 배너팝업 P1 게시중.
 * 계좌 보유 고객: customer01.
 */
class TopSlideBannerTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-20T01:00:00Z"), KST);

    private BannerPopupProperties props;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        props = new BannerPopupProperties();
        props.getTopSlide().setDemoCustomerParam(true);
        props.getTopSlide().setAccountHolders(List.of("customer01"));
        props.getSeed().add(slide("S1", 3, "2026-10-18 00:00", "2026-10-30 00:00", "#E3ECFA", true));
        props.getSeed().add(slide("S2", 1, "2026-10-20 00:00", "2026-10-30 00:00", "#0D47A1", true));
        props.getSeed().add(slide("S3", 2, "2026-10-19 00:00", "2026-10-30 00:00", null, true));
        props.getSeed().add(slide("S4", 5, "2026-10-19 00:00", "2026-10-30 00:00", "#FFF4E0", true));
        props.getSeed().add(slide("S5", 4, "2026-10-19 00:00", "2026-10-30 00:00", "#e6f4ea", true));
        props.getSeed().add(slide("S6", 6, "2026-10-19 00:00", "2026-10-30 00:00", "#FFFFFF", true));
        props.getSeed().add(slide("S7 예정", 1, "2026-10-21 00:00", "2026-10-30 00:00", "#FFFFFF", true));
        props.getSeed().add(slide("S8 종료", 1, "2026-10-10 00:00", "2026-10-20 00:00", "#FFFFFF", true));
        props.getSeed().add(slide("S9 미사용", 1, "2026-10-19 00:00", "2026-10-30 00:00", "#FFFFFF", false));
        BannerPopupProperties.Seed popup = new BannerPopupProperties.Seed();
        popup.setCategory("NOTICE");
        popup.setTitle("P1 배너팝업");
        popup.setContent("공지");
        popup.setStartAt("2026-10-19 00:00");
        popup.setEndAt("2026-10-30 00:00");
        props.getSeed().add(popup);
        rebuild();
    }

    private void rebuild() {
        BannerPopupService service = new BannerPopupService(new InMemoryBannerPopupRepository(), props,
                new ConfigAccountHoldingChecker(props), CLOCK);
        BannerRequestContext context = new BannerRequestContext(props);
        mvc = MockMvcBuilders.standaloneSetup(new TopSlideBannerApiController(service, context), new BannerPopupApiController(service))
                .setControllerAdvice(new BannerApiExceptionHandler())
                .build();
    }

    private static BannerPopupProperties.Seed slide(String title, int order, String start, String end, String bg, boolean use) {
        BannerPopupProperties.Seed s = new BannerPopupProperties.Seed();
        s.setPopupType("TOP_SLIDE");
        s.setCategory("EVENT");
        s.setTitle(title);
        s.setContent(title + " 서브");
        s.setImageUrl("/images/common/banner/slide-loan.svg");
        s.setAltText(title + " 이미지");
        s.setLinkUrl("/product/" + order);
        s.setBackgroundColor(bg);
        s.setStartAt(start);
        s.setEndAt(end);
        s.setDisplayOrder(order);
        s.setUse(use);
        return s;
    }

    @Test
    @DisplayName("기능1·2 + 정책2: 게시중 6건 중 표시순서 상위 5건, 2초 간격, 예정·종료(종료 미포함)·미사용 제외")
    void maxFiveInDisplayOrder() throws Exception {
        mvc.perform(get("/api/common/top-slide-banners").param("channel", "APP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eligible").value(true))
                .andExpect(jsonPath("$.data.audience").value("NO_ACCOUNT"))
                .andExpect(jsonPath("$.data.total").value(5))
                .andExpect(jsonPath("$.data.maxCount").value(5))
                .andExpect(jsonPath("$.data.intervalMs").value(2000))
                .andExpect(jsonPath("$.data.truncated").value(1))
                .andExpect(jsonPath("$.data.items[*].mainText").value(contains("S2", "S3", "S1", "S5", "S4")))
                .andExpect(jsonPath("$.data.items[0].subText").value("S2 서브"));
    }

    @Test
    @DisplayName("기능3 배너 구조 + 배경색: 미지정은 기본 배경색, 소문자는 대문자로 정규화, 어두운 배경은 흰 글자")
    void structureAndColors() throws Exception {
        mvc.perform(get("/api/common/top-slide-banners"))
                .andExpect(jsonPath("$.data.items[0].backgroundColor").value("#0D47A1"))
                .andExpect(jsonPath("$.data.items[0].textColor").value("#FFFFFF"))
                .andExpect(jsonPath("$.data.items[1].backgroundColor").value("#E3ECFA"))
                .andExpect(jsonPath("$.data.items[1].textColor").value("#111827"))
                .andExpect(jsonPath("$.data.items[3].backgroundColor").value("#E6F4EA"))
                .andExpect(jsonPath("$.data.items[0].imageUrl").value("/images/common/banner/slide-loan.svg"))
                .andExpect(jsonPath("$.data.items[0].altText").value("S2 이미지"))
                .andExpect(jsonPath("$.data.items[0].linkUrl").value("/product/1"));
    }

    @Test
    @DisplayName("정책1 제공대상: 계좌 보유 고객은 eligible=false, 배너 0건 (같은 영역에 계좌정보) / 미보유·비로그인은 노출")
    void accountHolderNotEligible() throws Exception {
        mvc.perform(get("/api/common/top-slide-banners").param("demoCustomer", "customer01"))
                .andExpect(jsonPath("$.data.eligible").value(false))
                .andExpect(jsonPath("$.data.audience").value("ACCOUNT_HOLDER"))
                .andExpect(jsonPath("$.data.items", hasSize(0)));
        mvc.perform(get("/api/common/top-slide-banners").param("demoCustomer", "customer02"))
                .andExpect(jsonPath("$.data.eligible").value(true))
                .andExpect(jsonPath("$.data.total").value(5));
    }

    @Test
    @DisplayName("운영 설정(demo-customer-param=false): 파라미터로 다른 고객 흉내 불가 → 비로그인으로 처리")
    void demoParamClosed() throws Exception {
        props.getTopSlide().setDemoCustomerParam(false);
        rebuild();
        mvc.perform(get("/api/common/top-slide-banners").param("demoCustomer", "customer01"))
                .andExpect(jsonPath("$.data.eligible").value(true));
    }

    @Test
    @DisplayName("구분 분리: 배너팝업(공통#39) API 에 상단슬라이드배너가 섞이지 않음")
    void separatedFromBannerPopup() throws Exception {
        mvc.perform(get("/api/common/banner-popups").param("channel", "APP"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("P1 배너팝업"));
    }

    @Test
    @DisplayName("배경색 위 글자 대비 4.5:1 이상 자동 선택 (어떤 색을 골라도 읽을 수 있음)")
    void readableTextColorContrast() {
        for (String bg : List.of("#000000", "#FFFFFF", "#0D47A1", "#E3ECFA", "#FF0000", "#777777", "#808080", "#F5A623", "#1B7A3D", "#E91E63", "#00A0E9")) {
            String fg = BannerSupport.readableTextColor(bg);
            assertThat(BannerSupport.contrastRatio(bg, fg)).as(bg + " on " + fg).isGreaterThanOrEqualTo(4.5);
        }
    }

    @Test
    @DisplayName("화면 계약: 최대 건수만큼만 슬롯 생성(빈 슬롯 숨김), 01/05 표기, 일시정지/재생, 2건 이상만 Indicator")
    void scriptContract() throws IOException {
        String js = resource("/static/js/common/top-slide-banner.js");
        assertThat(js).contains("this.items.map", "pad2(", "'/' + pad2(n)", "자동 넘김 일시정지", "자동 넘김 재생",
                "if (n > 1)", "visibilitychange", "prefers-reduced-motion", "aria-roledescription", "textContent");
        assertThat(js).doesNotContain("innerHTML");
    }

    private static String resource(String path) throws IOException {
        try (InputStream in = TopSlideBannerTest.class.getResourceAsStream(path)) {
            assertThat(in).as(path).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
