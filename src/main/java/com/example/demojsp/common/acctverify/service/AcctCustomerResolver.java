package com.example.demojsp.common.acctverify.service;

import com.example.demojsp.common.acctverify.config.AcctVerifyProperties;
import com.example.demojsp.common.acctverify.exception.AcctVerifyErrorCode;
import com.example.demojsp.common.acctverify.exception.AcctVerifyException;
import com.example.demojsp.common.consent.service.ConsentUser;
import com.example.demojsp.common.niceid.dto.IdvResult;
import com.example.demojsp.common.niceid.exception.NiceIdException;
import com.example.demojsp.common.niceid.service.NiceIdService;
import com.example.demojsp.domain.login.dto.UserSessionDto;
import com.example.demojsp.domain.login.service.LoginServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

/**
 * "누가 1원 인증을 하는가" 를 정한다. 예금주(본인 명의) 비교에 쓸 실명이 필요하므로 아래 순서로 찾는다.
 * <ol>
 *   <li>로그인 회원 → 회원 ID · 회원명 (대출계좌 자동완성 가능)</li>
 *   <li>공통#21 휴대폰 본인인증 결과 (idvReqNo + idvPurposeCd) → 인증된 성명 (비로그인 업무: 인증수단 등록 등)</li>
 *   <li>local 전용 (demo-api-open) → ?demoCustomer=&amp;demoName= (샘플 화면 고객 전환)</li>
 * </ol>
 * 셋 다 없으면 AVF-101. 화면이 보낸 이름은 절대 쓰지 않는다 (남의 계좌로 본인 인증하는 것을 막기 위해).
 */
@Component
public class AcctCustomerResolver {

    private final NiceIdService niceIdService;
    private final AcctVerifyProperties properties;

    public AcctCustomerResolver(NiceIdService niceIdService, AcctVerifyProperties properties) {
        this.niceIdService = niceIdService;
        this.properties = properties;
    }

    public AcctCustomer resolve(HttpServletRequest request, String idvReqNo, String idvPurposeCd) {
        ConsentUser u = ConsentUser.from(request);
        HttpSession session = request.getSession(false);
        Object v = session == null ? null : session.getAttribute(LoginServiceImpl.SESSION_USER_KEY);
        if (v instanceof UserSessionDto user && user.getMemberNm() != null) {
            return new AcctCustomer(user.getMemberId(), user.getMemberNm(), u.sessKey(), u.clientIp());
        }
        if (idvReqNo != null && !idvReqNo.isBlank() && idvPurposeCd != null) {
            try {
                IdvResult r = niceIdService.checkVerified(idvReqNo, idvPurposeCd, u);
                return new AcctCustomer(null, r.getName(), u.sessKey(), u.clientIp());
            } catch (NiceIdException e) {
                throw new AcctVerifyException(AcctVerifyErrorCode.CUSTOMER_REQUIRED, "본인인증 결과를 확인할 수 없습니다. (" + e.getMessage() + ")");
            }
        }
        String demoName = request.getParameter("demoName");
        if (properties.isDemoApiOpen() && demoName != null && !demoName.isBlank()) {
            String demoId = request.getParameter("demoCustomer");
            return new AcctCustomer(demoId == null || demoId.isBlank() ? null : demoId.strip(), demoName.strip(), u.sessKey(), u.clientIp());
        }
        throw new AcctVerifyException(AcctVerifyErrorCode.CUSTOMER_REQUIRED);
    }
}
