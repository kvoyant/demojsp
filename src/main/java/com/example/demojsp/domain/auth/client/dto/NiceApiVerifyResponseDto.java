package com.example.demojsp.domain.auth.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NiceApiVerifyResponseDto {
    private boolean success;
    private String resultCode;       // 0000: 성공
    private String resultMessage;
    
    // NICE 본인확인 암호화 응답 복호화 결과 데이터
    private String ci;               // 88자리 연계정보 (CI)
    private String di;               // 64자리 중복가입확인정보 (DI)
    private String name;             // 실명 검증된 성명
    private String birthDate;        // 생년월일 (YYYYMMDD)
    private String gender;           // 성별 (1:남, 2:여)
    private String nationalInfo;     // 내외국인 (1:내국인, 2:외국인)
    private String mobileNo;         // 휴대폰번호
    private String telcoCode;        // 최종 식별된 통신사 (SKT, KT, LGT, MVNO 등)
}
