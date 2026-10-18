package com.example.demojsp.common.resume.store;

import com.example.demojsp.common.resume.domain.ResumePoint;

import java.util.List;
import java.util.Optional;

/**
 * 이어하기 저장소.
 *
 * <p>기본 구현은 {@link InMemoryResumeRepository}(yaml seed). 운영에서는 각 신청 원장(대출신청·계좌개설 등)의 진행 단계를
 * 조회하는 MyBatis 구현체를 만들어 {@code @Primary} 로 등록한다.
 */
public interface ResumeRepository {

    /** 고객의 전체 이어하기 (상태 무관) */
    List<ResumePoint> findByCustomer(String customerId);

    Optional<ResumePoint> findById(long id);

    long nextId();

    /** 등록 또는 수정 (같은 ID 면 교체) */
    void save(ResumePoint point);

    /** 전체 (local 테스트 화면용) */
    List<ResumePoint> findAll();
}
