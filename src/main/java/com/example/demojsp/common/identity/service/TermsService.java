package com.example.demojsp.common.identity.service;

import com.example.demojsp.common.identity.config.IdentityProperties;
import com.example.demojsp.common.identity.domain.AuthMethod;
import com.example.demojsp.common.identity.domain.TermsDocument;
import com.example.demojsp.common.identity.exception.IdentityErrorCode;
import com.example.demojsp.common.identity.exception.IdentityVerificationException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 인증수단별 약관 서비스 ("인증별 약관 상세보기" 요구사항).
 *
 * <p>약관 목록은 application.yml 의 {@code common.identity.terms} 에서 읽는다.
 * 약관이 DB(약관관리 메뉴)로 관리된다면 이 클래스만 DB 조회로 바꾸면 된다.
 *
 * <p>REQ-AP-601 반영:
 * <ul>
 *   <li>AS-IS: 통신사 선택 필수 → 통신사별 약관 노출</li>
 *   <li>TO-BE: 통신사 선택 불필요 → 통신사 무관 공통 약관 + <b>비여신금융거래 약관</b> 추가</li>
 * </ul>
 */
@Service
public class TermsService {

    private final IdentityProperties properties;

    public TermsService(IdentityProperties properties) {
        this.properties = properties;
    }

    /** 인증수단의 약관 목록 */
    public List<TermsDocument> getTerms(AuthMethod method) {
        return properties.getTerms().getOrDefault(method, List.of());
    }

    /**
     * 필수 약관 동의 여부 서버 재검증.
     * <p>화면에서 체크박스로 막더라도, 서버에서 다시 확인해야 우회 호출을 막을 수 있다.
     */
    public void validateRequiredAgreed(AuthMethod method, Collection<String> agreedCodes) {
        List<String> missing = getTerms(method).stream()
                .filter(TermsDocument::required)
                .map(TermsDocument::code)
                .filter(code -> !agreedCodes.contains(code))
                .toList();
        if (!missing.isEmpty()) {
            throw new IdentityVerificationException(IdentityErrorCode.REQUIRED_TERMS_NOT_AGREED, "missing=" + missing);
        }
    }
}
