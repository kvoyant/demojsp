package com.example.demojsp.common.resume.exception;

import org.springframework.http.HttpStatus;

/**
 * 이어하기 공통 오류 코드. 접두어 RSM- 는 기존 코드(C/L/S/A/M/U/AC, IDV-, TRM-, BNR-)와 겹치지 않는다.
 */
public enum ResumeErrorCode {

    INVALID_SCOPE(HttpStatus.BAD_REQUEST, "RSM-001", "이어하기 조회 범위가 올바르지 않습니다."),
    NOT_RESUMABLE(HttpStatus.CONFLICT, "RSM-002", "이미 완료·취소되었거나 이어하기 기간이 지난 신청입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "RSM-003", "이어하기 신청 건을 찾을 수 없습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "RSM-004", "이어하기 요청 값이 올바르지 않습니다."),

    REQUIRED_MISSING(HttpStatus.BAD_REQUEST, "RSM-101", "필수 입력 항목이 누락되었습니다."),
    TOO_LONG(HttpStatus.BAD_REQUEST, "RSM-102", "입력 가능한 글자 수를 초과했습니다."),
    INVALID_URL(HttpStatus.BAD_REQUEST, "RSM-103", "이어하기 링크 형식이 올바르지 않습니다."),
    INVALID_CODE(HttpStatus.BAD_REQUEST, "RSM-104", "상품구분/업무코드/회사코드 값이 올바르지 않습니다."),
    DEPOSIT_OWN_ONLY(HttpStatus.BAD_REQUEST, "RSM-105", "수신상품 이어하기는 당행 상품만 등록할 수 있습니다."),

    LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "RSM-401", "로그인 후 이용할 수 있습니다."),
    DEMO_ONLY(HttpStatus.FORBIDDEN, "RSM-402", "테스트(local) 환경에서만 사용할 수 있는 기능입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ResumeErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}
