package com.example.demojsp.common.product.service;

import com.example.demojsp.common.product.config.ProductProperties;
import com.example.demojsp.common.product.domain.HomeSettingRequest;
import com.example.demojsp.common.product.domain.HomeStatus;
import com.example.demojsp.common.product.domain.Product;
import com.example.demojsp.common.product.domain.ProductAdminView;
import com.example.demojsp.common.product.domain.ProductKind;
import com.example.demojsp.common.product.domain.ProductUpdateRequest;
import com.example.demojsp.common.product.exception.ProductErrorCode;
import com.example.demojsp.common.product.exception.ProductException;
import com.example.demojsp.common.product.store.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * [공통#48] ADMIN 수신상품목록 / 여신상품목록 - 상품설정 "홈 표시여부 · 홈 표시순서".
 *
 * <ol>
 *   <li>홈 표시여부 Y/N 중 1개. Y = 추천 상품 영역 표시, N = 미표시</li>
 *   <li>수신상품목록 + 여신상품목록 <b>합산 3개까지</b>. 초과해 Y 시도 → 등록 불가, 안내팝업 "등록 건수 3개를 초과하였습니다."</li>
 *   <li>홈 표시순서: Y 이면 필수, 1/2/3 중 1개. <b>수신·여신 간 같은 숫자 불가</b> → "표시순서를 변경하세요"</li>
 *   <li>기존 상품설정(상품명·설명·금리·URL·판매여부)을 추천 상품 카드 각 항목에 연동</li>
 * </ol>
 * 화면 검사(음영처리·팝업)와 별개로 <b>서버가 최종 검증</b>한다 (두 관리자가 동시에 저장해도 규칙 유지 - synchronized).
 */
@Service
public class ProductAdminService {

    private static final Logger audit = LoggerFactory.getLogger("AUDIT.PRODUCT");
    private static final Pattern RELATIVE = Pattern.compile("^/(?!/)[^\\s<>\"'\\\\]{0,299}$");

    private final ProductRepository repository;
    private final ProductProperties properties;
    private final Clock clock;

    @Autowired
    public ProductAdminService(ProductRepository repository, ProductProperties properties) {
        this(repository, properties, Clock.systemDefaultZone());
    }

    public ProductAdminService(ProductRepository repository, ProductProperties properties, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    /** 수신상품목록(DEPOSIT) / 여신상품목록(LOAN) */
    public List<ProductAdminView> list(String kind) {
        ProductKind k = parseKind(kind);
        return repository.findAll().stream()
                .filter(p -> k == null || p.kind() == k)
                .sorted(Comparator.comparingLong(Product::id))
                .map(ProductAdminView::of)
                .toList();
    }

    /** 홈 표시 현황 (수신+여신 합산) */
    public HomeStatus homeStatus() {
        int max = max();
        List<Product> ys = repository.findAll().stream().filter(Product::homeDisplay).toList();
        List<HomeStatus.Slot> slots = new ArrayList<>();
        for (int i = 1; i <= max; i++) {
            final int order = i;
            Product p = ys.stream().filter(x -> x.homeOrder() != null && x.homeOrder() == order).findFirst().orElse(null);
            slots.add(p == null ? new HomeStatus.Slot(order, null, null, null)
                    : new HomeStatus.Slot(order, p.id(), p.name(), p.kind().getLabel()));
        }
        return new HomeStatus(max, ys.size(), slots);
    }

    /** 홈 표시여부 · 홈 표시순서 저장 */
    public synchronized ProductAdminView changeHome(long id, HomeSettingRequest req, String adminId) {
        Product p = find(id);
        if (req == null || req.homeDisplay() == null) {
            throw new ProductException(ProductErrorCode.REQUIRED_MISSING, "홈 표시여부");
        }
        boolean y = parseYn(req.homeDisplay());
        Integer order = null;
        if (y) {
            int max = max();
            List<Product> others = repository.findAll().stream().filter(o -> o.id() != id && o.homeDisplay()).toList();
            // 기능2 수신+여신 합산 3개 초과 → 등록 불가 (이미 Y 인 상품의 순서 변경은 건수 증가 아님)
            if (!p.homeDisplay() && others.size() >= max) {
                throw ProductException.format(ProductErrorCode.HOME_LIMIT_EXCEEDED, max);
            }
            // 기능3 Y 이면 순서 필수, 1..max, 수신·여신 간 같은 숫자 불가
            if (req.homeOrder() == null) {
                throw new ProductException(ProductErrorCode.HOME_ORDER_REQUIRED);
            }
            if (req.homeOrder() < 1 || req.homeOrder() > max) {
                throw ProductException.format(ProductErrorCode.HOME_ORDER_INVALID, max);
            }
            if (others.stream().anyMatch(o -> req.homeOrder().equals(o.homeOrder()))) {
                throw new ProductException(ProductErrorCode.HOME_ORDER_DUPLICATED);
            }
            order = req.homeOrder();
        }
        Product saved = new Product(p.id(), p.code(), p.kind(), p.subType(), p.name(), p.summary(), p.rateText(), p.icon(),
                p.detailUrl(), p.onSale(), y, order, LocalDateTime.now(clock), adminId);
        repository.save(saved);
        audit.info("[상품 홈 표시] id={} code={} kind={} homeDisplay={} homeOrder={} by={}", id, p.code(), p.kind(), y ? "Y" : "N", order, adminId);
        return ProductAdminView.of(saved);
    }

    /** 상품설정 기본 항목 수정 → 추천 상품 카드에 그대로 반영 (기능4) */
    public synchronized ProductAdminView update(long id, ProductUpdateRequest req, String adminId) {
        Product p = find(id);
        if (req == null) throw new ProductException(ProductErrorCode.INVALID_REQUEST);
        String name = text(req.name(), "상품명", 30, true);
        String summary = text(req.summary(), "상품 설명", 40, false);
        String rate = text(req.rateText(), "금리", 20, false);
        String url = text(req.detailUrl(), "상품안내페이지 URL", 300, true);
        if (!RELATIVE.matcher(url).matches()) throw new ProductException(ProductErrorCode.INVALID_URL, "상품안내페이지 URL");
        boolean onSale = req.onSale() == null ? p.onSale() : req.onSale();
        Product saved = new Product(p.id(), p.code(), p.kind(), p.subType(), name, summary, rate, p.icon(), url, onSale,
                p.homeDisplay(), p.homeOrder(), LocalDateTime.now(clock), adminId);
        repository.save(saved);
        audit.info("[상품설정 수정] id={} code={} onSale={} by={}", id, p.code(), onSale, adminId);
        return ProductAdminView.of(saved);
    }

    // ------------------------------------------------------------------ 내부

    private int max() {
        return Math.max(1, properties.getMaxHomeCount());
    }

    private Product find(long id) {
        return repository.findById(id).orElseThrow(() -> new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND));
    }

    private static ProductKind parseKind(String kind) {
        if (kind == null || kind.isBlank()) return null;
        try {
            return ProductKind.valueOf(kind.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ProductException(ProductErrorCode.INVALID_REQUEST, "상품구분");
        }
    }

    private static boolean parseYn(String v) {
        String s = v.strip().toUpperCase(Locale.ROOT);
        if (s.equals("Y") || s.equals("TRUE")) return true;
        if (s.equals("N") || s.equals("FALSE")) return false;
        throw new ProductException(ProductErrorCode.INVALID_REQUEST, "홈 표시여부");
    }

    private static String text(String v, String field, int max, boolean required) {
        String s = v == null ? null : v.strip();
        if (s == null || s.isEmpty()) {
            if (required) throw new ProductException(ProductErrorCode.REQUIRED_MISSING, field);
            return null;
        }
        if (s.length() > max) throw new ProductException(ProductErrorCode.TOO_LONG, field + "(" + max + "자)");
        return s;
    }
}
