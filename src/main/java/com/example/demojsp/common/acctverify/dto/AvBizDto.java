package com.example.demojsp.common.acctverify.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 1원 계좌인증 업무 Context 1건 (TB_CO_ACCT_BIZ).
 * 업무 화면은 bizCd 만 넘기고, 어떤 화면(LOAN/COMMON)·자동완성·횟수·시간을 쓸지는 이 행(DB)이 정한다.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AvBizDto {

    public static final String LOAN = "LOAN";
    public static final String COMMON = "COMMON";

    private String bizCd;
    private String bizNm;
    private String screenType;       // LOAN 대출 입출금계좌용 / COMMON 타 업무 공통
    private String prefillYn;        // 대출금 입금/출금계좌 자동완성
    private String ownerCheckYn;     // 예금주 = 본인 확인
    private String memoPrefix;       // 1원 입금자명 앞 글자
    private Integer codeValidMin;
    private Integer maxTry;
    private Integer resultValidMin;
    private String useYn;
    private LocalDateTime updAt;
    private String updBy;
}
