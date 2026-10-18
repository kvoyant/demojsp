package com.example.demojsp.common.banner.web;

import com.example.demojsp.common.banner.domain.BannerDisplayList;
import com.example.demojsp.common.banner.domain.BannerPopupAdminView;
import com.example.demojsp.common.banner.domain.BannerPopupRequest;
import com.example.demojsp.common.banner.exception.BannerErrorCode;
import com.example.demojsp.common.banner.service.BannerPopupService;
import com.example.demojsp.common.banner.service.BannerSupport;
import com.example.demojsp.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * [CF-BC-20 / 공통#39] 배너팝업 관리(ADMIN) API - 디지털어드민 "마케팅 > 배너관리 > 배너팝업목록".
 *
 * <pre>
 *  GET    /api/common/banner-admin/codes                       배너팝업구분 / 공지유형 / 채널 코드
 *  GET    /api/common/banner-admin/popups?popupType=TOP_SLIDE  목록 (구분 필터 선택, 게시상태, 중복게시 건수, 표시순서 중복 여부)
 *  GET    /api/common/banner-admin/popups/{id}                 상세
 *  POST   /api/common/banner-admin/popups                      등록
 *  PUT    /api/common/banner-admin/popups/{id}                 수정
 *  POST   /api/common/banner-admin/popups/{id}/use?use=false   사용여부 변경 (즉시 게시중지/재게시)
 *  DELETE /api/common/banner-admin/popups/{id}                 삭제
 *  GET    /api/common/banner-admin/preview?channel=APP&at=yyyy-MM-dd HH:mm   기준시각 노출 미리보기
 * </pre>
 *
 * <p>권한: {@link BannerRequestContext#requireAdmin} - ROLE_ADMIN 세션 또는 local 전용 {@code admin-api-open=true}.
 */
@RestController
@RequestMapping("/api/common/banner-admin")
public class BannerPopupAdminApiController {

    private final BannerPopupService bannerPopupService;
    private final BannerRequestContext context;

    public BannerPopupAdminApiController(BannerPopupService bannerPopupService, BannerRequestContext context) {
        this.bannerPopupService = bannerPopupService;
        this.context = context;
    }

    @GetMapping("/codes")
    public ApiResponse<Map<String, Map<String, String>>> codes(HttpServletRequest request) {
        context.requireAdmin(request);
        return ApiResponse.success(bannerPopupService.codes());
    }

    @GetMapping("/popups")
    public ApiResponse<List<BannerPopupAdminView>> list(@RequestParam(value = "popupType", required = false) String popupType,
                                                        HttpServletRequest request) {
        context.requireAdmin(request);
        return ApiResponse.success(bannerPopupService.adminList(popupType));
    }

    @GetMapping("/popups/{id}")
    public ApiResponse<BannerPopupAdminView> detail(@PathVariable("id") long id, HttpServletRequest request) {
        context.requireAdmin(request);
        return ApiResponse.success(bannerPopupService.adminDetail(id));
    }

    @PostMapping("/popups")
    public ApiResponse<BannerPopupAdminView> register(@RequestBody BannerPopupRequest body, HttpServletRequest request) {
        String adminId = context.requireAdmin(request);
        return ApiResponse.success("배너팝업이 등록되었습니다.", bannerPopupService.register(body, adminId));
    }

    @PutMapping("/popups/{id}")
    public ApiResponse<BannerPopupAdminView> update(@PathVariable("id") long id, @RequestBody BannerPopupRequest body,
                                                    HttpServletRequest request) {
        String adminId = context.requireAdmin(request);
        return ApiResponse.success("배너팝업이 수정되었습니다.", bannerPopupService.update(id, body, adminId));
    }

    @PostMapping("/popups/{id}/use")
    public ApiResponse<BannerPopupAdminView> changeUse(@PathVariable("id") long id, @RequestParam("use") boolean use,
                                                       HttpServletRequest request) {
        String adminId = context.requireAdmin(request);
        return ApiResponse.success(use ? "사용으로 변경되었습니다." : "미사용으로 변경되었습니다.",
                bannerPopupService.changeUse(id, use, adminId));
    }

    @DeleteMapping("/popups/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") long id, HttpServletRequest request) {
        String adminId = context.requireAdmin(request);
        bannerPopupService.delete(id, adminId);
        return ApiResponse.success("배너팝업이 삭제되었습니다.", null);
    }

    /** 기준시각 노출 미리보기 - 게시일자가 겹치는 시점에 어떤 순서로 슬라이드되는지 ADMIN 이 확인 */
    @GetMapping("/preview")
    public ApiResponse<BannerDisplayList> preview(@RequestParam(value = "channel", defaultValue = "APP") String channel,
                                                  @RequestParam(value = "at", required = false) String at,
                                                  HttpServletRequest request) {
        context.requireAdmin(request);
        if (at == null || at.isBlank()) {
            return ApiResponse.success(bannerPopupService.displayable(channel));
        }
        return ApiResponse.success(bannerPopupService.displayable(channel, BannerSupport.parse(at, "기준시각")));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        BannerErrorCode code = BannerErrorCode.INVALID_REQUEST;
        return ResponseEntity.status(code.getStatus()).body(ApiResponse.error(code.getCode(), code.getMessage()));
    }
}
