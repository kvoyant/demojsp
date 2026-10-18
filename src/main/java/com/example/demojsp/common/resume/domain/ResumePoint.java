package com.example.demojsp.common.resume.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Comparator;

/**
 * 이어하기 1건 (신청 업무가 "멈춘 지점"). 각 신청 업무(대출신청·계좌개설·서류제출·전자서명 등)가 단계를 저장할 때 등록/갱신한다.
 *
 * @param id             이어하기 ID
 * @param customerId     고객 ID (화면 응답에는 내보내지 않음)
 * @param productType    수신/여신
 * @param bizCode        업무코드 (LOAN_APPLY, DEPOSIT_OPEN ...)
 * @param bizName        업무명 (예: 대출신청, 계좌개설)
 * @param productCode    상품코드
 * @param productName    상품명 (기존 이어하기 상품명 준용)
 * @param partnerCode    회사코드 → {@link Partner} 마스터
 * @param stepName       멈춘 단계명 (예: 서류제출)
 * @param stepNo         멈춘 단계 순번 (1부터)
 * @param totalSteps     전체 단계 수 (0 = 표시 안 함)
 * @param resumeUrl      해당 상품 신청 링크 (멈춘 단계로 바로 진입)
 * @param status         상태
 * @param startedAt      최초 신청일시
 * @param lastActivityAt 마지막 진행일시 ("최신 1건" 판단 기준)
 * @param expiresAt      이어하기 만료일시 (상품별 기존 이어하기 정책. null = 만료 없음)
 */
public record ResumePoint(
        long id,
        String customerId,
        ProductType productType,
        String bizCode,
        String bizName,
        String productCode,
        String productName,
        String partnerCode,
        String stepName,
        int stepNo,
        int totalSteps,
        String resumeUrl,
        ResumeStatus status,
        LocalDateTime startedAt,
        LocalDateTime lastActivityAt,
        LocalDateTime expiresAt
) implements Serializable {

    /**
     * "최신" 정렬 규칙 (공통#43 기능1 "이어하기 전체 건 중 최신 1건만 노출 - 현행 정책 준용").
     * <ol>
     *   <li>마지막 진행일시 내림차순 (가장 최근에 손댄 신청)</li>
     *   <li>같으면 최초 신청일시 내림차순</li>
     *   <li>그래도 같으면 ID 내림차순 (항상 같은 결과)</li>
     * </ol>
     */
    public static final Comparator<ResumePoint> LATEST_FIRST = Comparator
            .comparing(ResumePoint::lastActivityAt, Comparator.reverseOrder())
            .thenComparing(ResumePoint::startedAt, Comparator.reverseOrder())
            .thenComparing(ResumePoint::id, Comparator.reverseOrder());

    /** 기준시각에 이어하기 가능한지 (진행중 + 만료 전) */
    public boolean resumableAt(LocalDateTime at) {
        return status == ResumeStatus.IN_PROGRESS && (expiresAt == null || at.isBefore(expiresAt));
    }

    public ResumePoint withStatus(ResumeStatus newStatus, LocalDateTime at) {
        return new ResumePoint(id, customerId, productType, bizCode, bizName, productCode, productName, partnerCode,
                stepName, stepNo, totalSteps, resumeUrl, newStatus, startedAt, at, expiresAt);
    }
}
