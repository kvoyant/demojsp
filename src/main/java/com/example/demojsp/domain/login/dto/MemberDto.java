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
public class MemberDto {
    private String memberId;
    private String memberType;     // INDIVIDUAL(개인), COUNSELOR(상담사/모집인), ADMIN(관리자/심사역)
    private String roleCd;         // ROLE_CUSTOMER, ROLE_COUNSELOR, ROLE_ADMIN
    private String password;
    private String memberNm;
    private String cellNo;
    private String email;
    private String ciVal;          // NICE 연계정보 (CI)
    private Integer loginFailCnt;  // 0~5회
    private String lockYn;         // Y / N
    private LocalDateTime lastLoginDt;
    private LocalDateTime lastPwModDt;
    private String useYn;
    private LocalDateTime regDt;
}
