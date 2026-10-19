package com.example.demojsp.common.producttab.exception;

import org.springframework.http.HttpStatus;

/** 상품 탭 오류 코드 (PTB-) */
public enum ProductTabErrorCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "PTB-001", "요청 값이 올바르지 않습니다."),
    SLIDE_NOT_FOUND(HttpStatus.NOT_FOUND, "PTB-002", "슬라이드 카드를 찾을 수 없습니다."),
    SECTION_NOT_FOUND(HttpStatus.NOT_FOUND, "PTB-003", "섹션을 찾을 수 없습니다."),
    INVALID_VALUE(HttpStatus.BAD_REQUEST, "PTB-101", "입력 값을 확인하세요."),
    ADMIN_ONLY(HttpStatus.FORBIDDEN, "PTB-401", "상품 탭 관리 권한이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ProductTabErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}
