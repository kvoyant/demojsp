package com.example.demojsp.domain.auth.controller;

import com.example.demojsp.common.response.ApiResponse;
import com.example.demojsp.domain.auth.dto.*;
import com.example.demojsp.domain.auth.service.CiMigrationService;
import com.example.demojsp.domain.auth.service.NiceCertService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/common/auth")
@RequiredArgsConstructor
public class NiceCertApiController {

    private final NiceCertService niceCertService;
    private final CiMigrationService ciMigrationService;

    // -------------------------------------------------------------
    // 1. NICE ID 본인인증 API (통신사 미지정 & MO 기기점유)
    // -------------------------------------------------------------

    /**
     * 비여신금융거래 본인확인 약관 목록 조회
     */
    @GetMapping("/nice/terms")
    public ApiResponse<List<TermsDto>> getActiveTermsList() {
        List<TermsDto> termsList = niceCertService.getActiveTermsList();
        return ApiResponse.success(termsList);
    }

    /**
     * 통신사 미지정 방식 본인인증 번호(OTP) 발송 요청
     * (성명 + 생년월일 + 휴대폰번호만 입력, 통신사 선택 불필요)
     */
    @PostMapping("/nice/request-otp")
    public ApiResponse<Map<String, Object>> requestOtp(@RequestBody NiceCertReqDto reqDto,
                                                       HttpServletRequest request) {
        String clientIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        Map<String, Object> result = niceCertService.requestOtp(reqDto, clientIp, userAgent);
        return ApiResponse.success("본인인증 번호가 전송되었습니다.", result);
    }

    /**
     * 본인인증 번호(OTP) 검증 및 CI/DI 획득 & 유의고객/미성년자/외국인 리스크 체크
     */
    @PostMapping("/nice/verify-otp")
    public ApiResponse<NiceCertResultDto> verifyOtp(@RequestBody NiceCertVerifyDto verifyDto) {
        NiceCertResultDto result = niceCertService.verifyOtp(verifyDto);
        return ApiResponse.success("본인인증이 완료되었습니다.", result);
    }

    /**
     * MO(기기 점유) 인증 세션 생성 요청
     * (휴대폰번호 입력 생략 가능 및 단말 소지 확인)
     */
    @PostMapping("/nice/request-mo")
    public ApiResponse<Map<String, Object>> requestMoSession(@RequestBody NiceCertMoReqDto moReqDto,
                                                            HttpServletRequest request) {
        String clientIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        Map<String, Object> result = niceCertService.requestMoSession(moReqDto, clientIp, userAgent);
        return ApiResponse.success("MO 기기점유 인증 요청이 완료되었습니다.", result);
    }

    /**
     * MO 인증 문자 수신 상태 확인 및 인증 완료 처리
     */
    @GetMapping("/nice/check-mo-status")
    public ApiResponse<NiceCertResultDto> checkMoStatus(@RequestParam("reqTxSeq") String reqTxSeq) {
        NiceCertResultDto result = niceCertService.checkMoStatus(reqTxSeq);
        return ApiResponse.success("기기 점유 MO 본인인증이 완료되었습니다.", result);
    }

    // -------------------------------------------------------------
    // 2. 9/30 이기종 CI Data 이관 관리 API
    // -------------------------------------------------------------

    /**
     * 이기종 CI 임시(스테이징) 데이터 목록 조회
     */
    @GetMapping("/ci-migration/list")
    public ApiResponse<List<CiMigrationDto.TempRecord>> getMigrationTempList(
            @RequestParam(value = "validStatus", required = false) String validStatus,
            @RequestParam(value = "migrationStatus", required = false) String migrationStatus) {
        List<CiMigrationDto.TempRecord> list = ciMigrationService.getTempRecords(validStatus, migrationStatus);
        return ApiResponse.success(list);
    }

    /**
     * 이기종 CI 데이터 사전 정합성 검증 (88자, Base64, 중복)
     */
    @PostMapping("/ci-migration/validate")
    public ApiResponse<Map<String, Object>> validateMigrationRecords() {
        int validCount = ciMigrationService.validateStagingRecords();
        return ApiResponse.success("CI 데이터 검증이 완료되었습니다.", Map.of("validCount", validCount));
    }

    /**
     * 9/30 이기종 CI 마이그레이션 실행
     */
    @PostMapping("/ci-migration/execute")
    public ApiResponse<CiMigrationDto.ExecutionResult> executeMigration(
            @RequestParam(value = "execBy", required = false, defaultValue = "ADMIN") String execBy) {
        CiMigrationDto.ExecutionResult result = ciMigrationService.executeMigration(execBy);
        return ApiResponse.success(result.getMessage(), result);
    }

    /**
     * 이관 실행 이력 목록 조회
     */
    @GetMapping("/ci-migration/history")
    public ApiResponse<List<CiMigrationDto.HistoryRecord>> getMigrationHistories() {
        List<CiMigrationDto.HistoryRecord> list = ciMigrationService.getMigrationHistories();
        return ApiResponse.success(list);
    }

    /**
     * 테스트용 9/30 샘플 데이터 재적재
     */
    @PostMapping("/ci-migration/reset")
    public ApiResponse<Void> resetSampleData() {
        ciMigrationService.resetSampleData();
        return ApiResponse.success("샘플 데이터가 초기화되었습니다.", null);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
