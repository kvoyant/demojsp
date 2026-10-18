package com.example.demojsp.common.terms.store;

import com.example.demojsp.common.terms.domain.TermsItem;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 약관 원장 저장소 (ADMIN 약관관리 데이터).
 *
 * <p>기본 구현은 {@link InMemoryTermsRepository}(yaml seed). 운영에서는 기존 TB_TERMS 를 확장하거나
 * 신규 테이블(BIZ_CODE, TERMS_CODE, VERSION PK)을 조회하는 MyBatis 구현체를 만들어 {@code @Primary} 로 등록한다.
 */
public interface TermsRepository {

    /** 업무코드의 현재 약관 목록 (약관코드별 최신 버전, sortOrder 순) */
    List<TermsItem> findCurrent(String bizCode);

    /** 업무코드의 특정 약관 현재 버전 */
    Optional<TermsItem> findCurrent(String bizCode, String termsCode);

    /** 업무코드의 모든 약관·모든 버전 (ADMIN 이력 조회) */
    List<TermsItem> findAllVersions(String bizCode);

    /** 신규 약관 또는 새 버전 등록. 같은 버전이 이미 있으면 false */
    boolean add(TermsItem item);

    /** 약관이 등록된 업무코드 목록 */
    Set<String> bizCodes();
}
