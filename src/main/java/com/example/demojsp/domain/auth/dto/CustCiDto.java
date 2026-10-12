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
public class CustCiDto {
    private String ciVal;
    private String diVal;
    private String custId;
    private String custNm;
    private String birthDate;
    private String gender;        // 1: 남, 2: 여
    private String nationalInfo;  // 1: 내국인, 2: 외국인
    private String cellNo;
    private LocalDateTime regDt;
    private LocalDateTime modDt;
}
