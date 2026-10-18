package com.example.demojsp.common.banner.store;

import com.example.demojsp.common.banner.domain.BannerPopup;

import java.util.List;
import java.util.Optional;

/**
 * 배너팝업 원장 저장소 (디지털어드민 "배너팝업목록" 데이터).
 *
 * <p>기본 구현은 {@link InMemoryBannerPopupRepository}(yaml seed). 운영에서는 기존 배너팝업 테이블을 조회하는
 * MyBatis 구현체를 만들어 {@code @Primary} 로 등록한다.
 *
 * <p><b>AS-IS 1건만 노출되던 원인 차단</b>: DB 구현 시 노출 조회 쿼리에 {@code LIMIT 1} / {@code ROWNUM = 1} 을 두지 말고
 * 게시기간 조건만 걸어 <b>전부</b> 가져온 뒤 서비스의 표시순서 정렬을 따른다.
 */
public interface BannerPopupRepository {

    /** 전체 배너 (등록 순) */
    List<BannerPopup> findAll();

    Optional<BannerPopup> findById(long id);

    /** 신규 ID 채번 */
    long nextId();

    /** 등록 또는 수정 (같은 ID 면 교체) */
    void save(BannerPopup banner);

    /** 삭제. 없으면 false */
    boolean delete(long id);
}
