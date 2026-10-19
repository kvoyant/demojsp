package com.example.demojsp.common.niceid.mapper;

import com.example.demojsp.common.niceid.dto.IdvPurposeDto;
import com.example.demojsp.common.niceid.dto.IdvReqDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/** [공통#21] 본인인증 - SQL 은 resources/mapper/common/NiceIdMapper.xml */
@Mapper
public interface NiceIdMapper {

    IdvPurposeDto selectPurpose(@Param("purposeCd") String purposeCd);

    List<IdvPurposeDto> selectPurposes();

    int mergePurpose(IdvPurposeDto purpose);

    int insertReq(IdvReqDto req);

    IdvReqDto selectReq(@Param("reqNo") String reqNo);

    /** 오입력 횟수 +1 (진행 중일 때만) */
    int increaseTry(@Param("reqNo") String reqNo);

    /** 결과 저장 (REQUESTED 일 때만 바꾼다 → 같은 요청을 두 번 완료 처리하지 않음) */
    int updateResult(IdvReqDto req);

    /** 같은 세션의 최근 요청 수 (과다 요청 차단) */
    int countRecentBySession(@Param("sessKey") String sessKey, @Param("since") LocalDateTime since);

    /** 유의고객 (기존 TB_WATCHLIST, 거래차단 Y) */
    int countWatchlist(@Param("ci") String ci);

    int existsWatchlist(@Param("ci") String ci);

    int insertWatchlist(@Param("ci") String ci, @Param("name") String name, @Param("grade") String grade,
                        @Param("type") String type, @Param("reason") String reason);

    List<IdvReqDto> selectReqList(@Param("purposeCd") String purposeCd, @Param("status") String status,
                                  @Param("limit") int limit);
}
