package com.example.demojsp.common.niceid.exception;

/** 본인인증 예외 - NiceIdExceptionHandler 가 ApiResponse(success=false, code, message) 로 바꿔 준다. */
public class NiceIdException extends RuntimeException {

    private final NiceIdErrorCode errorCode;

    public NiceIdException(NiceIdErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public NiceIdException(NiceIdErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /** 메시지에 값이 들어가는 경우 (예: "인증번호가 일치하지 않습니다. (2/5)") */
    public static NiceIdException format(NiceIdErrorCode errorCode, Object... args) {
        return new NiceIdException(errorCode, String.format(errorCode.getMessage(), args));
    }

    public NiceIdErrorCode getErrorCode() { return errorCode; }
}
