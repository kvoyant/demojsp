package com.example.demojsp.common.banner.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * [CF-BC-20] 배너팝업 공통모듈 설정 등록.
 *
 * <p>Clock 빈을 따로 만들지 않는다 (약관동의 모듈과 동일 이유) → 서비스가 테스트용 생성자로 Clock 을 받는다.
 */
@Configuration
@EnableConfigurationProperties(BannerPopupProperties.class)
public class BannerPopupConfig {
}
