package com.example.demojsp.common.product;

import com.example.demojsp.common.product.config.ProductProperties;
import com.example.demojsp.common.product.service.ProductAdminService;
import com.example.demojsp.common.product.service.ProductService;
import com.example.demojsp.common.product.store.InMemoryProductRepository;
import com.example.demojsp.common.product.web.ProductAdminApiController;
import com.example.demojsp.common.product.web.ProductApiController;
import com.example.demojsp.common.product.web.ProductApiExceptionHandler;
import com.example.demojsp.common.product.web.ProductRequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [공통#48] ADMIN 수신/여신상품목록 홈 표시여부·순서 테스트 (standalone MockMvc - DB 불필요).
 *
 * <p>seed: 여신 PS_SMART(Y,1) · PS_BIZ(N) / 수신 PS_2030(Y,2) · PS_PARKING4(N) · PS_TIME(N)
 */
class ProductAdminTest {

    private ProductProperties props;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        props = new ProductProperties();
        props.setAdminApiOpen(true);
        props.getSeed().add(RecommendProductTest.seed("PS_SMART", "LOAN", "신용대출", "페퍼스마트대출", true, true, 1));
        props.getSeed().add(RecommendProductTest.seed("PS_BIZ", "LOAN", "담보대출", "페퍼스 사업자담보대출", true, false, null));
        props.getSeed().add(RecommendProductTest.seed("PS_2030", "DEPOSIT", "적금", "페퍼스 2030적금", true, true, 2));
        props.getSeed().add(RecommendProductTest.seed("PS_PARKING4", "DEPOSIT", "입출금", "페퍼스 파킹통장4", true, false, null));
        props.getSeed().add(RecommendProductTest.seed("PS_TIME", "DEPOSIT", "정기예금", "페퍼스 정기예금", true, false, null));
        InMemoryProductRepository repo = new InMemoryProductRepository();
        ProductService service = new ProductService(repo, props);
        mvc = MockMvcBuilders.standaloneSetup(
                        new ProductApiController(service, RecommendProductTest.resolver()),
                        new ProductAdminApiController(new ProductAdminService(repo, props), new ProductRequestContext(props)))
                .setControllerAdvice(new ProductApiExceptionHandler()).build();
    }

    private long id(String code) throws Exception {
        String body = mvc.perform(get("/api/common/product-admin/products")).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        var m = Pattern.compile("\"id\":(\\d+),\"code\":\"" + code + "\"").matcher(body);
        if (!m.find()) throw new IllegalStateException(code);
        return Long.parseLong(m.group(1));
    }

    private ResultActions home(String code, String yn, Integer order) throws Exception {
        return mvc.perform(put("/api/common/product-admin/products/" + id(code) + "/home").contentType(MediaType.APPLICATION_JSON)
                .content("{\"homeDisplay\":\"" + yn + "\",\"homeOrder\":" + order + "}"));
    }

    @Test
    @DisplayName("기능1: 수신상품목록 / 여신상품목록 분리 조회 + 상품설정 컬럼(홈 표시여부·순서)")
    void listsWithSettingColumns() throws Exception {
        mvc.perform(get("/api/common/product-admin/products").param("kind", "DEPOSIT"))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[*].kind", everyItem(is("DEPOSIT"))))
                .andExpect(jsonPath("$.data[0].homeDisplay").value(true))
                .andExpect(jsonPath("$.data[0].homeOrder").value(2));
        mvc.perform(get("/api/common/product-admin/products").param("kind", "LOAN"))
                .andExpect(jsonPath("$.data.length()").value(2));
        mvc.perform(get("/api/common/product-admin/home-status"))
                .andExpect(jsonPath("$.data.maxCount").value(3))
                .andExpect(jsonPath("$.data.count").value(2))
                .andExpect(jsonPath("$.data.slots[0].name").value("페퍼스마트대출"))
                .andExpect(jsonPath("$.data.slots[2].productId").doesNotExist());
    }

    @Test
    @DisplayName("기능2: 홈 표시여부 Y → 추천 영역 표시, N → 미표시 (고객 API 즉시 반영)")
    void yAndNReflected() throws Exception {
        home("PS_PARKING4", "Y", 3).andExpect(status().isOk()).andExpect(jsonPath("$.data.homeOrder").value(3));
        mvc.perform(get("/api/common/products/recommend"))
                .andExpect(jsonPath("$.data.items[*].name").value(contains("페퍼스마트대출", "페퍼스 2030적금", "페퍼스 파킹통장4")));
        home("PS_SMART", "N", null).andExpect(jsonPath("$.data.homeDisplay").value(false)).andExpect(jsonPath("$.data.homeOrder").doesNotExist());
        mvc.perform(get("/api/common/products/recommend"))
                .andExpect(jsonPath("$.data.items[*].name").value(contains("페퍼스 2030적금", "페퍼스 파킹통장4")));
    }

    @Test
    @DisplayName("기능2: 수신+여신 합산 3개 초과 Y → 등록 불가 \"등록 건수 3개를 초과하였습니다.\" / 이미 Y 인 상품 순서 변경은 허용")
    void limitThreeAcrossLists() throws Exception {
        home("PS_PARKING4", "Y", 3).andExpect(status().isOk());
        home("PS_BIZ", "Y", 1)          // 여신 4번째
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRD-101"))
                .andExpect(jsonPath("$.message").value("등록 건수 3개를 초과하였습니다."));
        home("PS_TIME", "Y", 2)         // 수신 4번째도 동일
                .andExpect(jsonPath("$.message").value("등록 건수 3개를 초과하였습니다."));
        // 1개를 N 으로 빼면 다시 등록 가능
        home("PS_2030", "N", null).andExpect(status().isOk());
        home("PS_BIZ", "Y", 2).andExpect(status().isOk());
    }

    @Test
    @DisplayName("기능3: Y 이면 순서 필수(PRD-102), 1~3만(PRD-104), 수신·여신 간 같은 숫자 불가 \"표시순서를 변경하세요\"")
    void orderRules() throws Exception {
        home("PS_PARKING4", "Y", null).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PRD-102"));
        home("PS_PARKING4", "Y", 4).andExpect(jsonPath("$.code").value("PRD-104"))
                .andExpect(jsonPath("$.message").value("홈 표시순서는 1~3 중에서 선택하세요."));
        // 여신 PS_SMART 가 1 사용 중 → 수신에서 1 선택 불가
        home("PS_PARKING4", "Y", 1).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRD-103"))
                .andExpect(jsonPath("$.message").value("표시순서를 변경하세요"));
        // 자기 자신의 순서 유지·변경은 가능
        home("PS_SMART", "Y", 1).andExpect(status().isOk());
        home("PS_SMART", "Y", 3).andExpect(status().isOk());
        home("PS_PARKING4", "Y", 1).andExpect(status().isOk());
        mvc.perform(get("/api/common/products/recommend"))
                .andExpect(jsonPath("$.data.items[*].name").value(contains("페퍼스 파킹통장4", "페퍼스 2030적금", "페퍼스마트대출")));
    }

    @Test
    @DisplayName("기능4: 상품설정(상품명·설명·금리·URL·판매여부) 수정 → 추천 상품 카드 각 항목에 연동, 판매중지는 숨김")
    void contentLinkage() throws Exception {
        mvc.perform(put("/api/common/product-admin/products/" + id("PS_SMART")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"페퍼스마트대출 플러스\",\"summary\":\"한도조회 3분\",\"rateText\":\"최저 연 6.5%\",\"detailUrl\":\"/product/loan/PS_SMART_PLUS\",\"onSale\":true}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/common/products/recommend"))
                .andExpect(jsonPath("$.data.items[0].name").value("페퍼스마트대출 플러스"))
                .andExpect(jsonPath("$.data.items[0].summary").value("한도조회 3분"))
                .andExpect(jsonPath("$.data.items[0].rateText").value("최저 연 6.5%"))
                .andExpect(jsonPath("$.data.items[0].detailUrl").value("/product/loan/PS_SMART_PLUS"));
        mvc.perform(put("/api/common/product-admin/products/" + id("PS_SMART")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"페퍼스마트대출\",\"detailUrl\":\"/product/loan/PS_SMART\",\"onSale\":false}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/common/products/recommend"))
                .andExpect(jsonPath("$.data.items[*].name").value(contains("페퍼스 2030적금")));
        // URL 검증
        mvc.perform(put("/api/common/product-admin/products/" + id("PS_SMART")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"detailUrl\":\"javascript:alert(1)\"}"))
                .andExpect(jsonPath("$.code").value("PRD-107"));
    }

    @Test
    @DisplayName("권한: 운영 설정(admin-api-open=false) 비로그인 PRD-401")
    void adminOnly() throws Exception {
        props.setAdminApiOpen(false);
        mvc.perform(get("/api/common/product-admin/products"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PRD-401"));
    }
}
