package com.example.demojsp.common.platformlink.service;

import com.example.demojsp.common.platformlink.adapter.AdapterParseException;
import com.example.demojsp.common.platformlink.adapter.PartnerAdapter;
import com.example.demojsp.common.platformlink.adapter.PartnerAdapterRegistry;
import com.example.demojsp.common.platformlink.config.PlatformLinkProperties;
import com.example.demojsp.common.platformlink.css.CssRouter;
import com.example.demojsp.common.platformlink.dto.LinkTrace;
import com.example.demojsp.common.platformlink.dto.LnkCodeMapDto;
import com.example.demojsp.common.platformlink.dto.LnkPartnerDto;
import com.example.demojsp.common.platformlink.dto.LnkProductDto;
import com.example.demojsp.common.platformlink.dto.LnkReqDto;
import com.example.demojsp.common.platformlink.mapper.PlatformLinkMapper;
import com.example.demojsp.common.platformlink.model.CssProduct;
import com.example.demojsp.common.platformlink.model.CssRequest;
import com.example.demojsp.common.platformlink.model.LimitInquiry;
import com.example.demojsp.common.platformlink.model.LinkOutcome;
import com.example.demojsp.common.platformlink.security.PartnerSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 트랜잭션 메모: @Transactional 을 붙이지 않는다. CSS 를 기다리는 몇 초 동안 DB 커넥션·락을 잡고 있으면 안 되고,
 * "수신(RECEIVED)" 행은 CSS 호출 전에 먼저 커밋돼야 같은 요청번호의 동시 재전송을 막을 수 있다.
 */
@Service
public class PlatformLinkServiceImpl implements PlatformLinkService {

    private static final Logger log = LoggerFactory.getLogger(PlatformLinkServiceImpl.class);
    private static final Logger audit = LoggerFactory.getLogger("AUDIT.PLATFORM_LINK");
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);

    private final PlatformLinkMapper mapper;
    private final PartnerAdapterRegistry adapters;
    private final CssRouter router;
    private final PlatformLinkProperties props;
    private final SecureRandom random = new SecureRandom();
    private Clock clock = Clock.systemDefaultZone();

    public PlatformLinkServiceImpl(PlatformLinkMapper mapper, PartnerAdapterRegistry adapters, CssRouter router, PlatformLinkProperties props) {
        this.mapper = mapper;
        this.adapters = adapters;
        this.router = router;
        this.props = props;
    }

    void setClock(Clock clock) {
        this.clock = clock;
    }

    @Override
    public LinkResponse handle(String partnerCd, String timestamp, String signature, String body, String clientIp) {
        LinkTrace t = new LinkTrace();
        LnkPartnerDto p = partnerCd == null ? null : mapper.selectPartner(partnerCd);
        if (p == null) {
            t.step("제휴사 확인", false, "미등록 제휴사: " + partnerCd);
            return new LinkResponse(404, "{\"error\":\"UNKNOWN_PARTNER\"}", null, t);
        }
        PartnerAdapter a = adapters.get(p.getAdapterCd());
        if (a == null) {
            log.error("[플랫폼연계] {} 어댑터 미등록: {}", partnerCd, p.getAdapterCd());
            t.step("제휴사 확인", false, "전문 변환기 미등록: " + p.getAdapterCd());
            return new LinkResponse(500, "{\"error\":\"ADAPTER_NOT_CONFIGURED\"}", null, t);
        }
        t.step("제휴사 확인", true, p.getPartnerNm() + " · 변환기 " + a.adapterCd() + " · 엔진 " + p.getCssEngine()
                + " · 제한시간 " + p.getTimeoutMs() + "ms");

        // ① 인증 - 실패하면 이력을 남기지 않고 401 (신뢰할 수 없는 요청이라 DB 에 쓰지 않음, 로그만)
        String authError = authenticate(p, timestamp, signature, body, clientIp);
        if (authError != null) {
            t.step("① 인증", false, authError);
            audit.warn("auth-fail partner={} ip={} reason={}", partnerCd, clientIp, authError);
            return new LinkResponse(401, a.response(outcome(p, LinkOutcome.AUTH_FAIL, peekReqNo(a, body), null, null, null, 0, 0)), null, t);
        }
        t.step("① 인증", true, "허용 IP · HMAC 서명 · 타임스탬프 확인");

        // ② 전문 변환
        LimitInquiry q;
        try {
            q = a.parse(body);
            String invalid = validate(q);
            if (invalid != null) throw new AdapterParseException(invalid, q.partnerReqNo());
        } catch (AdapterParseException e) {
            t.step("② 전문 변환", false, e.getMessage());
            return finishWithoutCss(p, a, t, e.getPartnerReqNo(), null, clientIp, LinkOutcome.INVALID);
        }
        t.setCommonModel(maskedModel(q));
        t.step("② 전문 변환", true, "제휴사 전문 → 공통모델 (금액 원 · 생년월일 yyyyMMdd · 직업 " + q.employType() + ")");

        // ③ 중복 확인 + 수신 행 선점 (UNIQUE: 제휴사코드 + 제휴사요청번호)
        LnkReqDto prev = mapper.selectReqByPartnerReq(p.getPartnerCd(), q.partnerReqNo());
        if (prev != null) return duplicate(prev, a, p, q, t);
        LnkReqDto row = newRow(p, q, clientIp);
        try {
            mapper.insertReq(row);
        } catch (DuplicateKeyException e) {          // 동시에 같은 요청이 두 번 들어온 경우
            return duplicate(mapper.selectReqByPartnerReq(p.getPartnerCd(), q.partnerReqNo()), a, p, q, t);
        }
        t.step("③ 중복 확인", true, "신규 요청 · 연계번호 " + row.getLnkReqNo());

        // ④ 서비스 중단 스위치
        if (!"Y".equals(p.getUseYn())) {
            t.step("④ 서비스 상태", false, "USE_YN=N (ADMIN 에서 중단)");
            return finish(p, a, t, row, LinkOutcome.STOPPED, null, null, null);
        }
        t.step("④ 서비스 상태", true, "사용 중 (USE_YN=Y)");

        // ⑤ 상품 결정
        String product = CssProduct.decide(q);
        LnkProductDto prd = mapper.selectProduct(product);
        row.setCssProduct(product);
        if (prd == null || !"Y".equals(prd.getUseYn()) || !p.allows(product)) {
            t.step("⑤ 상품 결정", false, product + " - 이 제휴사 허용상품(" + p.getProducts() + ")이 아님");
            return finish(p, a, t, row, LinkOutcome.PRODUCT_NA, null, null, null);
        }
        t.step("⑤ 상품 결정", true, product + " → " + prd.getProductNm() + " (" + prd.getAdminList() + ")");

        // ⑥ CSS 호출 - 제휴사 제한시간 - 여유 - 지금까지 쓴 시간 안에서만
        CssRequest cr = CssRequest.of(product, p.getPartnerCd(), q);
        t.setCssRequest(maskedCss(cr));
        long budget = p.getTimeoutMs() - p.getMarginMs() - t.elapsedMs();
        CssRouter.Routed r = router.inquire(p.getCssEngine(), cr, budget);
        String inner = switch (r.kind()) {
            case "OK" -> r.result().approved() ? LinkOutcome.APPROVED : LinkOutcome.REJECTED;
            case "TIMEOUT" -> LinkOutcome.TIMEOUT;
            default -> LinkOutcome.CSS_ERROR;
        };
        row.setCssEngine(r.engine());
        row.setCssMs((int) r.cssMs());
        row.setShadowResult(r.shadow());
        t.step("⑥ CSS 한도조회", "OK".equals(r.kind()), r.engine() + " · " + r.cssMs() + "ms (허용 " + budget + "ms) · "
                + ("OK".equals(r.kind()) ? r.result().summary() : r.error()) + (r.shadow() == null ? "" : " | SHADOW: " + r.shadow()));
        return finish(p, a, t, row, inner, r.result() == null ? null : r.result().cssTxNo(), r, prd);
    }

    // ================================================================ 응답 마무리

    private LinkResponse finish(LnkPartnerDto p, PartnerAdapter a, LinkTrace t, LnkReqDto row, String inner, String cssTxNo,
                                CssRouter.Routed r, LnkProductDto prd) {
        boolean ok = LinkOutcome.APPROVED.equals(inner);
        long limit = ok ? r.result().limitAmt() : 0;
        int rate = ok ? r.result().rateBp() : 0;
        String outerPrd = null;
        if (ok) {
            outerPrd = mapper.selectOuterPrdCd(p.getPartnerCd(), row.getCssProduct());
            if (outerPrd == null) outerPrd = prd.getProductCd();          // 제휴사 상품코드 미등록 시 당행 코드
        }
        LinkOutcome o = outcome(p, inner, row.getPartnerReqNo(), row.getLnkReqNo(), outerPrd, ok ? prd.getProductNm() : null, limit, rate);
        String body = a.response(o);
        t.step("⑦ 결과 변환", true, "내부 " + inner + " → 제휴사 " + o.outerCd() + " (" + o.outerMsg() + ")");

        row.setStatus(LinkOutcome.APPROVED.equals(inner) || LinkOutcome.REJECTED.equals(inner) ? LnkReqDto.DONE : LnkReqDto.FAILED);
        row.setResultCd(inner);
        row.setOuterCd(o.outerCd());
        row.setLimitAmt(ok ? limit : null);
        row.setRateBp(ok ? rate : null);
        row.setCssTxNo(cssTxNo);
        row.setResBody(body);
        row.setTotalMs((int) t.elapsedMs());
        row.setResAt(LocalDateTime.now(clock));
        mapper.updateReq(row);
        t.step("⑧ 이력 저장", true, "TB_LNK_LIMIT_REQ " + row.getStatus() + " · 전체 " + row.getTotalMs() + "ms / 제한 " + p.getTimeoutMs() + "ms");
        audit.info("limit partner={} reqNo={} lnk={} product={} engine={} result={} outer={} totalMs={}", p.getPartnerCd(),
                row.getPartnerReqNo(), row.getLnkReqNo(), row.getCssProduct(), row.getCssEngine(), inner, o.outerCd(), row.getTotalMs());
        return new LinkResponse(200, body, row.getLnkReqNo(), t);
    }

    /** 전문 오류: 요청번호를 알면 이력을 남기고, 모르면 응답만 */
    private LinkResponse finishWithoutCss(LnkPartnerDto p, PartnerAdapter a, LinkTrace t, String reqNo, LimitInquiry q, String ip, String inner) {
        if (reqNo == null || mapper.selectReqByPartnerReq(p.getPartnerCd(), reqNo) != null) {
            LinkOutcome o = outcome(p, inner, reqNo, null, null, null, 0, 0);
            t.step("⑦ 결과 변환", true, "내부 " + inner + " → 제휴사 " + o.outerCd() + " (이력 없음)");
            return new LinkResponse(200, a.response(o), null, t);
        }
        LnkReqDto row = newRow(p, new LimitInquiry(reqNo, null, null, null, 0, null, null, null, null, null, 0), ip);
        try {
            mapper.insertReq(row);
        } catch (DuplicateKeyException e) {
            return new LinkResponse(200, a.response(outcome(p, inner, reqNo, null, null, null, 0, 0)), null, t);
        }
        return finish(p, a, t, row, inner, null, null, null);
    }

    private LinkResponse duplicate(LnkReqDto prev, PartnerAdapter a, LnkPartnerDto p, LimitInquiry q, LinkTrace t) {
        if (prev != null && prev.getResBody() != null) {
            t.step("③ 중복 확인", true, "이미 처리한 요청번호 → 저장된 응답을 그대로 재전송 (CSS 재호출 없음)");
            audit.info("duplicate partner={} reqNo={} lnk={}", p.getPartnerCd(), q.partnerReqNo(), prev.getLnkReqNo());
            return new LinkResponse(200, prev.getResBody(), prev.getLnkReqNo(), t);
        }
        t.step("③ 중복 확인", false, "같은 요청번호를 처리 중 - 잠시 후 재조회 필요");
        return new LinkResponse(200, a.response(outcome(p, LinkOutcome.CSS_ERROR, q.partnerReqNo(), null, null, null, 0, 0)), null, t);
    }

    // ================================================================ 도우미

    private String authenticate(LnkPartnerDto p, String timestamp, String signature, String body, String ip) {
        if (!p.allowsIp(ip)) return "허용되지 않은 IP: " + ip;
        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException | NullPointerException e) {
            return "타임스탬프 없음/형식 오류";
        }
        if (Math.abs(clock.millis() - ts) > props.getSignatureSkewSec() * 1000L) return "타임스탬프 만료 (허용 ±" + props.getSignatureSkewSec() + "초)";
        String secret = props.getSecrets().get(p.getSecretRef());
        if (secret == null || secret.isBlank()) return "서명키 미설정: common.platform-link.secrets." + p.getSecretRef();
        if (!PartnerSignature.verify(secret, p.getPartnerCd(), timestamp, body, signature)) return "서명 불일치";
        return null;
    }

    /** 인증 실패 응답에도 요청번호는 되돌려 준다 (제휴사가 어떤 요청이 실패했는지 알 수 있게) */
    private static String peekReqNo(PartnerAdapter a, String body) {
        try {
            return a.parse(body).partnerReqNo();
        } catch (RuntimeException e) {
            return e instanceof AdapterParseException pe ? pe.getPartnerReqNo() : null;
        }
    }

    private LinkOutcome outcome(LnkPartnerDto p, String inner, String reqNo, String lnkReqNo, String outerPrd, String prdNm, long limit, int rate) {
        LnkCodeMapDto m = mapper.selectCodeMap(p.getPartnerCd(), inner);
        if (m == null) log.warn("[플랫폼연계] {} 결과코드 매핑 없음: {} (TB_LNK_CODE_MAP)", p.getPartnerCd(), inner);
        return new LinkOutcome(inner, m == null ? inner : m.getOuterCd(), m == null ? inner : m.getOuterMsg(), reqNo, lnkReqNo,
                outerPrd, prdNm, limit, rate);
    }

    /** 공통 필수값 검사 - 어댑터가 형식을 맞춘 뒤, 제휴사와 무관하게 같은 규칙 */
    static String validate(LimitInquiry q) {
        if (q.partnerReqNo() == null || q.partnerReqNo().length() > 64) return "요청번호 오류";
        if (q.ci() == null || q.ci().length() < 20 || q.ci().length() > 100) return "CI 형식 오류";
        if (q.custNm() == null || q.custNm().length() > 50) return "고객명 오류";
        try {
            LocalDate.parse(q.birth(), YMD);
        } catch (DateTimeParseException | NullPointerException e) {
            return "생년월일 오류";
        }
        if (q.annualIncome() <= 0) return "연소득 오류";
        return null;
    }

    private LnkReqDto newRow(LnkPartnerDto p, LimitInquiry q, String ip) {
        byte[] b = new byte[16];
        random.nextBytes(b);
        LnkReqDto r = new LnkReqDto();
        r.setLnkReqNo(HexFormat.of().formatHex(b));
        r.setPartnerCd(p.getPartnerCd());
        r.setPartnerReqNo(q.partnerReqNo());
        r.setCiHash(q.ci() == null ? null : sha256(q.ci()));
        r.setCustNmMask(maskName(q.custNm()));
        r.setAnnualIncome(q.annualIncome() > 0 ? q.annualIncome() : null);
        r.setStatus(LnkReqDto.RECEIVED);
        r.setClientIp(ip);
        r.setReqAt(LocalDateTime.now(clock));
        return r;
    }

    private static Map<String, Object> maskedModel(LimitInquiry q) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("partnerReqNo", q.partnerReqNo());
        m.put("ci", q.ci().substring(0, Math.min(10, q.ci().length())) + "…(" + q.ci().length() + "자)");
        m.put("custNm", maskName(q.custNm()));
        m.put("birth", q.birth());
        m.put("annualIncome", q.annualIncome());
        m.put("employType", q.employType());
        m.put("companyNm", q.companyNm());
        m.put("vehicleNo", q.vehicleNo());
        m.put("collateralAddr", q.collateralAddr());
        m.put("productHint", q.productHint());
        m.put("requestAmt", q.requestAmt());
        return m;
    }

    private static Map<String, Object> maskedCss(CssRequest c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("product", c.product());
        m.put("channel", c.channel());
        m.put("ci", "(CI)");
        m.put("custNm", maskName(c.custNm()));
        m.put("birth", c.birth());
        m.put("annualIncome", c.annualIncome());
        m.put("employType", c.employType());
        m.put("vehicleNo", c.vehicleNo());
        m.put("collateralAddr", c.collateralAddr());
        m.put("requestAmt", c.requestAmt());
        return m;
    }

    static String maskName(String n) {
        if (n == null || n.length() < 2) return n;
        return n.length() == 2 ? n.charAt(0) + "*" : n.charAt(0) + "*".repeat(n.length() - 2) + n.charAt(n.length() - 1);
    }

    private static String sha256(String v) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(v.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
