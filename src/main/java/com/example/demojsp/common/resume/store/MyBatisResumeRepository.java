package com.example.demojsp.common.resume.store;

import com.example.demojsp.common.resume.domain.ProductType;
import com.example.demojsp.common.resume.domain.ResumePoint;
import com.example.demojsp.common.resume.domain.ResumeStatus;
import com.example.demojsp.common.resume.mapper.ResumeMapper;
import com.example.demojsp.common.resume.mapper.ResumePointRow;
import com.example.demojsp.common.store.CommonSequence;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.List;
import java.util.Optional;

/**
 * 이어하기 DB 저장소 (TB_RESUME_POINT, MyBatis) - {@code common.store.type=db}(기본).
 */
@org.springframework.stereotype.Repository
@ConditionalOnProperty(name = "common.store.type", havingValue = "db", matchIfMissing = true)
public class MyBatisResumeRepository implements ResumeRepository {

    static final String SEQ = "RESUME_POINT";

    private final ResumeMapper mapper;
    private final CommonSequence sequence;

    public MyBatisResumeRepository(ResumeMapper mapper, CommonSequence sequence) {
        this.mapper = mapper;
        this.sequence = sequence;
    }

    @Override
    public List<ResumePoint> findByCustomer(String customerId) {
        return mapper.selectByCustomer(customerId).stream().map(MyBatisResumeRepository::toDomain).toList();
    }

    @Override
    public Optional<ResumePoint> findById(long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(MyBatisResumeRepository::toDomain);
    }

    @Override
    public long nextId() {
        return sequence.next(SEQ, 5000);       // 메모리 저장소와 같은 번호대 (5001~)
    }

    @Override
    public void save(ResumePoint p) {
        ResumePointRow row = toRow(p);
        if (mapper.update(row) == 0) {
            mapper.insert(row);
        }
    }

    @Override
    public List<ResumePoint> findAll() {
        return mapper.selectAll().stream().map(MyBatisResumeRepository::toDomain).toList();
    }

    static ResumePoint toDomain(ResumePointRow r) {
        return new ResumePoint(r.getResumeId(), r.getCustomerId(), ProductType.valueOf(r.getProductType()), r.getBizCd(),
                r.getBizNm(), r.getProductCd(), r.getProductNm(), r.getPartnerCd(), r.getStepNm(), r.getStepNo(),
                r.getTotalSteps(), r.getResumeUrl(), ResumeStatus.valueOf(r.getStatus()), r.getStartedAt(),
                r.getLastActivityAt(), r.getExpiresAt());
    }

    static ResumePointRow toRow(ResumePoint p) {
        ResumePointRow r = new ResumePointRow();
        r.setResumeId(p.id());
        r.setCustomerId(p.customerId());
        r.setProductType(p.productType().name());
        r.setBizCd(p.bizCode());
        r.setBizNm(p.bizName());
        r.setProductCd(p.productCode());
        r.setProductNm(p.productName());
        r.setPartnerCd(p.partnerCode());
        r.setStepNm(p.stepName());
        r.setStepNo(p.stepNo());
        r.setTotalSteps(p.totalSteps());
        r.setResumeUrl(p.resumeUrl());
        r.setStatus(p.status().name());
        r.setStartedAt(p.startedAt());
        r.setLastActivityAt(p.lastActivityAt());
        r.setExpiresAt(p.expiresAt());
        return r;
    }
}
