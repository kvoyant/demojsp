package com.example.demojsp.common.consent.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** [ADMIN] 업무별 약관 1행 저장 요청 (PUT 은 업무의 목록 전체를 이 값으로 바꾼다) */
@Getter
@Setter
@NoArgsConstructor
public class ConsentBizTermsRequest {

    private String termsCd;
    private String reqYn;
    private Integer sortOrd;
    private String useYn;

    public ConsentBizTermsRequest(String termsCd, String reqYn, Integer sortOrd, String useYn) {
        this.termsCd = termsCd;
        this.reqYn = reqYn;
        this.sortOrd = sortOrd;
        this.useYn = useYn;
    }
}
