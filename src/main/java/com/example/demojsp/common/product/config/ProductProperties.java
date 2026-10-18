package com.example.demojsp.common.product.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * [공통#47·48] 상품 · 홈 추천 상품 설정 ({@code common.product.*}).
 * seed = ADMIN 수신상품목록 / 여신상품목록 원장 흉내 (운영은 상품 테이블 MyBatis 구현체로 교체).
 */
@ConfigurationProperties(prefix = "common.product")
public class ProductProperties {

    /** ADMIN API 개방 (local 전용, 운영 false = ROLE_ADMIN 세션 필요) */
    private boolean adminApiOpen = false;
    /** [더보기] 이동 = 전체상품목록(상품몰) - 전체메뉴 "전체상품"(공통#46)과 같은 화면 */
    private String mallUrl = "/product/all";
    /** 홈 표시 최대 건수 (공통#48 "수신상품목록&여신상품목록 총 3개까지") */
    private int maxHomeCount = 3;
    private List<Seed> seed = new ArrayList<>();

    public boolean isAdminApiOpen() { return adminApiOpen; }
    public void setAdminApiOpen(boolean adminApiOpen) { this.adminApiOpen = adminApiOpen; }
    public String getMallUrl() { return mallUrl; }
    public void setMallUrl(String mallUrl) { this.mallUrl = mallUrl; }
    public int getMaxHomeCount() { return maxHomeCount; }
    public void setMaxHomeCount(int maxHomeCount) { this.maxHomeCount = maxHomeCount; }
    public List<Seed> getSeed() { return seed; }
    public void setSeed(List<Seed> seed) { this.seed = seed; }

    public static class Seed {
        private String code;
        private String kind;
        private String subType;
        private String name;
        private String summary;
        private String rateText;
        private String icon;
        private String detailUrl;
        private boolean onSale = true;
        private boolean homeDisplay;
        private Integer homeOrder;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getKind() { return kind; }
        public void setKind(String kind) { this.kind = kind; }
        public String getSubType() { return subType; }
        public void setSubType(String subType) { this.subType = subType; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        public String getRateText() { return rateText; }
        public void setRateText(String rateText) { this.rateText = rateText; }
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        public String getDetailUrl() { return detailUrl; }
        public void setDetailUrl(String detailUrl) { this.detailUrl = detailUrl; }
        public boolean isOnSale() { return onSale; }
        public void setOnSale(boolean onSale) { this.onSale = onSale; }
        public boolean isHomeDisplay() { return homeDisplay; }
        public void setHomeDisplay(boolean homeDisplay) { this.homeDisplay = homeDisplay; }
        public Integer getHomeOrder() { return homeOrder; }
        public void setHomeOrder(Integer homeOrder) { this.homeOrder = homeOrder; }
    }
}
