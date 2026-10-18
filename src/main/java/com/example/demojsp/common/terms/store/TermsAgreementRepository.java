package com.example.demojsp.common.terms.store;

import com.example.demojsp.common.terms.domain.AgreementRecord;

import java.util.List;

/**
 * 약관 동의 이력(증적) 저장소.
 *
 * <p>운영에서는 기존 TB_TERMS_AGREE_LOG 에 BIZ_CODE, TERMS_VERSION, CONTENT_HASH, SUBJECT_ID, CHANNEL, CLIENT_IP
 * 컬럼을 추가하거나 신규 테이블로 구성한 MyBatis 구현체를 {@code @Primary} 로 등록한다.
 * 이력은 수정/삭제하지 않는다(INSERT ONLY).
 */
public interface TermsAgreementRepository {

    void saveAll(List<AgreementRecord> records);

    /**
     * 이력 조회 (최신순).
     *
     * @param bizCode   null 이면 전체
     * @param subjectId null 이면 전체
     * @param limit     최대 건수
     */
    List<AgreementRecord> find(String bizCode, String subjectId, int limit);
}
