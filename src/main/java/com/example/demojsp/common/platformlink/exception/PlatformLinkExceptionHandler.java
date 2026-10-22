package com.example.demojsp.common.platformlink.exception;

import com.example.demojsp.common.platformlink.controller.PlatformLinkAdminApiController;
import com.example.demojsp.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 관리·샘플 API 오류 응답 (이 모듈 controller 패키지에만 적용) */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackageClasses = PlatformLinkAdminApiController.class)
public class PlatformLinkExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(PlatformLinkExceptionHandler.class);

    @ExceptionHandler(PlatformLinkException.class)
    public ResponseEntity<ApiResponse<Void>> handle(PlatformLinkException e) {
        log.warn("[플랫폼연계] {} {}", e.getErrorCode().getCode(), e.getMessage());
        return ResponseEntity.status(e.getErrorCode().getStatus()).body(ApiResponse.error(e.getErrorCode().getCode(), e.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> notReadable(HttpMessageNotReadableException e) {
        PlatformLinkErrorCode c = PlatformLinkErrorCode.INVALID_REQUEST;
        return ResponseEntity.status(c.getStatus()).body(ApiResponse.error(c.getCode(), c.getMessage()));
    }
}
