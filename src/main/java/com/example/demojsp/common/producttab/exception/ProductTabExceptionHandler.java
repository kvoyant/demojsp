package com.example.demojsp.common.producttab.exception;

import com.example.demojsp.common.producttab.controller.ProductTabApiController;
import com.example.demojsp.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 상품 탭 API 오류 응답 (이 모듈 controller 패키지에만 적용 - 다른 업무의 오류 처리에 영향 없음) */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackageClasses = ProductTabApiController.class)
public class ProductTabExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ProductTabExceptionHandler.class);

    @ExceptionHandler(ProductTabException.class)
    public ResponseEntity<ApiResponse<Void>> handle(ProductTabException e) {
        log.warn("[상품탭] {} {}", e.getErrorCode().getCode(), e.getMessage());
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(ApiResponse.error(e.getErrorCode().getCode(), e.getMessage()));
    }

    /** JSON 형식 오류 (날짜 형식이 틀린 경우 등) */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> notReadable(HttpMessageNotReadableException e) {
        ProductTabErrorCode c = ProductTabErrorCode.INVALID_REQUEST;
        return ResponseEntity.status(c.getStatus()).body(ApiResponse.error(c.getCode(), c.getMessage()));
    }
}
