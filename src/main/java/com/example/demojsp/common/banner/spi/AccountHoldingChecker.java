package com.example.demojsp.common.banner.spi;

/**
 * 계좌 보유 여부 확인 (공통#44 정책1 "제공대상: 계좌 미보유 사용자").
 *
 * <p>기본 구현 {@code ConfigAccountHoldingChecker} 는 yaml 목록으로 흉내낸다. 운영에서는 수신/여신 계좌원장(계정계)을
 * 조회하는 구현체를 만들어 {@code @Primary} 로 등록한다. 홈 진입마다 호출되므로 캐시(로그인 세션 등)를 권장한다.
 *
 * <p>비로그인 고객(customerId=null)은 계좌 보유를 알 수 없으므로 계좌 미보유로 보고 배너를 노출한다.
 */
public interface AccountHoldingChecker {

    /**
     * @param customerId 로그인 고객 ID (비로그인 = null)
     * @return 수신 또는 여신 계좌를 1개 이상 보유하면 true
     */
    boolean hasAccount(String customerId);
}
