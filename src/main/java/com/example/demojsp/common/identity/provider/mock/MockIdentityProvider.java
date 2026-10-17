package com.example.demojsp.common.identity.provider.mock;

import com.example.demojsp.common.identity.domain.AuthMethod;
import com.example.demojsp.common.identity.domain.IdentityTx;
import com.example.demojsp.common.identity.domain.ProviderPrepareResult;
import com.example.demojsp.common.identity.domain.StartRequest;
import com.example.demojsp.common.identity.domain.VerifiedIdentity;
import com.example.demojsp.common.identity.exception.IdentityErrorCode;
import com.example.demojsp.common.identity.exception.IdentityVerificationException;
import com.example.demojsp.common.identity.spi.IdentityProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * 로컬/개발용 가짜 NICE Provider.
 *
 * <p>{@code common.identity.mock-enabled=true} 일 때만 활성화되며, 이때 실제 {@code NiceIdProvider} 는 비활성화된다.
 * NICE 계약/방화벽 오픈 전에도 업무 화면 개발자가 "본인인증 완료 이후" 로직을 바로 개발할 수 있게 해준다.
 *
 * <p>동작: 인증창 대신 공통 콜백 URL 로 바로 이동시키고, 콜백에서 고정된 가짜 사용자를 반환한다.
 * 콜백에 {@code mock_result=fail} 을 붙이면 실패 시나리오도 테스트할 수 있다.
 *
 * <p>※ 기존 NICE 구현의 {@code nice.api.mock-mode} 와는 별개 스위치({@code common.identity.mock-enabled})다.
 * <p>※ 운영 배포 시 mock-enabled 가 true 로 남지 않도록 profile 별 yaml 분리 필수.
 */
@Component
@ConditionalOnProperty(prefix = "common.identity", name = "mock-enabled", havingValue = "true")
public class MockIdentityProvider implements IdentityProvider {

    static final String P_MOCK_TX = "mock_tx";
    static final String P_MOCK_RESULT = "mock_result";

    private final Clock clock;

    public MockIdentityProvider(@Qualifier("identityClock") Clock clock) {
        this.clock = clock;
    }

    @Override
    public AuthMethod method() {
        return AuthMethod.NICE_MOBILE;
    }

    @Override
    public ProviderPrepareResult prepare(String txId, StartRequest request, String callbackUrl) {
        String correlationKey = "MOCK-" + UUID.randomUUID();
        // 인증창 대신 콜백 URL 로 바로 GET 이동
        return new ProviderPrepareResult(correlationKey, callbackUrl, "GET",
                Map.of(P_MOCK_TX, correlationKey), Map.of());
    }

    @Override
    public String extractCorrelationKey(Map<String, String> callbackParams) {
        return callbackParams.get(P_MOCK_TX);
    }

    @Override
    public VerifiedIdentity complete(IdentityTx tx, Map<String, String> params) {
        if ("fail".equals(params.get(P_MOCK_RESULT))) {
            throw new IdentityVerificationException(IdentityErrorCode.AUTH_FAILED, "mock fail");
        }
        String mobileNo = "01012345678";
        return new VerifiedIdentity(
                AuthMethod.NICE_MOBILE, tx.purpose(), tx.channel(),
                "홍길동", "19900101", "M", false, "1", mobileNo,
                fakeCi(mobileNo), fakeDi(mobileNo),
                "MOCK" + tx.txId(), clock.instant());
    }

    /** 실제 CI(88자 Base64)와 비슷한 형태의 고정값 - 동일 사용자면 항상 같은 값이 나오도록 */
    private String fakeCi(String seed) {
        byte[] h = sha256("CI" + seed);
        byte[] buf = new byte[66];
        for (int i = 0; i < buf.length; i++) buf[i] = h[i % h.length];
        return Base64.getEncoder().encodeToString(buf); // 88자
    }

    private String fakeDi(String seed) {
        return HexFormat.of().formatHex(sha256("DI" + seed)).toUpperCase(); // 64자
    }

    private static byte[] sha256(String s) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
