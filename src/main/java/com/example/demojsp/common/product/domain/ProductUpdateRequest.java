package com.example.demojsp.common.product.domain;

/**
 * 상품설정 기본 항목 수정 (공통#48 기능4 "기존 상품설정 내용을 콘텐츠 각 항목에 연동" - 고치면 홈 카드에 바로 반영).
 *
 * @param name      상품명 (필수, 30자)
 * @param summary   한줄 설명 (40자)
 * @param rateText  금리 문구 (20자)
 * @param detailUrl 상품안내페이지 (필수, 같은 도메인 상대경로)
 * @param onSale    판매여부
 */
public record ProductUpdateRequest(String name, String summary, String rateText, String detailUrl, Boolean onSale) {
}
