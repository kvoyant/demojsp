package com.example.demojsp.common.product.domain;

/**
 * 공통#48 상품설정 - 홈 표시여부 / 홈 표시순서 변경 요청.
 *
 * @param homeDisplay "Y" / "N" (또는 true/false)
 * @param homeOrder   1~3 (Y 이면 필수, N 이면 무시)
 */
public record HomeSettingRequest(String homeDisplay, Integer homeOrder) {
}
