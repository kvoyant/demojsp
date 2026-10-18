package com.example.demojsp.common.banner.domain;

/**
 * 배너팝업구분 (ADMIN "마케팅 > 배너관리 > 배너팝업목록" 의 구분값).
 *
 * <p>공통#45 "상단슬라이드배너" 개발 시 {@code TOP_SLIDE} 를 여기에 추가하고 같은 저장소/서비스를 재사용한다.
 */
public enum BannerPopupType {

    BANNER_POPUP("배너팝업");

    private final String label;

    BannerPopupType(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
