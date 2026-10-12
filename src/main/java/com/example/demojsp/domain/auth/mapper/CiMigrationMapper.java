package com.example.demojsp.domain.auth.mapper;

import com.example.demojsp.domain.auth.dto.CiMigrationDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CiMigrationMapper {

    // 스테이징 테이블 조회
    List<CiMigrationDto.TempRecord> selectTempList(@Param("validStatus") String validStatus, 
                                                   @Param("migrationStatus") String migrationStatus);

    // 검증 상태 업데이트
    int updateTempValidStatus(@Param("tempSeq") Long tempSeq, 
                              @Param("validStatus") String validStatus, 
                              @Param("errorMsg") String errorMsg);

    // 이관 상태 업데이트
    int updateTempMigrationStatus(@Param("tempSeq") Long tempSeq, 
                                  @Param("migrationStatus") String migrationStatus);

    // 고객 CI 기등록 여부 확인
    int countExistingCustCi(@Param("ciVal") String ciVal);

    // 마이그레이션 이력 관리
    int insertHistory(CiMigrationDto.HistoryRecord hist);
    int updateHistory(CiMigrationDto.HistoryRecord hist);
    List<CiMigrationDto.HistoryRecord> selectHistoryList();

    // 임시 데이터 재적재 및 초기화 (테스트 및 운영 관리용)
    int clearTempRecords();
    int insertSampleTempData();
}
