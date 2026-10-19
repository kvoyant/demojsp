package com.example.demojsp.common.acctverify.controller;

import com.example.demojsp.common.acctverify.config.AcctVerifyProperties;
import com.example.demojsp.common.acctverify.exception.AcctVerifyErrorCode;
import com.example.demojsp.common.acctverify.exception.AcctVerifyException;
import com.example.demojsp.domain.login.dto.UserSessionDto;
import com.example.demojsp.domain.login.service.LoginServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

/** 1원 계좌인증 ADMIN API 권한 확인 → 관리자 ID (ROLE_ADMIN 세션, 또는 local 에서 common.acct-verify.admin-api-open=true) */
@Component
public class AcctVerifyAdminAuth {

    private final AcctVerifyProperties properties;

    public AcctVerifyAdminAuth(AcctVerifyProperties properties) {
        this.properties = properties;
    }

    public String requireAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object v = session == null ? null : session.getAttribute(LoginServiceImpl.SESSION_USER_KEY);
        UserSessionDto user = v instanceof UserSessionDto u ? u : null;
        if (user != null && "ROLE_ADMIN".equals(user.getRoleCd())) return user.getMemberId();
        if (properties.isAdminApiOpen()) return user != null ? user.getMemberId() : "LOCAL_TEST";
        throw new AcctVerifyException(AcctVerifyErrorCode.ADMIN_ONLY);
    }
}
