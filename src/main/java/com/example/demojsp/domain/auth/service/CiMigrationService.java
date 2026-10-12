package com.example.demojsp.domain.auth.service;

import com.example.demojsp.domain.auth.dto.CiMigrationDto;

import java.util.List;

public interface CiMigrationService {

    /**
     * 스테이징(임시) 테이블의 이기종 CI 데이터 목록 조회
     */
    List<CiMigrationDto.TempRecord> getTempRecords(String validStatus, String migrationStatus);

    /**
     * 9/30 이기종 CI 데이터 정합성 검증
     * - CI 길이(88바이트) 및 Base64 인코딩 검증
     * - 중복 CI 여부 검증
     */
    int validateStagingRecords();

    /**
     * 검증 통과 데이터 마이그레이션 실행 (TB_CI_MIGRATION_TEMP -> TB_CUST_CI)
     */
    CiMigrationDto.ExecutionResult executeMigration(String execBy);

    /**
     * 이관 실행 이력 목록 조회
     */
    List<CiMigrationDto.HistoryRecord> getMigrationHistories();

    /**
     * 테스트용 9/30 이기종 CI 샘플 데이터 초기화/재적재
     */
    void resetSampleData();
}
