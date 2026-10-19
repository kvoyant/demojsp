package com.example.demojsp.common.notice.exception;

import org.springframework.http.HttpStatus;

/** 공지사항 오류 코드 (NTC-) */
public enum NoticeErrorCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "NTC-001", "요청 값이 올바르지 않습니다."),
    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "NTC-002", "게시 중인 공지사항이 아닙니다."),
    INVALID_VALUE(HttpStatus.BAD_REQUEST, "NTC-101", "입력 값을 확인하세요."),
    ADMIN_ONLY(HttpStatus.FORBIDDEN, "NTC-401", "공지사항 관리 권한이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    NoticeErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}
