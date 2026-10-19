package com.example.demojsp.common.banner.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 배너팝업목록 TB_BANNER_POPUP (mapper/common/BannerPopupMapper.xml) */
@Mapper
public interface BannerPopupMapper {

    List<BannerPopupRow> selectAll();

    BannerPopupRow selectById(@Param("bannerId") long bannerId);

    int update(BannerPopupRow row);

    int insert(BannerPopupRow row);

    int delete(@Param("bannerId") long bannerId);
}
