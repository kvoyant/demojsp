package com.example.demojsp.domain.login.mapper;

import com.example.demojsp.domain.login.dto.LoginLogDto;
import com.example.demojsp.domain.login.dto.MemberDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MemberMapper {

    MemberDto selectMemberById(@Param("memberId") String memberId);

    MemberDto selectMemberByCi(@Param("ciVal") String ciVal);

    int updateLoginSuccess(@Param("memberId") String memberId);

    int increaseLoginFailCount(@Param("memberId") String memberId);

    int lockMember(@Param("memberId") String memberId);

    int unlockMember(@Param("memberId") String memberId);

    int insertMember(MemberDto member);

    int insertLoginLog(LoginLogDto loginLog);
}
