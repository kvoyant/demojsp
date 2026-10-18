package com.example.demojsp.common.banner.domain;

import java.util.List;

/**
 * 홈 상단슬라이드배너 노출 결과 (공통#44).
 *
 * @param channel     조회 채널
 * @param baseAt      기준시각 (yyyy-MM-dd HH:mm:ss)
 * @param eligible    제공대상 여부. false 면 배너 대신 같은 영역에 다른 정보(계좌정보)를 보여준다
 * @param audience    대상 구분: NO_ACCOUNT(계좌 미보유 - 배너 노출) / ACCOUNT_HOLDER(계좌 보유 - 배너 미노출)
 * @param total       노출 배너 수 (최대 maxCount). 2~4건이면 그 수만큼만 슬롯·인디케이터를 만든다 (빈 슬롯 숨김)
 * @param maxCount    최대 노출 건수 (기능1: 5)
 * @param intervalMs  자동 슬라이딩 간격 ms (기능2: 2초)
 * @param truncated   최대 건수 초과로 제외된 건수 (표시순서 하위)
 * @param items       노출 배너 (표시순서대로)
 */
public record TopSlideBannerList(
        String channel,
        String baseAt,
        boolean eligible,
        String audience,
        int total,
        int maxCount,
        int intervalMs,
        int truncated,
        List<TopSlideBannerView> items
) {
}
