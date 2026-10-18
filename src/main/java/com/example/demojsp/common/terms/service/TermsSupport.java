package com.example.demojsp.common.terms.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

/**
 * 약관 모듈 내부 유틸 (해시, 시각, 거래번호).
 */
public final class TermsSupport {

    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter COMPACT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final SecureRandom RANDOM = new SecureRandom();

    private TermsSupport() {
    }

    /**
     * 약관 전문 SHA-256 (hex 64자).
     * <p>줄바꿈(CRLF/LF) 차이와 앞뒤 공백 때문에 같은 문구가 다른 해시가 되지 않도록 정규화 후 계산한다.
     */
    public static String sha256(String content) {
        String normalized = content == null ? "" : content.replace("\r\n", "\n").strip();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static String now() {
        return format(Clock.systemDefaultZone());
    }

    public static String format(Clock clock) {
        return LocalDateTime.now(clock).format(DISPLAY);
    }

    /** 동의 거래번호: TRM + yyyyMMddHHmmss + 랜덤 8자리 = 25자 */
    public static String newAgreementId(Clock clock) {
        return "TRM" + LocalDateTime.now(clock).format(COMPACT) + String.format("%08d", RANDOM.nextInt(100_000_000));
    }
}
