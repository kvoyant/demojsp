package com.example.demojsp.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class CiMigrationDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TempRecord {
        private Long tempSeq;
        private String legacySystemCd;
        private String legacyCustId;
        private String custNm;
        private String ciVal;
        private String diVal;
        private String birthDate;
        private String gender;
        private String nationalInfo;
        private String cellNo;
        private String rrnHash;
        private String validStatus;
        private String migrationStatus;
        private String errorMsg;
        private String sourceExtractDt;
        private LocalDateTime regDt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionResult {
        private String batchId;
        private int totalCount;
        private int validCount;
        private int successCount;
        private int duplicateCount;
        private int failCount;
        private String status;
        private LocalDateTime startDt;
        private LocalDateTime endDt;
        private long durationMs;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryRecord {
        private Long histSeq;
        private String batchId;
        private Integer totalCount;
        private Integer successCount;
        private Integer duplicateCount;
        private Integer failCount;
        private LocalDateTime startDt;
        private LocalDateTime endDt;
        private String status;
        private String execBy;
    }
}
