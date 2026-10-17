package com.example.demojsp.common.identity.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

import java.time.Clock;

/**
 * 본인인증 공통모듈 빈 설정.
 *
 * <p>DemojspApplication 의 컴포넌트 스캔(com.example.demojsp) 범위 안에 있으므로 별도 등록 없이 동작한다.
 *
 * <p>빈 이름은 모두 {@code identity*} 접두어를 붙여 기존 빈(niceApiClientImpl 등)과 충돌하지 않게 했다.
 */
@Configuration
@EnableScheduling   // InMemoryIdentityTxStore 만료 거래 정리용
@EnableConfigurationProperties(IdentityProperties.class)
public class IdentityConfig {

    /**
     * NICE API 호출 전용 RestClient (Spring 7).
     * <p>타임아웃을 반드시 지정한다. (미지정 시 인증기관 장애가 WAS 스레드 고갈로 번질 수 있음)
     */
    @Bean
    public RestClient identityNiceRestClient(IdentityProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.getNice().getConnectTimeout());
        factory.setReadTimeout(props.getNice().getReadTimeout());
        return RestClient.builder()
                .baseUrl(props.getNice().getApiBaseUrl())
                .requestFactory(factory)
                .build();
    }

    /** 시간 의존 로직(만료 검증) 테스트를 위해 Clock 을 빈으로 주입 */
    @Bean
    public Clock identityClock() {
        return Clock.systemDefaultZone();
    }
}
