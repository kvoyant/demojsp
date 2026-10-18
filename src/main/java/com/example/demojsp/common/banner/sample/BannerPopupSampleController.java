package com.example.demojsp.common.banner.sample;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * [업무 화면 예시] 배너팝업 공통모듈 사용 방법 (샘플 - 실제 APP 메인 적용 후 삭제 가능).
 *
 * <p>APP 메인 JSP 에서는 아래 두 줄이면 된다.
 * <pre>
 *  &lt;script src="/js/common/banner-popup.js"&gt;&lt;/script&gt;
 *  BannerPopup.open({ channel: 'APP' });
 * </pre>
 */
@Controller
public class BannerPopupSampleController {

    /** 샘플 화면 (WEB-INF/views/common/banner/sample.jsp) */
    @GetMapping("/common/banner/sample")
    public String page() {
        return "common/banner/sample";
    }

    /**
     * [공통#44] 홈 상단슬라이드배너 샘플 (WEB-INF/views/common/banner/top-slide.jsp).
     * <p>APP 홈 JSP 에서는 {@code TopSlideBanner.mount('#topSlide', { channel: 'APP', onIneligible: ... })} 한 줄.
     */
    @GetMapping("/common/banner/top-slide")
    public String topSlide() {
        return "common/banner/top-slide";
    }

    /**
     * [공통#45] 디지털어드민 "마케팅 > 배너관리 > 배너팝업목록" (WEB-INF/views/common/banner/admin.jsp).
     * <p>기존 화면에 배너팝업구분 "상단슬라이드배너" 와 이미지 파일선택·대체텍스트·링크·배경색·글자 수 제한 항목을 추가한 형태.
     */
    @GetMapping("/common/banner/admin")
    public String admin() {
        return "common/banner/admin";
    }
}
