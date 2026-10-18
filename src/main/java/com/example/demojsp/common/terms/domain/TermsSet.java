package com.example.demojsp.common.terms.domain;

import java.util.List;

/**
 * 업무코드 단위 약관 묶음 - 필수/선택을 분리해서 내려준다 (REQ-AP-602 "필수 항목, 선택 항목 페이지 분리").
 *
 * @param bizCode  업무코드
 * @param bizName  업무명
 * @param required 필수 약관 (1단계 페이지)
 * @param optional 선택 약관 (2단계 페이지, 비어 있으면 2단계 생략)
 */
public record TermsSet(
        String bizCode,
        String bizName,
        List<TermsSummary> required,
        List<TermsSummary> optional
) {
}
