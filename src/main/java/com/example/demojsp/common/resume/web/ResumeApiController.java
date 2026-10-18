package com.example.demojsp.common.resume.web;

import com.example.demojsp.common.resume.domain.ResumeCardView;
import com.example.demojsp.common.resume.domain.ResumeSummary;
import com.example.demojsp.common.resume.service.ResumeService;
import com.example.demojsp.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * [CF-BC-13 / 공통#43] 이어하기 고객 API (홈/저축/대출 공통 - scope 파라미터).
 *
 * <pre>
 *  GET  /api/common/resume?scope=ALL            이어하기 카드: 건수 + 최신 1건 + 범위별 건수 + 더보기 URL
 *  GET  /api/common/resume/list?scope=ALL       신청현황조회: 진행 중 전체 (최신순)
 *  GET  /api/common/resume/counts               범위별 진행 중 건수 {ALL, DEPOSIT, LOAN} - 하단 탭 툴팁(공통#50), 비로그인 0
 *  POST /api/common/resume/{id}/continue        [이어하기] 클릭 → 본인·진행중·만료 전 재확인 후 이동 URL
 * </pre>
 * 고객은 로그인 세션으로만 식별한다 ({@link ResumeRequestContext}).
 */
@RestController
@RequestMapping("/api/common/resume")
public class ResumeApiController {

    private final ResumeService resumeService;
    private final ResumeRequestContext context;

    public ResumeApiController(ResumeService resumeService, ResumeRequestContext context) {
        this.resumeService = resumeService;
        this.context = context;
    }

    @GetMapping
    public ApiResponse<ResumeSummary> summary(@RequestParam(value = "scope", defaultValue = "ALL") String scope,
                                              HttpServletRequest request) {
        return ApiResponse.success(resumeService.summary(context.customerId(request), scope));
    }

    @GetMapping("/list")
    public ApiResponse<List<ResumeCardView>> list(@RequestParam(value = "scope", defaultValue = "ALL") String scope,
                                                  HttpServletRequest request) {
        return ApiResponse.success(resumeService.list(context.customerId(request), scope));
    }

    @GetMapping("/counts")
    public ApiResponse<Map<String, Integer>> counts(HttpServletRequest request) {
        return ApiResponse.success(resumeService.counts(context.customerIdOrNull(request)));
    }

    @PostMapping("/{id}/continue")
    public ApiResponse<Map<String, String>> continueResume(@PathVariable("id") long id, HttpServletRequest request) {
        String url = resumeService.continueUrl(context.customerId(request), id);
        return ApiResponse.success("이어서 진행합니다.", Map.of("url", url));
    }
}
