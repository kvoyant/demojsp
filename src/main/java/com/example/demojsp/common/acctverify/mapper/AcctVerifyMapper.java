package com.example.demojsp.common.acctverify.mapper;

import com.example.demojsp.common.acctverify.dto.AvBankDto;
import com.example.demojsp.common.acctverify.dto.AvBizDto;
import com.example.demojsp.common.acctverify.dto.AvReqDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** [공통#25] 1원 계좌인증 - SQL 은 resources/mapper/common/AcctVerifyMapper.xml */
@Mapper
public interface AcctVerifyMapper {

    AvBizDto selectBiz(@Param("bizCd") String bizCd);

    List<AvBizDto> selectBizList();

    int mergeBiz(AvBizDto biz);

    /** 사용 중 은행 (기존 TB_BANK_CODE) */
    List<AvBankDto> selectBanks();

    AvBankDto selectBank(@Param("bankCd") String bankCd);

    /** 고객의 대출 입금/출금계좌 (기존 TB_LOAN_ACCOUNT) - 컬럼: LOAN_NO, PRODUCT_NM, BANK_CD, BANK_NM, ACCOUNT_NO, ACCOUNT_TYPE */
    List<Map<String, Object>> selectLoanAccounts(@Param("memberId") String memberId);

    Map<String, Object> selectLoanAccount(@Param("memberId") String memberId, @Param("loanNo") String loanNo);

    int insertReq(AvReqDto req);

    AvReqDto selectReq(@Param("reqNo") String reqNo);

    int increaseTry(@Param("reqNo") String reqNo);

    /** 결과 저장 (SENT 일 때만 바꾼다) */
    int updateResult(@Param("reqNo") String reqNo, @Param("status") String status, @Param("resultCd") String resultCd,
                     @Param("doneAt") LocalDateTime doneAt);

    int countRecentBySession(@Param("sessKey") String sessKey, @Param("since") LocalDateTime since);

    List<AvReqDto> selectReqList(@Param("bizCd") String bizCd, @Param("status") String status, @Param("limit") int limit);

    // ---- 샘플 적재 (기존 테이블이 비어 있을 때만)
    int countBanks();

    int countLoanAccounts();
}
