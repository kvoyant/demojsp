package com.example.demojsp.common.menu.sample;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * [공통#46 샘플] 홈 퀵메뉴 + 전체메뉴 + ADMIN 뱅킹메뉴목록 (WEB-INF/views/common/menu/sample.jsp).
 * <p>APP 홈 JSP: {@code QuickMenu.mount('#quickMenu')} / 전체메뉴: {@code QuickMenu.mountAll('#allMenu')}
 */
@Controller
public class MenuSampleController {

    @GetMapping("/common/menu/sample")
    public String sample() {
        return "common/menu/sample";
    }
}
