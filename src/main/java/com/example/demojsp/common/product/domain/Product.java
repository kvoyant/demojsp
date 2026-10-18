package com.example.demojsp.common.product.domain;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 상품 1건 (ADMIN 수신상품목록 / 여신상품목록 원장).
 *
 * <p>추천 상품 카드의 상품명·설명·금리·이동 URL 은 모두 이 원장에서 가져온다
 * (공통#48 기능4 "ADMIN 기존 상품설정 내용을 콘텐츠 각 항목에 연동") → 상품설정을 고치면 홈 카드도 바로 바뀐다.
 *
 * @param id          상품 ID
 * @param code        상품코드
 * @param kind        수신/여신
 * @param subType     상품유형명 (입출금, 적금, 정기예금, 신용대출, 담보대출 ...)
 * @param name        상품명
 * @param summary     상품 한줄 설명
 * @param rateText    금리 문구 (예: "최고 연 4.5%")
 * @param icon        공통 아이콘(공통#41) 이름
 * @param detailUrl   상품안내페이지 URL (공통#47 기능3)
 * @param onSale      판매중 (판매중지 상품은 홈 표시여부가 Y 여도 노출 안 함)
 * @param homeDisplay 홈 표시여부 (공통#48)
 * @param homeOrder   홈 표시순서 1~3 (홈 표시여부 N 이면 null)
 * @param updatedAt   수정일시
 * @param updatedBy   수정자
 */
public record Product(
        long id,
        String code,
        ProductKind kind,
        String subType,
        String name,
        String summary,
        String rateText,
        String icon,
        String detailUrl,
        boolean onSale,
        boolean homeDisplay,
        Integer homeOrder,
        LocalDateTime updatedAt,
        String updatedBy
) implements Serializable {

    /** 홈 추천 영역 노출 대상 */
    public boolean shownOnHome() {
        return onSale && homeDisplay && homeOrder != null;
    }
}
