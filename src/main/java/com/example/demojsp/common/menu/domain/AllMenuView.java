package com.example.demojsp.common.menu.domain;

import java.util.List;

/**
 * APP 전체메뉴 (공통#46 기능4 "전체메뉴 > 상품 > 전체상품, 예금상품, 대출상품, 금융계산기").
 *
 * @param categories 카테고리 (seed 등록 순)
 */
public record AllMenuView(List<Category> categories) {

    /** 카테고리 1개 */
    public record Category(String code, String name, List<Item> menus) {
    }

    /** 메뉴 1개 (사용 중인 메뉴만, 표시순서대로) */
    public record Item(long id, String name, String url, boolean newBadge) {
    }
}
