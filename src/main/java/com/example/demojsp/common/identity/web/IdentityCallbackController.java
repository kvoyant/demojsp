package com.example.demojsp.common.identity.web;

import com.example.demojsp.common.identity.domain.AuthMethod;
import com.example.demojsp.common.identity.domain.IdentityTx;
import com.example.demojsp.common.identity.exception.IdentityErrorCode;
import com.example.demojsp.common.identity.exception.IdentityVerificationException;
import com.example.demojsp.common.identity.service.IdentityVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Locale;
import java.util.Map;

/**
 * 인증기관 콜백 수신 컨트롤러 (화면 응답).
 *
 * <pre>
 *  GET|POST /common/identity/callback/{method}   예) /common/identity/callback/nice_mobile
 * </pre>
 *
 * <p>인증기관(NICE) 인증창이 브라우저를 이 URL 로 돌려보낸다. 성공/실패 모두 callback.jsp 로 렌더링하며,
 * JSP 는 팝업이면 부모창에 결과를 알리고 닫고, APP 이면 returnUrl 로 이동시킨다.
 *
 * <p><b>인터셉터</b>: 이 경로는 WebMvcConfig 의 LoginCheckInterceptor 대상 경로 목록에 없으므로
 * 비로그인(회원가입 전) 사용자도 접근 가능하다. RoleCheckInterceptor 는 @RequireRole 이 없으면 통과.
 * <p>※ Spring Security 도입 시 이 경로는 permitAll + CSRF 예외 처리 필요 (외부 도메인에서 호출됨).
 */
@Slf4j
@Controller
@RequestMapping("/common/identity")
@RequiredArgsConstructor
public class IdentityCallbackController {

    /** 결과 전달 JSP (WEB-INF/views/common/identity/callback.jsp) */
    static final String CALLBACK_VIEW = "common/identity/callback";

    private final IdentityVerificationService identityService;

    /**
     * 인증기관 콜백.
     * <p>NICE methodtype 이 get/post 어느 쪽이든 받을 수 있도록 두 메서드 모두 허용.
     * <p>예외를 밖으로 던지지 않는다 (GlobalExceptionHandler 의 JSON 응답이 팝업에 찍히는 것을 방지).
     */
    @RequestMapping(value = "/callback/{method}", method = {RequestMethod.GET, RequestMethod.POST})
    public String callback(@PathVariable("method") String methodPath,
                           @RequestParam Map<String, String> params,
                           HttpServletRequest httpRequest,
                           Model model) {
        try {
            AuthMethod method = AuthMethod.valueOf(methodPath.toUpperCase(Locale.ROOT));
            IdentityVerificationService.Completion done = identityService.complete(method, params, httpRequest);
            fillModel(model, done.tx(), true, null);
        } catch (IdentityVerificationService.TxAwareException e) {
            // 거래는 찾았지만 검증 실패 → 채널 정보로 화면 분기 가능
            fillModel(model, e.getTx(), false, e.getErrorCode());
        } catch (IdentityVerificationException e) {
            // 거래를 못 찾은 경우 등 - 채널을 알 수 없으므로 팝업 기본으로 처리
            log.warn("[공통본인인증] 콜백 처리 실패: {}", e.getMessage());
            fillModel(model, null, false, e.getErrorCode());
        } catch (IllegalArgumentException e) {
            // 정의되지 않은 인증수단 경로
            fillModel(model, null, false, IdentityErrorCode.UNSUPPORTED_METHOD);
        }
        return CALLBACK_VIEW;
    }

    /** callback.jsp 에서 사용하는 모델. JSP 는 이 값만 보고 분기한다. (CI 등 개인정보는 절대 넣지 않음) */
    private void fillModel(Model model, IdentityTx tx, boolean success, IdentityErrorCode error) {
        model.addAttribute("success", success);
        model.addAttribute("popup", tx == null || tx.channel().isPopup());
        model.addAttribute("purpose", tx == null ? "" : tx.purpose().name());
        model.addAttribute("returnUrl", tx == null ? null : tx.returnUrl());
        model.addAttribute("errorCode", error == null ? "" : error.getCode());
        model.addAttribute("errorMessage", error == null ? "" : error.getMessage());
    }
}
