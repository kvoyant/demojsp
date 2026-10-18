package com.example.demojsp.common.savings;

import com.example.demojsp.common.product.RecommendProductTest;
import com.example.demojsp.common.product.config.ProductProperties;
import com.example.demojsp.common.product.service.ProductAdminService;
import com.example.demojsp.common.product.service.ProductService;
import com.example.demojsp.common.product.store.InMemoryProductRepository;
import com.example.demojsp.common.product.web.ProductAdminApiController;
import com.example.demojsp.common.product.web.ProductRequestContext;
import com.example.demojsp.common.savings.config.SavingsProperties;
import com.example.demojsp.common.savings.service.SavingsMainService;
import com.example.demojsp.common.savings.web.SavingsApiController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [공통#51] 저축 - 메인 콘텐츠 테스트. 계좌 보유(금융회원) = customer01.
 */
class SavingsMainTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        ProductProperties pp = new ProductProperties();
        pp.setAdminApiOpen(true);
        pp.getSeed().add(RecommendProductTest.seed("PS_PARKING4", "DEPOSIT", "입출금", "페퍼스 파킹통장4", true, true, 1));
        pp.getSeed().add(RecommendProductTest.seed("PS_2030", "DEPOSIT", "적금", "페퍼스 2030적금", true, false, null));
        InMemoryProductRepository repo = new InMemoryProductRepository();
        new ProductService(repo, pp);
        SavingsProperties sp = new SavingsProperties();
        mvc = MockMvcBuilders.standaloneSetup(
                        new SavingsApiController(new SavingsMainService(sp, repo), RecommendProductTest.resolver()),
                        new ProductAdminApiController(new ProductAdminService(repo, pp), new ProductRequestContext(pp)))
                .build();
    }

    @Test
    @DisplayName("기능1·2·3: 텍스트(매일 쌓이는 이자 / 모으는 재미가 쏠쏠) + 이미지 + [계좌 개설하기] → 입출금통장 상품안내페이지")
    void contentForNoAccount() throws Exception {
        mvc.perform(get("/api/common/savings/main").param("demoCustomer", "customer02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eligible").value(true))
                .andExpect(jsonPath("$.data.lines").value(contains("매일 쌓이는 이자", "모으는 재미가 쏠쏠")))
                .andExpect(jsonPath("$.data.imageUrl").value("/images/common/savings/main-coins.svg"))
                .andExpect(jsonPath("$.data.imageAlt").value("통장에 동전이 쌓이는 그림"))
                .andExpect(jsonPath("$.data.ctaText").value("계좌 개설하기"))
                .andExpect(jsonPath("$.data.ctaUrl").value("/product/PS_PARKING4"))
                .andExpect(jsonPath("$.data.productName").value("페퍼스 파킹통장4"));
    }

    @Test
    @DisplayName("정책1·2: 계좌 미보유 모든 사용자 - 로그인 전(GUEST)·후(MEMBER) 같은 화면 / 계좌 보유는 미노출")
    void sameBeforeAfterLoginAndHiddenForHolder() throws Exception {
        String guest = mvc.perform(get("/api/common/savings/main")).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String member = mvc.perform(get("/api/common/savings/main").param("demoCustomer", "customer02"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(guest.replace("\"GUEST\"", "X")).isEqualTo(member.replace("\"MEMBER\"", "X"));
        mvc.perform(get("/api/common/savings/main").param("demoCustomer", "customer01"))
                .andExpect(jsonPath("$.data.eligible").value(false))
                .andExpect(jsonPath("$.data.segment").value("FINANCIAL"))
                .andExpect(jsonPath("$.data.lines", hasSize(0)))
                .andExpect(jsonPath("$.data.ctaUrl").doesNotExist());
    }

    @Test
    @DisplayName("기능3 연동: ADMIN 이 입출금통장 URL 수정 → 버튼 URL 반영 / 판매중지 → 대체 URL")
    void ctaFollowsProductMaster() throws Exception {
        String body = mvc.perform(get("/api/common/product-admin/products")).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        var m = Pattern.compile("\"id\":(\\d+),\"code\":\"PS_PARKING4\"").matcher(body);
        assertThat(m.find()).isTrue();
        String id = m.group(1);
        mvc.perform(put("/api/common/product-admin/products/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"페퍼스 파킹통장4\",\"detailUrl\":\"/product/deposit/parking4-new\",\"onSale\":true}")).andExpect(status().isOk());
        mvc.perform(get("/api/common/savings/main")).andExpect(jsonPath("$.data.ctaUrl").value("/product/deposit/parking4-new"));
        mvc.perform(put("/api/common/product-admin/products/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"페퍼스 파킹통장4\",\"detailUrl\":\"/product/deposit/parking4-new\",\"onSale\":false}")).andExpect(status().isOk());
        mvc.perform(get("/api/common/savings/main"))
                .andExpect(jsonPath("$.data.ctaUrl").value("/product/deposit"))
                .andExpect(jsonPath("$.data.productName").doesNotExist());
    }

    @Test
    @DisplayName("화면 계약: 하단 상시 버튼(ui-cta-bar, 본문 밖 자리 지원), press·이동 중(UiButton.navigate), 계좌 보유 시 숨김")
    void scriptContract() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/static/js/common/savings-main.js")) {
            assertThat(in).isNotNull();
            String js = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(js).contains("ui-cta-bar", "opts.cta", "UiButton.navigate", "!d.eligible", "onIneligible", "textContent")
                    .doesNotContain("innerHTML");
        }
    }
}
