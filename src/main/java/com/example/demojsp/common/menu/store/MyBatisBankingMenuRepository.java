package com.example.demojsp.common.menu.store;

import com.example.demojsp.common.menu.domain.BankingMenu;
import com.example.demojsp.common.menu.mapper.BankingMenuMapper;
import com.example.demojsp.common.menu.mapper.BankingMenuRow;
import com.example.demojsp.common.store.CommonSequence;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.List;
import java.util.Optional;

/**
 * 뱅킹메뉴 DB 저장소 (TB_BANKING_MENU, MyBatis) - {@code common.store.type=db}(기본).
 */
@org.springframework.stereotype.Repository
@ConditionalOnProperty(name = "common.store.type", havingValue = "db", matchIfMissing = true)
public class MyBatisBankingMenuRepository implements BankingMenuRepository {

    static final String SEQ = "BANKING_MENU";

    private final BankingMenuMapper mapper;
    private final CommonSequence sequence;

    public MyBatisBankingMenuRepository(BankingMenuMapper mapper, CommonSequence sequence) {
        this.mapper = mapper;
        this.sequence = sequence;
    }

    @Override
    public List<BankingMenu> findAll() {
        return mapper.selectAll().stream().map(MyBatisBankingMenuRepository::toDomain).toList();
    }

    @Override
    public Optional<BankingMenu> findById(long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(MyBatisBankingMenuRepository::toDomain);
    }

    @Override
    public long nextId() {
        return sequence.next(SEQ, 100);        // 메모리 저장소와 같은 번호대 (101~)
    }

    @Override
    public void save(BankingMenu m) {
        BankingMenuRow r = new BankingMenuRow();
        r.setMenuId(m.id());
        r.setCategoryCd(m.category());
        r.setCategoryNm(m.categoryName());
        r.setMenuNm(m.name());
        r.setMenuUrl(m.url());
        r.setDisplayOrder(m.displayOrder());
        r.setUseYn(m.use() ? "Y" : "N");
        r.setNewYn(m.newBadge() ? "Y" : "N");
        r.setUpdAt(m.updatedAt());
        r.setUpdBy(m.updatedBy());
        if (mapper.update(r) == 0) {
            mapper.insert(r);
        }
    }

    private static BankingMenu toDomain(BankingMenuRow r) {
        return new BankingMenu(r.getMenuId(), r.getCategoryCd(), r.getCategoryNm(), r.getMenuNm(), r.getMenuUrl(),
                r.getDisplayOrder(), "Y".equals(r.getUseYn()), "Y".equals(r.getNewYn()), r.getUpdAt(), r.getUpdBy());
    }
}
