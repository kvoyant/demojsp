package com.example.demojsp.common.testhub;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 공통모듈 테스트 허브 - 요구사항별 공통모듈 샘플/API 를 한 화면에서 목록으로 확인한다.
 *
 * <pre>
 *  GET /common/test-hub   (WEB-INF/views/common/testhub/list.jsp)
 * </pre>
 * 항목 추가: application.yaml {@code common.test-hub.items} 에 한 블록 추가 (Java/JSP 수정 불필요).
 */
@Controller
@EnableConfigurationProperties(TestHubProperties.class)
public class TestHubController {

    private final TestHubProperties properties;

    public TestHubController(TestHubProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/common/test-hub")
    public String list(Model model) {
        model.addAttribute("items", properties.getItems());
        model.addAttribute("total", properties.getItems().size());
        model.addAttribute("doneCount", properties.getItems().stream().filter(i -> "개발완료".equals(i.getStatus())).count());
        return "common/testhub/list";
    }
}
