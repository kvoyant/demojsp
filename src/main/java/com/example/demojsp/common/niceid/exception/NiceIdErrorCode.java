package com.example.demojsp.common.niceid.exception;

import org.springframework.http.HttpStatus;

/** 본인인증 오류 코드 (NID-) */
public enum NiceIdErrorCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "NID-001", "요청 값이 올바르지 않습니다."),
    PURPOSE_NOT_FOUND(HttpStatus.NOT_FOUND, "NID-002", "등록되지 않았거나 사용하지 않는 본인인증 업무입니다."),
    REQ_NOT_FOUND(HttpStatus.NOT_FOUND, "NID-003", "본인인증 요청을 찾을 수 없습니다. 처음부터 다시 진행해 주세요."),
    ALREADY_DONE(HttpStatus.CONFLICT, "NID-004", "이미 처리된 본인인증입니다."),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "NID-005", "인증 요청이 너무 많습니다. 잠시 후 다시 시도해 주세요."),
    EXPIRED(HttpStatus.BAD_REQUEST, "NID-006", "인증 시간이 지났습니다. 인증번호를 다시 받아 주세요."),
    CODE_MISMATCH(HttpStatus.BAD_REQUEST, "NID-007", "인증번호가 일치하지 않습니다. (%d/%d)"),
    TRY_EXCEEDED(HttpStatus.BAD_REQUEST, "NID-008", "인증번호를 %d회 잘못 입력했습니다. 처음부터 다시 진행해 주세요."),
    METHOD_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "NID-009", "이 업무에서 사용할 수 없는 인증 방식입니다."),
    CONSENT_REQUIRED(HttpStatus.FORBIDDEN, "NID-101", "본인인증 약관에 먼저 동의해 주세요."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "NID-102", "입력 값을 확인하세요."),
    PROVIDER_ERROR(HttpStatus.BAD_GATEWAY, "NID-201", "본인확인기관 연결이 원활하지 않습니다. 잠시 후 다시 시도해 주세요."),
    NOT_VERIFIED(HttpStatus.FORBIDDEN, "NID-301", "본인인증이 완료되지 않았습니다."),
    RESULT_EXPIRED(HttpStatus.FORBIDDEN, "NID-302", "본인인증 후 시간이 지났습니다. 다시 인증해 주세요."),
    DEMO_ONLY(HttpStatus.FORBIDDEN, "NID-901", "local 테스트에서만 사용할 수 있습니다."),
    ADMIN_ONLY(HttpStatus.FORBIDDEN, "NID-401", "본인인증 관리 권한이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    NiceIdErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}
