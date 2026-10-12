package com.example.demojsp.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRiskCheckDto {

    // 전체 검증 통과 여부 (거래 진행 가능 여부)
    private boolean pass;
    private String checkSummaryMessage;

    // 1. 유의고객(Watchlist / FDS / 연체자) 체크 결과
    private boolean watchlist;
    private boolean blocked;
    private String riskGrade;
    private String riskType;
    private String riskReason;

    // 2. 미성년자 체크 결과 (만 19세 기준)
    private boolean minor;
    private int exactAge;
    private String minorGuideMessage;

    // 3. 외국인 체크 결과 (내외국인 구분)
    private boolean foreigner;
    private String foreignerGuideMessage;
}
