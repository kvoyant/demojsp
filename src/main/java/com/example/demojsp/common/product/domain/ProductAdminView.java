package com.example.demojsp.common.product.domain;

import java.time.format.DateTimeFormatter;

/**
 * ADMIN 수신상품목록 / 여신상품목록 1행 (공통#48 "상품설정" 컬럼 포함).
 *
 * @param homeDisplay 홈 표시여부 (Y/N)
 * @param homeOrder   홈 표시순서 (1~3, N 이면 null)
 * @param shownOnHome 지금 홈 추천 영역에 보이는지 (Y + 판매중)
 */
public record ProductAdminView(
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
        boolean onSale,
        boolean homeDisplay,
        Integer homeOrder,
        boolean shownOnHome,
        String updatedAt,
        String updatedBy
) {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static ProductAdminView of(Product p) {
        return new ProductAdminView(p.id(), p.code(), p.kind().name(), p.kind().getLabel(), p.subType(), p.name(), p.summary(),
                p.rateText(), p.icon(), p.detailUrl(), p.onSale(), p.homeDisplay(), p.homeOrder(), p.shownOnHome(),
                p.updatedAt() == null ? null : p.updatedAt().format(FMT), p.updatedBy());
    }
}
