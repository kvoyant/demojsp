package com.example.demojsp.domain.auth.service;

import com.example.demojsp.domain.auth.dto.*;

import java.util.List;
import java.util.Map;

public interface NiceCertService {

    /**
     * 비여신금융거래 본인확인 약관 목록 조회
     */
    List<TermsDto> getActiveTermsList();

    /**
     * 통신사 미지정 방식 본인인증 번호(OTP) 발송 요청
     */
    Map<String, Object> requestOtp(NiceCertReqDto reqDto, String clientIp, String userAgent);

    /**
     * 인증번호(OTP) 검증 및 CI/DI 획득 & 리스크 체크
     */
    NiceCertResultDto verifyOtp(NiceCertVerifyDto verifyDto);

    /**
     * MO(기기 점유 문자 발송) 인증 세션 생성 요청
     */
    Map<String, Object> requestMoSession(NiceCertMoReqDto moReqDto, String clientIp, String userAgent);

    /**
     * MO 인증 문자 수신 상태 확인 및 완료 처리
     */
    NiceCertResultDto checkMoStatus(String reqTxSeq);
}
