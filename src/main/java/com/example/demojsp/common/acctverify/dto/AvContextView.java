package com.example.demojsp.common.acctverify.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** GET /api/common/acct-verify/context/{bizCd} - 화면을 그리는 데 필요한 것 전부 (업무 Context · 은행 · 자동완성) */
@Getter
@Setter
public class AvContextView {

    private String bizCd;
    private String bizNm;
    private String screenType;                              // LOAN / COMMON → 화면 분기
    private String custNm;                                  // 예금주 비교 기준 (고객 본인 이름) - "홍길동 님 명의 계좌만"
    private int codeValidMin;
    private int maxTry;
    private List<AvBankDto> banks = new ArrayList<>();
    private List<AvPrefillDto> prefills = new ArrayList<>(); // LOAN + 자동완성 Y 일 때만
}
