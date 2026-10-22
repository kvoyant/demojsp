package com.example.demojsp.common.platformlink.adapter;

import com.example.demojsp.common.platformlink.model.LimitInquiry;
import com.example.demojsp.common.platformlink.model.LinkOutcome;

/**
 * 제휴사 전문 ↔ 공통모델 변환기 (CF-TC-07 "기관별 Adapter").
 *
 * <pre>
 *   제휴사 요청 전문 ──parse()──▶ LimitInquiry(공통모델) ──▶ 업무 공통 로직 (제휴사를 모름)
 *   제휴사 응답 전문 ◀─response()── LinkOutcome(결과, 이미 제휴사 코드로 변환됨)
 * </pre>
 * 제휴사를 추가할 때 이 인터페이스 구현체 1개 + DB(TB_LNK_PARTNER 등) 행만 추가한다. 서비스 코드는 바꾸지 않는다.
 * 전문 형식이 같은 제휴사는 같은 어댑터를 쓴다 (TB_LNK_PARTNER.ADAPTER_CD).
 */
public interface PartnerAdapter {

    /** TB_LNK_PARTNER.ADAPTER_CD 와 같은 값 (예: TOSS_V1) */
    String adapterCd();

    /** 샘플·관리 화면 표시용 한 줄 설명 */
    String description();

    /**
     * 요청 전문 → 공통모델. 형식이 깨졌거나 필수값이 없으면 {@link AdapterParseException}.
     * 단위·코드 변환(만원→원, 날짜 형식, 직업코드)은 여기서 끝낸다.
     */
    LimitInquiry parse(String body);

    /** 응답 전문 만들기 (승인·거절·오류 모두) */
    String response(LinkOutcome outcome);

    /** [샘플 화면] 이 제휴사 형식의 요청 전문 예시를 만든다 */
    String sampleRequest(SampleInput in);

    /** 샘플 전문 입력값 (원 단위·yyyyMMdd·당행 코드) */
    record SampleInput(String reqNo, String ci, String name, String birth, long annualIncome, String employType,
                       String companyNm, String vehicleNo, String collateralAddr, String product, long requestAmt) {
    }
}
