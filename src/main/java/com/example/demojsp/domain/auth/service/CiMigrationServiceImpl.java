package com.example.demojsp.domain.auth.service;

import com.example.demojsp.domain.auth.dto.CustCiDto;
import com.example.demojsp.domain.auth.dto.CiMigrationDto;
import com.example.demojsp.domain.auth.mapper.CiMigrationMapper;
import com.example.demojsp.domain.auth.mapper.NiceAuthMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class CiMigrationServiceImpl implements CiMigrationService {

    private final CiMigrationMapper ciMigrationMapper;
    private final NiceAuthMapper niceAuthMapper;

    // Base64 88자리 정규식 (NICE 표준 CI 규격: 표준 Base64 및 이기종 URL-safe 지원)
    private static final Pattern BASE64_PATTERN = Pattern.compile("^[A-Za-z0-9+/=\\-_]{88}$");

    @Override
    public List<CiMigrationDto.TempRecord> getTempRecords(String validStatus, String migrationStatus) {
        return ciMigrationMapper.selectTempList(validStatus, migrationStatus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int validateStagingRecords() {
        log.info("[9/30 이기종 CI 데이터 검증 시작]");
        List<CiMigrationDto.TempRecord> pendingList = ciMigrationMapper.selectTempList(null, null);
        int validCount = 0;

        Set<String> seenCis = new HashSet<>();

        for (CiMigrationDto.TempRecord temp : pendingList) {
            String ci = temp.getCiVal();

            // 1. 길이 및 Base64 패턴 검증
            if (!StringUtils.hasText(ci) || ci.length() != 88) {
                ciMigrationMapper.updateTempValidStatus(temp.getTempSeq(), "INVALID_LENGTH", 
                        "CI 길이가 88자가 아닙니다 (현재 길이: " + (ci != null ? ci.length() : 0) + ")");
                continue;
            }

            if (!BASE64_PATTERN.matcher(ci).matches()) {
                ciMigrationMapper.updateTempValidStatus(temp.getTempSeq(), "INVALID_BASE64", 
                        "Base64 표준 인코딩 형식이 아닙니다.");
                continue;
            }

            // 2. 스테이징 내부 중복 검출
            if (seenCis.contains(ci)) {
                ciMigrationMapper.updateTempValidStatus(temp.getTempSeq(), "DUPLICATE_CI", 
                        "이기종 추출 대상 내 중복된 CI 값입니다.");
                continue;
            }
            seenCis.add(ci);

            // 3. 기등록 고객 CI 검출
            int existingCount = ciMigrationMapper.countExistingCustCi(ci);
            if (existingCount > 0) {
                ciMigrationMapper.updateTempValidStatus(temp.getTempSeq(), "DUPLICATE_CI", 
                        "운영 TB_CUST_CI에 이미 등록된 CI 값입니다.");
                continue;
            }

            // 검증 통과
            ciMigrationMapper.updateTempValidStatus(temp.getTempSeq(), "VALID", null);
            validCount++;
        }

        log.info("[9/30 이기종 CI 데이터 검증 완료] 총 {}건 중 유효건수: {}건", pendingList.size(), validCount);
        return validCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CiMigrationDto.ExecutionResult executeMigration(String execBy) {
        long startTime = System.currentTimeMillis();
        String batchId = "MIG_BATCH_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        log.info("[9/30 이기종 CI 마이그레이션 실행] BatchID={}, 실행자={}", batchId, execBy);

        // 1. 먼저 정합성 검증 수행
        validateStagingRecords();

        // 2. 검증 완료된 대상 조회 (VALID && READY)
        List<CiMigrationDto.TempRecord> targetList = ciMigrationMapper.selectTempList(null, null);

        int totalCount = targetList.size();
        int validCount = 0;
        int successCount = 0;
        int duplicateCount = 0;
        int failCount = 0;

        // 3. 히스토리 생성 (RUNNING)
        CiMigrationDto.HistoryRecord hist = CiMigrationDto.HistoryRecord.builder()
                .batchId(batchId)
                .totalCount(totalCount)
                .status("RUNNING")
                .execBy(StringUtils.hasText(execBy) ? execBy : "SYSTEM_ADMIN")
                .build();
        ciMigrationMapper.insertHistory(hist);

        // 4. 이관 루프
        for (CiMigrationDto.TempRecord record : targetList) {
            if ("VALID".equals(record.getValidStatus())) {
                validCount++;
                try {
                    CustCiDto custCi = CustCiDto.builder()
                            .ciVal(record.getCiVal())
                            .diVal(record.getDiVal())
                            .custId(record.getLegacyCustId())
                            .custNm(record.getCustNm())
                            .birthDate(record.getBirthDate())
                            .gender(record.getGender())
                            .nationalInfo(record.getNationalInfo())
                            .cellNo(record.getCellNo())
                            .build();

                    niceAuthMapper.insertOrUpdateCustCi(custCi);
                    ciMigrationMapper.updateTempMigrationStatus(record.getTempSeq(), "COMPLETED");
                    successCount++;
                } catch (Exception e) {
                    log.error("마이그레이션 적재 오류: TEMP_SEQ={}", record.getTempSeq(), e);
                    ciMigrationMapper.updateTempMigrationStatus(record.getTempSeq(), "FAILED");
                    failCount++;
                }
            } else if ("DUPLICATE_CI".equals(record.getValidStatus())) {
                duplicateCount++;
            } else {
                failCount++;
            }
        }

        long duration = System.currentTimeMillis() - startTime;

        // 5. 히스토리 완료 업데이트
        hist.setTotalCount(totalCount);
        hist.setSuccessCount(successCount);
        hist.setDuplicateCount(duplicateCount);
        hist.setFailCount(failCount);
        hist.setStatus("COMPLETED");
        ciMigrationMapper.updateHistory(hist);

        String message = String.format("총 %d건 중 성공 %d건, 중복제외 %d건, 오류 %d건 이관 완료 (%d ms 소요)",
                totalCount, successCount, duplicateCount, failCount, duration);
        log.info("[이관 결과] {}", message);

        return CiMigrationDto.ExecutionResult.builder()
                .batchId(batchId)
                .totalCount(totalCount)
                .validCount(validCount)
                .successCount(successCount)
                .duplicateCount(duplicateCount)
                .failCount(failCount)
                .status("COMPLETED")
                .startDt(LocalDateTime.now())
                .endDt(LocalDateTime.now())
                .durationMs(duration)
                .message(message)
                .build();
    }

    @Override
    public List<CiMigrationDto.HistoryRecord> getMigrationHistories() {
        return ciMigrationMapper.selectHistoryList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetSampleData() {
        ciMigrationMapper.clearTempRecords();
        ciMigrationMapper.insertSampleTempData();
        log.info("[9/30 이기종 CI 샘플 데이터 재적재 완료]");
    }
}
