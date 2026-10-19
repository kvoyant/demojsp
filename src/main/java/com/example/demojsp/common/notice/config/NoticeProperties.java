package com.example.demojsp.common.notice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** application.yaml {@code common.notice.*} - 공지 내용은 여기 두지 않는다 (전부 DB). */
@ConfigurationProperties(prefix = "common.notice")
public class NoticeProperties {

    /** 목록 한 페이지 건수 */
    private int pageSize = 10;

    /** 메인 띠를 눌렀을 때 이동할 공지 상세 화면 (뒤에 ?id=공지ID 가 붙는다) */
    private String detailUrl = "/common/notice/detail";

    /** local 테스트용 ADMIN API 개방. 운영 false (ROLE_ADMIN 세션 필요) */
    private boolean adminApiOpen = false;

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    public String getDetailUrl() { return detailUrl; }
    public void setDetailUrl(String detailUrl) { this.detailUrl = detailUrl; }
    public boolean isAdminApiOpen() { return adminApiOpen; }
    public void setAdminApiOpen(boolean adminApiOpen) { this.adminApiOpen = adminApiOpen; }
}
