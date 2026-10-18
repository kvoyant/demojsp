package com.example.demojsp.common.menu.web;

import com.example.demojsp.common.menu.config.MenuProperties;
import com.example.demojsp.common.menu.exception.MenuErrorCode;
import com.example.demojsp.common.menu.exception.MenuException;
import com.example.demojsp.domain.login.dto.UserSessionDto;
import com.example.demojsp.domain.login.service.LoginServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

/** 뱅킹메뉴 ADMIN API 권한 확인 (ROLE_ADMIN 세션 또는 local admin-api-open) */
@Component
public class MenuRequestContext {

    private final MenuProperties properties;

    public MenuRequestContext(MenuProperties properties) {
        this.properties = properties;
    }

    public String requireAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object v = session == null ? null : session.getAttribute(LoginServiceImpl.SESSION_USER_KEY);
        UserSessionDto user = v instanceof UserSessionDto u ? u : null;
        if (user != null && "ROLE_ADMIN".equals(user.getRoleCd())) return user.getMemberId();
        if (properties.isAdminApiOpen()) return user != null ? user.getMemberId() : "LOCAL_TEST";
        throw new MenuException(MenuErrorCode.ADMIN_ONLY);
    }
}
