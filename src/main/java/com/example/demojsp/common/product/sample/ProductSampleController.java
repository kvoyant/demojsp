package com.example.demojsp.common.product.sample;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * [공통#47 샘플] 홈 추천 상품 (WEB-INF/views/common/product/recommend.jsp).
 * <p>APP 홈 JSP: {@code RecommendProducts.mount('#recommend')}
 */
@Controller
public class ProductSampleController {

    @GetMapping("/common/product/recommend")
    public String recommend() {
        return "common/product/recommend";
    }

    /** [공통#48] ADMIN 수신상품목록 / 여신상품목록 - 상품설정 홈 표시여부·순서 (WEB-INF/views/common/product/admin.jsp) */
    @GetMapping("/common/product/admin")
    public String admin() {
        return "common/product/admin";
    }
}
