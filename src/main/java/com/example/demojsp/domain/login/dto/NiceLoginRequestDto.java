package com.example.demojsp.domain.login.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NiceLoginRequestDto {
    private String ciVal;      // NICE ID 본인인증 완료 후 수신한 88자리 CI
    private String custNm;     // 고객명
    private String cellNo;     // 휴대폰번호
    private String birthDate;  // 생년월일
}
