package com.example.demojsp.common.identity.sample;

import com.example.demojsp.common.identity.domain.AuthPurpose;
import com.example.demojsp.common.identity.domain.VerifiedIdentity;
import com.example.demojsp.common.identity.service.IdentitySessionManager;
import com.example.demojsp.common.response.ApiResponse;
import com.example.demojsp.common.util.MaskingUtil;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 * [업무 화면 예시] 업무 화면이 공통 본인인증을 사용하는 방법 (샘플 - 실제 업무 적용 후 삭제 가능).
 *
 * <p>업무 개발자가 해야 할 일은 두 가지뿐이다.
 * <ol>
 *   <li>JSP 에서 공통 JS {@code IdentityAuth.open({...})} 호출</li>
 *   <li>업무 API 에서 {@code identitySessionManager.require(session, 목적)} 로 CI 꺼내 쓰기</li>
 * </ol>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class IdentitySampleController {

    private final IdentitySessionManager identitySessionManager;

    /** 샘플 화면 (WEB-INF/views/common/identity/sample.jsp) */
    @GetMapping("/common/identity/sample")
    public String page() {
        return "common/identity/sample";
    }

    /**
     * 회원가입 처리 예시.
     * <p>본인인증을 안 했거나 만료됐으면 require() 가 IDV-301 예외 → IdentityApiExceptionHandler 가 401 응답.
     */
    @PostMapping("/api/common/identity/sample/join")
    @ResponseBody
    public ApiResponse<Map<String, Object>> join(HttpSession session) {
        VerifiedIdentity idv = identitySessionManager.require(session, AuthPurpose.SIGN_UP);

        // TODO 실제 업무: 동일 CI 기가입 여부 확인 → 회원 저장(CI 는 암호화 컬럼에 저장)
        //      memberService.join(form, idv.ci(), idv.di(), idv.mobileNo());
        log.info("[공통본인인증 샘플] 가입 처리 ci={}", MaskingUtil.maskCi(idv.ci()));

        // 인증결과는 1회 사용 후 제거 (같은 인증으로 중복 가입 방지)
        identitySessionManager.clear(session, AuthPurpose.SIGN_UP);
        return ApiResponse.success("샘플 가입 처리 완료", Map.of("name", MaskingUtil.maskName(idv.name())));
    }
}
