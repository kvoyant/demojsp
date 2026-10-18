package com.example.demojsp.common.product.exception;

/** 상품 공통 예외 */
public class ProductException extends RuntimeException {

    private final ProductErrorCode errorCode;
    private final String field;

    public ProductException(ProductErrorCode errorCode) {
        this(errorCode, null);
    }

    public ProductException(ProductErrorCode errorCode, String field) {
        super(errorCode.getCode() + " " + errorCode.getMessage() + (field == null ? "" : " (" + field + ")"));
        this.errorCode = errorCode;
        this.field = field;
    }

    public ProductErrorCode getErrorCode() { return errorCode; }

    public String displayMessage() {
        return field == null ? errorCode.getMessage() : errorCode.getMessage() + " [" + field + "]";
    }
}
