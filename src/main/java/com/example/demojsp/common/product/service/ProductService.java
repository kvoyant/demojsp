package com.example.demojsp.common.product.service;

import com.example.demojsp.common.member.MemberContext;
import com.example.demojsp.common.member.MemberSegment;
import com.example.demojsp.common.product.config.ProductProperties;
import com.example.demojsp.common.product.domain.Product;
import com.example.demojsp.common.product.domain.ProductKind;
import com.example.demojsp.common.product.domain.RecommendItem;
import com.example.demojsp.common.product.domain.RecommendView;
import com.example.demojsp.common.product.store.InMemoryProductRepository;
import com.example.demojsp.common.product.store.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * [공통#47] 홈 - 추천 상품.
 *
 * <ul>
 *   <li>정책1 제공대상: 금융회원 외 모든 회원 (비로그인·일반회원) / 기능4 금융회원 화면은 "추천 상품" 전체 숨김</li>
 *   <li>정책2 상품 노출순서 ADMIN 처리 → 상품 원장의 홈 표시여부 Y · 홈 표시순서(공통#48)로 결정</li>
 *   <li>기능1 상품목록 (여신·수신·입출금), 기능2 [더보기] → 전체상품목록(상품몰), 기능3 상품 터치 → 상품안내페이지</li>
 * </ul>
 * 카드 내용(상품명·설명·금리·URL)은 상품 원장 그대로 (공통#48 기능4 연동).
 */
@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    protected final ProductRepository repository;
    protected final ProductProperties properties;
    protected final Clock clock;

    @Autowired
    public ProductService(ProductRepository repository, ProductProperties properties) {
        this(repository, properties, Clock.systemDefaultZone());
    }

    public ProductService(ProductRepository repository, ProductProperties properties, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
        if (repository instanceof InMemoryProductRepository && repository.findAll().isEmpty()) {
            loadSeed();
        }
    }

    /** 홈 추천 상품 */
    public RecommendView recommend(MemberContext member) {
        if (member.segment() == MemberSegment.FINANCIAL) {
            return new RecommendView(false, member.segment().name(), properties.getMallUrl(), List.of());
        }
        List<RecommendItem> items = repository.findAll().stream()
                .filter(Product::shownOnHome)
                .sorted(Comparator.comparing(Product::homeOrder).thenComparingLong(Product::id))
                .limit(Math.max(0, properties.getMaxHomeCount()))
                .map(RecommendItem::of)
                .toList();
        return new RecommendView(true, member.segment().name(), properties.getMallUrl(), items);
    }

    private void loadSeed() {
        LocalDateTime now = LocalDateTime.now(clock);
        for (ProductProperties.Seed s : properties.getSeed()) {
            try {
                ProductKind kind = ProductKind.valueOf(s.getKind().strip().toUpperCase(Locale.ROOT));
                repository.save(new Product(repository.nextId(), s.getCode(), kind, s.getSubType(), s.getName(), s.getSummary(),
                        s.getRateText(), s.getIcon(), s.getDetailUrl(), s.isOnSale(), s.isHomeDisplay(),
                        s.isHomeDisplay() ? s.getHomeOrder() : null, now, "SYSTEM"));
            } catch (RuntimeException e) {
                log.warn("[상품] seed 적재 제외 code={} 사유={}", s.getCode(), e.getMessage());
            }
        }
    }
}
