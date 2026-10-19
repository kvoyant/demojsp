package com.example.demojsp.common.niceid.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** SMS 인증번호 확인 요청 */
@Getter
@Setter
@NoArgsConstructor
public class IdvVerifyRequest {

    private String reqNo;
    private String code;
}
