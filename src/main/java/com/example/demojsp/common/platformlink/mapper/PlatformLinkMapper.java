package com.example.demojsp.common.platformlink.mapper;

import com.example.demojsp.common.platformlink.dto.LnkCodeMapDto;
import com.example.demojsp.common.platformlink.dto.LnkPartnerDto;
import com.example.demojsp.common.platformlink.dto.LnkProductDto;
import com.example.demojsp.common.platformlink.dto.LnkReqDto;
import com.example.demojsp.common.platformlink.dto.LnkStatDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/** 플랫폼 대출 연계 매퍼 - resources/mapper/common/PlatformLinkMapper.xml */
@Mapper
public interface PlatformLinkMapper {

    // ---- 제휴사
    LnkPartnerDto selectPartner(@Param("partnerCd") String partnerCd);

    List<LnkPartnerDto> selectPartners();

    int updatePartner(LnkPartnerDto p);

    // ---- 상품 · 코드 변환
    LnkProductDto selectProduct(@Param("cssProduct") String cssProduct);

    List<LnkProductDto> selectProducts();

    String selectOuterPrdCd(@Param("partnerCd") String partnerCd, @Param("cssProduct") String cssProduct);

    LnkCodeMapDto selectCodeMap(@Param("partnerCd") String partnerCd, @Param("innerCd") String innerCd);

    List<LnkCodeMapDto> selectCodeMaps(@Param("partnerCd") String partnerCd);

    // ---- 이력
    int insertReq(LnkReqDto r);

    int updateReq(LnkReqDto r);

    LnkReqDto selectReqByPartnerReq(@Param("partnerCd") String partnerCd, @Param("partnerReqNo") String partnerReqNo);

    List<LnkReqDto> selectReqList(@Param("partnerCd") String partnerCd, @Param("status") String status,
                                  @Param("resultCd") String resultCd, @Param("limit") int limit);

    List<LnkStatDto> selectStats(@Param("since") LocalDateTime since);
}
