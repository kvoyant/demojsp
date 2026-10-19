package com.example.demojsp.common.producttab.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * [공통#26 샘플] 메인화면 상품 탭 화면.
 * <ul>
 *   <li>/common/product-tab/sample : APP / PC Web 사용자 화면 (product-tab.js)</li>
 *   <li>/common/product-tab/admin  : 디지털어드민 상품 탭 관리</li>
 * </ul>
 */
@Controller
public class ProductTabSampleController {

    @GetMapping("/common/product-tab/sample")
    public String sample() {
        return "common/producttab/sample";
    }

    @GetMapping("/common/product-tab/admin")
    public String admin() {
        return "common/producttab/admin";
    }
}
