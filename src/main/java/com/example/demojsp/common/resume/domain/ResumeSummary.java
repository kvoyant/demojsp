package com.example.demojsp.common.resume.domain;

import java.util.Map;

/**
 * 이어하기 카드 응답 (홈/저축/대출 공통).
 *
 * @param scope    조회 범위 (ALL / DEPOSIT / LOAN)
 * @param count    범위 안 진행 중 건수 → 카드에 "숫자" 표시 (공통#43 기능2). 0 이면 카드 미노출 (제공대상: 이어하기 대상자)
 * @param counts   범위별 건수 {ALL, DEPOSIT, LOAN} → 하단 탭 툴팁(공통#50) 등 다른 위치에서 같은 응답 재사용
 * @param latest   최신 1건 (공통#43 기능1). count 가 0 이면 null
 * @param moreUrl  [더보기] 이동 화면 = 신청현황조회 (공통#43 기능3)
 * @param baseAt   기준시각 (yyyy-MM-dd HH:mm:ss)
 */
public record ResumeSummary(
        String scope,
        int count,
        Map<String, Integer> counts,
        ResumeCardView latest,
        String moreUrl,
        String baseAt
) {
}
