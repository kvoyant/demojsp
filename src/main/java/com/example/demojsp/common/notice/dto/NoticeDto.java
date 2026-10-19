package com.example.demojsp.common.notice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 공지사항 1건 (TB_CO_NOTICE + 유형명·대상명 조인).
 *
 * <pre>
 *  scopeCd   ALL 전체 / PRODUCT 상품별 / BIZ 업무별        ← 요구사항 1 "전체/상품별/업무별 구분"
 *  targetCd  상품별 = 상품코드(TB_PRODUCT), 업무별 = 업무코드(TB_CO_NOTICE_BIZ), 전체 = null
 *  typeCd    업무공지 / 서비스공지 / 시스템점검 / 기타 ...  ← ADMIN 요구사항 2 "공지사항 유형"
 * </pre>
 * 목록 응답에서는 content 를 비우고(응답 크기 ↓), 상세에서만 채운다.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NoticeDto {

    public static final String SCOPE_ALL = "ALL";
    public static final String SCOPE_PRODUCT = "PRODUCT";
    public static final String SCOPE_BIZ = "BIZ";

    private Long noticeId;
    private String scopeCd;
    private String targetCd;
    private String typeCd;
    private String title;
    private String content;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String fixYn;        // 목록 상단 고정
    private String stripYn;      // 메인 띠 노출 대상
    private String popupYn;      // 메인 팝업
    private String useYn;
    private Integer viewCnt;
    private LocalDateTime regAt;
    private String regBy;
    private LocalDateTime updAt;
    private String updBy;

    /** 조회용 (조인) */
    private String typeNm;
    private String typeColor;
    private String targetNm;     // 상품명 또는 업무명
    /** ADMIN 목록용: 게시중 / 게시예약 / 게시종료 / 미사용 */
    private String status;
}
