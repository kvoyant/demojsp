package com.example.demojsp.domain.auth.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NiceApiTokenResponseDto {
    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private String scope;
}
