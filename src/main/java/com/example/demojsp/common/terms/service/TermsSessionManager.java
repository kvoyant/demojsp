package com.example.demojsp.common.terms.service;

import com.example.demojsp.common.terms.config.TermsProperties;
import com.example.demojsp.common.terms.domain.AgreeResult;
import com.example.demojsp.common.terms.exception.TermsErrorCode;
import com.example.demojsp.common.terms.exception.TermsException;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Optional;

/**
 * 약관 동의 결과의 세션 저장/조회 - 업무코드(bizCode)별로 분리 저장한다.
 *
 * <h3>업무 개발자 사용법</h3>
 * <pre>{@code
 *   AgreeResult terms = termsSessionManager.require(session, "SIGN_UP");   // 미동의/만료 → TRM-301(401)
 *   member.setTermsAgreementId(terms.agreementId());                        // 업무 데이터와 동의이력 연결
 *   member.setMarketingYn(terms.isAgreed("MARKETING_USE") ? "Y" : "N");     // 선택약관 반영
 *   termsSessionManager.clear(session, "SIGN_UP");                          // 1회 사용 후 제거
 * }</pre>
 *
 * <p>세션 키 접두어 COMMON_TERMS_AGREED. 는 LOGIN_USER / COMMON_IDENTITY_VERIFIED. 와 겹치지 않는다.
 */
@Component
public class TermsSessionManager {

    private static final String ATTR_PREFIX = "COMMON_TERMS_AGREED.";

    private final TermsProperties properties;
    private final Clock clock;

    @Autowired
    public TermsSessionManager(TermsProperties properties) {
        this(properties, Clock.systemDefaultZone());
    }

    /** 테스트용 (만료 검증 시 Clock 고정) */
    public TermsSessionManager(TermsProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public void save(HttpSession session, AgreeResult result) {
        session.setAttribute(key(result.bizCode()), result);
    }

    /** 동의 결과 조회 (없거나 만료면 empty) */
    public Optional<AgreeResult> find(HttpSession session, String bizCode) {
        if (session == null || bizCode == null) {
            return Optional.empty();
        }
        Object v = session.getAttribute(key(bizCode));
        if (!(v instanceof AgreeResult result)) {
            return Optional.empty();
        }
        if (result.agreedInstant().plus(properties.getAgreementTtl()).isBefore(clock.instant())) {
            session.removeAttribute(key(bizCode));
            return Optional.empty();
        }
        return Optional.of(result);
    }

    /** 동의 결과 필수 조회 (없으면 TRM-301 → 화면에서 약관동의 유도) */
    public AgreeResult require(HttpSession session, String bizCode) {
        return find(session, bizCode).orElseThrow(() -> new TermsException(TermsErrorCode.NOT_AGREED, bizCode));
    }

    public void clear(HttpSession session, String bizCode) {
        if (session != null && bizCode != null) {
            session.removeAttribute(key(bizCode));
        }
    }

    private static String key(String bizCode) {
        return ATTR_PREFIX + bizCode;
    }
}
