package com.example.demojsp.common.platformlink.css;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 엔진별 간이 차단기 (CF-TC-07 "Timeout·Retry·Circuit 정책").
 * <pre>
 *  CLOSED ─(연속 실패 n회)─▶ OPEN ─(openSec 경과)─▶ 한 번 시도 ─성공▶ CLOSED / 실패▶ OPEN
 * </pre>
 * OPEN 동안은 CSS 를 부르지 않고 바로 "일시 오류" 로 응답 → 장애 난 CSS 때문에 스레드가 쌓이거나
 * 제휴사 타임아웃을 계속 넘기는 일을 막는다. (서버 여러 대면 서버마다 따로 동작 - 운영은 Resilience4j 등으로 교체 검토)
 */
public class CircuitBreaker {

    private final int failures;
    private final int openSec;
    private final Clock clock;
    private final Map<String, State> states = new ConcurrentHashMap<>();

    public CircuitBreaker(int failures, int openSec, Clock clock) {
        this.failures = failures;
        this.openSec = openSec;
        this.clock = clock;
    }

    /** 지금 호출해도 되는가 */
    public boolean allow(String engine) {
        State s = states.get(engine);
        return s == null || s.openUntil == 0 || clock.millis() >= s.openUntil;
    }

    public void success(String engine) {
        states.put(engine, new State(0, 0));
    }

    public void failure(String engine) {
        states.compute(engine, (k, s) -> {
            int n = (s == null ? 0 : s.fails) + 1;
            return new State(n, n >= failures ? clock.millis() + openSec * 1000L : 0);
        });
    }

    public void reset(String engine) {
        states.remove(engine);
    }

    /** 관리화면용: 엔진별 {state, fails, openUntilEpochMs} */
    public Map<String, Map<String, Object>> snapshot() {
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        for (String e : new String[]{CssGateway.LEGACY, CssGateway.NEW}) {
            State s = states.get(e);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("state", allow(e) ? "CLOSED" : "OPEN");
            m.put("fails", s == null ? 0 : s.fails);
            m.put("openUntil", s == null || s.openUntil == 0 ? null : s.openUntil);
            out.put(e, m);
        }
        return out;
    }

    private record State(int fails, long openUntil) {
    }
}
