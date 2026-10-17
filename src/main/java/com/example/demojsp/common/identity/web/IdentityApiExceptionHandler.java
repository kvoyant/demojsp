package com.example.demojsp.common.identity.web;

import com.example.demojsp.common.identity.exception.IdentityVerificationException;
import com.example.demojsp.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 본인인증 예외 → ApiResponse 오류 응답 변환.
 *
 * <p><b>@Order(HIGHEST_PRECEDENCE) 가 필요한 이유</b>: 기존 GlobalExceptionHandler 에 {@code @ExceptionHandler(Exception.class)}
 * 가 있어서, 순서를 지정하지 않으면 IdentityVerificationException 이 그쪽에 먼저 잡혀 500(C004)으로 응답될 수 있다.
 * 이 Advice 는 IdentityVerificationException 하나만 처리하므로 우선순위를 높여도 다른 예외 처리에는 영향이 없다.
 *
 * <p>업무 컨트롤러에서 {@code identitySessionManager.require()} 가 던지는 IDV-301(미인증)도 여기서 처리되어
 * 화면은 {@code res.code === 'IDV-301'} 을 보고 본인인증 화면으로 유도하면 된다.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class IdentityApiExceptionHandler {

    @ExceptionHandler(IdentityVerificationException.class)
    public ResponseEntity<ApiResponse<Void>> handle(IdentityVerificationException e) {
        // 상세 사유(detail)는 로그로만 남기고 화면에는 표준 메시지만 내려준다
        log.warn("[공통본인인증] {} detail={}", e.getErrorCode().getCode(), e.getDetail(), e.getCause());
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(ApiResponse.error(e.getErrorCode().getCode(), e.getErrorCode().getMessage()));
    }
}
