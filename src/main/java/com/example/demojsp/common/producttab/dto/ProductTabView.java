package com.example.demojsp.common.producttab.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** GET /api/common/product-tab 응답 = 상품 탭 화면 한 장 분량 */
@Getter
@Setter
public class ProductTabView {

    private String channel;                               // APP / PC_WEB
    private LocalDate baseDt;                             // 게시기간 판단 기준일 (보통 오늘)
    private List<PtSlideDto> slides = new ArrayList<>();  // ① 상단 슬라이드 카드
    private List<PtSectionDto> sections = new ArrayList<>(); // ② 섹션 (빈 상품 섹션은 제외)
}
