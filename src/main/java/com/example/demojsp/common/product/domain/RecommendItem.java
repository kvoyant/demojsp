package com.example.demojsp.common.product.domain;

/**
 * 추천 상품 카드 1개 (고객 화면).
 *
 * @param id        상품 ID
 * @param code      상품코드
 * @param kind      DEPOSIT / LOAN
 * @param kindName  수신 / 여신
 * @param subType   상품유형명 (입출금, 적금, 신용대출 ...) → 카드 배지
 * @param name      상품명
 * @param summary   한줄 설명
 * @param rateText  금리 문구
 * @param icon      아이콘 이름
 * @param detailUrl 상품안내페이지 (터치 시 이동)
 * @param homeOrder 홈 표시순서
 */
public record RecommendItem(
        long id,
        String code,
        String kind,
        String kindName,
        String subType,
        String name,
        String summary,
        String rateText,
        String icon,
        String detailUrl,
        int homeOrder
) {

    public static RecommendItem of(Product p) {
        return new RecommendItem(p.id(), p.code(), p.kind().name(), p.kind().getLabel(), p.subType(), p.name(),
                p.summary(), p.rateText(), p.icon(), p.detailUrl(), p.homeOrder());
    }
}
