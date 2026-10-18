package com.example.demojsp.common.terms.web;

import com.example.demojsp.common.response.ApiResponse;
import com.example.demojsp.common.terms.exception.TermsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 약관 예외 → ApiResponse 오류 응답.
 *
 * <p>@Order(HIGHEST_PRECEDENCE): 기존 GlobalExceptionHandler 의 {@code @ExceptionHandler(Exception.class)} 에
 * 먼저 잡혀 500 으로 응답되는 것을 방지. TermsException 하나만 처리하므로 다른 예외 처리에 영향 없음.
 * <p>업무 API 에서 {@code termsSessionManager.require()} 가 던지는 TRM-301(미동의)도 여기서 401 로 변환된다.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class TermsApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(TermsApiExceptionHandler.class);

    @ExceptionHandler(TermsException.class)
    public ResponseEntity<ApiResponse<Void>> handle(TermsException e) {
        log.warn("[약관동의] {} detail={}", e.getErrorCode().getCode(), e.getDetail());
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(ApiResponse.error(e.getErrorCode().getCode(), e.getErrorCode().getMessage()));
    }
}
