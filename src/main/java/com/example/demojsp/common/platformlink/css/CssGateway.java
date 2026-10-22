package com.example.demojsp.common.platformlink.css;

import com.example.demojsp.common.platformlink.model.CssRequest;
import com.example.demojsp.common.platformlink.model.CssResult;

/**
 * 한도조회 엔진 (CF-BC-10 CssGateway). 공통모델로만 주고받는다.
 * <pre>
 *  LEGACY : 현행 CSS (지금 플랫폼 연계가 쓰는 엔진)  → 실연동 시 현행 전문/API 호출 구현체
 *  NEW    : 신규 전략관리시스템 CSS                → CSS 프로젝트 I/F 정의서 확정 후 실연동 구현체
 * </pre>
 * 지금은 둘 다 시뮬레이터(LegacyCssSimulator / NewCssSimulator). 실연동 구현체를 만들면 같은 engine() 값으로 교체한다.
 * CSS 오류는 {@link CssException} 으로 던진다 (거절은 오류가 아니라 approved=false 결과).
 */
public interface CssGateway {

    String LEGACY = "LEGACY";
    String NEW = "NEW";

    /** LEGACY / NEW */
    String engine();

    CssResult inquire(CssRequest request);
}
