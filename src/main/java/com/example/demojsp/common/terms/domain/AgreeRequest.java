package com.example.demojsp.common.terms.domain;

import java.util.List;

/**
 * 약관 동의 요청.
 *
 * <p>화면은 조회한 약관 전체(필수+선택)에 대해 동의 여부를 보낸다. 누락된 선택약관은 "미동의(N)" 로 기록한다.
 *
 * @param channel 호출 채널 (APP / PC_WEB / RECRUIT_WEB) - 이력 기록용
 * @param items   약관별 동의 여부
 */
public record AgreeRequest(String channel, List<Item> items) {

    /**
     * @param termsCode 약관코드
     * @param version   화면이 보여준 약관 버전 (서버 현재 버전과 다르면 TRM-103 → 재조회 유도)
     * @param agreed    동의 여부
     */
    public record Item(String termsCode, String version, boolean agreed) {
    }
}
