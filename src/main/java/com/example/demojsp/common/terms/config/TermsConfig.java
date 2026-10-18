package com.example.demojsp.common.terms.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * [CF-BC-04] 약관동의 공통모듈 설정 등록.
 *
 * <p>Clock 빈을 따로 만들지 않는다. (identityClock 과 Clock 타입 빈이 2개가 되면
 * 한정자 없이 Clock 을 주입받는 기존/향후 코드가 깨질 수 있음) → 각 클래스가 테스트용 생성자로 Clock 을 받는다.
 */
@Configuration
@EnableConfigurationProperties(TermsProperties.class)
public class TermsConfig {
}
