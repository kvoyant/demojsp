package com.example.demojsp.common.terms.service;

import com.example.demojsp.common.terms.config.TermsProperties;
import com.example.demojsp.common.terms.domain.AgreeRequest;
import com.example.demojsp.common.terms.domain.AgreeResult;
import com.example.demojsp.common.terms.domain.AgreementRecord;
import com.example.demojsp.common.terms.domain.BizCodeInfo;
import com.example.demojsp.common.terms.domain.TermsItem;
import com.example.demojsp.common.terms.domain.TermsRegisterRequest;
import com.example.demojsp.common.terms.domain.TermsSet;
import com.example.demojsp.common.terms.domain.TermsSummary;
import com.example.demojsp.common.terms.exception.TermsErrorCode;
import com.example.demojsp.common.terms.exception.TermsException;
import com.example.demojsp.common.terms.store.TermsAgreementRepository;
import com.example.demojsp.common.terms.store.TermsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * [CF-BC-04] 약관동의 공통 서비스 (Facade).
 *
 * <p>각 화면에 개별 코딩되어 있던 약관 처리를 "업무코드(bizCode) 하나로 조회 → 동의 → 증적 저장" 으로 통일한다.
 * <ul>
 *   <li>조회: 업무코드 단위로 ADMIN 등록 약관을 필수/선택 분리해서 반환 (코드·문구 하드코딩 금지)</li>
 *   <li>동의: 필수 전체동의 서버 재검증, 개정된 약관(버전 불일치) 차단, 미동의 선택약관도 N 으로 기록</li>
 *   <li>증적: 약관 버전 + 전문 SHA-256 해시 + 주체/채널/IP/일시 저장</li>
 *   <li>ADMIN: 약관 등록·개정(새 버전), 버전 이력/동의 이력 조회</li>
 * </ul>
 *
 * <p>빈 이름 {@code termsAgreeService} - 본인인증 모듈의 {@code termsService} 와 충돌하지 않게 이름을 달리했다.
 */
@Service
public class TermsAgreeService {

    private static final Logger log = LoggerFactory.getLogger(TermsAgreeService.class);
    /** 감사(증적) 로그 전용 로거 - 운영에서 별도 파일로 분리 권장 */
    private static final Logger audit = LoggerFactory.getLogger("AUDIT.TERMS");

    private static final Pattern CODE = Pattern.compile("^[A-Z0-9_]{2,40}$");
    private static final Pattern VERSION = Pattern.compile("^[0-9A-Za-z.\\-]{1,10}$");
    private static final int MAX_CONTENT = 200_000;

    private final TermsRepository termsRepository;
    private final TermsAgreementRepository agreementRepository;
    private final TermsProperties properties;
    private final Clock clock;

    @Autowired
    public TermsAgreeService(TermsRepository termsRepository, TermsAgreementRepository agreementRepository,
                             TermsProperties properties) {
        this(termsRepository, agreementRepository, properties, Clock.systemDefaultZone());
    }

    public TermsAgreeService(TermsRepository termsRepository, TermsAgreementRepository agreementRepository,
                             TermsProperties properties, Clock clock) {
        this.termsRepository = termsRepository;
        this.agreementRepository = agreementRepository;
        this.properties = properties;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ 조회

    /** 업무코드의 현재 약관 (필수/선택 분리) */
    public TermsSet getTermsSet(String bizCode) {
        List<TermsItem> current = currentOrThrow(bizCode);
        List<TermsSummary> required = new ArrayList<>();
        List<TermsSummary> optional = new ArrayList<>();
        for (TermsItem t : current) {
            (t.required() ? required : optional).add(t.toSummary());
        }
        return new TermsSet(bizCode, bizName(bizCode), required, optional);
    }

    /** 약관 상세보기 (전문 포함) */
    public TermsItem getDetail(String bizCode, String termsCode) {
        return termsRepository.findCurrent(bizCode, termsCode)
                .orElseThrow(() -> new TermsException(TermsErrorCode.TERMS_NOT_FOUND, bizCode + "/" + termsCode));
    }

    // ------------------------------------------------------------------ 동의

    /**
     * 약관 동의 처리 + 증적 저장.
     *
     * @param subjectId 동의 주체 (로그인 회원ID, 비로그인은 null → GUEST)
     * @param clientIp  접속 IP
     */
    public AgreeResult agree(String bizCode, AgreeRequest request, String subjectId, String clientIp) {
        if (request == null || request.items() == null) {
            throw new TermsException(TermsErrorCode.INVALID_REQUEST, "items is null");
        }
        List<TermsItem> current = currentOrThrow(bizCode);
        Map<String, TermsItem> currentByCode = new HashMap<>();
        current.forEach(t -> currentByCode.put(t.termsCode(), t));

        // 1) 요청 항목 검증: 존재하는 약관인지, 중복인지, 화면이 본 버전이 현재 버전인지
        Map<String, AgreeRequest.Item> requested = new HashMap<>();
        for (AgreeRequest.Item item : request.items()) {
            if (item == null || item.termsCode() == null) {
                throw new TermsException(TermsErrorCode.INVALID_REQUEST, "termsCode is null");
            }
            TermsItem terms = currentByCode.get(item.termsCode());
            if (terms == null) {
                throw new TermsException(TermsErrorCode.UNKNOWN_TERMS, bizCode + "/" + item.termsCode());
            }
            if (requested.put(item.termsCode(), item) != null) {
                throw new TermsException(TermsErrorCode.DUPLICATE_TERMS, item.termsCode());
            }
            if (!terms.version().equals(item.version())) {
                // 화면을 띄워둔 사이 ADMIN 이 약관을 개정한 경우 → 새 약관을 다시 보여줘야 한다
                throw new TermsException(TermsErrorCode.VERSION_MISMATCH,
                        item.termsCode() + " client=" + item.version() + " current=" + terms.version());
            }
        }

        // 2) 필수 약관 전체 동의 서버 재검증 (화면 버튼 비활성화만으로는 우회 호출을 막을 수 없다)
        List<String> missing = current.stream()
                .filter(TermsItem::required)
                .map(TermsItem::termsCode)
                .filter(code -> !requested.containsKey(code) || !requested.get(code).agreed())
                .toList();
        if (!missing.isEmpty()) {
            throw new TermsException(TermsErrorCode.REQUIRED_NOT_AGREED, "missing=" + missing);
        }

        // 3) 증적 저장 - 현재 약관 전체에 대해 Y/N 기록 (요청에 없는 선택약관 = 미동의)
        String agreementId = TermsSupport.newAgreementId(clock);
        String agreedAt = TermsSupport.format(clock);
        String subject = (subjectId == null || subjectId.isBlank()) ? "GUEST" : subjectId;
        String channel = sanitizeChannel(request.channel());

        List<AgreementRecord> records = new ArrayList<>();
        List<String> agreedCodes = new ArrayList<>();
        List<String> declinedCodes = new ArrayList<>();
        List<String> versions = new ArrayList<>();
        for (TermsItem t : current) {
            AgreeRequest.Item item = requested.get(t.termsCode());
            boolean agreed = item != null && item.agreed();
            records.add(new AgreementRecord(agreementId, bizCode, t.termsCode(), t.title(), t.version(),
                    t.contentHash(), t.required(), agreed, subject, channel, clientIp, agreedAt));
            (agreed ? agreedCodes : declinedCodes).add(t.termsCode());
            versions.add(t.termsCode() + "=" + t.version());
        }
        agreementRepository.saveAll(records);
        audit.info("[약관동의] id={} biz={} subject={} channel={} agreed={} declined={} versions={}",
                agreementId, bizCode, subject, channel, agreedCodes, declinedCodes, versions);

        return new AgreeResult(agreementId, bizCode, List.copyOf(agreedCodes), List.copyOf(declinedCodes),
                List.copyOf(versions), agreedAt, clock.instant());
    }

    // ------------------------------------------------------------------ ADMIN

    /** 약관이 등록된 업무코드 목록 */
    public List<BizCodeInfo> bizCodes() {
        List<BizCodeInfo> result = new ArrayList<>();
        for (String bizCode : termsRepository.bizCodes()) {
            List<TermsItem> current = termsRepository.findCurrent(bizCode);
            int required = (int) current.stream().filter(TermsItem::required).count();
            result.add(new BizCodeInfo(bizCode, bizName(bizCode), required, current.size() - required));
        }
        return result;
    }

    /** 업무코드의 전체 버전 이력 */
    public List<TermsItem> versions(String bizCode) {
        currentOrThrow(bizCode);
        return termsRepository.findAllVersions(bizCode);
    }

    /**
     * 약관 등록/개정. 새 버전은 즉시 현재 약관이 된다.
     * <p>운영에서는 시행일(effectiveDate) 예약 등록, 결재 승인 후 반영 등이 추가될 수 있다.
     */
    public TermsItem register(TermsRegisterRequest req, String adminId) {
        if (req == null || req.bizCode() == null || !CODE.matcher(req.bizCode()).matches()
                || req.termsCode() == null || !CODE.matcher(req.termsCode()).matches()) {
            throw new TermsException(TermsErrorCode.INVALID_REQUEST, "bizCode/termsCode 는 영문대문자·숫자·_ 2~40자");
        }
        if (req.version() == null || !VERSION.matcher(req.version()).matches()) {
            throw new TermsException(TermsErrorCode.INVALID_REQUEST, "version");
        }
        if (isBlank(req.title()) || req.title().length() > 100 || isBlank(req.content()) || req.content().length() > MAX_CONTENT) {
            throw new TermsException(TermsErrorCode.INVALID_REQUEST, "title/content");
        }
        if (!isSafeUrl(req.pdfUrl())) {
            throw new TermsException(TermsErrorCode.INVALID_REQUEST, "pdfUrl 은 / 로 시작하는 상대경로 또는 https:// 만 허용");
        }
        TermsItem previous = termsRepository.findCurrent(req.bizCode(), req.termsCode()).orElse(null);
        boolean required = req.required() != null ? req.required() : previous != null && previous.required();
        int sortOrder = req.sortOrder() != null ? req.sortOrder() : previous != null ? previous.sortOrder() : 99;

        TermsItem item = new TermsItem(req.bizCode(), req.termsCode(), req.title().strip(), required, req.version(),
                sortOrder, req.content(), isBlank(req.pdfUrl()) ? null : req.pdfUrl(), TermsSupport.sha256(req.content()),
                TermsSupport.format(clock), adminId == null ? "ADMIN" : adminId);
        if (!termsRepository.add(item)) {
            throw new TermsException(TermsErrorCode.DUPLICATE_VERSION, req.bizCode() + "/" + req.termsCode() + " v" + req.version());
        }
        log.info("[약관관리] 등록 biz={} terms={} v{} (이전 v{}) by {}", item.bizCode(), item.termsCode(), item.version(),
                previous == null ? "-" : previous.version(), item.registeredBy());
        return item;
    }

    /** 동의 이력 조회 (최신순) */
    public List<AgreementRecord> history(String bizCode, String subjectId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        return agreementRepository.find(emptyToNull(bizCode), emptyToNull(subjectId), safeLimit);
    }

    // ------------------------------------------------------------------ 내부

    private List<TermsItem> currentOrThrow(String bizCode) {
        List<TermsItem> current = bizCode == null ? List.of() : termsRepository.findCurrent(bizCode);
        if (current.isEmpty()) {
            throw new TermsException(TermsErrorCode.BIZ_CODE_NOT_FOUND, bizCode);
        }
        return current;
    }

    private String bizName(String bizCode) {
        return properties.getBizNames().getOrDefault(bizCode, bizCode);
    }

    private static String sanitizeChannel(String channel) {
        if (channel == null || !channel.matches("^[A-Z_]{1,20}$")) {
            return "UNKNOWN";
        }
        return channel;
    }

    private static boolean isSafeUrl(String url) {
        if (isBlank(url)) {
            return true;
        }
        return (url.startsWith("/") && !url.startsWith("//")) || url.startsWith("https://");
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String emptyToNull(String s) {
        return isBlank(s) ? null : s;
    }
}
