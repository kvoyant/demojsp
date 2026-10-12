package com.example.demojsp.domain.auth.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NiceApiOtpRequestDto {
    private String requestNo;        // 금융사 트랜잭션 번호
    private String userName;         // 고객 성명
    private String birthDate;        // 생년월일 (YYYYMMDD 8자리)
    private String gender;           // 성별 (1:남, 2:여)
    private String nationalInfo;     // 내외국인 (1:내국인, 2:외국인)
    private String mobileNo;         // 휴대폰번호
    private String carrier;          // 통신사 미지정 방식: "" 또는 NULL (이통3사 및 MVNO HLR 자동 식별)
    private List<String> agreedTerms; // 비여신 본인확인 약관 동의 코드 목록
    private String channelType;      // PC_WEB, APP
}
