package com.example.demojsp.common.product.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** [공통#47·48] 상품 설정 등록 */
@Configuration
@EnableConfigurationProperties(ProductProperties.class)
public class ProductConfig {
}
