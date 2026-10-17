package com.example.demojsp.common.identity.provider.nice;

import com.example.demojsp.common.identity.config.IdentityProperties;
import com.example.demojsp.common.identity.exception.IdentityErrorCode;
import com.example.demojsp.common.identity.exception.IdentityVerificationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Base64;
import java.util.Map;

/**
 * NICE API 서버(svc.niceapi.co.kr) 호출 전담 클라이언트 (공통모듈 전용).
 *
 * <p>HTTP 통신만 담당하고 암호화/흐름 제어는 하지 않는다. (단일 책임)
 * <ol>
 *   <li>기관토큰(access_token) 발급 - OAuth client_credentials, 최초 1회 후 캐시</li>
 *   <li>암호화 토큰(token_val) 발급 - 인증 요청 건마다 호출</li>
 * </ol>
 *
 * <p>※ 기존 {@code domain.auth.client.NiceApiClient(Impl)} 와 이름이 겹치지 않도록 NiceIdApiClient 로 명명.
 * <p>※ 응답은 Map 으로 받는다. (Jackson 2/3 의 JsonNode API 차이에 영향받지 않도록)
 * <p>{@code common.identity.mock-enabled=true} 이면 빈이 생성되지 않는다.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "common.identity", name = "mock-enabled", havingValue = "false", matchIfMissing = true)
public class NiceIdApiClient {

    /** NICE 응답 성공 코드 (dataHeader.GW_RSLT_CD / dataBody.rsp_cd / dataBody.result_cd) */
    private static final String GW_SUCCESS = "1200";
    private static final String RSP_SUCCESS = "P000";
    private static final String RESULT_SUCCESS = "0000";

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE = new ParameterizedTypeReference<>() {
    };

    private final RestClient restClient;
    private final IdentityProperties.Nice props;
    private final Clock clock;

    /** 발급된 기관토큰 메모리 캐시 (volatile: 멀티스레드 가시성 보장) */
    private volatile String cachedAccessToken;

    public NiceIdApiClient(@Qualifier("identityNiceRestClient") RestClient restClient,
                           IdentityProperties properties,
                           @Qualifier("identityClock") Clock clock) {
        this.restClient = restClient;
        this.props = properties.getNice();
        this.clock = clock;
        this.cachedAccessToken = props.getAccessToken(); // 미리 발급받은 토큰이 설정에 있으면 그대로 사용
    }

    /** 암호화 토큰 발급 결과 */
    public record CryptoToken(String siteCode, String tokenVersionId, String tokenVal) {
        @Override
        public String toString() {
            return "CryptoToken[siteCode=" + siteCode + ", tokenVersionId=" + tokenVersionId + ", tokenVal=****]";
        }
    }

    /**
     * 암호화 토큰 발급.
     *
     * @param reqDtim 요청일시 (yyyyMMddHHmmss) - 키 생성에 그대로 다시 쓰이므로 호출측과 동일 값이어야 함
     * @param reqNo   요청고유번호 (= 공통모듈 거래번호, 최대 30byte)
     */
    public CryptoToken requestCryptoToken(String reqDtim, String reqNo) {
        Map<String, Object> body = Map.of(
                "dataHeader", Map.of("CNTY_CD", "ko"),
                "dataBody", Map.of(
                        "req_dtim", reqDtim,
                        "req_no", reqNo,
                        "enc_mode", "1"   // 1: AES128/CBC/PKCS7 (가이드 기준)
                ));

        Map<String, Object> res = post(props.getCryptoTokenPath(), cryptoAuthorization(), body);
        Map<String, Object> header = child(res, "dataHeader");
        Map<String, Object> dataBody = child(res, "dataBody");

        // 응답코드 3단 검증: 게이트웨이 → 응답 → 결과
        String gw = str(header, "GW_RSLT_CD");
        String rsp = str(dataBody, "rsp_cd");
        String result = str(dataBody, "result_cd");
        if (!GW_SUCCESS.equals(gw) || !RSP_SUCCESS.equals(rsp) || !RESULT_SUCCESS.equals(result)) {
            throw new IdentityVerificationException(IdentityErrorCode.PROVIDER_ERROR,
                    "crypto token failed gw=" + gw + ", rsp=" + rsp + ", result=" + result);
        }
        return new CryptoToken(str(dataBody, "site_code"), str(dataBody, "token_version_id"), str(dataBody, "token_val"));
    }

    /**
     * 기관토큰 조회 (없으면 발급).
     * <p>synchronized + double-check: 기동 직후 동시 요청이 몰려도 발급 API 는 1회만 호출되도록 한다.
     * <p>다중 서버 환경에서는 운영 전에 미리 발급한 토큰을 설정(access-token)으로 주입하는 것을 권장.
     */
    String getAccessToken() {
        String token = cachedAccessToken;
        if (StringUtils.hasText(token)) {
            return token;
        }
        synchronized (this) {
            if (!StringUtils.hasText(cachedAccessToken)) {
                cachedAccessToken = issueAccessToken();
                log.info("[공통본인인증][NICE] 기관토큰 발급 완료");
            }
            return cachedAccessToken;
        }
    }

    /** 기관토큰 발급: Authorization: Basic Base64(client_id:client_secret) */
    private String issueAccessToken() {
        String basic = Base64.getEncoder().encodeToString(
                (props.getClientId() + ":" + props.getClientSecret()).getBytes(StandardCharsets.UTF_8));
        try {
            Map<String, Object> res = restClient.post()
                    .uri(props.getTokenPath())
                    .header("Authorization", "Basic " + basic)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body("grant_type=client_credentials&scope=default")
                    .retrieve()
                    .body(MAP_TYPE);
            String token = str(child(res, "dataBody"), "access_token");
            if (!StringUtils.hasText(token)) {
                throw new IdentityVerificationException(IdentityErrorCode.PROVIDER_ERROR, "access token empty");
            }
            return token;
        } catch (RestClientException e) {
            throw new IdentityVerificationException(IdentityErrorCode.PROVIDER_ERROR, "access token request failed", e);
        }
    }

    /** 암호화 토큰 API 인증 헤더: {@code bearer Base64(access_token:현재타임스탬프(초):client_id)} */
    private String cryptoAuthorization() {
        long epochSec = clock.instant().getEpochSecond();
        String raw = getAccessToken() + ":" + epochSec + ":" + props.getClientId();
        return "bearer " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private Map<String, Object> post(String path, String authorization, Object body) {
        try {
            Map<String, Object> res = restClient.post()
                    .uri(path)
                    .header("Authorization", authorization)
                    .header("ProductID", props.getProductId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(MAP_TYPE);
            if (res == null) {
                throw new IdentityVerificationException(IdentityErrorCode.PROVIDER_ERROR, "empty response: " + path);
            }
            return res;
        } catch (RestClientException e) {
            // 타임아웃/네트워크 오류 등. 원인은 로그로, 화면에는 공통 메시지
            throw new IdentityVerificationException(IdentityErrorCode.PROVIDER_ERROR, "NICE api call failed: " + path, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> child(Map<String, Object> parent, String key) {
        Object v = parent == null ? null : parent.get(key);
        return v instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    private static String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v == null ? null : String.valueOf(v);
    }
}
