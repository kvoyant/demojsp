package com.example.demojsp.common.acctverify.controller;

import com.example.demojsp.common.acctverify.dto.AvResult;
import com.example.demojsp.common.acctverify.service.AcctCustomer;
import com.example.demojsp.common.acctverify.service.AcctCustomerResolver;
import com.example.demojsp.common.acctverify.service.AcctVerifyService;
import com.example.demojsp.common.consent.service.ConsentUser;
import com.example.demojsp.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * [공통#25 샘플] 1원 계좌인증 화면 + "다음 업무 단계" 예시.
 * <ul>
 *   <li>/common/acct-verify/sample : 사용자 화면 (LOAN 대출 입출금계좌 / COMMON 공통 - 같은 컴포넌트, 업무 Context 만 다름)</li>
 *   <li>/common/acct-verify/admin  : 디지털어드민 업무 Context · 인증 이력</li>
 * </ul>
 */
@Controller
public class AcctVerifySampleController {

    private final AcctVerifyService service;
    private final AcctCustomerResolver resolver;

    public AcctVerifySampleController(AcctVerifyService service, AcctCustomerResolver resolver) {
        this.service = service;
        this.resolver = resolver;
    }

    @GetMapping("/common/acct-verify/sample")
    public String sample() {
        return "common/acctverify/sample";
    }

    @GetMapping("/common/acct-verify/admin")
    public String admin() {
        return "common/acctverify/admin";
    }

    /**
     * 업무 서버 예시 - 계좌인증 다음 단계 (대출 실행 계좌 등록 등). 실제 업무(로그인 회원)에서는 이 한 줄:
     * <pre>AvResult r = acctVerifyService.checkVerified(reqNo, "LOAN_DISBURSE", request);   // r.getAcctNo() 전체 계좌번호</pre>
     * 샘플은 화면에서 고객을 바꿔 가며 시험하므로 resolver 로 같은 고객을 만든다.
     */
    @PostMapping("/common/acct-verify/sample/next")
    @ResponseBody
    public ApiResponse<Map<String, Object>> next(@RequestParam("reqNo") String reqNo, @RequestParam("bizCd") String bizCd,
                                                 HttpServletRequest request) {
        AcctCustomer c = resolver.resolve(request, request.getParameter("idvReqNo"), request.getParameter("idvPurposeCd"));
        AvResult r = service.checkVerified(reqNo, bizCd, new ConsentUser(c.custId(), c.sessKey(), c.clientIp()));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("bankNm", r.getBankNm());
        out.put("acctNoLength", r.getAcctNo().length());
        out.put("acctNoTail", "…" + r.getAcctNo().substring(r.getAcctNo().length() - 4) + " (전체 번호는 서버만 보유)");
        out.put("holderNm", r.getHolderNm());
        out.put("source", "PREFILL".equals(r.getSource()) ? "대출계좌 자동완성" : "직접 입력" + ("Y".equals(r.getModifiedYn()) ? " (자동완성 수정)" : ""));
        return ApiResponse.success("계좌인증 확인 완료 - 계좌를 등록합니다.", out);
    }
}
