package com.example.demojsp.domain.login.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginLogDto {
    private Long logSeq;
    private String memberId;
    private String memberType;
    private String loginMethod;       // PASSWORD, NICE_CI_AUTH, TWO_FACTOR_OTP
    private String loginSuccessYn;    // Y, N
    private String failReason;
    private String clientIp;
    private String userAgent;
    private LocalDateTime loginDt;
}
