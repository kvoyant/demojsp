package com.example.demojsp.common.member;

/**
 * 요청 고객의 회원구분.
 *
 * @param customerId 고객 ID (비로그인 = null)
 * @param segment    회원구분
 */
public record MemberContext(String customerId, MemberSegment segment) {

    public boolean loggedIn() {
        return customerId != null;
    }
}
