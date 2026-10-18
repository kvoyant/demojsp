package com.example.demojsp.common.member;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 회원구분 판단 설정 ({@code common.member.*}).
 */
@ConfigurationProperties(prefix = "common.member")
public class MemberProperties {

    /**
     * 비로그인 시 {@code demoCustomer} 파라미터로 고객 흉내 허용 (<b>local 전용</b>, 운영 false).
     * 로그인 세션이 있으면 항상 세션 고객을 쓴다.
     */
    private boolean demoCustomerParam = false;

    public boolean isDemoCustomerParam() { return demoCustomerParam; }
    public void setDemoCustomerParam(boolean demoCustomerParam) { this.demoCustomerParam = demoCustomerParam; }
}
