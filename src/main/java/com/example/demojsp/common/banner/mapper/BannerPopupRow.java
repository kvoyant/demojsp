package com.example.demojsp.common.banner.mapper;

import java.time.LocalDateTime;

/** TB_BANNER_POPUP 1행 (MyBatis 매핑용) */
public class BannerPopupRow {
    private long bannerId;
    private String popupType;
    private String category;
    private String channels;
    private String title;
    private String content;
    private String imageUrl;
    private String altText;
    private String linkUrl;
    private String bgColor;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private int displayOrder;
    private String useYn;
    private LocalDateTime regAt;
    private String regBy;
    private LocalDateTime updAt;
    private String updBy;

    public long getBannerId() { return bannerId; }
    public void setBannerId(long bannerId) { this.bannerId = bannerId; }
    public String getPopupType() { return popupType; }
    public void setPopupType(String popupType) { this.popupType = popupType; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getChannels() { return channels; }
    public void setChannels(String channels) { this.channels = channels; }
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
    public String getBgColor() { return bgColor; }
    public void setBgColor(String bgColor) { this.bgColor = bgColor; }
    public LocalDateTime getStartAt() { return startAt; }
    public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public String getUseYn() { return useYn; }
    public void setUseYn(String useYn) { this.useYn = useYn; }
    public LocalDateTime getRegAt() { return regAt; }
    public void setRegAt(LocalDateTime regAt) { this.regAt = regAt; }
    public String getRegBy() { return regBy; }
    public void setRegBy(String regBy) { this.regBy = regBy; }
    public LocalDateTime getUpdAt() { return updAt; }
    public void setUpdAt(LocalDateTime updAt) { this.updAt = updAt; }
    public String getUpdBy() { return updBy; }
    public void setUpdBy(String updBy) { this.updBy = updBy; }
}
