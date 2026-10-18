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
}
