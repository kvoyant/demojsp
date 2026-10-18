package com.example.demojsp.common.resume.web;

import com.example.demojsp.common.resume.domain.Partner;
import com.example.demojsp.common.resume.domain.ResumePointRequest;
import com.example.demojsp.common.resume.exception.ResumeErrorCode;
import com.example.demojsp.common.resume.service.ResumeService;
import com.example.demojsp.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * [local 테스트 전용] 신청 업무 연동 흉내 API - {@code common.resume.demo-api-open=true} 일 때만 동작.
 *
 * <p>운영에서는 각 신청 업무(대출신청·계좌개설 등)가 {@link ResumeService#saveStep} / {@code complete} / {@code cancel} 을
 * 서버 내부에서 직접 호출한다. 고객이 외부에서 이어하기를 만들 수 있는 API 는 운영에 두지 않는다.
 *
 * <pre>
 *  GET  /api/common/resume-demo/partners            회사 마스터 (회사명 DB)
 *  POST /api/common/resume-demo/steps               단계 저장 (등록 또는 같은 업무·상품이면 갱신)
 *  POST /api/common/resume-demo/{id}/complete       신청 완료
 *  POST /api/common/resume-demo/{id}/cancel         신청 취소
 * </pre>
 */
@RestController
@RequestMapping("/api/common/resume-demo")
public class ResumeDemoApiController {

    private final ResumeService resumeService;
    private final ResumeRequestContext context;

    public ResumeDemoApiController(ResumeService resumeService, ResumeRequestContext context) {
        this.resumeService = resumeService;
        this.context = context;
    }

    @GetMapping("/partners")
    public ApiResponse<List<Partner>> partners() {
        context.requireDemo();
        return ApiResponse.success(resumeService.partners());
    }

    @PostMapping("/steps")
    public ApiResponse<Map<String, Long>> saveStep(@RequestBody ResumePointRequest body, HttpServletRequest request) {
        context.requireDemo();
        long id = resumeService.saveStep(context.customerId(request), body);
        return ApiResponse.success("이어하기 단계가 저장되었습니다.", Map.of("id", id));
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<Void> complete(@PathVariable("id") long id, HttpServletRequest request) {
        context.requireDemo();
        resumeService.complete(context.customerId(request), id);
        return ApiResponse.success("신청이 완료 처리되었습니다.", null);
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(@PathVariable("id") long id, HttpServletRequest request) {
        context.requireDemo();
        resumeService.cancel(context.customerId(request), id);
        return ApiResponse.success("신청이 취소 처리되었습니다.", null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        ResumeErrorCode code = ResumeErrorCode.INVALID_REQUEST;
        return ResponseEntity.status(code.getStatus()).body(ApiResponse.error(code.getCode(), code.getMessage()));
    }
}
