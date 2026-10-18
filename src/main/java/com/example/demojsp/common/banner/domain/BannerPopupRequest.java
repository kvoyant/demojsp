package com.example.demojsp.common.banner.domain;

import java.util.List;

/**
 * ADMIN 배너팝업 등록/수정 요청.
 *
 * <p>일시는 "yyyy-MM-dd HH:mm", "yyyy-MM-ddTHH:mm"(input datetime-local), 초 포함 형식 모두 허용.
 *
 * @param popupType    배너팝업구분 (생략 시 BANNER_POPUP)
 * @param category     공지유형 (필수)
 * @param channels     노출 채널 (필수, 1개 이상)
 * @param title        제목 (필수, 최대 50자)
 * @param content      본문 (필수, 최대 1,000자)
 * @param imageUrl     이미지 URL (선택, 상대경로 또는 https)
 * @param altText      대체텍스트 (이미지가 있으면 필수, 최대 100자)
 * @param linkUrl      링크 URL (선택, 상대경로 또는 http/https)
 * @param startAt      게시 시작일시 (필수)
 * @param endAt        게시 종료일시 (필수, 시작보다 늦어야 함)
 * @param displayOrder 표시순서 (필수, 1~999)
 * @param use          사용여부 (생략 시 true)
 */
public record BannerPopupRequest(
        String popupType,
        String category,
        List<String> channels,
        String title,
        String content,
        String imageUrl,
        String altText,
        String linkUrl,
        String startAt,
        String endAt,
        Integer displayOrder,
        Boolean use
) {
}
