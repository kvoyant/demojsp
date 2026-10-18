package com.example.demojsp.common.terms.web;

import com.example.demojsp.common.terms.config.TermsProperties;
import com.example.demojsp.common.terms.exception.TermsErrorCode;
import com.example.demojsp.common.terms.exception.TermsException;
import com.example.demojsp.domain.login.dto.UserSessionDto;
import com.example.demojsp.domain.login.service.LoginServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

/**
 * 요청에서 동의 주체/IP 를 꺼내고, ADMIN API 권한을 확인한다.
 *
 * <p>프로젝트 로그인 세션(LOGIN_USER)과 연결되는 유일한 지점이다. 로그인 체계가 바뀌면 이 클래스만 고친다.
 */
@Component
public class TermsRequestContext {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final TermsProperties properties;

    public TermsRequestContext(TermsProperties properties) {
        this.properties = properties;
    }

    /** 로그인 회원ID (비로그인이면 null → 이력에 GUEST 로 기록) */
    public String subjectId(HttpServletRequest request) {
        UserSessionDto user = loginUser(request);
        return user == null ? null : user.getMemberId();
    }

    /**
     * 접속 IP. L4/프록시 뒤라면 X-Forwarded-For 첫 번째 값을 쓰되,
     * <b>운영에서는 신뢰하는 프록시가 헤더를 덮어쓰는지 반드시 확인</b>할 것 (위조 가능 헤더).
     */
    public String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String first = xff.split(",")[0].strip();
            if (first.length() <= 45) {
                return first;
            }
        }
        return request.getRemoteAddr();
    }

    /**
     * ADMIN API 권한 확인. {@code common.terms.admin-api-open=true}(local) 이면 통과,
     * 아니면 ROLE_ADMIN 로그인 세션 필요.
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
        throw new TermsException(TermsErrorCode.ADMIN_ONLY, user == null ? "not logged in" : user.getRoleCd());
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
