package com.example.demojsp.common.producttab.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** [ADMIN] 상품 선택 상자용 상품 원장 요약 (TB_PRODUCT) */
@Getter
@Setter
@NoArgsConstructor
public class PtProductDto {

    private String productCd;
    private String productNm;
    private String productKind;
    private String subType;
    private String onSaleYn;
}
