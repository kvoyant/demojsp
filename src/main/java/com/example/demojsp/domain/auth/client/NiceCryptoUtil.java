package com.example.demojsp.domain.auth.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * =========================================================================================
 * [NICE평가정보(주) 본인확인 암복호화 표준 유틸리티]
 * 
 * 1. 개요:
 *    - NICE 본인확인 API 통신 시 개인정보(이름, 생년월일, CI, DI)는 AES-128-CBC (또는 AES-256-CBC)
 *      대칭키 암호화되어 전송됩니다.
 *    - 무결성 검증을 위해 HMAC-SHA256 서명값을 생성하여 Header/Body에 동봉합니다.
 * 
 * 2. 상용 CPClient.jar 연동 방식 가이드:
 *    - 금융사 기존 레거시 전용선/소프트웨어 방식인 경우:
 *      NiceID.Check.CPClient nice = new NiceID.Check.CPClient();
 *      int iRtn = nice.fnEncode(sSiteCode, sSitePw, sPlainData);
 *      String encData = nice.getCipherData();
 *      int iRtn = nice.fnDecode(sSiteCode, sSitePw, sEncData);
 *      String plainData = nice.getPlainData(); // UTF-8 디코딩
 * 
 * 3. 최신 NICE REST API 플랫폼 방식 (본 구현체 적용):
 *    - Key / IV 유도: SHA-256(req_dt + req_no + token_val)의 32바이트 중
 *      * Key (16바이트): hash[0..15]
 *      * IV  (16바이트): hash[16..31]
 *    - Cipher 알고리즘: "AES/CBC/PKCS5Padding"
 * =========================================================================================
 */
@Slf4j
public class NiceCryptoUtil {

    private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /**
     * NICE API 규격 대칭키 파생 (Key & IV)
     * @param reqDt 요청일시 (YYYYMMDDHHMISS)
     * @param reqNo 금융사 트랜잭션 번호
     * @param tokenVal 토큰값
     */
    public static byte[][] deriveKeyAndIv(String reqDt, String reqNo, String tokenVal) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            String raw = (reqDt != null ? reqDt : "") + (reqNo != null ? reqNo : "") + (tokenVal != null ? tokenVal : "NICE_SECRET_SALT");
            byte[] hash = sha256.digest(raw.getBytes(StandardCharsets.UTF_8));

            byte[] key = new byte[16];
            byte[] iv = new byte[16];
            System.arraycopy(hash, 0, key, 0, 16);
            System.arraycopy(hash, 16, iv, 0, 16);

            return new byte[][]{ key, iv };
        } catch (Exception e) {
            log.error("[NICE 암호화] 키 파생 오류", e);
            throw new RuntimeException("NICE 암호화 키 파생 실패", e);
        }
    }

    /**
     * 평문 데이터 AES-128-CBC 암호화 & Base64 인코딩
     */
    public static String encrypt(String plainText, byte[] key, byte[] iv) {
        if (!StringUtils.hasText(plainText)) return "";
        try {
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("[NICE 암호화] AES 암호화 실패: {}", e.getMessage(), e);
            throw new RuntimeException("NICE 전문 암호화 실패", e);
        }
    }

    /**
     * 암호문(Base64) AES-128-CBC 복호화
     */
    public static String decrypt(String cipherTextBase64, byte[] key, byte[] iv) {
        if (!StringUtils.hasText(cipherTextBase64)) return "";
        try {
            byte[] cipherBytes = Base64.getDecoder().decode(cipherTextBase64);
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] decrypted = cipher.doFinal(cipherBytes);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("[NICE 복호화] AES 복호화 실패: {}", e.getMessage(), e);
            throw new RuntimeException("NICE 전문 복호화 실패", e);
        }
    }

    /**
     * HMAC-SHA256 무결성 검증값(HMAC 서명) 생성
     */
    public static String generateHmac(String data, byte[] key) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(key, HMAC_ALGORITHM);
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (Exception e) {
            log.error("[NICE HMAC] 무결성 해시 생성 실패", e);
            return "";
        }
    }
}
