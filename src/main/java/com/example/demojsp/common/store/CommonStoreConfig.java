package com.example.demojsp.common.store;

import com.example.demojsp.common.store.mapper.CommonSequenceMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 공통모듈 저장소 설정. {@code common.store.type=db}(기본)일 때 채번·스키마/seed 초기화 빈을 등록한다.
 * 각 모듈의 저장소 구현은 같은 속성으로 MyBatis / 메모리 중 하나만 등록된다 (@ConditionalOnProperty).
 */
@Configuration
@EnableConfigurationProperties(CommonStoreProperties.class)
public class CommonStoreConfig {

    @Bean
    @ConditionalOnProperty(name = "common.store.type", havingValue = "db", matchIfMissing = true)
    public CommonSequence commonSequence(CommonSequenceMapper mapper, PlatformTransactionManager transactionManager) {
        return new CommonSequence(mapper, transactionManager);
    }
}
