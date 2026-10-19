package com.example.demojsp.common.producttab.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 섹션(카드 영역) 1개 (TB_PRD_TAB_SECTION).
 * <ul>
 *   <li>PRODUCT : 제목 + 상품 목록 (items)</li>
 *   <li>CARD    : 아이콘 + 제목 + 설명 → linkUrl 로 이동하는 서비스 카드 (금융계산기, AS-IS 시세조회 등)</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PtSectionDto {

    public static final String TYPE_PRODUCT = "PRODUCT";
    public static final String TYPE_CARD = "CARD";

    private String sectionCd;
    private String sectionNm;
    private String sectionDesc;
    private String sectionType;
    private String cardIcon;
    private String linkUrl;
    private Integer sortOrd;
    private String useYn;
    private LocalDateTime updAt;
    private String updBy;

    /** 조회용: ADMIN 목록의 연결 상품 수 */
    private Integer itemCnt;

    /** 사용자 화면: 이 섹션에 보여줄 상품 (PRODUCT 섹션만) */
    private List<PtItemDto> items = new ArrayList<>();
}
