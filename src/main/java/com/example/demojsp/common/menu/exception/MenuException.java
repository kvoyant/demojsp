package com.example.demojsp.common.menu.exception;

/** 메뉴 공통 예외 (화면에는 표준 메시지 + 항목명만) */
public class MenuException extends RuntimeException {

    private final MenuErrorCode errorCode;
    private final String field;

    public MenuException(MenuErrorCode errorCode) {
        this(errorCode, null);
    }

    public MenuException(MenuErrorCode errorCode, String field) {
        super(errorCode.getCode() + " " + errorCode.getMessage() + (field == null ? "" : " (" + field + ")"));
        this.errorCode = errorCode;
        this.field = field;
    }

    public MenuErrorCode getErrorCode() { return errorCode; }

    public String displayMessage() {
        return field == null ? errorCode.getMessage() : errorCode.getMessage() + " [" + field + "]";
    }
}
