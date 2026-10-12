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
public class NiceCertMoReqDto {
    private String custNm;         // 성명
    private String birthDate;      // 생년월일 (YYYYMMDD)
    private String channelType;    // APP, PC_WEB
    private List<String> agreedTermsIds;
    private String simulationMode; // NORMAL, MINOR, FOREIGNER, WATCHLIST_FRAUD
}
