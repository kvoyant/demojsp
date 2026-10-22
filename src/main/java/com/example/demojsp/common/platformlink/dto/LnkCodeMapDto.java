package com.example.demojsp.common.platformlink.dto;

import lombok.Data;

/** TB_LNK_CODE_MAP - 내부 결과코드 → 제휴사 결과코드 */
@Data
public class LnkCodeMapDto {
    private String partnerCd;
    private String innerCd;
    private String outerCd;
    private String outerMsg;
}
