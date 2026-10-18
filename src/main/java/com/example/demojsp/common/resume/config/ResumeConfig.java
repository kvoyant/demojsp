package com.example.demojsp.common.resume.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * [CF-BC-13] 이어하기 공통모듈 설정 등록.
 *
 * <p>Clock 빈을 따로 만들지 않는다 (다른 공통모듈과 동일) → 서비스가 테스트용 생성자로 Clock 을 받는다.
 */
@Configuration
@EnableConfigurationProperties(ResumeProperties.class)
public class ResumeConfig {
}
