package com.example.demojsp.common.acctverify.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 인증코드(입금자명 뒤 숫자 4자리) 확인 요청 */
@Getter
@Setter
@NoArgsConstructor
public class AvVerifyRequest {

    private String reqNo;
    private String code;
}
