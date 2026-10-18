package com.example.demojsp.common.terms.store;

import com.example.demojsp.common.terms.config.TermsProperties;
import com.example.demojsp.common.terms.domain.TermsItem;
import com.example.demojsp.common.terms.service.TermsSupport;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 메모리 약관 저장소 (기본 구현) - 기동 시 {@code common.terms.seed} 를 적재한다.
 *
 * <p>ADMIN API 로 등록한 약관은 메모리에만 있으므로 재기동하면 seed 상태로 돌아간다. (local 테스트 용도)
 * 운영은 DB 구현체로 교체할 것 ({@link TermsRepository} 참고).
 *
 * <p>버전 목록은 "등록 순서" 로 보관하고 마지막에 등록된 버전을 현재 약관으로 본다.
 */
@Component
public class InMemoryTermsRepository implements TermsRepository {

    /** key = bizCode|termsCode, value = 등록 순서대로의 버전 목록 */
    private final Map<String, List<TermsItem>> store = new LinkedHashMap<>();

    public InMemoryTermsRepository(TermsProperties properties) {
        for (TermsProperties.Seed s : properties.getSeed()) {
            add(new TermsItem(s.getBizCode(), s.getCode(), s.getTitle(), s.isRequired(), s.getVersion(),
                    s.getSortOrder(), s.getContent(), s.getPdfUrl(), TermsSupport.sha256(s.getContent()),
                    TermsSupport.now(), "SYSTEM"));
        }
    }

    @Override
    public synchronized List<TermsItem> findCurrent(String bizCode) {
        List<TermsItem> result = new ArrayList<>();
        for (List<TermsItem> versions : store.values()) {
            TermsItem latest = versions.get(versions.size() - 1);
            if (latest.bizCode().equals(bizCode)) {
                result.add(latest);
            }
        }
        result.sort(Comparator.comparingInt(TermsItem::sortOrder));
        return result;
    }

    @Override
    public synchronized Optional<TermsItem> findCurrent(String bizCode, String termsCode) {
        List<TermsItem> versions = store.get(key(bizCode, termsCode));
        return versions == null ? Optional.empty() : Optional.of(versions.get(versions.size() - 1));
    }

    @Override
    public synchronized List<TermsItem> findAllVersions(String bizCode) {
        List<TermsItem> result = new ArrayList<>();
        store.values().forEach(versions -> versions.stream()
                .filter(t -> t.bizCode().equals(bizCode))
                .forEach(result::add));
        result.sort(Comparator.comparingInt(TermsItem::sortOrder));
        return result;
    }

    @Override
    public synchronized boolean add(TermsItem item) {
        List<TermsItem> versions = store.computeIfAbsent(key(item.bizCode(), item.termsCode()), k -> new ArrayList<>());
        if (versions.stream().anyMatch(v -> v.version().equals(item.version()))) {
            return false;
        }
        versions.add(item);
        return true;
    }

    @Override
    public synchronized Set<String> bizCodes() {
        Set<String> codes = new LinkedHashSet<>();
        store.values().forEach(v -> codes.add(v.get(0).bizCode()));
        return codes;
    }

    private static String key(String bizCode, String termsCode) {
        return bizCode + "|" + termsCode;
    }
}
