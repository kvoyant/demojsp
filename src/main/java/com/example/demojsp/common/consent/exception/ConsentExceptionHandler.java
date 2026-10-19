package com.example.demojsp.common.consent.exception;

import com.example.demojsp.common.consent.controller.ConsentApiController;
import com.example.demojsp.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 약관동의 API 오류 응답 (이 패키지의 컨트롤러에만 적용) */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackageClasses = ConsentApiController.class)
public class ConsentExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ConsentExceptionHandler.class);

    @ExceptionHandler(ConsentException.class)
    public ResponseEntity<ApiResponse<Void>> handle(ConsentException e) {
        log.warn("[약관동의] {} {}", e.getErrorCode().getCode(), e.getMessage());
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(ApiResponse.error(e.getErrorCode().getCode(), e.getMessage()));
    }

    /** JSON 형식 오류 (날짜 형식 등) */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> notReadable(HttpMessageNotReadableException e) {
        ConsentErrorCode c = ConsentErrorCode.INVALID_REQUEST;
        return ResponseEntity.status(c.getStatus()).body(ApiResponse.error(c.getCode(), c.getMessage()));
    }
}
