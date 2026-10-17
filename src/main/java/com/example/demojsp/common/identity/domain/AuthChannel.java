package com.example.demojsp.common.identity.domain;

/**
 * 본인인증을 호출한 채널.
 *
 * <p>요구사항 정의서 기준 적용 채널은 APP / PC WEB / 모집인WEB 이다. (ADMIN, Batch 제외)
 * 채널에 따라 인증창을 띄우는 방식과 인증 완료 후 결과를 돌려주는 방식이 달라진다.
 *
 * <pre>
 *  채널        | 인증창          | 완료 후 결과 전달
 *  -----------+-----------------+-------------------------------------------
 *  PC_WEB     | 팝업(window)     | window.opener.postMessage → 팝업 close
 *  RECRUIT_WEB| 팝업(window)     | window.opener.postMessage → 팝업 close
 *  APP        | WebView 내 페이지 전환 | returnUrl 로 redirect (또는 앱 브릿지 호출)
 * </pre>
 */
public enum AuthChannel {

    APP(false),
    PC_WEB(true),
    /** 모집인 WEB */
    RECRUIT_WEB(true);

    /** 인증창을 팝업으로 띄우는 채널인지 여부 */
    private final boolean popup;

    AuthChannel(boolean popup) {
        this.popup = popup;
    }

    public boolean isPopup() {
        return popup;
    }
}
