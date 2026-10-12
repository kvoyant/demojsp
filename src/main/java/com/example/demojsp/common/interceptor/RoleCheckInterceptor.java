package com.example.demojsp.common.interceptor;

import com.example.demojsp.common.annotation.RequireRole;
import com.example.demojsp.domain.login.dto.UserSessionDto;
import com.example.demojsp.domain.login.service.LoginServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

/**
 * =========================================================================================
 * [역할 기반 인가 검사 인터셉터 (RoleCheckInterceptor)]
 * - @RequireRole 어노테이션을 감지하여 사용자의 roleCd(ROLE_CUSTOMER, ROLE_COUNSELOR, ROLE_ADMIN)를 검증합니다.
 * - 권한 미보유 시 접근 차단(HTTP 403 Forbidden)을 처리합니다.
 * =========================================================================================
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoleCheckInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;

        // 메서드 레벨 어노테이션 우선, 없으면 클래스 레벨 확인
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), RequireRole.class);
        }

        if (requireRole == null) {
            return true; // 별도 권한 제한 없음
        }

        HttpSession session = request.getSession(false);
        UserSessionDto loginUser = (session != null) ? (UserSessionDto) session.getAttribute(LoginServiceImpl.SESSION_USER_KEY) : null;

        if (loginUser == null) {
            // 미로그인 상태는 LoginCheckInterceptor에서 처리되거나 여기서 401
            sendForbiddenResponse(request, response, "로그인이 필요한 서비스입니다.");
            return false;
        }

        String userRole = loginUser.getRoleCd();
        String[] allowedRoles = requireRole.value();

        boolean hasRole = Arrays.asList(allowedRoles).contains(userRole);
        if (!hasRole) {
            log.warn("[권한 부족 접근 차단] 사용자: {}({}), 필요권한: {}, 접근URL: {}",
                    loginUser.getMemberId(), userRole, Arrays.toString(allowedRoles), request.getRequestURI());

            sendForbiddenResponse(request, response, "해당 메뉴에 대한 접근 권한(" + Arrays.toString(allowedRoles) + ")이 없습니다.");
            return false;
        }

        return true;
    }

    private void sendForbiddenResponse(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {
        String uri = request.getRequestURI();
        boolean isApi = uri.startsWith("/api/") || "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"));

        if (isApi) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"status\":403,\"code\":\"U006\",\"message\":\"해당 메뉴 또는 기능에 대한 접근 권한이 없습니다.\",\"data\":null}");
        } else {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("text/html;charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.println("<script>");
            out.println("alert('" + message + "');");
            out.println("history.back();");
            out.println("</script>");
            out.flush();
        }
    }
}
