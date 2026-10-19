package com.example.demojsp.common.notice.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** 목록 화면 탭 · 필터 칩 (게시 중 공지 기준) */
@Getter
@Setter
public class NoticeFilters {

    private int allCnt;                                       // [전체] 탭 건수
    private List<NoticeTargetDto> products = new ArrayList<>(); // [상품별] 탭 칩
    private List<NoticeTargetDto> bizs = new ArrayList<>();     // [업무별] 탭 칩
}
