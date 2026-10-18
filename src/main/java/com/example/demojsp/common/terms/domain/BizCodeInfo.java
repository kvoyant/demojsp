package com.example.demojsp.common.terms.domain;

/**
 * [ADMIN] 약관이 등록된 업무코드 요약.
 */
public record BizCodeInfo(String bizCode, String bizName, int requiredCount, int optionalCount) {
}
