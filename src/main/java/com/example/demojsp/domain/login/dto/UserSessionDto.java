package com.example.demojsp.domain.login.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSessionDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String memberId;
    private String memberNm;
    private String memberType;     // INDIVIDUAL, COUNSELOR, ADMIN
    private String roleCd;         // ROLE_CUSTOMER, ROLE_COUNSELOR, ROLE_ADMIN
    private String cellNo;
    private String email;
    private String ciVal;
    private String sessionId;
    private String clientIp;
    private LocalDateTime loginTime;
    private int timeoutMinutes;    // 개인 30분, 상담사/관리자 15분
}
