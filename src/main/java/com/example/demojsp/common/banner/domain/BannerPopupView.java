package com.example.demojsp.common.banner.domain;

import com.example.demojsp.common.banner.service.BannerSupport;

/**
 * 고객 화면(APP/WEB) 노출용 배너 1건. 등록자 등 관리 정보는 내려보내지 않는다.
 *
 * @param id           배너 ID ("오늘 하루 보지 않기" 저장 키)
 * @param category     공지유형 코드
 * @param categoryName 공지유형명 (배지 표시)
 * @param title        제목
 * @param content      본문
 * @param imageUrl     이미지 URL (없으면 null)
 * @param altText      이미지 대체텍스트
 * @param linkUrl      [자세히 보기] URL (없으면 null)
 * @param displayOrder 표시순서
 * @param startAt      게시 시작 (yyyy-MM-dd HH:mm)
 * @param endAt        게시 종료 (yyyy-MM-dd HH:mm)
 */
public record BannerPopupView(
        long id,
        String category,
        String categoryName,
        String title,
        String content,
        String imageUrl,
        String altText,
        String linkUrl,
        int displayOrder,
        String startAt,
        String endAt
) {

    public static BannerPopupView of(BannerPopup b) {
        return new BannerPopupView(b.id(), b.category().name(), b.category().getLabel(), b.title(), b.content(),
                b.imageUrl(), b.altText(), b.linkUrl(), b.displayOrder(),
                BannerSupport.format(b.startAt()), BannerSupport.format(b.endAt()));
    }
}
