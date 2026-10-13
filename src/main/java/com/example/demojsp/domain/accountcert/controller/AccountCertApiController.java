package com.example.demojsp.domain.accountcert.controller;

import com.example.demojsp.common.response.ApiResponse;
import com.example.demojsp.domain.accountcert.dto.*;
import com.example.demojsp.domain.accountcert.service.AccountCertService;
import com.example.demojsp.domain.login.dto.UserSessionDto;
import com.example.demojsp.domain.login.service.LoginServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * =========================================================================================
 * [공통 본인확인 | 1원 계좌인증 REST API 컨트롤러 (REQ-AP-605)]
 * =========================================================================================
 */
@Slf4j
@RestController
@RequestMapping("/api/common/auth/account-cert")
@RequiredArgsConstructor
public class AccountCertApiController {

    private final AccountCertService accountCertService;

    /**
     * 1. 금융기관(은행/증권) 전체 목록 조회
     */
    @GetMapping("/banks")
    public ApiResponse<List<BankCodeDto>> getBankCodeList() {
        List<BankCodeDto> bankList = accountCertService.getBankCodeList();
        return ApiResponse.success(bankList);
    }

    /**
     * 2. 대출신청 대출금 입금/출금 계좌 정보 조회 (요구사항 1: 자동완성용)
     */
    @GetMapping("/loan-account")
    public ApiResponse<LoanAccountDto> getLoanAccount(@RequestParam(value = "loanNo", required = false) String loanNo,
                                                      HttpServletRequest request) {
        String memberId = null;
        HttpSession session = request.getSession(false);
        if (session != null) {
            UserSessionDto user = (UserSessionDto) session.getAttribute(LoginServiceImpl.SESSION_USER_KEY);
            if (user != null) {
                memberId = user.getMemberId();
            }
        }

        LoanAccountDto loanAccount = accountCertService.getLoanAccount(loanNo, memberId);
        return ApiResponse.success(loanAccount);
    }

    /**
     * 3. 타행 본인계좌로 1원 송금 요청 (실명확인 + 적요 인자 4자리 발송)
     */
    @PostMapping("/request-transfer")
    public ApiResponse<OneWonTransferResDto> requestOneWonTransfer(@RequestBody OneWonTransferReqDto reqDto,
                                                                   HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            UserSessionDto user = (UserSessionDto) session.getAttribute(LoginServiceImpl.SESSION_USER_KEY);
            if (user != null && reqDto.getMemberId() == null) {
                reqDto.setMemberId(user.getMemberId());
            }
        }

        OneWonTransferResDto resDto = accountCertService.requestOneWonTransfer(reqDto, request);
        return ApiResponse.success(resDto);
    }

    /**
     * 4. 1원 송금 적요 인증코드(4자리) 검증
     */
    @PostMapping("/verify")
    public ApiResponse<OneWonVerifyResDto> verifyOneWon(@RequestBody OneWonVerifyReqDto verifyDto,
                                                        HttpServletRequest request) {
        OneWonVerifyResDto resDto = accountCertService.verifyOneWon(verifyDto, request);
        return ApiResponse.success(resDto);
    }
}
