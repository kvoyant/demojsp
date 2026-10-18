package com.example.demojsp.common.product.domain;

/**
 * 상품 구분 - ADMIN 수신상품목록 / 여신상품목록.
 * 입출금(파킹통장 등)은 수신상품목록에서 관리한다.
 */
public enum ProductKind {

    DEPOSIT("수신"),
    LOAN("여신");

    private final String label;

    ProductKind(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
