package com.example.demojsp.common.product;

import com.example.demojsp.common.banner.config.BannerPopupProperties;
import com.example.demojsp.common.banner.store.ConfigAccountHoldingChecker;
import com.example.demojsp.common.member.MemberProperties;
import com.example.demojsp.common.member.MemberSegmentResolver;
import com.example.demojsp.common.product.config.ProductProperties;
import com.example.demojsp.common.product.service.ProductService;
import com.example.demojsp.common.product.store.InMemoryProductRepository;
import com.example.demojsp.common.product.web.ProductApiController;
import com.example.demojsp.common.product.web.ProductApiExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [공통#47] 홈 추천 상품 테스트 (standalone MockMvc - DB 불필요). 금융회원(계좌 보유) = customer01.
 */
public class RecommendProductTest {

    public static ProductProperties.Seed seed(String code, String kind, String sub, String name, boolean onSale, boolean home, Integer order) {
        ProductProperties.Seed s = new ProductProperties.Seed();
        s.setCode(code); s.setKind(kind); s.setSubType(sub); s.setName(name); s.setSummary(name + " 설명");
        s.setRateText("연 3.0%"); s.setIcon(kind.equals("LOAN") ? "loan" : "savings");
        s.setDetailUrl("/product/" + code); s.setOnSale(onSale); s.setHomeDisplay(home); s.setHomeOrder(order);
        return s;
    }

    static ProductProperties props() {
        ProductProperties p = new ProductProperties();
        p.setAdminApiOpen(true);
        p.getSeed().add(seed("PS_SMART", "LOAN", "신용대출", "페퍼스마트대출", true, true, 1));
        p.getSeed().add(seed("PS_BIZ", "LOAN", "담보대출", "페퍼스 사업자담보대출", true, false, null));
        p.getSeed().add(seed("PS_PARKING4", "DEPOSIT", "입출금", "페퍼스 파킹통장4", true, true, 3));
        p.getSeed().add(seed("PS_2030", "DEPOSIT", "적금", "페퍼스 2030적금", true, true, 2));
        p.getSeed().add(seed("PS_AUTO", "LOAN", "오토대출", "판매중지", false, true, 1));
        return p;
    }

    public static MemberSegmentResolver resolver() {
        BannerPopupProperties bp = new BannerPopupProperties();
        bp.getTopSlide().setAccountHolders(List.of("customer01"));
        MemberProperties mp = new MemberProperties();
        mp.setDemoCustomerParam(true);
        return new MemberSegmentResolver(mp, new ConfigAccountHoldingChecker(bp));
    }

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        ProductService service = new ProductService(new InMemoryProductRepository(), props());
        mvc = MockMvcBuilders.standaloneSetup(new ProductApiController(service, resolver()))
                .setControllerAdvice(new ProductApiExceptionHandler()).build();
    }

    @Test
    @DisplayName("기능1 + 정책2: 홈 표시여부 Y 상품을 홈 표시순서대로 (N·판매중지 제외), 여신·수신·입출금 혼합")
    void listInHomeOrder() throws Exception {
        mvc.perform(get("/api/common/products/recommend").param("demoCustomer", "customer02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eligible").value(true))
                .andExpect(jsonPath("$.data.segment").value("MEMBER"))
                .andExpect(jsonPath("$.data.items", hasSize(3)))
                .andExpect(jsonPath("$.data.items[*].name").value(contains("페퍼스마트대출", "페퍼스 2030적금", "페퍼스 파킹통장4")))
                .andExpect(jsonPath("$.data.items[*].kindName").value(contains("여신", "수신", "수신")))
                .andExpect(jsonPath("$.data.items[2].subType").value("입출금"));
    }

    @Test
    @DisplayName("기능2·3: 더보기 = 전체상품목록(상품몰), 상품 터치 = 상품안내페이지 / 카드 내용 = 상품 원장")
    void links() throws Exception {
        mvc.perform(get("/api/common/products/recommend"))
                .andExpect(jsonPath("$.data.segment").value("GUEST"))
                .andExpect(jsonPath("$.data.moreUrl").value("/product/all"))
                .andExpect(jsonPath("$.data.items[0].detailUrl").value("/product/PS_SMART"))
                .andExpect(jsonPath("$.data.items[0].summary").value("페퍼스마트대출 설명"))
                .andExpect(jsonPath("$.data.items[0].rateText").value("연 3.0%"));
    }

    @Test
    @DisplayName("정책1·기능4: 금융회원은 eligible=false, 상품 0건 (영역 전체 숨김)")
    void financialMemberHidden() throws Exception {
        mvc.perform(get("/api/common/products/recommend").param("demoCustomer", "customer01"))
                .andExpect(jsonPath("$.data.eligible").value(false))
                .andExpect(jsonPath("$.data.segment").value("FINANCIAL"))
                .andExpect(jsonPath("$.data.items", hasSize(0)));
    }

    @Test
    @DisplayName("화면 계약: 영역 숨김 처리, 더보기·상품 링크, XSS 안전")
    void scriptContract() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/static/js/common/recommend-products.js")) {
            assertThat(in).isNotNull();
            String js = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(js).contains("!data.eligible", "host.hidden = true", "data.moreUrl", "p.detailUrl", "textContent")
                    .doesNotContain("innerHTML");
        }
    }
}
