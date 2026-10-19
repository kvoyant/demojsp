package com.example.demojsp.common.niceid.config;

import com.example.demojsp.common.niceid.gateway.MockNiceIdGateway;
import com.example.demojsp.common.niceid.mapper.NiceIdMapper;
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
 * 본인인증 샘플 데이터 적재 (local).
 * <ol>
 *   <li>TB_CO_IDV_PURPOSE 가 비어 있으면 data-common-niceid.sql (업무별 정책 4건)</li>
 *   <li>유의고객 테스트 인물 2명을 TB_WATCHLIST 에 "없을 때만" 추가 (기존 유의고객 데이터는 건드리지 않음)
 *       - 김유의 / 19800101 / 01099990000, 박차단 / 19750505 / 01088880000 (시뮬레이터 CI 기준)</li>
 * </ol>
 */
@Component
@ConditionalOnProperty(name = "common.store.type", havingValue = "db", matchIfMissing = true)
public class NiceIdSeeder implements CommonSeeder {

    private static final Logger log = LoggerFactory.getLogger(NiceIdSeeder.class);
    static final String DATA = "data-common-niceid.sql";

    private final DataSource dataSource;
    private final NiceIdMapper mapper;

    public NiceIdSeeder(DataSource dataSource, NiceIdMapper mapper) {
        this.dataSource = dataSource;
        this.mapper = mapper;
    }

    @Override
    public String seedName() {
        return "본인인증";
    }

    @Override
    public void seedIfEmpty() {
        Integer n = new JdbcTemplate(dataSource).queryForObject("SELECT COUNT(*) FROM TB_CO_IDV_PURPOSE", Integer.class);
        if (n == null || n == 0) {
            ResourceDatabasePopulator p = new ResourceDatabasePopulator(new ClassPathResource(DATA));
            p.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
            p.execute(dataSource);
            log.info("[공통 DB] 본인인증 정책 샘플 적재 ({})", DATA);
        }
        watch("김유의", "19800101", "01099990000", "CRITICAL", "FDS_FRAUD", "[샘플] 보이스피싱 연루 - 본인인증 차단 테스트");
        watch("박차단", "19750505", "01088880000", "HIGH", "IDENTITY_THEFT", "[샘플] 명의도용 신고 - 본인인증 차단 테스트");
    }

    private void watch(String name, String birth, String phone, String grade, String type, String reason) {
        String ci = MockNiceIdGateway.identity(name, birth, phone).ci();
        if (mapper.existsWatchlist(ci) == 0) {
            mapper.insertWatchlist(ci, name, grade, type, reason);
        }
    }
}
