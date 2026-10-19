package com.example.demojsp.common.notice.controller;

import com.example.demojsp.common.notice.config.NoticeProperties;
import com.example.demojsp.common.notice.exception.NoticeErrorCode;
import com.example.demojsp.common.notice.exception.NoticeException;
import com.example.demojsp.domain.login.dto.UserSessionDto;
import com.example.demojsp.domain.login.service.LoginServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

/** 공지사항 ADMIN API 권한 확인 → 관리자 ID (ROLE_ADMIN 세션, 또는 local 에서 common.notice.admin-api-open=true) */
@Component
public class NoticeAdminAuth {

    private final NoticeProperties properties;

    public NoticeAdminAuth(NoticeProperties properties) {
        this.properties = properties;
    }

    public String requireAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object v = session == null ? null : session.getAttribute(LoginServiceImpl.SESSION_USER_KEY);
        UserSessionDto user = v instanceof UserSessionDto u ? u : null;
        if (user != null && "ROLE_ADMIN".equals(user.getRoleCd())) return user.getMemberId();
        if (properties.isAdminApiOpen()) return user != null ? user.getMemberId() : "LOCAL_TEST";
        throw new NoticeException(NoticeErrorCode.ADMIN_ONLY);
    }
}
