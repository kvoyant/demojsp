package com.example.demojsp.common.resume.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * [CF-BC-13] 이어하기 공통모듈 설정 (application.yaml 의 {@code common.resume.*}).
 *
 * <p>partners = "회사명 DB"(공통#43 기능5) 초기 데이터, seed = 진행 중인 신청 건 초기 데이터.
 * 운영에서는 저장소를 DB(MyBatis) 구현체로 교체하면 둘 다 사용하지 않는다.
 * Lombok 미사용(순수 Getter/Setter): 공통모듈 단독 이관 시 의존성 최소화 (다른 공통모듈과 동일 규칙).
 */
@ConfigurationProperties(prefix = "common.resume")
public class ResumeProperties {

    /**
     * 테스트 API·고객 전환 허용 여부.
     * <p>true: 로그인 없이 {@code demoCustomer} 파라미터로 고객을 바꿔 조회, 테스트 등록/취소 API 사용 (<b>local 전용</b>)
     * <br>false: 로그인 세션의 고객만 조회, 테스트 API 차단.
     */
    private boolean demoApiOpen = false;

    /** local 에서 고객 미지정 시 기본 고객 ID */
    private String demoDefaultCustomer = "customer01";

    /** [더보기] 이동 화면 = 신청현황조회 (공통#43 기능3) */
    private String statusUrl = "/common/resume/status";

    /** 당행 회사코드 (partner-code 생략 시 사용) */
    private String ownPartnerCode = "OWN";

    /** 만료 일수 기본값 (업무가 expire-days 를 주지 않을 때). 0 = 만료 없음 */
    private int defaultExpireDays = 30;

    /** 이어하기 링크 허용 접두어 (상대경로 "/" 외에 APP 딥링크 사용 시 "pepperapp://" 등 추가) */
    private List<String> allowedUrlPrefixes = new ArrayList<>(List.of("/"));

    /** 회사(당행/제휴사) 마스터 */
    private List<PartnerSeed> partners = new ArrayList<>();

    /** 진행 중인 신청 초기 데이터 */
    private List<Seed> seed = new ArrayList<>();

    public boolean isDemoApiOpen() { return demoApiOpen; }
    public void setDemoApiOpen(boolean demoApiOpen) { this.demoApiOpen = demoApiOpen; }
    public String getDemoDefaultCustomer() { return demoDefaultCustomer; }
    public void setDemoDefaultCustomer(String demoDefaultCustomer) { this.demoDefaultCustomer = demoDefaultCustomer; }
    public String getStatusUrl() { return statusUrl; }
    public void setStatusUrl(String statusUrl) { this.statusUrl = statusUrl; }
    public String getOwnPartnerCode() { return ownPartnerCode; }
    public void setOwnPartnerCode(String ownPartnerCode) { this.ownPartnerCode = ownPartnerCode; }
    public int getDefaultExpireDays() { return defaultExpireDays; }
    public void setDefaultExpireDays(int defaultExpireDays) { this.defaultExpireDays = defaultExpireDays; }
    public List<String> getAllowedUrlPrefixes() { return allowedUrlPrefixes; }
    public void setAllowedUrlPrefixes(List<String> allowedUrlPrefixes) { this.allowedUrlPrefixes = allowedUrlPrefixes; }
    public List<PartnerSeed> getPartners() { return partners; }
    public void setPartners(List<PartnerSeed> partners) { this.partners = partners; }
    public List<Seed> getSeed() { return seed; }
    public void setSeed(List<Seed> seed) { this.seed = seed; }

    /** 회사 1건 */
    public static class PartnerSeed {
        private String code;
        private String name;
        private String logoUrl;
        private boolean own;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getLogoUrl() { return logoUrl; }
        public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
        public boolean isOwn() { return own; }
        public void setOwn(boolean own) { this.own = own; }
    }

    /**
     * 진행 중인 신청 1건.
     * <p>날짜가 지나도 샘플이 동작하도록 일시는 <b>기동 시각 기준 상대값</b>으로 지정한다
     * ({@code started-minutes-ago}, {@code last-activity-minutes-ago}, {@code expire-days}).
     */
    public static class Seed {
        private String customerId;
        private String productType;
        private String bizCode;
        private String bizName;
        private String productCode;
        private String productName;
        private String partnerCode;
        private String stepName;
        private int stepNo = 1;
        private int totalSteps;
        private String resumeUrl;
        private String status = "IN_PROGRESS";
        private long startedMinutesAgo = 60;
        private long lastActivityMinutesAgo = 30;
        /** 최초 신청일 기준 만료 일수 (음수로 주면 이미 만료된 건) */
        private Integer expireDays;

        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
        public String getProductType() { return productType; }
        public void setProductType(String productType) { this.productType = productType; }
        public String getBizCode() { return bizCode; }
        public void setBizCode(String bizCode) { this.bizCode = bizCode; }
        public String getBizName() { return bizName; }
        public void setBizName(String bizName) { this.bizName = bizName; }
        public String getProductCode() { return productCode; }
        public void setProductCode(String productCode) { this.productCode = productCode; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public String getPartnerCode() { return partnerCode; }
        public void setPartnerCode(String partnerCode) { this.partnerCode = partnerCode; }
        public String getStepName() { return stepName; }
        public void setStepName(String stepName) { this.stepName = stepName; }
        public int getStepNo() { return stepNo; }
        public void setStepNo(int stepNo) { this.stepNo = stepNo; }
        public int getTotalSteps() { return totalSteps; }
        public void setTotalSteps(int totalSteps) { this.totalSteps = totalSteps; }
        public String getResumeUrl() { return resumeUrl; }
        public void setResumeUrl(String resumeUrl) { this.resumeUrl = resumeUrl; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public long getStartedMinutesAgo() { return startedMinutesAgo; }
        public void setStartedMinutesAgo(long startedMinutesAgo) { this.startedMinutesAgo = startedMinutesAgo; }
        public long getLastActivityMinutesAgo() { return lastActivityMinutesAgo; }
        public void setLastActivityMinutesAgo(long lastActivityMinutesAgo) { this.lastActivityMinutesAgo = lastActivityMinutesAgo; }
        public Integer getExpireDays() { return expireDays; }
        public void setExpireDays(Integer expireDays) { this.expireDays = expireDays; }
    }
}
