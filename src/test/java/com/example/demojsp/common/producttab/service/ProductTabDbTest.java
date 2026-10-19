package com.example.demojsp.common.producttab.service;

import com.example.demojsp.common.product.config.ProductProperties;
import com.example.demojsp.common.product.domain.ProductUpdateRequest;
import com.example.demojsp.common.product.mapper.ProductMapper;
import com.example.demojsp.common.product.service.ProductAdminService;
import com.example.demojsp.common.product.service.ProductService;
import com.example.demojsp.common.product.store.MyBatisProductRepository;
import com.example.demojsp.common.producttab.config.ProductTabProperties;
import com.example.demojsp.common.producttab.config.ProductTabSeeder;
import com.example.demojsp.common.producttab.controller.ProductTabAdminApiController;
import com.example.demojsp.common.producttab.controller.ProductTabAdminAuth;
import com.example.demojsp.common.producttab.controller.ProductTabApiController;
import com.example.demojsp.common.producttab.dto.ProductTabView;
import com.example.demojsp.common.producttab.dto.PtItemDto;
import com.example.demojsp.common.producttab.dto.PtSectionDto;
import com.example.demojsp.common.producttab.dto.PtSlideDto;
import com.example.demojsp.common.producttab.exception.ProductTabException;
import com.example.demojsp.common.producttab.exception.ProductTabExceptionHandler;
import com.example.demojsp.common.producttab.mapper.ProductTabMapper;
import com.example.demojsp.common.store.CommonSequence;
import com.example.demojsp.common.store.CommonStoreInitializer;
import com.example.demojsp.common.store.CommonStoreProperties;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [공통#26] 메인화면 상품 탭 - 실제 schema/data SQL + ProductTabMapper.xml 을 H2(MySQL 모드)에서 실행.
 * 상품 원장(TB_PRODUCT)은 공통#47·48 서비스가 application.yaml seed 로 채운다 (앱 기동과 같은 순서).
 * 기준일 2026-10-19.
 */
class ProductTabDbTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Instant NOW = LocalDate.of(2026, 10, 19).atTime(10, 0).atZone(KST).toInstant();

    private JdbcTemplate jdbc;
    private ProductTabServiceImpl service;
    private ProductTabAdminServiceImpl admin;
    private ProductTabProperties props;
    private ProductAdminService productAdmin;
    private CommonStoreInitializer initializer;

    @BeforeEach
    void setUp() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:ptab" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        jdbc = new JdbcTemplate(ds);

        SqlSessionFactoryBean f = new SqlSessionFactoryBean();
        f.setDataSource(ds);
        f.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/common/*.xml"));
        org.apache.ibatis.session.Configuration cfg = new org.apache.ibatis.session.Configuration();
        cfg.setMapUnderscoreToCamelCase(true);
        f.setConfiguration(cfg);
        SqlSessionFactory factory = f.getObject();
        SqlSessionTemplate session = new SqlSessionTemplate(factory);

        // 상품 원장 (공통#47·48) - yaml seed
        List<PropertySource<?>> yaml = new YamlPropertySourceLoader().load("app", new ClassPathResource("application.yaml"));
        Binder binder = new Binder(ConfigurationPropertySources.from(yaml));
        ProductProperties pp = binder.bind("common.product", ProductProperties.class).get();
        CommonSequence seq = new CommonSequence(session.getMapper(CommonSequenceMapper.class), new DataSourceTransactionManager(ds));
        MyBatisProductRepository productRepo = new MyBatisProductRepository(session.getMapper(ProductMapper.class), seq);
        ProductService productService = new ProductService(productRepo, pp);
        productAdmin = new ProductAdminService(productRepo, pp);

        props = binder.bind("common.product-tab", ProductTabProperties.class).get();
        ProductTabMapper mapper = session.getMapper(ProductTabMapper.class);
        service = new ProductTabServiceImpl(mapper, props);
        service.setClock(Clock.fixed(NOW, KST));
        admin = new ProductTabAdminServiceImpl(mapper);
        admin.setClock(Clock.fixed(NOW, KST));

        CommonStoreProperties sp = new CommonStoreProperties();
        sp.setInitSchema(true);
        sp.setSeedIfEmpty(true);
        initializer = new CommonStoreInitializer(ds, sp, List.of(productService, new ProductTabSeeder(ds)));
        assertThat(initializer.initialize()).isTrue();
    }

    private static List<String> slideTitles(ProductTabView v) {
        return v.getSlides().stream().map(PtSlideDto::getTitle).toList();
    }

    private static List<String> sectionCodes(ProductTabView v) {
        return v.getSections().stream().map(PtSectionDto::getSectionCd).toList();
    }

    private static List<String> items(ProductTabView v, String section) {
        return v.getSections().stream().filter(s -> s.getSectionCd().equals(section)).findFirst().orElseThrow()
                .getItems().stream().map(PtItemDto::getProductCd).toList();
    }

    @Test
    @DisplayName("기동 초기화: 테이블 3개 + 샘플, 다시 초기화해도 중복 없음")
    void schemaAndSeed() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM TB_PRD_TAB_SLIDE", Integer.class)).isEqualTo(6);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM TB_PRD_TAB_SECTION", Integer.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM TB_PRD_TAB_ITEM", Integer.class)).isEqualTo(6);
        initializer.initialize();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM TB_PRD_TAB_SLIDE", Integer.class)).isEqualTo(6);
    }

    @Test
    @DisplayName("APP 화면: 게시 중 카드만 표시순서대로, 판매중지 상품 카드·목록 제외, 시세조회 카드 미노출")
    void appView() {
        ProductTabView app = service.getView("APP");
        // 게시종료·게시예정·판매중지(오토론) 카드 제외
        assertThat(slideTitles(app)).containsExactly("하루만 맡겨도 연 3.2%", "모바일로 한도조회부터 대출까지", "2030 우대금리 적금");
        PtSlideDto first = app.getSlides().get(0);
        assertThat(first.getLinkUrl()).isEqualTo("/product/deposit/PS_PARKING4");        // 상품 원장 URL
        assertThat(first.getTextColor()).isEqualTo("#FFFFFF");                            // 진한 파랑 배경 → 흰 글자
        assertThat(app.getSlides().get(1).getTextColor()).isEqualTo("#111827");           // 밝은 노랑 → 진한 글자
        assertThat(first.getUseYn()).isNull();                                            // 관리 항목은 응답에서 제외

        // 섹션: 시세조회(AUTO_PRICE) 는 사용 N → 없음
        assertThat(sectionCodes(app)).containsExactly("DEPOSIT", "LOAN", "CALC");
        assertThat(items(app, "DEPOSIT")).containsExactly("PS_PARKING4", "PS_2030", "PS_TIME_DEPOSIT");
        assertThat(items(app, "LOAN")).containsExactly("PS_SMART", "PS_BIZ_MORTGAGE");     // PS_AUTO 판매중지
        PtItemDto smart = app.getSections().get(1).getItems().get(0);
        assertThat(smart.getProductNm()).isEqualTo("페퍼스마트대출");
        assertThat(smart.getRateText()).isEqualTo("최저 연 6.9%");
        assertThat(smart.getBadgeText()).isEqualTo("인기");
        assertThat(app.getSections().get(2).getLinkUrl()).isEqualTo("/product/calculator");
    }

    @Test
    @DisplayName("채널 · 기준일: PC Web 은 APP 전용 카드 제외, 2027-01-01 미리보기는 게시예정 카드 노출")
    void channelAndDate() {
        assertThat(slideTitles(service.getView("PC_WEB"))).containsExactly("하루만 맡겨도 연 3.2%", "모바일로 한도조회부터 대출까지");
        assertThat(slideTitles(service.getView("APP", LocalDate.of(2027, 1, 1))))
                .contains("[게시예정] 새해 정기예금 특판").doesNotContain("[게시종료] 봄맞이 이벤트");
        assertThatThrownBy(() -> service.getView("ATM")).isInstanceOf(ProductTabException.class);

        props.setMaxSlides(2);
        assertThat(service.getView("APP").getSlides()).hasSize(2);
    }

    @Test
    @DisplayName("상품 원장 연동: 상품설정(공통#48)에서 판매중지 → 상품 탭 카드·목록에서 바로 빠짐")
    void followsProductMaster() {
        long parkingId = jdbc.queryForObject("SELECT PRODUCT_ID FROM TB_PRODUCT WHERE PRODUCT_CD='PS_PARKING4'", Long.class);
        ProductUpdateRequest req = new ProductUpdateRequest("페퍼스 파킹통장4", "하루만 맡겨도 이자가 쌓이는 통장", "연 3.2%",
                "/product/deposit/PS_PARKING4", false);
        productAdmin.update(parkingId, req, "admin01");
        ProductTabView app = service.getView("APP");
        assertThat(slideTitles(app)).doesNotContain("하루만 맡겨도 연 3.2%");
        assertThat(items(app, "DEPOSIT")).containsExactly("PS_2030", "PS_TIME_DEPOSIT");
    }

    @Test
    @DisplayName("ADMIN: 슬라이드 등록·수정 검증, 섹션 사용 N(카드 삭제)·순서 변경, 섹션 상품 목록 저장")
    void adminControls() {
        PtSlideDto s = new PtSlideDto();
        s.setTitle("신규 카드");
        s.setBgColor("#ff0000");
        s.setLinkUrl("javascript:alert(1)");
        s.setChannels("APP");
        s.setStartDt(LocalDate.of(2026, 10, 1));
        s.setEndDt(LocalDate.of(2026, 12, 31));
        s.setSortOrd(1);
        assertCode(() -> admin.saveSlide(s, "admin01"), "PTB-101");                        // 위험한 URL
        s.setLinkUrl("/event/new");
        s.setImageUrl("/images/x.png");
        assertCode(() -> admin.saveSlide(s, "admin01"), "PTB-101");                        // 대체텍스트 누락
        s.setImageAlt("이벤트 이미지");
        PtSlideDto saved = admin.saveSlide(s, "admin01");
        assertThat(saved.getSlideId()).isNotNull();
        assertThat(saved.getBgColor()).isEqualTo("#FF0000");
        assertThat(slideTitles(service.getView("APP"))).startsWith("하루만 맡겨도 연 3.2%", "신규 카드");

        saved.setUseYn("N");
        admin.saveSlide(saved, "admin01");
        assertThat(slideTitles(service.getView("APP"))).doesNotContain("신규 카드");

        // 섹션: 대출을 맨 위로, 금융계산기 카드 삭제(사용 N), 시세조회 복구는 하지 않음
        PtSectionDto loan = admin.sections().stream().filter(x -> x.getSectionCd().equals("LOAN")).findFirst().orElseThrow();
        loan.setSortOrd(0);
        assertCode(() -> admin.saveSection(loan, "admin01"), "PTB-101");
        loan.setSortOrd(1);
        admin.saveSection(loan, "admin01");
        PtSectionDto calc = admin.sections().stream().filter(x -> x.getSectionCd().equals("CALC")).findFirst().orElseThrow();
        calc.setUseYn("N");
        admin.saveSection(calc, "admin01");
        assertThat(sectionCodes(service.getView("APP"))).containsExactly("DEPOSIT", "LOAN").doesNotContain("CALC");

        // 섹션 상품: 예금·적금 순서 바꾸고 하나는 사용 안 함
        admin.saveSectionItems("DEPOSIT", List.of(
                new PtItemDto("PS_2030", 1, "추천", "Y"),
                new PtItemDto("PS_PARKING4", 2, null, "Y"),
                new PtItemDto("PS_TIME_DEPOSIT", 3, null, "N")), "admin01");
        assertThat(items(service.getView("APP"), "DEPOSIT")).containsExactly("PS_2030", "PS_PARKING4");
        assertCode(() -> admin.saveSectionItems("DEPOSIT", List.of(new PtItemDto("NOPE", 1, null, "Y")), "a"), "PTB-101");
        assertCode(() -> admin.saveSectionItems("DEPOSIT", List.of(
                new PtItemDto("PS_2030", 1, null, "Y"), new PtItemDto("PS_2030", 2, null, "Y")), "a"), "PTB-101");
        assertCode(() -> admin.saveSectionItems("CALC", List.of(), "a"), "PTB-101");      // 서비스 카드엔 상품 불가
        assertThat(admin.sectionItems("DEPOSIT")).hasSize(3);                              // 실패해도 기존 유지

        // 상품을 모두 빼면 섹션 제목도 숨김
        admin.saveSectionItems("LOAN", List.of(), "admin01");
        assertThat(sectionCodes(service.getView("APP"))).containsExactly("DEPOSIT");
    }

    @Test
    @DisplayName("API: 사용자 화면 JSON / ADMIN 권한")
    void api() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ProductTabApiController(service),
                        new ProductTabAdminApiController(admin, service, new ProductTabAdminAuth(props)))
                .setControllerAdvice(new ProductTabExceptionHandler()).build();
        mvc.perform(get("/api/common/product-tab").param("channel", "APP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slides", hasSize(3)))
                .andExpect(jsonPath("$.data.sections", hasSize(3)))
                .andExpect(jsonPath("$.data.sections[0].items[0].productNm").value("페퍼스 파킹통장4"))
                .andExpect(jsonPath("$.data.slides[0].useYn").doesNotExist());
        mvc.perform(get("/api/common/product-tab").param("channel", "X"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PTB-001"));
        mvc.perform(get("/api/common/product-tab-admin/preview").param("channel", "APP").param("baseDt", "2027-01-01"))
                .andExpect(jsonPath("$.data.slides[*].title", hasItem("[게시예정] 새해 정기예금 특판")));
        props.setAdminApiOpen(false);
        mvc.perform(get("/api/common/product-tab-admin/slides")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PTB-401"));
    }

    private static void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String code) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ProductTabException.class,
                e -> assertThat(e.getErrorCode().getCode()).isEqualTo(code));
    }
}
