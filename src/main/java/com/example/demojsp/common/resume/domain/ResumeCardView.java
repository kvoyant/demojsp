package com.example.demojsp.common.resume.domain;

/**
 * 이어하기 카드 1건 (고객 화면용). 고객 ID·내부 URL 은 내보내지 않는다 → [이어하기] 는 {@code POST /api/common/resume/{id}/continue}
 * 로 서버가 유효성(본인·진행중·만료 전)을 다시 확인한 뒤 이동 URL 을 준다.
 *
 * <p>공통#43 기능5 "팝업 구조: 로고이미지 + 회사명 + 상품명 + 이어하기 버튼".
 *
 * @param id              이어하기 ID
 * @param productType     DEPOSIT / LOAN
 * @param productTypeName 수신 / 여신
 * @param bizName         업무명 (대출신청, 계좌개설 ...)
 * @param companyName     회사명 (회사 마스터)
 * @param companyLogoUrl  로고 URL (없으면 null → 화면 대체 로고)
 * @param own             당행 상품 여부 (false = 대외 연계 상품)
 * @param productName     상품명
 * @param stepName        멈춘 단계명
 * @param stepNo          멈춘 단계 순번
 * @param totalSteps      전체 단계 수 (0 = 표시 안 함)
 * @param lastActivityAt  마지막 진행일시 (yyyy-MM-dd HH:mm)
 * @param expiresAt       만료일시 (yyyy-MM-dd HH:mm, 없으면 null)
 * @param daysLeft        만료까지 남은 일수 (오늘 만료 = 0, 만료 없음 = null)
 */
public record ResumeCardView(
        long id,
        String productType,
        String productTypeName,
        String bizName,
        String companyName,
        String companyLogoUrl,
        boolean own,
        String productName,
        String stepName,
        int stepNo,
        int totalSteps,
        String lastActivityAt,
        String expiresAt,
        Long daysLeft
) {
}
