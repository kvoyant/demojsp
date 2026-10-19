package com.example.demojsp.common.notice.controller;

import com.example.demojsp.common.notice.dto.NoticeBizDto;
import com.example.demojsp.common.notice.dto.NoticeDto;
import com.example.demojsp.common.notice.dto.NoticeFilters;
import com.example.demojsp.common.notice.dto.NoticeTypeDto;
import com.example.demojsp.common.notice.service.NoticeAdminService;
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

/**
 * [디지털어드민] 공지사항 관리 API (REQ-AP-610)
 *
 * <pre>
 *  GET  /api/common/notice-admin/notices?scope=&type=&q=&limit=   목록 (상태: 게시중/게시예약/게시종료/미사용)
 *  GET  /api/common/notice-admin/notices/{id}                     상세 (수정 폼)
 *  POST /api/common/notice-admin/notices                          등록(noticeId 없음) / 수정
 *  GET  /api/common/notice-admin/types        POST .../types       공지 유형 목록 / 등록·수정
 *  GET  /api/common/notice-admin/bizs         POST .../bizs        업무 구분 목록 / 등록·수정
 *  GET  /api/common/notice-admin/targets                          대상 선택 상자 (상품 · 업무)
 * </pre>
 */
@RestController
@RequestMapping("/api/common/notice-admin")
public class NoticeAdminApiController {

    private final NoticeAdminService service;
    private final NoticeAdminAuth auth;

    public NoticeAdminApiController(NoticeAdminService service, NoticeAdminAuth auth) {
        this.service = service;
        this.auth = auth;
    }

    @GetMapping("/notices")
    public ApiResponse<List<NoticeDto>> list(@RequestParam(value = "scope", required = false) String scope,
                                             @RequestParam(value = "type", required = false) String type,
                                             @RequestParam(value = "q", required = false) String keyword,
                                             @RequestParam(value = "limit", defaultValue = "100") int limit,
                                             HttpServletRequest request) {
        auth.requireAdmin(request);
        return ApiResponse.success(service.list(scope, type, keyword, limit));
    }

    @GetMapping("/notices/{noticeId}")
    public ApiResponse<NoticeDto> get(@PathVariable("noticeId") long noticeId, HttpServletRequest request) {
        auth.requireAdmin(request);
        return ApiResponse.success(service.get(noticeId));
    }

    @PostMapping("/notices")
    public ApiResponse<NoticeDto> save(@RequestBody NoticeDto body, HttpServletRequest request) {
        boolean create = body.getNoticeId() == null;
        NoticeDto saved = service.save(body, auth.requireAdmin(request));
        return ApiResponse.success(create ? "공지사항이 등록되었습니다." : "공지사항이 수정되었습니다.", saved);
    }

    @GetMapping("/types")
    public ApiResponse<List<NoticeTypeDto>> types(HttpServletRequest request) {
        auth.requireAdmin(request);
        return ApiResponse.success(service.types());
    }

    @PostMapping("/types")
    public ApiResponse<NoticeTypeDto> saveType(@RequestBody NoticeTypeDto body, HttpServletRequest request) {
        return ApiResponse.success("공지 유형이 저장되었습니다.", service.saveType(body, auth.requireAdmin(request)));
    }

    @GetMapping("/bizs")
    public ApiResponse<List<NoticeBizDto>> bizs(HttpServletRequest request) {
        auth.requireAdmin(request);
        return ApiResponse.success(service.bizs());
    }

    @PostMapping("/bizs")
    public ApiResponse<NoticeBizDto> saveBiz(@RequestBody NoticeBizDto body, HttpServletRequest request) {
        return ApiResponse.success("업무 구분이 저장되었습니다.", service.saveBiz(body, auth.requireAdmin(request)));
    }

    @GetMapping("/targets")
    public ApiResponse<NoticeFilters> targets(HttpServletRequest request) {
        auth.requireAdmin(request);
        return ApiResponse.success(service.targets());
    }
}
