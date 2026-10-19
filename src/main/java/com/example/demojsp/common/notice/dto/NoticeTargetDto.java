package com.example.demojsp.common.notice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 목록 화면 필터 칩 1개 = 게시 중 공지가 있는 상품/업무 (예: "페퍼스마트대출 1").
 * ADMIN 대상 선택 상자에도 같은 모양으로 쓴다 (cnt 없음).
 */
@Getter
@Setter
@NoArgsConstructor
public class NoticeTargetDto {

    private String scopeCd;      // PRODUCT / BIZ
    private String targetCd;
    private String targetNm;
    private Integer cnt;
}
