package com.example.demojsp.common.product.exception;

import org.springframework.http.HttpStatus;

/** 상품 공통 오류 코드 (PRD-) */
public enum ProductErrorCode {

    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRD-001", "상품을 찾을 수 없습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "PRD-002", "상품 요청 값이 올바르지 않습니다."),
    ADMIN_ONLY(HttpStatus.FORBIDDEN, "PRD-401", "상품 관리 권한이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ProductErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}
