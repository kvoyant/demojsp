package com.example.demojsp.common.identity.service;

import com.example.demojsp.common.identity.domain.AuthMethod;
import com.example.demojsp.common.identity.exception.IdentityErrorCode;
import com.example.demojsp.common.identity.exception.IdentityVerificationException;
import com.example.demojsp.common.identity.spi.IdentityProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 등록된 IdentityProvider 빈들을 AuthMethod 별로 모아두는 레지스트리.
 *
 * <p>스프링이 IdentityProvider 구현체 전부를 List 로 주입해 주므로,
 * 새 Provider 를 @Component 로 추가하기만 하면 자동으로 여기 등록된다. (if/switch 분기 불필요)
 */
@Component
public class IdentityProviderRegistry {

    private static final Logger log = LoggerFactory.getLogger(IdentityProviderRegistry.class);

    private final Map<AuthMethod, IdentityProvider> providers = new EnumMap<>(AuthMethod.class);

    public IdentityProviderRegistry(List<IdentityProvider> providerBeans) {
        for (IdentityProvider p : providerBeans) {
            IdentityProvider dup = providers.put(p.method(), p);
            if (dup != null) {
                // 같은 수단에 Provider 가 2개 → 설정 오류. 기동 시점에 바로 실패시킨다.
                throw new IllegalStateException("Duplicate IdentityProvider for " + p.method()
                        + ": " + dup.getClass().getSimpleName() + ", " + p.getClass().getSimpleName());
            }
            log.info("[Identity] provider registered: {} -> {}", p.method(), p.getClass().getSimpleName());
        }
    }

    public IdentityProvider get(AuthMethod method) {
        IdentityProvider p = providers.get(method);
        if (p == null) {
            throw new IdentityVerificationException(IdentityErrorCode.UNSUPPORTED_METHOD, String.valueOf(method));
        }
        return p;
    }

    /** 현재 사용 가능한 인증수단 (화면에서 인증수단 선택 버튼 노출용) */
    public Set<AuthMethod> availableMethods() {
        return Collections.unmodifiableSet(providers.keySet());
    }
}
