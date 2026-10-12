package com.example.demojsp.domain.login.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorVerifyDto {
    private String twoFactorTxSeq;
    private String otpCode;
}
