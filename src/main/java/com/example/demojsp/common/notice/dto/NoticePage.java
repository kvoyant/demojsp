package com.example.demojsp.common.notice.dto;

import lombok.Getter;

import java.util.List;

/** 공지 목록 한 페이지 - 화면은 [더보기] 로 다음 페이지를 이어 붙인다 (hasNext) */
@Getter
public class NoticePage {

    private final List<NoticeDto> items;
    private final int total;
    private final int page;
    private final int size;
    private final boolean hasNext;

    public NoticePage(List<NoticeDto> items, int total, int page, int size) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.size = size;
        this.hasNext = (long) page * size < total;
    }
}
