package com.example.demojsp.common.platformlink.service;

import com.example.demojsp.common.platformlink.adapter.FindaAdapter;
import com.example.demojsp.common.platformlink.adapter.KakaoPayAdapter;
import com.example.demojsp.common.platformlink.adapter.PartnerAdapter;
import com.example.demojsp.common.platformlink.adapter.PartnerAdapterRegistry;
import com.example.demojsp.common.platformlink.adapter.TossAdapter;
import com.example.demojsp.common.platformlink.config.PlatformLinkProperties;
import com.example.demojsp.common.platformlink.config.PlatformLinkSeeder;
import com.example.demojsp.common.platformlink.css.CircuitBreaker;
import com.example.demojsp.common.platformlink.css.CssRouter;
import com.example.demojsp.common.platformlink.css.LegacyCssSimulator;
import com.example.demojsp.common.platformlink.css.NewCssSimulator;
import com.example.demojsp.common.platformlink.dto.LnkPartnerDto;
import com.example.demojsp.common.platformlink.dto.LnkStatDto;
import com.example.demojsp.common.platformlink.exception.PlatformLinkException;
import com.example.demojsp.common.platformlink.mapper.PlatformLinkMapper;
import com.example.demojsp.common.platformlink.security.PartnerSignature;
import com.example.demojsp.common.store.CommonStoreInitializer;
import com.example.demojsp.common.store.CommonStoreProperties;
import org.apache.ibatis.session.SqlSessionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * [공통#1 REQ-AP-074] 플랫폼 대출 연계 - 실제 schema/data SQL + PlatformLinkMapper.xml 을 H2(MySQL 모드)에서 실행.
 * 제휴사 전문 → 어댑터 → CSS 시뮬레이터(LEGACY/NEW) → 결과코드 변환 → 응답 전문까지 끝에서 끝으로 확인한다.
 */
class PlatformLinkDbTest {

    private static final String CI = "kQ9vT2mXcLr8Hn3ZpW5yB1sD7fJ0aGuE4oNiYbKt6RwVhPq2CxMlSzA8eFdUjgO1Tn5HbLcIvR3yXqWm7sKpDa0e==";
    private static final String IP = "203.0.113.10";

    private final ObjectMapper om = JsonMapper.builder().build();
    private JdbcTemplate jdbc;
    private PlatformLinkProperties props;
    private PlatformLinkServiceImpl service;
    private PlatformLinkAdminServiceImpl admin;
    private PartnerAdapterRegistry adapters;
    private ExecutorService executor;

    @BeforeEach
    void setUp() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:plink" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        jdbc = new JdbcTemplate(ds);

        SqlSessionFactoryBean f = new SqlSessionFactoryBean();
        f.setDataSource(ds);
        f.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/common/*.xml"));
        org.apache.ibatis.session.Configuration cfg = new org.apache.ibatis.session.Configuration();
        cfg.setMapUnderscoreToCamelCase(true);
        f.setConfiguration(cfg);
        SqlSessionFactory factory = f.getObject();
        PlatformLinkMapper mapper = new SqlSessionTemplate(factory).getMapper(PlatformLinkMapper.class);

        List<PropertySource<?>> yaml = new YamlPropertySourceLoader().load("app", new ClassPathResource("application.yaml"));
        props = new Binder(ConfigurationPropertySources.from(yaml)).bind("common.platform-link", PlatformLinkProperties.class).get();
        assertThat(props.getSecrets()).containsEntry("TOSS", "local-toss-secret");      // yaml 키 바인딩 확인
        props.setLegacyLatencyMs(20);          // 테스트 속도
        props.setNewLatencyMs(10);
        props.setSlowLatencyMs(2_000);

        executor = Executors.newCachedThreadPool();
        CssRouter router = new CssRouter(List.of(new LegacyCssSimulator(props), new NewCssSimulator(props)), executor,
                new CircuitBreaker(3, 30, Clock.systemDefaultZone()));
        adapters = new PartnerAdapterRegistry(List.of(new TossAdapter(om), new KakaoPayAdapter(om), new FindaAdapter(om)));
        service = new PlatformLinkServiceImpl(mapper, adapters, router, props);
        admin = new PlatformLinkAdminServiceImpl(mapper, router);

        CommonStoreProperties sp = new CommonStoreProperties();
        sp.setInitSchema(true);
        sp.setSeedIfEmpty(true);
        assertThat(new CommonStoreInitializer(ds, sp, List.of(new PlatformLinkSeeder(ds))).initialize()).isTrue();
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    // ---------------------------------------------------------------- 제휴사 흉내

    private String sample(String adapterCd, String reqNo, String name, long income, String vehicle, String addr, String product) {
        return adapters.get(adapterCd).sampleRequest(new PartnerAdapter.SampleInput(reqNo, CI, name, "19900101", income, "EMPLOYEE",
                "페퍼테크", vehicle, addr, product, 0));
    }

    private PlatformLinkService.LinkResponse send(String partnerCd, String body) {
        String ts = String.valueOf(System.currentTimeMillis());
        String sig = PartnerSignature.sign(props.getSecrets().get(partnerCd), partnerCd, ts, body);
        return service.handle(partnerCd, ts, sig, body, IP);
    }

    private JsonNode json(PlatformLinkService.LinkResponse r) {
        return om.readTree(r.body());
    }

    private String col(String col, String lnkReqNo) {
        return jdbc.queryForObject("SELECT " + col + " FROM TB_LNK_LIMIT_REQ WHERE LNK_REQ_NO = ?", String.class, lnkReqNo);
    }

    private int rows() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM TB_LNK_LIMIT_REQ", Integer.class);
    }

    // ================================================================

    @Test
    @DisplayName("기동 초기화: 제휴사 4 · 상품유형 3 · 결과코드 변환 32건, 재실행해도 중복 없음")
    void seed() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM TB_LNK_PARTNER", Integer.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM TB_LNK_PRODUCT", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM TB_LNK_CODE_MAP", Integer.class)).isEqualTo(32);
    }

    @Test
    @DisplayName("토스(LEGACY) 신용: 토스 형식 응답 · 제휴사 상품코드 · 이력에는 CI 해시와 마스킹 이름만")
    void tossCredit() {
        PlatformLinkService.LinkResponse r = send("TOSS", sample("TOSS_V1", "T-1", "홍길동", 60_000_000, null, null, null));
        JsonNode n = json(r);
        assertThat(r.httpStatus()).isEqualTo(200);
        assertThat(n.path("requestId").asString()).isEqualTo("T-1");
        assertThat(n.path("resultCode").asString()).isEqualTo("SUCCESS");
        assertThat(n.path("offer").path("productCode").asString()).isEqualTo("PEPPER-CREDIT-01");
        assertThat(n.path("offer").path("limitAmount").asLong()).isEqualTo(24_000_000L);          // 현행: 연소득 40%
        assertThat(n.path("offer").path("interestRate").asDouble()).isEqualTo(13.10);            // 14.90 - 소득우대 1.80
        assertThat(col("CSS_ENGINE", r.lnkReqNo())).isEqualTo("LEGACY");
        assertThat(col("STATUS", r.lnkReqNo())).isEqualTo("DONE");
        assertThat(col("CUST_NM_MASK", r.lnkReqNo())).isEqualTo("홍*동");
        assertThat(col("CI_HASH", r.lnkReqNo())).hasSize(64).doesNotContain(CI.substring(0, 10));
        assertThat(Integer.parseInt(col("TOTAL_MS", r.lnkReqNo()))).isLessThan(3_000);
    }

    @Test
    @DisplayName("카카오페이(SHADOW) 오토: 만원↔원 변환 · 현행 결과로 응답 · 신규 CSS 결과는 비교용으로만 기록")
    void kakaoShadow() {
        PlatformLinkService.LinkResponse r = send("KAKAOPAY", sample("KAKAOPAY_V1", "KP-1", "김오토", 50_000_000, "12가3456", null, "AUTO"));
        JsonNode n = json(r);
        assertThat(n.path("res_cd").asString()).isEqualTo("0000");
        assertThat(n.path("offer").path("prd_cd").asString()).isEqualTo("KP0201");
        assertThat(n.path("offer").path("lmt_amt_manwon").asLong()).isEqualTo(3_500L);           // 현행 70% = 3,500만원
        assertThat(col("ANNUAL_INCOME", r.lnkReqNo())).isEqualTo("50000000");                    // 5000 만원 → 원
        assertThat(col("CSS_ENGINE", r.lnkReqNo())).isEqualTo("LEGACY");
        assertThat(col("SHADOW_RESULT", r.lnkReqNo())).startsWith("NEW 승인 4000만원");           // 신규 80%
    }

    @Test
    @DisplayName("핀다(NEW) 모기지 승인 / 토스는 모기지 미허용 → 미취급 상품 코드")
    void productRules() {
        PlatformLinkService.LinkResponse f = send("FINDA", sample("FINDA_V1", "FD-1", "이주택", 90_000_000, null, "서울 강남구 테헤란로 123", null));
        JsonNode n = json(f);
        assertThat(n.path("status").asString()).isEqualTo("OK");
        assertThat(n.path("loan").path("code").asString()).isEqualTo("FD-HOME-PEPPER");
        assertThat(n.path("loan").path("maxAmount").asLong()).isEqualTo(360_000_000L);           // 신규 400%
        assertThat(n.path("loan").path("minRate").asString()).isEqualTo("4.50");
        assertThat(col("CSS_ENGINE", f.lnkReqNo())).isEqualTo("NEW");

        PlatformLinkService.LinkResponse t = send("TOSS", sample("TOSS_V1", "T-2", "이주택", 90_000_000, null, "서울 강남구", null));
        assertThat(json(t).path("resultCode").asString()).isEqualTo("E404");
        assertThat(col("RESULT_CD", t.lnkReqNo())).isEqualTo("PRODUCT_NA");
        assertThat(col("STATUS", t.lnkReqNo())).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("인증: 서명 불일치 · 오래된 타임스탬프 · 허용 IP 아님 → 401 + 제휴사 인증실패 코드, 이력 없음")
    void auth() {
        String body = sample("TOSS_V1", "T-3", "홍길동", 60_000_000, null, null, null);
        String ts = String.valueOf(System.currentTimeMillis());
        PlatformLinkService.LinkResponse bad = service.handle("TOSS", ts, PartnerSignature.sign("wrong", "TOSS", ts, body), body, IP);
        assertThat(bad.httpStatus()).isEqualTo(401);
        assertThat(json(bad).path("resultCode").asString()).isEqualTo("E401");
        assertThat(json(bad).path("requestId").asString()).isEqualTo("T-3");

        String old = String.valueOf(System.currentTimeMillis() - 600_000);
        assertThat(service.handle("TOSS", old, PartnerSignature.sign("local-toss-secret", "TOSS", old, body), body, IP).httpStatus()).isEqualTo(401);

        // 본문을 바꾸면 서명이 맞지 않는다
        String sig = PartnerSignature.sign("local-toss-secret", "TOSS", ts, body);
        assertThat(service.handle("TOSS", ts, sig, body.replace("60000000", "99000000"), IP).httpStatus()).isEqualTo(401);

        jdbc.update("UPDATE TB_LNK_PARTNER SET ALLOW_IPS = '10.1.1.1' WHERE PARTNER_CD = 'TOSS'");
        assertThat(send("TOSS", body).httpStatus()).isEqualTo(401);
        assertThat(rows()).isZero();
        assertThat(service.handle("NOPE", ts, sig, body, IP).httpStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("같은 요청번호 재전송 → 저장된 응답 그대로 (CSS 재호출 없음, 이력 1건)")
    void duplicate() {
        String body = sample("TOSS_V1", "T-4", "홍길동", 60_000_000, null, null, null);
        PlatformLinkService.LinkResponse a = send("TOSS", body);
        PlatformLinkService.LinkResponse b = send("TOSS", body);
        assertThat(b.body()).isEqualTo(a.body());
        assertThat(b.lnkReqNo()).isEqualTo(a.lnkReqNo());
        assertThat(rows()).isEqualTo(1);
        assertThat(b.trace().getSteps()).anyMatch(s -> s.detail().contains("재전송"));
    }

    @Test
    @DisplayName("CSS 지연 → 제휴사 제한시간 안에 타임아웃 코드로 응답 / 서비스 중단 스위치")
    void timeoutAndStop() {
        jdbc.update("UPDATE TB_LNK_PARTNER SET TIMEOUT_MS = 800, MARGIN_MS = 100 WHERE PARTNER_CD = 'TOSS'");
        long start = System.currentTimeMillis();
        PlatformLinkService.LinkResponse r = send("TOSS", sample("TOSS_V1", "T-5", "정지연", 55_000_000, null, null, null));
        assertThat(System.currentTimeMillis() - start).isLessThan(800);                          // CSS 는 2초 걸리지만 기다리지 않음
        assertThat(json(r).path("resultCode").asString()).isEqualTo("E504");
        assertThat(col("RESULT_CD", r.lnkReqNo())).isEqualTo("TIMEOUT");

        // 네이버파이낸셜: 오픈 예정(USE_YN=N) → 서비스 중단 코드
        PlatformLinkService.LinkResponse s = send("NAVERFIN", sample("TOSS_V1", "N-1", "홍길동", 60_000_000, "34나5678", null, null));
        assertThat(json(s).path("resultCode").asString()).isEqualTo("E503");
        assertThat(col("RESULT_CD", s.lnkReqNo())).isEqualTo("STOPPED");
    }

    @Test
    @DisplayName("CSS 연속 장애 3회 → 차단기 열림(호출 생략) → ADMIN 에서 닫으면 정상")
    void circuit() {
        for (int i = 0; i < 3; i++) {
            assertThat(json(send("TOSS", sample("TOSS_V1", "T-E" + i, "강오류", 55_000_000, null, null, null))).path("resultCode").asString())
                    .isEqualTo("E500");
        }
        assertThat(admin.circuit().get("LEGACY").get("state")).isEqualTo("OPEN");
        PlatformLinkService.LinkResponse blocked = send("TOSS", sample("TOSS_V1", "T-6", "홍길동", 60_000_000, null, null, null));
        assertThat(json(blocked).path("resultCode").asString()).isEqualTo("E500");
        assertThat(blocked.trace().getSteps()).anyMatch(s -> s.detail().contains("차단기"));

        admin.resetCircuit("LEGACY");
        assertThat(json(send("TOSS", sample("TOSS_V1", "T-7", "홍길동", 60_000_000, null, null, null))).path("resultCode").asString())
                .isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("전문 오류: 필수값 누락은 요청번호와 함께 오류코드 + 이력, JSON 깨짐은 응답만")
    void invalid() {
        String noCi = sample("TOSS_V1", "T-8", "홍길동", 60_000_000, null, null, null).replace(CI, "");
        PlatformLinkService.LinkResponse r = send("TOSS", noCi);
        assertThat(json(r).path("resultCode").asString()).isEqualTo("E400");
        assertThat(json(r).path("requestId").asString()).isEqualTo("T-8");
        assertThat(col("RESULT_CD", r.lnkReqNo())).isEqualTo("INVALID");

        PlatformLinkService.LinkResponse broken = send("KAKAOPAY", "{not json");
        assertThat(json(broken).path("res_cd").asString()).isEqualTo("8001");
        assertThat(rows()).isEqualTo(1);
    }

    @Test
    @DisplayName("어드민: 엔진 전환 즉시 적용 · 입력 검증 · 처리현황")
    void adminOps() {
        LnkPartnerDto p = admin.partners().stream().filter(x -> "TOSS".equals(x.getPartnerCd())).findFirst().orElseThrow();
        p.setCssEngine("NEW");
        assertThat(admin.savePartner(p, "admin01").getUpdBy()).isEqualTo("admin01");
        PlatformLinkService.LinkResponse r = send("TOSS", sample("TOSS_V1", "T-9", "홍길동", 60_000_000, null, null, null));
        assertThat(json(r).path("offer").path("limitAmount").asLong()).isEqualTo(30_000_000L);    // 신규 50%
        assertThat(col("CSS_ENGINE", r.lnkReqNo())).isEqualTo("NEW");

        p.setCssEngine("FAST");
        assertThatThrownBy(() -> admin.savePartner(p, "admin01")).isInstanceOf(PlatformLinkException.class);
        p.setCssEngine("LEGACY");
        p.setProducts("CREDIT,LEASE");
        assertThatThrownBy(() -> admin.savePartner(p, "admin01")).isInstanceOf(PlatformLinkException.class);
        p.setProducts("CREDIT");
        p.setTimeoutMs(100);
        assertThatThrownBy(() -> admin.savePartner(p, "admin01")).isInstanceOf(PlatformLinkException.class);

        LnkStatDto toss = admin.stats(24).stream().filter(s -> "TOSS".equals(s.getPartnerCd())).findFirst().orElseThrow();
        assertThat(toss.getTotal()).isEqualTo(1);
        assertThat(toss.getApproved()).isEqualTo(1);
        assertThat(admin.requests("TOSS", null, null, 10)).hasSize(1);
    }
}
