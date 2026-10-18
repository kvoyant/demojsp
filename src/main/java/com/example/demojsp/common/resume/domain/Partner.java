package com.example.demojsp.common.resume.domain;

import java.io.Serializable;

/**
 * 회사(당행/제휴사) 마스터 - 공통#43 "회사명: DB 구성 필요".
 *
 * <p>이어하기 카드의 로고·회사명은 신청 건이 아니라 이 마스터에서 조회한다 (CF-BC-13 "회사명·로고는 상품/제휴사 마스터에서 조회").
 * 제휴사명·로고가 바뀌어도 진행 중인 신청 데이터를 고칠 필요가 없다.
 *
 * @param code    회사코드 (예: OWN, PARTNER_A)
 * @param name    회사명 (카드에 표시)
 * @param logoUrl 로고 이미지 URL. 없으면 화면이 대체 로고(당행: 은행 아이콘 / 제휴사: 회사명 첫 글자)를 그린다
 * @param own     당행 여부 (true = 당행 상품, false = 대외 연계 상품)
 */
public record Partner(
        String code,
        String name,
        String logoUrl,
        boolean own
) implements Serializable {
}
