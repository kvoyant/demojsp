package com.example.demojsp.common.producttab.service;

import com.example.demojsp.common.producttab.dto.ProductTabView;

import java.time.LocalDate;

/**
 * [CF-BC-20 / 공통#26] 메인화면 상품 탭 - 사용자 화면(APP / PC Web) 조회.
 *
 * <pre>
 *  getView("APP")  →  ① 상단 슬라이드 카드 (게시 중 · 채널 · 판매중 상품만, 표시순서, 최대 N장)
 *                     ② 섹션 목록 (사용 중, 표시순서) + 섹션별 상품 (판매중 상품만, 표시순서)
 * </pre>
 * 화면에 무엇을 보여줄지는 전부 DB(디지털어드민 설정)가 정한다. 화면 코드에는 상품·카드가 하나도 없다.
 */
public interface ProductTabService {

    /** 오늘 기준 상품 탭 */
    ProductTabView getView(String channel);

    /** 기준일 상품 탭 (ADMIN 미리보기: 게시 예정 카드 확인용) */
    ProductTabView getView(String channel, LocalDate baseDt);
}
