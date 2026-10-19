package com.example.demojsp.common.acctverify.service;

import com.example.demojsp.common.acctverify.dto.AvContextView;
import com.example.demojsp.common.acctverify.dto.AvRequest;
import com.example.demojsp.common.acctverify.dto.AvResult;
import com.example.demojsp.common.acctverify.dto.AvStartResult;
import com.example.demojsp.common.consent.service.ConsentUser;
import jakarta.servlet.http.HttpServletRequest;

/**
 * [CF-BC-06 / 공통#25] 1원 계좌인증 (DB 방식) - 서비스는 하나, 화면만 업무 Context 로 분기.
 *
 * <pre>
 *  context(bizCd)   → 화면 종류(LOAN 대출 입출금계좌용 / COMMON 공통) · 은행 목록 · 대출계좌 자동완성 후보
 *  request(...)     → 계좌 확정(자동완성 or 직접입력) → 예금주 본인 확인 → 1원 송금 (입금자명 "페퍼1234") → reqNo
 *  verify(reqNo, 4자리) → 일치하면 VERIFIED (오입력 횟수 · 유효시간은 업무 Context 기준)
 *  checkVerified(reqNo, bizCd, request) → 업무 서버가 인증된 계좌번호를 받는다
 * </pre>
 */
public interface AcctVerifyService {

    AvContextView context(String bizCd, AcctCustomer customer);

    AvStartResult request(AvRequest request, AcctCustomer customer);

    AvResult verify(String reqNo, String code, AcctCustomer customer);

    AvResult checkVerified(String reqNo, String bizCd, HttpServletRequest request);

    AvResult checkVerified(String reqNo, String bizCd, ConsentUser user);
}
