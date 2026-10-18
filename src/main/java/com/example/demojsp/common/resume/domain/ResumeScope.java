package com.example.demojsp.common.resume.domain;

/**
 * 이어하기 조회 범위 (화면 위치). 같은 API · 같은 카드 컴포넌트를 scope 만 바꿔 재사용한다 (CF-BC-13 구현 방안).
 *
 * <ul>
 *   <li>{@link #ALL} - 홈 탭 (공통#43): 수신 + 여신 전체</li>
 *   <li>{@link #DEPOSIT} - 저축 탭 (공통#49): 수신상품만</li>
 *   <li>{@link #LOAN} - 대출 탭 (공통#60): 여신상품만</li>
 * </ul>
 */
public enum ResumeScope {

    ALL("전체(홈)", null),
    DEPOSIT("수신(저축)", ProductType.DEPOSIT),
    LOAN("여신(대출)", ProductType.LOAN);

    private final String label;
    private final ProductType productType;

    ResumeScope(String label, ProductType productType) {
        this.label = label;
        this.productType = productType;
    }

    public String getLabel() { return label; }

    /** 이 범위에 포함되는 상품인지 */
    public boolean includes(ProductType type) {
        return productType == null || productType == type;
    }
}
