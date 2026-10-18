package com.example.demojsp.common.terms.store;

import com.example.demojsp.common.terms.domain.AgreementRecord;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/**
 * 메모리 동의 이력 저장소 (local 테스트용). 최근 {@value #MAX_SIZE} 건만 보관한다.
 * <b>운영은 반드시 DB 구현체로 교체</b> (증적은 법정 보존기간 동안 보관 필요).
 */
@Component
public class InMemoryTermsAgreementRepository implements TermsAgreementRepository {

    static final int MAX_SIZE = 2000;

    /** 최신 이력이 앞쪽 */
    private final Deque<AgreementRecord> records = new ArrayDeque<>();

    @Override
    public synchronized void saveAll(List<AgreementRecord> list) {
        for (AgreementRecord r : list) {
            records.addFirst(r);
        }
        while (records.size() > MAX_SIZE) {
            records.removeLast();
        }
    }

    @Override
    public synchronized List<AgreementRecord> find(String bizCode, String subjectId, int limit) {
        List<AgreementRecord> result = new ArrayList<>();
        Iterator<AgreementRecord> it = records.iterator();
        while (it.hasNext() && result.size() < limit) {
            AgreementRecord r = it.next();
            if ((bizCode == null || bizCode.equals(r.bizCode())) && (subjectId == null || subjectId.equals(r.subjectId()))) {
                result.add(r);
            }
        }
        return result;
    }
}
