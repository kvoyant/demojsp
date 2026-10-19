package com.example.demojsp.common.resume.store;

import com.example.demojsp.common.resume.domain.Partner;
import com.example.demojsp.common.resume.mapper.PartnerRow;
import com.example.demojsp.common.resume.mapper.ResumeMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 회사(당행/제휴사) 마스터 DB 저장소 (TB_CO_PARTNER) - 공통#43 "회사명 DB 구성".
 */
@org.springframework.stereotype.Repository
@ConditionalOnProperty(name = "common.store.type", havingValue = "db", matchIfMissing = true)
public class MyBatisPartnerRepository implements PartnerRepository {

    private final ResumeMapper mapper;

    public MyBatisPartnerRepository(ResumeMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<Partner> findByCode(String code) {
        if (code == null) return Optional.empty();
        return Optional.ofNullable(mapper.selectPartner(code.strip().toUpperCase(Locale.ROOT))).map(MyBatisPartnerRepository::toDomain);
    }

    @Override
    public List<Partner> findAll() {
        return mapper.selectPartners().stream().map(MyBatisPartnerRepository::toDomain).toList();
    }

    @Override
    public void save(Partner p) {
        PartnerRow r = new PartnerRow();
        r.setPartnerCd(p.code());
        r.setPartnerNm(p.name());
        r.setLogoUrl(p.logoUrl());
        r.setOwnYn(p.own() ? "Y" : "N");
        mapper.insertPartner(r);
    }

    private static Partner toDomain(PartnerRow r) {
        return new Partner(r.getPartnerCd(), r.getPartnerNm(), r.getLogoUrl(), "Y".equals(r.getOwnYn()));
    }
}
