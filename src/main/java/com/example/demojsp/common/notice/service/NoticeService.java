package com.example.demojsp.common.notice.service;

import com.example.demojsp.common.notice.dto.NoticeDto;
import com.example.demojsp.common.notice.dto.NoticeFilters;
import com.example.demojsp.common.notice.dto.NoticePage;

import java.util.List;

/**
 * [CF-BC-20 / 공통#28] 공지사항 - 사용자 화면(APP) 조회. 게시 중인 공지만 다룬다.
 *
 * <pre>
 *  strip()              메인 띠 - 최신공지 1건 제목 (요구사항 2), 누르면 detail (요구사항 3)
 *  list(scope, target)  공지 목록 - 전체 / 상품별 / 업무별 구분 (요구사항 1)
 *  filters()            목록 탭 건수 · 상품/업무 칩
 *  detail(id)           공지 상세 (+조회수)
 *  popups()             메인 팝업 대상 (ADMIN 요구사항 3: 신규 UI 팝업)
 * </pre>
 */
public interface NoticeService {

    NoticeDto strip();

    /**
     * @param scopeCd  null = 전체 탭(모든 공지), ALL = 전체공지만, PRODUCT = 상품별, BIZ = 업무별
     * @param targetCd 상품코드 / 업무코드 (칩 선택 시), null = 해당 범위 전부
     * @param typeCd   유형 (null = 전부)
     * @param page     1부터
     */
    NoticePage list(String scopeCd, String targetCd, String typeCd, int page);

    NoticeFilters filters();

    NoticeDto detail(long noticeId);

    List<NoticeDto> popups();
}
