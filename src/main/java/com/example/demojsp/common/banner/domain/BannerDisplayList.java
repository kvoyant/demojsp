package com.example.demojsp.common.banner.domain;

import java.util.List;

/**
 * 고객 화면 노출 대상 배너 목록 (표시순서 정렬 완료).
 *
 * @param channel    조회 채널
 * @param baseAt     기준시각 (yyyy-MM-dd HH:mm:ss)
 * @param total      노출 건수
 * @param slide      2건 이상이면 true → 화면은 좌우 슬라이드로 표시 (공통#39 요구사항 2)
 * @param truncated  최대 노출 건수(max-display-count) 초과로 잘린 건수
 * @param items      노출 배너 (표시순서대로)
 */
public record BannerDisplayList(
        String channel,
        String baseAt,
        int total,
        boolean slide,
        int truncated,
        List<BannerPopupView> items
) {
}
