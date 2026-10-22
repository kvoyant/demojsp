package com.example.demojsp.common.platformlink.exception;

public class PlatformLinkException extends RuntimeException {

    private final PlatformLinkErrorCode errorCode;

    public PlatformLinkException(PlatformLinkErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public PlatformLinkException(PlatformLinkErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public PlatformLinkErrorCode getErrorCode() { return errorCode; }
}
