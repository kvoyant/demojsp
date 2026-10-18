package com.example.demojsp.common.banner.domain;

/**
 * 배너팝업구분 (ADMIN "마케팅 > 배너관리 > 배너팝업목록" 의 구분값).
 *
 * <ul>
 *   <li>{@link #BANNER_POPUP} - 메인 진입 배너팝업 (공통#39, 공지 성격)</li>
 *   <li>{@link #TOP_SLIDE} - 홈 상단슬라이드배너 (공통#44 APP / 공통#45 ADMIN, 상품소개 롤링).
 *       기존 메인 "슬라이드 배너팝업" 을 대체한다. 같은 저장소·게시기간·표시순서 규칙을 재사용하고 노출 API 만 분리.</li>
 * </ul>
 */
public enum BannerPopupType {

    BANNER_POPUP("배너팝업"),
    TOP_SLIDE("상단슬라이드배너");

    private final String label;

    BannerPopupType(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
