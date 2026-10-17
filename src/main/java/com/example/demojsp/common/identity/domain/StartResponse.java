package com.example.demojsp.common.identity.domain;

import java.util.Map;

/**
 * 본인인증 시작 응답 (공통 API → 화면).
 *
 * <p>화면(JS)은 이 응답만 보고 &lt;form&gt; 을 만들어 인증기관 URL 로 submit 하면 된다.
 * 즉 화면은 인증기관별 파라미터 이름을 몰라도 된다. (공통 JS: /js/common/identity-auth.js)
 *
 * @param txId       공통모듈 내부 거래번호 (로그 추적용, 화면은 보관만)
 * @param actionUrl  form 을 submit 할 인증기관 URL
 * @param httpMethod form method (GET/POST)
 * @param formParams form hidden 필드 (name → value)
 * @param popup      팝업으로 열어야 하는지 여부 (채널에 따라 결정)
 */
public record StartResponse(
        String txId,
        String actionUrl,
        String httpMethod,
        Map<String, String> formParams,
        boolean popup
) {
}
