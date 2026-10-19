package com.example.demojsp.common.producttab.controller;

import com.example.demojsp.common.producttab.dto.ProductTabView;
import com.example.demojsp.common.producttab.service.ProductTabService;
import com.example.demojsp.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * [CF-BC-20 / 공통#26] 메인화면 상품 탭 - 사용자 화면 API (화면 공통 컴포넌트 product-tab.js 가 호출).
 *
 * <pre>
 *  GET /api/common/product-tab?channel=APP|PC_WEB
 *      → { slides: [상단 슬라이드 카드...], sections: [ {섹션, items:[상품...]} ... ] }
 * </pre>
 * 로그인 여부와 관계없이 같은 내용 (상품 안내 화면).
 */
@RestController
@RequestMapping("/api/common/product-tab")
public class ProductTabApiController {

    private final ProductTabService service;

    public ProductTabApiController(ProductTabService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<ProductTabView> view(@RequestParam(value = "channel", defaultValue = "APP") String channel) {
        return ApiResponse.success(service.getView(channel));
    }
}
