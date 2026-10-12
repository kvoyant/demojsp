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
public class WatchlistDto {
    private Long watchSeq;
    private String ciVal;
    private String custNm;
    private String riskGrade;   // CRITICAL, HIGH, CAUTION
    private String riskType;    // FDS_FRAUD, LOAN_DELINQUENT, IDENTITY_THEFT, VOICE_PHISHING
    private String riskReason;
    private String blockYn;     // Y / N
    private LocalDateTime regDt;
    private String regUserId;
}
