package com.example.demojsp.common.banner.service;

import com.example.demojsp.common.banner.exception.BannerErrorCode;
import com.example.demojsp.common.banner.exception.BannerException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * 배너 모듈 내부 유틸 (일시 변환, URL 검증).
 */
public final class BannerSupport {

    private static final DateTimeFormatter MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter FULL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 같은 도메인 상대경로 ("/" 로 시작, "//" 프로토콜 상대 URL 은 외부로 나가므로 불가) */
    private static final Pattern RELATIVE = Pattern.compile("^/(?!/)[^\\s<>\"'\\\\]*$");
    /** 외부 URL (http/https 만, javascript:/data: 등 차단) */
    private static final Pattern ABSOLUTE = Pattern.compile("^https?://[A-Za-z0-9.\\-]+(:\\d{1,5})?(/[^\\s<>\"'\\\\]*)?$");

    private BannerSupport() {
    }

    public static String format(LocalDateTime t) {
        return t == null ? null : t.format(MINUTE);
    }

    public static String formatFull(LocalDateTime t) {
        return t == null ? null : t.format(FULL);
    }

    /**
     * 일시 문자열 → LocalDateTime.
     * <p>허용: "yyyy-MM-dd HH:mm", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-ddTHH:mm[:ss]"(input datetime-local)
     *
     * @param field 오류 시 화면에 표시할 항목명
     */
    public static LocalDateTime parse(String text, String field) {
        if (text == null || text.isBlank()) {
            throw new BannerException(BannerErrorCode.REQUIRED_MISSING, field);
        }
        String v = text.strip().replace(' ', 'T');
        if (v.length() == 16) {
            v = v + ":00";
        }
        try {
            return LocalDateTime.parse(v);
        } catch (DateTimeParseException e) {
            throw new BannerException(BannerErrorCode.INVALID_DATETIME, field);
        }
    }

    /** 링크 URL 허용 여부 (상대경로 또는 http/https) */
    public static boolean isSafeUrl(String url) {
        return url != null && url.length() <= 500 && (RELATIVE.matcher(url).matches() || ABSOLUTE.matcher(url).matches());
    }

    /** 공백 → null, 앞뒤 공백 제거 */
    public static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.strip();
        return t.isEmpty() ? null : t;
    }
}
