package com.example.demojsp.domain.login.controller;

import com.example.demojsp.domain.login.dto.UserSessionDto;
import com.example.demojsp.domain.login.service.LoginService;
import com.example.demojsp.domain.login.service.LoginServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * =========================================================================================
 * [로그인 화면 및 로그아웃 뷰 컨트롤러 (LoginController)]
 * =========================================================================================
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "redirectUrl", required = false) String redirectUrl,
                            @RequestParam(value = "logout", required = false) String logout,
                            HttpServletRequest request,
                            Model model) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            UserSessionDto loginUser = (UserSessionDto) session.getAttribute(LoginServiceImpl.SESSION_USER_KEY);
            if (loginUser != null) {
                // 이미 로그인되어 있으면 대시보드/게시판으로 이동
                return "redirect:" + (redirectUrl != null && !redirectUrl.isBlank() ? redirectUrl : "/board/list");
            }
        }

        model.addAttribute("redirectUrl", redirectUrl != null ? redirectUrl : "/board/list");
        model.addAttribute("isLogout", "true".equalsIgnoreCase(logout));
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        loginService.logout(request);
        return "redirect:/login?logout=true";
    }
}
