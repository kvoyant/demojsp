package com.example.demojsp.common.savings.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * [공통#51] 저축 탭 메인 콘텐츠 설정 ({@code common.savings.main.*}).
 *
 * <p>문구는 요구사항 확정 문구("매일 쌓이는 이자 / 모으는 재미가 쏠쏠")를 기본값으로 두고, 바뀌면 yaml 만 수정한다.
 * [계좌 개설하기] 이동 URL 은 상품 원장(공통#47·48)의 입출금통장 상품안내페이지를 따른다 → ADMIN 이 URL 을 바꾸면 자동 반영.
 */
@ConfigurationProperties(prefix = "common.savings")
public class SavingsProperties {

    private Main main = new Main();

    public Main getMain() { return main; }
    public void setMain(Main main) { this.main = main; }

    public static class Main {
        /** 텍스트 (줄 단위, 기능2) */
        private List<String> lines = new ArrayList<>(List.of("매일 쌓이는 이자", "모으는 재미가 쏠쏠"));
        private String imageUrl = "/images/common/savings/main-coins.svg";
        /** 이미지 대체텍스트 (장식이 아니라 내용 전달용) */
        private String imageAlt = "통장에 동전이 쌓이는 그림";
        /** 버튼 문구 (기능3) */
        private String ctaText = "계좌 개설하기";
        /** 입출금통장 상품코드 → 상품 원장의 상품안내페이지 URL 사용 */
        private String productCode = "PS_PARKING4";
        /** 상품 원장에 없거나 판매중지일 때 이동 URL */
        private String fallbackUrl = "/product/deposit";

        public List<String> getLines() { return lines; }
        public void setLines(List<String> lines) { this.lines = lines; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public String getImageAlt() { return imageAlt; }
        public void setImageAlt(String imageAlt) { this.imageAlt = imageAlt; }
        public String getCtaText() { return ctaText; }
        public void setCtaText(String ctaText) { this.ctaText = ctaText; }
        public String getProductCode() { return productCode; }
        public void setProductCode(String productCode) { this.productCode = productCode; }
        public String getFallbackUrl() { return fallbackUrl; }
        public void setFallbackUrl(String fallbackUrl) { this.fallbackUrl = fallbackUrl; }
    }
}
