package com.example.demojsp.common.producttab.mapper;

import com.example.demojsp.common.producttab.dto.PtItemDto;
import com.example.demojsp.common.producttab.dto.PtProductDto;
import com.example.demojsp.common.producttab.dto.PtSectionDto;
import com.example.demojsp.common.producttab.dto.PtSlideDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/** [공통#26] 메인화면 상품 탭 - SQL 은 resources/mapper/common/ProductTabMapper.xml */
@Mapper
public interface ProductTabMapper {

    // ===================================================== 사용자 화면
    /** 게시 중 슬라이드 (사용 Y · 기간 안 · 연결 상품이 있으면 판매중) - 채널은 서비스에서 거른다 */
    List<PtSlideDto> selectActiveSlides(@Param("baseDt") LocalDate baseDt);

    /** 사용 중 섹션 (표시순서) */
    List<PtSectionDto> selectActiveSections();

    /** 사용 중 섹션의 화면 표시 상품 (항목 사용 Y · 상품 판매중) */
    List<PtItemDto> selectActiveItems();

    // ===================================================== ADMIN - 슬라이드
    List<PtSlideDto> selectSlides();

    PtSlideDto selectSlide(@Param("slideId") long slideId);

    int insertSlide(PtSlideDto slide);

    int updateSlide(PtSlideDto slide);

    // ===================================================== ADMIN - 섹션 · 상품
    List<PtSectionDto> selectSections();

    PtSectionDto selectSection(@Param("sectionCd") String sectionCd);

    int mergeSection(PtSectionDto section);

    /** 섹션의 상품 전체 (사용 안 함 · 판매중지 포함) */
    List<PtItemDto> selectSectionItems(@Param("sectionCd") String sectionCd);

    int deleteSectionItems(@Param("sectionCd") String sectionCd);

    int insertSectionItem(PtItemDto item);

    /** 상품 원장 목록 (선택 상자) */
    List<PtProductDto> selectProducts();
}
