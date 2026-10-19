package com.example.demojsp.common.notice.config;

import com.example.demojsp.common.store.CommonSeeder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;

/**
 * 공지사항 샘플 데이터 적재 - TB_CO_NOTICE_TYPE 이 비어 있을 때만 data-common-notice.sql 실행.
 * (테이블 생성은 CommonStoreInitializer 가 schema-common-notice.sql 로 먼저 처리한다)
 */
@Component
@ConditionalOnProperty(name = "common.store.type", havingValue = "db", matchIfMissing = true)
public class NoticeSeeder implements CommonSeeder {

    private static final Logger log = LoggerFactory.getLogger(NoticeSeeder.class);
    static final String DATA = "data-common-notice.sql";

    private final DataSource dataSource;

    public NoticeSeeder(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public String seedName() {
        return "공지사항";
    }

    @Override
    public void seedIfEmpty() {
        Integer n = new JdbcTemplate(dataSource).queryForObject("SELECT COUNT(*) FROM TB_CO_NOTICE_TYPE", Integer.class);
        if (n != null && n > 0) {
            return;
        }
        ResourceDatabasePopulator p = new ResourceDatabasePopulator(new ClassPathResource(DATA));
        p.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
        p.execute(dataSource);
        log.info("[공통 DB] 공지사항 샘플 데이터 적재 ({})", DATA);
    }
}
