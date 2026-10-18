package com.example.demojsp.common.member;

/**
 * 회원구분 - 홈 화면 개인화 기준 (공통#46 퀵메뉴 "회원구분 별", 공통#47 추천 상품 "금융회원 외").
 *
 * <ul>
 *   <li>{@link #GUEST} 비로그인</li>
 *   <li>{@link #MEMBER} 일반회원 - 로그인했지만 수신/여신 계좌 없음</li>
 *   <li>{@link #FINANCIAL} 금융회원 - 수신 또는 여신 계좌 보유</li>
 * </ul>
 * 금융회원 판단은 상단슬라이드배너(공통#44)와 같은 {@code AccountHoldingChecker} 를 쓴다 (판단 기준 1곳).
 */
public enum MemberSegment {

    GUEST("비로그인"),
    MEMBER("일반회원"),
    FINANCIAL("금융회원");

    private final String label;

    MemberSegment(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
