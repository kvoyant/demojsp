package com.example.demojsp.common.acctverify.gateway;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * 펌뱅킹 시뮬레이터 (local / 개발 전용). 실제 송금은 하지 않는다.
 *
 * <p>계좌번호 끝자리로 상황을 흉내 낸다 (테스트·시연용 규칙):
 * <ul>
 *   <li>…0000 : 없는 계좌</li>
 *   <li>…9999 : 다른 사람 명의 (예금주 "김타인")</li>
 *   <li>…8888 : 은행 점검 중 - 1원 송금 실패</li>
 *   <li>그 외   : 고객 본인 명의 계좌</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "common.acct-verify.gateway", havingValue = "mock", matchIfMissing = true)
public class MockFirmBankingGateway implements FirmBankingGateway {

    private final SecureRandom random = new SecureRandom();

    @Override
    public OwnerCheck checkOwner(String bankCd, String acctNo, String custNm) {
        if (acctNo.endsWith("0000")) return new OwnerCheck(false, null);
        if (acctNo.endsWith("9999")) return new OwnerCheck(true, "김타인");
        return new OwnerCheck(true, custNm);
    }

    @Override
    public String transferOneWon(String bankCd, String acctNo, String memo) {
        if (acctNo.endsWith("8888")) throw new FirmBankingException("수취 은행 점검 시간입니다.");
        byte[] b = new byte[6];
        random.nextBytes(b);
        return "FB-" + HexFormat.of().formatHex(b);
    }
}
