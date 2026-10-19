package com.example.demojsp.common.producttab.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 섹션 안의 상품 1건 = TB_PRD_TAB_ITEM(어디에 · 몇 번째 · 배지) + TB_PRODUCT(상품 정보).
 * 상품명·설명·금리·아이콘·URL·판매여부는 상품 원장 값이라 ADMIN 상품설정(공통#48)에서 바꾸면 여기에도 바로 반영된다.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PtItemDto {

    // ---- TB_PRD_TAB_ITEM
    private String sectionCd;
    private String productCd;
    private Integer sortOrd;
    private String badgeText;
    private String useYn;
    private java.time.LocalDateTime updAt;
    private String updBy;

    // ---- TB_PRODUCT (조인)
    private String productNm;
    private String productKind;   // DEPOSIT / LOAN
    private String subType;       // 입출금, 적금, 신용대출 ...
    private String summary;
    private String rateText;
    private String icon;
    private String detailUrl;
    private String onSaleYn;

    public PtItemDto(String productCd, Integer sortOrd, String badgeText, String useYn) {
        this.productCd = productCd;
        this.sortOrd = sortOrd;
        this.badgeText = badgeText;
        this.useYn = useYn;
    }
}
