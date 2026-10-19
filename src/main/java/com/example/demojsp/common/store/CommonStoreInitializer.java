package com.example.demojsp.common.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * DB 저장소 기동 초기화: ① 공통 테이블 생성(없을 때만) ② 비어 있는 테이블에 seed 적재.
 *
 * <p>DB 에 연결할 수 없어도 애플리케이션 기동은 막지 않는다 (경고 로그) - 공통모듈 화면만 실패하고 다른 업무는 동작.
 */
@Component
@ConditionalOnProperty(name = "common.store.type", havingValue = "db", matchIfMissing = true)
public class CommonStoreInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CommonStoreInitializer.class);
    /** 공통모듈 테이블 DDL (순서대로 실행) */
    static final List<String> SCHEMAS = List.of("schema-common-home.sql", "schema-common-consent.sql", "schema-common-producttab.sql",
            "schema-common-notice.sql", "schema-common-niceid.sql");

    private final DataSource dataSource;
    private final CommonStoreProperties properties;
    private final List<CommonSeeder> seeders;

    public CommonStoreInitializer(DataSource dataSource, CommonStoreProperties properties, List<CommonSeeder> seeders) {
        this.dataSource = dataSource;
        this.properties = properties;
        this.seeders = seeders;
    }

    @Override
    public void run(ApplicationArguments args) {
        initialize();
    }

    /** 테스트에서도 직접 호출 */
    public boolean initialize() {
        try {
            if (properties.isInitSchema()) {
                for (String schema : SCHEMAS) {
                    ResourceDatabasePopulator p = new ResourceDatabasePopulator(new ClassPathResource(schema));
                    p.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
                    p.execute(dataSource);
                    log.info("[공통 DB] {} 적용 (CREATE TABLE IF NOT EXISTS)", schema);
                }
            }
            if (properties.isSeedIfEmpty()) {
                for (CommonSeeder s : seeders) {
                    s.seedIfEmpty();
                }
            }
            return true;
        } catch (RuntimeException e) {
            log.warn("[공통 DB] 초기화 실패 - DB 연결/권한을 확인하세요 (common.store.type=memory 로 DB 없이 실행 가능): {}", e.getMessage());
            return false;
        }
    }
}
