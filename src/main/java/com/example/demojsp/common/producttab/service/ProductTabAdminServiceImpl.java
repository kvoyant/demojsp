package com.example.demojsp.common.producttab.service;

import com.example.demojsp.common.banner.service.BannerSupport;
import com.example.demojsp.common.producttab.dto.PtItemDto;
import com.example.demojsp.common.producttab.dto.PtProductDto;
import com.example.demojsp.common.producttab.dto.PtSectionDto;
import com.example.demojsp.common.producttab.dto.PtSlideDto;
import com.example.demojsp.common.producttab.exception.ProductTabErrorCode;
import com.example.demojsp.common.producttab.exception.ProductTabException;
import com.example.demojsp.common.producttab.mapper.ProductTabMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ProductTabAdminServiceImpl implements ProductTabAdminService {

    /** 섹션코드: 영문 대문자로 시작, 대문자·숫자·밑줄 2~30자 */
    private static final Pattern CODE = Pattern.compile("^[A-Z][A-Z0-9_]{1,29}$");
    /** 공통 아이콘 이름 (ui-icons.svg 의 ic-이름) */
    private static final Pattern ICON = Pattern.compile("^[a-z][a-z0-9-]{0,39}$");

    private final ProductTabMapper mapper;
    private Clock clock = Clock.systemDefaultZone();

    public ProductTabAdminServiceImpl(ProductTabMapper mapper) {
        this.mapper = mapper;
    }

    void setClock(Clock clock) {
        this.clock = clock;
    }

    // ================================================================ 상단 슬라이드 카드

    @Override
    public List<PtSlideDto> slides() {
        return mapper.selectSlides();
    }

    @Override
    @Transactional
    public PtSlideDto saveSlide(PtSlideDto s, String adminId) {
        if (s == null) throw new ProductTabException(ProductTabErrorCode.INVALID_REQUEST);
        // ---- 문구 (요구사항 3: 디자인·콘텐츠 형식 → 카드 한 장에 들어가는 길이로 제한)
        s.setTitle(text(s.getTitle(), "메인 문구", 30, true));
        s.setSubText(text(s.getSubText(), "서브 문구", 40, false));
        s.setBadgeText(text(s.getBadgeText(), "배지", 10, false));
        if (!BannerSupport.isHexColor(s.getBgColor())) {
            throw invalid("배경색은 #RRGGBB 형식으로 입력하세요.");
        }
        s.setBgColor(s.getBgColor().toUpperCase());
        // ---- 이미지: 경로 검사 + 이미지가 있으면 대체텍스트 필수 (접근성)
        s.setImageUrl(text(s.getImageUrl(), "이미지 URL", 500, false));
        s.setImageAlt(text(s.getImageAlt(), "이미지 대체텍스트", 100, false));
        if (s.getImageUrl() != null && !safeUrl(s.getImageUrl())) throw invalid("이미지 URL 은 / 로 시작하는 경로 또는 https:// 주소만 입력할 수 있습니다.");
        if (s.getImageUrl() != null && s.getImageAlt() == null) throw invalid("이미지가 있으면 대체텍스트를 입력하세요.");
        // ---- 이동: 상품 연결 또는 링크 URL 중 하나는 있어야 한다
        s.setProductCd(text(s.getProductCd(), "연결 상품", 30, false));
        s.setLinkUrl(text(s.getLinkUrl(), "이동 URL", 500, false));
        if (s.getProductCd() != null && productCodes().stream().noneMatch(s.getProductCd()::equals)) {
            throw invalid("등록되지 않은 상품코드입니다. (" + s.getProductCd() + ")");
        }
        if (s.getProductCd() == null && s.getLinkUrl() == null) throw invalid("연결 상품 또는 이동 URL 을 입력하세요.");
        if (s.getLinkUrl() != null && !safeUrl(s.getLinkUrl())) throw invalid("이동 URL 은 / 로 시작하는 경로 또는 https:// 주소만 입력할 수 있습니다.");
        // ---- 채널 · 게시기간 · 순서
        s.setChannels(channels(s.getChannels()));
        if (s.getStartDt() == null || s.getEndDt() == null) throw invalid("게시 시작일과 종료일을 입력하세요.");
        if (s.getEndDt().isBefore(s.getStartDt())) throw invalid("게시 종료일은 시작일과 같거나 이후여야 합니다.");
        s.setSortOrd(order(s.getSortOrd()));
        s.setUseYn(yn(s.getUseYn() == null ? "Y" : s.getUseYn(), "사용여부"));
        s.setUpdAt(now());
        s.setUpdBy(adminId);

        if (s.getSlideId() == null) {
            mapper.insertSlide(s);                         // slideId 자동 채번 (AUTO_INCREMENT)
        } else if (mapper.updateSlide(s) == 0) {
            throw new ProductTabException(ProductTabErrorCode.SLIDE_NOT_FOUND);
        }
        return mapper.selectSlide(s.getSlideId());
    }

    // ================================================================ 섹션

    @Override
    public List<PtSectionDto> sections() {
        return mapper.selectSections();
    }

    @Override
    @Transactional
    public PtSectionDto saveSection(PtSectionDto s, String adminId) {
        if (s == null) throw new ProductTabException(ProductTabErrorCode.INVALID_REQUEST);
        if (s.getSectionCd() == null || !CODE.matcher(s.getSectionCd()).matches()) {
            throw invalid("섹션코드는 영문 대문자로 시작하는 대문자·숫자·_ 2~30자로 입력하세요.");
        }
        s.setSectionNm(text(s.getSectionNm(), "섹션 제목", 30, true));
        s.setSectionDesc(text(s.getSectionDesc(), "섹션 설명", 60, false));
        if (!PtSectionDto.TYPE_PRODUCT.equals(s.getSectionType()) && !PtSectionDto.TYPE_CARD.equals(s.getSectionType())) {
            throw invalid("섹션 유형은 PRODUCT(상품목록) 또는 CARD(서비스 카드) 입니다.");
        }
        s.setCardIcon(text(s.getCardIcon(), "아이콘", 40, false));
        s.setLinkUrl(text(s.getLinkUrl(), "이동 URL", 500, false));
        if (PtSectionDto.TYPE_CARD.equals(s.getSectionType())) {
            // 서비스 카드는 눌렀을 때 갈 곳이 있어야 한다
            if (s.getLinkUrl() == null || !safeUrl(s.getLinkUrl())) throw invalid("서비스 카드는 이동 URL(/ 또는 https://)이 필요합니다.");
            if (s.getCardIcon() != null && !ICON.matcher(s.getCardIcon()).matches()) throw invalid("아이콘 이름은 영문 소문자·숫자·- 입니다. (예: calculator)");
        } else {
            s.setCardIcon(null);                           // 상품목록 섹션은 아이콘·URL 을 쓰지 않는다
            s.setLinkUrl(null);
        }
        s.setSortOrd(order(s.getSortOrd()));
        s.setUseYn(yn(s.getUseYn() == null ? "Y" : s.getUseYn(), "사용여부"));
        s.setUpdAt(now());
        s.setUpdBy(adminId);
        mapper.mergeSection(s);
        return mapper.selectSection(s.getSectionCd());
    }

    // ================================================================ 섹션별 상품

    @Override
    public List<PtItemDto> sectionItems(String sectionCd) {
        requireProductSection(sectionCd);
        return mapper.selectSectionItems(sectionCd);
    }

    @Override
    @Transactional
    public List<PtItemDto> saveSectionItems(String sectionCd, List<PtItemDto> items, String adminId) {
        requireProductSection(sectionCd);
        if (items == null) throw new ProductTabException(ProductTabErrorCode.INVALID_REQUEST);
        Set<String> known = productCodes();
        Set<String> seen = new HashSet<>();
        // 1) 전부 검사한 다음 저장한다 - 하나라도 틀리면 기존 목록은 그대로
        for (PtItemDto i : items) {
            if (i == null || i.getProductCd() == null || !known.contains(i.getProductCd())) {
                throw invalid("등록되지 않은 상품코드입니다. (" + (i == null ? "" : i.getProductCd()) + ")");
            }
            if (!seen.add(i.getProductCd())) throw invalid("같은 상품을 한 섹션에 두 번 넣을 수 없습니다. (" + i.getProductCd() + ")");
            i.setSortOrd(order(i.getSortOrd()));
            i.setBadgeText(text(i.getBadgeText(), "배지", 10, false));
            i.setUseYn(yn(i.getUseYn() == null ? "Y" : i.getUseYn(), "사용여부"));
        }
        // 2) 지우고 다시 넣기 (섹션 하나의 목록은 몇 건 안 되므로 가장 단순한 방법)
        mapper.deleteSectionItems(sectionCd);
        LocalDateTime now = now();
        for (PtItemDto i : items) {
            i.setSectionCd(sectionCd);
            i.setUpdAt(now);
            i.setUpdBy(adminId);
            mapper.insertSectionItem(i);
        }
        return mapper.selectSectionItems(sectionCd);
    }

    @Override
    public List<PtProductDto> products() {
        return mapper.selectProducts();
    }

    // ================================================================ 검증 도우미

    private void requireProductSection(String sectionCd) {
        PtSectionDto s = sectionCd == null ? null : mapper.selectSection(sectionCd);
        if (s == null) throw new ProductTabException(ProductTabErrorCode.SECTION_NOT_FOUND);
        if (!PtSectionDto.TYPE_PRODUCT.equals(s.getSectionType())) throw invalid("서비스 카드 섹션에는 상품을 넣을 수 없습니다.");
    }

    private Set<String> productCodes() {
        return mapper.selectProducts().stream().map(PtProductDto::getProductCd).collect(Collectors.toSet());
    }

    /** "APP, PC_WEB" → "APP,PC_WEB" (허용값만, 순서 고정) */
    private static String channels(String value) {
        if (value == null || value.isBlank()) throw invalid("노출채널을 하나 이상 선택하세요.");
        Set<String> picked = new LinkedHashSet<>();
        for (String c : value.split(",")) {
            String v = c.strip();
            if (!ProductTabServiceImpl.CHANNELS.contains(v)) throw invalid("노출채널은 APP, PC_WEB 입니다.");
            picked.add(v);
        }
        return Arrays.stream(new String[]{"APP", "PC_WEB"}).filter(picked::contains).collect(Collectors.joining(","));
    }

    private static int order(Integer value) {
        if (value == null || value < 1 || value > 99) throw invalid("표시순서는 1~99 사이 숫자로 입력하세요.");
        return value;
    }

    private static String text(String value, String label, int max, boolean required) {
        String v = value == null ? null : value.strip();
        if (v == null || v.isEmpty()) {
            if (required) throw invalid(label + "을(를) 입력하세요.");
            return null;
        }
        if (v.length() > max) throw invalid(label + "은(는) " + max + "자 이내로 입력하세요.");
        return v;
    }

    private static String yn(String value, String label) {
        if (!"Y".equals(value) && !"N".equals(value)) throw invalid(label + "는 Y 또는 N 입니다.");
        return value;
    }

    /** 같은 사이트 경로(/...) 또는 https:// 만 허용 (javascript: 등 차단) */
    private static boolean safeUrl(String url) {
        return url.startsWith("https://") || (url.startsWith("/") && !url.startsWith("//"));
    }

    private static ProductTabException invalid(String message) {
        return new ProductTabException(ProductTabErrorCode.INVALID_VALUE, message);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }
}
