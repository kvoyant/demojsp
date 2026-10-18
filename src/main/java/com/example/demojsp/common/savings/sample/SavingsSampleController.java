package com.example.demojsp.common.savings.sample;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * [공통#49~51 샘플] APP 저축 탭 (WEB-INF/views/common/savings/sample.jsp).
 *
 * <ul>
 *   <li>공통#49 저축 - 이어하기: {@code ResumeCard.mount('#resumeCard', { scope: 'DEPOSIT' })}</li>
 * </ul>
 */
@Controller
public class SavingsSampleController {

    @GetMapping("/common/savings/sample")
    public String sample() {
        return "common/savings/sample";
    }
}
