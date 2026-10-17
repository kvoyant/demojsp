package com.example.demojsp.common.identity.domain;

import java.util.Map;

/**
 * Provider 가 인증 시작 준비를 마치고 공통 서비스에 돌려주는 값.
 *
 * @param correlationKey 콜백에서 거래를 찾을 키 (NICE: token_version_id)
 * @param actionUrl      인증창 URL
 * @param httpMethod     form method
 * @param formParams     form 파라미터
 * @param providerState  콜백 처리 때 필요한 비밀값 (서버에만 보관)
 */
public record ProviderPrepareResult(
        String correlationKey,
        String actionUrl,
        String httpMethod,
        Map<String, String> formParams,
        Map<String, String> providerState
) {
}
