package com.example.demojsp.common.platformlink.model;

/**
 * 한도조회 <b>공통모델</b> - 제휴사 전문을 어댑터가 이 모양으로 바꾼다. 이후 업무 로직은 제휴사를 모른다.
 *
 * <p>단위·형식은 여기서 통일한다: 금액은 원, 생년월일은 yyyyMMdd, 직업구분은 당행 코드.
 * (카카오페이처럼 만원 단위로 보내는 제휴사는 어댑터에서 원으로 바꾼다)
 *
 * @param partnerReqNo   제휴사 요청번호 - 중복 요청 판단 키
 * @param ci             CI (본인확인기관 연계정보, 88자)
 * @param custNm         고객명
 * @param birth          생년월일 yyyyMMdd
 * @param annualIncome   연소득 (원)
 * @param employType     직업구분: EMPLOYEE 직장인 / FREELANCER 프리랜서 / BUSINESS 사업자
 * @param companyNm      직장명 (선택)
 * @param vehicleNo      차량번호 (있으면 오토)
 * @param collateralAddr 담보 부동산 주소 (있으면 모기지)
 * @param productHint    제휴사가 상품유형을 직접 보낸 경우 (CREDIT/AUTO/MORTGAGE), 없으면 null
 * @param requestAmt     희망금액 (원, 선택)
 */
public record LimitInquiry(String partnerReqNo, String ci, String custNm, String birth, long annualIncome, String employType,
                           String companyNm, String vehicleNo, String collateralAddr, String productHint, long requestAmt) {
}
