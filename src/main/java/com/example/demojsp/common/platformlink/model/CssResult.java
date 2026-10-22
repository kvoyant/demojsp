package com.example.demojsp.common.platformlink.model;

/**
 * CSS 결과 공통모델.
 *
 * @param approved     승인 여부
 * @param limitAmt     한도 (원)
 * @param rateBp       금리 (bp: 1250 = 연 12.50%)
 * @param rejectReason 거절 사유 (내부용, 제휴사에는 결과코드만)
 * @param cssTxNo      CSS 거래번호
 * @param engine       응답한 엔진 (LEGACY / NEW)
 */
public record CssResult(boolean approved, long limitAmt, int rateBp, String rejectReason, String cssTxNo, String engine) {

    public String summary() {
        return approved ? engine + " 승인 " + limitAmt / 10_000 + "만원 " + String.format("%.2f%%", rateBp / 100.0)
                : engine + " 거절(" + rejectReason + ")";
    }
}
