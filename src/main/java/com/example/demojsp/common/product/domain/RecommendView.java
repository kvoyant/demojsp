package com.example.demojsp.common.product.domain;

import java.util.List;

/**
 * 홈 추천 상품 (공통#47).
 *
 * @param eligible    제공대상 여부 - 금융회원이면 false → "추천 상품" 영역 전체 숨김 (기능4)
 * @param segment     회원구분 (GUEST / MEMBER / FINANCIAL)
 * @param moreUrl     [더보기] → 전체상품목록(상품몰) (기능2)
 * @param items       추천 상품 (홈 표시순서대로, ADMIN 공통#48 설정)
 */
public record RecommendView(
        boolean eligible,
        String segment,
        String moreUrl,
        List<RecommendItem> items
) {
}
