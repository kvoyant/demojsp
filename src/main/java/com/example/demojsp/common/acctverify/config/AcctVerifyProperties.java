package com.example.demojsp.common.acctverify.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** application.yaml {@code common.acct-verify.*} - 업무별 기준(화면·횟수·시간)은 여기 두지 않는다 (전부 DB). */
@ConfigurationProperties(prefix = "common.acct-verify")
public class AcctVerifyProperties {

    /** 펌뱅킹 연동 구현: mock(시뮬레이터, 기본) / 실연동 구현체를 만들면 그 값 */
    private String gateway = "mock";

    /** local 전용: 시뮬레이터 인증코드를 응답에 포함 (운영 반드시 false) */
    private boolean exposeTestCode = false;

    /** local 전용: 로그인 없이 ?demoCustomer=&demoName= 으로 고객 흉내 (운영 반드시 false) */
    private boolean demoApiOpen = false;

    /** 같은 세션이 10분 동안 보낼 수 있는 1원 송금 요청 수 (송금 비용·악용 방지) */
    private int maxRequestsPer10Min = 5;

    /** local 테스트용 ADMIN API 개방. 운영 false */
    private boolean adminApiOpen = false;

    public String getGateway() { return gateway; }
    public void setGateway(String gateway) { this.gateway = gateway; }
    public boolean isExposeTestCode() { return exposeTestCode; }
    public void setExposeTestCode(boolean exposeTestCode) { this.exposeTestCode = exposeTestCode; }
    public boolean isDemoApiOpen() { return demoApiOpen; }
    public void setDemoApiOpen(boolean demoApiOpen) { this.demoApiOpen = demoApiOpen; }
    public int getMaxRequestsPer10Min() { return maxRequestsPer10Min; }
    public void setMaxRequestsPer10Min(int maxRequestsPer10Min) { this.maxRequestsPer10Min = maxRequestsPer10Min; }
    public boolean isAdminApiOpen() { return adminApiOpen; }
    public void setAdminApiOpen(boolean adminApiOpen) { this.adminApiOpen = adminApiOpen; }
}
