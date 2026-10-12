package com.example.demojsp.common.annotation;

import java.lang.annotation.*;

/**
 * =========================================================================================
 * [전자금융 접근제어 RBAC 권한 체크 어노테이션]
 * 컨트롤러 메소드 또는 클래스에 적용하여 특정 권한을 보유한 사용자만 접근할 수 있도록 통제합니다.
 * 예: @RequireRole({"ROLE_ADMIN"}), @RequireRole({"ROLE_COUNSELOR", "ROLE_ADMIN"})
 * =========================================================================================
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {
    String[] value() default {};
}
