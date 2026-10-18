package com.example.demojsp.common.resume.web;

import com.example.demojsp.common.resume.config.ResumeProperties;
import com.example.demojsp.common.resume.exception.ResumeErrorCode;
import com.example.demojsp.common.resume.exception.ResumeException;
import com.example.demojsp.domain.login.dto.UserSessionDto;
import com.example.demojsp.domain.login.service.LoginServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 이어하기 API 고객 식별. 프로젝트 로그인 세션(LOGIN_USER)과 연결되는 유일한 지점이다.
 *
 * <p>운영: 로그인 세션의 회원 ID 만 사용 (요청 파라미터로 다른 고객 조회 불가).
 * <br>local({@code common.resume.demo-api-open=true}): 로그인하지 않았을 때만 {@code demoCustomer} 파라미터 → 기본 고객 순으로 사용.
 */
@Component
public class ResumeRequestContext {

    static final String DEMO_PARAM = "demoCustomer";
    private static final Pattern DEMO_ID = Pattern.compile("^[A-Za-z0-9_]{1,30}$");

    private final ResumeProperties properties;

    public ResumeRequestContext(ResumeProperties properties) {
        this.properties = properties;
    }

    /** 고객 ID. 식별 불가하면 RSM-401 */
    public String customerId(HttpServletRequest request) {
        UserSessionDto user = loginUser(request);
        if (user != null && user.getMemberId() != null) {
            return user.getMemberId();
        }
        if (properties.isDemoApiOpen()) {
            String demo = request.getParameter(DEMO_PARAM);
            if (demo != null && DEMO_ID.matcher(demo).matches()) {
                return demo;
            }
            return properties.getDemoDefaultCustomer();
        }
        throw new ResumeException(ResumeErrorCode.LOGIN_REQUIRED);
    }

    /** 테스트 API 허용 여부 (local 전용) */
    public void requireDemo() {
        if (!properties.isDemoApiOpen()) {
            throw new ResumeException(ResumeErrorCode.DEMO_ONLY);
        }
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
