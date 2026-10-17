package com.example.demojsp.common.identity.provider.nice;

import com.example.demojsp.common.identity.exception.IdentityErrorCode;
import com.example.demojsp.common.identity.exception.IdentityVerificationException;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * NICE ID 암복호화 유틸 (NICE 개발가이드의 "대칭키 생성 / 요청데이터 암호화 / 무결성값 생성" 절차 구현).
 *
 * <h3>키 생성 절차 (가이드 기준)</h3>
 * <pre>
 *  value     = req_dtim.trim() + req_no.trim() + token_val.trim()
 *  resultVal = Base64( SHA-256(value) )                 // 44자 문자열
 *  key       = resultVal 앞 16자   → AES-128 대칭키
 *  iv        = resultVal 뒤 16자   → CBC 초기화 벡터
 *  hmacKey   = resultVal 앞 32자   → HMAC-SHA256 키
 * </pre>
 *
 * <h3>암호화</h3>
 * <pre>
 *  enc_data        = Base64( AES/CBC/PKCS5Padding(key, iv, 요청JSON) )
 *  integrity_value = Base64( HMAC-SHA256(hmacKey, enc_data) )
 * </pre>
 *
 * <p>상태가 없는 순수 함수만 두어 단위테스트가 쉽도록 했다. (NiceIdCryptoTest 참고)
 *
 * <p><b>기존 {@code domain.auth.client.NiceCryptoUtil} 과의 차이</b>:
 * 기존 유틸은 SHA-256 해시 "바이트" 앞/뒤 16byte 를 Key/IV 로 쓰고, 공통모듈은 NICE 표준창 가이드대로
 * Base64(SHA-256) "문자열" 의 앞16/뒤16/앞32자를 Key/IV/HMAC키로 쓴다. 규격이 달라 별도 클래스로 둔다.
 * (기존 유틸은 수정하지 않음)
 */
public final class NiceIdCrypto {

    /** 응답 복호화 문자셋. NICE 가이드 샘플 기준 EUC-KR (utf8_name 필드는 별도로 URL 인코딩된 UTF-8) */
    public static final Charset RESPONSE_CHARSET = Charset.forName("EUC-KR");

    private static final String AES_TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private NiceIdCrypto() {
    }

    /** 대칭키 묶음 (거래마다 새로 생성, 서버 TxStore 에만 보관) */
    public record Keys(String key, String iv, String hmacKey) {
        @Override
        public String toString() {
            return "Keys[****]"; // 로그 노출 방지
        }
    }

    /** 암호화 토큰 발급 결과로 대칭키/IV/HMAC키를 생성한다. */
    public static Keys deriveKeys(String reqDtim, String reqNo, String tokenVal) {
        try {
            String value = reqDtim.trim() + reqNo.trim() + tokenVal.trim();
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            String resultVal = Base64.getEncoder().encodeToString(digest);
            return new Keys(
                    resultVal.substring(0, 16),
                    resultVal.substring(resultVal.length() - 16),
                    resultVal.substring(0, 32));
        } catch (Exception e) {
            throw new IdentityVerificationException(IdentityErrorCode.INTERNAL_ERROR, "NICE key derive failed", e);
        }
    }

    /** 요청 데이터(JSON 문자열) 암호화 → enc_data */
    public static String encrypt(Keys keys, String plainJson) {
        try {
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey(keys), new IvParameterSpec(keys.iv().getBytes(StandardCharsets.UTF_8)));
            byte[] encrypted = cipher.doFinal(plainJson.trim().getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new IdentityVerificationException(IdentityErrorCode.INTERNAL_ERROR, "NICE encrypt failed", e);
        }
    }

    /** 콜백 enc_data 복호화 → 결과 JSON 문자열 */
    public static String decrypt(Keys keys, String encData, Charset charset) {
        try {
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, aesKey(keys), new IvParameterSpec(keys.iv().getBytes(StandardCharsets.UTF_8)));
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encData));
            return new String(decrypted, charset);
        } catch (Exception e) {
            // 키 불일치/데이터 손상 → 위변조 의심으로 처리
            throw new IdentityVerificationException(IdentityErrorCode.INTEGRITY_FAILED, "NICE decrypt failed", e);
        }
    }

    /** enc_data 에 대한 무결성값(integrity_value) 생성 */
    public static String hmac(Keys keys, String encData) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(keys.hmacKey().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return Base64.getEncoder().encodeToString(mac.doFinal(encData.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IdentityVerificationException(IdentityErrorCode.INTERNAL_ERROR, "NICE hmac failed", e);
        }
    }

    /**
     * 콜백 무결성 검증.
     * <p>MessageDigest.isEqual 로 비교한다. (String.equals 는 타이밍 공격에 취약)
     */
    public static boolean verifyIntegrity(Keys keys, String encData, String integrityValue) {
        if (encData == null || integrityValue == null) {
            return false;
        }
        byte[] expected = hmac(keys, encData).getBytes(StandardCharsets.UTF_8);
        byte[] actual = integrityValue.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    private static SecretKeySpec aesKey(Keys keys) {
        return new SecretKeySpec(keys.key().getBytes(StandardCharsets.UTF_8), "AES");
    }
}
