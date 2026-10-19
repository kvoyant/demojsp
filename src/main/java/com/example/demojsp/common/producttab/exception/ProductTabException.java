package com.example.demojsp.common.producttab.exception;

/** 상품 탭 예외 - ProductTabExceptionHandler 가 ApiResponse(success=false, code, message) 로 바꿔 준다. */
public class ProductTabException extends RuntimeException {

    private final ProductTabErrorCode errorCode;

    public ProductTabException(ProductTabErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /** 화면에 보여줄 문구를 구체적으로 바꿀 때 (예: "메인 문구는 30자 이내로 입력하세요.") */
    public ProductTabException(ProductTabErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ProductTabErrorCode getErrorCode() { return errorCode; }
}
