package com.example.demojsp.common.niceid.controller;

import com.example.demojsp.common.niceid.dto.IdvPurposeDto;
import com.example.demojsp.common.niceid.dto.IdvReqDto;
import com.example.demojsp.common.niceid.service.NiceIdAdminService;
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
 * [디지털어드민] 본인인증 관리 API
 * <pre>
 *  GET  /api/common/niceid-admin/purposes              업무별 정책 목록
 *  POST /api/common/niceid-admin/purposes              정책 등록/수정 (약관·SMS/MO·최소나이·외국인·유의고객·시간·횟수)
 *  GET  /api/common/niceid-admin/requests?purpose=&status=&limit=   인증 이력 (CI·휴대폰 마스킹)
 * </pre>
 */
@RestController
@RequestMapping("/api/common/niceid-admin")
public class NiceIdAdminApiController {

    private final NiceIdAdminService service;
    private final NiceIdAdminAuth auth;

    public NiceIdAdminApiController(NiceIdAdminService service, NiceIdAdminAuth auth) {
        this.service = service;
        this.auth = auth;
    }

    @GetMapping("/purposes")
    public ApiResponse<List<IdvPurposeDto>> purposes(HttpServletRequest request) {
        auth.requireAdmin(request);
        return ApiResponse.success(service.purposes());
    }

    @PostMapping("/purposes")
    public ApiResponse<IdvPurposeDto> savePurpose(@RequestBody IdvPurposeDto body, HttpServletRequest request) {
        return ApiResponse.success("본인인증 정책이 저장되었습니다.", service.savePurpose(body, auth.requireAdmin(request)));
    }

    @GetMapping("/requests")
    public ApiResponse<List<IdvReqDto>> requests(@RequestParam(value = "purpose", required = false) String purpose,
                                                 @RequestParam(value = "status", required = false) String status,
                                                 @RequestParam(value = "limit", defaultValue = "100") int limit,
                                                 HttpServletRequest request) {
        auth.requireAdmin(request);
        return ApiResponse.success(service.requests(purpose, status, limit));
    }
}
