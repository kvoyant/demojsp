package com.example.demojsp.common.product.web;

import com.example.demojsp.common.product.config.ProductProperties;
import com.example.demojsp.common.product.exception.ProductErrorCode;
import com.example.demojsp.common.product.exception.ProductException;
import com.example.demojsp.domain.login.dto.UserSessionDto;
import com.example.demojsp.domain.login.service.LoginServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

/** 상품 ADMIN API 권한 확인 (ROLE_ADMIN 세션 또는 local admin-api-open) */
@Component
public class ProductRequestContext {

    private final ProductProperties properties;

    public ProductRequestContext(ProductProperties properties) {
        this.properties = properties;
    }

    public String requireAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object v = session == null ? null : session.getAttribute(LoginServiceImpl.SESSION_USER_KEY);
        UserSessionDto user = v instanceof UserSessionDto u ? u : null;
        if (user != null && "ROLE_ADMIN".equals(user.getRoleCd())) return user.getMemberId();
        if (properties.isAdminApiOpen()) return user != null ? user.getMemberId() : "LOCAL_TEST";
        throw new ProductException(ProductErrorCode.ADMIN_ONLY);
    }
}
