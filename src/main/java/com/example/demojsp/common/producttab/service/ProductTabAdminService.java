package com.example.demojsp.common.producttab.service;

import com.example.demojsp.common.producttab.dto.PtItemDto;
import com.example.demojsp.common.producttab.dto.PtProductDto;
import com.example.demojsp.common.producttab.dto.PtSectionDto;
import com.example.demojsp.common.producttab.dto.PtSlideDto;

import java.util.List;

/**
 * [디지털어드민] 상품 탭 관리 (요구사항 세부항목 2: 하단 상품 목록 ADMIN 컨트롤).
 * <ul>
 *   <li>상단 슬라이드 카드 : 등록 / 수정 (게시기간·채널·표시순서·사용여부)</li>
 *   <li>섹션              : 등록 / 수정 (표시순서·사용여부 - 불필요 카드는 사용 N 으로 화면에서 삭제)</li>
 *   <li>섹션별 상품        : 목록 통째 저장 (어떤 상품을 몇 번째로, 배지, 사용여부)</li>
 * </ul>
 * 저장 즉시 사용자 화면에 반영된다 (캐시 없음).
 */
public interface ProductTabAdminService {

    List<PtSlideDto> slides();

    /** slideId 가 없으면 등록, 있으면 수정 */
    PtSlideDto saveSlide(PtSlideDto slide, String adminId);

    List<PtSectionDto> sections();

    PtSectionDto saveSection(PtSectionDto section, String adminId);

    List<PtItemDto> sectionItems(String sectionCd);

    List<PtItemDto> saveSectionItems(String sectionCd, List<PtItemDto> items, String adminId);

    List<PtProductDto> products();
}
