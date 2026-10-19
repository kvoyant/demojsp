package com.example.demojsp.common.notice.exception;

/** 공지사항 예외 - NoticeExceptionHandler 가 ApiResponse(success=false, code, message) 로 바꿔 준다. */
public class NoticeException extends RuntimeException {

    private final NoticeErrorCode errorCode;

    public NoticeException(NoticeErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /** 화면에 보여줄 문구를 구체적으로 바꿀 때 (예: "제목은 100자 이내로 입력하세요.") */
    public NoticeException(NoticeErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public NoticeErrorCode getErrorCode() { return errorCode; }
}
