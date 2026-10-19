package com.example.demojsp.common.producttab.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** application.yaml {@code common.product-tab.*} - 화면 내용(카드·상품)은 여기 두지 않는다 (전부 DB). */
@ConfigurationProperties(prefix = "common.product-tab")
public class ProductTabProperties {

    /** 상단 슬라이드 카드 최대 노출 수 (0 = 제한 없음). 너무 많으면 고객이 끝까지 넘기지 않는다. */
    private int maxSlides = 5;

    /** local 테스트용 ADMIN API 개방. 운영 false (ROLE_ADMIN 세션 필요) */
    private boolean adminApiOpen = false;

    public int getMaxSlides() { return maxSlides; }
    public void setMaxSlides(int maxSlides) { this.maxSlides = maxSlides; }
    public boolean isAdminApiOpen() { return adminApiOpen; }
    public void setAdminApiOpen(boolean adminApiOpen) { this.adminApiOpen = adminApiOpen; }
}
