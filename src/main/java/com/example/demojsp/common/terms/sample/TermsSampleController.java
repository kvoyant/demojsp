package com.example.demojsp.common.terms.sample;

import com.example.demojsp.common.response.ApiResponse;
import com.example.demojsp.common.terms.domain.AgreeResult;
import com.example.demojsp.common.terms.service.TermsSessionManager;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * [업무 화면 예시] 약관동의 공통모듈 사용 방법 (샘플 - 실제 업무 적용 후 삭제 가능).
 *
 * <ol>
 *   <li>JSP: {@code TermsAgree.open({ bizCode: 'SIGN_UP', channel: 'PC_WEB' })}</li>
 *   <li>업무 API: {@code termsSessionManager.require(session, "SIGN_UP")} 로 동의 결과 확인</li>
 * </ol>
 */
@Controller
public class TermsSampleController {

    private final TermsSessionManager termsSessionManager;

    public TermsSampleController(TermsSessionManager termsSessionManager) {
        this.termsSessionManager = termsSessionManager;
    }

    /** 샘플 화면 (WEB-INF/views/common/terms/sample.jsp) */
    @GetMapping("/common/terms/sample")
    public String page() {
        return "common/terms/sample";
    }

    /**
     * 업무 처리 예시 (예: 회원가입 저장).
     * <p>약관 미동의/만료 → TRM-301(401). 동의 결과는 1회 사용 후 제거한다.
     */
    @PostMapping("/api/common/terms-sample/submit")
    @ResponseBody
    public ApiResponse<Map<String, Object>> submit(@RequestParam("bizCode") String bizCode, HttpSession session) {
        AgreeResult terms = termsSessionManager.require(session, bizCode);

        // TODO 실제 업무: 업무 데이터에 agreementId 저장 → 동의 이력(증적)과 연결
        //      member.setTermsAgreementId(terms.agreementId());
        //      member.setMarketingYn(terms.isAgreed("MARKETING_USE") ? "Y" : "N");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("bizCode", bizCode);
        data.put("agreementId", terms.agreementId());
        data.put("agreedCodes", terms.agreedCodes());
        data.put("declinedCodes", terms.declinedCodes());

        termsSessionManager.clear(session, bizCode);
        return ApiResponse.success("샘플 업무 처리 완료 (동의 결과 사용 후 세션에서 제거)", data);
    }
}
