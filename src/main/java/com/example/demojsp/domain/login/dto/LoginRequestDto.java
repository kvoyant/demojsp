package com.example.demojsp.domain.login.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDto {
    private String memberId;
    private String password;
    private String memberType;     // INDIVIDUAL, COUNSELOR, ADMIN
}
