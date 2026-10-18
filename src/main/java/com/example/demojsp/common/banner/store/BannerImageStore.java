package com.example.demojsp.common.banner.store;

import com.example.demojsp.common.banner.config.BannerPopupProperties;
import com.example.demojsp.common.banner.domain.BannerImage;
import com.example.demojsp.common.banner.exception.BannerErrorCode;
import com.example.demojsp.common.banner.exception.BannerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 배너 이미지 저장소 (local 디스크). 운영은 공용 파일서버/오브젝트 스토리지 구현으로 교체한다.
 *
 * <p><b>보안</b>
 * <ul>
 *   <li>파일 확장자·Content-Type 헤더를 믿지 않고 <b>파일 앞부분(매직바이트)</b>으로 PNG/JPG/GIF/WEBP 만 허용</li>
 *   <li>SVG·HTML 은 거부 - 같은 도메인에서 열리면 스크립트가 실행될 수 있음 (저장형 XSS)</li>
 *   <li>저장 파일명은 난수 + 판별된 확장자 → 경로 조작(../)·덮어쓰기 불가, 원본 파일명 미보관</li>
 *   <li>조회 시 파일명 형식을 다시 검사하고 저장 폴더 밖을 읽지 않는다</li>
 * </ul>
 */
@Component
public class BannerImageStore {

    private static final Logger audit = LoggerFactory.getLogger("AUDIT.BANNER");
    private static final Pattern NAME = Pattern.compile("^[a-f0-9]{32}\\.(png|jpg|gif|webp)$");
    private static final Map<String, String> TYPES = Map.of(
            "png", "image/png", "jpg", "image/jpeg", "gif", "image/gif", "webp", "image/webp");

    private final BannerPopupProperties properties;
    private final SecureRandom random = new SecureRandom();

    public BannerImageStore(BannerPopupProperties properties) {
        this.properties = properties;
    }

    /** 이미지 저장 → URL 반환 */
    public BannerImage save(byte[] data, String adminId) {
        BannerPopupProperties.ImageUpload cfg = properties.getImageUpload();
        if (data == null || data.length == 0) {
            throw new BannerException(BannerErrorCode.INVALID_IMAGE, "빈 파일");
        }
        if (data.length > cfg.getMaxBytes()) {
            throw new BannerException(BannerErrorCode.IMAGE_TOO_LARGE, "최대 " + (cfg.getMaxBytes() / 1024) + "KB");
        }
        String ext = detect(data);
        if (ext == null) {
            throw new BannerException(BannerErrorCode.INVALID_IMAGE);
        }
        Integer w = null;
        Integer h = null;
        if (!"webp".equals(ext)) {
            try {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
                if (img == null) throw new BannerException(BannerErrorCode.INVALID_IMAGE, "손상된 이미지");
                w = img.getWidth();
                h = img.getHeight();
            } catch (IOException e) {
                throw new BannerException(BannerErrorCode.INVALID_IMAGE, "손상된 이미지");
            }
        }
        byte[] rnd = new byte[16];
        random.nextBytes(rnd);
        String name = HexFormat.of().formatHex(rnd) + "." + ext;
        try {
            Path dir = dir();
            Files.createDirectories(dir);
            Files.write(dir.resolve(name), data);
        } catch (IOException e) {
            throw new UncheckedIOException("배너 이미지 저장 실패", e);
        }
        audit.info("[배너 이미지 등록] name={} type={} bytes={} size={}x{} by={}", name, TYPES.get(ext), data.length, w, h, adminId);
        return new BannerImage(name, cfg.getUrlPrefix() + name, TYPES.get(ext), data.length, w, h);
    }

    /** 이미지 읽기 (없거나 형식이 다르면 BNR-111) */
    public byte[] load(String name) {
        if (name == null || !NAME.matcher(name).matches()) {
            throw new BannerException(BannerErrorCode.IMAGE_NOT_FOUND);
        }
        Path dir = dir();
        Path file = dir.resolve(name).normalize();
        if (!file.startsWith(dir) || !Files.isRegularFile(file)) {
            throw new BannerException(BannerErrorCode.IMAGE_NOT_FOUND);
        }
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new BannerException(BannerErrorCode.IMAGE_NOT_FOUND);
        }
    }

    public static String contentType(String name) {
        String ext = name.substring(name.lastIndexOf('.') + 1);
        return TYPES.getOrDefault(ext, "application/octet-stream");
    }

    private Path dir() {
        String d = properties.getImageUpload().getDir();
        Path p = d == null || d.isBlank() ? Paths.get(System.getProperty("java.io.tmpdir"), "demojsp-banner-images") : Paths.get(d);
        return p.toAbsolutePath().normalize();
    }

    /** 매직바이트로 형식 판별. 허용 형식이 아니면 null */
    static String detect(byte[] b) {
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) return "png";
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "jpg";
        if (b.length >= 6 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8'
                && (b[4] == '7' || b[4] == '9') && b[5] == 'a') return "gif";
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "webp";
        return null;
    }
}
