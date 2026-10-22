package com.example.demojsp.common.platformlink.dto;

import lombok.Data;

/** TB_LNK_PRODUCT - CSS 상품유형 ↔ 당행 상품 */
@Data
public class LnkProductDto {
    private String cssProduct;
    private String productCd;
    private String productNm;
    private String adminList;
    private String useYn;
}
