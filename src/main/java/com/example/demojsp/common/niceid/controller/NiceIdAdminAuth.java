package com.example.demojsp.common.niceid.controller;

import com.example.demojsp.common.niceid.config.NiceIdProperties;
import com.example.demojsp.common.niceid.exception.NiceIdErrorCode;
import com.example.demojsp.common.niceid.exception.NiceIdException;
import com.example.demojsp.domain.login.dto.UserSessionDto;
import com.example.demojsp.domain.login.service.LoginServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

/** 본인인증 ADMIN API 권한 확인 → 관리자 ID (ROLE_ADMIN 세션, 또는 local 에서 common.niceid.admin-api-open=true) */
@Component
public class NiceIdAdminAuth {

    private final NiceIdProperties properties;

    public NiceIdAdminAuth(NiceIdProperties properties) {
        this.properties = properties;
    }

    public String requireAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object v = session == null ? null : session.getAttribute(LoginServiceImpl.SESSION_USER_KEY);
        UserSessionDto user = v instanceof UserSessionDto u ? u : null;
        if (user != null && "ROLE_ADMIN".equals(user.getRoleCd())) return user.getMemberId();
        if (properties.isAdminApiOpen()) return user != null ? user.getMemberId() : "LOCAL_TEST";
        throw new NiceIdException(NiceIdErrorCode.ADMIN_ONLY);
    }
}
