package com.example.demojsp.common.consent.controller;

import com.example.demojsp.common.consent.dto.ConsentAgreeDto;
import com.example.demojsp.common.consent.dto.ConsentBizDto;
import com.example.demojsp.common.consent.dto.ConsentBizTermsRequest;
import com.example.demojsp.common.consent.dto.ConsentListDto;
import com.example.demojsp.common.consent.dto.ConsentTermsDto;
import com.example.demojsp.common.consent.dto.ConsentTermsSaveRequest;
import com.example.demojsp.common.consent.service.ConsentAdminService;
import com.example.demojsp.common.consent.service.ConsentService;
import com.example.demojsp.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * [디지털어드민] 약관 관리 API (REQ-AP-485/486 업무관리 - 약관/서식 · 업무별 약관)
 *
 * <pre>
 *  GET  /api/common/consent-admin/biz                      업무코드 목록 (필수/선택 약관 수)
 *  POST /api/common/consent-admin/biz                      업무코드 등록/수정
 *  GET  /api/common/consent-admin/biz/{bizCd}/terms        업무별 약관
 *  PUT  /api/common/consent-admin/biz/{bizCd}/terms        업무별 약관 저장 (목록 전체)
 *  GET  /api/common/consent-admin/biz/{bizCd}/preview?baseDt=2027-01-01   기준일 사용자 화면 미리보기
 *  GET  /api/common/consent-admin/terms                    약관 목록 (코드별 최신)
 *  GET  /api/common/consent-admin/terms/{termsCd}/versions 버전 이력
 *  POST /api/common/consent-admin/terms                    약관 등록 / 개정 (다음 버전)
 *  GET  /api/common/consent-admin/agreements?bizCd=&custId=&limit=   동의내역
 *  GET  /api/common/consent-admin/agreements/{agreeNo}     동의내역 상세 (약관별 버전·해시 = 증적)
 * </pre>
 */
@RestController
@RequestMapping("/api/common/consent-admin")
public class ConsentAdminApiController {

    private final ConsentAdminService adminService;
    private final ConsentService consentService;
    private final ConsentAdminAuth auth;

    public ConsentAdminApiController(ConsentAdminService adminService, ConsentService consentService, ConsentAdminAuth auth) {
        this.adminService = adminService;
        this.consentService = consentService;
        this.auth = auth;
    }

    // ---------------------------------------------------------------- 업무코드 · 업무별 약관
    @GetMapping("/biz")
    public ApiResponse<List<ConsentBizDto>> bizList(HttpServletRequest request) {
        auth.requireAdmin(request);
        return ApiResponse.success(adminService.bizList());
    }

    @PostMapping("/biz")
    public ApiResponse<ConsentBizDto> saveBiz(@RequestBody ConsentBizDto body, HttpServletRequest request) {
        return ApiResponse.success("업무코드가 저장되었습니다.", adminService.saveBiz(body, auth.requireAdmin(request)));
    }

    @GetMapping("/biz/{bizCd}/terms")
    public ApiResponse<List<ConsentTermsDto>> bizTerms(@PathVariable("bizCd") String bizCd, HttpServletRequest request) {
        auth.requireAdmin(request);
        return ApiResponse.success(adminService.bizTerms(bizCd));
    }

    @PutMapping("/biz/{bizCd}/terms")
    public ApiResponse<List<ConsentTermsDto>> saveBizTerms(@PathVariable("bizCd") String bizCd,
                                                           @RequestBody List<ConsentBizTermsRequest> body,
                                                           HttpServletRequest request) {
        return ApiResponse.success("업무별 약관이 저장되었습니다.", adminService.saveBizTerms(bizCd, body, auth.requireAdmin(request)));
    }

    @GetMapping("/biz/{bizCd}/preview")
    public ApiResponse<ConsentListDto> preview(@PathVariable("bizCd") String bizCd,
                                               @RequestParam(value = "baseDt", required = false)
                                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDt,
                                               HttpServletRequest request) {
        auth.requireAdmin(request);
        return ApiResponse.success(baseDt == null ? consentService.getTerms(bizCd) : consentService.getTerms(bizCd, baseDt));
    }

    // ---------------------------------------------------------------- 약관
    @GetMapping("/terms")
    public ApiResponse<List<ConsentTermsDto>> termsList(HttpServletRequest request) {
        auth.requireAdmin(request);
        return ApiResponse.success(adminService.termsList());
    }

    @GetMapping("/terms/{termsCd}/versions")
    public ApiResponse<List<ConsentTermsDto>> versions(@PathVariable("termsCd") String termsCd, HttpServletRequest request) {
        auth.requireAdmin(request);
        return ApiResponse.success(adminService.termsVersions(termsCd));
    }

    @PostMapping("/terms")
    public ApiResponse<ConsentTermsDto> saveTerms(@RequestBody ConsentTermsSaveRequest body, HttpServletRequest request) {
        ConsentTermsDto saved = adminService.saveTerms(body, auth.requireAdmin(request));
        return ApiResponse.success(saved.getTermsVer() == 1 ? "약관이 등록되었습니다." : "약관이 개정되었습니다. (v" + saved.getTermsVer() + ")", saved);
    }

    // ---------------------------------------------------------------- 동의내역
    @GetMapping("/agreements")
    public ApiResponse<List<ConsentAgreeDto>> agreements(@RequestParam(value = "bizCd", required = false) String bizCd,
                                                         @RequestParam(value = "custId", required = false) String custId,
                                                         @RequestParam(value = "limit", defaultValue = "50") int limit,
                                                         HttpServletRequest request) {
        auth.requireAdmin(request);
        return ApiResponse.success(adminService.agreeList(bizCd, custId, limit));
    }

    @GetMapping("/agreements/{agreeNo}")
    public ApiResponse<ConsentAgreeDto> agreement(@PathVariable("agreeNo") long agreeNo, HttpServletRequest request) {
        auth.requireAdmin(request);
        return ApiResponse.success(adminService.agreeDetail(agreeNo));
    }
}
