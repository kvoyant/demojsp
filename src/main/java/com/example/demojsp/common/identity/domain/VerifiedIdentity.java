package com.example.demojsp.common.identity.domain;

import com.example.demojsp.common.util.MaskingUtil;

import java.io.Serializable;
import java.time.Instant;

/**
 * 본인인증 완료 결과 (인증수단과 무관한 공통 표준 모델).
 *
 * <p>NICE, 사설인증, 공동인증서 등 어떤 수단으로 인증하든 업무 화면/서비스는 이 객체만 사용한다.
 * HttpSession 에 저장되므로 Serializable 을 구현한다. (세션 클러스터링/Redis 세션 대비)
 *
 * <p><b>개인정보 주의</b>: CI/DI/휴대폰번호/생년월일은 민감정보다.
 * <ul>
 *   <li>로그에 남기지 않는다 → {@link #toString()} 에서 기존 프로젝트 {@link MaskingUtil} 로 마스킹</li>
 *   <li>화면(JSON)으로 내려주지 않는다 → 화면용은 {@link #toMaskedView()} 사용 (CI/DI 미포함)</li>
 * </ul>
 *
 * @param method        인증수단
 * @param purpose       인증 목적
 * @param channel       채널
 * @param name          성명
 * @param birthDate     생년월일 (yyyyMMdd)
 * @param gender        성별 공통코드 (M/F) - NICE 기준 1:남, 0:여 를 변환하여 저장
 * @param foreigner     외국인 여부 (true: 외국인)
 * @param mobileCarrier 통신사 코드 (NICE 기준 1:SKT 2:KT 3:LGU+ 4:SKT알뜰 5:KT알뜰 6:LGU+알뜰)
 * @param mobileNo      휴대폰번호
 * @param ci            연계정보(CI, 88byte) - 회원 식별 키
 * @param di            중복가입확인정보(DI, 64byte) - 사이트별 고유값
 * @param providerTxNo  인증기관 거래번호 (NICE responseno) - 민원/대사용
 * @param verifiedAt    인증 완료 시각
 */
public record VerifiedIdentity(
        AuthMethod method,
        AuthPurpose purpose,
        AuthChannel channel,
        String name,
        String birthDate,
        String gender,
        boolean foreigner,
        String mobileCarrier,
        String mobileNo,
        String ci,
        String di,
        String providerTxNo,
        Instant verifiedAt
) implements Serializable {

    /** 화면 응답용 마스킹 모델 (CI/DI 제외) */
    public MaskedView toMaskedView() {
        String maskedBirth = (birthDate == null || birthDate.length() < 4) ? birthDate : birthDate.substring(0, 4) + "****";
        return new MaskedView(method, purpose, MaskingUtil.maskName(name), maskedBirth,
                MaskingUtil.maskPhone(mobileNo), verifiedAt);
    }

    /** 화면에 내려줘도 되는 값만 담은 모델 */
    public record MaskedView(AuthMethod method, AuthPurpose purpose, String name,
                             String birthDate, String mobileNo, Instant verifiedAt) {
    }

    /** 로그 출력용 - 민감정보 마스킹 */
    @Override
    public String toString() {
        return "VerifiedIdentity[method=" + method + ", purpose=" + purpose
                + ", name=" + MaskingUtil.maskName(name)
                + ", mobileNo=" + MaskingUtil.maskPhone(mobileNo)
                + ", ci=" + MaskingUtil.maskCi(ci)
                + ", providerTxNo=" + providerTxNo + "]";
    }
}
