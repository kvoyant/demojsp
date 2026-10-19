package com.example.demojsp.common.acctverify.gateway;

/**
 * 펌뱅킹 / 오픈뱅킹 연동부 (외부 시스템 경계). 서비스는 이 인터페이스만 안다.
 *
 * <pre>
 *  local : MockFirmBankingGateway (common.acct-verify.gateway=mock, 기본) - 실제 송금 없음
 *  운영  : 펌뱅킹(또는 오픈뱅킹 수취조회·입금이체) 실연동 구현체로 교체 (기존 domain/accountcert/client/FirmBankingClientImpl 참고)
 * </pre>
 */
public interface FirmBankingGateway {

    /**
     * 예금주 확인 (수취조회 / 실명조회). 계좌가 없으면 exists=false.
     *
     * @param custNm 고객 실명 - 실명조회형 API 는 일치 여부를 돌려주고, 수취조회형 API 는 무시하고 예금주명만 돌려준다
     */
    OwnerCheck checkOwner(String bankCd, String acctNo, String custNm);

    /** 1원 입금 (입금자명 = memo, 예: "페퍼3821") → 거래번호. 실패하면 FirmBankingException */
    String transferOneWon(String bankCd, String acctNo, String memo);

    record OwnerCheck(boolean exists, String holderNm) {
    }
}
