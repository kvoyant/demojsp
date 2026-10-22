package com.example.demojsp.common.platformlink.exception;

import org.springframework.http.HttpStatus;

/**
 * 관리·샘플 API 오류코드 (PLK-xxx).
 * ※ 제휴사 연계 API 는 이 코드를 쓰지 않는다 - 제휴사에는 항상 제휴사 결과코드(TB_LNK_CODE_MAP)로 응답한다.
 */
public enum PlatformLinkErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "PLK-001", "요청 값이 올바르지 않습니다."),
    PARTNER_NOT_FOUND(HttpStatus.NOT_FOUND, "PLK-002", "등록되지 않은 제휴사입니다."),
    ADAPTER_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "PLK-003", "제휴사 전문 변환기(ADAPTER_CD)가 등록되지 않았습니다."),
    INVALID_VALUE(HttpStatus.BAD_REQUEST, "PLK-101", "입력값을 확인하세요."),
    ADMIN_ONLY(HttpStatus.FORBIDDEN, "PLK-401", "관리자만 사용할 수 있습니다."),
    DEMO_ONLY(HttpStatus.FORBIDDEN, "PLK-901", "local 샘플 전용 기능입니다. (common.platform-link.demo-api-open)");

    private final HttpStatus status;
    private final String code;
    private final String message;

    PlatformLinkErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}
