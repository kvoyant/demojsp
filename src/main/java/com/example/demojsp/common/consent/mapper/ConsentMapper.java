package com.example.demojsp.common.consent.mapper;

import com.example.demojsp.common.consent.dto.ConsentAgreeDto;
import com.example.demojsp.common.consent.dto.ConsentAgreeItemDto;
import com.example.demojsp.common.consent.dto.ConsentBizDto;
import com.example.demojsp.common.consent.dto.ConsentTermsDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/** 약관동의 (DB 방식) - SQL 은 resources/mapper/common/ConsentMapper.xml */
@Mapper
public interface ConsentMapper {

    // ===================================================== 사용자 화면
    /** 업무코드 1건 (사용여부 무관) */
    ConsentBizDto selectBiz(@Param("bizCd") String bizCd);

    /** 업무의 현재 약관 목록 (baseDt 기준 시행 중인 최신 버전, 전문 제외) - 필수 먼저, 표시순서 순 */
    List<ConsentTermsDto> selectBizTerms(@Param("bizCd") String bizCd, @Param("baseDt") LocalDate baseDt);

    /** 약관 1개 버전 (전문 포함) */
    ConsentTermsDto selectTermsVer(@Param("termsCd") String termsCd, @Param("termsVer") int termsVer);

    /** 전문 해시가 비어 있으면 채운다 (CN_HASH IS NULL 일 때만) */
    int updateTermsHash(@Param("termsCd") String termsCd, @Param("termsVer") int termsVer, @Param("cnHash") String cnHash);

    /** 동의 헤더 저장 → agreeNo 가 채워진다 (AUTO_INCREMENT) */
    int insertAgree(ConsentAgreeDto agree);

    int insertAgreeDtl(ConsentAgreeItemDto item);

    ConsentAgreeDto selectAgree(@Param("agreeNo") long agreeNo);

    List<ConsentAgreeItemDto> selectAgreeDtl(@Param("agreeNo") long agreeNo);

    // ===================================================== ADMIN
    List<ConsentBizDto> selectBizList();

    int mergeBiz(ConsentBizDto biz);

    /** 업무별 약관 전체 (사용 안 함 포함) + 각 약관의 현재 버전 정보 */
    List<ConsentTermsDto> selectBizTermsAll(@Param("bizCd") String bizCd, @Param("baseDt") LocalDate baseDt);

    int deleteBizTerms(@Param("bizCd") String bizCd);

    int insertBizTerms(ConsentTermsDto row);

    /** 약관코드별 1행: 최신 등록 버전의 이름 + 버전 수 + 현재 시행 버전 */
    List<ConsentTermsDto> selectTermsList(@Param("baseDt") LocalDate baseDt);

    List<ConsentTermsDto> selectTermsVersions(@Param("termsCd") String termsCd);

    /** 약관코드의 마지막 버전 (없으면 null) */
    ConsentTermsDto selectLastVer(@Param("termsCd") String termsCd);

    int insertTerms(ConsentTermsDto terms);

    List<ConsentAgreeDto> selectAgreeList(@Param("bizCd") String bizCd, @Param("custId") String custId, @Param("limit") int limit);
}
