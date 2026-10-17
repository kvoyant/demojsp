package com.example.demojsp.common.identity.exception;

/**
 * 본인인증 공통 예외.
 *
 * <p>{@code detail} 에는 내부 분석용 상세 사유(인증기관 결과코드 등)를 담고, 화면에는 노출하지 않는다.
 */
public class IdentityVerificationException extends RuntimeException {

    private final IdentityErrorCode errorCode;
    private final String detail;

    public IdentityVerificationException(IdentityErrorCode errorCode) {
        this(errorCode, null, null);
    }

    public IdentityVerificationException(IdentityErrorCode errorCode, String detail) {
        this(errorCode, detail, null);
    }

    public IdentityVerificationException(IdentityErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode.getCode() + " " + errorCode.getMessage() + (detail == null ? "" : " (" + detail + ")"), cause);
        this.errorCode = errorCode;
        this.detail = detail;
    }

    public IdentityErrorCode getErrorCode() { return errorCode; }
    public String getDetail() { return detail; }
}
