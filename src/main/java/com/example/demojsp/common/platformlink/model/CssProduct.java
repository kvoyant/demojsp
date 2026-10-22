package com.example.demojsp.common.platformlink.model;

/** CSS 상품유형 (TB_LNK_PRODUCT.CSS_PRODUCT) */
public final class CssProduct {
    public static final String CREDIT = "CREDIT";
    public static final String AUTO = "AUTO";
    public static final String MORTGAGE = "MORTGAGE";

    private CssProduct() {
    }

    /**
     * 상품유형 결정 규칙 (공통#68 REQ-AP-1330 과 같은 규칙).
     * 제휴사가 상품을 지정했으면 그대로, 아니면 차량번호 → 오토, 담보주소 → 모기지, 그 외 → 신용.
     */
    public static String decide(LimitInquiry q) {
        if (q.productHint() != null && !q.productHint().isBlank()) return q.productHint();
        if (q.vehicleNo() != null && !q.vehicleNo().isBlank()) return AUTO;
        if (q.collateralAddr() != null && !q.collateralAddr().isBlank()) return MORTGAGE;
        return CREDIT;
    }
}
