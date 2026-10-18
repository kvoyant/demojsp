package com.example.demojsp.common.banner.exception;

import org.springframework.http.HttpStatus;

/**
 * 배너팝업 공통 오류 코드. 접두어 BNR- 는 기존 코드(C/L/S/A/M/U/AC, IDV-, TRM-)와 겹치지 않는다.
 */
public enum BannerErrorCode {

    INVALID_CHANNEL(HttpStatus.BAD_REQUEST, "BNR-001", "지원하지 않는 노출 채널입니다."),
    INVALID_DATETIME(HttpStatus.BAD_REQUEST, "BNR-002", "일시 형식이 올바르지 않습니다. (yyyy-MM-dd HH:mm)"),
    BANNER_NOT_FOUND(HttpStatus.NOT_FOUND, "BNR-003", "배너팝업을 찾을 수 없습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "BNR-004", "배너팝업 요청 값이 올바르지 않습니다."),

    REQUIRED_MISSING(HttpStatus.BAD_REQUEST, "BNR-101", "필수 입력 항목이 누락되었습니다."),
    INVALID_PERIOD(HttpStatus.BAD_REQUEST, "BNR-102", "게시 종료일시는 시작일시보다 늦어야 합니다."),
    INVALID_ORDER(HttpStatus.BAD_REQUEST, "BNR-103", "표시순서는 1~999 사이 숫자로 입력해 주세요."),
    INVALID_URL(HttpStatus.BAD_REQUEST, "BNR-104", "링크/이미지 URL 형식이 올바르지 않습니다."),
    ALT_TEXT_REQUIRED(HttpStatus.BAD_REQUEST, "BNR-105", "이미지를 등록하면 대체텍스트를 반드시 입력해야 합니다."),
    TOO_LONG(HttpStatus.BAD_REQUEST, "BNR-106", "입력 가능한 글자 수를 초과했습니다."),
    INVALID_CODE(HttpStatus.BAD_REQUEST, "BNR-107", "배너팝업구분/공지유형 값이 올바르지 않습니다."),

    ADMIN_ONLY(HttpStatus.FORBIDDEN, "BNR-401", "배너팝업 관리 권한이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    BannerErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}
