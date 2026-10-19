package com.example.demojsp.common.product.mapper;

import java.time.LocalDateTime;

/** TB_PRODUCT 1행 (MyBatis 매핑용) */
public class ProductRow {
    private long productId;
    private String productCd;
    private String productKind;
    private String subType;
    private String productNm;
    private String summary;
    private String rateText;
    private String icon;
    private String detailUrl;
    private String onSaleYn;
    private String homeDispYn;
    private Integer homeDispOrd;
    private LocalDateTime updAt;
    private String updBy;

    public long getProductId() { return productId; }
    public void setProductId(long productId) { this.productId = productId; }
    public String getProductCd() { return productCd; }
    public void setProductCd(String productCd) { this.productCd = productCd; }
    public String getProductKind() { return productKind; }
    public void setProductKind(String productKind) { this.productKind = productKind; }
    public String getSubType() { return subType; }
    public void setSubType(String subType) { this.subType = subType; }
    public String getProductNm() { return productNm; }
    public void setProductNm(String productNm) { this.productNm = productNm; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getRateText() { return rateText; }
    public void setRateText(String rateText) { this.rateText = rateText; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getDetailUrl() { return detailUrl; }
    public void setDetailUrl(String detailUrl) { this.detailUrl = detailUrl; }
    public String getOnSaleYn() { return onSaleYn; }
    public void setOnSaleYn(String onSaleYn) { this.onSaleYn = onSaleYn; }
    public String getHomeDispYn() { return homeDispYn; }
    public void setHomeDispYn(String homeDispYn) { this.homeDispYn = homeDispYn; }
    public Integer getHomeDispOrd() { return homeDispOrd; }
    public void setHomeDispOrd(Integer homeDispOrd) { this.homeDispOrd = homeDispOrd; }
    public LocalDateTime getUpdAt() { return updAt; }
    public void setUpdAt(LocalDateTime updAt) { this.updAt = updAt; }
    public String getUpdBy() { return updBy; }
    public void setUpdBy(String updBy) { this.updBy = updBy; }
}
