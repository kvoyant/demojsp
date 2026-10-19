package com.example.demojsp.common.acctverify.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** 1원 송금 완료 응답 - "국민은행 123****890 으로 1원을 보냈습니다. 입금자명 '페퍼' 뒤 숫자 4자리를 입력하세요." */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AvStartResult {

    private final String reqNo;
    private final String bankNm;
    private final String acctMask;
    private final String holderNm;
    private final String memoPrefix;
    private final LocalDateTime expireAt;
    private final int validSec;
    private final int maxTry;
    private final String testCode;     // local 전용 (common.acct-verify.expose-test-code=true) - 운영은 항상 null
}
