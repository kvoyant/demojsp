package com.example.demojsp.common.identity.provider.privatecert;

import com.example.demojsp.common.identity.domain.AuthMethod;
import com.example.demojsp.common.identity.domain.IdentityTx;
import com.example.demojsp.common.identity.domain.ProviderPrepareResult;
import com.example.demojsp.common.identity.domain.StartRequest;
import com.example.demojsp.common.identity.domain.VerifiedIdentity;
import com.example.demojsp.common.identity.exception.IdentityErrorCode;
import com.example.demojsp.common.identity.exception.IdentityVerificationException;
import com.example.demojsp.common.identity.spi.IdentityProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * [확장 예시] 사설인증(통신사 PASS·민간 인증서) Provider 골격.
 *
 * <p>요구사항의 "사설인증(통신사·민간)", "공동인증서" 를 추가할 때 이 클래스를 템플릿으로 사용한다.
 * 구현해야 할 것은 3개 메서드뿐이며, 거래관리/세션반영/약관/화면연동은 공통모듈이 그대로 처리한다.
 *
 * <p>기본 비활성화: {@code common.identity.private-cert.enabled=true} 일 때만 빈 등록.
 */
@Component
@ConditionalOnProperty(prefix = "common.identity.private-cert", name = "enabled", havingValue = "true")
public class PrivateCertProvider implements IdentityProvider {

    @Override
    public AuthMethod method() {
        return AuthMethod.PRIVATE_CERT;
    }

    @Override
    public ProviderPrepareResult prepare(String txId, StartRequest request, String callbackUrl) {
        // TODO 1. 중계사(예: 인증 중계 서비스) 인증요청 API 호출 → 인증 URL / 거래키 수신
        // TODO 2. request.options() 로 세부 인증기관(카카오/네이버/토스/PASS 등) 선택값 전달
        // TODO 3. 콜백 검증용 비밀값은 providerState 에 저장
        throw new IdentityVerificationException(IdentityErrorCode.UNSUPPORTED_METHOD, "PRIVATE_CERT not implemented yet");
    }

    @Override
    public String extractCorrelationKey(Map<String, String> callbackParams) {
        // TODO 중계사 콜백 파라미터 중 거래키 반환
        return null;
    }

    @Override
    public VerifiedIdentity complete(IdentityTx tx, Map<String, String> callbackParams) {
        // TODO 1. 서명/무결성 검증  2. 결과 조회 API 호출  3. CI 등 VerifiedIdentity 로 변환
        throw new IdentityVerificationException(IdentityErrorCode.UNSUPPORTED_METHOD, "PRIVATE_CERT not implemented yet");
    }
}
