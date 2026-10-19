package com.example.demojsp.common.acctverify.exception;

import org.springframework.http.HttpStatus;

/** 1원 계좌인증 오류 코드 (AVF-) */
public enum AcctVerifyErrorCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "AVF-001", "요청 값이 올바르지 않습니다."),
    BIZ_NOT_FOUND(HttpStatus.NOT_FOUND, "AVF-002", "등록되지 않았거나 사용하지 않는 계좌인증 업무입니다."),
    REQ_NOT_FOUND(HttpStatus.NOT_FOUND, "AVF-003", "계좌인증 요청을 찾을 수 없습니다. 처음부터 다시 진행해 주세요."),
    ALREADY_DONE(HttpStatus.CONFLICT, "AVF-004", "이미 처리된 계좌인증입니다."),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "AVF-005", "1원 송금 요청이 너무 많습니다. 잠시 후 다시 시도해 주세요."),
    EXPIRED(HttpStatus.BAD_REQUEST, "AVF-006", "인증 시간이 지났습니다. 1원을 다시 받아 주세요."),
    CODE_MISMATCH(HttpStatus.BAD_REQUEST, "AVF-007", "입금자명 숫자가 일치하지 않습니다. (%d/%d)"),
    TRY_EXCEEDED(HttpStatus.BAD_REQUEST, "AVF-008", "%d회 잘못 입력했습니다. 처음부터 다시 진행해 주세요."),
    INVALID_ACCOUNT(HttpStatus.BAD_REQUEST, "AVF-102", "은행과 계좌번호를 확인하세요."),
    PREFILL_NOT_FOUND(HttpStatus.BAD_REQUEST, "AVF-103", "불러온 계좌를 찾을 수 없습니다. 계좌를 직접 입력해 주세요."),
    NO_ACCOUNT(HttpStatus.BAD_REQUEST, "AVF-104", "계좌를 찾을 수 없습니다. 은행과 계좌번호를 확인하세요."),
    NOT_OWNER(HttpStatus.BAD_REQUEST, "AVF-105", "본인 명의 계좌만 인증할 수 있습니다."),
    TRANSFER_FAIL(HttpStatus.BAD_GATEWAY, "AVF-201", "1원 송금에 실패했습니다. 잠시 후 다시 시도하거나 다른 계좌를 이용해 주세요."),
    CUSTOMER_REQUIRED(HttpStatus.UNAUTHORIZED, "AVF-101", "고객 확인이 필요합니다. 로그인 또는 본인인증 후 진행해 주세요."),
    NOT_VERIFIED(HttpStatus.FORBIDDEN, "AVF-301", "계좌인증이 완료되지 않았습니다."),
    RESULT_EXPIRED(HttpStatus.FORBIDDEN, "AVF-302", "계좌인증 후 시간이 지났습니다. 다시 인증해 주세요."),
    INVALID_VALUE(HttpStatus.BAD_REQUEST, "AVF-401", "입력 값을 확인하세요."),
    ADMIN_ONLY(HttpStatus.FORBIDDEN, "AVF-402", "계좌인증 관리 권한이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    AcctVerifyErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}
