package com.example.demojsp.common.resume.exception;

/**
 * 이어하기 공통 예외. 화면에는 표준 메시지 + (검증 오류일 때) 항목명만, 상세는 서버 로그로 남긴다.
 */
public class ResumeException extends RuntimeException {

    private final ResumeErrorCode errorCode;
    /** 화면에 함께 보여줄 항목명 - 사용자 입력값은 넣지 않는다 */
    private final String field;

    public ResumeException(ResumeErrorCode errorCode) {
        this(errorCode, null);
    }

    public ResumeException(ResumeErrorCode errorCode, String field) {
        super(errorCode.getCode() + " " + errorCode.getMessage() + (field == null ? "" : " (" + field + ")"));
        this.errorCode = errorCode;
        this.field = field;
    }

    public ResumeErrorCode getErrorCode() { return errorCode; }
    public String getField() { return field; }

    public String displayMessage() {
        return field == null ? errorCode.getMessage() : errorCode.getMessage() + " [" + field + "]";
    }
}
