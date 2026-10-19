package com.example.demojsp.common.notice.exception;

import com.example.demojsp.common.notice.controller.NoticeApiController;
import com.example.demojsp.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 공지사항 API 오류 응답 (이 모듈 controller 패키지에만 적용) */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackageClasses = NoticeApiController.class)
public class NoticeExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(NoticeExceptionHandler.class);

    @ExceptionHandler(NoticeException.class)
    public ResponseEntity<ApiResponse<Void>> handle(NoticeException e) {
        log.warn("[공지사항] {} {}", e.getErrorCode().getCode(), e.getMessage());
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(ApiResponse.error(e.getErrorCode().getCode(), e.getMessage()));
    }

    /** JSON 형식 오류 (일시 형식이 틀린 경우 등) */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> notReadable(HttpMessageNotReadableException e) {
        NoticeErrorCode c = NoticeErrorCode.INVALID_REQUEST;
        return ResponseEntity.status(c.getStatus()).body(ApiResponse.error(c.getCode(), c.getMessage()));
    }
}
