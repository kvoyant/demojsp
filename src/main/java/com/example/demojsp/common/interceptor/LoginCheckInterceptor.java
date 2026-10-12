package com.example.demojsp.common.interceptor;

import com.example.demojsp.domain.login.dto.UserSessionDto;
import com.example.demojsp.domain.login.service.LoginServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * =========================================================================================
 * [로그인 인증 검사 인터셉터 (LoginCheckInterceptor)]
 * - 세션 내 LOGIN_USER 존재 여부를 확인하여 비인가 요청을 차단합니다.
 * - API 요청 시 HTTP 401(SESSION_EXPIRED) JSON 응답을 반환합니다.
 * - 화면 요청 시 /login 페이지로 리다이렉트하며, 원래 접근하고자 했던 URL을 보존합니다.
 * =========================================================================================
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginCheckInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        UserSessionDto loginUser = (session != null) ? (UserSessionDto) session.getAttribute(LoginServiceImpl.SESSION_USER_KEY) : null;

        if (loginUser == null) {
            log.warn("[비인가 접근 차단] 요청 URI: {}, Client IP: {}", request.getRequestURI(), request.getRemoteAddr());

            if (isAjaxOrApiRequest(request)) {
                sendJsonUnauthorized(response);
            } else {
                String requestUri = request.getRequestURI();
                String queryString = request.getQueryString();
                String targetUrl = (queryString != null) ? requestUri + "?" + queryString : requestUri;
                String encodedTarget = URLEncoder.encode(targetUrl, StandardCharsets.UTF_8);

                response.sendRedirect(request.getContextPath() + "/login?redirectUrl=" + encodedTarget);
            }
            return false;
        }

        return true;
    }

    private boolean isAjaxOrApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/")) {
            return true;
        }
        String requestedWith = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
            return true;
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }

    private void sendJsonUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"status\":401,\"code\":\"U007\",\"message\":\"세션이 만료되었습니다. 다시 로그인해 주세요.\",\"data\":null}");
    }
}
