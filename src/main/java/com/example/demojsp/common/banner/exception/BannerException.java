package com.example.demojsp.common.banner.exception;

/**
 * 배너팝업 공통 예외. 화면에는 errorCode 의 표준 메시지 + (검증 오류일 때) 항목명만, 상세는 서버 로그로 남긴다.
 */
public class BannerException extends RuntimeException {

    private final BannerErrorCode errorCode;
    /** 화면에 함께 보여줄 항목명 (예: "제목") - 사용자 입력값은 넣지 않는다 */
    private final String field;

    public BannerException(BannerErrorCode errorCode) {
        this(errorCode, null);
    }

    public BannerException(BannerErrorCode errorCode, String field) {
        super(errorCode.getCode() + " " + errorCode.getMessage() + (field == null ? "" : " (" + field + ")"));
        this.errorCode = errorCode;
        this.field = field;
    }

    public BannerErrorCode getErrorCode() { return errorCode; }
    public String getField() { return field; }

    /** 응답 메시지: 표준 메시지 + [항목] */
    public String displayMessage() {
        return field == null ? errorCode.getMessage() : errorCode.getMessage() + " [" + field + "]";
    }
}
