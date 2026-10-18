package com.example.demojsp.common.banner.web;

import com.example.demojsp.common.banner.exception.BannerException;
import com.example.demojsp.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 배너팝업 예외 → ApiResponse 오류 응답.
 *
 * <p>@Order(HIGHEST_PRECEDENCE): 기존 GlobalExceptionHandler 의 {@code @ExceptionHandler(Exception.class)} 에
 * 먼저 잡혀 500 으로 응답되는 것을 방지. BannerException 하나만 처리하므로 다른 예외 처리에 영향 없음.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class BannerApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(BannerApiExceptionHandler.class);

    @ExceptionHandler(BannerException.class)
    public ResponseEntity<ApiResponse<Void>> handle(BannerException e) {
        log.warn("[배너팝업] {}", e.getMessage());
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(ApiResponse.error(e.getErrorCode().getCode(), e.displayMessage()));
    }
}
