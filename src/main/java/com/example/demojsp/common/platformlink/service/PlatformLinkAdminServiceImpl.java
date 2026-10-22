package com.example.demojsp.common.platformlink.service;

import com.example.demojsp.common.platformlink.css.CssGateway;
import com.example.demojsp.common.platformlink.css.CssRouter;
import com.example.demojsp.common.platformlink.dto.LnkCodeMapDto;
import com.example.demojsp.common.platformlink.dto.LnkPartnerDto;
import com.example.demojsp.common.platformlink.dto.LnkProductDto;
import com.example.demojsp.common.platformlink.dto.LnkReqDto;
import com.example.demojsp.common.platformlink.dto.LnkStatDto;
import com.example.demojsp.common.platformlink.exception.PlatformLinkErrorCode;
import com.example.demojsp.common.platformlink.exception.PlatformLinkException;
import com.example.demojsp.common.platformlink.mapper.PlatformLinkMapper;
import com.example.demojsp.common.platformlink.model.CssProduct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class PlatformLinkAdminServiceImpl implements PlatformLinkAdminService {

    private static final Logger audit = LoggerFactory.getLogger("AUDIT.PLATFORM_LINK");
    private static final Set<String> ENGINES = Set.of(CssGateway.LEGACY, CssGateway.NEW, CssRouter.SHADOW);
    private static final Set<String> PRODUCTS = Set.of(CssProduct.CREDIT, CssProduct.AUTO, CssProduct.MORTGAGE);
    private static final Pattern IP = Pattern.compile("^(\\*|[0-9a-fA-F:.]{3,45})$");

    private final PlatformLinkMapper mapper;
    private final CssRouter router;
    private Clock clock = Clock.systemDefaultZone();

    public PlatformLinkAdminServiceImpl(PlatformLinkMapper mapper, CssRouter router) {
        this.mapper = mapper;
        this.router = router;
    }

    void setClock(Clock clock) {
        this.clock = clock;
    }

    @Override
    public List<LnkPartnerDto> partners() {
        return mapper.selectPartners();
    }

    @Override
    public LnkPartnerDto savePartner(LnkPartnerDto in, String adminId) {
        if (in == null || in.getPartnerCd() == null) throw new PlatformLinkException(PlatformLinkErrorCode.INVALID_REQUEST);
        LnkPartnerDto cur = mapper.selectPartner(in.getPartnerCd());
        if (cur == null) throw new PlatformLinkException(PlatformLinkErrorCode.PARTNER_NOT_FOUND);
        if (!ENGINES.contains(in.getCssEngine())) throw invalid("한도조회 엔진은 LEGACY / SHADOW / NEW 중 하나입니다.");
        Set<String> prd = new LinkedHashSet<>();
        for (String s : (in.getProducts() == null ? "" : in.getProducts()).split(",")) if (!s.isBlank()) prd.add(s.strip());
        if (prd.isEmpty() || !PRODUCTS.containsAll(prd)) throw invalid("허용상품은 CREDIT / AUTO / MORTGAGE 중에서 하나 이상 고르세요.");
        if (in.getTimeoutMs() == null || in.getTimeoutMs() < 500 || in.getTimeoutMs() > 30_000) throw invalid("제휴사 제한시간은 500~30000ms 입니다.");
        if (in.getMarginMs() == null || in.getMarginMs() < 0 || in.getMarginMs() > in.getTimeoutMs() / 2) throw invalid("응답 여유는 0 ~ 제한시간의 절반입니다.");
        String ips = in.getAllowIps() == null ? "" : in.getAllowIps().replace(" ", "");
        if (ips.isEmpty() || !Arrays.stream(ips.split(",")).allMatch(s -> IP.matcher(s).matches())) throw invalid("허용 IP 를 쉼표로 입력하세요. (* = 제한 없음, local 전용)");
        if (!"Y".equals(in.getUseYn()) && !"N".equals(in.getUseYn())) throw invalid("사용여부는 Y 또는 N 입니다.");

        LnkPartnerDto u = new LnkPartnerDto();
        u.setPartnerCd(cur.getPartnerCd());
        u.setCssEngine(in.getCssEngine());
        u.setProducts(String.join(",", prd));
        u.setTimeoutMs(in.getTimeoutMs());
        u.setMarginMs(in.getMarginMs());
        u.setAllowIps(ips);
        u.setUseYn(in.getUseYn());
        u.setUpdAt(LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS));
        u.setUpdBy(adminId);
        mapper.updatePartner(u);
        // 엔진 전환·중단은 운영 영향이 크므로 변경 전/후를 감사로그로 남긴다
        audit.info("partner-update by={} partner={} engine {}→{} use {}→{} timeout {}→{}", adminId, cur.getPartnerCd(),
                cur.getCssEngine(), u.getCssEngine(), cur.getUseYn(), u.getUseYn(), cur.getTimeoutMs(), u.getTimeoutMs());
        return mapper.selectPartner(cur.getPartnerCd());
    }

    @Override
    public List<LnkProductDto> products() {
        return mapper.selectProducts();
    }

    @Override
    public List<LnkCodeMapDto> codeMaps(String partnerCd) {
        return mapper.selectCodeMaps(partnerCd);
    }

    @Override
    public List<LnkReqDto> requests(String partnerCd, String status, String resultCd, int limit) {
        return mapper.selectReqList(blank(partnerCd), blank(status), blank(resultCd), Math.max(1, Math.min(limit, 500)));
    }

    @Override
    public List<LnkStatDto> stats(int hours) {
        return mapper.selectStats(LocalDateTime.now(clock).minusHours(Math.max(1, Math.min(hours, 24 * 31))));
    }

    @Override
    public Map<String, Map<String, Object>> circuit() {
        return router.circuit().snapshot();
    }

    @Override
    public void resetCircuit(String engine) {
        if (!CssGateway.LEGACY.equals(engine) && !CssGateway.NEW.equals(engine)) throw invalid("엔진은 LEGACY 또는 NEW 입니다.");
        router.circuit().reset(engine);
        audit.info("circuit-reset engine={}", engine);
    }

    private static String blank(String v) {
        return v == null || v.isBlank() ? null : v.strip();
    }

    private static PlatformLinkException invalid(String m) {
        return new PlatformLinkException(PlatformLinkErrorCode.INVALID_VALUE, m);
    }
}
