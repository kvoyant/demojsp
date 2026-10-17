package com.example.demojsp.common.identity.exception;

import org.springframework.http.HttpStatus;

/**
 * 본인인증 공통 오류 코드.
 *
 * <p>화면에는 code 와 message 만 내려주고(ApiResponse.error), 상세 원인(인증기관 응답코드 등)은 서버 로그로만 남긴다.
 *
 * <p><b>기존 {@code common.exception.ErrorCode} 와 분리한 이유</b>:
 * 기존 NICE 구현(A001~A007)과 코드/메시지가 섞이지 않게 하고, 공통모듈을 독립적으로 이관·배포할 수 있게 하기 위함.
 * 코드 접두어 IDV- 는 기존 코드(C/L/S/A/M/U/AC)와 겹치지 않는다.
 */
public enum IdentityErrorCode {

    UNSUPPORTED_METHOD(HttpStatus.BAD_REQUEST, "IDV-001", "지원하지 않는 인증수단입니다."),
    REQUIRED_TERMS_NOT_AGREED(HttpStatus.BAD_REQUEST, "IDV-002", "필수 약관에 동의해 주세요."),
    INVALID_RETURN_URL(HttpStatus.BAD_REQUEST, "IDV-003", "허용되지 않은 이동 경로입니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "IDV-004", "본인인증 요청 값이 올바르지 않습니다."),

    TX_NOT_FOUND(HttpStatus.BAD_REQUEST, "IDV-101", "인증 요청 정보가 없거나 이미 처리되었습니다. 다시 시도해 주세요."),
    TX_EXPIRED(HttpStatus.BAD_REQUEST, "IDV-102", "인증 유효시간이 지났습니다. 다시 시도해 주세요."),
    SESSION_MISMATCH(HttpStatus.FORBIDDEN, "IDV-103", "인증을 요청한 사용자와 일치하지 않습니다."),
    REQUEST_NO_MISMATCH(HttpStatus.BAD_REQUEST, "IDV-104", "인증 요청 정보가 일치하지 않습니다."),

    INTEGRITY_FAILED(HttpStatus.BAD_REQUEST, "IDV-201", "인증 결과 위변조가 의심되어 처리할 수 없습니다."),
    AUTH_FAILED(HttpStatus.BAD_REQUEST, "IDV-202", "본인인증에 실패했습니다."),
    AUTH_CANCELED(HttpStatus.BAD_REQUEST, "IDV-203", "본인인증이 취소되었습니다."),

    NOT_VERIFIED(HttpStatus.UNAUTHORIZED, "IDV-301", "본인인증이 필요합니다."),

    PROVIDER_ERROR(HttpStatus.BAD_GATEWAY, "IDV-901", "인증기관과 통신 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "IDV-999", "본인인증 처리 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    IdentityErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}
