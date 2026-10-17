package com.example.demojsp.common.identity.domain;

import java.util.List;
import java.util.Map;

/**
 * 본인인증 시작 요청 (화면 → POST /api/common/identity/start).
 *
 * <p>※ demojsp 는 spring-boot-starter-validation 을 쓰지 않으므로 @NotNull 대신
 * {@code IdentityVerificationService#validateRequest} 에서 직접 필수값을 검증한다.
 *
 * @param method      인증수단 (예: NICE_MOBILE) - 필수
 * @param channel     호출 채널 (APP / PC_WEB / RECRUIT_WEB) - 필수
 * @param purpose     인증 목적 (결과 세션 저장 시 key 로 사용) - 필수
 * @param returnUrl   [APP 채널 전용] 인증 완료 후 이동할 화면 경로.
 *                    오픈 리다이렉트 방지를 위해 반드시 화이트리스트 검증을 거친다. (팝업 채널은 무시)
 * @param agreedTerms 사용자가 동의한 약관 코드 목록 (필수 약관 동의 여부를 서버에서 재검증)
 * @param options     인증수단별 부가 옵션. 공통모듈은 내용을 해석하지 않고 Provider 로 그대로 전달.
 */
public record StartRequest(
        AuthMethod method,
        AuthChannel channel,
        AuthPurpose purpose,
        String returnUrl,
        List<String> agreedTerms,
        Map<String, String> options
) {
    public StartRequest {
        // null-safe 처리: 이후 로직에서 매번 null 체크하지 않도록 빈 컬렉션으로 치환
        agreedTerms = agreedTerms == null ? List.of() : List.copyOf(agreedTerms);
        options = options == null ? Map.of() : Map.copyOf(options);
    }
}
