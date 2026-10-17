package com.example.demojsp.common.identity.provider.nice;

import com.example.demojsp.common.identity.domain.AuthMethod;
import com.example.demojsp.common.identity.domain.IdentityTx;
import com.example.demojsp.common.identity.domain.ProviderPrepareResult;
import com.example.demojsp.common.identity.domain.StartRequest;
import com.example.demojsp.common.identity.domain.VerifiedIdentity;
import com.example.demojsp.common.identity.config.IdentityProperties;
import com.example.demojsp.common.identity.exception.IdentityErrorCode;
import com.example.demojsp.common.identity.exception.IdentityVerificationException;
import com.example.demojsp.common.identity.spi.IdentityProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * NICE ID 휴대폰 본인인증 Provider (CF-BC-03 / REQ-AP-601).
 *
 * <h3>전체 흐름</h3>
 * <pre>
 *  ① prepare()
 *     ├─ NICE 암호화토큰 발급 (req_dtim, req_no=txId)  → site_code, token_version_id, token_val
 *     ├─ 대칭키 생성 (req_dtim + req_no + token_val)
 *     ├─ 요청 JSON 생성 → AES 암호화(enc_data) → HMAC(integrity_value)
 *     └─ 화면에 form 정보 반환 (m, token_version_id, enc_data, integrity_value)
 *        ※ 대칭키는 providerState 로 서버(TxStore)에만 보관
 *
 *  ② 사용자: NICE 표준창에서 성명+생년월일+휴대폰번호 입력 (통신사 미지정 / MO인증)
 *
 *  ③ complete()  (NICE → 공통 콜백 /common/identity/callback/nice_mobile)
 *     ├─ integrity_value 검증 (HMAC) - 복호화보다 먼저
 *     ├─ enc_data 복호화 → 결과 JSON
 *     ├─ resultcode == 0000 확인
 *     ├─ requestno == txId 확인 (거래 바꿔치기 방지)
 *     └─ 공통 표준 모델(VerifiedIdentity)로 변환 (성별/내외국인 코드 변환 포함)
 * </pre>
 *
 * <p><b>기존 NICE ID 구현(domain.auth)과의 관계</b>: 기존 구현은 OTP 발송/검증을 업무 서비스(NiceCertServiceImpl)가
 * 직접 처리하는 구조이고, 이 Provider 는 NICE 표준창 방식으로 "NICE 와 대화하는 부분"만 담당한다.
 * 거래관리/세션/약관/화면연동은 공통 서비스가 처리한다. 두 구현은 서로 참조하지 않는다.
 *
 * <p>JSON 처리는 Spring Boot 4 기본인 Jackson 3 (tools.jackson) ObjectMapper 를 사용한다.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "common.identity", name = "mock-enabled", havingValue = "false", matchIfMissing = true)
public class NiceIdProvider implements IdentityProvider {

    private static final DateTimeFormatter REQ_DTIM = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** providerState 키 (TxStore 에 저장되는 비밀값) */
    static final String ST_KEY = "nice.key";
    static final String ST_IV = "nice.iv";
    static final String ST_HMAC = "nice.hmac";
    static final String ST_TOKEN_VERSION_ID = "nice.tokenVersionId";
    static final String ST_SITE_CODE = "nice.siteCode";

    /** 콜백 파라미터명 (NICE → 우리) */
    static final String P_TOKEN_VERSION_ID = "token_version_id";
    static final String P_ENC_DATA = "enc_data";
    static final String P_INTEGRITY = "integrity_value";

    private static final String RESULT_SUCCESS = "0000";

    private final NiceIdApiClient apiClient;
    private final IdentityProperties.Nice props;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public NiceIdProvider(NiceIdApiClient apiClient, IdentityProperties properties,
                          ObjectMapper objectMapper, @Qualifier("identityClock") Clock clock) {
        this.apiClient = apiClient;
        this.props = properties.getNice();
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public AuthMethod method() {
        return AuthMethod.NICE_MOBILE;
    }

    // =====================================================================
    // ① 인증 시작 준비
    // =====================================================================
    @Override
    public ProviderPrepareResult prepare(String txId, StartRequest request, String callbackUrl) {
        // 1) 암호화 토큰 발급 - req_dtim 은 키 생성에도 동일하게 써야 하므로 변수로 고정
        String reqDtim = LocalDateTime.now(clock).format(REQ_DTIM);
        NiceIdApiClient.CryptoToken token = apiClient.requestCryptoToken(reqDtim, txId);

        // 2) 대칭키 생성
        NiceIdCrypto.Keys keys = NiceIdCrypto.deriveKeys(reqDtim, txId, token.tokenVal());

        // 3) 요청 데이터 생성 → 암호화 → 무결성값
        String requestJson = buildRequestJson(txId, token.siteCode(), callbackUrl, request);
        String encData = NiceIdCrypto.encrypt(keys, requestJson);
        String integrity = NiceIdCrypto.hmac(keys, encData);

        // 4) 화면이 NICE 표준창으로 submit 할 form 파라미터
        Map<String, String> form = new LinkedHashMap<>();
        form.put("m", "service");                       // 고정값 (가이드 기준)
        form.put(P_TOKEN_VERSION_ID, token.tokenVersionId());
        form.put(P_ENC_DATA, encData);
        form.put(P_INTEGRITY, integrity);

        // 5) 콜백 복호화에 필요한 비밀값 (서버에만 저장, 화면으로 나가지 않음)
        Map<String, String> state = Map.of(
                ST_KEY, keys.key(),
                ST_IV, keys.iv(),
                ST_HMAC, keys.hmacKey(),
                ST_TOKEN_VERSION_ID, token.tokenVersionId(),
                ST_SITE_CODE, token.siteCode());

        log.info("[공통본인인증][NICE] 인증 준비 완료 txId={}, tokenVersionId={}", txId, token.tokenVersionId());
        // NICE 표준창은 POST submit 이 기본
        return new ProviderPrepareResult(token.tokenVersionId(), props.getCheckplusUrl(), "POST", form, state);
    }

    /**
     * NICE 요청 JSON 생성.
     *
     * <ul>
     *   <li>requestno  : 거래번호 (콜백에서 동일한지 재검증)</li>
     *   <li>returnurl  : 결과 수신 URL (공통 콜백)</li>
     *   <li>sitecode   : 암호화토큰 발급 시 받은 사이트코드</li>
     *   <li>authtype   : M (휴대폰)</li>
     *   <li>methodtype : get/post - 기본 get (SameSite 쿠키 이슈, IdentityProperties 주석 참고)</li>
     *   <li>popupyn    : 팝업 채널이면 Y, APP(WebView 페이지 전환)이면 N</li>
     *   <li>receivedata: 콜백 때 그대로 돌려받는 값. 목적/채널을 넣어 추적용으로 사용(필수 아님)</li>
     * </ul>
     * 통신사 미지정/MO인증 관련 옵션은 {@code extra-request-params} 설정으로 merge 한다.
     */
    private String buildRequestJson(String txId, String siteCode, String callbackUrl, StartRequest request) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("requestno", txId);
        data.put("returnurl", callbackUrl);
        data.put("sitecode", siteCode);
        data.put("authtype", props.getAuthType());
        data.put("methodtype", props.getMethodType());
        data.put("popupyn", request.channel().isPopup() ? "Y" : "N");
        data.put("receivedata", request.purpose().name() + "|" + request.channel().name());

        // 상품 옵션(통신사 미지정, MO인증 결합 등) - 설정값 반영. 위 핵심 필드는 덮어쓰지 못하게 putIfAbsent
        props.getExtraRequestParams().forEach(data::putIfAbsent);
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JacksonException e) {
            throw new IdentityVerificationException(IdentityErrorCode.INTERNAL_ERROR, "request json failed", e);
        }
    }

    // =====================================================================
    // ③ 콜백 처리
    // =====================================================================
    @Override
    public String extractCorrelationKey(Map<String, String> callbackParams) {
        return callbackParams.get(P_TOKEN_VERSION_ID);
    }

    @Override
    public VerifiedIdentity complete(IdentityTx tx, Map<String, String> params) {
        Map<String, String> st = tx.providerState();
        NiceIdCrypto.Keys keys = new NiceIdCrypto.Keys(st.get(ST_KEY), st.get(ST_IV), st.get(ST_HMAC));

        String encData = params.get(P_ENC_DATA);
        String integrity = params.get(P_INTEGRITY);

        // 1) 무결성 검증 - 복호화보다 먼저! (위변조 데이터는 복호화 시도조차 하지 않음)
        if (!NiceIdCrypto.verifyIntegrity(keys, encData, integrity)) {
            throw new IdentityVerificationException(IdentityErrorCode.INTEGRITY_FAILED, "txId=" + tx.txId());
        }

        // 2) 복호화 → JSON 을 Map 으로 파싱
        String json = NiceIdCrypto.decrypt(keys, encData, NiceIdCrypto.RESPONSE_CHARSET);
        Map<String, Object> r = readJson(json);

        // 3) 결과코드 확인. 실패코드 의미는 NICE 가이드 결과코드표 참고 (detail 로 로그에만 남김)
        String resultCode = str(r, "resultcode");
        if (!RESULT_SUCCESS.equals(resultCode)) {
            throw new IdentityVerificationException(IdentityErrorCode.AUTH_FAILED,
                    "txId=" + tx.txId() + ", resultcode=" + resultCode);
        }

        // 4) 요청번호 일치 확인 - 다른 거래의 결과를 끼워넣는 공격 차단
        String requestNo = str(r, "requestno");
        if (!tx.txId().equals(requestNo)) {
            throw new IdentityVerificationException(IdentityErrorCode.REQUEST_NO_MISMATCH,
                    "txId=" + tx.txId() + ", requestno=" + requestNo);
        }

        // 5) 사이트코드 확인 (다른 사이트 결과 차단 - 방어적 검증)
        String siteCode = str(r, "sitecode");
        if (siteCode != null && !siteCode.equals(st.get(ST_SITE_CODE))) {
            throw new IdentityVerificationException(IdentityErrorCode.REQUEST_NO_MISMATCH, "sitecode mismatch");
        }

        // 6) 공통 표준 모델로 변환
        return new VerifiedIdentity(
                AuthMethod.NICE_MOBILE,
                tx.purpose(),
                tx.channel(),
                resolveName(r),
                str(r, "birthdate"),
                toGender(str(r, "gender")),
                "1".equals(str(r, "nationalinfo")),   // 0:내국인 1:외국인
                str(r, "mobileco"),
                str(r, "mobileno"),
                str(r, "ci"),
                str(r, "di"),
                str(r, "responseno"),
                clock.instant());
    }

    /**
     * 성명: utf8_name(URL 인코딩된 UTF-8)이 있으면 우선 사용.
     * name 필드는 EUC-KR 복호화 결과라 일부 외자/특수 한글이 깨질 수 있다.
     */
    private String resolveName(Map<String, Object> r) {
        String utf8Name = str(r, "utf8_name");
        if (StringUtils.hasText(utf8Name)) {
            return URLDecoder.decode(utf8Name, StandardCharsets.UTF_8);
        }
        return str(r, "name");
    }

    /** NICE 성별코드(0:여, 1:남) → 공통코드(F/M) */
    private static String toGender(String niceGender) {
        if ("1".equals(niceGender)) return "M";
        if ("0".equals(niceGender)) return "F";
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readJson(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JacksonException e) {
            throw new IdentityVerificationException(IdentityErrorCode.INTEGRITY_FAILED, "result json parse failed", e);
        }
    }

    private static String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v == null ? null : String.valueOf(v);
    }
}
