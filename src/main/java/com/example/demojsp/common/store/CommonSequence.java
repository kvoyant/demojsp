package com.example.demojsp.common.store;

import com.example.demojsp.common.store.mapper.CommonSequenceMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 공통 채번 (TB_CO_SEQ). MySQL 에는 시퀀스가 없으므로 채번 행을 {@code UPDATE ... +1} 로 잠그고 읽는다.
 *
 * <p>별도 트랜잭션(REQUIRES_NEW)에서 증가 → 조회 → 커밋하므로, 서버가 여러 대여도 같은 번호가 나오지 않는다
 * (행 잠금은 UPDATE 순간부터 커밋까지 유지). 업무 트랜잭션이 롤백돼도 번호는 건너뛸 뿐 재사용하지 않는다.
 */
public class CommonSequence {

    private final CommonSequenceMapper mapper;
    private final TransactionTemplate tx;

    public CommonSequence(CommonSequenceMapper mapper, PlatformTransactionManager transactionManager) {
        this.mapper = mapper;
        this.tx = new TransactionTemplate(transactionManager);
        this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * @param seqName    채번 이름 (예: RESUME_POINT)
     * @param startValue 처음 쓸 때의 기준값 (첫 번호 = startValue + 1, 메모리 저장소와 같은 번호대)
     */
    public long next(String seqName, long startValue) {
        Long v = tx.execute(status -> {
            if (mapper.increase(seqName) == 0) {
                try {
                    mapper.insert(seqName, startValue);
                } catch (DuplicateKeyException e) {
                    // 다른 서버가 동시에 처음 만든 경우 - 그 행을 증가시키면 된다
                }
                mapper.increase(seqName);
            }
            return mapper.current(seqName);
        });
        if (v == null) {
            throw new IllegalStateException("채번 실패: " + seqName);
        }
        return v;
    }
}
