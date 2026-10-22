package com.example.demojsp.common.platformlink.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@EnableConfigurationProperties(PlatformLinkProperties.class)
public class PlatformLinkConfig {

    /**
     * CSS 호출 전용 스레드 풀.
     * 요청 스레드가 CSS 응답을 무한정 기다리지 않도록, 별도 스레드에서 호출하고 Future.get(제한시간) 으로 끊는다.
     */
    @Bean(name = "platformLinkCssExecutor", destroyMethod = "shutdownNow")
    public ExecutorService platformLinkCssExecutor(PlatformLinkProperties props) {
        AtomicInteger n = new AtomicInteger();
        return Executors.newFixedThreadPool(props.getCssPoolSize(), r -> {
            Thread t = new Thread(r, "plink-css-" + n.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
    }
}
