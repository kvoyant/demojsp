package com.example.demojsp.common.platformlink.controller;

import com.example.demojsp.common.platformlink.config.PlatformLinkProperties;
import com.example.demojsp.common.platformlink.exception.PlatformLinkErrorCode;
import com.example.demojsp.common.platformlink.exception.PlatformLinkException;
import com.example.demojsp.domain.login.dto.UserSessionDto;
import com.example.demojsp.domain.login.service.LoginServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

/** 플랫폼 연계 ADMIN API 권한 → 관리자 ID (ROLE_ADMIN 세션, 또는 local 에서 common.platform-link.admin-api-open=true) */
@Component
public class PlatformLinkAdminAuth {

    private final PlatformLinkProperties properties;

    public PlatformLinkAdminAuth(PlatformLinkProperties properties) {
        this.properties = properties;
    }

    public String requireAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object v = session == null ? null : session.getAttribute(LoginServiceImpl.SESSION_USER_KEY);
        UserSessionDto user = v instanceof UserSessionDto u ? u : null;
        if (user != null && "ROLE_ADMIN".equals(user.getRoleCd())) return user.getMemberId();
        if (properties.isAdminApiOpen()) return user != null ? user.getMemberId() : "LOCAL_TEST";
        throw new PlatformLinkException(PlatformLinkErrorCode.ADMIN_ONLY);
    }
}
