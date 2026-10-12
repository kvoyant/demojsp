package com.example.demojsp.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NiceCertResultDto {

    // 인증 결과 기본 정보
    private String reqTxSeq;
    private boolean authenticated;
    private String authMethod;       // SMS_NO_CARRIER, MO_DEVICE_AUTH
    
    // NICE ID 반환 식별값
    private String ciVal;            // 연계정보 (CI 88자리)
    private String ciMasked;         // 화면 출력용 마스킹 CI
    private String diVal;            // 중복가입확인정보 (DI 64자리)
    
    // 본인확인 정보
    private String custNm;           // 성명
    private String birthDate;        // 생년월일 (YYYYMMDD)
    private String gender;           // 성별 (1: 남, 2: 여)
    private String nationalInfo;     // 내외국인 (1: 내국인, 2: 외국인)
    private String cellNo;           // 휴대폰번호
    private String cellNoMasked;     // 마스킹 휴대폰번호

    // 종합 체크 결과 (유의고객, 미성년자, 외국인)
    private CustomerRiskCheckDto riskCheck;

    // 인증 일시
    private LocalDateTime authCompleteTime;
}
