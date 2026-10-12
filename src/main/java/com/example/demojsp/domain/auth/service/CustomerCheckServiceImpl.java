package com.example.demojsp.domain.auth.service;

import com.example.demojsp.domain.auth.dto.CustomerRiskCheckDto;
import com.example.demojsp.domain.auth.dto.WatchlistDto;
import com.example.demojsp.domain.auth.mapper.NiceAuthMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerCheckServiceImpl implements CustomerCheckService {

    private final NiceAuthMapper niceAuthMapper;

    @Override
    public CustomerRiskCheckDto checkCustomerRisk(String ciVal, String birthDate, String nationalInfo, String custNm) {
        log.info("[고객체크] CI={}, 생년월일={}, 내외국인={}, 고객명={}", 
                (ciVal != null && ciVal.length() > 10 ? ciVal.substring(0, 10) + "..." : ciVal), 
                birthDate, nationalInfo, custNm);

        CustomerRiskCheckDto.CustomerRiskCheckDtoBuilder builder = CustomerRiskCheckDto.builder();
        boolean isPass = true;
        StringBuilder summary = new StringBuilder();

        // 1. 유의고객 체크 (CI 기준)
        WatchlistDto watchlistDto = findWatchlistByCi(ciVal);
        if (watchlistDto != null) {
            builder.watchlist(true)
                   .riskGrade(watchlistDto.getRiskGrade())
                   .riskType(watchlistDto.getRiskType())
                   .riskReason(watchlistDto.getRiskReason())
                   .blocked("Y".equalsIgnoreCase(watchlistDto.getBlockYn()));

            if ("Y".equalsIgnoreCase(watchlistDto.getBlockYn())) {
                isPass = false;
                summary.append("[유의고객 차단] ").append(watchlistDto.getRiskReason()).append(" ");
            }
        } else {
            builder.watchlist(false).blocked(false);
        }

        // 2. 미성년자 체크 (만 19세 미만)
        int exactAge = calculateExactAge(birthDate);
        builder.exactAge(exactAge);
        if (exactAge < 19) {
            builder.minor(true)
                   .minorGuideMessage("만 19세 미만 미성년자(만 " + exactAge + "세)는 비여신/여신 금융거래 규정상 법정대리인 동의 또는 영업점 서면 접수가 필요합니다.");
            summary.append("[미성년자 주의: 만 ").append(exactAge).append("세] ");
        } else {
            builder.minor(false);
        }

        // 3. 외국인 체크 (nationalInfo: 1-내국인, 2-외국인)
        if ("2".equals(nationalInfo)) {
            builder.foreigner(true)
                   .foreignerGuideMessage("외국인 고객은 실명확인증표(외국인등록증) 진위확인 및 체류자격 심사 등 추가 절차가 진행됩니다.");
            summary.append("[외국인 고객] ");
        } else {
            builder.foreigner(false);
        }

        if (summary.length() == 0) {
            summary.append("정상 고객 (유의고객 미해당, 성인 내국인)");
        }

        builder.pass(isPass)
               .checkSummaryMessage(summary.toString().trim());

        return builder.build();
    }

    @Override
    public WatchlistDto findWatchlistByCi(String ciVal) {
        if (!StringUtils.hasText(ciVal)) {
            return null;
        }
        return niceAuthMapper.selectWatchlistByCi(ciVal);
    }

    @Override
    public int calculateExactAge(String birthDate) {
        if (!StringUtils.hasText(birthDate) || birthDate.length() != 8) {
            return 30; // 기본 성인 가정
        }

        try {
            LocalDate birth = LocalDate.parse(birthDate, DateTimeFormatter.ofPattern("yyyyMMdd"));
            LocalDate today = LocalDate.now();
            return (int) ChronoUnit.YEARS.between(birth, today);
        } catch (Exception e) {
            log.warn("생년월일 파싱 오류: {}", birthDate, e);
            return 30;
        }
    }
}
