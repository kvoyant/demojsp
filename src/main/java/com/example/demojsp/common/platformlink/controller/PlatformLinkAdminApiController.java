package com.example.demojsp.common.platformlink.controller;

import com.example.demojsp.common.platformlink.dto.LnkCodeMapDto;
import com.example.demojsp.common.platformlink.dto.LnkPartnerDto;
import com.example.demojsp.common.platformlink.dto.LnkProductDto;
import com.example.demojsp.common.platformlink.dto.LnkReqDto;
import com.example.demojsp.common.platformlink.dto.LnkStatDto;
import com.example.demojsp.common.platformlink.service.PlatformLinkAdminService;
import com.example.demojsp.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * [디지털어드민] 플랫폼 대출 연계 운영 API.
 * <pre>
 *  GET/POST /api/common/platform-link-admin/partners            제휴사 설정 (엔진 LEGACY→SHADOW→NEW 전환 · 제한시간 · 중단)
 *  GET      /api/common/platform-link-admin/partners/{cd}/codes 결과코드 변환표
 *  GET      /api/common/platform-link-admin/products            상품유형 ↔ 당행 상품
 *  GET      /api/common/platform-link-admin/stats?hours=24      제휴사별 처리현황 (승인·거절·실패·타임아웃·평균/최대 ms)
 *  GET      /api/common/platform-link-admin/requests            이력 (?partner=&status=&result=)
 *  GET      /api/common/platform-link-admin/circuit             차단기 상태 / POST .../circuit/{engine}/reset
 * </pre>
 */
@RestController
@RequestMapping("/api/common/platform-link-admin")
public class PlatformLinkAdminApiController {

    private final PlatformLinkAdminService service;
    private final PlatformLinkAdminAuth auth;

    public PlatformLinkAdminApiController(PlatformLinkAdminService service, PlatformLinkAdminAuth auth) {
        this.service = service;
        this.auth = auth;
    }

    @GetMapping("/partners")
    public ApiResponse<List<LnkPartnerDto>> partners(HttpServletRequest request) {
        auth.requireAdmin(request);
        return ApiResponse.success(service.partners());
    }

    @PostMapping("/partners")
    public ApiResponse<LnkPartnerDto> savePartner(@RequestBody LnkPartnerDto body, HttpServletRequest request) {
        return ApiResponse.success("저장했습니다. 다음 요청부터 바로 적용됩니다.", service.savePartner(body, auth.requireAdmin(request)));
    }

    @GetMapping("/partners/{partnerCd}/codes")
    public ApiResponse<List<LnkCodeMapDto>> codes(@PathVariable("partnerCd") String partnerCd, HttpServletRequest request) {
        auth.requireAdmin(request);
        return ApiResponse.success(service.codeMaps(partnerCd));
    }

    @GetMapping("/products")
    public ApiResponse<List<LnkProductDto>> products(HttpServletRequest request) {
        auth.requireAdmin(request);
        return ApiResponse.success(service.products());
    }

    @GetMapping("/stats")
    public ApiResponse<List<LnkStatDto>> stats(@RequestParam(value = "hours", defaultValue = "24") int hours, HttpServletRequest request) {
        auth.requireAdmin(request);
        return ApiResponse.success(service.stats(hours));
    }

    @GetMapping("/requests")
    public ApiResponse<List<LnkReqDto>> requests(@RequestParam(value = "partner", required = false) String partner,
                                                 @RequestParam(value = "status", required = false) String status,
                                                 @RequestParam(value = "result", required = false) String result,
                                                 HttpServletRequest request) {
        auth.requireAdmin(request);
        return ApiResponse.success(service.requests(partner, status, result, 100));
    }

    @GetMapping("/circuit")
    public ApiResponse<Map<String, Map<String, Object>>> circuit(HttpServletRequest request) {
        auth.requireAdmin(request);
        return ApiResponse.success(service.circuit());
    }

    @PostMapping("/circuit/{engine}/reset")
    public ApiResponse<Map<String, Map<String, Object>>> resetCircuit(@PathVariable("engine") String engine, HttpServletRequest request) {
        auth.requireAdmin(request);
        service.resetCircuit(engine);
        return ApiResponse.success("차단기를 닫았습니다.", service.circuit());
    }
}
