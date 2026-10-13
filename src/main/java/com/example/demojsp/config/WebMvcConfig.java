package com.example.demojsp.config;

import com.example.demojsp.common.interceptor.LoginCheckInterceptor;
import com.example.demojsp.common.interceptor.RoleCheckInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * =========================================================================================
 * [웹 MVC 및 보안 인터셉터 설정 (WebMvcConfig)]
 * - LoginCheckInterceptor: 로그인 필수 URL 경로 통제
 * - RoleCheckInterceptor: @RequireRole 어노테이션 기반 세부 RBAC 권한 통제
 * =========================================================================================
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginCheckInterceptor loginCheckInterceptor;
    private final RoleCheckInterceptor roleCheckInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. 로그인 인증 인터셉터
        registry.addInterceptor(loginCheckInterceptor)
                .addPathPatterns(
                        "/board/**",
                        "/common/auth/ci-migration/**",
                        "/admin/**",
                        "/api/auth/session/**",
                        "/api/auth/unlock/**"
                )
                .excludePathPatterns(
                        "/login",
                        "/logout",
                        "/api/auth/login/**",
                        "/api/auth/logout",
                        "/common/auth/nice/**",
                        "/api/auth/nice/**",
                        "/common/auth/account-cert/**",
                        "/api/common/auth/account-cert/**",
                        "/static/**",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/favicon.ico",
                        "/error"
                );

        // 2. 권한(RBAC) 인터셉터
        registry.addInterceptor(roleCheckInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login",
                        "/logout",
                        "/api/auth/**",
                        "/common/auth/nice/**",
                        "/api/auth/nice/**",
                        "/common/auth/account-cert/**",
                        "/api/common/auth/account-cert/**",
                        "/static/**",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/favicon.ico",
                        "/error"
                );
    }
}
