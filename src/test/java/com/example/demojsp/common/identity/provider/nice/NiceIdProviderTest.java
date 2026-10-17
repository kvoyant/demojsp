package com.example.demojsp.common.identity.provider.nice;

import com.example.demojsp.common.identity.config.IdentityProperties;
import com.example.demojsp.common.identity.domain.AuthChannel;
import com.example.demojsp.common.identity.domain.AuthMethod;
import com.example.demojsp.common.identity.domain.AuthPurpose;
import com.example.demojsp.common.identity.domain.IdentityTx;
import com.example.demojsp.common.identity.domain.ProviderPrepareResult;
import com.example.demojsp.common.identity.domain.StartRequest;
import com.example.demojsp.common.identity.domain.VerifiedIdentity;
import com.example.demojsp.common.identity.exception.IdentityErrorCode;
import com.example.demojsp.common.identity.exception.IdentityVerificationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * NiceIdProvider 단위 테스트 (Spring 컨텍스트/DB 불필요).
 * NICE API 는 Mockito 로 대체하고, "NICE 가 보내줄 암호화 콜백"을 테스트에서 직접 만들어 complete() 를 검증한다.
 */
class NiceIdProviderTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-18T03:00:00Z"), ZoneId.of("Asia/Seoul"));
    private final ObjectMapper om = JsonMapper.builder().build();
    private NiceIdProvider provider;

    @BeforeEach
    void setUp() {
        NiceIdApiClient api = mock(NiceIdApiClient.class);
        when(api.requestCryptoToken(anyString(), anyString()))
                .thenReturn(new NiceIdApiClient.CryptoToken("SITE01", "TVID-001", "tokenVal"));
        IdentityProperties props = new IdentityProperties();
        props.getNice().getExtraRequestParams().put("sampleoption", "Y");
        provider = new NiceIdProvider(api, props, om, clock);
    }

    private StartRequest req(AuthChannel ch) {
        return new StartRequest(AuthMethod.NICE_MOBILE, ch, AuthPurpose.SIGN_UP, null, List.of(), Map.of());
    }

    private IdentityTx txFrom(String txId, ProviderPrepareResult p) {
        return new IdentityTx(txId, p.correlationKey(), AuthMethod.NICE_MOBILE, AuthChannel.PC_WEB,
                AuthPurpose.SIGN_UP, "S1", null, p.providerState(), clock.instant(), clock.instant().plusSeconds(600));
    }

    private NiceIdCrypto.Keys keysOf(ProviderPrepareResult p) {
        Map<String, String> st = p.providerState();
        return new NiceIdCrypto.Keys(st.get("nice.key"), st.get("nice.iv"), st.get("nice.hmac"));
    }

    /** NICE 가 보내는 것과 같은 형식의 콜백 파라미터 생성 (응답 JSON 은 EUC-KR 로 암호화) */
    private Map<String, String> callback(ProviderPrepareResult p, Map<String, String> result) throws Exception {
        NiceIdCrypto.Keys keys = keysOf(p);
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keys.key().getBytes(StandardCharsets.UTF_8), "AES"),
                new IvParameterSpec(keys.iv().getBytes(StandardCharsets.UTF_8)));
        String enc = Base64.getEncoder().encodeToString(
                c.doFinal(om.writeValueAsString(result).getBytes(NiceIdCrypto.RESPONSE_CHARSET)));
        return new HashMap<>(Map.of("token_version_id", p.correlationKey(), "enc_data", enc,
                "integrity_value", NiceIdCrypto.hmac(keys, enc)));
    }

    private Map<String, String> successResult(String txId) {
        Map<String, String> r = new HashMap<>();
        r.put("resultcode", "0000");
        r.put("requestno", txId);
        r.put("sitecode", "SITE01");
        r.put("responseno", "NICE-RES-1");
        r.put("name", "홍길동");
        r.put("utf8_name", URLEncoder.encode("홍길동", StandardCharsets.UTF_8));
        r.put("birthdate", "19900101");
        r.put("gender", "1");
        r.put("nationalinfo", "0");
        r.put("mobileco", "1");
        r.put("mobileno", "01012345678");
        r.put("ci", "CI_VALUE_0123456789012345678901234567890");
        r.put("di", "DI_VALUE");
        return r;
    }

    private static IdentityErrorCode codeOf(Throwable e) {
        return ((IdentityVerificationException) e).getErrorCode();
    }

    @Test
    @DisplayName("prepare: NICE 표준창 form 정보 생성 + 요청데이터 암호화 + 채널별 popupyn")
    void prepare() {
        ProviderPrepareResult p = provider.prepare("IDV1", req(AuthChannel.APP), "https://x/cb/nice_mobile");

        assertThat(p.correlationKey()).isEqualTo("TVID-001");
        assertThat(p.formParams()).containsKeys("m", "token_version_id", "enc_data", "integrity_value");

        @SuppressWarnings("unchecked")
        Map<String, Object> json = om.readValue(
                NiceIdCrypto.decrypt(keysOf(p), p.formParams().get("enc_data"), StandardCharsets.UTF_8), Map.class);
        assertThat(json).containsEntry("requestno", "IDV1")
                .containsEntry("popupyn", "N")          // APP 채널 → 팝업 아님
                .containsEntry("sampleoption", "Y");    // extra-request-params 반영
    }

    @Test
    @DisplayName("complete: 성공 시 공통모델 변환, 로그 문자열에 CI 원문 미노출")
    void completeSuccess() throws Exception {
        ProviderPrepareResult p = provider.prepare("IDV1", req(AuthChannel.PC_WEB), "cb");
        VerifiedIdentity v = provider.complete(txFrom("IDV1", p), callback(p, successResult("IDV1")));

        assertThat(v.name()).isEqualTo("홍길동");
        assertThat(v.gender()).isEqualTo("M");
        assertThat(v.foreigner()).isFalse();
        assertThat(v.ci()).startsWith("CI_VALUE");
        assertThat(v.toString()).doesNotContain("CI_VALUE_0123456789012345678901234567890");
    }

    @Test
    @DisplayName("complete: 무결성값 위변조 → IDV-201")
    void integrityFail() throws Exception {
        ProviderPrepareResult p = provider.prepare("IDV1", req(AuthChannel.PC_WEB), "cb");
        Map<String, String> cb = callback(p, successResult("IDV1"));
        cb.put("integrity_value", "tampered");
        assertThatThrownBy(() -> provider.complete(txFrom("IDV1", p), cb))
                .extracting(NiceIdProviderTest::codeOf).isEqualTo(IdentityErrorCode.INTEGRITY_FAILED);
    }

    @Test
    @DisplayName("complete: 요청번호 불일치 → IDV-104")
    void requestNoMismatch() throws Exception {
        ProviderPrepareResult p = provider.prepare("IDV1", req(AuthChannel.PC_WEB), "cb");
        Map<String, String> cb = callback(p, successResult("OTHER"));
        assertThatThrownBy(() -> provider.complete(txFrom("IDV1", p), cb))
                .extracting(NiceIdProviderTest::codeOf).isEqualTo(IdentityErrorCode.REQUEST_NO_MISMATCH);
    }

    @Test
    @DisplayName("complete: NICE 결과코드 실패 → IDV-202")
    void authFailed() throws Exception {
        ProviderPrepareResult p = provider.prepare("IDV1", req(AuthChannel.PC_WEB), "cb");
        Map<String, String> r = successResult("IDV1");
        r.put("resultcode", "0001");
        Map<String, String> cb = callback(p, r);
        assertThatThrownBy(() -> provider.complete(txFrom("IDV1", p), cb))
                .extracting(NiceIdProviderTest::codeOf).isEqualTo(IdentityErrorCode.AUTH_FAILED);
    }
}
