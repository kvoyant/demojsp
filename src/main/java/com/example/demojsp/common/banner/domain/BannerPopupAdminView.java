package com.example.demojsp.common.banner.domain;

import com.example.demojsp.common.banner.service.BannerSupport;

import java.util.List;

/**
 * ADMIN 배너팝업목록 1행.
 *
 * @param id             배너 ID
 * @param popupType      배너팝업구분 코드
 * @param popupTypeName  배너팝업구분명
 * @param category       공지유형 코드
 * @param categoryName   공지유형명
 * @param channels       노출 채널 코드
 * @param title          제목
 * @param content        본문
 * @param imageUrl       이미지 URL
 * @param altText        대체텍스트
 * @param linkUrl        링크 URL
 * @param startAt        게시 시작 (yyyy-MM-dd HH:mm)
 * @param endAt          게시 종료 (yyyy-MM-dd HH:mm)
 * @param displayOrder   표시순서
 * @param use            사용여부
 * @param status         게시상태 (게시중 / 게시예정 / 게시종료 / 미사용)
 * @param overlapCount   같은 채널에서 게시기간이 겹치는 다른 사용중 배너 수 → 1 이상이면 고객 화면에 슬라이드로 함께 노출
 * @param orderConflict  겹치는 배너 중 표시순서가 같은 배너가 있음 (등록은 허용, 정렬은 최신 시작일시 우선)
 * @param registeredAt   등록일시
 * @param registeredBy   등록자
 * @param updatedAt      수정일시
 * @param updatedBy      수정자
 */
public record BannerPopupAdminView(
        long id,
        String popupType,
        String popupTypeName,
        String category,
        String categoryName,
        List<String> channels,
        String title,
        String content,
        String imageUrl,
        String altText,
        String linkUrl,
        String startAt,
        String endAt,
        int displayOrder,
        boolean use,
        String status,
        int overlapCount,
        boolean orderConflict,
        String registeredAt,
        String registeredBy,
        String updatedAt,
        String updatedBy
) {

    public static BannerPopupAdminView of(BannerPopup b, String status, int overlapCount, boolean orderConflict) {
        return new BannerPopupAdminView(b.id(), b.popupType().name(), b.popupType().getLabel(),
                b.category().name(), b.category().getLabel(),
                b.channels().stream().map(Enum::name).toList(),
                b.title(), b.content(), b.imageUrl(), b.altText(), b.linkUrl(),
                BannerSupport.format(b.startAt()), BannerSupport.format(b.endAt()),
                b.displayOrder(), b.use(), status, overlapCount, orderConflict,
                BannerSupport.formatFull(b.registeredAt()), b.registeredBy(),
                BannerSupport.formatFull(b.updatedAt()), b.updatedBy());
    }
}
