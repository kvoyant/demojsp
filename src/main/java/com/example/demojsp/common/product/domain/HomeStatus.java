package com.example.demojsp.common.product.domain;

import java.util.List;

/**
 * 홈 표시 현황 (수신+여신 합산) - ADMIN 화면이 순서 선택지 음영처리·건수 안내에 사용.
 *
 * @param maxCount  최대 등록 건수 (3)
 * @param count     현재 홈 표시여부 Y 건수
 * @param slots     순서별 사용 현황 (1..maxCount)
 */
public record HomeStatus(int maxCount, int count, List<Slot> slots) {

    /** 표시순서 1칸 - 비어 있으면 productId·name 이 null */
    public record Slot(int order, Long productId, String name, String kindName) {
    }
}
