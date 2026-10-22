package com.example.demojsp.common.platformlink.adapter;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 어댑터 찾기: 스프링이 PartnerAdapter 구현체(@Component)를 모두 모아 주고, ADAPTER_CD 로 고른다.
 * → 서비스에 "if (TOSS) ... else if (KAKAO) ..." 분기가 생기지 않는다.
 */
@Component
public class PartnerAdapterRegistry {

    private final Map<String, PartnerAdapter> byCd = new TreeMap<>();

    public PartnerAdapterRegistry(List<PartnerAdapter> adapters) {
        for (PartnerAdapter a : adapters) {
            if (byCd.put(a.adapterCd(), a) != null) throw new IllegalStateException("어댑터 코드 중복: " + a.adapterCd());
        }
    }

    /** 없으면 null (등록 안 된 ADAPTER_CD → 연계 설정 오류) */
    public PartnerAdapter get(String adapterCd) {
        return adapterCd == null ? null : byCd.get(adapterCd);
    }

    public Collection<PartnerAdapter> all() {
        return byCd.values();
    }
}
