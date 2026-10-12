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
    DUPLICATE_APPLY(400, "L003", "중복된 대출 신청입니다."),

    // 본인인증 (NICE ID) 및 고객 체크 오류
    AUTH_OTP_NOT_FOUND(400, "A001", "인증 요청 정보를 찾을 수 없거나 인증 유효시간이 만료되었습니다."),
    AUTH_OTP_MISMATCH(400, "A002", "인증번호가 일치하지 않습니다. 다시 확인해 주세요."),
    AUTH_TERMS_REQUIRED(400, "A003", "필수 비여신금융거래 및 본인인증 약관에 모두 동의해야 합니다."),
    AUTH_WATCHLIST_BLOCKED(403, "A004", "금융사고 방지 및 금융소비자 보호를 위해 본인인증이 제한된 고객입니다."),
    AUTH_MINOR_RESTRICTED(400, "A005", "만 19세 미만 미성년자는 비여신/여신 거래 시 법정대리인 동의 또는 영업점 방문이 필요합니다."),
    AUTH_FOREIGNER_RESTRICTED(400, "A006", "외국인 고객은 실명확인증표(외국인등록증) 진위확인 등 추가 절차가 필요합니다."),
    AUTH_MO_NOT_RECEIVED(400, "A007", "기기 점유 MO 인증 문자가 아직 수신되지 않았습니다. 문자를 전송 후 다시 확인해 주세요."),
    CI_MIGRATION_FAILED(500, "M001", "CI 데이터 이관 처리 중 오류가 발생했습니다."),

    // 로그인 정책 (개인, 상담사, 관리자 및 권한) 오류
    MEMBER_NOT_FOUND(400, "U001", "등록되지 않은 사용자 아이디입니다."),
    LOGIN_PASSWORD_MISMATCH(400, "U002", "비밀번호가 일치하지 않습니다."),
    LOGIN_ACCOUNT_LOCKED(403, "U003", "비밀번호 5회 연속 오류로 계정이 잠겼습니다. 본인인증(NICE ID) 또는 관리자에게 문의하세요."),
    LOGIN_TWO_FACTOR_REQUIRED(400, "U004", "상담사 및 관리자는 2차 인증(OTP)이 필수입니다."),
    LOGIN_TWO_FACTOR_MISMATCH(400, "U005", "2차 인증번호가 일치하지 않습니다. 다시 확인해 주세요."),
    LOGIN_ACCESS_DENIED(403, "U006", "해당 메뉴 또는 기능에 대한 접근 권한이 없습니다."),
    SESSION_EXPIRED(401, "U007", "세션이 만료되었습니다. 다시 로그인해 주세요."),
    CONCURRENT_LOGIN_BLOCKED(401, "U008", "다른 기기 또는 브라우저에서 로그인하여 현재 접속이 종료되었습니다.");

    private final int status;
    private final String code;
    private final String message;

    ErrorCode(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
