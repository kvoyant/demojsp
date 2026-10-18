package com.example.demojsp.common.resume.domain;

/**
 * 신청 업무 → 이어하기 등록/갱신 요청 (업무가 단계를 저장할 때 호출).
 *
 * <p>같은 고객 · 같은 업무코드 · 같은 상품코드로 진행 중인 건이 있으면 새로 만들지 않고 단계만 갱신한다
 * (한 상품을 여러 번 눌러도 이어하기 건수가 늘지 않음).
 *
 * @param productType   DEPOSIT / LOAN (필수)
 * @param bizCode       업무코드 (필수, 영문 대문자·숫자·_)
 * @param bizName       업무명 (필수, 20자)
 * @param productCode   상품코드 (필수)
 * @param productName   상품명 (필수, 40자)
 * @param partnerCode   회사코드 (생략 시 당행)
 * @param stepName      멈춘 단계명 (필수, 20자)
 * @param stepNo        멈춘 단계 순번 (1 이상)
 * @param totalSteps    전체 단계 수 (0 = 표시 안 함)
 * @param resumeUrl     이어하기 링크 (필수, 같은 도메인 상대경로 또는 허용된 APP 딥링크)
 * @param expireDays    만료 일수 (상품별 이어하기 정책. 생략 시 설정값 default-expire-days, 0 = 만료 없음)
 */
public record ResumePointRequest(
        String productType,
        String bizCode,
        String bizName,
        String productCode,
        String productName,
        String partnerCode,
        String stepName,
        Integer stepNo,
        Integer totalSteps,
        String resumeUrl,
        Integer expireDays
) {
}
