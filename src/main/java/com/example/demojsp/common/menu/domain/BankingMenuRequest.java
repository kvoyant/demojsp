package com.example.demojsp.common.menu.domain;

/**
 * ADMIN 뱅킹메뉴 등록/수정 요청.
 *
 * @param category     카테고리 코드 (필수, 기존 카테고리)
 * @param name         메뉴명 (필수, 20자)
 * @param url          이동 URL (필수, 같은 도메인 상대경로)
 * @param displayOrder 표시순서 (필수, 1 이상 - 그 자리에 끼워 넣고 나머지는 뒤로 밀림)
 * @param use          사용여부 (생략 시 true)
 * @param newBadge     신규 표시 (생략 시 false)
 */
public record BankingMenuRequest(
        String category,
        String name,
        String url,
        Integer displayOrder,
        Boolean use,
        Boolean newBadge
) {
}
