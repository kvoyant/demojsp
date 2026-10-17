package com.example.demojsp.common.identity.domain;

/**
 * 본인인증 수단.
 *
 * <p>공통모듈은 "어떤 인증수단이냐"만 알고, 실제 연동 방식은 {@link com.example.demojsp.common.identity.spi.IdentityProvider}
 * 구현체가 담당한다. 새 인증수단(예: 토스인증, PASS 앱인증)을 추가할 때는
 * <ol>
 *   <li>여기에 enum 상수를 추가하고</li>
 *   <li>IdentityProvider 구현체를 @Component 로 등록하면</li>
 * </ol>
 * 서비스/컨트롤러/화면 수정 없이 바로 사용 가능하도록 설계했다.
 */
public enum AuthMethod {

    /** NICE ID 휴대폰 본인인증 (통신사 미지정 + MO인증 결합) - 1차 구축 대상 */
    NICE_MOBILE("휴대폰 본인인증"),

    /** 사설인증 (통신사 PASS, 카카오/네이버/토스 등 민간인증) - 확장 예정 */
    PRIVATE_CERT("사설인증"),

    /** 공동인증서 (구 공인인증서) - 확장 예정 */
    JOINT_CERT("공동인증서");

    private final String displayName;

    AuthMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
