package com.example.demojsp.common.acctverify.config;

import com.example.demojsp.common.acctverify.mapper.AcctVerifyMapper;
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
 * 1원 계좌인증 샘플 적재 (local).
 * <ol>
 *   <li>TB_CO_ACCT_BIZ 가 비어 있으면 data-common-acctverify.sql (업무 Context 4건)</li>
 *   <li>기존 TB_BANK_CODE 가 비어 있으면 은행 21곳, 기존 TB_LOAN_ACCOUNT 가 비어 있으면 대출계좌 4건
 *       (이미 데이터가 있는 DB 는 건드리지 않는다 - 기존 data-account-cert.sql 과 같은 값)</li>
 * </ol>
 */
@Component
@ConditionalOnProperty(name = "common.store.type", havingValue = "db", matchIfMissing = true)
public class AcctVerifySeeder implements CommonSeeder {

    private static final Logger log = LoggerFactory.getLogger(AcctVerifySeeder.class);
    static final String DATA = "data-common-acctverify.sql";

    private final DataSource dataSource;
    private final AcctVerifyMapper mapper;

    public AcctVerifySeeder(DataSource dataSource, AcctVerifyMapper mapper) {
        this.dataSource = dataSource;
        this.mapper = mapper;
    }

    @Override
    public String seedName() {
        return "1원계좌인증";
    }

    @Override
    public void seedIfEmpty() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM TB_CO_ACCT_BIZ", Integer.class);
        if (n == null || n == 0) {
            ResourceDatabasePopulator p = new ResourceDatabasePopulator(new ClassPathResource(DATA));
            p.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
            p.execute(dataSource);
            log.info("[공통 DB] 1원 계좌인증 업무 Context 샘플 적재 ({})", DATA);
        }
        if (mapper.countBanks() == 0) {
            String[][] banks = {{"004", "KB국민은행"}, {"088", "신한은행"}, {"020", "우리은행"}, {"081", "하나은행"}, {"011", "NH농협은행"},
                    {"003", "IBK기업은행"}, {"090", "카카오뱅크"}, {"092", "토스뱅크"}, {"089", "케이뱅크"}, {"023", "SC제일은행"},
                    {"027", "한국씨티은행"}, {"031", "DGB대구은행(iM뱅크)"}, {"032", "BNK부산은행"}, {"034", "광주은행"}, {"039", "BNK경남은행"},
                    {"037", "전북은행"}, {"035", "제주은행"}, {"071", "우체국"}, {"045", "새마을금고"}, {"048", "신협"}, {"050", "상호저축은행"}};
            for (int i = 0; i < banks.length; i++) {
                jdbc.update("INSERT INTO TB_BANK_CODE (BANK_CD, BANK_NM, BANK_TYPE, SORT_ORDER, USE_YN) VALUES (?, ?, 'BANK', ?, 'Y')",
                        banks[i][0], banks[i][1], i + 1);
            }
            log.info("[공통 DB] TB_BANK_CODE 비어 있음 → 은행 {}곳 적재", banks.length);
        }
        if (mapper.countLoanAccounts() == 0) {
            String sql = "INSERT INTO TB_LOAN_ACCOUNT (LOAN_NO, MEMBER_ID, CUST_NM, PRODUCT_NM, BANK_CD, BANK_NM, ACCOUNT_NO, ACCOUNT_HOLDER_NM, ACCOUNT_TYPE) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'DISBURSEMENT_REPAYMENT')";
            jdbc.update(sql, "LN202610130001", "customer01", "홍길동", "중금리 직장인 신용대출", "004", "KB국민은행", "123402-04-567890", "홍길동");
            jdbc.update(sql, "LN202610130002", "counselor01", "김상담", "햇살론 (근로자 연계)", "088", "신한은행", "110-333-444555", "김상담");
            jdbc.update(sql, "LN202610130003", "locked01", "이잠금", "서민금융 소상공인대출", "020", "우리은행", "1002-888-999000", "이잠금");
            jdbc.update(sql, "LN202610130004", "customer01", "홍길동", "모바일 마이너스통장 대출", "090", "카카오뱅크", "3333-01-9876543", "홍길동");
            log.info("[공통 DB] TB_LOAN_ACCOUNT 비어 있음 → 대출계좌 샘플 4건 적재");
        }
    }
}
