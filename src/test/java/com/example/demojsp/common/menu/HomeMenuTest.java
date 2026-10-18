package com.example.demojsp.common.menu;

import com.example.demojsp.common.banner.config.BannerPopupProperties;
import com.example.demojsp.common.banner.store.ConfigAccountHoldingChecker;
import com.example.demojsp.common.member.MemberProperties;
import com.example.demojsp.common.member.MemberSegmentResolver;
import com.example.demojsp.common.menu.config.MenuProperties;
import com.example.demojsp.common.menu.service.BankingMenuService;
import com.example.demojsp.common.menu.service.QuickMenuService;
import com.example.demojsp.common.menu.store.InMemoryBankingMenuRepository;
import com.example.demojsp.common.menu.web.BankingMenuAdminApiController;
import com.example.demojsp.common.menu.web.MenuApiController;
import com.example.demojsp.common.menu.web.MenuApiExceptionHandler;
import com.example.demojsp.common.menu.web.MenuRequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [공통#46] 홈 퀵메뉴 · 전체메뉴 · ADMIN 뱅킹메뉴목록 테스트 (standalone MockMvc - DB 불필요).
 * 금융회원(계좌 보유): customer01.
 */
class HomeMenuTest {

    private MenuProperties props;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        props = new MenuProperties();
        props.setAdminApiOpen(true);
        String[][] q = {
                {"ALL_PRODUCTS", "전체상품", "products", "/product/all", "N"},
                {"ACCOUNT_OPEN", "계좌개설", "account", "/account/open", "N"},
                {"LOAN_APPLY", "대출신청", "loan", "/loan/apply", "N"},
                {"OPEN_BANKING", "오픈뱅킹", "open-banking", "/openbanking", "Y"},
                {"E_SIGN", "전자서명", "contract", "/esign", "Y"},
                {"DOC_SUBMIT", "서류제출", "document", "/document/submit", "Y"},
                {"EXTRA", "7번째", "event", "/extra", "N"}};
        for (String[] r : q) {
            MenuProperties.QuickItem i = new MenuProperties.QuickItem();
            i.setCode(r[0]); i.setName(r[1]); i.setIcon(r[2]); i.setUrl(r[3]); i.setRequiresLogin("Y".equals(r[4]));
            props.getQuick().getItems().add(i);
        }
        props.getQuick().getLayouts().put("DEFAULT", List.of("ALL_PRODUCTS", "ACCOUNT_OPEN", "LOAN_APPLY", "OPEN_BANKING", "E_SIGN", "DOC_SUBMIT", "EXTRA"));
        props.getQuick().getLayouts().put("FINANCIAL", List.of("OPEN_BANKING", "ALL_PRODUCTS", "ACCOUNT_OPEN", "LOAN_APPLY", "E_SIGN", "DOC_SUBMIT"));
        String[][] b = {
                {"PRODUCT", "상품", "예금상품", "/product/deposit", "1"},
                {"PRODUCT", "상품", "대출상품", "/product/loan", "2"},
                {"PRODUCT", "상품", "금융계산기", "/product/calculator", "3"},
                {"MY", "내계좌", "계좌조회", "/account/list", "1"}};
        for (String[] r : b) {
            MenuProperties.BankingSeed s = new MenuProperties.BankingSeed();
            s.setCategory(r[0]); s.setCategoryName(r[1]); s.setName(r[2]); s.setUrl(r[3]); s.setDisplayOrder(Integer.parseInt(r[4]));
            props.getBankingSeed().add(s);
        }
        BannerPopupProperties bp = new BannerPopupProperties();
        bp.getTopSlide().setAccountHolders(List.of("customer01"));
        MemberProperties mp = new MemberProperties();
        mp.setDemoCustomerParam(true);
        MemberSegmentResolver resolver = new MemberSegmentResolver(mp, new ConfigAccountHoldingChecker(bp));
        BankingMenuService banking = new BankingMenuService(new InMemoryBankingMenuRepository(), props);
        mvc = MockMvcBuilders.standaloneSetup(
                        new MenuApiController(new QuickMenuService(props), banking, resolver),
                        new BankingMenuAdminApiController(banking, new MenuRequestContext(props)))
                .setControllerAdvice(new MenuApiExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("기능1·2·3: 6개만 3*2 (7번째 제외), 이미지(아이콘)+메뉴명, 하드코딩 6종 순서")
    void sixMenusThreeByTwo() throws Exception {
        mvc.perform(get("/api/common/menus/quick").param("demoCustomer", "customer02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.segment").value("MEMBER"))
                .andExpect(jsonPath("$.data.columns").value(3))
                .andExpect(jsonPath("$.data.rows").value(2))
                .andExpect(jsonPath("$.data.items", hasSize(6)))
                .andExpect(jsonPath("$.data.items[*].name").value(contains("전체상품", "계좌개설", "대출신청", "오픈뱅킹", "전자서명", "서류제출")))
                .andExpect(jsonPath("$.data.items[0].icon").value("products"))
                .andExpect(jsonPath("$.data.items[3].url").value("/openbanking"))
                .andExpect(jsonPath("$.data.items[3].loginFirst").value(false));
    }

    @Test
    @DisplayName("정책1·2: 비로그인도 제공, 로그인 필요 메뉴는 로그인(returnUrl) 경유 / 금융회원은 구분별 배치")
    void personalizedBySegment() throws Exception {
        mvc.perform(get("/api/common/menus/quick"))
                .andExpect(jsonPath("$.data.segment").value("GUEST"))
                .andExpect(jsonPath("$.data.items[0].url").value("/product/all"))
                .andExpect(jsonPath("$.data.items[0].loginFirst").value(false))
                .andExpect(jsonPath("$.data.items[4].url").value("/login?returnUrl=%2Fesign"))
                .andExpect(jsonPath("$.data.items[4].loginFirst").value(true));
        mvc.perform(get("/api/common/menus/quick").param("demoCustomer", "customer01"))
                .andExpect(jsonPath("$.data.segment").value("FINANCIAL"))
                .andExpect(jsonPath("$.data.segmentName").value("금융회원"))
                .andExpect(jsonPath("$.data.items[0].name").value("오픈뱅킹"));
    }

    @Test
    @DisplayName("기능4·5: ADMIN 전체상품 신규등록(표시순서 1) → 나머지 밀림 → 전체메뉴 > 상품 = 전체상품, 예금상품, 대출상품, 금융계산기")
    void registerAllProductsMenu() throws Exception {
        mvc.perform(get("/api/common/menus/all"))
                .andExpect(jsonPath("$.data.categories[0].menus[*].name").value(contains("예금상품", "대출상품", "금융계산기")));
        mvc.perform(post("/api/common/menu-admin/banking-menus").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"PRODUCT\",\"name\":\"전체상품\",\"url\":\"/product/all\",\"displayOrder\":1,\"newBadge\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayOrder").value(1));
        mvc.perform(get("/api/common/menu-admin/banking-menus").param("category", "PRODUCT"))
                .andExpect(jsonPath("$.data[*].name").value(contains("전체상품", "예금상품", "대출상품", "금융계산기")))
                .andExpect(jsonPath("$.data[*].displayOrder").value(contains(1, 2, 3, 4)));
        mvc.perform(get("/api/common/menus/all"))
                .andExpect(jsonPath("$.data.categories[0].name").value("상품"))
                .andExpect(jsonPath("$.data.categories[0].menus[*].name").value(contains("전체상품", "예금상품", "대출상품", "금융계산기")))
                .andExpect(jsonPath("$.data.categories[0].menus[0].newBadge").value(true));
    }

    @Test
    @DisplayName("기능5 표시순서 수정: 위/아래 이동(reorder) · 수정 시 끼워넣기 · 미사용은 전체메뉴 제외")
    void reorderAndUpdate() throws Exception {
        String body = mvc.perform(get("/api/common/menu-admin/banking-menus").param("category", "PRODUCT"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<Long> ids = java.util.regex.Pattern.compile("\"id\":(\\d+)").matcher(body).results().map(m -> Long.parseLong(m.group(1))).toList();
        mvc.perform(post("/api/common/menu-admin/banking-menus/reorder").param("category", "PRODUCT")
                        .contentType(MediaType.APPLICATION_JSON).content("[" + ids.get(2) + "," + ids.get(0) + "," + ids.get(1) + "]"))
                .andExpect(jsonPath("$.data[*].name").value(contains("금융계산기", "예금상품", "대출상품")));
        mvc.perform(put("/api/common/menu-admin/banking-menus/" + ids.get(2)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"PRODUCT\",\"name\":\"금융계산기\",\"url\":\"/product/calculator\",\"displayOrder\":9,\"use\":false}"))
                .andExpect(jsonPath("$.data.displayOrder").value(3));
        mvc.perform(get("/api/common/menus/all"))
                .andExpect(jsonPath("$.data.categories[0].menus[*].name").value(contains("예금상품", "대출상품")));
        // 일부 ID 만 보내면 거부
        mvc.perform(post("/api/common/menu-admin/banking-menus/reorder").param("category", "PRODUCT")
                        .contentType(MediaType.APPLICATION_JSON).content("[" + ids.get(0) + "]"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("MNU-105"));
    }

    @Test
    @DisplayName("ADMIN 검증: 같은 이름 MNU-106, 외부/스크립트 URL MNU-103, 없는 카테고리 MNU-104, 운영 설정 MNU-401")
    void adminValidation() throws Exception {
        mvc.perform(post("/api/common/menu-admin/banking-menus").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"PRODUCT\",\"name\":\"예금상품\",\"url\":\"/x\",\"displayOrder\":1}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MNU-106"));
        for (String url : new String[]{"javascript:alert(1)", "//evil.example.com", "https://evil.example.com"}) {
            mvc.perform(post("/api/common/menu-admin/banking-menus").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"category\":\"PRODUCT\",\"name\":\"새메뉴\",\"url\":\"" + url + "\",\"displayOrder\":1}"))
                    .andExpect(jsonPath("$.code").value("MNU-103"));
        }
        mvc.perform(post("/api/common/menu-admin/banking-menus").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"NOPE\",\"name\":\"새메뉴\",\"url\":\"/x\",\"displayOrder\":1}"))
                .andExpect(jsonPath("$.code").value("MNU-104"));
        props.setAdminApiOpen(false);
        mvc.perform(get("/api/common/menu-admin/banking-menus"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("MNU-401"));
    }

    @Test
    @DisplayName("화면 계약: 퀵메뉴 아이콘이 공통 스프라이트에 존재(products·open-banking 추가), 링크·로그인 필요 낭독")
    void contract() throws IOException {
        String svg = resource("/static/images/common/ui-icons.svg");
        for (String icon : List.of("products", "account", "loan", "open-banking", "contract", "document")) {
            assertThat(svg).contains("id=\"ic-" + icon + "\"");
        }
        String js = resource("/static/js/common/quick-menu.js");
        assertThat(js).contains("(로그인 필요)", "aria-label', '퀵메뉴'", "--qm-cols", "textContent").doesNotContain("innerHTML");
    }

    private static String resource(String path) throws IOException {
        try (InputStream in = HomeMenuTest.class.getResourceAsStream(path)) {
            assertThat(in).as(path).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
