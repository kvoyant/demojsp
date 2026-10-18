package com.example.demojsp.common.product.exception;

import org.springframework.http.HttpStatus;

/** 상품 공통 오류 코드 (PRD-) */
public enum ProductErrorCode {

    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRD-001", "상품을 찾을 수 없습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "PRD-002", "상품 요청 값이 올바르지 않습니다."),

    // 공통#48 홈 표시여부·홈 표시순서 (안내팝업 문구는 요구사항 원문 그대로)
    HOME_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "PRD-101", "등록 건수 %d개를 초과하였습니다."),
    HOME_ORDER_REQUIRED(HttpStatus.BAD_REQUEST, "PRD-102", "홈 표시여부 Y 는 홈 표시순서를 선택해야 합니다."),
    HOME_ORDER_DUPLICATED(HttpStatus.CONFLICT, "PRD-103", "표시순서를 변경하세요"),
    HOME_ORDER_INVALID(HttpStatus.BAD_REQUEST, "PRD-104", "홈 표시순서는 1~%d 중에서 선택하세요."),

    // 상품설정 (콘텐츠 연동 항목)
    REQUIRED_MISSING(HttpStatus.BAD_REQUEST, "PRD-105", "필수 입력 항목이 누락되었습니다."),
    TOO_LONG(HttpStatus.BAD_REQUEST, "PRD-106", "입력 가능한 글자 수를 초과했습니다."),
    INVALID_URL(HttpStatus.BAD_REQUEST, "PRD-107", "상품안내페이지 URL 형식이 올바르지 않습니다."),
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
