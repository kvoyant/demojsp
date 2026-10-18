package com.example.demojsp.common.menu.domain;

/**
 * 퀵메뉴 1개 (고객 화면).
 *
 * @param code          메뉴 코드
 * @param name          메뉴명
 * @param icon          공통 아이콘 이름 (공통#41 스프라이트 ic-{icon})
 * @param url           이동 URL. 비로그인 고객의 로그인 필요 메뉴는 로그인 화면(returnUrl 포함)
 * @param requiresLogin 로그인 필요 메뉴 여부
 * @param loginFirst    지금 누르면 로그인부터 하는지 (비로그인 + 로그인 필요)
 */
public record QuickMenuItem(
        String code,
        String name,
        String icon,
        String url,
        boolean requiresLogin,
        boolean loginFirst
) {
}
