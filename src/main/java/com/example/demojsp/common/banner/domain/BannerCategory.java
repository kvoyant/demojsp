package com.example.demojsp.common.banner.domain;

/**
 * 배너 공지유형. 요구사항(공통#39) 기능설명의 "일반 긴급공지, 시스템 긴급공지 등" 을 반영.
 *
 * <p>공지유형 TO-BE(REQ-AP-609 검토필요)가 확정되면 이 enum 만 조정한다. 노출 순서는 유형이 아니라
 * ADMIN 이 입력한 <b>표시순서</b>로만 결정한다 (유형별 자동 우선순위 없음).
 */
public enum BannerCategory {

    EMERGENCY_SYSTEM("시스템 긴급공지"),
    EMERGENCY_GENERAL("일반 긴급공지"),
    NOTICE("일반 공지"),
    EVENT("이벤트·상품안내");

    private final String label;

    BannerCategory(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
