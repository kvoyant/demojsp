package com.example.demojsp.common.producttab.service;

import com.example.demojsp.common.banner.service.BannerSupport;
import com.example.demojsp.common.producttab.config.ProductTabProperties;
import com.example.demojsp.common.producttab.dto.ProductTabView;
import com.example.demojsp.common.producttab.dto.PtItemDto;
import com.example.demojsp.common.producttab.dto.PtSectionDto;
import com.example.demojsp.common.producttab.dto.PtSlideDto;
import com.example.demojsp.common.producttab.exception.ProductTabErrorCode;
import com.example.demojsp.common.producttab.exception.ProductTabException;
import com.example.demojsp.common.producttab.mapper.ProductTabMapper;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ProductTabServiceImpl implements ProductTabService {

    /** 상품 탭을 쓰는 채널 (요구사항: APP / PC Web) */
    static final Set<String> CHANNELS = Set.of("APP", "PC_WEB");

    private final ProductTabMapper mapper;
    private final ProductTabProperties properties;
    private Clock clock = Clock.systemDefaultZone();

    public ProductTabServiceImpl(ProductTabMapper mapper, ProductTabProperties properties) {
        this.mapper = mapper;
        this.properties = properties;
    }

    /** 테스트용 시계 교체 */
    void setClock(Clock clock) {
        this.clock = clock;
    }

    @Override
    public ProductTabView getView(String channel) {
        return getView(channel, LocalDate.now(clock));
    }

    @Override
    public ProductTabView getView(String channel, LocalDate baseDt) {
        if (channel == null || !CHANNELS.contains(channel)) {
            throw new ProductTabException(ProductTabErrorCode.INVALID_REQUEST, "채널은 APP 또는 PC_WEB 입니다.");
        }
        ProductTabView view = new ProductTabView();
        view.setChannel(channel);
        view.setBaseDt(baseDt);

        // ① 상단 슬라이드 카드 - SQL 이 기간·사용·판매중을 거르고, 여기서는 채널과 최대 장수만 본다
        int max = properties.getMaxSlides();
        for (PtSlideDto s : mapper.selectActiveSlides(baseDt)) {
            if (!hasChannel(s.getChannels(), channel)) continue;
            if (max > 0 && view.getSlides().size() >= max) break;
            view.getSlides().add(toScreen(s));
        }

        // ② 섹션 + 상품 - 상품은 한 번에 읽어서 섹션코드별로 나눈다 (섹션 수만큼 SQL 을 반복하지 않음)
        Map<String, List<PtItemDto>> itemsBySection = new LinkedHashMap<>();
        for (PtItemDto item : mapper.selectActiveItems()) {
            itemsBySection.computeIfAbsent(item.getSectionCd(), k -> new java.util.ArrayList<>()).add(toScreen(item));
        }
        for (PtSectionDto sec : mapper.selectActiveSections()) {
            if (PtSectionDto.TYPE_PRODUCT.equals(sec.getSectionType())) {
                List<PtItemDto> items = itemsBySection.getOrDefault(sec.getSectionCd(), List.of());
                if (items.isEmpty()) continue;            // 보여줄 상품이 없는 섹션은 제목도 숨긴다
                sec.setItems(items);
            }
            sec.setUseYn(null);                           // 화면에 필요 없는 관리 항목은 응답에서 뺀다
            sec.setSortOrd(null);
            view.getSections().add(sec);
        }
        return view;
    }

    // ---------------------------------------------------------------- 화면용 변환

    /** 슬라이드 → 화면 응답: 이동 URL 결정, 글자색 계산, 관리용 항목 제거 */
    private static PtSlideDto toScreen(PtSlideDto s) {
        PtSlideDto v = new PtSlideDto();
        v.setSlideId(s.getSlideId());
        v.setTitle(s.getTitle());
        v.setSubText(s.getSubText());
        v.setBadgeText(s.getBadgeText());
        v.setBgColor(s.getBgColor());
        v.setTextColor(BannerSupport.readableTextColor(s.getBgColor()));   // 어떤 배경색이든 읽을 수 있는 글자색 (공통#45 와 같은 규칙)
        v.setImageUrl(s.getImageUrl());
        v.setImageAlt(s.getImageAlt());
        v.setProductCd(s.getProductCd());
        // 상품이 연결돼 있으면 상품 원장의 상품안내 URL 을 따른다 → ADMIN 상품설정에서 URL 을 바꾸면 카드도 같이 바뀜
        v.setLinkUrl(s.getProductCd() != null ? s.getProductUrl() : s.getLinkUrl());
        return v;
    }

    private static PtItemDto toScreen(PtItemDto i) {
        PtItemDto v = new PtItemDto(i.getProductCd(), null, i.getBadgeText(), null);
        v.setSectionCd(i.getSectionCd());
        v.setProductNm(i.getProductNm());
        v.setProductKind(i.getProductKind());
        v.setSubType(i.getSubType());
        v.setSummary(i.getSummary());
        v.setRateText(i.getRateText());
        v.setIcon(i.getIcon());
        v.setDetailUrl(i.getDetailUrl());
        return v;
    }

    /** "APP,PC_WEB" 에 channel 이 있는지 */
    static boolean hasChannel(String channels, String channel) {
        return channels != null && Arrays.stream(channels.split(",")).map(String::strip).anyMatch(channel::equals);
    }
}
