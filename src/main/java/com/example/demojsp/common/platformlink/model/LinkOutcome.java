package com.example.demojsp.common.platformlink.model;

/**
 * 어댑터가 제휴사 응답 전문을 만들 때 쓰는 값 (내부 결과 → 제휴사 코드로 이미 변환됨).
 *
 * @param innerCd     내부 결과코드 (APPROVED, REJECTED, TIMEOUT ...)
 * @param outerCd     제휴사 결과코드 (TB_LNK_CODE_MAP)
 * @param outerMsg    제휴사 결과문구
 * @param partnerReqNo 제휴사 요청번호 (응답에 되돌려 준다)
 * @param lnkReqNo    당행 연계 요청번호
 * @param outerPrdCd  제휴사 상품코드 (승인 시)
 * @param productNm   당행 상품명 (승인 시)
 * @param limitAmt    한도 (원)
 * @param rateBp      금리 (bp)
 */
public record LinkOutcome(String innerCd, String outerCd, String outerMsg, String partnerReqNo, String lnkReqNo,
                          String outerPrdCd, String productNm, long limitAmt, int rateBp) {

    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
    public static final String TIMEOUT = "TIMEOUT";
    public static final String CSS_ERROR = "CSS_ERROR";
    public static final String INVALID = "INVALID";
    public static final String PRODUCT_NA = "PRODUCT_NA";
    public static final String STOPPED = "STOPPED";
    public static final String AUTH_FAIL = "AUTH_FAIL";

    public boolean approved() {
        return APPROVED.equals(innerCd);
    }
}
