package com.example.demojsp.common.product.web;

import com.example.demojsp.common.product.exception.ProductException;
import com.example.demojsp.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 상품 예외 → ApiResponse (ProductException 만 처리) */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class ProductApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ProductApiExceptionHandler.class);

    @ExceptionHandler(ProductException.class)
    public ResponseEntity<ApiResponse<Void>> handle(ProductException e) {
        log.warn("[상품] {}", e.getMessage());
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(ApiResponse.error(e.getErrorCode().getCode(), e.displayMessage()));
    }
}
