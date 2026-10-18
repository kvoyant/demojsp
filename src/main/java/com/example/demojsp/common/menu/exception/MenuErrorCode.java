package com.example.demojsp.common.menu.exception;

import org.springframework.http.HttpStatus;

/**
 * 메뉴 공통 오류 코드 (MNU-). 기존 접두어(BNR-, RSM- 등)와 겹치지 않는다.
 */
public enum MenuErrorCode {

    MENU_NOT_FOUND(HttpStatus.NOT_FOUND, "MNU-001", "메뉴를 찾을 수 없습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "MNU-002", "메뉴 요청 값이 올바르지 않습니다."),
    REQUIRED_MISSING(HttpStatus.BAD_REQUEST, "MNU-101", "필수 입력 항목이 누락되었습니다."),
    TOO_LONG(HttpStatus.BAD_REQUEST, "MNU-102", "입력 가능한 글자 수를 초과했습니다."),
    INVALID_URL(HttpStatus.BAD_REQUEST, "MNU-103", "메뉴 URL 형식이 올바르지 않습니다."),
    INVALID_CATEGORY(HttpStatus.BAD_REQUEST, "MNU-104", "카테고리 값이 올바르지 않습니다."),
    INVALID_ORDER(HttpStatus.BAD_REQUEST, "MNU-105", "표시순서가 올바르지 않습니다."),
    DUPLICATE_NAME(HttpStatus.CONFLICT, "MNU-106", "같은 카테고리에 같은 이름의 메뉴가 있습니다."),
    ADMIN_ONLY(HttpStatus.FORBIDDEN, "MNU-401", "뱅킹메뉴 관리 권한이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    MenuErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}
