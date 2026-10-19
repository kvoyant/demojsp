package com.example.demojsp.common.banner.service;

import com.example.demojsp.common.banner.config.BannerPopupProperties;
import com.example.demojsp.common.banner.domain.BannerCategory;
import com.example.demojsp.common.banner.domain.BannerChannel;
import com.example.demojsp.common.banner.domain.BannerDisplayList;
import com.example.demojsp.common.banner.domain.BannerPopup;
import com.example.demojsp.common.banner.domain.BannerPopupAdminView;
import com.example.demojsp.common.banner.domain.BannerPopupRequest;
import com.example.demojsp.common.banner.domain.BannerPopupType;
import com.example.demojsp.common.banner.domain.BannerPopupView;
import com.example.demojsp.common.banner.domain.TopSlideBannerList;
import com.example.demojsp.common.banner.domain.TopSlideBannerView;
import com.example.demojsp.common.banner.spi.AccountHoldingChecker;
import com.example.demojsp.common.banner.exception.BannerErrorCode;
import com.example.demojsp.common.banner.exception.BannerException;
import com.example.demojsp.common.banner.store.BannerPopupRepository;
import com.example.demojsp.common.banner.store.InMemoryBannerPopupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * [CF-BC-20 / 공통#39] 배너팝업 공통 서비스.
 *
 * <p><b>AS-IS 문제</b>: 디지털어드민에서 같은 시간대에 배너팝업을 여러 건 등록하고 표시순서를 지정해도 APP 에는 1건만 노출 →
 * 일정이 겹치는 복수 공지(일반 긴급공지, 시스템 긴급공지 등) 중 일부를 고객이 보지 못함.
 *
 * <p><b>TO-BE</b>
 * <ol>
 *   <li>게시기간이 겹치는 배너가 2건 이상이면 <b>모두</b> 노출 대상으로 조회하고 ADMIN 표시순서로 정렬
 *       ({@link BannerPopup#DISPLAY_ORDER})</li>
 *   <li>화면은 응답의 {@code slide=true} 일 때 좌우 슬라이드로 여러 건을 넘겨 확인 ({@code /js/common/banner-popup.js})</li>
 * </ol>
 *
 * <p>ADMIN 등록/수정 시 서버측 Validator 로 기간·표시순서·URL·대체텍스트를 강제한다 (CF-BC-20 구현 방안).
 */
@Service
public class BannerPopupService implements com.example.demojsp.common.store.CommonSeeder {

    private static final Logger log = LoggerFactory.getLogger(BannerPopupService.class);
    /** 감사 로그 (배너 등록·수정·삭제 이력) - 운영에서 별도 파일로 분리 권장 */
    private static final Logger audit = LoggerFactory.getLogger("AUDIT.BANNER");

    static final int MAX_TITLE = 50;
    static final int MAX_CONTENT = 1000;
    static final int MAX_ALT = 100;
    static final int MIN_ORDER = 1;
    static final int MAX_ORDER = 999;

    public static final String STATUS_ON = "게시중";
    public static final String STATUS_READY = "게시예정";
    public static final String STATUS_END = "게시종료";
    public static final String STATUS_OFF = "미사용";

    public static final String AUDIENCE_NO_ACCOUNT = "NO_ACCOUNT";
    public static final String AUDIENCE_ACCOUNT_HOLDER = "ACCOUNT_HOLDER";

    private final BannerPopupRepository repository;
    private final BannerPopupProperties properties;
    private final AccountHoldingChecker accountHoldingChecker;
    private final Clock clock;

    @Autowired
    public BannerPopupService(BannerPopupRepository repository, BannerPopupProperties properties,
                              AccountHoldingChecker accountHoldingChecker) {
        this(repository, properties, accountHoldingChecker, Clock.systemDefaultZone());
    }

    /** 테스트용 (계좌 보유 확인 없이 - 상단슬라이드배너는 모두 계좌 미보유로 간주) */
    public BannerPopupService(BannerPopupRepository repository, BannerPopupProperties properties, Clock clock) {
        this(repository, properties, customerId -> false, clock);
    }

    public BannerPopupService(BannerPopupRepository repository, BannerPopupProperties properties,
                              AccountHoldingChecker accountHoldingChecker, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.accountHoldingChecker = accountHoldingChecker;
        this.clock = clock;
        // 메모리 저장소일 때만 seed 적재 (DB 구현체에는 운영 데이터가 있으므로 넣지 않는다)
        if (repository instanceof InMemoryBannerPopupRepository && repository.findAll().isEmpty()) {
            loadSeed();
        }
    }

    // ------------------------------------------------------------------ 고객 화면 노출

    /** 현재 시각 기준 노출 대상 (고객 화면) */
    public BannerDisplayList displayable(String channel) {
        return displayable(channel, LocalDateTime.now(clock));
    }

    /**
     * 기준시각·채널의 노출 대상 배너.
     * <p>게시기간 안 + 사용 + 채널 일치 배너를 <b>전부</b> 조회 → 표시순서 정렬 → 최대 노출 건수 적용.
     */
    public BannerDisplayList displayable(String channel, LocalDateTime at) {
        BannerChannel ch = parseChannel(channel);
        List<BannerPopup> targets = repository.findAll().stream()
                .filter(b -> b.displayableAt(at, ch, BannerPopupType.BANNER_POPUP))   // 상단슬라이드배너 제외
                .sorted(BannerPopup.DISPLAY_ORDER)
                .toList();

        int max = properties.getMaxDisplayCount();
        int truncated = 0;
        if (max > 0 && targets.size() > max) {
            truncated = targets.size() - max;
            log.warn("[배너팝업] 노출 대상 {}건 중 표시순서 상위 {}건만 노출 (channel={}, at={})", targets.size(), max, ch, at);
            targets = targets.subList(0, max);
        }
        List<BannerPopupView> items = targets.stream().map(BannerPopupView::of).toList();
        return new BannerDisplayList(ch.name(), BannerSupport.formatFull(at), items.size(), items.size() >= 2, truncated, items);
    }

    // ------------------------------------------------------------------ 홈 상단슬라이드배너 (공통#44)

    /** 현재 시각 기준 상단슬라이드배너 */
    public TopSlideBannerList topSlides(String channel, String customerId) {
        return topSlides(channel, customerId, LocalDateTime.now(clock));
    }

    /**
     * 홈 상단슬라이드배너 (기존 메인 "슬라이드 배너팝업" 대체).
     * <ol>
     *   <li>정책1 제공대상: 계좌 미보유 사용자 → 계좌 보유 고객은 eligible=false, 배너 없음 (같은 영역에 계좌정보)</li>
     *   <li>정책2 배너노출기간: 시작 포함 ~ 종료 미포함 (YYYY-MM-DD 00:00:00 ~ YYYY-MM-DD 00:00:00)</li>
     *   <li>기능1 최대 5건 (표시순서 상위), 기능2 자동 슬라이딩 간격 2초 → 응답에 함께 내려 화면이 설정값을 따르게 한다</li>
     * </ol>
     */
    public TopSlideBannerList topSlides(String channel, String customerId, LocalDateTime at) {
        BannerChannel ch = parseChannel(channel);
        BannerPopupProperties.TopSlide cfg = properties.getTopSlide();
        if (accountHoldingChecker.hasAccount(customerId)) {
            return new TopSlideBannerList(ch.name(), BannerSupport.formatFull(at), false, AUDIENCE_ACCOUNT_HOLDER,
                    0, cfg.getMaxCount(), cfg.getIntervalMs(), 0, List.of());
        }
        return buildTopSlides(ch, at);
    }

    /** ADMIN 미리보기 - 기준시각의 상단슬라이드배너 (계좌 보유 여부와 무관하게 계좌 미보유 고객 화면 기준) */
    public TopSlideBannerList topSlidePreview(String channel, LocalDateTime at) {
        return buildTopSlides(parseChannel(channel), at);
    }

    private TopSlideBannerList buildTopSlides(BannerChannel ch, LocalDateTime at) {
        BannerPopupProperties.TopSlide cfg = properties.getTopSlide();
        List<BannerPopup> targets = repository.findAll().stream()
                .filter(b -> b.displayableAt(at, ch, BannerPopupType.TOP_SLIDE))
                .sorted(BannerPopup.DISPLAY_ORDER)
                .toList();
        int max = cfg.getMaxCount();
        int truncated = 0;
        if (max > 0 && targets.size() > max) {
            truncated = targets.size() - max;
            targets = targets.subList(0, max);
        }
        List<TopSlideBannerView> items = targets.stream().map(b -> {
            String bg = b.backgroundColor() != null ? b.backgroundColor() : cfg.getDefaultBackgroundColor();
            return new TopSlideBannerView(b.id(), b.title(), b.content(), b.imageUrl(), b.altText(), b.linkUrl(),
                    bg, BannerSupport.readableTextColor(bg), b.displayOrder());
        }).toList();
        return new TopSlideBannerList(ch.name(), BannerSupport.formatFull(at), true, AUDIENCE_NO_ACCOUNT,
                items.size(), max, cfg.getIntervalMs(), truncated, items);
    }

    // ------------------------------------------------------------------ ADMIN

    /** ADMIN 배너팝업목록 (전체 구분) */
    public List<BannerPopupAdminView> adminList() {
        return adminList(null);
    }

    /**
     * ADMIN 배너팝업목록 (게시상태·중복게시 정보 포함). 정렬: 게시중 → 게시예정 → 미사용 → 게시종료, 같은 상태 안에서는 표시순서
     *
     * @param popupType 배너팝업구분 필터 (null/공백 = 전체)
     */
    public List<BannerPopupAdminView> adminList(String popupType) {
        LocalDateTime now = LocalDateTime.now(clock);
        BannerPopupType type = popupType == null || popupType.isBlank() ? null
                : parseEnum(BannerPopupType.class, popupType, "배너팝업구분");
        List<BannerPopup> all = repository.findAll();
        List<BannerPopupAdminView> rows = new ArrayList<>();
        for (BannerPopup b : all) {
            if (type == null || b.popupType() == type) rows.add(toAdminView(b, all, now));
        }
        List<String> statusOrder = List.of(STATUS_ON, STATUS_READY, STATUS_OFF, STATUS_END);
        rows.sort(Comparator.<BannerPopupAdminView>comparingInt(r -> statusOrder.indexOf(r.status()))
                .thenComparingInt(BannerPopupAdminView::displayOrder)
                .thenComparing(BannerPopupAdminView::startAt, Comparator.reverseOrder())
                .thenComparingLong(BannerPopupAdminView::id));
        return rows;
    }

    public BannerPopupAdminView adminDetail(long id) {
        BannerPopup b = find(id);
        return toAdminView(b, repository.findAll(), LocalDateTime.now(clock));
    }

    public BannerPopupAdminView register(BannerPopupRequest req, String adminId) {
        LocalDateTime now = LocalDateTime.now(clock);
        BannerPopup b = validate(req, repository.nextId(), now, adminId, now, adminId);
        repository.save(b);
        audit.info("[배너팝업 등록] id={} type={} category={} channels={} period={}~{} order={} by={}",
                b.id(), b.popupType(), b.category(), b.channels(), b.startAt(), b.endAt(), b.displayOrder(), adminId);
        return adminDetail(b.id());
    }

    public BannerPopupAdminView update(long id, BannerPopupRequest req, String adminId) {
        BannerPopup old = find(id);
        BannerPopup b = validate(req, id, old.registeredAt(), old.registeredBy(), LocalDateTime.now(clock), adminId);
        repository.save(b);
        audit.info("[배너팝업 수정] id={} period={}~{} order={} use={} by={}",
                id, b.startAt(), b.endAt(), b.displayOrder(), b.use(), adminId);
        return adminDetail(id);
    }

    /** 사용여부만 변경 (목록에서 즉시 게시중지/재게시) */
    public BannerPopupAdminView changeUse(long id, boolean use, String adminId) {
        BannerPopup o = find(id);
        BannerPopup b = new BannerPopup(o.id(), o.popupType(), o.category(), o.channels(), o.title(), o.content(),
                o.imageUrl(), o.altText(), o.linkUrl(), o.backgroundColor(), o.startAt(), o.endAt(), o.displayOrder(), use,
                o.registeredAt(), o.registeredBy(), LocalDateTime.now(clock), adminId);
        repository.save(b);
        audit.info("[배너팝업 사용여부] id={} use={} by={}", id, use, adminId);
        return adminDetail(id);
    }

    public void delete(long id, String adminId) {
        if (!repository.delete(id)) {
            throw new BannerException(BannerErrorCode.BANNER_NOT_FOUND);
        }
        audit.info("[배너팝업 삭제] id={} by={}", id, adminId);
    }

    /** ADMIN 화면 선택값 (배너팝업구분 / 공지유형 / 채널) */
    public Map<String, Map<String, String>> codes() {
        Map<String, Map<String, String>> codes = new LinkedHashMap<>();
        Map<String, String> types = new LinkedHashMap<>();
        for (BannerPopupType t : BannerPopupType.values()) types.put(t.name(), t.getLabel());
        Map<String, String> categories = new LinkedHashMap<>();
        for (BannerCategory c : BannerCategory.values()) categories.put(c.name(), c.getLabel());
        Map<String, String> channels = new LinkedHashMap<>();
        for (BannerChannel c : BannerChannel.values()) channels.put(c.name(), c.getLabel());
        codes.put("popupTypes", types);
        codes.put("categories", categories);
        codes.put("channels", channels);
        // 화면 글자 수 표시·입력 제한용 (서버 검증과 같은 값)
        BannerPopupProperties.TopSlide ts = properties.getTopSlide();
        Map<String, String> limits = new LinkedHashMap<>();
        limits.put("titleMax", String.valueOf(MAX_TITLE));
        limits.put("contentMax", String.valueOf(MAX_CONTENT));
        limits.put("altMax", String.valueOf(MAX_ALT));
        limits.put("mainTextMax", String.valueOf(ts.getMainTextMax()));
        limits.put("subTextMax", String.valueOf(ts.getSubTextMax()));
        limits.put("topSlideMaxCount", String.valueOf(ts.getMaxCount()));
        limits.put("imageMaxBytes", String.valueOf(properties.getImageUpload().getMaxBytes()));
        limits.put("defaultBackgroundColor", ts.getDefaultBackgroundColor());
        codes.put("limits", limits);
        return codes;
    }

    // ------------------------------------------------------------------ 내부

    private BannerPopup find(long id) {
        return repository.findById(id).orElseThrow(() -> new BannerException(BannerErrorCode.BANNER_NOT_FOUND));
    }

    private BannerPopupAdminView toAdminView(BannerPopup b, List<BannerPopup> all, LocalDateTime now) {
        int overlap = 0;
        boolean orderConflict = false;
        for (BannerPopup o : all) {
            // 같은 구분끼리만 겹침 계산 (배너팝업과 상단슬라이드배너는 노출 영역이 다르다)
            if (o.id() != b.id() && o.popupType() == b.popupType() && o.use() && o.channelOverlaps(b) && o.periodOverlaps(b)) {
                overlap++;
                orderConflict |= o.displayOrder() == b.displayOrder();
            }
        }
        String status = status(b, now);
        Integer rank = null;
        boolean overLimit = false;
        if (b.popupType() == BannerPopupType.TOP_SLIDE && STATUS_ON.equals(status)) {
            // 노출 채널 중 가장 앞선 순번 (보통 APP 하나)
            int max = properties.getTopSlide().getMaxCount();
            for (BannerChannel ch : b.channels()) {
                List<BannerPopup> shown = all.stream()
                        .filter(o -> o.displayableAt(now, ch, BannerPopupType.TOP_SLIDE))
                        .sorted(BannerPopup.DISPLAY_ORDER)
                        .toList();
                int r = shown.indexOf(b) + 1;
                if (r > 0 && (rank == null || r < rank)) rank = r;
            }
            overLimit = rank != null && max > 0 && rank > max;
        }
        return BannerPopupAdminView.of(b, status, overlap, orderConflict, rank, overLimit);
    }

    static String status(BannerPopup b, LocalDateTime now) {
        if (!b.use()) return STATUS_OFF;
        if (now.isBefore(b.startAt())) return STATUS_READY;
        if (!now.isBefore(b.endAt())) return STATUS_END;
        return STATUS_ON;
    }

    static BannerChannel parseChannel(String channel) {
        if (channel == null || channel.isBlank()) {
            throw new BannerException(BannerErrorCode.INVALID_CHANNEL, "channel");
        }
        try {
            return BannerChannel.valueOf(channel.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BannerException(BannerErrorCode.INVALID_CHANNEL, "channel");
        }
    }

    /** 등록/수정 요청 서버측 검증 → 원장 객체 생성 */
    private BannerPopup validate(BannerPopupRequest req, long id, LocalDateTime registeredAt, String registeredBy,
                                        LocalDateTime updatedAt, String updatedBy) {
        if (req == null) {
            throw new BannerException(BannerErrorCode.INVALID_REQUEST);
        }
        BannerPopupType type = parseEnum(BannerPopupType.class,
                req.popupType() == null || req.popupType().isBlank() ? "BANNER_POPUP" : req.popupType(), "배너팝업구분");
        boolean topSlide = type == BannerPopupType.TOP_SLIDE;
        // 상단슬라이드배너는 상품소개 전용 → 공지유형 입력 없이 "이벤트·상품안내" (공통#45)
        String categoryValue = topSlide && (req.category() == null || req.category().isBlank()) ? BannerCategory.EVENT.name() : req.category();
        if (categoryValue == null || categoryValue.isBlank()) {
            throw new BannerException(BannerErrorCode.REQUIRED_MISSING, "공지유형");
        }
        BannerCategory category = parseEnum(BannerCategory.class, categoryValue, "공지유형");

        if (req.channels() == null || req.channels().isEmpty()) {
            throw new BannerException(BannerErrorCode.REQUIRED_MISSING, "노출채널");
        }
        Set<BannerChannel> channels = new LinkedHashSet<>();
        for (String c : req.channels()) {
            channels.add(parseChannel(c));
        }

        // 공통#45 기능6 상품소개 메인텍스트·서브텍스트 글자 수 제한 (화면 2줄 안에 들어가도록)
        BannerPopupProperties.TopSlide ts = properties.getTopSlide();
        String title = topSlide ? required(req.title(), "상품소개 메인텍스트", ts.getMainTextMax()) : required(req.title(), "제목", MAX_TITLE);
        String content = topSlide ? required(req.content(), "서브텍스트", ts.getSubTextMax()) : required(req.content(), "내용", MAX_CONTENT);

        String imageUrl = BannerSupport.trimToNull(req.imageUrl());
        String altText = BannerSupport.trimToNull(req.altText());
        if (imageUrl != null) {
            if (!BannerSupport.isSafeUrl(imageUrl)) throw new BannerException(BannerErrorCode.INVALID_URL, "이미지 URL");
            if (altText == null) throw new BannerException(BannerErrorCode.ALT_TEXT_REQUIRED, "대체텍스트");
        }
        if (altText != null && altText.length() > MAX_ALT) {
            throw new BannerException(BannerErrorCode.TOO_LONG, "대체텍스트(" + MAX_ALT + "자)");
        }
        String linkUrl = BannerSupport.trimToNull(req.linkUrl());
        if (linkUrl != null && !BannerSupport.isSafeUrl(linkUrl)) {
            throw new BannerException(BannerErrorCode.INVALID_URL, "링크 URL");
        }
        // 배경 색상: 화면 style 에 들어가므로 #RRGGBB 만 허용 (CSS 주입 차단). 배너팝업은 사용하지 않는다
        String bgColor = BannerSupport.trimToNull(req.backgroundColor());
        if (bgColor != null && !BannerSupport.isHexColor(bgColor)) {
            throw new BannerException(BannerErrorCode.INVALID_COLOR, "배경 색상");
        }
        bgColor = topSlide && bgColor != null ? bgColor.toUpperCase(Locale.ROOT) : null;

        LocalDateTime start = BannerSupport.parse(req.startAt(), "게시 시작일시");
        LocalDateTime end = BannerSupport.parse(req.endAt(), "게시 종료일시");
        if (!end.isAfter(start)) {
            throw new BannerException(BannerErrorCode.INVALID_PERIOD, "게시기간");
        }
        if (req.displayOrder() == null) {
            throw new BannerException(BannerErrorCode.REQUIRED_MISSING, "표시순서");
        }
        int order = req.displayOrder();
        if (order < MIN_ORDER || order > MAX_ORDER) {
            throw new BannerException(BannerErrorCode.INVALID_ORDER, "표시순서");
        }
        boolean use = req.use() == null || req.use();

        return new BannerPopup(id, type, category, List.copyOf(channels), title, content, imageUrl, altText, linkUrl,
                bgColor, start, end, order, use, registeredAt, registeredBy, updatedAt, updatedBy);
    }

    private static String required(String value, String field, int max) {
        String v = BannerSupport.trimToNull(value);
        if (v == null) throw new BannerException(BannerErrorCode.REQUIRED_MISSING, field);
        if (v.length() > max) throw new BannerException(BannerErrorCode.TOO_LONG, field + "(" + max + "자)");
        return v;
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, String field) {
        try {
            return Enum.valueOf(type, value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BannerException(BannerErrorCode.INVALID_CODE, field);
        }
    }

    @Override
    public String seedName() {
        return "배너팝업";
    }

    /** DB 저장소: 배너팝업 테이블이 비어 있으면 yaml seed 적재 (CommonStoreInitializer 가 기동 후 호출) */
    @Override
    public void seedIfEmpty() {
        if (repository.findAll().isEmpty()) {
            loadSeed();
            log.info("[배너팝업] seed 적재 완료");
        }
    }

    /** yaml seed → 저장소. 상대 일수(start/end-day-offset)는 기동일 00:00 기준 */
    private void loadSeed() {
        LocalDateTime today = LocalDate.now(clock).atStartOfDay();
        LocalDateTime now = LocalDateTime.now(clock);
        for (BannerPopupProperties.Seed s : properties.getSeed()) {
            String start = s.getStartAt() != null ? s.getStartAt()
                    : BannerSupport.format(today.plusDays(s.getStartDayOffset() == null ? 0 : s.getStartDayOffset()));
            String end = s.getEndAt() != null ? s.getEndAt()
                    : BannerSupport.format(today.plusDays(s.getEndDayOffset() == null ? 7 : s.getEndDayOffset()));
            BannerPopupRequest req = new BannerPopupRequest(s.getPopupType(), s.getCategory(), s.getChannels(),
                    s.getTitle(), s.getContent(), s.getImageUrl(), s.getAltText(), s.getLinkUrl(),
                    start, end, s.getDisplayOrder(), s.isUse(), s.getBackgroundColor());
            try {
                repository.save(validate(req, repository.nextId(), now, "SYSTEM", now, "SYSTEM"));
            } catch (BannerException e) {
                log.warn("[배너팝업] seed 적재 제외 title={} 사유={}", s.getTitle(), e.getMessage());
            }
        }
    }
}
