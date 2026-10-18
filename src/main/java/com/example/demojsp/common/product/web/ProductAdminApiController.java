package com.example.demojsp.common.product.web;

import com.example.demojsp.common.product.domain.HomeSettingRequest;
import com.example.demojsp.common.product.domain.HomeStatus;
import com.example.demojsp.common.product.domain.ProductAdminView;
import com.example.demojsp.common.product.domain.ProductUpdateRequest;
import com.example.demojsp.common.product.exception.ProductErrorCode;
import com.example.demojsp.common.product.service.ProductAdminService;
import com.example.demojsp.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * [공통#48] ADMIN 수신상품목록 / 여신상품목록 - 상품설정 (홈 표시여부 · 홈 표시순서).
 *
 * <pre>
 *  GET /api/common/product-admin/products?kind=DEPOSIT|LOAN   목록 (상품설정 컬럼 포함)
 *  GET /api/common/product-admin/home-status                  홈 표시 현황 (합산 건수, 순서별 사용 상품)
 *  PUT /api/common/product-admin/products/{id}/home           {homeDisplay:"Y", homeOrder:1}
 *  PUT /api/common/product-admin/products/{id}                상품설정 기본 항목 (상품명·설명·금리·URL·판매여부)
 * </pre>
 */
@RestController
@RequestMapping("/api/common/product-admin")
public class ProductAdminApiController {

    private final ProductAdminService service;
    private final ProductRequestContext context;

    public ProductAdminApiController(ProductAdminService service, ProductRequestContext context) {
        this.service = service;
        this.context = context;
    }

    @GetMapping("/products")
    public ApiResponse<List<ProductAdminView>> list(@RequestParam(value = "kind", required = false) String kind,
                                                    HttpServletRequest request) {
        context.requireAdmin(request);
        return ApiResponse.success(service.list(kind));
    }

    @GetMapping("/home-status")
    public ApiResponse<HomeStatus> homeStatus(HttpServletRequest request) {
        context.requireAdmin(request);
        return ApiResponse.success(service.homeStatus());
    }

    @PutMapping("/products/{id}/home")
    public ApiResponse<ProductAdminView> changeHome(@PathVariable("id") long id, @RequestBody HomeSettingRequest body,
                                                    HttpServletRequest request) {
        return ApiResponse.success("홈 표시 설정이 저장되었습니다.", service.changeHome(id, body, context.requireAdmin(request)));
    }

    @PutMapping("/products/{id}")
    public ApiResponse<ProductAdminView> update(@PathVariable("id") long id, @RequestBody ProductUpdateRequest body,
                                                HttpServletRequest request) {
        return ApiResponse.success("상품설정이 저장되었습니다.", service.update(id, body, context.requireAdmin(request)));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> notReadable(HttpMessageNotReadableException e) {
        ProductErrorCode c = ProductErrorCode.INVALID_REQUEST;
        return ResponseEntity.status(c.getStatus()).body(ApiResponse.error(c.getCode(), c.getMessage()));
    }
}
