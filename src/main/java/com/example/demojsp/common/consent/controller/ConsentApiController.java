package com.example.demojsp.common.consent.controller;

import com.example.demojsp.common.consent.dto.ConsentAgreeRequest;
import com.example.demojsp.common.consent.dto.ConsentListDto;
import com.example.demojsp.common.consent.dto.ConsentResult;
import com.example.demojsp.common.consent.dto.ConsentTermsDto;
import com.example.demojsp.common.consent.service.ConsentService;
import com.example.demojsp.common.consent.service.ConsentUser;
import com.example.demojsp.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [CF-BC-04 / 공통#22] 약관동의 (DB 방식) - 사용자 화면용 API. 화면 공통 컴포넌트 consent-agree.js 가 호출한다.
 *
 * <pre>
 *  GET  /api/common/consent/{bizCd}                    약관 목록 (required 1페이지 / optional 2페이지, 전문 제외)
 *  GET  /api/common/consent/terms/{termsCd}/{termsVer} 약관 전문
 *  POST /api/common/consent/{bizCd}/agree              동의 저장 → agreeNo
 * </pre>
 */
@RestController
@RequestMapping("/api/common/consent")
public class ConsentApiController {

    private final ConsentService consentService;

    public ConsentApiController(ConsentService consentService) {
        this.consentService = consentService;
    }

    @GetMapping("/{bizCd}")
    public ApiResponse<ConsentListDto> terms(@PathVariable("bizCd") String bizCd) {
        return ApiResponse.success(consentService.getTerms(bizCd));
    }

    @GetMapping("/terms/{termsCd}/{termsVer}")
    public ApiResponse<ConsentTermsDto> detail(@PathVariable("termsCd") String termsCd, @PathVariable("termsVer") int termsVer) {
        return ApiResponse.success(consentService.getTermsDetail(termsCd, termsVer));
    }

    @PostMapping("/{bizCd}/agree")
    public ApiResponse<ConsentResult> agree(@PathVariable("bizCd") String bizCd, @RequestBody ConsentAgreeRequest body,
                                            HttpServletRequest request) {
        return ApiResponse.success("약관동의가 완료되었습니다.", consentService.agree(bizCd, body, ConsentUser.from(request)));
    }
}
