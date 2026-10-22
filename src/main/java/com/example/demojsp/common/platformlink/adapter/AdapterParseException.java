package com.example.demojsp.common.platformlink.adapter;

/** 요청 전문을 공통모델로 바꿀 수 없음 → 제휴사에 INVALID 결과코드로 응답한다 */
public class AdapterParseException extends RuntimeException {

    /** 형식이 깨져도 요청번호는 읽힐 수 있으면 응답에 되돌려 준다 */
    private final String partnerReqNo;

    public AdapterParseException(String message, String partnerReqNo) {
        super(message);
        this.partnerReqNo = partnerReqNo;
    }

    public String getPartnerReqNo() { return partnerReqNo; }
}
