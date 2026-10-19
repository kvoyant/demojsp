package com.example.demojsp.common.store.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 공통 채번 테이블 TB_CO_SEQ (mapper/common/CommonSequenceMapper.xml) */
@Mapper
public interface CommonSequenceMapper {

    int increase(@Param("seqName") String seqName);

    Long current(@Param("seqName") String seqName);

    int insert(@Param("seqName") String seqName, @Param("startValue") long startValue);
}
