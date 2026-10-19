package com.example.demojsp.common.acctverify.controller;

import com.example.demojsp.common.acctverify.dto.AvContextView;
import com.example.demojsp.common.acctverify.dto.AvRequest;
import com.example.demojsp.common.acctverify.dto.AvResult;
import com.example.demojsp.common.acctverify.dto.AvStartResult;
import com.example.demojsp.common.acctverify.dto.AvVerifyRequest;
import com.example.demojsp.common.acctverify.service.AcctCustomerResolver;
import com.example.demojsp.common.acctverify.service.AcctVerifyService;
import com.example.demojsp.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * [CF-BC-06 / 공통#25] 1원 계좌인증 - 사용자 화면 API (화면 공통 컴포넌트 acct-verify.js 가 호출).
 *
 * <pre>
 *  GET  /api/common/acct-verify/context/{bizCd}   화면 종류(LOAN/COMMON) · 은행 · 대출계좌 자동완성 후보(마스킹)
 *  POST /api/common/acct-verify/request           {bizCd, channel, prefillId | bankCd+acctNo, modifiedYn} → 1원 송금 · reqNo
 *  POST /api/common/acct-verify/verify            {reqNo, code} → 결과
 * </pre>
 * 고객 식별: 로그인 세션 → (비로그인) ?idvReqNo=&amp;idvPurposeCd= 공통#21 본인인증 → (local) ?demoCustomer=&amp;demoName=
 */
@RestController
@RequestMapping("/api/common/acct-verify")
public class AcctVerifyApiController {

    private final AcctVerifyService service;
    private final AcctCustomerResolver resolver;

    public AcctVerifyApiController(AcctVerifyService service, AcctCustomerResolver resolver) {
        this.service = service;
        this.resolver = resolver;
    }

    @GetMapping("/context/{bizCd}")
    public ApiResponse<AvContextView> context(@PathVariable("bizCd") String bizCd,
                                              @RequestParam(value = "idvReqNo", required = false) String idvReqNo,
                                              @RequestParam(value = "idvPurposeCd", required = false) String idvPurposeCd,
                                              HttpServletRequest request) {
        return ApiResponse.success(service.context(bizCd, resolver.resolve(request, idvReqNo, idvPurposeCd)));
    }

    @PostMapping("/request")
    public ApiResponse<AvStartResult> request(@RequestBody AvRequest body, HttpServletRequest request) {
        AvStartResult r = service.request(body, resolver.resolve(request, body.getIdvReqNo(), body.getIdvPurposeCd()));
        return ApiResponse.success("1원을 보냈습니다. 입금자명의 숫자 4자리를 입력하세요.", r);
    }

    @PostMapping("/verify")
    public ApiResponse<AvResult> verify(@RequestBody AvVerifyRequest body,
                                        @RequestParam(value = "idvReqNo", required = false) String idvReqNo,
                                        @RequestParam(value = "idvPurposeCd", required = false) String idvPurposeCd,
                                        HttpServletRequest request) {
        AvResult r = service.verify(body.getReqNo(), body.getCode(), resolver.resolve(request, idvReqNo, idvPurposeCd));
        return ApiResponse.success(r.getMessage(), r);
    }
}
