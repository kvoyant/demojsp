package com.example.demojsp.common.identity.service;

import com.example.demojsp.common.identity.config.IdentityProperties;
import com.example.demojsp.common.identity.domain.AuthPurpose;
import com.example.demojsp.common.identity.domain.VerifiedIdentity;
import com.example.demojsp.common.identity.exception.IdentityErrorCode;
import com.example.demojsp.common.identity.exception.IdentityVerificationException;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Optional;

/**
 * 본인인증 결과의 세션 저장/조회 담당 ("인증 결과 CI 획득 및 Session 반영" 요구사항).
 *
 * <h3>업무 개발자 사용법</h3>
 * <pre>{@code
 *   // 회원가입/대출신청 등 업무 API 에서
 *   VerifiedIdentity idv = identitySessionManager.require(session, AuthPurpose.SIGN_UP);
 *   memberService.join(form, idv.ci(), idv.di(), idv.mobileNo());
 *   identitySessionManager.clear(session, AuthPurpose.SIGN_UP);   // 사용 후 즉시 제거 권장
 * }</pre>
 *
 * <h3>설계 포인트</h3>
 * <ul>
 *   <li>세션 키를 목적(AuthPurpose)별로 분리 → 다른 업무용 인증결과 재사용 차단</li>
 *   <li>결과 유효시간(result-ttl) 초과 시 미인증으로 간주</li>
 *   <li>세션 속성명은 상수로 숨기고 반드시 이 클래스를 통해서만 접근</li>
 *   <li>세션 키 접두어 COMMON_IDENTITY_VERIFIED. 는 기존 LOGIN_USER / 기존 NICE 세션키와 겹치지 않는다</li>
 * </ul>
 */
@Component
public class IdentitySessionManager {

    private static final String ATTR_PREFIX = "COMMON_IDENTITY_VERIFIED.";

    private final IdentityProperties properties;
    private final Clock clock;

    public IdentitySessionManager(IdentityProperties properties, @Qualifier("identityClock") Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public void save(HttpSession session, VerifiedIdentity identity) {
        session.setAttribute(key(identity.purpose()), identity);
    }

    /** 인증 결과 조회 (없거나 만료면 empty) */
    public Optional<VerifiedIdentity> find(HttpSession session, AuthPurpose purpose) {
        if (session == null) {
            return Optional.empty();
        }
        Object v = session.getAttribute(key(purpose));
        if (!(v instanceof VerifiedIdentity idv)) {
            return Optional.empty();
        }
        if (idv.verifiedAt().plus(properties.getResultTtl()).isBefore(clock.instant())) {
            session.removeAttribute(key(purpose)); // 만료된 결과는 정리
            return Optional.empty();
        }
        return Optional.of(idv);
    }

    /** 인증 결과 필수 조회 (없으면 IDV-301 예외 → 화면에서 본인인증 유도) */
    public VerifiedIdentity require(HttpSession session, AuthPurpose purpose) {
        return find(session, purpose)
                .orElseThrow(() -> new IdentityVerificationException(IdentityErrorCode.NOT_VERIFIED, purpose.name()));
    }

    public void clear(HttpSession session, AuthPurpose purpose) {
        if (session != null) {
            session.removeAttribute(key(purpose));
        }
    }

    private static String key(AuthPurpose purpose) {
        return ATTR_PREFIX + purpose.name();
    }
}
