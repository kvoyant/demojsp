package com.example.demojsp.common.consent.service;

import com.example.demojsp.common.consent.dto.ConsentAgreeDto;
import com.example.demojsp.common.consent.dto.ConsentBizDto;
import com.example.demojsp.common.consent.dto.ConsentBizTermsRequest;
import com.example.demojsp.common.consent.dto.ConsentTermsDto;
import com.example.demojsp.common.consent.dto.ConsentTermsSaveRequest;

import java.util.List;

/**
 * [디지털어드민] 약관 관리 - 업무코드 / 약관(버전) / 업무별 약관 / 동의내역 조회.
 * 여기서 바꾼 값이 사용자 화면에 바로 반영된다 (캐시 없음).
 */
public interface ConsentAdminService {

    List<ConsentBizDto> bizList();

    ConsentBizDto saveBiz(ConsentBizDto biz, String adminId);

    /** 업무별 약관 (사용 안 함 포함) */
    List<ConsentTermsDto> bizTerms(String bizCd);

    /** 업무별 약관 목록을 통째로 저장 */
    List<ConsentTermsDto> saveBizTerms(String bizCd, List<ConsentBizTermsRequest> rows, String adminId);

    /** 약관코드별 최신 1행 */
    List<ConsentTermsDto> termsList();

    List<ConsentTermsDto> termsVersions(String termsCd);

    /** 약관 등록 (새 코드 → 1버전) / 개정 (기존 코드 → 다음 버전) */
    ConsentTermsDto saveTerms(ConsentTermsSaveRequest request, String adminId);

    List<ConsentAgreeDto> agreeList(String bizCd, String custId, int limit);

    ConsentAgreeDto agreeDetail(long agreeNo);
}
