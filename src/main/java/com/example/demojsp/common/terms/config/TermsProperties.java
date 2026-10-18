package com.example.demojsp.common.terms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * [CF-BC-04] 약관동의 공통모듈 설정 (application.yaml 의 {@code common.terms.*}).
 *
 * <p><b>seed 는 "ADMIN 약관관리에 이미 등록된 약관" 을 흉내내는 초기 데이터</b>다.
 * 화면/Java 코드에는 약관 코드·문구를 하드코딩하지 않고, 업무코드(bizCode)만 넘겨 조회한다.
 * 운영에서는 {@code TermsRepository} 를 DB(MyBatis) 구현체로 교체하면 seed 는 사용하지 않는다.
 *
 * <p>Lombok 미사용(순수 Getter/Setter): 공통모듈 단독 이관 시 의존성 최소화.
 */
@ConfigurationProperties(prefix = "common.terms")
public class TermsProperties {

    /** 동의 완료 결과의 세션 유효시간 (약관동의 후 업무 처리 완료까지 허용 시간) */
    private Duration agreementTtl = Duration.ofMinutes(30);

    /**
     * 관리(ADMIN) API 개방 여부.
     * <p>true: 로그인 없이 호출 가능 (<b>local 테스트 전용</b>) / false: ROLE_ADMIN 로그인 세션 필요.
     * 운영 yaml 에서는 반드시 false.
     */
    private boolean adminApiOpen = false;

    /** 업무코드 → 업무명 (화면 표시용) */
    private Map<String, String> bizNames = new LinkedHashMap<>();

    /** 초기 약관 데이터 (InMemoryTermsRepository 가 기동 시 적재) */
    private List<Seed> seed = new ArrayList<>();

    public Duration getAgreementTtl() { return agreementTtl; }
    public void setAgreementTtl(Duration agreementTtl) { this.agreementTtl = agreementTtl; }
    public boolean isAdminApiOpen() { return adminApiOpen; }
    public void setAdminApiOpen(boolean adminApiOpen) { this.adminApiOpen = adminApiOpen; }
    public Map<String, String> getBizNames() { return bizNames; }
    public void setBizNames(Map<String, String> bizNames) { this.bizNames = bizNames; }
    public List<Seed> getSeed() { return seed; }
    public void setSeed(List<Seed> seed) { this.seed = seed; }

    /** 약관 1건 초기 데이터 */
    public static class Seed {
        private String bizCode;
        private String code;
        private String title;
        private boolean required;
        private String version = "1.0";
        private int sortOrder;
        private String content;
        /** PDF 원문 URL (선택). 상세보기에서 CF-NA-03 PDF/약관 Viewer 로 연결 */
        private String pdfUrl;

        public String getBizCode() { return bizCode; }
        public void setBizCode(String bizCode) { this.bizCode = bizCode; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public boolean isRequired() { return required; }
        public void setRequired(boolean required) { this.required = required; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public int getSortOrder() { return sortOrder; }
        public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getPdfUrl() { return pdfUrl; }
        public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }
    }
}
