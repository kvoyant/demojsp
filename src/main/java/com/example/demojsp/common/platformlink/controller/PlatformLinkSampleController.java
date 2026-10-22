package com.example.demojsp.common.platformlink.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * [공통#1 샘플 화면]
 * <ul>
 *   <li>/common/platform-link/sample : 제휴사 시뮬레이터 - 제휴사 형식 전문을 보내고 내부 처리 단계를 본다</li>
 *   <li>/common/platform-link/admin  : 디지털어드민 - 엔진 전환(LEGACY/SHADOW/NEW) · 제한시간 · 중단 · 처리현황 · 이력</li>
 * </ul>
 */
@Controller
public class PlatformLinkSampleController {

    @GetMapping("/common/platform-link/sample")
    public String sample() {
        return "common/platformlink/sample";
    }

    @GetMapping("/common/platform-link/admin")
    public String admin() {
        return "common/platformlink/admin";
    }
}
