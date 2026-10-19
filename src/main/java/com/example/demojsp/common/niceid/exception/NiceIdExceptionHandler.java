package com.example.demojsp.common.niceid.exception;

import com.example.demojsp.common.niceid.controller.NiceIdApiController;
import com.example.demojsp.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 본인인증 API 오류 응답 (이 모듈 controller 패키지에만 적용) */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackageClasses = NiceIdApiController.class)
public class NiceIdExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(NiceIdExceptionHandler.class);

    @ExceptionHandler(NiceIdException.class)
    public ResponseEntity<ApiResponse<Void>> handle(NiceIdException e) {
        log.warn("[본인인증] {} {}", e.getErrorCode().getCode(), e.getMessage());
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(ApiResponse.error(e.getErrorCode().getCode(), e.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> notReadable(HttpMessageNotReadableException e) {
        NiceIdErrorCode c = NiceIdErrorCode.INVALID_REQUEST;
        return ResponseEntity.status(c.getStatus()).body(ApiResponse.error(c.getCode(), c.getMessage()));
    }
}
