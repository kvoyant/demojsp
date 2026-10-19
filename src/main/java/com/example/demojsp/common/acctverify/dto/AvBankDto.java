package com.example.demojsp.common.acctverify.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 금융기관 (기존 TB_BANK_CODE) - 은행 선택 목록 */
@Getter
@Setter
@NoArgsConstructor
public class AvBankDto {

    private String bankCd;
    private String bankNm;
}
