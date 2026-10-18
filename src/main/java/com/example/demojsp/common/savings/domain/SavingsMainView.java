package com.example.demojsp.common.savings.domain;

import java.util.List;

/**
 * 저축 탭 메인 콘텐츠 (공통#51).
 *
 * @param eligible    제공대상 여부 - 계좌 미보유(비로그인·일반회원) true / 계좌 보유(금융회원) false → 계좌정보 영역
 * @param segment     회원구분 (로그인 전/후 같은 화면 - 정책2)
 * @param lines       텍스트 줄 ("매일 쌓이는 이자", "모으는 재미가 쏠쏠")
 * @param imageUrl    이미지
 * @param imageAlt    이미지 대체텍스트
 * @param ctaText     버튼 문구 ("계좌 개설하기")
 * @param ctaUrl      버튼 이동 = 입출금통장 상품안내페이지
 * @param productName 연결 상품명 (스크린리더 보조 안내)
 */
public record SavingsMainView(
        boolean eligible,
        String segment,
        List<String> lines,
        String imageUrl,
        String imageAlt,
        String ctaText,
        String ctaUrl,
        String productName
) {
}
