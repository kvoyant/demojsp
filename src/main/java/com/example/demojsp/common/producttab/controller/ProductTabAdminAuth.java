package com.example.demojsp.common.producttab.controller;

import com.example.demojsp.common.producttab.config.ProductTabProperties;
import com.example.demojsp.common.producttab.exception.ProductTabErrorCode;
import com.example.demojsp.common.producttab.exception.ProductTabException;
import com.example.demojsp.domain.login.dto.UserSessionDto;
import com.example.demojsp.domain.login.service.LoginServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

/** 상품 탭 ADMIN API 권한 확인 → 관리자 ID (ROLE_ADMIN 세션, 또는 local 에서 common.product-tab.admin-api-open=true) */
@Component
public class ProductTabAdminAuth {

    private final ProductTabProperties properties;

    public ProductTabAdminAuth(ProductTabProperties properties) {
        this.properties = properties;
    }

    public String requireAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object v = session == null ? null : session.getAttribute(LoginServiceImpl.SESSION_USER_KEY);
        UserSessionDto user = v instanceof UserSessionDto u ? u : null;
        if (user != null && "ROLE_ADMIN".equals(user.getRoleCd())) return user.getMemberId();
        if (properties.isAdminApiOpen()) return user != null ? user.getMemberId() : "LOCAL_TEST";
        throw new ProductTabException(ProductTabErrorCode.ADMIN_ONLY);
    }
}
