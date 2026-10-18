package com.example.demojsp.common.banner.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * [CF-BC-20] 배너팝업 공통모듈 설정 (application.yaml 의 {@code common.banner-popup.*}).
 *
 * <p><b>seed 는 "디지털어드민 배너팝업목록에 이미 등록된 배너" 를 흉내내는 초기 데이터</b>다.
 * 운영에서는 {@code BannerPopupRepository} 를 DB(MyBatis) 구현체로 교체하면 seed 는 사용하지 않는다.
 *
 * <p>Lombok 미사용(순수 Getter/Setter): 공통모듈 단독 이관 시 의존성 최소화 (약관동의 모듈과 동일 규칙).
 */
@ConfigurationProperties(prefix = "common.banner-popup")
public class BannerPopupProperties {

    /**
     * 관리(ADMIN) API 개방 여부.
     * <p>true: 로그인 없이 호출 가능 (<b>local 테스트 전용</b>) / false: ROLE_ADMIN 로그인 세션 필요.
     */
    private boolean adminApiOpen = false;

    /**
     * 한 번에 노출할 최대 배너 수 (표시순서 상위 N건). 0 이하 = 제한 없음.
     * <p>요구사항(공통#39)에 상한은 없음 → 고객 피로도를 고려한 수행사 제안값. 업무 확정 후 조정.
     */
    private int maxDisplayCount = 10;

    /** 초기 배너 데이터 (InMemoryBannerPopupRepository 가 기동 시 적재) */
    private List<Seed> seed = new ArrayList<>();

    public boolean isAdminApiOpen() { return adminApiOpen; }
    public void setAdminApiOpen(boolean adminApiOpen) { this.adminApiOpen = adminApiOpen; }
    public int getMaxDisplayCount() { return maxDisplayCount; }
    public void setMaxDisplayCount(int maxDisplayCount) { this.maxDisplayCount = maxDisplayCount; }
    public List<Seed> getSeed() { return seed; }
    public void setSeed(List<Seed> seed) { this.seed = seed; }

    /**
     * 배너 1건 초기 데이터.
     * <p>게시기간은 날짜가 지나도 샘플이 계속 동작하도록 <b>기동일 00:00 기준 상대 일수</b>({@code start-day-offset},
     * {@code end-day-offset})로 지정한다. 절대 일시가 필요하면 {@code start-at}/{@code end-at} (yyyy-MM-dd HH:mm) 사용.
     */
    public static class Seed {
        private String popupType = "BANNER_POPUP";
        private String category;
        private List<String> channels = new ArrayList<>(List.of("APP"));
        private String title;
        private String content;
        private String imageUrl;
        private String altText;
        private String linkUrl;
        private Integer startDayOffset;
        private Integer endDayOffset;
        private String startAt;
        private String endAt;
        private int displayOrder = 1;
        private boolean use = true;

        public String getPopupType() { return popupType; }
        public void setPopupType(String popupType) { this.popupType = popupType; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public List<String> getChannels() { return channels; }
        public void setChannels(List<String> channels) { this.channels = channels; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public String getAltText() { return altText; }
        public void setAltText(String altText) { this.altText = altText; }
        public String getLinkUrl() { return linkUrl; }
        public void setLinkUrl(String linkUrl) { this.linkUrl = linkUrl; }
        public Integer getStartDayOffset() { return startDayOffset; }
        public void setStartDayOffset(Integer startDayOffset) { this.startDayOffset = startDayOffset; }
        public Integer getEndDayOffset() { return endDayOffset; }
        public void setEndDayOffset(Integer endDayOffset) { this.endDayOffset = endDayOffset; }
        public String getStartAt() { return startAt; }
        public void setStartAt(String startAt) { this.startAt = startAt; }
        public String getEndAt() { return endAt; }
        public void setEndAt(String endAt) { this.endAt = endAt; }
        public int getDisplayOrder() { return displayOrder; }
        public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
        public boolean isUse() { return use; }
        public void setUse(boolean use) { this.use = use; }
    }
}
