package com.example.demojsp.common.resume.service;

import com.example.demojsp.common.resume.config.ResumeProperties;
import com.example.demojsp.common.resume.domain.Partner;
import com.example.demojsp.common.resume.domain.ProductType;
import com.example.demojsp.common.resume.domain.ResumeCardView;
import com.example.demojsp.common.resume.domain.ResumePoint;
import com.example.demojsp.common.resume.domain.ResumePointRequest;
import com.example.demojsp.common.resume.domain.ResumeScope;
import com.example.demojsp.common.resume.domain.ResumeStatus;
import com.example.demojsp.common.resume.domain.ResumeSummary;
import com.example.demojsp.common.resume.exception.ResumeErrorCode;
import com.example.demojsp.common.resume.exception.ResumeException;
import com.example.demojsp.common.resume.store.InMemoryResumeRepository;
import com.example.demojsp.common.resume.store.PartnerRepository;
import com.example.demojsp.common.resume.store.ResumeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * [CF-BC-13 / 공통#43] 이어하기(Resume) 공통 서비스.
 *
 * <p><b>AS-IS 문제</b>: 이어하기가 메인 화면을 덮는 팝업으로 떠서 뒷 화면을 가리고, 진행 중인 건이 몇 건인지 알 수 없음.
 *
 * <p><b>TO-BE (공통#43 홈 - 이어하기)</b>
 * <ol>
 *   <li>이어하기 전체 건 중 <b>최신 1건</b>만 카드로 노출 ({@link ResumePoint#LATEST_FIRST})</li>
 *   <li>이어하기 <b>건수 숫자</b> 표시</li>
 *   <li>[더보기] → 신청현황조회 화면 ({@code common.resume.status-url})</li>
 *   <li>[이어하기] → 해당 상품 신청 링크 (서버가 본인·진행중·만료 전을 다시 확인한 뒤 URL 제공)</li>
 *   <li>카드 구조: 로고 + 회사명 + 상품명 + [이어하기]. 회사명·로고는 회사 마스터({@link PartnerRepository})에서 조회</li>
 *   <li>위치: 화면 상단 (팝업이 아닌 화면 안 카드 → 뒷 화면 가림 해소) - /js/common/resume-card.js</li>
 * </ol>
 *
 * <p>홈(전체) / 저축(수신, 공통#49) / 대출(여신, 공통#60) 은 {@link ResumeScope} 만 바꿔 같은 서비스를 쓴다.
 * <p><b>공통#49 저축 - 이어하기</b>: scope=DEPOSIT → 수신상품 이어하기 중 최신 1건 + 수신 건수 + [더보기](신청현황조회 수신 탭)
 * + [이어하기]. 수신상품은 당행 상품만 등록 가능(당행 로고).
 */
@Service
public class ResumeService {

    private static final Logger log = LoggerFactory.getLogger(ResumeService.class);
    /** 감사 로그 (이어하기 진입·등록·종료) */
    private static final Logger audit = LoggerFactory.getLogger("AUDIT.RESUME");

    private static final DateTimeFormatter MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter FULL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern CODE = Pattern.compile("^[A-Z0-9_]{1,30}$");
    /** 상대경로: "/" 로 시작, "//"(외부 도메인) · 공백 · 따옴표 · 꺾쇠 · 역슬래시 불가 */
    private static final Pattern RELATIVE = Pattern.compile("^/(?!/)[^\\s<>\"'\\\\]*$");
    /** 딥링크: scheme://경로 (javascript:, data: 등은 "//" 가 없어 통과 불가 + 허용 접두어로 한 번 더 제한) */
    private static final Pattern DEEP_LINK = Pattern.compile("^[a-z][a-z0-9+.\\-]*://[^\\s<>\"'\\\\]*$");

    static final int MAX_BIZ_NAME = 20;
    static final int MAX_PRODUCT_NAME = 40;
    static final int MAX_STEP_NAME = 20;
    static final int MAX_URL = 500;

    private final ResumeRepository repository;
    private final PartnerRepository partnerRepository;
    private final ResumeProperties properties;
    private final Clock clock;

    @Autowired
    public ResumeService(ResumeRepository repository, PartnerRepository partnerRepository, ResumeProperties properties) {
        this(repository, partnerRepository, properties, Clock.systemDefaultZone());
    }

    public ResumeService(ResumeRepository repository, PartnerRepository partnerRepository, ResumeProperties properties,
                         Clock clock) {
        this.repository = repository;
        this.partnerRepository = partnerRepository;
        this.properties = properties;
        this.clock = clock;
        if (repository instanceof InMemoryResumeRepository && repository.findAll().isEmpty()) {
            loadSeed();
        }
    }

    // ------------------------------------------------------------------ 고객 화면

    /** 이어하기 카드 (현재 시각) */
    public ResumeSummary summary(String customerId, String scope) {
        return summary(customerId, parseScope(scope), LocalDateTime.now(clock));
    }

    /**
     * 이어하기 카드: 범위 안 진행 중 건수 + 최신 1건 + 범위별 건수.
     * <p>건수가 0 이면 latest=null → 화면은 카드를 그리지 않는다 (제공대상: 이어하기 대상자).
     */
    public ResumeSummary summary(String customerId, ResumeScope scope, LocalDateTime at) {
        List<ResumePoint> active = active(customerId, at);
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ResumeScope s : ResumeScope.values()) {
            counts.put(s.name(), (int) active.stream().filter(p -> s.includes(p.productType())).count());
        }
        List<ResumePoint> inScope = active.stream().filter(p -> scope.includes(p.productType())).toList();
        ResumeCardView latest = inScope.isEmpty() ? null : toView(inScope.get(0), at);
        return new ResumeSummary(scope.name(), inScope.size(), counts, latest, properties.getStatusUrl(), at.format(FULL));
    }

    /** 신청현황조회 - 범위 안 진행 중 전체 (최신순) */
    public List<ResumeCardView> list(String customerId, String scope) {
        LocalDateTime now = LocalDateTime.now(clock);
        ResumeScope s = parseScope(scope);
        return active(customerId, now).stream()
                .filter(p -> s.includes(p.productType()))
                .map(p -> toView(p, now))
                .toList();
    }

    /**
     * [이어하기] - 이동 URL.
     * <p>카드를 띄운 뒤 다른 기기에서 완료/취소했거나 만료됐을 수 있으므로, 누르는 시점에 다시 확인한다.
     * 다른 고객의 ID 를 넣으면 존재 여부를 알 수 없도록 "찾을 수 없음" 으로 응답한다.
     */
    public String continueUrl(String customerId, long id) {
        LocalDateTime now = LocalDateTime.now(clock);
        ResumePoint p = own(customerId, id);
        if (!p.resumableAt(now)) {
            audit.info("[이어하기 불가] id={} status={} expiresAt={}", id, p.status(), p.expiresAt());
            throw new ResumeException(ResumeErrorCode.NOT_RESUMABLE);
        }
        audit.info("[이어하기 진입] id={} biz={} product={} step={}", id, p.bizCode(), p.productCode(), p.stepNo());
        return p.resumeUrl();
    }

    // ------------------------------------------------------------------ 신청 업무 연동 (ResumePoint 등록)

    /**
     * 신청 업무가 단계를 저장할 때 호출 - 진행 중인 같은 업무·상품 건이 있으면 단계만 갱신 (건수 중복 방지).
     *
     * @return 등록/갱신된 이어하기 ID
     */
    public long saveStep(String customerId, ResumePointRequest req) {
        LocalDateTime now = LocalDateTime.now(clock);
        ResumePoint draft = validate(customerId, req, 0, now, now);
        ResumePoint existing = repository.findByCustomer(customerId).stream()
                .filter(p -> p.resumableAt(now) && p.bizCode().equals(draft.bizCode()) && p.productCode().equals(draft.productCode()))
                .findFirst().orElse(null);
        ResumePoint saved;
        if (existing != null) {
            saved = new ResumePoint(existing.id(), customerId, draft.productType(), draft.bizCode(), draft.bizName(),
                    draft.productCode(), draft.productName(), draft.partnerCode(), draft.stepName(), draft.stepNo(),
                    draft.totalSteps(), draft.resumeUrl(), ResumeStatus.IN_PROGRESS, existing.startedAt(), now,
                    req.expireDays() == null ? existing.expiresAt() : draft.expiresAt());
        } else {
            saved = new ResumePoint(repository.nextId(), customerId, draft.productType(), draft.bizCode(), draft.bizName(),
                    draft.productCode(), draft.productName(), draft.partnerCode(), draft.stepName(), draft.stepNo(),
                    draft.totalSteps(), draft.resumeUrl(), ResumeStatus.IN_PROGRESS, now, now, draft.expiresAt());
        }
        repository.save(saved);
        audit.info("[이어하기 {}] id={} biz={} product={} step={}/{}", existing != null ? "갱신" : "등록",
                saved.id(), saved.bizCode(), saved.productCode(), saved.stepNo(), saved.totalSteps());
        return saved.id();
    }

    /** 신청 완료 → 이어하기 대상에서 제외 */
    public void complete(String customerId, long id) {
        close(customerId, id, ResumeStatus.COMPLETED);
    }

    /** 신청 취소 → 이어하기 대상에서 제외 (공통#50 "이어하기 진행 중 취소하여 잔여 건이 없을 시" 툴팁 미노출과 연동) */
    public void cancel(String customerId, long id) {
        close(customerId, id, ResumeStatus.CANCELED);
    }

    /** 회사 마스터 (테스트 화면용) */
    public List<Partner> partners() {
        return partnerRepository.findAll();
    }

    // ------------------------------------------------------------------ 내부

    private void close(String customerId, long id, ResumeStatus status) {
        ResumePoint p = own(customerId, id);
        if (p.status() != ResumeStatus.IN_PROGRESS) {
            throw new ResumeException(ResumeErrorCode.NOT_RESUMABLE);
        }
        repository.save(p.withStatus(status, LocalDateTime.now(clock)));
        audit.info("[이어하기 종료] id={} status={}", id, status);
    }

    private ResumePoint own(String customerId, long id) {
        return repository.findById(id)
                .filter(p -> p.customerId().equals(customerId))
                .orElseThrow(() -> new ResumeException(ResumeErrorCode.NOT_FOUND));
    }

    /** 진행 중 + 만료 전, 최신순 */
    private List<ResumePoint> active(String customerId, LocalDateTime at) {
        if (customerId == null || customerId.isBlank()) {
            throw new ResumeException(ResumeErrorCode.LOGIN_REQUIRED);
        }
        return repository.findByCustomer(customerId).stream()
                .filter(p -> p.resumableAt(at))
                .sorted(ResumePoint.LATEST_FIRST)
                .toList();
    }

    private ResumeCardView toView(ResumePoint p, LocalDateTime at) {
        Partner partner = partnerRepository.findByCode(p.partnerCode())
                .orElseGet(() -> {
                    // 마스터 누락은 카드가 깨지지 않게 당행 처리하되 운영자가 알 수 있도록 남긴다
                    log.warn("[이어하기] 회사 마스터 누락 partnerCode={} id={}", p.partnerCode(), p.id());
                    return partnerRepository.findByCode(properties.getOwnPartnerCode())
                            .orElse(new Partner(p.partnerCode(), "", null, true));
                });
        Long daysLeft = null;
        if (p.expiresAt() != null) {
            // 00:00 만료면 전날이 마지막 이용일 (종료 미포함 규칙)
            java.time.LocalDate lastDay = p.expiresAt().toLocalTime().equals(java.time.LocalTime.MIDNIGHT)
                    ? p.expiresAt().toLocalDate().minusDays(1) : p.expiresAt().toLocalDate();
            daysLeft = Math.max(0, ChronoUnit.DAYS.between(at.toLocalDate(), lastDay));
        }
        return new ResumeCardView(p.id(), p.productType().name(), p.productType().getLabel(), p.bizName(),
                partner.name(), partner.logoUrl(), partner.own(), p.productName(), p.stepName(), p.stepNo(), p.totalSteps(),
                p.lastActivityAt().format(MINUTE), p.expiresAt() == null ? null : p.expiresAt().format(MINUTE), daysLeft);
    }

    public static ResumeScope parseScope(String scope) {
        if (scope == null || scope.isBlank()) {
            return ResumeScope.ALL;
        }
        try {
            return ResumeScope.valueOf(scope.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ResumeException(ResumeErrorCode.INVALID_SCOPE, "scope");
        }
    }

    /** 등록 요청 서버측 검증 (id·일시는 호출부에서 다시 채움) */
    private ResumePoint validate(String customerId, ResumePointRequest req, long id, LocalDateTime startedAt, LocalDateTime now) {
        if (customerId == null || customerId.isBlank()) {
            throw new ResumeException(ResumeErrorCode.LOGIN_REQUIRED);
        }
        if (req == null) {
            throw new ResumeException(ResumeErrorCode.INVALID_REQUEST);
        }
        ProductType type;
        try {
            type = ProductType.valueOf(required(req.productType(), "상품구분", 10).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ResumeException(ResumeErrorCode.INVALID_CODE, "상품구분");
        }
        String bizCode = code(req.bizCode(), "업무코드");
        String productCode = code(req.productCode(), "상품코드");
        String partnerCode = req.partnerCode() == null || req.partnerCode().isBlank()
                ? properties.getOwnPartnerCode() : code(req.partnerCode(), "회사코드");
        Partner partner = partnerRepository.findByCode(partnerCode)
                .orElseThrow(() -> new ResumeException(ResumeErrorCode.INVALID_CODE, "회사코드"));
        // 공통#49 저축 이어하기: "수신상품만 취급" + "당행 상품일 경우 당행 로고" → 수신은 당행 상품만 (제휴 수신상품 없음)
        if (type == ProductType.DEPOSIT && !partner.own()) {
            throw new ResumeException(ResumeErrorCode.DEPOSIT_OWN_ONLY, "회사코드");
        }
        String bizName = required(req.bizName(), "업무명", MAX_BIZ_NAME);
        String productName = required(req.productName(), "상품명", MAX_PRODUCT_NAME);
        String stepName = required(req.stepName(), "단계명", MAX_STEP_NAME);
        int stepNo = req.stepNo() == null ? 1 : req.stepNo();
        int total = req.totalSteps() == null ? 0 : req.totalSteps();
        if (stepNo < 1 || total < 0 || (total > 0 && stepNo > total) || total > 50) {
            throw new ResumeException(ResumeErrorCode.INVALID_REQUEST, "단계 순번");
        }
        String url = required(req.resumeUrl(), "이어하기 링크", MAX_URL);
        if (!isAllowedUrl(url)) {
            throw new ResumeException(ResumeErrorCode.INVALID_URL, "이어하기 링크");
        }
        int expireDays = req.expireDays() == null ? properties.getDefaultExpireDays() : req.expireDays();
        LocalDateTime expiresAt = expireDays == 0 ? null : startedAt.plusDays(expireDays);
        return new ResumePoint(id, customerId, type, bizCode, bizName, productCode, productName, partnerCode,
                stepName, stepNo, total, url, ResumeStatus.IN_PROGRESS, startedAt, now, expiresAt);
    }

    /** 이어하기 링크 허용 여부: 같은 도메인 상대경로, 또는 허용 접두어로 시작하는 APP 딥링크 */
    boolean isAllowedUrl(String url) {
        boolean shapeOk = RELATIVE.matcher(url).matches() || DEEP_LINK.matcher(url).matches();
        if (!shapeOk) return false;
        for (String prefix : properties.getAllowedUrlPrefixes()) {
            if (prefix == null || prefix.isEmpty()) continue;
            if ("/".equals(prefix) ? RELATIVE.matcher(url).matches() : url.startsWith(prefix)) return true;
        }
        return false;
    }

    private static String required(String value, String field, int max) {
        String v = value == null ? null : value.strip();
        if (v == null || v.isEmpty()) throw new ResumeException(ResumeErrorCode.REQUIRED_MISSING, field);
        if (v.length() > max) throw new ResumeException(ResumeErrorCode.TOO_LONG, field + "(" + max + "자)");
        return v;
    }

    private static String code(String value, String field) {
        String v = required(value, field, 30).toUpperCase(Locale.ROOT);
        if (!CODE.matcher(v).matches()) throw new ResumeException(ResumeErrorCode.INVALID_CODE, field);
        return v;
    }

    /** yaml seed → 메모리 저장소 (기동 시각 기준 상대 일시) */
    private void loadSeed() {
        LocalDateTime now = LocalDateTime.now(clock);
        for (ResumeProperties.Seed s : properties.getSeed()) {
            try {
                LocalDateTime started = now.minusMinutes(s.getStartedMinutesAgo());
                LocalDateTime last = now.minusMinutes(s.getLastActivityMinutesAgo());
                ResumePointRequest req = new ResumePointRequest(s.getProductType(), s.getBizCode(), s.getBizName(),
                        s.getProductCode(), s.getProductName(), s.getPartnerCode(), s.getStepName(), s.getStepNo(),
                        s.getTotalSteps(), s.getResumeUrl(), s.getExpireDays());
                ResumePoint v = validate(s.getCustomerId(), req, repository.nextId(), started, last);
                ResumeStatus status = ResumeStatus.valueOf(s.getStatus().strip().toUpperCase(Locale.ROOT));
                repository.save(status == ResumeStatus.IN_PROGRESS ? v : v.withStatus(status, last));
            } catch (RuntimeException e) {
                log.warn("[이어하기] seed 적재 제외 product={} 사유={}", s.getProductName(), e.getMessage());
            }
        }
    }
}
