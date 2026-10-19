package com.example.demojsp.common.producttab.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 상단 슬라이드 카드 1장 (TB_PRD_TAB_SLIDE).
 *
 * <p>사용자 화면 응답에서는 {@link #linkUrl} 이 "실제로 이동할 주소" 로 바뀌어 나간다.
 * (상품이 연결돼 있으면 상품 원장의 상품안내 URL, 아니면 등록한 LINK_URL)
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PtSlideDto {

    private Long slideId;
    private String title;        // 메인 문구 (30자)
    private String subText;      // 서브 문구 (40자)
    private String badgeText;    // 배지 (10자)
    private String bgColor;      // #RRGGBB
    private String textColor;    // 응답 전용: 배경색 대비로 계산한 글자색
    private String imageUrl;
    private String imageAlt;
    private String productCd;    // 연결 상품 (TB_PRODUCT)
    private String linkUrl;
    private String channels;     // "APP,PC_WEB"
    private LocalDate startDt;
    private LocalDate endDt;
    private Integer sortOrd;
    private String useYn;
    private LocalDateTime updAt;
    private String updBy;

    /** 조회용 (TB_PRODUCT 조인): 연결 상품명 · 판매여부 · 상품안내 URL */
    private String productNm;
    private String productOnSaleYn;
    private String productUrl;
}
