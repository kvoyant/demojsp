package com.example.demojsp.common.acctverify.gateway;

/** 펌뱅킹 연동 오류 (은행 점검, 통신 실패 등) */
public class FirmBankingException extends RuntimeException {

    public FirmBankingException(String message) {
        super(message);
    }
}
