package com.example.demojsp.common.acctverify.exception;

/** 1원 계좌인증 예외 - AcctVerifyExceptionHandler 가 ApiResponse(success=false, code, message) 로 바꿔 준다. */
public class AcctVerifyException extends RuntimeException {

    private final AcctVerifyErrorCode errorCode;

    public AcctVerifyException(AcctVerifyErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public AcctVerifyException(AcctVerifyErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public static AcctVerifyException format(AcctVerifyErrorCode errorCode, Object... args) {
        return new AcctVerifyException(errorCode, String.format(errorCode.getMessage(), args));
    }

    public AcctVerifyErrorCode getErrorCode() { return errorCode; }
}
