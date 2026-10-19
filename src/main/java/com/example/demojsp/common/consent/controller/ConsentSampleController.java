package com.example.demojsp.common.consent.controller;

import com.example.demojsp.common.consent.dto.ConsentResult;
import com.example.demojsp.common.consent.service.ConsentService;
import com.example.demojsp.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * [공통#22 샘플] 약관동의 (DB 방식) 화면 + "다음 업무 단계" 예시.
 * <ul>
 *   <li>/common/consent/sample : 사용자 화면 (consent-agree.js)</li>
 *   <li>/common/consent/admin  : 디지털어드민 약관관리 화면</li>
 * </ul>
 */
@Controller
public class ConsentSampleController {

    private final ConsentService consentService;

    public ConsentSampleController(ConsentService consentService) {
        this.consentService = consentService;
    }

    @GetMapping("/common/consent/sample")
    public String sample() {
        return "common/consent/sample";
    }

    @GetMapping("/common/consent/admin")
    public String admin() {
        return "common/consent/admin";
    }

    /**
     * 업무 서버 예시 - 약관동의 다음 단계 (예: 회원가입 정보입력).
     * 실제 업무 Controller/Service 에서는 이 한 줄만 쓰면 된다:
     * <pre>ConsentResult r = consentService.checkAgreed(agreeNo, "SIGN_UP", request);</pre>
     */
    @PostMapping("/common/consent/sample/next")
    @ResponseBody
    public ApiResponse<ConsentResult> next(@RequestParam("bizCd") String bizCd, @RequestParam("agreeNo") long agreeNo,
                                           HttpServletRequest request) {
        ConsentResult r = consentService.checkAgreed(agreeNo, bizCd, request);
        return ApiResponse.success("약관동의 확인 완료 - 다음 단계로 진행합니다.", r);
    }
}
