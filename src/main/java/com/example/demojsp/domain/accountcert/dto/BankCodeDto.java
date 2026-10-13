package com.example.demojsp.domain.accountcert.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankCodeDto {
    private String bankCd;
    private String bankNm;
    private String bankType;
    private Integer sortOrder;
    private String useYn;
}
