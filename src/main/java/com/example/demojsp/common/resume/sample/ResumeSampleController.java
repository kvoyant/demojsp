package com.example.demojsp.common.resume.sample;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.regex.Pattern;

/**
 * [업무 화면 예시] 이어하기 공통모듈 사용 방법 (샘플 - 실제 APP 홈 적용 후 삭제 가능).
 *
 * <p>APP 홈 JSP 에서는 아래면 된다.
 * <pre>
 *  &lt;div id="resumeCard"&gt;&lt;/div&gt;     ← 화면 상단 (공통#43 기능6)
 *  &lt;script src="/js/common/resume-card.js"&gt;&lt;/script&gt;
 *  ResumeCard.mount('#resumeCard', { scope: 'ALL' });      // 저축 탭: 'DEPOSIT' / 대출 탭: 'LOAN'
 * </pre>
 */
@Controller
public class ResumeSampleController {

    private static final Pattern SAFE = Pattern.compile("^[A-Za-z0-9_]{1,30}$");

    /** 샘플 화면 (APP 홈 목업 + 테스트 도구) */
    @GetMapping("/common/resume/sample")
    public String sample() {
        return "common/resume/sample";
    }

    /** 신청현황조회 (공통#43 [더보기] 이동 화면) - 실제 화면은 REQ-IA-APP-0015 신청현황조회 */
    @GetMapping("/common/resume/status")
    public String status(@RequestParam(value = "scope", defaultValue = "ALL") String scope,
                         @RequestParam(value = "demoCustomer", required = false) String demoCustomer,
                         Model model) {
        // JS 에 출력되므로 허용 문자만 전달 (XSS 방지)
        model.addAttribute("scope", SAFE.matcher(scope).matches() ? scope : "ALL");
        model.addAttribute("demoCustomer", demoCustomer != null && SAFE.matcher(demoCustomer).matches() ? demoCustomer : "");
        return "common/resume/status";
    }

    /** [이어하기] 도착 화면 흉내 (실제는 각 상품 신청 화면의 해당 단계) */
    @GetMapping("/common/resume/landing")
    public String landing(@RequestParam(value = "biz", defaultValue = "") String biz,
                          @RequestParam(value = "product", defaultValue = "") String product,
                          @RequestParam(value = "step", defaultValue = "1") int step,
                          Model model) {
        model.addAttribute("biz", SAFE.matcher(biz).matches() ? biz : "");
        model.addAttribute("product", SAFE.matcher(product).matches() ? product : "");
        model.addAttribute("step", Math.max(1, Math.min(step, 50)));
        return "common/resume/landing";
    }
}
