package com.example.demojsp.common.member;

import com.example.demojsp.common.banner.spi.AccountHoldingChecker;
import com.example.demojsp.domain.login.dto.UserSessionDto;
import com.example.demojsp.domain.login.service.LoginServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 요청 → 회원구분 (비로그인 / 일반회원 / 금융회원).
 *
 * <p>로그인 세션(LOGIN_USER) 고객 우선. 비로그인이고 {@code common.member.demo-customer-param=true}(local)일 때만
 * {@code demoCustomer} 파라미터를 고객으로 본다.
 */
@Component
@EnableConfigurationProperties(MemberProperties.class)
public class MemberSegmentResolver {

    private static final Pattern DEMO_ID = Pattern.compile("^[A-Za-z0-9_]{1,30}$");

    private final MemberProperties properties;
    private final AccountHoldingChecker accountHoldingChecker;

    public MemberSegmentResolver(MemberProperties properties, AccountHoldingChecker accountHoldingChecker) {
        this.properties = properties;
        this.accountHoldingChecker = accountHoldingChecker;
    }

    public MemberContext resolve(HttpServletRequest request) {
        return resolve(customerId(request));
    }

    public MemberContext resolve(String customerId) {
        if (customerId == null) {
            return new MemberContext(null, MemberSegment.GUEST);
        }
        return new MemberContext(customerId,
                accountHoldingChecker.hasAccount(customerId) ? MemberSegment.FINANCIAL : MemberSegment.MEMBER);
    }

    private String customerId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object v = session == null ? null : session.getAttribute(LoginServiceImpl.SESSION_USER_KEY);
        if (v instanceof UserSessionDto u && u.getMemberId() != null) {
            return u.getMemberId();
        }
        if (properties.isDemoCustomerParam()) {
            String demo = request.getParameter("demoCustomer");
            if (demo != null && DEMO_ID.matcher(demo).matches()) {
                return demo;
            }
        }
        return null;
    }
}
