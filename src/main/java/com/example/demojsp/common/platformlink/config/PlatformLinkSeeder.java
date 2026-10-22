package com.example.demojsp.common.platformlink.config;

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

/** 플랫폼 대출 연계 샘플 적재: TB_LNK_PARTNER 가 비어 있을 때만 data-common-platformlink.sql 1회 */
@Component
@ConditionalOnProperty(name = "common.store.type", havingValue = "db", matchIfMissing = true)
public class PlatformLinkSeeder implements CommonSeeder {

    private static final Logger log = LoggerFactory.getLogger(PlatformLinkSeeder.class);
    static final String DATA = "data-common-platformlink.sql";

    private final DataSource dataSource;

    public PlatformLinkSeeder(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public String seedName() {
        return "플랫폼대출연계";
    }

    @Override
    public void seedIfEmpty() {
        Integer n = new JdbcTemplate(dataSource).queryForObject("SELECT COUNT(*) FROM TB_LNK_PARTNER", Integer.class);
        if (n != null && n > 0) return;
        ResourceDatabasePopulator p = new ResourceDatabasePopulator(new ClassPathResource(DATA));
        p.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
        p.execute(dataSource);
        log.info("[공통 DB] 플랫폼 대출 연계 샘플 적재 ({})", DATA);
    }
}
