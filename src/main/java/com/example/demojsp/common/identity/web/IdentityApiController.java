package com.example.demojsp.common.identity.web;

import com.example.demojsp.common.identity.domain.AuthMethod;
import com.example.demojsp.common.identity.domain.AuthPurpose;
import com.example.demojsp.common.identity.domain.StartRequest;
import com.example.demojsp.common.identity.domain.StartResponse;
import com.example.demojsp.common.identity.domain.TermsDocument;
import com.example.demojsp.common.identity.domain.VerifiedIdentity;
import com.example.demojsp.common.identity.exception.IdentityErrorCode;
import com.example.demojsp.common.identity.service.IdentityProviderRegistry;
import com.example.demojsp.common.identity.service.IdentitySessionManager;
import com.example.demojsp.common.identity.service.IdentityVerificationService;
import com.example.demojsp.common.identity.service.TermsService;
import com.example.demojsp.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * 본인인증 공통 API - 모든 채널(APP / PC WEB / 모집인WEB)이 같은 URL 을 사용한다.
 * 응답은 기존 프로젝트 표준인 {@link ApiResponse} ({success, code, message, data}) 형식을 따른다.
 *
 * <pre>
 *  GET  /api/common/identity/methods              사용 가능한 인증수단 목록
 *  GET  /api/common/identity/terms?method=...     인증수단별 약관 목록 (약관 상세보기)
 *  POST /api/common/identity/start                인증 시작 → 인증창 form 정보 반환
 *  GET  /api/common/identity/result?purpose=...   인증 결과 확인 (마스킹, CI 미포함)
 * </pre>
 *
 * <p>기존 NICE API(/api/common/auth/nice/**) 와 URL 이 겹치지 않는다.
 * <p>컨트롤러는 HTTP 변환만 하고 로직은 {@link IdentityVerificationService} 에 위임한다.
 */
@Slf4j
@RestController
@RequestMapping("/api/common/identity")
@RequiredArgsConstructor
public class IdentityApiController {

    private final IdentityVerificationService identityService;
    private final IdentityProviderRegistry registry;
    private final TermsService termsService;
    private final IdentitySessionManager sessionManager;

    @GetMapping("/methods")
    public ApiResponse<Set<AuthMethod>> methods() {
        return ApiResponse.success(registry.availableMethods());
    }

    @GetMapping("/terms")
    public ApiResponse<List<TermsDocument>> terms(@RequestParam("method") AuthMethod method) {
        return ApiResponse.success(termsService.getTerms(method));
    }

    /**
     * 인증 시작.
     * <p>세션이 없으면 생성한다(getSession(true)) - 콜백 시 "같은 브라우저인지" 검증하는 기준이 된다.
     */
    @PostMapping("/start")
    public ApiResponse<StartResponse> start(@RequestBody StartRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.success(identityService.start(request, httpRequest.getSession(true)));
    }

    /**
     * 인증 결과 조회 (화면에서 "인증 완료 상태" 표시용).
     * <p>CI/DI 는 절대 화면에 내리지 않는다 → 업무 API 에서 서버 세션으로 직접 사용.
     * <p>미인증이면 success=true, data=null 로 응답 (오류가 아니라 "아직 안 함" 상태).
     */
    @GetMapping("/result")
    public ApiResponse<VerifiedIdentity.MaskedView> result(@RequestParam("purpose") AuthPurpose purpose, HttpSession session) {
        return ApiResponse.success(sessionManager.find(session, purpose).map(VerifiedIdentity::toMaskedView).orElse(null));
    }

    /**
     * 요청 JSON 파싱 실패(존재하지 않는 enum 값 등) → IDV-004.
     * <p>컨트롤러 내부 @ExceptionHandler 라서 이 컨트롤러에만 적용된다. (기존 GlobalExceptionHandler 동작에 영향 없음)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("[공통본인인증] 요청 파싱 실패: {}", e.getMessage());
        IdentityErrorCode code = IdentityErrorCode.INVALID_REQUEST;
        return ResponseEntity.status(code.getStatus()).body(ApiResponse.error(code.getCode(), code.getMessage()));
    }
}
