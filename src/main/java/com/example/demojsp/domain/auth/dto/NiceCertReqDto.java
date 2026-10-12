package com.example.demojsp.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NiceCertReqDto {

    // 기본 본인인증 정보 (통신사 미지정 방식: 통신사 필드 불필요)
    private String custNm;         // 성명
    private String birthDate;      // 생년월일 (YYYYMMDD 8자리)
    private String gender;         // 성별 (1: 남, 2: 여, 주민번호 7번째 자리 혹은 선택)
    private String cellNo;         // 휴대폰번호 (01012345678)
    
    // 채널 및 인증방식
    private String channelType;    // APP, PC_WEB, MO_WEB
    private String authMethod;     // SMS_NO_CARRIER, MO_DEVICE_AUTH

    // 동의한 약관 ID 목록
    private List<String> agreedTermsIds;

    // 테스트/시뮬레이션 모드 플래그 (NORMAL, MINOR, FOREIGNER, WATCHLIST_FRAUD)
    private String simulationMode;
}
