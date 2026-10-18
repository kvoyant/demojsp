package com.example.demojsp.common.resume.web;

import com.example.demojsp.common.resume.exception.ResumeException;
import com.example.demojsp.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 이어하기 예외 → ApiResponse 오류 응답.
 *
 * <p>@Order(HIGHEST_PRECEDENCE): GlobalExceptionHandler 의 {@code Exception.class} 처리보다 먼저 잡는다
 * (ResumeException 하나만 처리하므로 다른 예외에 영향 없음).
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class ResumeApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ResumeApiExceptionHandler.class);

    @ExceptionHandler(ResumeException.class)
    public ResponseEntity<ApiResponse<Void>> handle(ResumeException e) {
        log.warn("[이어하기] {}", e.getMessage());
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(ApiResponse.error(e.getErrorCode().getCode(), e.displayMessage()));
    }
}
