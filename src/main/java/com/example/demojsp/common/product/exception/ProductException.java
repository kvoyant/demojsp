package com.example.demojsp.common.product.exception;

/** 상품 공통 예외 */
public class ProductException extends RuntimeException {

    private final ProductErrorCode errorCode;
    private final String field;

    public ProductException(ProductErrorCode errorCode) {
        this(errorCode, null);
    }

    public ProductException(ProductErrorCode errorCode, String field) {
        this(errorCode, field, errorCode.getMessage());
    }

    /** 메시지에 값이 들어가는 경우 (예: "등록 건수 3개를 초과하였습니다.") */
    public static ProductException format(ProductErrorCode errorCode, Object... args) {
        return new ProductException(errorCode, null, String.format(errorCode.getMessage(), args));
    }

    private final String message;

    private ProductException(ProductErrorCode errorCode, String field, String message) {
        super(errorCode.getCode() + " " + message + (field == null ? "" : " (" + field + ")"));
        this.errorCode = errorCode;
        this.field = field;
        this.message = message;
    }

    public ProductErrorCode getErrorCode() { return errorCode; }

    public String displayMessage() {
        return field == null ? message : message + " [" + field + "]";
    }
}
