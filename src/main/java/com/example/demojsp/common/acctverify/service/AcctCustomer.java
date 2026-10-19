package com.example.demojsp.common.acctverify.service;

/**
 * 1원 계좌인증을 요청한 고객.
 *
 * @param custId  회원 ID (로그인) - 대출계좌 자동완성 조회 기준. 비로그인 null
 * @param custNm  고객 실명 - 예금주 비교 기준 (로그인 회원명 또는 공통#21 본인인증 성명)
 * @param sessKey 세션ID SHA-256 - 비로그인 고객이 "같은 사람" 인지 확인
 * @param clientIp 접속 IP
 */
public record AcctCustomer(String custId, String custNm, String sessKey, String clientIp) {
}
