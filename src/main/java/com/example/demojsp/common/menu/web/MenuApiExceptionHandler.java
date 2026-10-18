package com.example.demojsp.common.menu.web;

import com.example.demojsp.common.menu.exception.MenuException;
import com.example.demojsp.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 메뉴 예외 → ApiResponse (GlobalExceptionHandler 보다 먼저, MenuException 만 처리) */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class MenuApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(MenuApiExceptionHandler.class);

    @ExceptionHandler(MenuException.class)
    public ResponseEntity<ApiResponse<Void>> handle(MenuException e) {
        log.warn("[메뉴] {}", e.getMessage());
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(ApiResponse.error(e.getErrorCode().getCode(), e.displayMessage()));
    }
}
