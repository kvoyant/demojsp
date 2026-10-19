package com.example.demojsp.common.niceid.gateway;

/**
 * NICE 가 돌려주는 본인확인 결과 (표준 결과 객체).
 *
 * @param ci         연계정보 88자 (Base64) - 주민번호 대신 고객을 식별하는 값
 * @param di         중복가입확인정보 64자
 * @param name       성명
 * @param birthDt    생년월일 yyyyMMdd
 * @param gender     M / F
 * @param nationalCd L 내국인 / F 외국인
 * @param cellNo     휴대폰번호 (숫자만)
 */
public record NiceIdIdentity(String ci, String di, String name, String birthDt, String gender, String nationalCd, String cellNo) {
}
