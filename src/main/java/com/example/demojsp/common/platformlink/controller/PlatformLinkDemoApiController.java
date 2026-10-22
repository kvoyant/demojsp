package com.example.demojsp.common.platformlink.controller;

import com.example.demojsp.common.platformlink.adapter.PartnerAdapter;
import com.example.demojsp.common.platformlink.adapter.PartnerAdapterRegistry;
import com.example.demojsp.common.platformlink.config.PlatformLinkProperties;
import com.example.demojsp.common.platformlink.dto.LnkPartnerDto;
import com.example.demojsp.common.platformlink.exception.PlatformLinkErrorCode;
import com.example.demojsp.common.platformlink.exception.PlatformLinkException;
import com.example.demojsp.common.platformlink.mapper.PlatformLinkMapper;
import com.example.demojsp.common.platformlink.security.PartnerSignature;
import com.example.demojsp.common.platformlink.service.PlatformLinkService;
import com.example.demojsp.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * [local 샘플 전용] "제휴사 흉내" API - 샘플 화면이 제휴사 역할을 해서 실제 수신 창구와 같은 서비스를 호출한다.
 * 서명키를 서버가 대신 써 주므로 운영에서는 반드시 꺼야 한다 (common.platform-link.demo-api-open=false).
 * <pre>
 *  GET  /api/common/platform-link-demo/setup     제휴사 · 어댑터 설명 · 시나리오 목록
 *  POST /api/common/platform-link-demo/sample    {partnerCd, scenario} → 그 제휴사 형식의 요청 전문 예시
 *  POST /api/common/platform-link-demo/send      {partnerCd, body, signMode: OK|BAD|OLD} → 서명 → 수신 처리 → 응답 + 처리 단계
 * </pre>
 */
@RestController
@RequestMapping("/api/common/platform-link-demo")
public class PlatformLinkDemoApiController {

    private static final String CI = "kQ9vT2mXcLr8Hn3ZpW5yB1sD7fJ0aGuE4oNiYbKt6RwVhPq2CxMlSzA8eFdUjgO1Tn5HbLcIvR3yXqWm7sKpDa0e==";

    /** 시나리오: 이름 규칙으로 CSS 시뮬레이터 동작이 바뀐다 ('지연' = 타임아웃, '오류' = CSS 장애) */
    private static final List<Scenario> SCENARIOS = List.of(
            new Scenario("CREDIT_OK", "신용 · 정상 승인", "홍길동", "19900101", 60_000_000, "EMPLOYEE", "페퍼테크", null, null, "CREDIT"),
            new Scenario("AUTO_OK", "오토 · 차량번호 입력", "김오토", "19850315", 50_000_000, "EMPLOYEE", "한국자동차", "12가3456", null, "AUTO"),
            new Scenario("MORTGAGE_OK", "모기지 · 담보주소 입력", "이주택", "19800707", 90_000_000, "BUSINESS", null, null,
                    "서울특별시 강남구 테헤란로 123 페퍼아파트 101동 1001호", "MORTGAGE"),
            new Scenario("FREELANCER", "신용 · 프리랜서 (금리 +1%p)", "최자유", "19920202", 40_000_000, "FREELANCER", null, null, null, "CREDIT"),
            new Scenario("LOW_INCOME", "거절 · 연소득 1,500만원", "박소득", "19950505", 15_000_000, "EMPLOYEE", "소규모상사", null, null, "CREDIT"),
            new Scenario("SLOW", "CSS 지연 → 타임아웃", "정지연", "19880808", 55_000_000, "EMPLOYEE", "늦은회사", null, null, "CREDIT"),
            new Scenario("CSS_ERROR", "CSS 장애 → 일시 오류", "강오류", "19870909", 55_000_000, "EMPLOYEE", "장애회사", null, null, "CREDIT"));

    private final PlatformLinkService service;
    private final PlatformLinkMapper mapper;
    private final PartnerAdapterRegistry adapters;
    private final PlatformLinkProperties props;

    public PlatformLinkDemoApiController(PlatformLinkService service, PlatformLinkMapper mapper, PartnerAdapterRegistry adapters,
                                         PlatformLinkProperties props) {
        this.service = service;
        this.mapper = mapper;
        this.adapters = adapters;
        this.props = props;
    }

    @GetMapping("/setup")
    public ApiResponse<Map<String, Object>> setup() {
        demoOnly();
        List<Map<String, Object>> partners = new ArrayList<>();
        for (LnkPartnerDto p : mapper.selectPartners()) {
            PartnerAdapter a = adapters.get(p.getAdapterCd());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("partnerCd", p.getPartnerCd());
            m.put("partnerNm", p.getPartnerNm());
            m.put("adapterCd", p.getAdapterCd());
            m.put("adapterDesc", a == null ? "(미등록)" : a.description());
            m.put("cssEngine", p.getCssEngine());
            m.put("products", p.getProducts());
            m.put("timeoutMs", p.getTimeoutMs());
            m.put("useYn", p.getUseYn());
            partners.add(m);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("partners", partners);
        out.put("scenarios", SCENARIOS.stream().map(s -> Map.of("code", s.code(), "label", s.label())).toList());
        return ApiResponse.success(out);
    }

    @PostMapping("/sample")
    public ApiResponse<String> sample(@RequestBody Map<String, String> body) {
        demoOnly();
        LnkPartnerDto p = partner(body.get("partnerCd"));
        Scenario s = SCENARIOS.stream().filter(x -> x.code().equals(body.get("scenario"))).findFirst().orElse(SCENARIOS.get(0));
        String reqNo = p.getPartnerCd().charAt(0) + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(100, 999);
        PartnerAdapter.SampleInput in = new PartnerAdapter.SampleInput(reqNo, CI, s.name(), s.birth(), s.income(), s.employType(),
                s.companyNm(), s.vehicleNo(), s.collateralAddr(), s.product(), 0);
        return ApiResponse.success(adapter(p).sampleRequest(in));
    }

    @PostMapping("/send")
    public ApiResponse<Map<String, Object>> send(@RequestBody Map<String, String> body, HttpServletRequest request) {
        demoOnly();
        LnkPartnerDto p = partner(body.get("partnerCd"));
        String reqBody = body.getOrDefault("body", "");
        String mode = body.getOrDefault("signMode", "OK");
        long now = System.currentTimeMillis();
        String ts = String.valueOf("OLD".equals(mode) ? now - (props.getSignatureSkewSec() + 60) * 1000L : now);
        String secret = props.getSecrets().getOrDefault(p.getSecretRef(), "");
        String sig = "BAD".equals(mode) ? PartnerSignature.sign("wrong-secret", p.getPartnerCd(), ts, reqBody)
                : PartnerSignature.sign(secret, p.getPartnerCd(), ts, reqBody);

        long start = System.nanoTime();
        PlatformLinkService.LinkResponse r = service.handle(p.getPartnerCd(), ts, sig, reqBody, request.getRemoteAddr());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("url", "POST /api/link/platform/" + p.getPartnerCd() + "/limit");
        out.put("headers", Map.of(PartnerSignature.H_TIMESTAMP, ts, PartnerSignature.H_SIGNATURE, sig));
        out.put("httpStatus", r.httpStatus());
        out.put("responseBody", r.body());
        out.put("lnkReqNo", r.lnkReqNo());
        out.put("elapsedMs", (System.nanoTime() - start) / 1_000_000);
        out.put("timeoutMs", p.getTimeoutMs());
        out.put("trace", r.trace());
        return ApiResponse.success(out);
    }

    private void demoOnly() {
        if (!props.isDemoApiOpen()) throw new PlatformLinkException(PlatformLinkErrorCode.DEMO_ONLY);
    }

    private LnkPartnerDto partner(String cd) {
        LnkPartnerDto p = cd == null ? null : mapper.selectPartner(cd);
        if (p == null) throw new PlatformLinkException(PlatformLinkErrorCode.PARTNER_NOT_FOUND);
        return p;
    }

    private PartnerAdapter adapter(LnkPartnerDto p) {
        PartnerAdapter a = adapters.get(p.getAdapterCd());
        if (a == null) throw new PlatformLinkException(PlatformLinkErrorCode.ADAPTER_NOT_FOUND);
        return a;
    }

    private record Scenario(String code, String label, String name, String birth, long income, String employType, String companyNm,
                            String vehicleNo, String collateralAddr, String product) {
    }
}
