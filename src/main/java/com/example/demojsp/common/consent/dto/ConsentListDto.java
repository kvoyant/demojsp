package com.example.demojsp.common.consent.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 업무코드 하나의 약관 목록 - 1페이지(필수) / 2페이지(선택) 로 나눠서 내려준다. */
@Getter
@Setter
public class ConsentListDto {

    private String bizCd;
    private String bizNm;
    private LocalDate baseDt;                              // 어느 날짜 기준 약관인지 (보통 오늘)
    private List<ConsentTermsDto> required = new ArrayList<>();
    private List<ConsentTermsDto> optional = new ArrayList<>();
}
