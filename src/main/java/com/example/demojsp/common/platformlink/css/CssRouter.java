package com.example.demojsp.common.platformlink.css;

import com.example.demojsp.common.platformlink.config.PlatformLinkProperties;
import com.example.demojsp.common.platformlink.model.CssRequest;
import com.example.demojsp.common.platformlink.model.CssResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * CSS 엔진 선택 + 제한시간 + 차단기 + SHADOW 병행비교. "현행 서비스를 끊지 않고 신규 CSS 로 옮기는" 핵심 부품.
 *
 * <pre>
 *  제휴사 CSS_ENGINE (DB, ADMIN 에서 즉시 변경)
 *   LEGACY : 현행 CSS 결과로 응답                       ← 전환 전 (지금)
 *   SHADOW : 현행 CSS 결과로 응답 + 신규 CSS 를 동시에 호출해 결과만 기록(비교)  ← 전환 검증 기간
 *   NEW    : 신규 CSS 결과로 응답                       ← 전환 완료
 *  문제가 생기면 ADMIN 에서 NEW → LEGACY 로 되돌리면 즉시 원복 (배포 없음).
 * </pre>
 * 제한시간: 호출을 별도 스레드에서 하고 Future.get(남은시간) 으로 끊는다. 제휴사 TIMEOUT_MS 안에 반드시 응답하기 위함.
 */
@Component
public class CssRouter {

    private static final Logger log = LoggerFactory.getLogger(CssRouter.class);
    public static final String SHADOW = "SHADOW";

    private final Map<String, CssGateway> gateways = new HashMap<>();
    private final ExecutorService executor;
    private final CircuitBreaker circuit;

    @Autowired
    public CssRouter(List<CssGateway> gateways, @Qualifier("platformLinkCssExecutor") ExecutorService executor,
                     PlatformLinkProperties props) {
        this(gateways, executor, new CircuitBreaker(props.getCircuitFailures(), props.getCircuitOpenSec(), Clock.systemDefaultZone()));
    }

    /** 테스트·수동 구성용 (차단기 시계·기준을 직접 넣는다) */
    public CssRouter(List<CssGateway> gateways, ExecutorService executor, CircuitBreaker circuit) {
        for (CssGateway g : gateways) this.gateways.put(g.engine(), g);
        this.executor = executor;
        this.circuit = circuit;
    }

    public CircuitBreaker circuit() {
        return circuit;
    }

    /**
     * @param mode     LEGACY / NEW / SHADOW
     * @param budgetMs CSS 에 줄 수 있는 최대 시간 (제휴사 타임아웃 - 여유 - 이미 쓴 시간)
     */
    public Routed inquire(String mode, CssRequest req, long budgetMs) {
        String primary = SHADOW.equals(mode) ? CssGateway.LEGACY : mode;
        CssGateway g = gateways.get(primary);
        if (g == null) return Routed.error(primary, "엔진 미등록: " + primary, 0, null);
        long start = System.nanoTime();

        // SHADOW: 신규 CSS 를 먼저 병행 출발시킨다 (응답에는 쓰지 않음)
        Future<CssResult> shadow = null;
        if (SHADOW.equals(mode) && gateways.containsKey(CssGateway.NEW) && circuit.allow(CssGateway.NEW)) {
            CssGateway n = gateways.get(CssGateway.NEW);
            shadow = executor.submit(() -> n.inquire(req));
        }

        Routed r;
        if (!circuit.allow(primary)) {
            r = Routed.error(primary, "차단기 열림 - CSS 호출 생략", 0, null);
        } else if (budgetMs <= 0) {
            r = Routed.timeout(primary, 0);
        } else {
            Future<CssResult> f = executor.submit(() -> g.inquire(req));
            try {
                CssResult res = f.get(budgetMs, TimeUnit.MILLISECONDS);
                circuit.success(primary);
                r = Routed.ok(res, ms(start));
            } catch (TimeoutException e) {
                f.cancel(true);
                circuit.failure(primary);
                log.warn("[플랫폼연계] {} CSS 제한시간 초과 ({}ms)", primary, budgetMs);
                r = Routed.timeout(primary, ms(start));
            } catch (ExecutionException e) {
                circuit.failure(primary);
                r = Routed.error(primary, e.getCause() == null ? e.getMessage() : e.getCause().getMessage(), ms(start), null);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                f.cancel(true);
                r = Routed.error(primary, "중단됨", ms(start), null);
            }
        }

        if (shadow != null) r = r.withShadow(shadowSummary(shadow, Math.max(0, budgetMs - ms(start))));
        return r;
    }

    /** SHADOW 결과는 남은 시간 안에서만 기다린다 - 비교 때문에 제휴사 응답이 늦어지면 안 된다 */
    private String shadowSummary(Future<CssResult> f, long waitMs) {
        try {
            CssResult n = f.get(waitMs, TimeUnit.MILLISECONDS);
            circuit.success(CssGateway.NEW);
            return n.summary();
        } catch (TimeoutException e) {
            f.cancel(true);
            return "NEW 시간초과(비교 불가)";
        } catch (ExecutionException e) {
            circuit.failure(CssGateway.NEW);
            return "NEW 오류: " + (e.getCause() == null ? e.getMessage() : e.getCause().getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "NEW 중단";
        }
    }

    private static long ms(long startNano) {
        return (System.nanoTime() - startNano) / 1_000_000;
    }

    /**
     * 라우팅 결과.
     *
     * @param kind   OK / TIMEOUT / ERROR
     * @param result OK 일 때 CSS 결과
     * @param engine 응답에 쓴 엔진
     * @param cssMs  CSS 소요시간
     * @param error  오류 사유
     * @param shadow SHADOW 비교 요약
     */
    public record Routed(String kind, CssResult result, String engine, long cssMs, String error, String shadow) {
        static Routed ok(CssResult r, long ms) {
            return new Routed("OK", r, r.engine(), ms, null, null);
        }

        static Routed timeout(String engine, long ms) {
            return new Routed("TIMEOUT", null, engine, ms, "제한시간 초과", null);
        }

        static Routed error(String engine, String msg, long ms, String shadow) {
            return new Routed("ERROR", null, engine, ms, msg, shadow);
        }

        Routed withShadow(String s) {
            return new Routed(kind, result, engine, cssMs, error, s);
        }
    }
}
