package com.example.demojsp.common.notice.dto;

/**
 * 메인 띠 응답. 게시 중인 띠 공지가 없으면 notice = null → 화면은 띠 영역을 숨긴다.
 *
 * @param notice    최신공지 (제목·유형만 사용)
 * @param detailUrl 띠를 눌렀을 때 이동할 공지 상세 주소 (application.yaml common.notice.detail-url + ?id=)
 */
public record NoticeStripView(NoticeDto notice, String detailUrl) {
}
