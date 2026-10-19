package com.example.demojsp.common.niceid.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yaml {@code common.niceid.*} - 업무별 정책(나이·외국인·유의고객·시간·횟수)은 여기 두지 않는다 (전부 DB).
 */
@ConfigurationProperties(prefix = "common.niceid")
public class NiceIdProperties {

    /** NICE 연동 구현 선택: mock(시뮬레이터, 기본) / nice(실연동 구현체를 만들었을 때) */
    private String gateway = "mock";

    /** local 전용: 시뮬레이터 인증번호를 응답에 넣어 화면이 자동입력 (운영 반드시 false) */
    private boolean exposeTestCode = false;

    /** local 전용: MO 문자 발송 흉내 API 허용 (운영 반드시 false) */
    private boolean demoApiOpen = false;

    /** 같은 세션이 10분 동안 보낼 수 있는 인증 요청 수 (문자 폭탄·비용 방지) */
    private int maxRequestsPer10Min = 10;

    /** local 테스트용 ADMIN API 개방. 운영 false (ROLE_ADMIN 세션 필요) */
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
