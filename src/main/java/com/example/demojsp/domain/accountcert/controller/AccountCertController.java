package com.example.demojsp.domain.accountcert.controller;

import com.example.demojsp.domain.login.dto.UserSessionDto;
import com.example.demojsp.domain.login.service.LoginServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * =========================================================================================
 * [공통 본인확인 | 1원 계좌인증 뷰 컨트롤러 (REQ-AP-605)]
 * 
 * [목적]
 * 대출신청 시 대출금입금/출금계좌 입력 목적의 화면과 타 업무에서 공통으로 사용할 화면 분리하여 운영 관리
 * =========================================================================================
 */
@Slf4j
@Controller
@RequestMapping("/common/auth")
@RequiredArgsConstructor
public class AccountCertController {

    /**
     * 1원 계좌인증 화면 (대출신청 연계 모드 & 타업무 공통 모드 통합 분리 지원)
     */
    @GetMapping("/account-cert")
    public String accountCertPage(@RequestParam(value = "purpose", required = false, defaultValue = "COMMON") String purpose,
                                  @RequestParam(value = "loanNo", required = false) String loanNo,
                                  @RequestParam(value = "custNm", required = false) String custNm,
                                  HttpServletRequest request,
                                  Model model) {
        HttpSession session = request.getSession(false);
        String sessionCustNm = null;
        String sessionMemberId = null;

        if (session != null) {
            UserSessionDto loginUser = (UserSessionDto) session.getAttribute(LoginServiceImpl.SESSION_USER_KEY);
            if (loginUser != null) {
                sessionCustNm = loginUser.getMemberNm();
                sessionMemberId = loginUser.getMemberId();
            }
        }

        String finalCustNm = (custNm != null && !custNm.isBlank()) ? custNm : (sessionCustNm != null ? sessionCustNm : "홍길동");
        String finalLoanNo = (loanNo != null && !loanNo.isBlank()) ? loanNo : "LN202610130001";

        model.addAttribute("purpose", purpose);
        model.addAttribute("loanNo", finalLoanNo);
        model.addAttribute("custNm", finalCustNm);
        model.addAttribute("memberId", sessionMemberId);

        log.info("[1원 계좌인증 화면 진입] purpose={}, loanNo={}, custNm={}", purpose, finalLoanNo, finalCustNm);

        return "common/auth/accountCert";
    }
}
