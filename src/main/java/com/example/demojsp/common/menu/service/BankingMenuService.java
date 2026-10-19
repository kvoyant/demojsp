package com.example.demojsp.common.menu.service;

import com.example.demojsp.common.menu.config.MenuProperties;
import com.example.demojsp.common.menu.domain.AllMenuView;
import com.example.demojsp.common.menu.domain.BankingMenu;
import com.example.demojsp.common.menu.domain.BankingMenuRequest;
import com.example.demojsp.common.menu.exception.MenuErrorCode;
import com.example.demojsp.common.menu.exception.MenuException;
import com.example.demojsp.common.menu.store.BankingMenuRepository;
import com.example.demojsp.common.menu.store.InMemoryBankingMenuRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * [공통#46 기능4·5] 전체메뉴 · ADMIN 뱅킹메뉴목록.
 *
 * <ul>
 *   <li>기능4 APP 전체메뉴 > 상품 > <b>전체상품</b>, 예금상품, 대출상품, 금융계산기 (AS-IS 에 전체상품 없음)</li>
 *   <li>기능5 ADMIN 뱅킹메뉴목록: "전체상품" 신규등록 + 예금상품/대출상품/금융계산기 표시순서 수정</li>
 * </ul>
 * 표시순서는 카테고리 안에서 항상 1..N (중복·빈 번호 없음)으로 정리한다 - 원하는 자리에 넣으면 나머지가 밀린다.
 */
@Service
public class BankingMenuService implements com.example.demojsp.common.store.CommonSeeder {

    private static final Logger audit = LoggerFactory.getLogger("AUDIT.MENU");
    private static final Logger log = LoggerFactory.getLogger(BankingMenuService.class);
    private static final Pattern RELATIVE = Pattern.compile("^/(?!/)[^\\s<>\"'\\\\]{0,299}$");
    static final int MAX_NAME = 20;

    private final BankingMenuRepository repository;
    private final MenuProperties properties;
    private final Clock clock;
    /** yaml 에 정의된 카테고리 코드 → 이름 (정의 순서 = 전체메뉴 카테고리 순서). DB 에만 있는 카테고리는 뒤에 붙는다 */
    private final Map<String, String> seedCategories = new LinkedHashMap<>();

    @Autowired
    public BankingMenuService(BankingMenuRepository repository, MenuProperties properties) {
        this(repository, properties, Clock.systemDefaultZone());
    }

    public BankingMenuService(BankingMenuRepository repository, MenuProperties properties, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
        for (MenuProperties.BankingSeed s : properties.getBankingSeed()) {
            seedCategories.putIfAbsent(s.getCategory(), s.getCategoryName());
        }
        if (repository instanceof InMemoryBankingMenuRepository && repository.findAll().isEmpty()) {
            loadSeed();
        }
    }

    @Override
    public String seedName() {
        return "뱅킹메뉴";
    }

    /** DB 저장소: 뱅킹메뉴 테이블이 비어 있으면 yaml seed 적재 (CommonStoreInitializer 가 기동 후 호출) */
    @Override
    public void seedIfEmpty() {
        if (repository.findAll().isEmpty()) {
            loadSeed();
            log.info("[뱅킹메뉴] seed 적재 완료");
        }
    }

    private void loadSeed() {
        LocalDateTime now = LocalDateTime.now(clock);
        for (MenuProperties.BankingSeed s : properties.getBankingSeed()) {
            repository.save(new BankingMenu(repository.nextId(), s.getCategory(), s.getCategoryName(), s.getName(),
                    s.getUrl(), s.getDisplayOrder(), s.isUse(), s.isNewBadge(), now, "SYSTEM"));
        }
    }

    /** 카테고리 = yaml 정의 + 저장소에만 있는 카테고리 (ADMIN 이 DB 에 직접 추가한 경우) */
    private Map<String, String> categoryMap() {
        Map<String, String> map = new LinkedHashMap<>(seedCategories);
        for (BankingMenu m : repository.findAll()) {
            map.putIfAbsent(m.category(), m.categoryName());
        }
        return map;
    }

    /** APP 전체메뉴 (사용 메뉴만, 카테고리별 표시순서) */
    public AllMenuView allMenu() {
        List<AllMenuView.Category> list = new ArrayList<>();
        for (Map.Entry<String, String> c : categoryMap().entrySet()) {
            List<AllMenuView.Item> items = inCategory(c.getKey()).stream()
                    .filter(BankingMenu::use)
                    .map(m -> new AllMenuView.Item(m.id(), m.name(), m.url(), m.newBadge()))
                    .toList();
            if (!items.isEmpty()) list.add(new AllMenuView.Category(c.getKey(), c.getValue(), items));
        }
        return new AllMenuView(list);
    }

    /** ADMIN 뱅킹메뉴목록 (카테고리 필터 선택) */
    public List<BankingMenu> adminList(String category) {
        List<BankingMenu> rows = new ArrayList<>();
        for (String c : categoryMap().keySet()) {
            if (category == null || category.isBlank() || c.equals(category)) rows.addAll(inCategory(c));
        }
        return rows;
    }

    public Map<String, String> categories() {
        return categoryMap();
    }

    public synchronized BankingMenu register(BankingMenuRequest req, String adminId) {
        BankingMenu draft = validate(req, null, adminId);
        BankingMenu saved = new BankingMenu(repository.nextId(), draft.category(), draft.categoryName(), draft.name(),
                draft.url(), draft.displayOrder(), draft.use(), draft.newBadge(), draft.updatedAt(), adminId);
        placeAt(saved);
        audit.info("[뱅킹메뉴 등록] id={} category={} name={} order={} by={}", saved.id(), saved.category(), saved.name(), saved.displayOrder(), adminId);
        return find(saved.id());
    }

    public synchronized BankingMenu update(long id, BankingMenuRequest req, String adminId) {
        BankingMenu old = find(id);
        BankingMenu draft = validate(req, old, adminId);
        BankingMenu saved = new BankingMenu(id, draft.category(), draft.categoryName(), draft.name(), draft.url(),
                draft.displayOrder(), draft.use(), draft.newBadge(), draft.updatedAt(), adminId);
        placeAt(saved);                                   // 같은 ID 로 저장 → 카테고리를 옮기면 원래 카테고리에서 빠진다
        if (!old.category().equals(saved.category())) {
            renumber(old.category());                     // 원래 카테고리 빈 번호 정리
        }
        audit.info("[뱅킹메뉴 수정] id={} order={} use={} by={}", id, saved.displayOrder(), saved.use(), adminId);
        return find(id);
    }

    /**
     * 카테고리 표시순서 일괄 변경 (목록에서 위/아래로 옮긴 결과).
     *
     * @param ids 카테고리의 <b>모든</b> 메뉴 ID 를 원하는 순서대로
     */
    public synchronized List<BankingMenu> reorder(String category, List<Long> ids, String adminId) {
        requireCategory(category);
        List<BankingMenu> current = inCategory(category);
        if (ids == null || ids.size() != current.size() || !new HashSet<>(ids).equals(new HashSet<>(current.stream().map(BankingMenu::id).toList()))) {
            throw new MenuException(MenuErrorCode.INVALID_ORDER, "카테고리 메뉴 전체 ID 필요");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        for (int i = 0; i < ids.size(); i++) {
            BankingMenu m = find(ids.get(i));
            repository.save(new BankingMenu(m.id(), m.category(), m.categoryName(), m.name(), m.url(), i + 1, m.use(), m.newBadge(), now, adminId));
        }
        audit.info("[뱅킹메뉴 순서] category={} ids={} by={}", category, ids, adminId);
        return inCategory(category);
    }

    // ------------------------------------------------------------------ 내부

    private List<BankingMenu> inCategory(String category) {
        return repository.findAll().stream().filter(m -> m.category().equals(category)).sorted(BankingMenu.ORDER).toList();
    }

    private BankingMenu find(long id) {
        return repository.findById(id).orElseThrow(() -> new MenuException(MenuErrorCode.MENU_NOT_FOUND));
    }

    /** 원하는 순번에 넣고 카테고리를 1..N 으로 다시 매김 */
    private void placeAt(BankingMenu menu) {
        List<BankingMenu> others = new ArrayList<>(inCategory(menu.category()).stream().filter(m -> m.id() != menu.id()).toList());
        int pos = Math.max(0, Math.min(menu.displayOrder() - 1, others.size()));
        others.add(pos, menu);
        for (int i = 0; i < others.size(); i++) repository.save(others.get(i).withOrder(i + 1));
    }

    private void renumber(String category) {
        List<BankingMenu> list = inCategory(category);
        for (int i = 0; i < list.size(); i++) repository.save(list.get(i).withOrder(i + 1));
    }

    private void requireCategory(String category) {
        if (category == null || !categoryMap().containsKey(category)) {
            throw new MenuException(MenuErrorCode.INVALID_CATEGORY, "카테고리");
        }
    }

    private BankingMenu validate(BankingMenuRequest req, BankingMenu old, String adminId) {
        if (req == null) throw new MenuException(MenuErrorCode.INVALID_REQUEST);
        requireCategory(req.category());
        String name = req.name() == null ? "" : req.name().strip();
        if (name.isEmpty()) throw new MenuException(MenuErrorCode.REQUIRED_MISSING, "메뉴명");
        if (name.length() > MAX_NAME) throw new MenuException(MenuErrorCode.TOO_LONG, "메뉴명(" + MAX_NAME + "자)");
        boolean dup = inCategory(req.category()).stream()
                .anyMatch(m -> (old == null || m.id() != old.id()) && m.name().equals(name));
        if (dup) throw new MenuException(MenuErrorCode.DUPLICATE_NAME, "메뉴명");
        String url = req.url() == null ? "" : req.url().strip();
        if (url.isEmpty()) throw new MenuException(MenuErrorCode.REQUIRED_MISSING, "URL");
        if (!RELATIVE.matcher(url).matches()) throw new MenuException(MenuErrorCode.INVALID_URL, "URL");
        if (req.displayOrder() == null) throw new MenuException(MenuErrorCode.REQUIRED_MISSING, "표시순서");
        if (req.displayOrder() < 1 || req.displayOrder() > 999) throw new MenuException(MenuErrorCode.INVALID_ORDER, "표시순서");
        return new BankingMenu(old == null ? 0 : old.id(), req.category(), categoryMap().get(req.category()), name, url,
                req.displayOrder(), req.use() == null || req.use(), Boolean.TRUE.equals(req.newBadge()),
                LocalDateTime.now(clock), adminId);
    }
}
