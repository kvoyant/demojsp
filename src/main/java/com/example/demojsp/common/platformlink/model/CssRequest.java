package com.example.demojsp.common.platformlink.model;

/**
 * 신규 CSS 요청 공통모델 (CF-BC-10). 신용·오토·모기지가 같은 모양을 쓰고, 상품별로 필요한 값만 채운다.
 * CSS 프로젝트의 I/F 정의서가 확정되면 이 필드를 정의서에 맞춘다 - 제휴사 전문은 영향 없음.
 */
public record CssRequest(String product, String channel, String ci, String custNm, String birth, long annualIncome,
                         String employType, String vehicleNo, String collateralAddr, long requestAmt) {

    public static CssRequest of(String product, String partnerCd, LimitInquiry q) {
        return new CssRequest(product, "PLATFORM:" + partnerCd, q.ci(), q.custNm(), q.birth(), q.annualIncome(), q.employType(),
                q.vehicleNo(), q.collateralAddr(), q.requestAmt());
    }
}
