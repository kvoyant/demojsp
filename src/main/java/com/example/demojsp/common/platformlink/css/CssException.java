package com.example.demojsp.common.platformlink.css;

/** CSS 시스템 오류 (통신 실패·내부 오류). 심사 거절과 구분한다. */
public class CssException extends RuntimeException {
    public CssException(String message) {
        super(message);
    }
}
