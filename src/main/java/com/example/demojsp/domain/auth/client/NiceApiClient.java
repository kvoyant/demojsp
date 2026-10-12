package com.example.demojsp.domain.auth.client;

import com.example.demojsp.domain.auth.client.dto.NiceApiOtpRequestDto;
import com.example.demojsp.domain.auth.client.dto.NiceApiOtpResponseDto;
import com.example.demojsp.domain.auth.client.dto.NiceApiVerifyRequestDto;
import com.example.demojsp.domain.auth.client.dto.NiceApiVerifyResponseDto;

public interface NiceApiClient {

    /**
     * NICE 본인확인 서버에 통신사 미지정 본인인증 번호(OTP) 발송 요청 전문 전송
     * (성명 + 생년월일 + 휴대폰번호 전달, 통신사 미지정)
     */
    NiceApiOtpResponseDto requestCarrierUnspecifiedOtp(NiceApiOtpRequestDto reqDto);

    /**
     * NICE 본인확인 서버에 인증번호(OTP) 검증 전문 전송 및 암호화된 본인확인 결과(CI, DI) 복호화 수신
     */
    NiceApiVerifyResponseDto verifyOtp(NiceApiVerifyRequestDto verifyDto);
}
