package com.example.demojsp.common.consent.service;

import com.example.demojsp.domain.login.dto.UserSessionDto;
import com.example.demojsp.domain.login.service.LoginServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * 동의한 사람 식별값.
 * <ul>
 *   <li>custId  : 로그인 회원 ID (비로그인 null)</li>
 *   <li>sessKey : 세션ID 의 SHA-256 - 비로그인(회원가입 등)도 "같은 사람이 동의했는지" 확인용. 세션ID 원문은 DB 에 남기지 않는다.</li>
 * </ul>
 */
public record ConsentUser(String custId, String sessKey, String clientIp) {

    public static ConsentUser from(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        Object v = session.getAttribute(LoginServiceImpl.SESSION_USER_KEY);
        String custId = v instanceof UserSessionDto u ? u.getMemberId() : null;
        return new ConsentUser(custId, ConsentServiceImpl.sha256(session.getId()), clientIp(request));
    }

    private static String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");          // 프록시(L4/WAF) 뒤라면 첫 번째 값
        String ip = xff != null && !xff.isBlank() ? xff.split(",")[0].strip() : request.getRemoteAddr();
        return ip != null && ip.length() > 45 ? ip.substring(0, 45) : ip;
    }
}
