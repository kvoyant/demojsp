package com.example.demojsp.common.menu.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 뱅킹메뉴목록 TB_BANKING_MENU (mapper/common/BankingMenuMapper.xml) */
@Mapper
public interface BankingMenuMapper {

    List<BankingMenuRow> selectAll();

    BankingMenuRow selectById(@Param("menuId") long menuId);

    int update(BankingMenuRow row);

    int insert(BankingMenuRow row);
}
