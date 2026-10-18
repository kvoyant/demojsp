package com.example.demojsp.common.banner.web;

import com.example.demojsp.common.banner.domain.TopSlideBannerList;
import com.example.demojsp.common.banner.service.BannerPopupService;
import com.example.demojsp.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * [CF-BC-20 / 공통#44] 홈 상단슬라이드배너 고객 API (APP 홈 진입 시 호출).
 *
 * <pre>
 *  GET /api/common/top-slide-banners?channel=APP
 *      → eligible(계좌 미보유=true), items(최대 5건, 표시순서), intervalMs(2000), total
 * </pre>
 * 로그인 불필요 (비로그인 = 계좌 미보유로 보고 노출). 기준시각은 서버 시각만 사용.
 */
@RestController
public class TopSlideBannerApiController {

    private final BannerPopupService bannerPopupService;
    private final BannerRequestContext context;

    public TopSlideBannerApiController(BannerPopupService bannerPopupService, BannerRequestContext context) {
        this.bannerPopupService = bannerPopupService;
        this.context = context;
    }

    @GetMapping("/api/common/top-slide-banners")
    public ApiResponse<TopSlideBannerList> list(@RequestParam(value = "channel", defaultValue = "APP") String channel,
                                                HttpServletRequest request) {
        return ApiResponse.success(bannerPopupService.topSlides(channel, context.customerIdOrNull(request)));
    }
}
