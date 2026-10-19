package com.example.demojsp.common.acctverify.controller;

import com.example.demojsp.common.acctverify.dto.AvBankDto;
import com.example.demojsp.common.acctverify.dto.AvBizDto;
import com.example.demojsp.common.acctverify.dto.AvReqDto;
import com.example.demojsp.common.acctverify.mapper.AcctVerifyMapper;
import com.example.demojsp.common.acctverify.service.AcctVerifyAdminService;
import com.example.demojsp.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * [디지털어드민] 1원 계좌인증 관리 API.
 * <pre>
 *  GET/POST /api/common/acct-verify-admin/bizs       업무 Context (화면 종류 · 자동완성 · 예금주확인 · 접두어 · 유효시간 · 횟수)
 *  GET      /api/common/acct-verify-admin/requests   인증 이력 (?biz=&amp;status=)
 *  GET      /api/common/acct-verify-admin/banks      은행 코드 (TB_BANK_CODE)
 * </pre>
 */
@RestController
@RequestMapping("/api/common/acct-verify-admin")
public class AcctVerifyAdminApiController {

    private final AcctVerifyAdminService service;
    private final AcctVerifyMapper mapper;
    private final AcctVerifyAdminAuth auth;

    public AcctVerifyAdminApiController(AcctVerifyAdminService service, AcctVerifyMapper mapper, AcctVerifyAdminAuth auth) {
        this.service = service;
        this.mapper = mapper;
        this.auth = auth;
    }

    @GetMapping("/bizs")
    public ApiResponse<List<AvBizDto>> bizs(HttpServletRequest request) {
        auth.requireAdmin(request);
        return ApiResponse.success(service.bizList());
    }

    @PostMapping("/bizs")
    public ApiResponse<AvBizDto> saveBiz(@RequestBody AvBizDto body, HttpServletRequest request) {
        return ApiResponse.success("저장했습니다.", service.saveBiz(body, auth.requireAdmin(request)));
    }

    @GetMapping("/requests")
    public ApiResponse<List<AvReqDto>> requests(@RequestParam(value = "biz", required = false) String biz,
                                                @RequestParam(value = "status", required = false) String status,
                                                HttpServletRequest request) {
        auth.requireAdmin(request);
        return ApiResponse.success(service.requests(biz, status, 100));
    }

    @GetMapping("/banks")
    public ApiResponse<List<AvBankDto>> banks(HttpServletRequest request) {
        auth.requireAdmin(request);
        return ApiResponse.success(mapper.selectBanks());
    }
}
