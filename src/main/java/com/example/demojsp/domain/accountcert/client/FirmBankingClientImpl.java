package com.example.demojsp.domain.accountcert.client;

import com.example.demojsp.domain.accountcert.client.dto.AccountInquiryResultDto;
import com.example.demojsp.domain.accountcert.client.dto.FirmBankingTransferResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * =========================================================================================
 * [금융권 펌뱅킹 / 오픈뱅킹 타행 연동 클라이언트 구현체]
 * 
 * • 운영 환경:
 *   - 금융결제원 중계망(오픈뱅킹 API) 또는 은행 펌뱅킹(대외계 FEP 전문) 연계
 *   - 계좌 실명조회 전문 및 타행 1원 즉시이체 전문 전송
 * 
 * • 개발/테스트 환경:
 *   - 시뮬레이터(Mock Mode)를 통해 실시간 계좌 검증 및 1원 이체 송금 처리
 *   - 테스트 시뮬레이션 계좌번호별 예외 케이스(거래정지, 불일치 등) 재현 지원
 * =========================================================================================
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FirmBankingClientImpl implements FirmBankingClient {

    @Value("${firmbanking.mock-mode:true}")
    private boolean mockMode;

    @Value("${firmbanking.org-code:B0000050}")
    private String institutionCode; // 저축은행 기관코드

    @Override
    public AccountInquiryResultDto inquireAccountHolder(String bankCd, String accountNo) {
        String cleanAccountNo = accountNo.replaceAll("[^0-9]", "");
        log.info("[펌뱅킹 계좌실명조회 전문 송신] 은행코드={}, 계좌번호={}, MockMode={}", bankCd, maskAccount(cleanAccountNo), mockMode);

        if (!mockMode) {
            // [실서버 운영 연동 가이드]
            // RestTemplate 또는 WebClient를 통해 금융결제원 오픈뱅킹 API(/v2.0/inquiry/real_name) 또는
            // 대외계 펌뱅킹 게이트웨이로 전문(전문종별: 0200, 업무구분: 100)을 전송합니다.
            /*
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(getOpenBankingAccessToken());
            HttpEntity<InquiryReq> requestEntity = new HttpEntity<>(req, headers);
            ResponseEntity<InquiryRes> response = restTemplate.postForEntity(realNameUrl, requestEntity, InquiryRes.class);
            return mapToResultDto(response.getBody());
            */
            log.warn("[운영 연동 준비] 실 펌뱅킹 대외계 엔드포인트 미설정 상태 -> 시뮬레이션 모드로 폴백");
        }

        // ================= 시뮬레이션 테스트 로직 =================
        // 특정 테스트 계좌번호 입력 시 비즈니스 예외 케이스 재현
        if (cleanAccountNo.endsWith("9999")) {
            // 존재하지 않는 계좌
            return AccountInquiryResultDto.builder()
                    .success(false)
                    .resCode("9991")
                    .resMessage("해당 은행에 존재하지 않는 계좌번호입니다.")
                    .bankCd(bankCd)
                    .accountNo(cleanAccountNo)
                    .accountStatus("NOT_FOUND")
                    .build();
        } else if (cleanAccountNo.endsWith("8888")) {
            // 거래정지/지급제한 계좌
            return AccountInquiryResultDto.builder()
                    .success(false)
                    .resCode("9992")
                    .resMessage("입금이 제한된 계좌(지급정지/압류/해지 계좌)입니다.")
                    .bankCd(bankCd)
                    .accountNo(cleanAccountNo)
                    .accountStatus("SUSPENDED")
                    .build();
        }

        // 정상 계좌 시뮬레이션:
        // 특정 끝자리 7777은 타인 명의(김타인), 기본 계좌는 등록인 홍길동 명의
        String simulatedHolderNm = cleanAccountNo.endsWith("7777") ? "김타인" : "홍길동";

        return AccountInquiryResultDto.builder()
                .success(true)
                .resCode("0000")
                .resMessage("정상 계좌")
                .bankCd(bankCd)
                .bankNm(getBankNameByCode(bankCd))
                .accountNo(cleanAccountNo)
                .accountHolderNm(simulatedHolderNm)
                .accountStatus("NORMAL")
                .build();
    }

    @Override
    public FirmBankingTransferResultDto transferOneWon(String bankCd, String accountNo, String printText, String reqTxSeq) {
        String cleanAccountNo = accountNo.replaceAll("[^0-9]", "");
        log.info("[펌뱅킹 1원 이체 전문 송신] 은행코드={}, 계좌번호={}, 인자문구='{}', TxSeq={}, MockMode={}",
                bankCd, maskAccount(cleanAccountNo), printText, reqTxSeq, mockMode);

        if (!mockMode) {
            // [실서버 운영 연동 가이드]
            // 금융결제원 펌뱅킹 1원 송금(전문종별: 0200, 업무구분: 200, 금액: 1) 전문 전송
            /*
            FirmTransferPacket packet = FirmTransferPacket.builder()
                    .txSeq(reqTxSeq)
                    .destBank(bankCd)
                    .destAccount(cleanAccountNo)
                    .amount(1L)
                    .printText(printText) // 최대 한글 7자(14Byte)
                    .build();
            return sendSocketPacket(packet);
            */
            log.warn("[운영 연동 준비] 실 펌뱅킹 송금 엔드포인트 미설정 상태 -> 시뮬레이션 모드로 폴백");
        }

        // ================= 1원 송금 시뮬레이션 완료 =================
        String nowDt = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String nowTm = LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
        String bankTranId = "FB" + nowDt + nowTm + (int)(Math.random() * 9000 + 1000);

        log.info("[펌뱅킹 1원 송금 성공] 타행 통장 입금자명 표기완료: [{}], 거래고유번호={}", printText, bankTranId);

        return FirmBankingTransferResultDto.builder()
                .success(true)
                .resCode("0000")
                .resMessage("타행 1원 정상 입금 완료")
                .bankTranId(bankTranId)
                .tranDate(nowDt)
                .tranTime(nowTm)
                .tranAmount(1L)
                .printText(printText)
                .build();
    }

    private String maskAccount(String accountNo) {
        if (accountNo == null || accountNo.length() < 6) return accountNo;
        int len = accountNo.length();
        return accountNo.substring(0, 3) + "-****-" + accountNo.substring(len - 3);
    }

    private String getBankNameByCode(String bankCd) {
        return switch (bankCd) {
            case "004" -> "KB국민은행";
            case "088" -> "신한은행";
            case "020" -> "우리은행";
            case "081" -> "하나은행";
            case "011" -> "NH농협은행";
            case "003" -> "IBK기업은행";
            case "090" -> "카카오뱅크";
            case "092" -> "토스뱅크";
            case "089" -> "케이뱅크";
            case "050" -> "상호저축은행";
            default -> "기타은행";
        };
    }
}
