package com.example.demojsp.common.banner.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * 배너팝업 1건 (ADMIN 배너팝업목록의 원장 데이터).
 *
 * <p>게시기간은 <b>시작 포함, 종료 미포함</b> ({@code startAt <= 기준시각 < endAt}) 으로 판단한다.
 * (상단슬라이드배너 요건의 "YYYY-MM-DD 00:00:00 ~ YYYY-MM-DD 00:00:00" 표기와 같은 규칙)
 *
 * @param id           배너 ID
 * @param popupType    배너팝업구분
 * @param category     공지유형
 * @param channels     노출 채널
 * @param title        제목
 * @param content      본문 (텍스트, 줄바꿈 허용)
 * @param imageUrl     이미지 URL (선택)
 * @param altText      이미지 대체텍스트 (이미지가 있으면 필수 - 접근성 CF-UI-02)
 * @param linkUrl      [자세히 보기] 이동 URL (선택)
 * @param startAt      게시 시작일시 (포함)
 * @param endAt        게시 종료일시 (미포함)
 * @param displayOrder 표시순서 (1 이 가장 먼저)
 * @param use          사용여부
 * @param registeredAt 등록일시
 * @param registeredBy 등록자
 * @param updatedAt    수정일시
 * @param updatedBy    수정자
 */
public record BannerPopup(
        long id,
        BannerPopupType popupType,
        BannerCategory category,
        List<BannerChannel> channels,
        String title,
        String content,
        String imageUrl,
        String altText,
        String linkUrl,
        LocalDateTime startAt,
        LocalDateTime endAt,
        int displayOrder,
        boolean use,
        LocalDateTime registeredAt,
        String registeredBy,
        LocalDateTime updatedAt,
        String updatedBy
) implements Serializable {

    /**
     * 노출 정렬 규칙 (공통#39 요구사항 1 "표시순서 적용").
     * <ol>
     *   <li>표시순서 오름차순</li>
     *   <li>표시순서가 같으면 게시 시작일시가 늦은(최신) 배너 먼저</li>
     *   <li>그래도 같으면 배너 ID 오름차순 (항상 같은 결과가 나오도록)</li>
     * </ol>
     */
    public static final Comparator<BannerPopup> DISPLAY_ORDER = Comparator
            .comparingInt(BannerPopup::displayOrder)
            .thenComparing(BannerPopup::startAt, Comparator.reverseOrder())
            .thenComparingLong(BannerPopup::id);

    /** 기준시각에 게시기간 안인지 (사용여부·채널 무관) */
    public boolean inPeriod(LocalDateTime at) {
        return !at.isBefore(startAt) && at.isBefore(endAt);
    }

    /** 기준시각·채널에 고객에게 노출 대상인지 */
    public boolean displayableAt(LocalDateTime at, BannerChannel channel) {
        return use && channels.contains(channel) && inPeriod(at);
    }

    /** 두 배너의 게시기간이 겹치는지 */
    public boolean periodOverlaps(BannerPopup other) {
        return startAt.isBefore(other.endAt) && other.startAt.isBefore(endAt);
    }

    /** 노출 채널이 하나라도 겹치는지 */
    public boolean channelOverlaps(BannerPopup other) {
        return channels.stream().anyMatch(other.channels::contains);
    }
}
