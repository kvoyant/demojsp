package com.example.demojsp.common.consent.service;

import com.example.demojsp.common.consent.dto.ConsentAgreeDto;
import com.example.demojsp.common.consent.dto.ConsentBizDto;
import com.example.demojsp.common.consent.dto.ConsentBizTermsRequest;
import com.example.demojsp.common.consent.dto.ConsentTermsDto;
import com.example.demojsp.common.consent.dto.ConsentTermsSaveRequest;
import com.example.demojsp.common.consent.exception.ConsentErrorCode;
import com.example.demojsp.common.consent.exception.ConsentException;
import com.example.demojsp.common.consent.mapper.ConsentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class ConsentAdminServiceImpl implements ConsentAdminService {

    /** 업무코드·약관코드: 영문 대문자로 시작, 대문자·숫자·밑줄 2~30자 */
    private static final Pattern CODE = Pattern.compile("^[A-Z][A-Z0-9_]{1,29}$");
    private static final int MAX_CONTENT = 200_000;

    private final ConsentMapper mapper;
    private Clock clock = Clock.systemDefaultZone();

    public ConsentAdminServiceImpl(ConsentMapper mapper) {
        this.mapper = mapper;
    }

    void setClock(Clock clock) {
        this.clock = clock;
    }

    // ================================================================ 업무코드

    @Override
    public List<ConsentBizDto> bizList() {
        return mapper.selectBizList();
    }

    @Override
    @Transactional
    public ConsentBizDto saveBiz(ConsentBizDto biz, String adminId) {
        if (biz == null) throw new ConsentException(ConsentErrorCode.INVALID_REQUEST);
        code(biz.getBizCd(), "업무코드");
        biz.setBizNm(text(biz.getBizNm(), "업무명", 50, true));
        biz.setBizDesc(text(biz.getBizDesc(), "설명", 200, false));
        biz.setUseYn(yn(biz.getUseYn() == null ? "Y" : biz.getUseYn(), "사용여부"));
        biz.setUpdAt(now());
        biz.setUpdBy(adminId);
        mapper.mergeBiz(biz);
        return mapper.selectBiz(biz.getBizCd());
    }

    // ================================================================ 업무별 약관

    @Override
    public List<ConsentTermsDto> bizTerms(String bizCd) {
        requireBiz(bizCd);
        return mapper.selectBizTermsAll(bizCd, LocalDate.now(clock));
    }

    @Override
    @Transactional
    public List<ConsentTermsDto> saveBizTerms(String bizCd, List<ConsentBizTermsRequest> rows, String adminId) {
        requireBiz(bizCd);
        if (rows == null) throw new ConsentException(ConsentErrorCode.INVALID_REQUEST);
        Set<String> seen = new HashSet<>();
        for (ConsentBizTermsRequest r : rows) {
            if (r == null || !seen.add(String.valueOf(r.getTermsCd()))) {
                throw new ConsentException(ConsentErrorCode.INVALID_VALUE, "같은 약관을 두 번 연결할 수 없습니다. (" + (r == null ? "" : r.getTermsCd()) + ")");
            }
            if (mapper.selectLastVer(r.getTermsCd()) == null) {
                throw new ConsentException(ConsentErrorCode.INVALID_VALUE, "등록되지 않은 약관코드입니다. (" + r.getTermsCd() + ")");
            }
            yn(r.getReqYn(), "필수여부");
            yn(r.getUseYn() == null ? "Y" : r.getUseYn(), "사용여부");
            if (r.getSortOrd() == null || r.getSortOrd() < 1 || r.getSortOrd() > 999) {
                throw new ConsentException(ConsentErrorCode.INVALID_VALUE, "표시순서는 1~999 사이 숫자로 입력하세요. (" + r.getTermsCd() + ")");
            }
        }
        // 지우고 다시 넣기 - 동의내역(DTL)에는 동의 당시 필수여부가 따로 남아 있으므로 영향 없음
        mapper.deleteBizTerms(bizCd);
        LocalDateTime now = now();
        for (ConsentBizTermsRequest r : rows) {
            ConsentTermsDto row = new ConsentTermsDto();
            row.setBizCd(bizCd);
            row.setTermsCd(r.getTermsCd());
            row.setReqYn(r.getReqYn());
            row.setSortOrd(r.getSortOrd());
            row.setMapUseYn(r.getUseYn() == null ? "Y" : r.getUseYn());
            row.setRegAt(now);
            row.setRegBy(adminId);
            mapper.insertBizTerms(row);
        }
        return bizTerms(bizCd);
    }

    // ================================================================ 약관

    @Override
    public List<ConsentTermsDto> termsList() {
        return mapper.selectTermsList(LocalDate.now(clock));
    }

    @Override
    public List<ConsentTermsDto> termsVersions(String termsCd) {
        List<ConsentTermsDto> list = mapper.selectTermsVersions(termsCd);
        if (list.isEmpty()) throw new ConsentException(ConsentErrorCode.TERMS_NOT_FOUND);
        return list;
    }

    @Override
    @Transactional
    public ConsentTermsDto saveTerms(ConsentTermsSaveRequest req, String adminId) {
        if (req == null) throw new ConsentException(ConsentErrorCode.INVALID_REQUEST);
        code(req.getTermsCd(), "약관코드");
        String name = text(req.getTermsNm(), "약관명", 100, true);
        String content = text(req.getTermsCn(), "약관 전문", MAX_CONTENT, true);
        String pdfUrl = text(req.getPdfUrl(), "PDF URL", 500, false);
        if (pdfUrl != null && !(pdfUrl.startsWith("https://") || (pdfUrl.startsWith("/") && !pdfUrl.startsWith("//")))) {
            throw new ConsentException(ConsentErrorCode.INVALID_VALUE, "PDF URL 은 / 로 시작하는 경로 또는 https:// 주소만 입력할 수 있습니다.");
        }
        if (req.getEffectDt() == null) {
            throw new ConsentException(ConsentErrorCode.INVALID_VALUE, "시행일을 입력하세요.");
        }
        ConsentTermsDto last = mapper.selectLastVer(req.getTermsCd());
        if (last != null && req.getEffectDt().isBefore(last.getEffectDt())) {
            throw new ConsentException(ConsentErrorCode.EFFECT_DT_INVALID,
                    String.format(ConsentErrorCode.EFFECT_DT_INVALID.getMessage(), last.getEffectDt()));
        }
        ConsentTermsDto t = new ConsentTermsDto();
        t.setTermsCd(req.getTermsCd());
        t.setTermsVer(last == null ? 1 : last.getTermsVer() + 1);
        t.setTermsNm(name);
        t.setTermsCn(content);
        t.setPdfUrl(pdfUrl);
        t.setCnHash(ConsentServiceImpl.contentHashOf(content));
        t.setEffectDt(req.getEffectDt());
        t.setRegAt(now());
        t.setRegBy(adminId);
        mapper.insertTerms(t);
        return mapper.selectTermsVer(t.getTermsCd(), t.getTermsVer());
    }

    // ================================================================ 동의내역

    @Override
    public List<ConsentAgreeDto> agreeList(String bizCd, String custId, int limit) {
        return mapper.selectAgreeList(blankToNull(bizCd), blankToNull(custId), Math.max(1, Math.min(limit, 200)));
    }

    @Override
    public ConsentAgreeDto agreeDetail(long agreeNo) {
        ConsentAgreeDto head = mapper.selectAgree(agreeNo);
        if (head == null) throw new ConsentException(ConsentErrorCode.AGREE_NOT_FOUND, "동의내역을 찾을 수 없습니다.");
        head.setSessKey(null);                        // 화면에 내보낼 필요 없음
        head.setItems(mapper.selectAgreeDtl(agreeNo));
        return head;
    }

    // ================================================================ 검증 도우미

    private void requireBiz(String bizCd) {
        if (bizCd == null || mapper.selectBiz(bizCd) == null) {
            throw new ConsentException(ConsentErrorCode.BIZ_NOT_FOUND);
        }
    }

    private static void code(String value, String label) {
        if (value == null || !CODE.matcher(value).matches()) {
            throw new ConsentException(ConsentErrorCode.INVALID_VALUE, label + "는 영문 대문자로 시작하는 대문자·숫자·_ 2~30자로 입력하세요.");
        }
    }

    private static String text(String value, String label, int max, boolean required) {
        String v = value == null ? null : value.strip();
        if (v == null || v.isEmpty()) {
            if (required) throw new ConsentException(ConsentErrorCode.INVALID_VALUE, label + "을(를) 입력하세요.");
            return null;
        }
        if (v.length() > max) {
            throw new ConsentException(ConsentErrorCode.INVALID_VALUE, label + "은(는) " + max + "자 이내로 입력하세요.");
        }
        return v;
    }

    private static String yn(String value, String label) {
        if (!"Y".equals(value) && !"N".equals(value)) {
            throw new ConsentException(ConsentErrorCode.INVALID_VALUE, label + "는 Y 또는 N 입니다.");
        }
        return value;
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.strip();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }
}
