package com.example.demojsp.common.acctverify.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 자동완성 후보 1건 = 고객의 대출 입금/출금계좌 (기존 TB_LOAN_ACCOUNT).
 * <p>화면에는 <b>마스킹 계좌번호와 prefillId(대출신청번호)</b>만 보낸다. 그대로 인증하면 서버가 prefillId 로 DB 에서 계좌를 다시 읽으므로
 * 전체 계좌번호가 화면에 노출되지 않는다. [수정]을 누르면 고객이 은행·계좌를 직접 입력한다 (요구사항 2).
 */
@Getter
@Setter
@NoArgsConstructor
public class AvPrefillDto {

    private String prefillId;     // 대출신청번호
    private String productNm;     // 대출상품명
    private String bankCd;
    private String bankNm;
    private String acctMask;
    private String usage;         // 대출금 입금 · 원리금 출금
}
