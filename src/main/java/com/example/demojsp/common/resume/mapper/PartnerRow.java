package com.example.demojsp.common.resume.mapper;

/** TB_CO_PARTNER 1행 */
public class PartnerRow {
    private String partnerCd;
    private String partnerNm;
    private String logoUrl;
    private String ownYn;

    public String getPartnerCd() { return partnerCd; }
    public void setPartnerCd(String partnerCd) { this.partnerCd = partnerCd; }
    public String getPartnerNm() { return partnerNm; }
    public void setPartnerNm(String partnerNm) { this.partnerNm = partnerNm; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getOwnYn() { return ownYn; }
    public void setOwnYn(String ownYn) { this.ownYn = ownYn; }
}
