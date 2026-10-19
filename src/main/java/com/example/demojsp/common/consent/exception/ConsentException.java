package com.example.demojsp.common.consent.exception;

/** 약관동의 예외 - ConsentExceptionHandler 가 ApiResponse(success=false, code, message) 로 바꿔 준다. */
public class ConsentException extends RuntimeException {

    private final ConsentErrorCode errorCode;

    public ConsentException(ConsentErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /** 메시지를 바꿔야 할 때 (예: "약관명은 100자 이내로 입력하세요.") */
    public ConsentException(ConsentErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ConsentErrorCode getErrorCode() { return errorCode; }
}
