package com.example.demojsp.common.savings.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** [공통#51] 저축 탭 설정 등록 */
@Configuration
@EnableConfigurationProperties(SavingsProperties.class)
public class SavingsConfig {
}
