package com.example.demojsp.common.identity.domain;

/**
 * 본인인증 목적(업무 구분).
 *
 * <p>인증 결과(CI)를 세션에 저장할 때 목적별로 분리해서 저장한다.
 * "회원가입용으로 받은 인증 결과를 비밀번호 재설정에 재사용"하는 식의 오용을 막기 위함이다.
 * 업무팀은 자기 화면에 맞는 목적을 지정해서 호출하고, 결과도 같은 목적으로 꺼내야 한다.
 *
 * <p>※ 프로젝트 업무에 맞춰 상수를 추가/수정해서 사용한다.
 */
public enum AuthPurpose {
    SIGN_UP("회원가입"),
    FIND_ID("아이디 찾기"),
    RESET_PASSWORD("비밀번호 재설정"),
    CHANGE_MOBILE("휴대폰번호 변경"),
    CONTRACT("계약/신청"),
    ETC("기타");

    private final String description;

    AuthPurpose(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
