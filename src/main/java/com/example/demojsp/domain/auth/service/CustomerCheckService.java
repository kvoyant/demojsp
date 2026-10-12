package com.example.demojsp.domain.auth.service;

import com.example.demojsp.domain.auth.dto.CustomerRiskCheckDto;
import com.example.demojsp.domain.auth.dto.WatchlistDto;

public interface CustomerCheckService {

    /**
     * 주민번호 대체 CI 도입에 따른 종합 리스크 체크
     * 1) 유의고객 체크 (CI 기반 Watchlist 조회)
     * 2) 미성년자 체크 (생년월일 기준 만 19세 미만 판별)
     * 3) 외국인 체크 (NICE 내외국인 구분값 판별)
     */
    CustomerRiskCheckDto checkCustomerRisk(String ciVal, String birthDate, String nationalInfo, String custNm);

    /**
     * CI 기반 유의고객 여부 개별 조회
     */
    WatchlistDto findWatchlistByCi(String ciVal);

    /**
     * 생년월일(YYYYMMDD) 기준 만 나이 계산
     */
    int calculateExactAge(String birthDate);
}
