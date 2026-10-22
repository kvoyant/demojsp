package com.example.demojsp.common.platformlink.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Arrays;

/** TB_LNK_PARTNER - 제휴사 연계 설정 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LnkPartnerDto {
    private String partnerCd;
    private String partnerNm;
    private String adapterCd;
    /** LEGACY / NEW / SHADOW */
    private String cssEngine;
    /** 쉼표 구분 CREDIT,AUTO,MORTGAGE */
    private String products;
    private Integer timeoutMs;
    private Integer marginMs;
    private String allowIps;
    private String secretRef;
    private String useYn;
    private LocalDateTime updAt;
    private String updBy;

    public boolean allows(String cssProduct) {
        return products != null && Arrays.stream(products.split(",")).map(String::strip).anyMatch(cssProduct::equals);
    }

    public boolean allowsIp(String ip) {
        if (allowIps == null || allowIps.isBlank()) return false;
        return Arrays.stream(allowIps.split(",")).map(String::strip).anyMatch(a -> "*".equals(a) || a.equals(ip));
    }
}
