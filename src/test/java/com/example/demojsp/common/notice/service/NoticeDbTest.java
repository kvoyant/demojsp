package com.example.demojsp.common.notice.service;

import com.example.demojsp.common.notice.config.NoticeProperties;
import com.example.demojsp.common.notice.config.NoticeSeeder;
import com.example.demojsp.common.notice.controller.NoticeAdminApiController;
import com.example.demojsp.common.notice.controller.NoticeAdminAuth;
import com.example.demojsp.common.notice.controller.NoticeApiController;
import com.example.demojsp.common.notice.dto.NoticeDto;
import com.example.demojsp.common.notice.dto.NoticeFilters;
import com.example.demojsp.common.notice.dto.NoticePage;
import com.example.demojsp.common.notice.dto.NoticeTargetDto;
import com.example.demojsp.common.notice.dto.NoticeTypeDto;
import com.example.demojsp.common.notice.exception.NoticeException;
import com.example.demojsp.common.notice.exception.NoticeExceptionHandler;
import com.example.demojsp.common.notice.mapper.NoticeMapper;
import com.example.demojsp.common.product.config.ProductProperties;
import com.example.demojsp.common.product.mapper.ProductMapper;
import com.example.demojsp.common.product.service.ProductService;
import com.example.demojsp.common.product.store.MyBatisProductRepository;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [공통#28] 공지사항 - 실제 schema/data SQL + NoticeMapper.xml 을 H2(MySQL 모드)에서 실행.
 * 기준 시각 2026-10-20 10:00. 샘플 8건 중 게시 중 5건 (종료 1 · 예약 1 · 미사용 1 제외).
 * 공지 ID 는 샘플 INSERT 순서: 1 시스템점검(전체·띠·팝업·고정) 2 스마트대출(상품·띠) 3 앱개편(전체) 4 파킹통장(상품)
 *                              5 대출서류(업무 LOAN) 6 인증서(종료·띠) 7 개인정보(예약·띠) 8 미사용
 */
class NoticeDbTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 20, 10, 0);

    private JdbcTemplate jdbc;
    private NoticeServiceImpl service;
    private NoticeAdminServiceImpl admin;
    private NoticeProperties props;
    private CommonStoreInitializer initializer;

    @BeforeEach
    void setUp() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:notice" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
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

        // 상품 원장 (상품별 공지의 상품명) - 앱 기동과 같이 yaml seed
        List<PropertySource<?>> yaml = new YamlPropertySourceLoader().load("app", new ClassPathResource("application.yaml"));
        Binder binder = new Binder(ConfigurationPropertySources.from(yaml));
        ProductProperties pp = binder.bind("common.product", ProductProperties.class).get();
        CommonSequence seq = new CommonSequence(session.getMapper(CommonSequenceMapper.class), new DataSourceTransactionManager(ds));
        ProductService productService = new ProductService(new MyBatisProductRepository(session.getMapper(ProductMapper.class), seq), pp);

        props = binder.bind("common.notice", NoticeProperties.class).get();
        NoticeMapper mapper = session.getMapper(NoticeMapper.class);
        service = new NoticeServiceImpl(mapper, props);
        admin = new NoticeAdminServiceImpl(mapper);
        at(NOW);

        CommonStoreProperties sp = new CommonStoreProperties();
        sp.setInitSchema(true);
        sp.setSeedIfEmpty(true);
        initializer = new CommonStoreInitializer(ds, sp, List.of(productService, new NoticeSeeder(ds)));
        assertThat(initializer.initialize()).isTrue();
    }

    private void at(LocalDateTime t) {
        Clock c = Clock.fixed(t.atZone(KST).toInstant(), KST);
        service.setClock(c);
        admin.setClock(c);
    }

    private static List<Long> ids(NoticePage p) {
        return p.getItems().stream().map(NoticeDto::getNoticeId).toList();
    }

    @Test
    @DisplayName("기동 초기화: 테이블 3개 + 샘플(유형 4 · 업무 4 · 공지 8), 다시 초기화해도 중복 없음")
    void schemaAndSeed() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM TB_CO_NOTICE_TYPE", Integer.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM TB_CO_NOTICE_BIZ", Integer.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM TB_CO_NOTICE", Integer.class)).isEqualTo(8);
        initializer.initialize();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM TB_CO_NOTICE", Integer.class)).isEqualTo(8);
    }

    @Test
    @DisplayName("요구사항 2·3: 메인 띠 = 게시 중 띠 공지 중 최신 1건 (종료·예약 제외), 게시가 끝나면 다음 공지로")
    void strip() {
        NoticeDto s = service.strip();
        assertThat(s.getNoticeId()).isEqualTo(2L);                                    // 10-18 스마트대출 (10-15 시스템점검보다 최신)
        assertThat(s.getTargetNm()).isEqualTo("페퍼스마트대출");
        assertThat(s.getContent()).isNull();                                         // 띠에는 제목만

        at(LocalDateTime.of(2026, 11, 1, 0, 0));                                     // 예약 공지(7) 게시 시작
        assertThat(service.strip().getNoticeId()).isEqualTo(7L);

        jdbc.update("UPDATE TB_CO_NOTICE SET STRIP_YN='N'");
        assertThat(service.strip()).isNull();                                        // 띠 공지 없음 → 화면은 숨김
    }

    @Test
    @DisplayName("요구사항 1: 전체 / 상품별 / 업무별 구분 목록 + 칩 필터, 상단 고정 우선, 페이지")
    void listByScope() {
        assertThat(ids(service.list(null, null, null, 1))).containsExactly(1L, 2L, 5L, 3L, 4L);   // 고정(1) → 최신순
        assertThat(ids(service.list("ALL", "IGNORED", null, 1))).containsExactly(1L, 3L);        // 전체공지만 (대상 무시)
        assertThat(ids(service.list("PRODUCT", null, null, 1))).containsExactly(2L, 4L);
        assertThat(ids(service.list("PRODUCT", "PS_SMART", null, 1))).containsExactly(2L);
        assertThat(ids(service.list("BIZ", "LOAN", null, 1))).containsExactly(5L);
        assertThat(ids(service.list("BIZ", "AUTH", null, 1))).isEmpty();                         // 종료된 공지만 있음
        assertThat(ids(service.list(null, null, "SYSTEM", 1))).containsExactly(1L);

        NoticeDto first = service.list(null, null, null, 1).getItems().get(0);
        assertThat(first.getTypeNm()).isEqualTo("시스템점검");
        assertThat(first.getContent()).isNull();                                     // 목록은 내용 제외

        props.setPageSize(2);
        NoticePage p1 = service.list(null, null, null, 1);
        assertThat(p1.getTotal()).isEqualTo(5);
        assertThat(p1.isHasNext()).isTrue();
        assertThat(ids(service.list(null, null, null, 3))).containsExactly(4L);
        assertThat(service.list(null, null, null, 3).isHasNext()).isFalse();

        assertThatThrownBy(() -> service.list("ETC", null, null, 1)).isInstanceOf(NoticeException.class);

        NoticeFilters fl = service.filters();
        assertThat(fl.getAllCnt()).isEqualTo(5);
        assertThat(fl.getProducts()).extracting(NoticeTargetDto::getTargetCd).containsExactlyInAnyOrder("PS_SMART", "PS_PARKING4");
        assertThat(fl.getBizs()).extracting(NoticeTargetDto::getTargetNm).containsExactly("대출신청");
    }

    @Test
    @DisplayName("상세: 게시 중만 (+조회수), 팝업 = 게시 중 팝업 공지")
    void detailAndPopup() {
        NoticeDto d = service.detail(1);
        assertThat(d.getContent()).contains("점검일시");
        assertThat(d.getViewCnt()).isEqualTo(1);
        assertThat(service.detail(1).getViewCnt()).isEqualTo(2);
        assertThatThrownBy(() -> service.detail(6)).hasMessageContaining("게시 중");    // 종료
        assertThatThrownBy(() -> service.detail(7)).isInstanceOf(NoticeException.class); // 예약
        assertThatThrownBy(() -> service.detail(8)).isInstanceOf(NoticeException.class); // 미사용
        assertThatThrownBy(() -> service.detail(999)).isInstanceOf(NoticeException.class);

        assertThat(service.popups()).extracting(NoticeDto::getNoticeId).containsExactly(1L);
        assertThat(service.popups().get(0).getContent()).isNotBlank();                // 팝업은 내용 포함
        at(LocalDateTime.of(2026, 10, 25, 6, 0));                                     // 점검 공지 종료 시각 (미포함)
        assertThat(service.popups()).isEmpty();
    }

    @Test
    @DisplayName("ADMIN: 구분별 대상 검증, 유형 추가·미사용, 등록 즉시 띠·목록 반영, 상태 표시")
    void adminSave() {
        NoticeDto n = new NoticeDto();
        n.setScopeCd("PRODUCT");
        n.setTypeCd("BIZ");
        n.setTitle("오토론 판매 재개 안내");
        n.setContent("오토론 신규 신청을 다시 받습니다.");
        n.setStartAt(LocalDateTime.of(2026, 10, 20, 9, 30));
        assertCode(() -> admin.save(n, "admin01"), "NTC-101");                       // 상품별인데 대상 없음
        n.setTargetCd("NOPE");
        assertCode(() -> admin.save(n, "admin01"), "NTC-101");
        n.setTargetCd("PS_AUTO");
        n.setEndAt(LocalDateTime.of(2026, 10, 20, 9, 0));
        assertCode(() -> admin.save(n, "admin01"), "NTC-101");                       // 종료 < 시작
        n.setEndAt(null);
        n.setStripYn("Y");
        NoticeDto saved = admin.save(n, "admin01");
        assertThat(saved.getNoticeId()).isEqualTo(9L);
        assertThat(saved.getStatus()).isEqualTo("게시중");
        assertThat(service.strip().getNoticeId()).isEqualTo(9L);                      // 가장 최근 띠 공지

        // 전체공지는 대상이 있어도 비운다
        saved.setScopeCd("ALL");
        saved.setTargetCd("PS_AUTO");
        assertThat(admin.save(saved, "admin01").getTargetCd()).isNull();

        // 유형: TO-BE 유형 추가 → 사용 / 미사용이면 새 공지에 못 씀
        NoticeTypeDto ev = new NoticeTypeDto();
        ev.setTypeCd("EVENT");
        ev.setTypeNm("이벤트");
        ev.setBadgeColor("#b45309");
        ev.setSortOrd(5);
        assertThat(admin.saveType(ev, "admin01").getBadgeColor()).isEqualTo("#B45309");
        ev.setUseYn("N");
        admin.saveType(ev, "admin01");
        NoticeDto e = new NoticeDto();
        e.setScopeCd("BIZ");
        e.setTargetCd("DEPOSIT");
        e.setTypeCd("EVENT");
        e.setTitle("적금 이벤트");
        e.setContent("내용");
        e.setStartAt(NOW);
        assertCode(() -> admin.save(e, "admin01"), "NTC-101");

        assertThat(admin.list(null, null, null, 100)).extracting(NoticeDto::getStatus)
                .contains("게시중", "게시종료", "게시예약", "미사용");
        assertThat(admin.list("BIZ", null, "대출", 100)).extracting(NoticeDto::getNoticeId).containsExactly(5L);
        assertThat(admin.targets().getBizs()).hasSize(4);
    }

    @Test
    @DisplayName("API: 띠(상세 URL 포함) · 목록 · 상세 404 · ADMIN 등록 · 권한")
    void api() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new NoticeApiController(service, props),
                        new NoticeAdminApiController(admin, new NoticeAdminAuth(props)))
                .setControllerAdvice(new NoticeExceptionHandler()).build();
        mvc.perform(get("/api/common/notice/strip"))
                .andExpect(jsonPath("$.data.notice.noticeId").value(2))
                .andExpect(jsonPath("$.data.detailUrl").value("/common/notice/detail?id=2"));
        mvc.perform(get("/api/common/notice").param("scope", "PRODUCT"))
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.items[0].content").doesNotExist());
        mvc.perform(get("/api/common/notice/6")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NTC-002"));
        mvc.perform(post("/api/common/notice-admin/notices").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scopeCd\":\"BIZ\",\"targetCd\":\"TRANSFER\",\"typeCd\":\"SERVICE\",\"title\":\"이체 한도 상향\","
                                + "\"content\":\"하루 이체 한도가 늘어납니다.\",\"startAt\":\"2026-10-20T09:00\",\"popupYn\":\"Y\"}"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.targetNm").value("이체"));
        mvc.perform(get("/api/common/notice/popups")).andExpect(jsonPath("$.data", hasSize(2)));
        props.setAdminApiOpen(false);
        mvc.perform(get("/api/common/notice-admin/notices")).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("NTC-401"));
    }

    private static void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String code) {
        assertThatThrownBy(call).isInstanceOfSatisfying(NoticeException.class,
                e -> assertThat(e.getErrorCode().getCode()).isEqualTo(code));
    }
}
