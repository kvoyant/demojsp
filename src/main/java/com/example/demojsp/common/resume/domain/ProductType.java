package com.example.demojsp.common.resume.domain;

/**
 * 이어하기 대상 상품 구분.
 *
 * <p>홈 이어하기(공통#43)는 전체, 저축 이어하기(공통#49)는 수신만, 대출 이어하기(공통#60)는 여신만 노출한다.
 */
public enum ProductType {

    DEPOSIT("수신"),
    LOAN("여신");

    private final String label;

    ProductType(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
