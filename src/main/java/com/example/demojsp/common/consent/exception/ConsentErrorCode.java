package com.example.demojsp.common.consent.exception;

import org.springframework.http.HttpStatus;

/** 약관동의(DB 방식) 오류 코드 (CNS-) */
public enum ConsentErrorCode {

    // ---- 조회
    BIZ_NOT_FOUND(HttpStatus.NOT_FOUND, "CNS-001", "등록되지 않았거나 사용하지 않는 업무코드입니다."),
    TERMS_EMPTY(HttpStatus.NOT_FOUND, "CNS-002", "이 업무에 등록된 약관이 없습니다. 디지털어드민 업무별 약관을 확인하세요."),
    TERMS_NOT_FOUND(HttpStatus.NOT_FOUND, "CNS-003", "약관을 찾을 수 없습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "CNS-004", "요청 값이 올바르지 않습니다."),

    // ---- 동의
    REQUIRED_NOT_AGREED(HttpStatus.BAD_REQUEST, "CNS-101", "필수 약관에 모두 동의해야 다음 단계로 이동할 수 있습니다."),
    TERMS_CHANGED(HttpStatus.CONFLICT, "CNS-102", "약관이 개정되었습니다. 변경된 약관을 다시 확인해 주세요."),
    TERMS_NOT_IN_BIZ(HttpStatus.BAD_REQUEST, "CNS-103", "이 업무의 약관이 아닙니다."),

    // ---- 업무 서버 확인 (checkAgreed)
    AGREE_NOT_FOUND(HttpStatus.FORBIDDEN, "CNS-201", "약관동의 내역이 없습니다. 약관동의부터 진행해 주세요."),
    AGREE_EXPIRED(HttpStatus.FORBIDDEN, "CNS-202", "약관동의 후 시간이 지났습니다. 약관에 다시 동의해 주세요."),

    // ---- ADMIN
    INVALID_VALUE(HttpStatus.BAD_REQUEST, "CNS-301", "입력 값을 확인하세요."),
    EFFECT_DT_INVALID(HttpStatus.BAD_REQUEST, "CNS-302", "개정 약관의 시행일은 이전 버전 시행일(%s)과 같거나 이후여야 합니다."),
    ADMIN_ONLY(HttpStatus.FORBIDDEN, "CNS-401", "약관 관리 권한이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ConsentErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}
