package com.example.demojsp.common.identity.config;

import com.example.demojsp.common.identity.domain.AuthMethod;
import com.example.demojsp.common.identity.domain.TermsDocument;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 본인인증 공통모듈 설정 (application.yaml 의 {@code common.identity.*}).
 *
 * <p><b>기존 NICE 구현 설정({@code nice.api.*})과 완전히 분리</b>했다.
 * 기존 NiceApiClientImpl 은 nice.api.* 를, 공통모듈은 common.identity.nice.* 를 읽으므로 서로 영향이 없다.
 *
 * <p>환경(local/dev/prd)별로 달라지는 값은 모두 여기로 모아 소스 수정 없이 yaml 만으로 전환한다.
 * client-secret 같은 비밀값은 yaml 에 평문으로 두지 말고 환경변수/Vault/Jasypt 로 주입한다.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "common.identity")
public class IdentityProperties {

    /** 서비스 외부 기준 URL (콜백 URL 생성용). 예: https://www.example.com - LB/프록시 뒤에 있을 때 필수 */
    private String baseUrl = "http://localhost:8080";

    /** 공통 콜백 경로. 실제 URL = baseUrl + callbackPath + "/" + method(소문자) */
    private String callbackPath = "/common/identity/callback";

    /** 인증 거래 유효시간 (인증창 열고 완료까지 허용 시간) */
    private Duration txTtl = Duration.ofMinutes(10);

    /** 세션에 저장된 인증 결과의 유효시간 (업무 처리 완료까지 허용 시간) */
    private Duration resultTtl = Duration.ofMinutes(30);

    /** APP 채널 returnUrl 허용 prefix 목록 (오픈 리다이렉트 방지). 예: "/", "myapp://" */
    private List<String> allowedReturnUrlPrefixes = new ArrayList<>(List.of("/"));

    /**
     * Mock Provider 사용 여부. true 면 NICE 실연동 대신 가짜 인증으로 즉시 완료된다.
     * <b>local/개발 PC 전용. 운영 yaml 에서는 반드시 false.</b>
     */
    private boolean mockEnabled = false;

    /** 인증수단별 약관 목록 (약관 상세보기 / 필수동의 검증용) */
    private Map<AuthMethod, List<TermsDocument>> terms = new EnumMap<>(AuthMethod.class);

    /** NICE ID 설정 */
    private final Nice nice = new Nice();

    /**
     * NICE ID(통합인증 표준창) 연동 설정.
     *
     * <p>기본값은 NICE 개발가이드(API 방식) 기준이다.
     * <b>URL/파라미터는 계약 시 수령한 최신 가이드와 반드시 대조할 것.</b>
     */
    @Getter
    @Setter
    public static class Nice {
        /** NICE 발급 Client ID */
        private String clientId;
        /** NICE 발급 Client Secret (외부 주입 필수) */
        private String clientSecret;
        /** NICE 발급 상품 ID (본인인증 상품) */
        private String productId;
        /**
         * 기관토큰(Access Token). NICE 기관토큰은 유효기간이 매우 길어 최초 1회 발급 후 보관해 쓰는 것이 일반적.
         * 값이 있으면 그대로 사용하고, 비어 있으면 최초 호출 시 발급하여 메모리에 캐시한다.
         */
        private String accessToken;
        /** NICE API 서버 */
        private String apiBaseUrl = "https://svc.niceapi.co.kr:22001";
        /** 기관토큰 발급 경로 */
        private String tokenPath = "/digital/niceid/oauth/oauth/token";
        /** 암호화 토큰 발급 경로 */
        private String cryptoTokenPath = "/digital/niceid/api/v1.0/common/crypto/token";
        /** 표준창(인증창) URL */
        private String checkplusUrl = "https://nice.checkplus.co.kr/CheckPlusSafeModel/service.cb";
        /** 인증 유형. M: 휴대폰 */
        private String authType = "M";
        /**
         * 결과 전달 방식 (get/post).
         * <p><b>기본 get 권장</b>: NICE 도메인에서 우리 콜백으로 cross-site POST 가 오면
         * 브라우저 SameSite=Lax 기본 정책 때문에 세션 쿠키(JSESSIONID)가 전송되지 않아 세션 검증이 실패한다.
         * post 를 쓰려면 세션 쿠키를 SameSite=None; Secure 로 설정해야 한다.
         */
        private String methodType = "get";
        /** API 연결/응답 타임아웃 */
        private Duration connectTimeout = Duration.ofSeconds(3);
        private Duration readTimeout = Duration.ofSeconds(5);
        /**
         * NICE 요청 JSON 에 추가할 파라미터 (그대로 merge).
         * <p>요구사항의 "통신사 미지정", "MO인증 결합 시 번호입력 생략" 은 NICE 상품/옵션 계약 사항이므로
         * NICE 가 안내한 파라미터명/값을 여기 설정해서 반영한다. (소스 수정 불필요)
         */
        private Map<String, String> extraRequestParams = new LinkedHashMap<>();
    }
}
