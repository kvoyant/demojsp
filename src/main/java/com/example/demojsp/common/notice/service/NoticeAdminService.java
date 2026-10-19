package com.example.demojsp.common.notice.service;

import com.example.demojsp.common.notice.dto.NoticeBizDto;
import com.example.demojsp.common.notice.dto.NoticeDto;
import com.example.demojsp.common.notice.dto.NoticeFilters;
import com.example.demojsp.common.notice.dto.NoticeTypeDto;

import java.util.List;

/**
 * [디지털어드민] 공지사항 관리 (REQ-AP-610)
 * <ol>
 *   <li>전체 / 상품별 / 업무별 구분하여 등록 (노출 범위 + 대상 선택)</li>
 *   <li>공지사항 유형 관리 (AS-IS 4종, TO-BE 검토 후 추가·변경)</li>
 *   <li>메인 띠 · 메인 팝업 노출 여부, 상단 고정, 게시기간</li>
 * </ol>
 */
public interface NoticeAdminService {

    List<NoticeDto> list(String scopeCd, String typeCd, String keyword, int limit);

    NoticeDto get(long noticeId);

    /** noticeId 가 없으면 등록, 있으면 수정 */
    NoticeDto save(NoticeDto notice, String adminId);

    List<NoticeTypeDto> types();

    NoticeTypeDto saveType(NoticeTypeDto type, String adminId);

    List<NoticeBizDto> bizs();

    NoticeBizDto saveBiz(NoticeBizDto biz, String adminId);

    /** 대상 선택 상자: products = 상품 원장, bizs = 업무 구분 */
    NoticeFilters targets();
}
