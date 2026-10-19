package com.example.demojsp.common.product.store;

import com.example.demojsp.common.product.domain.Product;
import com.example.demojsp.common.product.domain.ProductKind;
import com.example.demojsp.common.product.mapper.ProductMapper;
import com.example.demojsp.common.product.mapper.ProductRow;
import com.example.demojsp.common.store.CommonSequence;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.List;
import java.util.Optional;

/**
 * 수신/여신 상품 DB 저장소 (TB_PRODUCT, MyBatis) - {@code common.store.type=db}(기본).
 */
@org.springframework.stereotype.Repository
@ConditionalOnProperty(name = "common.store.type", havingValue = "db", matchIfMissing = true)
public class MyBatisProductRepository implements ProductRepository {

    static final String SEQ = "PRODUCT";

    private final ProductMapper mapper;
    private final CommonSequence sequence;

    public MyBatisProductRepository(ProductMapper mapper, CommonSequence sequence) {
        this.mapper = mapper;
        this.sequence = sequence;
    }

    @Override
    public List<Product> findAll() {
        return mapper.selectAll().stream().map(MyBatisProductRepository::toDomain).toList();
    }

    @Override
    public Optional<Product> findById(long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(MyBatisProductRepository::toDomain);
    }

    @Override
    public long nextId() {
        return sequence.next(SEQ, 300);        // 메모리 저장소와 같은 번호대 (301~)
    }

    @Override
    public void save(Product p) {
        ProductRow r = new ProductRow();
        r.setProductId(p.id());
        r.setProductCd(p.code());
        r.setProductKind(p.kind().name());
        r.setSubType(p.subType());
        r.setProductNm(p.name());
        r.setSummary(p.summary());
        r.setRateText(p.rateText());
        r.setIcon(p.icon());
        r.setDetailUrl(p.detailUrl());
        r.setOnSaleYn(p.onSale() ? "Y" : "N");
        r.setHomeDispYn(p.homeDisplay() ? "Y" : "N");
        r.setHomeDispOrd(p.homeDisplay() ? p.homeOrder() : null);
        r.setUpdAt(p.updatedAt());
        r.setUpdBy(p.updatedBy());
        if (mapper.update(r) == 0) {
            mapper.insert(r);
        }
    }

    private static Product toDomain(ProductRow r) {
        boolean home = "Y".equals(r.getHomeDispYn());
        return new Product(r.getProductId(), r.getProductCd(), ProductKind.valueOf(r.getProductKind()), r.getSubType(),
                r.getProductNm(), r.getSummary(), r.getRateText(), r.getIcon(), r.getDetailUrl(), "Y".equals(r.getOnSaleYn()),
                home, home ? r.getHomeDispOrd() : null, r.getUpdAt(), r.getUpdBy());
    }
}
