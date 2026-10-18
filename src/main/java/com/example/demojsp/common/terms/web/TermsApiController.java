package com.example.demojsp.common.terms.web;

import com.example.demojsp.common.response.ApiResponse;
import com.example.demojsp.common.terms.domain.AgreeRequest;
import com.example.demojsp.common.terms.domain.AgreeResult;
import com.example.demojsp.common.terms.domain.TermsItem;
import com.example.demojsp.common.terms.domain.TermsSet;
import com.example.demojsp.common.terms.exception.TermsErrorCode;
import com.example.demojsp.common.terms.service.TermsAgreeService;
import com.example.demojsp.common.terms.service.TermsSessionManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [CF-BC-04] 약관동의 공통 API - 모든 채널(APP / PC WEB / 모집인WEB) 공용.
 *
 * <pre>
 *  GET  /api/common/terms/{bizCode}                    업무코드 약관 목록 (필수/선택 분리, 전문 제외)
 *  GET  /api/common/terms/{bizCode}/items/{termsCode}  약관 상세보기 (전문)
 *  POST /api/common/terms/{bizCode}/agree              동의 처리 → 증적 저장 + 세션 반영
 *  GET  /api/common/terms/{bizCode}/status             현재 세션의 동의 결과 (없으면 data=null)
 * </pre>
 * 화면은 공통 JS {@code /js/common/terms-agree.js} 를 통해 호출한다.
 */
@RestController
@RequestMapping("/api/common/terms")
public class TermsApiController {

    private static final Logger log = LoggerFactory.getLogger(TermsApiController.class);

    private final TermsAgreeService termsAgreeService;
    private final TermsSessionManager sessionManager;
    private final TermsRequestContext context;

    public TermsApiController(TermsAgreeService termsAgreeService, TermsSessionManager sessionManager,
                              TermsRequestContext context) {
        this.termsAgreeService = termsAgreeService;
        this.sessionManager = sessionManager;
        this.context = context;
    }

    @GetMapping("/{bizCode}")
    public ApiResponse<TermsSet> list(@PathVariable("bizCode") String bizCode) {
        return ApiResponse.success(termsAgreeService.getTermsSet(bizCode));
    }

    @GetMapping("/{bizCode}/items/{termsCode}")
    public ApiResponse<TermsItem> detail(@PathVariable("bizCode") String bizCode,
                                         @PathVariable("termsCode") String termsCode) {
        return ApiResponse.success(termsAgreeService.getDetail(bizCode, termsCode));
    }

    @PostMapping("/{bizCode}/agree")
    public ApiResponse<AgreeResult> agree(@PathVariable("bizCode") String bizCode,
                                          @RequestBody AgreeRequest request,
                                          HttpServletRequest httpRequest) {
        AgreeResult result = termsAgreeService.agree(bizCode, request,
                context.subjectId(httpRequest), context.clientIp(httpRequest));
        sessionManager.save(httpRequest.getSession(true), result);
        return ApiResponse.success("약관 동의가 완료되었습니다.", result);
    }

    @GetMapping("/{bizCode}/status")
    public ApiResponse<AgreeResult> status(@PathVariable("bizCode") String bizCode, HttpSession session) {
        return ApiResponse.success(sessionManager.find(session, bizCode).orElse(null));
    }

    /** JSON 파싱 실패 → TRM-004 (이 컨트롤러에만 적용) */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("[약관동의] 요청 파싱 실패: {}", e.getMessage());
        TermsErrorCode code = TermsErrorCode.INVALID_REQUEST;
        return ResponseEntity.status(code.getStatus()).body(ApiResponse.error(code.getCode(), code.getMessage()));
    }
}
