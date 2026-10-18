package com.example.demojsp.common.resume.store;

import com.example.demojsp.common.resume.config.ResumeProperties;
import com.example.demojsp.common.resume.domain.Partner;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 회사 마스터 메모리 구현 (yaml {@code common.resume.partners}).
 */
@org.springframework.stereotype.Repository
public class InMemoryPartnerRepository implements PartnerRepository {

    private final Map<String, Partner> store = new LinkedHashMap<>();

    public InMemoryPartnerRepository(ResumeProperties properties) {
        for (ResumeProperties.PartnerSeed p : properties.getPartners()) {
            if (p.getCode() == null || p.getName() == null) continue;
            String code = p.getCode().strip().toUpperCase(Locale.ROOT);
            String logo = p.getLogoUrl() == null || p.getLogoUrl().isBlank() ? null : p.getLogoUrl().strip();
            store.put(code, new Partner(code, p.getName().strip(), logo, p.isOwn()));
        }
    }

    @Override
    public Optional<Partner> findByCode(String code) {
        return code == null ? Optional.empty() : Optional.ofNullable(store.get(code.strip().toUpperCase(Locale.ROOT)));
    }

    @Override
    public List<Partner> findAll() {
        return new ArrayList<>(store.values());
    }
}
