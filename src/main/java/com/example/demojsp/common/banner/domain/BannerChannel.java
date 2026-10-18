package com.example.demojsp.common.banner.domain;

/**
 * 노출 채널. 공통#39 의 적용 대상은 APP 메인이지만, 콘텐츠 공통 모델(CF-BC-20) 재사용을 위해 채널을 분리해 둔다.
 */
public enum BannerChannel {

    APP("APP"),
    PC_WEB("PC WEB"),
    RECRUIT_WEB("모집인WEB");

    private final String label;

    BannerChannel(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
