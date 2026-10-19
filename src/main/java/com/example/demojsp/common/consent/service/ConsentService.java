package com.example.demojsp.common.consent.service;

import com.example.demojsp.common.consent.dto.ConsentAgreeRequest;
import com.example.demojsp.common.consent.dto.ConsentListDto;
import com.example.demojsp.common.consent.dto.ConsentResult;
import com.example.demojsp.common.consent.dto.ConsentTermsDto;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDate;

/**
 * [CF-BC-04 / 공통#22] 약관동의 (DB 방식).
 *
 * <pre>
 *  화면      getTerms("SIGN_UP")            → 1페이지 필수 / 2페이지 선택 목록 (DB 에서 조회)
 *            getTermsDetail(termsCd, ver)   → 약관 전문
 *            agree("SIGN_UP", 요청, 사용자) → 필수 재검증 + 동의내역 저장 → agreeNo
 *  업무서버  checkAgreed(agreeNo, "SIGN_UP", request) → 내 동의인지·필수 동의했는지·유효시간 확인
 * </pre>
 */
public interface ConsentService {

    /** 오늘 기준 업무 약관 목록 */
    ConsentListDto getTerms(String bizCd);

    /** 기준일 약관 목록 (ADMIN 미리보기: 시행 예정 개정본 확인) */
    ConsentListDto getTerms(String bizCd, LocalDate baseDt);

    /** 약관 전문 (화면이 목록에서 받은 버전 그대로 조회) */
    ConsentTermsDto getTermsDetail(String termsCd, int termsVer);

    /** 동의 저장 → 동의번호 */
    ConsentResult agree(String bizCd, ConsentAgreeRequest request, ConsentUser user);

    /** 업무 서버에서 다음 단계 진행 전 확인. 문제가 있으면 ConsentException(CNS-201/202) */
    ConsentResult checkAgreed(long agreeNo, String bizCd, HttpServletRequest request);

    /** 위와 같음 (요청 객체가 없는 곳 / 테스트) */
    ConsentResult checkAgreed(long agreeNo, String bizCd, ConsentUser user);
}
