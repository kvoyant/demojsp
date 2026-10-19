package com.example.demojsp.common.niceid.controller;

import com.example.demojsp.common.consent.service.ConsentUser;
import com.example.demojsp.common.niceid.dto.IdvPurposeDto;
import com.example.demojsp.common.niceid.dto.IdvResult;
import com.example.demojsp.common.niceid.dto.IdvStartRequest;
import com.example.demojsp.common.niceid.dto.IdvStartResult;
import com.example.demojsp.common.niceid.dto.IdvVerifyRequest;
import com.example.demojsp.common.niceid.service.NiceIdService;
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
 * [CF-BC-03 / 공통#21] 휴대폰 본인인증 NICE ID - 사용자 화면 API (화면 공통 컴포넌트 nice-id.js 가 호출).
 *
 * <pre>
 *  GET  /api/common/niceid/purposes/{purposeCd}   업무 정책 (약관 업무코드 · SMS/MO 사용 여부 · 유효시간)
 *  POST /api/common/niceid/sms/request            통신사 미지정 인증번호 요청 {purposeCd, agreeNo, channel, name, birth, phone}
 *  POST /api/common/niceid/sms/verify             인증번호 확인 {reqNo, code} → 결과
 *  POST /api/common/niceid/mo/request             MO 인증 시작 {purposeCd, agreeNo, channel, name, birth} → 보낼 번호·문구
 *  GET  /api/common/niceid/mo/status?reqNo=       MO 진행 상태 (문자 받았으면 결과)
 *  POST /api/common/niceid/mo/demo-send           [local 시뮬레이터] MO 문자 보낸 것으로 처리 {reqNo, phone}
 * </pre>
 */
@RestController
@RequestMapping("/api/common/niceid")
public class NiceIdApiController {

    private final NiceIdService service;

    public NiceIdApiController(NiceIdService service) {
        this.service = service;
    }

    @GetMapping("/purposes/{purposeCd}")
    public ApiResponse<IdvPurposeDto> purpose(@PathVariable("purposeCd") String purposeCd) {
        return ApiResponse.success(service.purpose(purposeCd));
    }

    @PostMapping("/sms/request")
    public ApiResponse<IdvStartResult> smsRequest(@RequestBody IdvStartRequest body, HttpServletRequest request) {
        return ApiResponse.success("인증번호를 보냈습니다.", service.startSms(body, ConsentUser.from(request)));
    }

    @PostMapping("/sms/verify")
    public ApiResponse<IdvResult> smsVerify(@RequestBody IdvVerifyRequest body, HttpServletRequest request) {
        IdvResult r = service.verifySms(body.getReqNo(), body.getCode(), ConsentUser.from(request));
        return ApiResponse.success(r.getMessage(), r);
    }

    @PostMapping("/mo/request")
    public ApiResponse<IdvStartResult> moRequest(@RequestBody IdvStartRequest body, HttpServletRequest request) {
        return ApiResponse.success("아래 번호로 문자를 보내 주세요.", service.startMo(body, ConsentUser.from(request)));
    }

    @GetMapping("/mo/status")
    public ApiResponse<IdvResult> moStatus(@RequestParam("reqNo") String reqNo, HttpServletRequest request) {
        IdvResult r = service.moStatus(reqNo, ConsentUser.from(request));
        return ApiResponse.success(r.getMessage(), r);
    }

    @PostMapping("/mo/demo-send")
    public ApiResponse<Void> demoSend(@RequestBody DemoSend body, HttpServletRequest request) {
        service.demoMoSend(body.reqNo(), body.phone(), ConsentUser.from(request));
        return ApiResponse.success("[시뮬레이터] MO 문자를 보낸 것으로 처리했습니다.", null);
    }

    /** [local 시뮬레이터] MO 문자 발송 흉내 요청 */
    public record DemoSend(String reqNo, String phone) {
    }
}
