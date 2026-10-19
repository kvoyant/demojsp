package com.example.demojsp.common.resume.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 이어하기 · 회사 마스터 (mapper/common/ResumeMapper.xml) */
@Mapper
public interface ResumeMapper {

    List<ResumePointRow> selectAll();

    List<ResumePointRow> selectByCustomer(@Param("customerId") String customerId);

    ResumePointRow selectById(@Param("resumeId") long resumeId);

    int update(ResumePointRow row);

    int insert(ResumePointRow row);

    List<PartnerRow> selectPartners();

    PartnerRow selectPartner(@Param("partnerCd") String partnerCd);

    int insertPartner(PartnerRow row);
}
