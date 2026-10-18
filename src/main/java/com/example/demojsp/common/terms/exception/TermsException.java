package com.example.demojsp.common.terms.exception;

/**
 * 약관동의 공통 예외. 화면에는 errorCode 의 표준 메시지만, detail 은 서버 로그로만 남긴다.
 */
public class TermsException extends RuntimeException {

    private final TermsErrorCode errorCode;
    private final String detail;

    public TermsException(TermsErrorCode errorCode) {
        this(errorCode, null);
    }

    public TermsException(TermsErrorCode errorCode, String detail) {
        super(errorCode.getCode() + " " + errorCode.getMessage() + (detail == null ? "" : " (" + detail + ")"));
        this.errorCode = errorCode;
        this.detail = detail;
    }

    public TermsErrorCode getErrorCode() { return errorCode; }
    public String getDetail() { return detail; }
}
