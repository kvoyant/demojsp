package com.example.demojsp.common.terms.web;

import com.example.demojsp.common.response.ApiResponse;
import com.example.demojsp.common.terms.domain.AgreementRecord;
import com.example.demojsp.common.terms.domain.BizCodeInfo;
import com.example.demojsp.common.terms.domain.TermsItem;
import com.example.demojsp.common.terms.domain.TermsRegisterRequest;
import com.example.demojsp.common.terms.exception.TermsErrorCode;
import com.example.demojsp.common.terms.service.TermsAgreeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * [CF-BC-04] 약관 관리(ADMIN) API - 약관 등록/개정, 버전 이력, 동의 이력 조회.
 *
 * <pre>
 *  GET  /api/common/terms-admin/biz-codes              약관 등록된 업무코드 목록
 *  GET  /api/common/terms-admin/{bizCode}/versions     업무코드의 전체 약관 버전 이력
 *  POST /api/common/terms-admin/terms                  약관 등록/개정 (새 버전 → 즉시 현재 약관)
 *  GET  /api/common/terms-admin/agreements?bizCode=&subjectId=&limit=   동의 이력(증적) 조회
 * </pre>
 *
 * <p>권한: {@link TermsRequestContext#requireAdmin} - ROLE_ADMIN 세션 또는 local 전용 {@code common.terms.admin-api-open=true}.
 * <p>경로를 /api/common/terms 하위가 아닌 별도 prefix 로 둔 이유: {bizCode} 경로변수와 충돌 방지 + 인터셉터/보안 정책 분리 용이.
 */
@RestController
@RequestMapping("/api/common/terms-admin")
public class TermsAdminApiController {

    private final TermsAgreeService termsAgreeService;
    private final TermsRequestContext context;

    public TermsAdminApiController(TermsAgreeService termsAgreeService, TermsRequestContext context) {
        this.termsAgreeService = termsAgreeService;
        this.context = context;
    }

    @GetMapping("/biz-codes")
    public ApiResponse<List<BizCodeInfo>> bizCodes(HttpServletRequest request) {
        context.requireAdmin(request);
        return ApiResponse.success(termsAgreeService.bizCodes());
    }

    @GetMapping("/{bizCode}/versions")
    public ApiResponse<List<TermsItem>> versions(@PathVariable("bizCode") String bizCode, HttpServletRequest request) {
        context.requireAdmin(request);
        return ApiResponse.success(termsAgreeService.versions(bizCode));
    }

    @PostMapping("/terms")
    public ApiResponse<TermsItem> register(@RequestBody TermsRegisterRequest body, HttpServletRequest request) {
        String adminId = context.requireAdmin(request);
        return ApiResponse.success("약관이 등록되었습니다.", termsAgreeService.register(body, adminId));
    }

    @GetMapping("/agreements")
    public ApiResponse<List<AgreementRecord>> agreements(
            @RequestParam(value = "bizCode", required = false) String bizCode,
            @RequestParam(value = "subjectId", required = false) String subjectId,
            @RequestParam(value = "limit", defaultValue = "50") int limit,
            HttpServletRequest request) {
        context.requireAdmin(request);
        return ApiResponse.success(termsAgreeService.history(bizCode, subjectId, limit));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        TermsErrorCode code = TermsErrorCode.INVALID_REQUEST;
        return ResponseEntity.status(code.getStatus()).body(ApiResponse.error(code.getCode(), code.getMessage()));
    }
}
