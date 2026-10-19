package com.example.demojsp.common.menu.mapper;

import java.time.LocalDateTime;

/** TB_BANKING_MENU 1행 (MyBatis 매핑용) */
public class BankingMenuRow {
    private long menuId;
    private String categoryCd;
    private String categoryNm;
    private String menuNm;
    private String menuUrl;
    private int displayOrder;
    private String useYn;
    private String newYn;
    private LocalDateTime updAt;
    private String updBy;

    public long getMenuId() { return menuId; }
    public void setMenuId(long menuId) { this.menuId = menuId; }
    public String getCategoryCd() { return categoryCd; }
    public void setCategoryCd(String categoryCd) { this.categoryCd = categoryCd; }
    public String getCategoryNm() { return categoryNm; }
    public void setCategoryNm(String categoryNm) { this.categoryNm = categoryNm; }
    public String getMenuNm() { return menuNm; }
    public void setMenuNm(String menuNm) { this.menuNm = menuNm; }
    public String getMenuUrl() { return menuUrl; }
    public void setMenuUrl(String menuUrl) { this.menuUrl = menuUrl; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public String getUseYn() { return useYn; }
    public void setUseYn(String useYn) { this.useYn = useYn; }
    public String getNewYn() { return newYn; }
    public void setNewYn(String newYn) { this.newYn = newYn; }
    public LocalDateTime getUpdAt() { return updAt; }
    public void setUpdAt(LocalDateTime updAt) { this.updAt = updAt; }
    public String getUpdBy() { return updBy; }
    public void setUpdBy(String updBy) { this.updBy = updBy; }
}
