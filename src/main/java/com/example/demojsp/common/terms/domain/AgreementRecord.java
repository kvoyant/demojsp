package com.example.demojsp.common.terms.domain;

import java.io.Serializable;

/**
 * 약관 동의 이력 1건 (증적). 동의/미동의 모두 기록한다.
 *
 * <p>약관 전문은 저장하지 않고 <b>버전 + 전문 해시(SHA-256)</b>를 저장한다.
 * 분쟁 시 "그 시점 그 버전 원문" 을 약관 원장에서 꺼내 해시를 재계산하면 동의 대상 문구가 변조되지 않았음을 입증할 수 있다.
 *
 * @param agreementId 동의 거래번호 (한 번의 동의 요청 = 1개 번호, 약관 N건 공유)
 * @param bizCode     업무코드
 * @param termsCode   약관코드
 * @param title       약관명 (조회 편의)
 * @param version     동의한 약관 버전
 * @param contentHash 동의한 약관 전문 SHA-256
 * @param required    필수 여부
 * @param agreed      동의 여부
 * @param subjectId   동의 주체 (로그인 회원ID, 비로그인은 "GUEST")
 * @param channel     채널
 * @param clientIp    접속 IP
 * @param agreedAt    처리 일시 (yyyy-MM-dd HH:mm:ss)
 */
public record AgreementRecord(
        String agreementId,
        String bizCode,
        String termsCode,
        String title,
        String version,
        String contentHash,
        boolean required,
        boolean agreed,
        String subjectId,
        String channel,
        String clientIp,
        String agreedAt
) implements Serializable {
}
