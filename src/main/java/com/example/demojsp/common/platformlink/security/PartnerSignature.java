package com.example.demojsp.common.platformlink.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 제휴사 요청 서명 (HMAC-SHA256) - ※ 제휴사별 실제 인증 방식(토큰·mTLS·전문 암호화 등)은 연동규격에 따라 바꾼다.
 * <pre>
 *  헤더  X-Link-Timestamp : epoch ms
 *        X-Link-Signature : Base64( HMAC-SHA256( 서명키, 제휴사코드 + "\n" + timestamp + "\n" + 본문 ) )
 * </pre>
 * 본문까지 서명하므로 중간에 값이 바뀌면 검증에 실패하고, timestamp 로 오래된 요청의 재전송을 막는다.
 */
public final class PartnerSignature {

    public static final String H_TIMESTAMP = "X-Link-Timestamp";
    public static final String H_SIGNATURE = "X-Link-Signature";

    private PartnerSignature() {
    }

    public static String sign(String secret, String partnerCd, String timestamp, String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] d = mac.doFinal((partnerCd + "\n" + timestamp + "\n" + (body == null ? "" : body)).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(d);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** 서명 일치 (상수 시간 비교) */
    public static boolean verify(String secret, String partnerCd, String timestamp, String body, String signature) {
        if (secret == null || secret.isBlank() || timestamp == null || signature == null) return false;
        byte[] expected = sign(secret, partnerCd, timestamp, body).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, signature.getBytes(StandardCharsets.UTF_8));
    }
}
