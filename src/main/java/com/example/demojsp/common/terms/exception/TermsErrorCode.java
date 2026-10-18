package com.example.demojsp.common.terms.exception;

import org.springframework.http.HttpStatus;

/**
 * 약관동의 공통 오류 코드. 접두어 TRM- 는 기존 코드(C/L/S/A/M/U/AC) 및 본인인증(IDV-)과 겹치지 않는다.
 */
public enum TermsErrorCode {

    BIZ_CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "TRM-001", "등록된 약관이 없는 업무입니다."),
    REQUIRED_NOT_AGREED(HttpStatus.BAD_REQUEST, "TRM-002", "필수 약관에 모두 동의해 주세요."),
    TERMS_NOT_FOUND(HttpStatus.NOT_FOUND, "TRM-003", "약관 정보를 찾을 수 없습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "TRM-004", "약관 동의 요청 값이 올바르지 않습니다."),
    UNKNOWN_TERMS(HttpStatus.BAD_REQUEST, "TRM-101", "존재하지 않는 약관이 포함되어 있습니다."),
    DUPLICATE_TERMS(HttpStatus.BAD_REQUEST, "TRM-102", "같은 약관이 중복 요청되었습니다."),
    VERSION_MISMATCH(HttpStatus.CONFLICT, "TRM-103", "약관이 개정되었습니다. 변경된 약관을 다시 확인해 주세요."),

    DUPLICATE_VERSION(HttpStatus.CONFLICT, "TRM-201", "이미 등록된 약관 버전입니다."),

    NOT_AGREED(HttpStatus.UNAUTHORIZED, "TRM-301", "약관 동의가 필요합니다."),
    ADMIN_ONLY(HttpStatus.FORBIDDEN, "TRM-401", "약관 관리 권한이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    TermsErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}
