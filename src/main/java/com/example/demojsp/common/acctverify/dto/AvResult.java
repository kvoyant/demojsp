package com.example.demojsp.common.acctverify.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 1원 계좌인증 결과.
 * 화면 응답에는 마스킹 계좌만 나가고, 업무 서버는 {@code acctVerifyService.checkVerified(...)} 로 전체 계좌번호(getAcctNo)를 받는다.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AvResult {

    private final String reqNo;
    private final String bizCd;
    private final String status;       // SENT / VERIFIED / FAILED / EXPIRED
    private final String resultCd;
    private final String message;
    private final String bankCd;
    private final String bankNm;
    private final String acctMask;
    private final String holderNm;
    private final String source;
    private final String modifiedYn;
    private final LocalDateTime doneAt;

    @JsonIgnore private final String acctNo;     // 서버 전용

    @JsonIgnore
    public boolean isVerified() {
        return "VERIFIED".equals(status);
    }
}
