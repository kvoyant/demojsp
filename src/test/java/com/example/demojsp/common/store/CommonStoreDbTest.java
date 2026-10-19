package com.example.demojsp.common.store;

import com.example.demojsp.common.banner.config.BannerPopupProperties;
import com.example.demojsp.common.banner.domain.BannerPopupRequest;
import com.example.demojsp.common.banner.mapper.BannerPopupMapper;
import com.example.demojsp.common.banner.service.BannerPopupService;
import com.example.demojsp.common.banner.store.ConfigAccountHoldingChecker;
import com.example.demojsp.common.banner.store.MyBatisBannerPopupRepository;
import com.example.demojsp.common.member.MemberContext;
import com.example.demojsp.common.member.MemberSegment;
import com.example.demojsp.common.menu.config.MenuProperties;
import com.example.demojsp.common.menu.domain.BankingMenu;
import com.example.demojsp.common.menu.mapper.BankingMenuMapper;
import com.example.demojsp.common.menu.service.BankingMenuService;
import com.example.demojsp.common.menu.store.MyBatisBankingMenuRepository;
import com.example.demojsp.common.product.config.ProductProperties;
import com.example.demojsp.common.product.domain.HomeSettingRequest;
import com.example.demojsp.common.product.domain.ProductAdminView;
import com.example.demojsp.common.product.exception.ProductException;
import com.example.demojsp.common.product.mapper.ProductMapper;
import com.example.demojsp.common.product.service.ProductAdminService;
import com.example.demojsp.common.product.service.ProductService;
import com.example.demojsp.common.product.store.MyBatisProductRepository;
import com.example.demojsp.common.resume.config.ResumeProperties;
import com.example.demojsp.common.resume.domain.ResumePointRequest;
import com.example.demojsp.common.resume.domain.ResumeScope;
import com.example.demojsp.common.resume.mapper.ResumeMapper;
import com.example.demojsp.common.resume.service.ResumeService;
import com.example.demojsp.common.resume.store.MyBatisPartnerRepository;
import com.example.demojsp.common.resume.store.MyBatisResumeRepository;
import com.example.demojsp.common.store.mapper.CommonSequenceMapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.h2.jdbcx.JdbcDataSource;
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
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 공통모듈 DB 저장소 통합 테스트 - <b>실제 schema-common-home.sql + mapper/common/*.xml + application.yaml seed</b> 를
 * H2(MySQL 호환 모드)에서 실행한다. (운영/로컬은 MySQL demo_db02 - 같은 SQL)
 *
 * <p>Spring 컨텍스트 없이 MyBatis·트랜잭션만 구성: 다른 업무(로그인·NICE 등)의 MySQL 의존 없이 공통모듈만 검증.
 */
class CommonStoreDbTest {

    private JdbcTemplate jdbc;
    private Stack s;

    /** 같은 DB 위에 서비스 묶음 (재기동 흉내 시 다시 생성) */
    private static final class Stack {
        ResumeService resume;
        BannerPopupService banner;
        BankingMenuService menu;
        ProductService product;
        ProductAdminService productAdmin;
        CommonStoreInitializer initializer;
    }

    private JdbcDataSource ds;
    private SqlSessionTemplate session;
    private DataSourceTransactionManager tm;

    @BeforeEach
    void setUp() throws Exception {
        ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:common" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        jdbc = new JdbcTemplate(ds);
        tm = new DataSourceTransactionManager(ds);

        SqlSessionFactoryBean f = new SqlSessionFactoryBean();
        f.setDataSource(ds);
        f.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/common/*.xml"));
        org.apache.ibatis.session.Configuration cfg = new org.apache.ibatis.session.Configuration();
        cfg.setMapUnderscoreToCamelCase(true);       // application.yaml mybatis.configuration 과 동일
        f.setConfiguration(cfg);
        SqlSessionFactory factory = f.getObject();
        session = new SqlSessionTemplate(factory);
        s = start();
        assertThat(s.initializer.initialize()).isTrue();
    }

    /** application.yaml 을 앱과 같은 방식으로 바인딩해 서비스 구성 (MyBatis 저장소) */
    private Stack start() throws Exception {
        List<PropertySource<?>> yaml = new YamlPropertySourceLoader().load("app", new ClassPathResource("application.yaml"));
        Binder binder = new Binder(ConfigurationPropertySources.from(yaml));
        ResumeProperties rp = binder.bind("common.resume", ResumeProperties.class).get();
        BannerPopupProperties bp = binder.bind("common.banner-popup", BannerPopupProperties.class).get();
        MenuProperties mp = binder.bind("common.menu", MenuProperties.class).get();
        ProductProperties pp = binder.bind("common.product", ProductProperties.class).get();
        CommonStoreProperties sp = binder.bind("common.store", CommonStoreProperties.class).get();
        assertThat(sp.getType()).isEqualTo("db");

        CommonSequence seq = new CommonSequence(session.getMapper(CommonSequenceMapper.class), tm);
        Stack st = new Stack();
        st.resume = new ResumeService(new MyBatisResumeRepository(session.getMapper(ResumeMapper.class), seq),
                new MyBatisPartnerRepository(session.getMapper(ResumeMapper.class)), rp);
        st.banner = new BannerPopupService(new MyBatisBannerPopupRepository(session.getMapper(BannerPopupMapper.class), seq), bp,
                new ConfigAccountHoldingChecker(bp));
        MyBatisProductRepository productRepo = new MyBatisProductRepository(session.getMapper(ProductMapper.class), seq);
        st.menu = new BankingMenuService(new MyBatisBankingMenuRepository(session.getMapper(BankingMenuMapper.class), seq), mp);
        st.product = new ProductService(productRepo, pp);
        st.productAdmin = new ProductAdminService(productRepo, pp);
        st.initializer = new CommonStoreInitializer(ds, sp, List.of(st.resume, st.banner, st.menu, st.product));
        return st;
    }

    private int count(String table) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return n == null ? 0 : n;
    }

    @Test
    @DisplayName("기동 초기화: 테이블 생성 + 빈 테이블에 yaml seed 적재 / 재기동(다시 초기화)해도 중복 적재 없음")
    void schemaAndSeedIdempotent() throws Exception {
        Map<String, Integer> before = Map.of(
                "TB_CO_PARTNER", count("TB_CO_PARTNER"), "TB_RESUME_POINT", count("TB_RESUME_POINT"),
                "TB_BANNER_POPUP", count("TB_BANNER_POPUP"), "TB_BANKING_MENU", count("TB_BANKING_MENU"),
                "TB_PRODUCT", count("TB_PRODUCT"));
        assertThat(before.get("TB_CO_PARTNER")).isEqualTo(3);
        assertThat(before.get("TB_RESUME_POINT")).isEqualTo(7);          // customer01 6건(진행 4·완료 1·만료 1) + customer02 1건
        assertThat(before.get("TB_BANNER_POPUP")).isEqualTo(14);         // 배너팝업 7 + 상단슬라이드 7
        assertThat(before.get("TB_BANKING_MENU")).isEqualTo(10);
        assertThat(before.get("TB_PRODUCT")).isEqualTo(6);

        Stack restarted = start();                                       // 재기동
        assertThat(restarted.initializer.initialize()).isTrue();
        before.forEach((t, n) -> assertThat(count(t)).as(t).isEqualTo(n));
        assertThat(jdbc.queryForList("SELECT SEQ_NM FROM TB_CO_SEQ ORDER BY SEQ_NM", String.class))
                .containsExactly("BANKING_MENU", "BANNER_POPUP", "PRODUCT", "RESUME_POINT");
    }

    @Test
    @DisplayName("이어하기(공통#43·49·50): DB 조회 - 홈 4건·저축 2건, 단계 저장 갱신/등록, 취소가 DB 에 반영되고 재기동 후에도 유지")
    void resumeOnDb() throws Exception {
        assertThat(s.resume.summary("customer01", ResumeScope.ALL, LocalDateTime.now()).count()).isEqualTo(4);
        assertThat(s.resume.counts("customer01")).containsEntry("DEPOSIT", 2).containsEntry("LOAN", 2);
        assertThat(s.resume.summary("customer01", ResumeScope.ALL, LocalDateTime.now()).latest().companyName()).isEqualTo("제휴플랫폼A");

        long same = s.resume.saveStep("customer01", new ResumePointRequest("LOAN", "LOAN_APPLY", "대출신청", "PS_SMART",
                "페퍼스마트대출", "PARTNER_A", "전자서명", 4, 5, "/loan/apply?step=4", 7));
        assertThat(s.resume.counts("customer01").get("ALL")).isEqualTo(4);     // 같은 업무·상품 → 갱신
        long added = s.resume.saveStep("customer03", new ResumePointRequest("DEPOSIT", "DEPOSIT_OPEN", "계좌개설", "PS_2030",
                "페퍼스 2030적금", "OWN", "약관동의", 1, 4, "/deposit/open", 30));
        assertThat(added).isGreaterThan(same);
        assertThat(jdbc.queryForObject("SELECT STEP_NO FROM TB_RESUME_POINT WHERE RESUME_ID = ?", Integer.class, same)).isEqualTo(4);

        s.resume.cancel("customer03", added);
        assertThat(jdbc.queryForObject("SELECT STATUS FROM TB_RESUME_POINT WHERE RESUME_ID = ?", String.class, added)).isEqualTo("CANCELED");
        Stack restarted = start();
        assertThat(restarted.resume.counts("customer03").get("DEPOSIT")).isZero();
        assertThat(restarted.resume.summary("customer01", ResumeScope.LOAN, LocalDateTime.now()).latest().stepNo()).isEqualTo(4);
    }

    @Test
    @DisplayName("배너(공통#39·44·45): 겹침 배너팝업 3건 전부, 상단슬라이드 최대 5건, 상단슬라이드 등록(배경색)·삭제가 DB 반영")
    void bannerOnDb() {
        assertThat(s.banner.displayable("APP").total()).isEqualTo(3);
        var slides = s.banner.topSlides("APP", null);
        assertThat(slides.total()).isEqualTo(5);
        assertThat(slides.truncated()).isEqualTo(1);
        assertThat(s.banner.topSlides("APP", "customer01").eligible()).isFalse();

        LocalDateTime now = LocalDateTime.now();
        var saved = s.banner.register(new BannerPopupRequest("TOP_SLIDE", null, List.of("APP", "PC_WEB"), "DB 저장 배너", "서브",
                null, null, "/product/x", now.minusHours(1).toString(), now.plusDays(1).toString(), 1, true, "#0d47a1"), "admin01");
        assertThat(jdbc.queryForMap("SELECT BG_COLOR, CHANNELS, CATEGORY FROM TB_BANNER_POPUP WHERE BANNER_ID = ?", saved.id()))
                .containsEntry("BG_COLOR", "#0D47A1").containsEntry("CHANNELS", "APP,PC_WEB").containsEntry("CATEGORY", "EVENT");
        assertThat(s.banner.topSlides("APP", null).items().get(0).mainText()).isEqualTo("DB 저장 배너");
        s.banner.delete(saved.id(), "admin01");
        assertThat(count("TB_BANNER_POPUP")).isEqualTo(14);
    }

    @Test
    @DisplayName("뱅킹메뉴(공통#46): 전체메뉴 상품 = 전체상품·예금·대출·계산기, 순서 변경이 DB 반영")
    void menuOnDb() {
        var all = s.menu.allMenu();
        assertThat(all.categories().get(0).menus()).extracting(m -> m.name())
                .containsExactly("전체상품", "예금상품", "대출상품", "금융계산기");
        List<Long> ids = s.menu.adminList("PRODUCT").stream().map(BankingMenu::id).toList();
        s.menu.reorder("PRODUCT", List.of(ids.get(3), ids.get(0), ids.get(1), ids.get(2)), "admin01");
        assertThat(jdbc.queryForObject("SELECT DISPLAY_ORDER FROM TB_BANKING_MENU WHERE MENU_ID = ?", Integer.class, ids.get(3))).isEqualTo(1);
        assertThat(s.menu.categories()).containsKeys("PRODUCT", "MY", "TRANSFER", "SERVICE");
    }

    @Test
    @DisplayName("상품(공통#47·48): 추천 3건, 합산 3개 초과 문구, N 저장 시 HOME_DISP_ORD NULL, 금융회원 숨김")
    void productOnDb() {
        var rec = s.product.recommend(new MemberContext(null, MemberSegment.GUEST));
        assertThat(rec.items()).extracting(i -> i.name()).containsExactly("페퍼스마트대출", "페퍼스 2030적금", "페퍼스 파킹통장4");
        assertThat(s.product.recommend(new MemberContext("customer01", MemberSegment.FINANCIAL)).eligible()).isFalse();

        ProductAdminView biz = s.productAdmin.list("LOAN").stream().filter(p -> p.code().equals("PS_BIZ_MORTGAGE")).findFirst().orElseThrow();
        assertThatThrownBy(() -> s.productAdmin.changeHome(biz.id(), new HomeSettingRequest("Y", 1), "admin01"))
                .isInstanceOf(ProductException.class)
                .satisfies(e -> assertThat(((ProductException) e).displayMessage()).isEqualTo("등록 건수 3개를 초과하였습니다."));

        ProductAdminView smart = s.productAdmin.list("LOAN").stream().filter(p -> p.code().equals("PS_SMART")).findFirst().orElseThrow();
        s.productAdmin.changeHome(smart.id(), new HomeSettingRequest("N", null), "admin01");
        assertThat(jdbc.queryForMap("SELECT HOME_DISP_YN, HOME_DISP_ORD FROM TB_PRODUCT WHERE PRODUCT_ID = ?", smart.id()))
                .containsEntry("HOME_DISP_YN", "N").containsEntry("HOME_DISP_ORD", null);
        s.productAdmin.changeHome(biz.id(), new HomeSettingRequest("Y", 1), "admin01");
        assertThat(s.product.recommend(new MemberContext(null, MemberSegment.GUEST)).items().get(0).name()).isEqualTo("페퍼스 사업자담보대출");
    }

    @Test
    @DisplayName("채번(TB_CO_SEQ): 동시 요청 200건도 번호 중복 없음 (행 잠금)")
    void sequenceUniqueUnderConcurrency() throws Exception {
        CommonSequence seq = new CommonSequence(session.getMapper(CommonSequenceMapper.class), tm);
        Set<Long> ids = java.util.Collections.synchronizedSet(new HashSet<>());
        List<Thread> threads = new java.util.ArrayList<>();
        for (int t = 0; t < 8; t++) {
            Thread th = new Thread(() -> { for (int i = 0; i < 25; i++) ids.add(seq.next("TEST_SEQ", 0)); });
            threads.add(th);
            th.start();
        }
        for (Thread th : threads) th.join();
        assertThat(ids).hasSize(200);
        assertThat(ids).allMatch(v -> v >= 1 && v <= 200);
    }
}
