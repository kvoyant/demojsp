package com.example.demojsp.common.testhub;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 공통모듈 테스트 허브 목록 (application.yaml 의 {@code common.test-hub.items}).
 *
 * <p><b>요구사항이 추가되면 yaml 에 항목만 추가</b>하면 테스트 페이지(/common/test-hub)에 자동으로 나타난다.
 */
@ConfigurationProperties(prefix = "common.test-hub")
public class TestHubProperties {

    private List<Item> items = new ArrayList<>();

    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }

    /** 요구사항(공통모듈) 1건 */
    public static class Item {
        /** 공통기능 ID (예: CF-BC-04) */
        private String cfId;
        /** 요구사항 ID (예: REQ-AP-602) */
        private String reqId;
        /** 요구사항명 */
        private String title;
        /** 업무분류 (예: 공통 요구사항 > 약관동의) */
        private String category;
        /** 개발 상태 (개발완료 / 개발중 / 대기) */
        private String status = "대기";
        /** 요건 발생일 (yyyy-MM-dd) */
        private String requestedAt;
        /** 적용 채널 (APP, PC WEB, 모집인WEB, ADMIN, Batch) */
        private List<String> channels = new ArrayList<>();
        /** 핵심 기능 요약 */
        private List<String> features = new ArrayList<>();
        /** 테스트 링크 */
        private List<Link> links = new ArrayList<>();
        /** 가이드 문서 경로 (프로젝트 내 상대경로) */
        private String guide;

        public String getCfId() { return cfId; }
        public void setCfId(String cfId) { this.cfId = cfId; }
        public String getReqId() { return reqId; }
        public void setReqId(String reqId) { this.reqId = reqId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getRequestedAt() { return requestedAt; }
        public void setRequestedAt(String requestedAt) { this.requestedAt = requestedAt; }
        public List<String> getChannels() { return channels; }
        public void setChannels(List<String> channels) { this.channels = channels; }
        public List<String> getFeatures() { return features; }
        public void setFeatures(List<String> features) { this.features = features; }
        public List<Link> getLinks() { return links; }
        public void setLinks(List<Link> links) { this.links = links; }
        public String getGuide() { return guide; }
        public void setGuide(String guide) { this.guide = guide; }
    }

    /** 테스트 링크 (type: PAGE = 화면, API = GET API 호출 결과 보기) */
    public static class Link {
        private String label;
        private String url;
        private String type = "PAGE";

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }
}
