package com.example.demojsp.domain.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/common/auth")
public class NiceCertController {

    /**
     * NICE ID 휴대폰 본인인증 화면 (통신사 미지정 방식 & MO 기기점유인증 통합)
     * APP 및 PC Web 공용 반응형 화면
     */
    @GetMapping("/nice-cert")
    public String niceCertPage(@RequestParam(value = "channel", required = false, defaultValue = "PC_WEB") String channel,
                               Model model) {
        model.addAttribute("channel", channel);
        return "common/auth/niceCert";
    }

    /**
     * 9/30 이기종 CI 데이터 이관 관리 화면 (심사역/관리자 전용)
     */
    @GetMapping("/ci-migration")
    @com.example.demojsp.common.annotation.RequireRole({"ROLE_ADMIN"})
    public String ciMigrationPage(Model model) {
        return "common/auth/ciMigration";
    }
}
