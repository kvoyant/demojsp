package com.example.demojsp.common.resume.mapper;

import java.time.LocalDateTime;

/** TB_RESUME_POINT 1행 (MyBatis 매핑용 - 도메인 변환은 MyBatisResumeRepository) */
public class ResumePointRow {
    private long resumeId;
    private String customerId;
    private String productType;
    private String bizCd;
    private String bizNm;
    private String productCd;
    private String productNm;
    private String partnerCd;
    private String stepNm;
    private int stepNo;
    private int totalSteps;
    private String resumeUrl;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime lastActivityAt;
    private LocalDateTime expiresAt;

    public long getResumeId() { return resumeId; }
    public void setResumeId(long resumeId) { this.resumeId = resumeId; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }
    public String getBizCd() { return bizCd; }
    public void setBizCd(String bizCd) { this.bizCd = bizCd; }
    public String getBizNm() { return bizNm; }
    public void setBizNm(String bizNm) { this.bizNm = bizNm; }
    public String getProductCd() { return productCd; }
    public void setProductCd(String productCd) { this.productCd = productCd; }
    public String getProductNm() { return productNm; }
    public void setProductNm(String productNm) { this.productNm = productNm; }
    public String getPartnerCd() { return partnerCd; }
    public void setPartnerCd(String partnerCd) { this.partnerCd = partnerCd; }
    public String getStepNm() { return stepNm; }
    public void setStepNm(String stepNm) { this.stepNm = stepNm; }
    public int getStepNo() { return stepNo; }
    public void setStepNo(int stepNo) { this.stepNo = stepNo; }
    public int getTotalSteps() { return totalSteps; }
    public void setTotalSteps(int totalSteps) { this.totalSteps = totalSteps; }
    public String getResumeUrl() { return resumeUrl; }
    public void setResumeUrl(String resumeUrl) { this.resumeUrl = resumeUrl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getLastActivityAt() { return lastActivityAt; }
    public void setLastActivityAt(LocalDateTime lastActivityAt) { this.lastActivityAt = lastActivityAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
