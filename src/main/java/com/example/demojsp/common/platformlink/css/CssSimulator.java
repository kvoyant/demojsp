package com.example.demojsp.common.platformlink.css;

import com.example.demojsp.common.platformlink.model.CssProduct;
import com.example.demojsp.common.platformlink.model.CssRequest;
import com.example.demojsp.common.platformlink.model.CssResult;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * CSS 시뮬레이터 공통 규칙 (실제 심사 로직 아님 - 연계 흐름 시험용).
 * <ul>
 *   <li>고객명에 '오류' → CssException (CSS 장애), '지연' → slowLatencyMs 만큼 늦게 응답 (타임아웃 시험)</li>
 *   <li>만 19세 미만 · 연소득 2,000만원 미만 → 거절</li>
 *   <li>오토는 차량번호, 모기지는 담보주소가 없으면 거절</li>
 *   <li>한도 = 연소득 × 상품별 배수 (상한 있음), 금리 = 기준금리 - 소득 구간 우대</li>
 * </ul>
 * 현행(LEGACY)과 신규(NEW)는 배수·금리를 조금 다르게 줘서 SHADOW 비교가 의미 있게 보이도록 했다.
 */
abstract class CssSimulator implements CssGateway {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    protected abstract int latencyMs();

    protected abstract int slowLatencyMs();

    /** {CREDIT, AUTO, MORTGAGE} 연소득 대비 한도 배수 (%) */
    protected abstract int multiplePct(String product);

    /** 상품 기준금리 (bp) */
    protected abstract int baseRateBp(String product);

    @Override
    public CssResult inquire(CssRequest q) {
        sleep(q.custNm() != null && q.custNm().contains("지연") ? slowLatencyMs() : latencyMs());
        if (q.custNm() != null && q.custNm().contains("오류")) throw new CssException(engine() + " CSS 내부 오류(시뮬레이터)");
        String tx = engine().charAt(0) + "CSS" + System.currentTimeMillis() % 100_000_000 + ThreadLocalRandom.current().nextInt(100, 999);

        int age = Period.between(LocalDate.parse(q.birth(), YMD), LocalDate.now()).getYears();
        if (age < 19) return reject("MINOR", tx);
        if (q.annualIncome() < 20_000_000L) return reject("LOW_INCOME", tx);
        if (CssProduct.AUTO.equals(q.product()) && blank(q.vehicleNo())) return reject("NO_VEHICLE", tx);
        if (CssProduct.MORTGAGE.equals(q.product()) && blank(q.collateralAddr())) return reject("NO_COLLATERAL", tx);

        long cap = switch (q.product()) {
            case CssProduct.AUTO -> 70_000_000L;
            case CssProduct.MORTGAGE -> 500_000_000L;
            default -> 50_000_000L;
        };
        long limit = Math.min(cap, q.annualIncome() * multiplePct(q.product()) / 100 / 1_000_000 * 1_000_000);
        if (q.requestAmt() > 0) limit = Math.min(limit, q.requestAmt());
        int discount = (int) Math.min(300, q.annualIncome() / 10_000_000 * 30);   // 소득 1천만원당 0.3%p, 최대 3%p
        int rate = Math.max(450, baseRateBp(q.product()) - discount);
        if ("FREELANCER".equals(q.employType())) rate += 100;
        return new CssResult(true, limit, rate, null, tx, engine());
    }

    private CssResult reject(String reason, String tx) {
        return new CssResult(false, 0, 0, reason, tx, engine());
    }

    private static boolean blank(String v) {
        return v == null || v.isBlank();
    }

    private static void sleep(int ms) {
        if (ms <= 0) return;
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();          // 타임아웃으로 취소됨
            throw new CssException("취소됨");
        }
    }
}
