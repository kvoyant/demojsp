package com.example.demojsp.common.product.web;

import com.example.demojsp.common.member.MemberSegmentResolver;
import com.example.demojsp.common.product.domain.RecommendView;
import com.example.demojsp.common.product.service.ProductService;
import com.example.demojsp.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [공통#47] 홈 추천 상품 고객 API (로그인 불필요).
 *
 * <pre>
 *  GET /api/common/products/recommend   → eligible(금융회원이면 false), moreUrl(상품몰), items(홈 표시순서)
 * </pre>
 */
@RestController
public class ProductApiController {

    private final ProductService productService;
    private final MemberSegmentResolver memberResolver;

    public ProductApiController(ProductService productService, MemberSegmentResolver memberResolver) {
        this.productService = productService;
        this.memberResolver = memberResolver;
    }

    @GetMapping("/api/common/products/recommend")
    public ApiResponse<RecommendView> recommend(HttpServletRequest request) {
        return ApiResponse.success(productService.recommend(memberResolver.resolve(request)));
    }
}
