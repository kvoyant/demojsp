package com.example.demojsp.common.notice.controller;

import com.example.demojsp.common.notice.config.NoticeProperties;
import com.example.demojsp.common.notice.dto.NoticeDto;
import com.example.demojsp.common.notice.dto.NoticeFilters;
import com.example.demojsp.common.notice.dto.NoticePage;
import com.example.demojsp.common.notice.dto.NoticeStripView;
import com.example.demojsp.common.notice.service.NoticeService;
import com.example.demojsp.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * [CF-BC-20 / 공통#28] 공지사항 - 사용자 화면 API (화면 공통 컴포넌트 notice.js 가 호출).
 *
 * <pre>
 *  GET /api/common/notice/strip                       메인 띠 최신공지 1건 + 상세 URL
 *  GET /api/common/notice?scope=&target=&type=&page=  목록 (scope 없음=전체탭, PRODUCT 상품별, BIZ 업무별, ALL 전체공지만)
 *  GET /api/common/notice/filters                     목록 탭 건수 · 상품/업무 칩
 *  GET /api/common/notice/{id}                        상세 (+조회수)
 *  GET /api/common/notice/popups                      메인 팝업 대상
 * </pre>
 * 로그인 없이 조회할 수 있다 (대고객 안내).
 */
@RestController
@RequestMapping("/api/common/notice")
public class NoticeApiController {

    private final NoticeService service;
    private final NoticeProperties properties;

    public NoticeApiController(NoticeService service, NoticeProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @GetMapping("/strip")
    public ApiResponse<NoticeStripView> strip() {
        NoticeDto n = service.strip();
        return ApiResponse.success(new NoticeStripView(n, n == null ? null : properties.getDetailUrl() + "?id=" + n.getNoticeId()));
    }

    @GetMapping
    public ApiResponse<NoticePage> list(@RequestParam(value = "scope", required = false) String scope,
                                        @RequestParam(value = "target", required = false) String target,
                                        @RequestParam(value = "type", required = false) String type,
                                        @RequestParam(value = "page", defaultValue = "1") int page) {
        return ApiResponse.success(service.list(scope, target, type, page));
    }

    @GetMapping("/filters")
    public ApiResponse<NoticeFilters> filters() {
        return ApiResponse.success(service.filters());
    }

    @GetMapping("/popups")
    public ApiResponse<List<NoticeDto>> popups() {
        return ApiResponse.success(service.popups());
    }

    @GetMapping("/{noticeId}")
    public ApiResponse<NoticeDto> detail(@PathVariable("noticeId") long noticeId) {
        return ApiResponse.success(service.detail(noticeId));
    }
}
