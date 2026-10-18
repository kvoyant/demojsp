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
}
