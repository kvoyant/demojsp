package com.example.demojsp.common.platformlink.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * application.yaml {@code common.platform-link.*} - 시스템 기술값만 둔다.
 * 제휴사별 값(타임아웃·엔진·허용상품·결과코드)은 전부 DB(TB_LNK_*) - 운영 중 ADMIN 에서 바꾼다.
 */
@ConfigurationProperties(prefix = "common.platform-link")
public class PlatformLinkProperties {

    /**
     * 제휴사 서명키 (TB_LNK_PARTNER.SECRET_REF → 키).
     * 키 원문을 DB 에 두지 않기 위한 구조. 운영은 반드시 환경변수/비밀관리 도구로 주입: TOSS: ${LINK_SECRET_TOSS}
     */
    private Map<String, String> secrets = new LinkedHashMap<>();

    /** 서명 타임스탬프 허용 오차(초) - 재전송 공격 방지 */
    private int signatureSkewSec = 300;

    /** CSS 호출 스레드 수 (제휴사 동시 요청 수 고려) */
    private int cssPoolSize = 16;

    /** 연속 실패 n 회면 차단기 열림 → 열린 동안은 CSS 를 부르지 않고 즉시 "일시 오류" 응답 */
    private int circuitFailures = 5;

    /** 차단기 열림 유지(초) - 지나면 다시 시도 */
    private int circuitOpenSec = 30;

    /** [시뮬레이터] 현행 CSS 평균 응답시간(ms) */
    private int legacyLatencyMs = 400;

    /** [시뮬레이터] 신규 CSS 평균 응답시간(ms) */
    private int newLatencyMs = 150;

    /** [시뮬레이터] 고객명에 '지연' 이 들어가면 이만큼 늦게 응답 (타임아웃 시험) */
    private int slowLatencyMs = 8000;

    /** local 전용: 샘플 화면의 "제휴사 흉내" API (서명을 서버가 대신 만들어 줌). 운영 반드시 false */
    private boolean demoApiOpen = false;

    /** local 테스트용 ADMIN API 개방. 운영 false */
    private boolean adminApiOpen = false;

    public Map<String, String> getSecrets() { return secrets; }
    public void setSecrets(Map<String, String> secrets) { this.secrets = secrets; }
    public int getSignatureSkewSec() { return signatureSkewSec; }
    public void setSignatureSkewSec(int signatureSkewSec) { this.signatureSkewSec = signatureSkewSec; }
    public int getCssPoolSize() { return cssPoolSize; }
    public void setCssPoolSize(int cssPoolSize) { this.cssPoolSize = cssPoolSize; }
    public int getCircuitFailures() { return circuitFailures; }
    public void setCircuitFailures(int circuitFailures) { this.circuitFailures = circuitFailures; }
    public int getCircuitOpenSec() { return circuitOpenSec; }
    public void setCircuitOpenSec(int circuitOpenSec) { this.circuitOpenSec = circuitOpenSec; }
    public int getLegacyLatencyMs() { return legacyLatencyMs; }
    public void setLegacyLatencyMs(int legacyLatencyMs) { this.legacyLatencyMs = legacyLatencyMs; }
    public int getNewLatencyMs() { return newLatencyMs; }
    public void setNewLatencyMs(int newLatencyMs) { this.newLatencyMs = newLatencyMs; }
    public int getSlowLatencyMs() { return slowLatencyMs; }
    public void setSlowLatencyMs(int slowLatencyMs) { this.slowLatencyMs = slowLatencyMs; }
    public boolean isDemoApiOpen() { return demoApiOpen; }
    public void setDemoApiOpen(boolean demoApiOpen) { this.demoApiOpen = demoApiOpen; }
    public boolean isAdminApiOpen() { return adminApiOpen; }
    public void setAdminApiOpen(boolean adminApiOpen) { this.adminApiOpen = adminApiOpen; }
}
