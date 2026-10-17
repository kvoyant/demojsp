package com.example.demojsp.common.identity.service;

import com.example.demojsp.common.identity.config.IdentityProperties;
import com.example.demojsp.common.identity.domain.AuthMethod;
import com.example.demojsp.common.identity.domain.IdentityTx;
import com.example.demojsp.common.identity.domain.ProviderPrepareResult;
import com.example.demojsp.common.identity.domain.StartRequest;
import com.example.demojsp.common.identity.domain.StartResponse;
import com.example.demojsp.common.identity.domain.VerifiedIdentity;
import com.example.demojsp.common.identity.exception.IdentityErrorCode;
import com.example.demojsp.common.identity.exception.IdentityVerificationException;
import com.example.demojsp.common.identity.spi.IdentityProvider;
import com.example.demojsp.common.identity.store.IdentityTxStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 본인인증 공통 서비스 (Facade) - 인증수단과 무관한 "흐름"을 책임진다.
 *
 * <pre>
 *  start()                                     complete()
 *  ───────────────────────────────            ──────────────────────────────────────
 *  0. 필수값 검증 (method/channel/purpose)
 *  1. 필수약관 동의 검증                        1. Provider 로 correlationKey 추출
 *  2. returnUrl 화이트리스트 검증 (APP)          2. TxStore 에서 거래 1회성 조회(consume)
 *  3. 거래번호 생성                             3. 만료 / 세션 일치 검증
 *  4. Provider.prepare()                       4. Provider.complete() (무결성·복호화·결과코드)
 *  5. 거래(비밀값 포함) TxStore 저장             5. 세션에 결과 반영 + 세션ID 재발급
 *  6. 화면용 form 정보 반환                     6. 감사 로그
 * </pre>
 *
 * <p>컨트롤러는 HTTP 입출력만, Provider 는 인증기관 통신만 하고, 검증/상태관리는 전부 여기서 한다.
 */
@Service
public class IdentityVerificationService {

    private static final Logger log = LoggerFactory.getLogger(IdentityVerificationService.class);
    /** 감사(audit) 로그는 별도 logger 로 분리 → logback 에서 전용 파일로 적재 권장 */
    private static final Logger audit = LoggerFactory.getLogger("AUDIT.IDENTITY");

    private static final DateTimeFormatter TX_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final char[] ALNUM = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final IdentityProviderRegistry registry;
    private final IdentityTxStore txStore;
    private final IdentitySessionManager sessionManager;
    private final TermsService termsService;
    private final IdentityProperties properties;
    private final Clock clock;

    public IdentityVerificationService(IdentityProviderRegistry registry, IdentityTxStore txStore,
                                       IdentitySessionManager sessionManager, TermsService termsService,
                                       IdentityProperties properties, @Qualifier("identityClock") Clock clock) {
        this.registry = registry;
        this.txStore = txStore;
        this.sessionManager = sessionManager;
        this.termsService = termsService;
        this.properties = properties;
        this.clock = clock;
    }

    /** 콜백 처리 결과 (컨트롤러가 화면 분기에 사용) */
    public record Completion(VerifiedIdentity identity, IdentityTx tx) {
    }

    // =====================================================================
    // 인증 시작
    // =====================================================================
    public StartResponse start(StartRequest request, HttpSession session) {
        // 0) 필수값 검증 (demojsp 는 Bean Validation 미사용 → 직접 검증)
        validateRequest(request);
        IdentityProvider provider = registry.get(request.method());

        // 1) 필수 약관 동의 서버 재검증
        termsService.validateRequiredAgreed(request.method(), request.agreedTerms());

        // 2) APP 채널 returnUrl 검증 (팝업 채널은 returnUrl 을 쓰지 않으므로 무시)
        String returnUrl = request.channel().isPopup() ? null : validateReturnUrl(request.returnUrl());

        // 3) 거래번호 생성
        String txId = newTxId();

        // 4) Provider 준비 (NICE: 토큰발급 + 암호화)
        String callbackUrl = trimSlash(properties.getBaseUrl()) + properties.getCallbackPath()
                + "/" + request.method().name().toLowerCase();
        ProviderPrepareResult prepared = provider.prepare(txId, request, callbackUrl);

        // 5) 거래 저장 (비밀값은 여기에만 존재)
        Instant now = clock.instant();
        IdentityTx tx = new IdentityTx(txId, prepared.correlationKey(), request.method(), request.channel(),
                request.purpose(), session.getId(), returnUrl, prepared.providerState(),
                now, now.plus(properties.getTxTtl()));
        txStore.save(tx);

        // 6) 이전에 같은 목적으로 받아둔 인증결과는 무효화 (새로 인증 중이므로)
        sessionManager.clear(session, request.purpose());

        audit.info("START txId={} method={} channel={} purpose={}", txId, request.method(), request.channel(), request.purpose());
        return new StartResponse(txId, prepared.actionUrl(), prepared.httpMethod(), prepared.formParams(),
                request.channel().isPopup());
    }

    // =====================================================================
    // 인증 완료 (콜백)
    // =====================================================================
    public Completion complete(AuthMethod method, Map<String, String> params, HttpServletRequest httpRequest) {
        IdentityProvider provider = registry.get(method);

        // 1~2) 거래 조회 - consume 이므로 같은 콜백 재전송(새로고침/뒤로가기)은 TX_NOT_FOUND
        String correlationKey = provider.extractCorrelationKey(params);
        IdentityTx tx = txStore.consume(correlationKey)
                .orElseThrow(() -> new IdentityVerificationException(IdentityErrorCode.TX_NOT_FOUND,
                        "correlationKey=" + correlationKey));

        try {
            // 3-1) 만료 검증
            if (tx.isExpired(clock.instant())) {
                throw new IdentityVerificationException(IdentityErrorCode.TX_EXPIRED, "txId=" + tx.txId());
            }
            if (tx.method() != method) {
                throw new IdentityVerificationException(IdentityErrorCode.REQUEST_NO_MISMATCH, "method mismatch");
            }

            // 3-2) 세션 일치 검증: 인증을 시작한 브라우저 == 콜백을 받은 브라우저
            //      (다른 사람의 인증 결과 URL 을 가로채 자기 세션에 반영하는 공격 차단)
            HttpSession session = httpRequest.getSession(false);
            if (session == null || !session.getId().equals(tx.sessionId())) {
                throw new IdentityVerificationException(IdentityErrorCode.SESSION_MISMATCH, "txId=" + tx.txId());
            }

            // 4) Provider 검증/복호화
            VerifiedIdentity identity = provider.complete(tx, params);

            // 5) 세션 반영 + 세션 고정(Session Fixation) 공격 방지를 위한 세션ID 재발급
            sessionManager.save(session, identity);
            httpRequest.changeSessionId();

            audit.info("SUCCESS txId={} method={} purpose={} providerTxNo={}",
                    tx.txId(), tx.method(), tx.purpose(), identity.providerTxNo());
            return new Completion(identity, tx);

        } catch (IdentityVerificationException e) {
            audit.warn("FAIL txId={} code={} detail={}", tx.txId(), e.getErrorCode().getCode(), e.getDetail());
            throw new TxAwareException(e, tx);
        } catch (RuntimeException e) {
            log.error("[Identity] unexpected error txId={}", tx.txId(), e);
            throw new TxAwareException(new IdentityVerificationException(IdentityErrorCode.INTERNAL_ERROR, e.getMessage(), e), tx);
        }
    }

    /**
     * 실패 시에도 거래 정보(채널/returnUrl)를 컨트롤러에 넘겨 화면 분기(팝업 닫기 / APP 이동)를 할 수 있게 감싼 예외.
     */
    public static class TxAwareException extends IdentityVerificationException {
        private final transient IdentityTx tx;

        public TxAwareException(IdentityVerificationException cause, IdentityTx tx) {
            super(cause.getErrorCode(), cause.getDetail(), cause);
            this.tx = tx;
        }

        public IdentityTx getTx() {
            return tx;
        }
    }

    // =====================================================================
    // 내부 유틸
    // =====================================================================

    /** 필수값 검증. 누락 시 IDV-004 */
    void validateRequest(StartRequest request) {
        if (request == null || request.method() == null || request.channel() == null || request.purpose() == null) {
            throw new IdentityVerificationException(IdentityErrorCode.INVALID_REQUEST, "method/channel/purpose required");
        }
    }

    /**
     * 거래번호: "IDV" + yyyyMMddHHmmss + 랜덤 10자리 = 27자.
     * NICE requestno 최대 30byte 이내, 영숫자만 사용(혼동 문자 0/O, 1/I 제외).
     */
    String newTxId() {
        StringBuilder sb = new StringBuilder("IDV").append(LocalDateTime.now(clock).format(TX_TIME));
        for (int i = 0; i < 10; i++) {
            sb.append(ALNUM[RANDOM.nextInt(ALNUM.length)]);
        }
        return sb.toString();
    }

    /**
     * 오픈 리다이렉트 방지.
     * <ul>
     *   <li>허용 prefix 로 시작해야 함 (기본 "/" = 같은 도메인 상대경로)</li>
     *   <li>"//evil.com" (프로토콜 상대 URL), "/\evil.com" 같은 우회 패턴 차단</li>
     * </ul>
     */
    String validateReturnUrl(String returnUrl) {
        if (!StringUtils.hasText(returnUrl)) {
            return null;
        }
        if (returnUrl.startsWith("//") || returnUrl.startsWith("/\\") || returnUrl.contains("\r") || returnUrl.contains("\n")) {
            throw new IdentityVerificationException(IdentityErrorCode.INVALID_RETURN_URL, returnUrl);
        }
        boolean allowed = properties.getAllowedReturnUrlPrefixes().stream().anyMatch(returnUrl::startsWith);
        if (!allowed) {
            throw new IdentityVerificationException(IdentityErrorCode.INVALID_RETURN_URL, returnUrl);
        }
        return returnUrl;
    }

    private static String trimSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
