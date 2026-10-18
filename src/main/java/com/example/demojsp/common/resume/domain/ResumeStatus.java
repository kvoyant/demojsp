package com.example.demojsp.common.resume.domain;

/**
 * 이어하기 상태. 고객 화면에는 {@link #IN_PROGRESS} 이면서 만료 전인 건만 노출한다.
 */
public enum ResumeStatus {

    IN_PROGRESS("진행중"),
    COMPLETED("완료"),
    CANCELED("취소");

    private final String label;

    ResumeStatus(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
