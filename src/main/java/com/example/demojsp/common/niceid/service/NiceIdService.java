package com.example.demojsp.common.niceid.service;

import com.example.demojsp.common.consent.service.ConsentUser;
import com.example.demojsp.common.niceid.dto.IdvPurposeDto;
import com.example.demojsp.common.niceid.dto.IdvResult;
import com.example.demojsp.common.niceid.dto.IdvStartRequest;
import com.example.demojsp.common.niceid.dto.IdvStartResult;
import jakarta.servlet.http.HttpServletRequest;

/**
 * [CF-BC-03 / 공통#21] 휴대폰 본인인증 NICE ID (DB 방식).
 *
 * <pre>
 *  ① 약관동의 (공통#22 ConsentAgree, 업무 정책의 CONSENT_BIZ_CD) → agreeNo
 *  ② SMS : startSms(성명·생년월일·휴대폰)  → verifySms(reqNo, 인증번호)        ← 통신사 선택 없음
 *     MO  : startMo(성명·생년월일)         → 고객이 문자 발송 → moStatus(reqNo)  ← 휴대폰번호 입력 생략
 *  ③ 결과 검사 (업무 정책): 성명·생년월일 일치 → 유의고객(CI) → 미성년자(만 나이) → 외국인
 *     → VERIFIED / BLOCKED(사유) / FAILED / EXPIRED 를 TB_CO_IDV_REQ 에 저장 (이력·증적)
 *  ④ 업무 서버: checkVerified(reqNo, 업무코드, request) → CI 등 결과 사용
 * </pre>
 */
public interface NiceIdService {

    /** 업무 정책 (화면이 약관 업무코드·사용 가능한 인증 방식을 알기 위해) */
    IdvPurposeDto purpose(String purposeCd);

    IdvStartResult startSms(IdvStartRequest request, ConsentUser user);

    IdvResult verifySms(String reqNo, String code, ConsentUser user);

    IdvStartResult startMo(IdvStartRequest request, ConsentUser user);

    /** MO 진행 상태 (화면이 몇 초마다 확인) - 문자를 받았으면 결과까지 처리해서 돌려준다 */
    IdvResult moStatus(String reqNo, ConsentUser user);

    /** [local 시뮬레이터] 고객이 phone 에서 MO 문자를 보낸 것으로 처리 */
    void demoMoSend(String reqNo, String phone, ConsentUser user);

    /** 업무 서버 확인 - 같은 사람 · 같은 업무 · 인증 성공 · 유효시간 이내. 아니면 NiceIdException(NID-3xx) */
    IdvResult checkVerified(String reqNo, String purposeCd, HttpServletRequest request);

    IdvResult checkVerified(String reqNo, String purposeCd, ConsentUser user);
}
