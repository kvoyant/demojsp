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

    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private static final String DARK_TEXT = "#111827";
    private static final String LIGHT_TEXT = "#FFFFFF";
    private static final String BLACK_TEXT = "#000000";

    /** #RRGGBB 형식인지 */
    public static boolean isHexColor(String color) {
        return color != null && HEX_COLOR.matcher(color).matches();
    }

    /**
     * 배경색 위에서 읽을 수 있는 글자색 (WCAG 대비 4.5:1 이상). ADMIN 이 어떤 배경색을 골라도 문구를 읽을 수 있게 한다 (CF-UI-02).
     * <ol>
     *   <li>기본 글자색 #111827 이 4.5:1 이상이고 흰 글자보다 대비가 높으면 #111827</li>
     *   <li>아니면 흰색 #FFFFFF 와 검정 #000000 중 대비가 높은 쪽 (빨강 #FF0000 처럼 중간 밝기 색 대응)</li>
     * </ol>
     * 흰색/검정 중 높은 쪽은 어떤 배경에서도 4.58:1 이상이 보장된다.
     */
    public static String readableTextColor(String bgHex) {
        if (!isHexColor(bgHex)) return DARK_TEXT;
        double withDark = contrastRatio(bgHex, DARK_TEXT);
        double withWhite = contrastRatio(bgHex, LIGHT_TEXT);
        if (withDark >= 4.5 && withDark >= withWhite) return DARK_TEXT;
        return withWhite >= contrastRatio(bgHex, BLACK_TEXT) ? LIGHT_TEXT : BLACK_TEXT;
    }

    /** WCAG 대비율 (테스트·ADMIN 미리보기용) */
    public static double contrastRatio(String hexA, String hexB) {
        double a = luminance(hexA);
        double b = luminance(hexB);
        return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
    }

    private static double luminance(String hex) {
        int rgb = Integer.parseInt(hex.substring(1), 16);
        return 0.2126 * channel((rgb >> 16) & 0xFF) + 0.7152 * channel((rgb >> 8) & 0xFF) + 0.0722 * channel(rgb & 0xFF);
    }

    private static double channel(int c) {
        double v = c / 255.0;
        return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
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
