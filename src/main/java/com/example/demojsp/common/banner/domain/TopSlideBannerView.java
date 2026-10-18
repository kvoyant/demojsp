package com.example.demojsp.common.banner.domain;

/**
 * 홈 상단슬라이드배너 1장 (고객 화면용) - 공통#44 기능3 "배너 구조: 상품소개메인텍스트 + 서브텍스트 + 배너 수".
 *
 * @param id              배너 ID
 * @param mainText        상품소개 메인텍스트 (ADMIN 제목)
 * @param subText         서브텍스트 (ADMIN 내용)
 * @param imageUrl        상품 이미지 URL (없으면 null)
 * @param altText         이미지 대체텍스트
 * @param linkUrl         배너 터치 시 이동 URL (없으면 null → 터치 동작 없음)
 * @param backgroundColor 배경 색상 #RRGGBB
 * @param textColor       배경색 대비가 높은 글자색 (#111827 / #FFFFFF / #000000 중 4.5:1 이상 자동 선택)
 * @param displayOrder    표시순서
 */
public record TopSlideBannerView(
        long id,
        String mainText,
        String subText,
        String imageUrl,
        String altText,
        String linkUrl,
        String backgroundColor,
        String textColor,
        int displayOrder
) {
}
