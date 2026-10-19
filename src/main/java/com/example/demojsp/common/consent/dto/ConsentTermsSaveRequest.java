package com.example.demojsp.common.consent.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** [ADMIN] 약관 등록 / 개정 요청 - 같은 약관코드가 있으면 다음 버전으로 저장된다. */
@Getter
@Setter
@NoArgsConstructor
public class ConsentTermsSaveRequest {

    private String termsCd;
    private String termsNm;
    private String termsCn;
    private String pdfUrl;
    private LocalDate effectDt;
}
