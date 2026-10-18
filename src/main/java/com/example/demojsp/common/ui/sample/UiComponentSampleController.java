package com.example.demojsp.common.ui.sample;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;

/**
 * [CF-UI-01] 공통 UI 컴포넌트 샘플 화면 (Button 공통#40 · Icon 공통#41 · Alert 공통#42).
 *
 * <p>공통 UI 컴포넌트는 서버 로직 없이 정적 자원(CSS/JS/SVG)으로 제공된다. 업무 JSP 에서는 아래만 포함하면 된다.
 * <pre>
 *  &lt;link rel="stylesheet" href="/css/common/ui-tokens.css"&gt;
 *  &lt;link rel="stylesheet" href="/css/common/ui-button.css"&gt;
 *  &lt;script src="/js/common/ui-button.js"&gt;&lt;/script&gt;
 * </pre>
 * 이 컨트롤러는 샘플·테스트 허브 확인용이다 (실 화면 적용 후 삭제 가능).
 */
@Controller
public class UiComponentSampleController {

    /** 공통#40 Button 샘플 (WEB-INF/views/common/ui/button.jsp) */
    @GetMapping("/common/ui/button")
    public String button(@RequestParam(value = "from", required = false) String from,
                         @RequestParam(value = "delayMs", defaultValue = "0") long delayMs,
                         Model model) throws InterruptedException {
        // 페이지 이동 데모: 로컬은 응답이 너무 빨라 "이동 중" 표시를 볼 수 없으므로 지연을 흉내낸다 (최대 3초)
        long delay = Math.max(0, Math.min(delayMs, MAX_DEMO_DELAY_MS));
        if (delay > 0) {
            Thread.sleep(delay);
        }
        model.addAttribute("navigatedFrom", "nav-demo".equals(from));
        return "common/ui/button";
    }

    /** 샘플 지연 상한 (요청 스레드 점유 방지) */
    static final long MAX_DEMO_DELAY_MS = 3000;
}
