package com.example.demojsp.common.consent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yaml {@code common.consent.*} - 약관 내용은 여기 두지 않는다 (전부 DB).
 */
@ConfigurationProperties(prefix = "common.consent")
public class ConsentProperties {

    /** 약관동의 후 업무 처리(checkAgreed)까지 허용 시간(분). 0 = 제한 없음 */
    private int validMinutes = 30;

    /** local 테스트용 ADMIN API 개방. 운영 false (ROLE_ADMIN 세션 필요) */
    private boolean adminApiOpen = false;

    public int getValidMinutes() { return validMinutes; }
    public void setValidMinutes(int validMinutes) { this.validMinutes = validMinutes; }
    public boolean isAdminApiOpen() { return adminApiOpen; }
    public void setAdminApiOpen(boolean adminApiOpen) { this.adminApiOpen = adminApiOpen; }
}
