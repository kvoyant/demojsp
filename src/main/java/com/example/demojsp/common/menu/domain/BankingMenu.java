package com.example.demojsp.common.menu.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Comparator;

/**
 * 뱅킹메뉴 1건 (ADMIN "뱅킹메뉴목록" 원장 → APP 전체메뉴).
 *
 * @param id           메뉴 ID
 * @param category     카테고리 코드 (예: PRODUCT)
 * @param categoryName 카테고리명 (예: 상품)
 * @param name         메뉴명
 * @param url          이동 URL
 * @param displayOrder 카테고리 안 전체메뉴 표시순서 (1부터, 카테고리 안에서 중복 없이 1..N 으로 유지)
 * @param use          사용여부
 * @param newBadge     신규 표시 ("N")
 * @param updatedAt    수정일시
 * @param updatedBy    수정자
 */
public record BankingMenu(
        long id,
        String category,
        String categoryName,
        String name,
        String url,
        int displayOrder,
        boolean use,
        boolean newBadge,
        LocalDateTime updatedAt,
        String updatedBy
) implements Serializable {

    public static final Comparator<BankingMenu> ORDER = Comparator
            .comparingInt(BankingMenu::displayOrder)
            .thenComparingLong(BankingMenu::id);

    public BankingMenu withOrder(int order) {
        return new BankingMenu(id, category, categoryName, name, url, order, use, newBadge, updatedAt, updatedBy);
    }
}
