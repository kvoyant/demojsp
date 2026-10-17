package com.example.demojsp.common.identity.spi;

import com.example.demojsp.common.identity.domain.AuthMethod;
import com.example.demojsp.common.identity.domain.IdentityTx;
import com.example.demojsp.common.identity.domain.ProviderPrepareResult;
import com.example.demojsp.common.identity.domain.StartRequest;
import com.example.demojsp.common.identity.domain.VerifiedIdentity;

import java.util.Map;

/**
 * 인증기관 연동 SPI (Service Provider Interface) - <b>공통모듈의 핵심 확장 포인트</b>.
 *
 * <p>공통 서비스({@link com.example.demojsp.common.identity.service.IdentityVerificationService})는
 * 인증 흐름(거래 생성 → 저장 → 콜백 검증 → 세션 반영)만 담당하고,
 * "인증기관과 어떻게 통신하는가"는 전부 이 인터페이스 구현체에 위임한다. (전략 패턴)
 *
 * <pre>
 *  [화면] ─start─▶ [Controller] ─▶ [IdentityVerificationService] ─prepare()─▶ [NiceIdProvider]
 *                                          │                                   [MockIdentityProvider]
 *  [인증기관] ─callback─▶ [Controller] ─▶  └──────────────complete()──────────▶ [PrivateCertProvider] ...
 * </pre>
 *
 * <h3>구현 규칙</h3>
 * <ul>
 *   <li>@Component 로 등록하면 {@link com.example.demojsp.common.identity.service.IdentityProviderRegistry} 가 자동 수집한다.</li>
 *   <li>같은 AuthMethod 를 지원하는 빈은 동시에 1개만 활성화되어야 한다. (예: 로컬은 Mock, 운영은 NICE → @ConditionalOnProperty)</li>
 *   <li>암호키 등 콜백 처리에 필요한 비밀값은 {@link ProviderPrepareResult#providerState()} 에 담는다.
 *       서버(TxStore)에만 보관되고 화면으로 나가지 않는다.</li>
 *   <li>인증기관 고유 코드(성별, 통신사 등)는 complete() 안에서 공통 표준값으로 변환해서 반환한다.</li>
 * </ul>
 */
public interface IdentityProvider {

    /** 이 Provider 가 처리하는 인증수단 */
    AuthMethod method();

    /**
     * 인증 시작 준비. 인증기관 토큰 발급, 요청 데이터 암호화 등을 수행하고
     * 화면이 submit 할 form 정보를 만든다.
     *
     * @param txId        공통모듈이 발급한 거래번호 (인증기관 요청번호로 사용)
     * @param request     화면 요청
     * @param callbackUrl 인증기관이 결과를 돌려줄 공통 콜백 URL (절대경로)
     */
    ProviderPrepareResult prepare(String txId, StartRequest request, String callbackUrl);

    /**
     * 콜백 파라미터에서 거래 조회 키를 꺼낸다. (복호화 전 단계)
     *
     * @return correlationKey, 없으면 null
     */
    String extractCorrelationKey(Map<String, String> callbackParams);

    /**
     * 콜백 결과를 검증/복호화하여 공통 표준 결과로 변환한다.
     * 구현체는 반드시 다음을 수행해야 한다.
     * <ol>
     *   <li>무결성 검증 (HMAC 등) - 실패 시 INTEGRITY_FAILED</li>
     *   <li>인증기관 결과코드 확인 - 실패/취소 시 AUTH_FAILED / AUTH_CANCELED</li>
     *   <li>응답의 요청번호 == tx.txId() 검증 - 불일치 시 REQUEST_NO_MISMATCH</li>
     * </ol>
     */
    VerifiedIdentity complete(IdentityTx tx, Map<String, String> callbackParams);
}
