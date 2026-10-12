package com.example.demojsp.domain.auth.mapper;

import com.example.demojsp.domain.auth.dto.CertLogDto;
import com.example.demojsp.domain.auth.dto.CustCiDto;
import com.example.demojsp.domain.auth.dto.TermsDto;
import com.example.demojsp.domain.auth.dto.WatchlistDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NiceAuthMapper {

    // 약관 관련
    List<TermsDto> selectActiveTermsList();
    TermsDto selectTermsById(@Param("termsId") String termsId);
    int insertTermsAgreeLog(@Param("reqTxSeq") String reqTxSeq, @Param("termsId") String termsId, @Param("agreeYn") String agreeYn);

    // 본인인증 감사 로그
    int insertCertLog(CertLogDto certLog);
    int updateCertLog(CertLogDto certLog);
    CertLogDto selectCertLogByTxSeq(@Param("reqTxSeq") String reqTxSeq);

    // 유의고객 조회 (CI 기반)
    WatchlistDto selectWatchlistByCi(@Param("ciVal") String ciVal);

    // 고객 CI 마스터 관리
    CustCiDto selectCustCiByCi(@Param("ciVal") String ciVal);
    int insertOrUpdateCustCi(CustCiDto custCi);
}
