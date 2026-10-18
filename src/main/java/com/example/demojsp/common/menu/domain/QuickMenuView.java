package com.example.demojsp.common.menu.domain;

import java.util.List;

/**
 * 홈 퀵메뉴 (공통#46).
 *
 * @param segment     회원구분 코드 (GUEST / MEMBER / FINANCIAL)
 * @param segmentName 회원구분명
 * @param columns     열 수 (3)
 * @param rows        행 수 (2)
 * @param items       메뉴 (최대 6개, 배치 순서)
 */
public record QuickMenuView(
        String segment,
        String segmentName,
        int columns,
        int rows,
        List<QuickMenuItem> items
) {
}
