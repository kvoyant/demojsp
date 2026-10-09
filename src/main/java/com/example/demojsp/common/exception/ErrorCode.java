package com.example.demojsp.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    INVALID_INPUT_VALUE(400, "C001", "적절하지 않은 요청 값입니다."),
    METHOD_NOT_ALLOWED(405, "C002", "지원하지 않는 HTTP 메서드입니다."),
    ENTITY_NOT_FOUND(400, "C003", "대상 데이터를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(500, "C004", "서버 내부 오류가 발생했습니다."),
    INVALID_TYPE_VALUE(400, "C005", "잘못된 타입의 값입니다."),
    HANDLE_ACCESS_DENIED(403, "C006", "접근 권한이 없습니다."),

    // 금융 / 대출 비즈니스 오류
    LOAN_APPLICANT_NOT_FOUND(400, "L001", "대출 신청자 정보를 찾을 수 없습니다."),
    COLLATERAL_EVALUATION_FAILED(400, "L002", "담보 가치 평가 중 오류가 발생했습니다."),
    SCREENING_ALREADY_COMPLETED(400, "S001", "이미 심사가 완료된 대출건입니다."),
    EXCEED_CREDIT_LIMIT(400, "S002", "한도를 초과한 대출 신청입니다."),
    DUPLICATE_APPLY(400, "L003", "중복된 대출 신청입니다.");

    private final int status;
    private final String code;
    private final String message;

    ErrorCode(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
