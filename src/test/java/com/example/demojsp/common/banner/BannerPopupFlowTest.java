package com.example.demojsp.common.banner;

import com.example.demojsp.common.banner.config.BannerPopupProperties;
import com.example.demojsp.common.banner.service.BannerPopupService;
import com.example.demojsp.common.banner.store.BannerImageStore;
import com.example.demojsp.common.banner.store.InMemoryBannerPopupRepository;
import com.example.demojsp.common.banner.web.BannerApiExceptionHandler;
import com.example.demojsp.common.banner.web.BannerPopupAdminApiController;
import com.example.demojsp.common.banner.web.BannerPopupApiController;
import com.example.demojsp.common.banner.web.BannerRequestContext;
import com.example.demojsp.common.banner.service.BannerSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [CF-BC-20 / 공통#39] 배너팝업 복수 노출 흐름 테스트 (standalone MockMvc - DB 불필요).
 *
 * <p>기준시각 고정: 2026-10-20 10:00 (Asia/Seoul).
 * seed: A(순서2) · B(순서1) · C(순서3) 기간 겹침 / D 게시예정 / E 종료 / F PC_WEB 전용 / G 미사용
 */
class BannerPopupFlowTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    /** 2026-10-20 10:00 KST */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-20T01:00:00Z"), KST);

    private BannerPopupProperties props;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        props = new BannerPopupProperties();
        props.setAdminApiOpen(true);
        props.getSeed().add(seed("A 일반 긴급공지", "EMERGENCY_GENERAL", "2026-10-19 00:00", "2026-10-25 00:00", 2, true, "APP"));
        props.getSeed().add(seed("B 시스템 긴급공지", "EMERGENCY_SYSTEM", "2026-10-20 00:00", "2026-10-23 00:00", 1, true, "APP"));
        props.getSeed().add(seed("C 이벤트", "EVENT", "2026-10-17 00:00", "2026-10-30 00:00", 3, true, "APP"));
        props.getSeed().add(seed("D 게시예정", "NOTICE", "2026-10-23 00:00", "2026-10-30 00:00", 1, true, "APP"));
        props.getSeed().add(seed("E 게시종료", "NOTICE", "2026-10-10 00:00", "2026-10-18 00:00", 1, true, "APP"));
        props.getSeed().add(seed("F PC전용", "NOTICE", "2026-10-19 00:00", "2026-10-27 00:00", 1, true, "PC_WEB"));
        props.getSeed().add(seed("G 미사용", "EVENT", "2026-10-19 00:00", "2026-10-27 00:00", 4, false, "APP"));
        rebuild();
    }

    private void rebuild() {
        BannerPopupService service = new BannerPopupService(new InMemoryBannerPopupRepository(), props, CLOCK);
        BannerRequestContext context = new BannerRequestContext(props);
        mvc = MockMvcBuilders.standaloneSetup(
                        new BannerPopupApiController(service),
                        new BannerPopupAdminApiController(service, context, new BannerImageStore(props)))
                .setControllerAdvice(new BannerApiExceptionHandler())
                .build();
    }

    private static BannerPopupProperties.Seed seed(String title, String category, String start, String end,
                                                   int order, boolean use, String channel) {
        BannerPopupProperties.Seed s = new BannerPopupProperties.Seed();
        s.setTitle(title);
        s.setCategory(category);
        s.setContent(title + " 내용");
        s.setStartAt(start);
        s.setEndAt(end);
        s.setDisplayOrder(order);
        s.setUse(use);
        s.setChannels(List.of(channel));
        return s;
    }

    private static String body(String title, String start, String end, Integer order) {
        return "{\"category\":\"NOTICE\",\"channels\":[\"APP\"],\"title\":\"" + title + "\",\"content\":\"본문\","
                + "\"startAt\":\"" + start + "\",\"endAt\":\"" + end + "\",\"displayOrder\":" + order + "}";
    }

    @Test
    @DisplayName("요구사항1: 게시일자가 겹치는 배너 3건 모두 노출(AS-IS 1건) + 표시순서 1,2,3 정렬 + slide=true")
    void overlappingBannersAllShownInDisplayOrder() throws Exception {
        mvc.perform(get("/api/common/banner-popups").param("channel", "APP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.slide").value(true))
                .andExpect(jsonPath("$.data.baseAt").value("2026-10-20 10:00:00"))
                .andExpect(jsonPath("$.data.items[*].title").value(contains("B 시스템 긴급공지", "A 일반 긴급공지", "C 이벤트")))
                .andExpect(jsonPath("$.data.items[*].displayOrder").value(contains(1, 2, 3)))
                .andExpect(jsonPath("$.data.items[0].categoryName").value("시스템 긴급공지"))
                // 관리 정보는 고객 응답에 없음
                .andExpect(jsonPath("$.data.items[0].registeredBy").doesNotExist());
    }

    @Test
    @DisplayName("채널 분리 - PC_WEB 은 PC 전용 1건만, 1건이면 slide=false / 잘못된 채널 BNR-001")
    void channelFilter() throws Exception {
        mvc.perform(get("/api/common/banner-popups").param("channel", "pc_web"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.slide").value(false))
                .andExpect(jsonPath("$.data.items[0].title").value("F PC전용"));
        mvc.perform(get("/api/common/banner-popups").param("channel", "WATCH"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BNR-001"));
    }

    @Test
    @DisplayName("게시기간 경계 - 시작 포함, 종료 미포함 (미리보기 기준시각)")
    void periodBoundary() throws Exception {
        // 10-23 00:00 → B 종료(미포함), D 시작(포함) → A(2), D(1), C(3) → D, A, C
        mvc.perform(get("/api/common/banner-admin/preview").param("channel", "APP").param("at", "2026-10-23 00:00"))
                .andExpect(jsonPath("$.data.items[*].title").value(contains("D 게시예정", "A 일반 긴급공지", "C 이벤트")));
        // 10-22 23:59 → B 아직 노출
        mvc.perform(get("/api/common/banner-admin/preview").param("channel", "APP").param("at", "2026-10-22T23:59"))
                .andExpect(jsonPath("$.data.items[0].title").value("B 시스템 긴급공지"));
        // 겹치는 배너 없음
        mvc.perform(get("/api/common/banner-admin/preview").param("channel", "APP").param("at", "2026-11-01 00:00"))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.items", hasSize(0)));
        mvc.perform(get("/api/common/banner-admin/preview").param("at", "2026/10/20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BNR-002"));
    }

    @Test
    @DisplayName("표시순서 동일 → 게시 시작일시 최신 우선 / ADMIN 목록에 겹침·순서중복 표시")
    void sameOrderTieBreakAndAdminFlags() throws Exception {
        mvc.perform(post("/api/common/banner-admin/popups").contentType(MediaType.APPLICATION_JSON)
                        .content(body("H 신규 순서1", "2026-10-20T09:00", "2026-10-21T00:00", 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("게시중"))
                .andExpect(jsonPath("$.data.registeredBy").value("LOCAL_TEST"))
                .andExpect(jsonPath("$.data.overlapCount").value(3))
                .andExpect(jsonPath("$.data.orderConflict").value(true));

        // B(순서1, 10-20 00:00 시작) 와 H(순서1, 10-20 09:00 시작) → H 먼저
        mvc.perform(get("/api/common/banner-popups"))
                .andExpect(jsonPath("$.data.items[*].title").value(contains("H 신규 순서1", "B 시스템 긴급공지", "A 일반 긴급공지", "C 이벤트")));

        mvc.perform(get("/api/common/banner-admin/popups"))
                .andExpect(jsonPath("$.data", hasSize(8)))
                .andExpect(jsonPath("$.data[0].status").value("게시중"))
                .andExpect(jsonPath("$.data[?(@.title=='E 게시종료')].status").value("게시종료"))
                .andExpect(jsonPath("$.data[?(@.title=='D 게시예정')].status").value("게시예정"))
                .andExpect(jsonPath("$.data[?(@.title=='G 미사용')].status").value("미사용"))
                .andExpect(jsonPath("$.data[?(@.title=='F PC전용')].overlapCount").value(0));
    }

    @Test
    @DisplayName("ADMIN 수정·사용여부·삭제가 고객 노출에 즉시 반영")
    void adminChangesReflected() throws Exception {
        long bId = idOf("B 시스템 긴급공지");
        // B 표시순서 1 → 9 로 수정 → A, C, B
        mvc.perform(put("/api/common/banner-admin/popups/" + bId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"EMERGENCY_SYSTEM\",\"channels\":[\"APP\"],\"title\":\"B 시스템 긴급공지\",\"content\":\"수정\","
                                + "\"startAt\":\"2026-10-20 00:00\",\"endAt\":\"2026-10-23 00:00\",\"displayOrder\":9}"))
                .andExpect(jsonPath("$.data.displayOrder").value(9))
                .andExpect(jsonPath("$.data.registeredBy").value("SYSTEM"))
                .andExpect(jsonPath("$.data.updatedBy").value("LOCAL_TEST"));
        mvc.perform(get("/api/common/banner-popups"))
                .andExpect(jsonPath("$.data.items[*].title").value(contains("A 일반 긴급공지", "C 이벤트", "B 시스템 긴급공지")));

        long aId = idOf("A 일반 긴급공지");
        mvc.perform(post("/api/common/banner-admin/popups/" + aId + "/use").param("use", "false"))
                .andExpect(jsonPath("$.data.status").value("미사용"));
        mvc.perform(delete("/api/common/banner-admin/popups/" + bId)).andExpect(status().isOk());
        mvc.perform(get("/api/common/banner-popups"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.slide").value(false));
        mvc.perform(delete("/api/common/banner-admin/popups/" + bId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BNR-003"));
    }

    @Test
    @DisplayName("ADMIN 서버 검증 - 기간 역전, 표시순서 범위, 필수값, URL, 대체텍스트, 잘못된 JSON")
    void validation() throws Exception {
        expect(body("기간역전", "2026-10-21 00:00", "2026-10-20 00:00", 1), "BNR-102");
        expect(body("기간같음", "2026-10-21 00:00", "2026-10-21 00:00", 1), "BNR-102");
        expect(body("순서0", "2026-10-20 00:00", "2026-10-21 00:00", 0), "BNR-103");
        expect(body("순서없음", "2026-10-20 00:00", "2026-10-21 00:00", null), "BNR-101");
        expect(body("", "2026-10-20 00:00", "2026-10-21 00:00", 1), "BNR-101");
        expect(body("x".repeat(51), "2026-10-20 00:00", "2026-10-21 00:00", 1), "BNR-106");
        expect(body("날짜형식", "20261020", "2026-10-21 00:00", 1), "BNR-002");
        expect("{\"category\":\"NOTICE\",\"channels\":[],\"title\":\"t\",\"content\":\"c\",\"startAt\":\"2026-10-20 00:00\",\"endAt\":\"2026-10-21 00:00\",\"displayOrder\":1}", "BNR-101");
        expect("{\"category\":\"WHAT\",\"channels\":[\"APP\"],\"title\":\"t\",\"content\":\"c\",\"startAt\":\"2026-10-20 00:00\",\"endAt\":\"2026-10-21 00:00\",\"displayOrder\":1}", "BNR-107");
        expect("{\"category\":\"NOTICE\",\"channels\":[\"APP\"],\"title\":\"t\",\"content\":\"c\",\"linkUrl\":\"javascript:alert(1)\",\"startAt\":\"2026-10-20 00:00\",\"endAt\":\"2026-10-21 00:00\",\"displayOrder\":1}", "BNR-104");
        expect("{\"category\":\"NOTICE\",\"channels\":[\"APP\"],\"title\":\"t\",\"content\":\"c\",\"linkUrl\":\"//evil.com\",\"startAt\":\"2026-10-20 00:00\",\"endAt\":\"2026-10-21 00:00\",\"displayOrder\":1}", "BNR-104");
        expect("{\"category\":\"NOTICE\",\"channels\":[\"APP\"],\"title\":\"t\",\"content\":\"c\",\"imageUrl\":\"/images/a.png\",\"startAt\":\"2026-10-20 00:00\",\"endAt\":\"2026-10-21 00:00\",\"displayOrder\":1}", "BNR-105");
        expect("{not json", "BNR-004");
    }

    @Test
    @DisplayName("최대 노출 건수 초과 시 표시순서 상위 N건만 + truncated")
    void maxDisplayCount() throws Exception {
        props.setMaxDisplayCount(2);
        rebuild();
        mvc.perform(get("/api/common/banner-popups"))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.truncated").value(1))
                .andExpect(jsonPath("$.data.items[*].displayOrder").value(contains(1, 2)));
    }

    @Test
    @DisplayName("ADMIN 권한 - admin-api-open=false 이고 비로그인이면 BNR-401, 고객 API 는 영향 없음")
    void adminOnly() throws Exception {
        props.setAdminApiOpen(false);
        rebuild();
        mvc.perform(get("/api/common/banner-admin/popups"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("BNR-401"));
        mvc.perform(get("/api/common/banner-popups")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("URL 허용 규칙 - 상대경로/http(s)만")
    void safeUrl() {
        assertThat(BannerSupport.isSafeUrl("/board/list?id=1")).isTrue();
        assertThat(BannerSupport.isSafeUrl("https://www.pepperbank.kr/event")).isTrue();
        assertThat(BannerSupport.isSafeUrl("javascript:alert(1)")).isFalse();
        assertThat(BannerSupport.isSafeUrl("//evil.com/x")).isFalse();
        assertThat(BannerSupport.isSafeUrl("/a\"onmouseover=x")).isFalse();
        assertThat(BannerSupport.isSafeUrl("data:text/html,x")).isFalse();
    }

    private void expect(String json, String code) throws Exception {
        mvc.perform(post("/api/common/banner-admin/popups").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").value(code));
    }

    private long idOf(String title) throws Exception {
        String json = mvc.perform(get("/api/common/banner-admin/popups")).andReturn().getResponse().getContentAsString();
        int t = json.indexOf("\"title\":\"" + title + "\"");
        int idPos = json.lastIndexOf("\"id\":", t);
        String num = json.substring(idPos + 5, json.indexOf(',', idPos));
        return Long.parseLong(num.trim());
    }
}
