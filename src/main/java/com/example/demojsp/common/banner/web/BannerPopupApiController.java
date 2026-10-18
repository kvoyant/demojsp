package com.example.demojsp.common.banner.web;

import com.example.demojsp.common.banner.domain.BannerDisplayList;
import com.example.demojsp.common.banner.service.BannerPopupService;
import com.example.demojsp.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * [CF-BC-20 / 공통#39] 배너팝업 고객 API (APP 메인 진입 시 호출).
 *
 * <pre>
 *  GET /api/common/banner-popups?channel=APP     현재 노출 대상 배너 (표시순서 정렬, 2건 이상이면 slide=true)
 * </pre>
 * 로그인 불필요 (메인 진입 전 공지). 기준시각은 서버 시각만 사용 - 고객 요청으로 시각을 바꿀 수 없다.
 */
@RestController
public class BannerPopupApiController {

    private final BannerPopupService bannerPopupService;

    public BannerPopupApiController(BannerPopupService bannerPopupService) {
        this.bannerPopupService = bannerPopupService;
    }

    @GetMapping("/api/common/banner-popups")
    public ApiResponse<BannerDisplayList> list(@RequestParam(value = "channel", defaultValue = "APP") String channel) {
        return ApiResponse.success(bannerPopupService.displayable(channel));
    }
}
