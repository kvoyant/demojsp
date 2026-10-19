package com.example.demojsp.common.niceid.controller;

import com.example.demojsp.common.niceid.dto.IdvResult;
import com.example.demojsp.common.niceid.service.NiceIdService;
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
 * [공통#21 샘플] 휴대폰 본인인증 NICE ID 화면 + "다음 업무 단계" 예시.
 * <ul>
 *   <li>/common/niceid/sample : 사용자 화면 (nice-id.js - 약관 → SMS / MO → 결과)</li>
 *   <li>/common/niceid/admin  : 디지털어드민 업무별 정책 · 인증 이력</li>
 * </ul>
 */
@Controller
public class NiceIdSampleController {

    private final NiceIdService service;

    public NiceIdSampleController(NiceIdService service) {
        this.service = service;
    }

    @GetMapping("/common/niceid/sample")
    public String sample() {
        return "common/niceid/sample";
    }

    @GetMapping("/common/niceid/admin")
    public String admin() {
        return "common/niceid/admin";
    }

    /**
     * 업무 서버 예시 - 본인인증 다음 단계. 실제 업무에서는 이 한 줄:
     * <pre>IdvResult r = niceIdService.checkVerified(reqNo, "SIGN_UP", request);   // r.getCi() 로 회원 식별</pre>
     */
    @PostMapping("/common/niceid/sample/next")
    @ResponseBody
    public ApiResponse<Map<String, Object>> next(@RequestParam("reqNo") String reqNo, @RequestParam("purposeCd") String purposeCd,
                                                 HttpServletRequest request) {
        IdvResult r = service.checkVerified(reqNo, purposeCd, request);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("name", r.getName());
        out.put("age", r.getAge());
        out.put("ciPrefix", r.getCi().substring(0, 12) + "… (서버만 보유, 화면에는 보내지 않음)");
        out.put("ciLength", r.getCi().length());
        return ApiResponse.success("본인인증 확인 완료 - 다음 단계로 진행합니다.", out);
    }
}
