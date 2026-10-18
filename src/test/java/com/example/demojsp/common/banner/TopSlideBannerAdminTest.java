package com.example.demojsp.common.banner;

import com.example.demojsp.common.banner.config.BannerPopupProperties;
import com.example.demojsp.common.banner.service.BannerPopupService;
import com.example.demojsp.common.banner.store.BannerImageStore;
import com.example.demojsp.common.banner.store.InMemoryBannerPopupRepository;
import com.example.demojsp.common.banner.web.BannerApiExceptionHandler;
import com.example.demojsp.common.banner.web.BannerImageController;
import com.example.demojsp.common.banner.web.BannerPopupAdminApiController;
import com.example.demojsp.common.banner.web.BannerRequestContext;
import com.example.demojsp.common.banner.web.TopSlideBannerApiController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [CF-BC-20 / 공통#45] 상단슬라이드배너 ADMIN 수정개발 테스트 (standalone MockMvc - DB 불필요).
 *
 * <p>기준시각 고정 2026-10-20 10:00 (Asia/Seoul). 이미지 저장 폴더는 테스트 임시 폴더.
 */
class TopSlideBannerAdminTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-20T01:00:00Z"), KST);

    @TempDir
    Path tmp;

    private BannerPopupProperties props;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        props = new BannerPopupProperties();
        props.setAdminApiOpen(true);
        props.getImageUpload().setDir(tmp.toString());
        props.getImageUpload().setMaxBytes(50_000);
        BannerPopupService service = new BannerPopupService(new InMemoryBannerPopupRepository(), props, CLOCK);
        BannerRequestContext context = new BannerRequestContext(props);
        BannerImageStore store = new BannerImageStore(props);
        mvc = MockMvcBuilders.standaloneSetup(
                        new BannerPopupAdminApiController(service, context, store),
                        new BannerImageController(store),
                        new TopSlideBannerApiController(service, context))
                .setControllerAdvice(new BannerApiExceptionHandler())
                .build();
    }

    private static String slide(String main, String sub, String bg, String image, String alt, int order) {
        return "{\"popupType\":\"TOP_SLIDE\",\"channels\":[\"APP\"],\"title\":\"" + main + "\",\"content\":\"" + sub + "\","
                + (bg == null ? "" : "\"backgroundColor\":\"" + bg + "\",")
                + (image == null ? "" : "\"imageUrl\":\"" + image + "\",")
                + (alt == null ? "" : "\"altText\":\"" + alt + "\",")
                + "\"linkUrl\":\"/product/loan/PS_SMART\",\"startAt\":\"2026-10-20 00:00\",\"endAt\":\"2026-11-01 00:00\",\"displayOrder\":" + order + "}";
    }

    private static byte[] png() throws IOException {
        BufferedImage img = new BufferedImage(144, 144, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    @Test
    @DisplayName("기능1·4·5·6: 상단슬라이드배너 등록 - 공지유형 없이 EVENT, 배경색 대문자 정규화, 링크, 메인/서브텍스트")
    void registerTopSlide() throws Exception {
        mvc.perform(post("/api/common/banner-admin/popups").contentType(MediaType.APPLICATION_JSON)
                        .content(slide("3분이면 끝나는 한도조회", "페퍼스마트대출 · 신용점수 영향 없이", "#0d47a1", null, null, 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.popupType").value("TOP_SLIDE"))
                .andExpect(jsonPath("$.data.popupTypeName").value("상단슬라이드배너"))
                .andExpect(jsonPath("$.data.category").value("EVENT"))
                .andExpect(jsonPath("$.data.backgroundColor").value("#0D47A1"))
                .andExpect(jsonPath("$.data.linkUrl").value("/product/loan/PS_SMART"))
                .andExpect(jsonPath("$.data.displayRank").value(1))
                .andExpect(jsonPath("$.data.overLimit").value(false));
        // 고객 화면 반영 + 글자색 자동
        mvc.perform(get("/api/common/top-slide-banners"))
                .andExpect(jsonPath("$.data.items[0].mainText").value("3분이면 끝나는 한도조회"))
                .andExpect(jsonPath("$.data.items[0].textColor").value("#FFFFFF"));
    }

    @Test
    @DisplayName("기능6 글자 수 제한: 메인 20자·서브 30자 초과 BNR-106 (배너팝업 제목 50자 규칙과 별도)")
    void textLimits() throws Exception {
        String main21 = "가".repeat(21);
        String sub31 = "나".repeat(31);
        mvc.perform(post("/api/common/banner-admin/popups").contentType(MediaType.APPLICATION_JSON)
                        .content(slide(main21, "서브", null, null, null, 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BNR-106"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("상품소개 메인텍스트(20자)")));
        mvc.perform(post("/api/common/banner-admin/popups").contentType(MediaType.APPLICATION_JSON)
                        .content(slide("메인", sub31, null, null, null, 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("서브텍스트(30자)")));
        mvc.perform(post("/api/common/banner-admin/popups").contentType(MediaType.APPLICATION_JSON)
                        .content(slide("가".repeat(20), "나".repeat(30), null, null, null, 1)))
                .andExpect(status().isOk());
        // 화면 카운터용 제한값 제공
        mvc.perform(get("/api/common/banner-admin/codes"))
                .andExpect(jsonPath("$.data.popupTypes.TOP_SLIDE").value("상단슬라이드배너"))
                .andExpect(jsonPath("$.data.limits.mainTextMax").value("20"))
                .andExpect(jsonPath("$.data.limits.subTextMax").value("30"));
    }

    @Test
    @DisplayName("기능5 배경 색상: #RRGGBB 외(이름·rgb()·CSS 주입) BNR-108")
    void colorValidation() throws Exception {
        for (String bad : new String[]{"red", "#FFF", "rgb(0,0,0)", "#000000;background:url(x)"}) {
            mvc.perform(post("/api/common/banner-admin/popups").contentType(MediaType.APPLICATION_JSON)
                            .content(slide("메인", "서브", bad, null, null, 1)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("BNR-108"));
        }
    }

    @Test
    @DisplayName("기능2·3 이미지 파일선택: PNG 업로드 → URL·크기 반환 → 조회(nosniff) / 이미지 등록 시 대체텍스트 필수")
    void uploadImageAndAltRequired() throws Exception {
        String body = mvc.perform(multipart("/api/common/banner-admin/images")
                        .file(new MockMultipartFile("file", "../../evil name.png", "image/png", png())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contentType").value("image/png"))
                .andExpect(jsonPath("$.data.width").value(144))
                .andExpect(jsonPath("$.data.url").value(startsWith("/common/banner/images/")))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String url = body.replaceAll(".*\"url\":\"([^\"]+)\".*", "$1");
        assertThat(url).matches("/common/banner/images/[a-f0-9]{32}\\.png");   // 원본 파일명·경로 미사용

        mvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));

        mvc.perform(post("/api/common/banner-admin/popups").contentType(MediaType.APPLICATION_JSON)
                        .content(slide("메인", "서브", null, url, null, 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BNR-105"));
        mvc.perform(post("/api/common/banner-admin/popups").contentType(MediaType.APPLICATION_JSON)
                        .content(slide("메인", "서브", null, url, "대출 카드 그림", 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imageUrl").value(url))
                .andExpect(jsonPath("$.data.altText").value("대출 카드 그림"));
    }

    @Test
    @DisplayName("이미지 보안: SVG·HTML·확장자 위장 BNR-109, 용량 초과 BNR-110, 경로 조작 조회 차단")
    void imageSecurity() throws Exception {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>".getBytes(StandardCharsets.UTF_8);
        mvc.perform(multipart("/api/common/banner-admin/images").file(new MockMultipartFile("file", "a.svg", "image/svg+xml", svg)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BNR-109"));
        mvc.perform(multipart("/api/common/banner-admin/images").file(new MockMultipartFile("file", "fake.png", "image/png",
                        "<html><script>x</script></html>".getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BNR-109"));
        mvc.perform(multipart("/api/common/banner-admin/images").file(new MockMultipartFile("file", "big.png", "image/png", new byte[60_000])))
                .andExpect(status().isPayloadTooLarge()).andExpect(jsonPath("$.code").value("BNR-110"));
        mvc.perform(get("/common/banner/images/..%2F..%2Fetc%2Fpasswd")).andExpect(status().is4xxClientError());
        mvc.perform(get("/common/banner/images/0123456789abcdef0123456789abcdef.png"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("BNR-111"));
    }

    @Test
    @DisplayName("목록: 구분 필터 + 게시중 6건이면 6번째는 overLimit(최대 5건 초과) / 기준시각 미리보기 5건")
    void listFilterAndOverLimit() throws Exception {
        for (int i = 1; i <= 6; i++) {
            mvc.perform(post("/api/common/banner-admin/popups").contentType(MediaType.APPLICATION_JSON)
                    .content(slide("배너" + i, "서브" + i, "#E3ECFA", null, null, i))).andExpect(status().isOk());
        }
        mvc.perform(post("/api/common/banner-admin/popups").contentType(MediaType.APPLICATION_JSON)
                .content("{\"category\":\"NOTICE\",\"channels\":[\"APP\"],\"title\":\"공지\",\"content\":\"본문\",\"startAt\":\"2026-10-20 00:00\",\"endAt\":\"2026-11-01 00:00\",\"displayOrder\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayRank").doesNotExist());

        mvc.perform(get("/api/common/banner-admin/popups").param("popupType", "TOP_SLIDE"))
                .andExpect(jsonPath("$.data.length()").value(6))
                .andExpect(jsonPath("$.data[*].popupType", everyItem(is("TOP_SLIDE"))))
                .andExpect(jsonPath("$.data[5].title").value("배너6"))
                .andExpect(jsonPath("$.data[5].displayRank").value(6))
                .andExpect(jsonPath("$.data[5].overLimit").value(true))
                .andExpect(jsonPath("$.data[4].overLimit").value(false));
        mvc.perform(get("/api/common/banner-admin/popups").param("popupType", "BANNER_POPUP"))
                .andExpect(jsonPath("$.data.length()").value(1));

        mvc.perform(get("/api/common/banner-admin/top-slide-preview").param("at", "2026-10-25 09:00"))
                .andExpect(jsonPath("$.data.total").value(5))
                .andExpect(jsonPath("$.data.truncated").value(1));
        mvc.perform(get("/api/common/banner-admin/top-slide-preview").param("at", "2026-11-01 00:00"))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @DisplayName("수정: 배너팝업 → 상단슬라이드배너 구분 변경 시 새 규칙(글자 수) 적용, 배너팝업은 배경색 저장 안 함")
    void typeSwitchRules() throws Exception {
        String id = mvc.perform(post("/api/common/banner-admin/popups").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"NOTICE\",\"channels\":[\"APP\"],\"title\":\"" + "공".repeat(40) + "\",\"content\":\"본문\","
                                + "\"backgroundColor\":\"#123456\",\"startAt\":\"2026-10-20 00:00\",\"endAt\":\"2026-11-01 00:00\",\"displayOrder\":1}"))
                .andExpect(jsonPath("$.data.backgroundColor").doesNotExist())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8).replaceAll(".*\"id\":(\\d+).*", "$1");
        mvc.perform(put("/api/common/banner-admin/popups/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(slide("공".repeat(40), "서브", null, null, null, 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BNR-106"));
    }
}
