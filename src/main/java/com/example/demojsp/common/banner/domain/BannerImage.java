package com.example.demojsp.common.banner.domain;

/**
 * 업로드된 배너 이미지 (공통#45 기능2 "이미지 파일선택").
 *
 * @param name        저장 파일명 (난수 32자리 + 확장자 - 원본 파일명은 저장·노출하지 않는다)
 * @param url         화면에서 쓰는 URL (배너 imageUrl 에 그대로 입력)
 * @param contentType image/png 등 (파일 내용으로 판별한 값)
 * @param bytes       용량
 * @param width       가로 px (판별 불가 시 null - WEBP 등)
 * @param height      세로 px
 */
public record BannerImage(
        String name,
        String url,
        String contentType,
        long bytes,
        Integer width,
        Integer height
) {
}
