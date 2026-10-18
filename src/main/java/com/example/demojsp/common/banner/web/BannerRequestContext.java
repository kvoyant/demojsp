package com.example.demojsp.common.banner.web;

import com.example.demojsp.common.banner.config.BannerPopupProperties;
import com.example.demojsp.common.banner.exception.BannerErrorCode;
import com.example.demojsp.common.banner.exception.BannerException;
import com.example.demojsp.domain.login.dto.UserSessionDto;
import com.example.demojsp.domain.login.service.LoginServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

/**
 * 배너팝업 ADMIN API 권한 확인. 프로젝트 로그인 세션(LOGIN_USER)과 연결되는 유일한 지점이다.
 */
@Component
public class BannerRequestContext {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final BannerPopupProperties properties;

    public BannerRequestContext(BannerPopupProperties properties) {
        this.properties = properties;
    }

    /**
     * ROLE_ADMIN 로그인 세션 또는 {@code common.banner-popup.admin-api-open=true}(local) 이면 통과.
     *
     * @return 처리자 ID (감사 로그용)
     */
    public String requireAdmin(HttpServletRequest request) {
        UserSessionDto user = loginUser(request);
        if (user != null && ROLE_ADMIN.equals(user.getRoleCd())) {
            return user.getMemberId();
        }
        if (properties.isAdminApiOpen()) {
            return user != null ? user.getMemberId() : "LOCAL_TEST";
        }
        throw new BannerException(BannerErrorCode.ADMIN_ONLY);
    }

    private static UserSessionDto loginUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object v = session.getAttribute(LoginServiceImpl.SESSION_USER_KEY);
        return v instanceof UserSessionDto u ? u : null;
    }
}
