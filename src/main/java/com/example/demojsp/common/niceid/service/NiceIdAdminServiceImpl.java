package com.example.demojsp.common.niceid.service;

import com.example.demojsp.common.consent.mapper.ConsentMapper;
import com.example.demojsp.common.niceid.dto.IdvPurposeDto;
import com.example.demojsp.common.niceid.dto.IdvReqDto;
import com.example.demojsp.common.niceid.exception.NiceIdErrorCode;
import com.example.demojsp.common.niceid.exception.NiceIdException;
import com.example.demojsp.common.niceid.mapper.NiceIdMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class NiceIdAdminServiceImpl implements NiceIdAdminService {

    private static final Pattern CODE = Pattern.compile("^[A-Z][A-Z0-9_]{1,29}$");

    private final NiceIdMapper mapper;
    private final ConsentMapper consentMapper;      // 약관 업무코드 확인 (공통#22)
    private Clock clock = Clock.systemDefaultZone();

    public NiceIdAdminServiceImpl(NiceIdMapper mapper, ConsentMapper consentMapper) {
        this.mapper = mapper;
        this.consentMapper = consentMapper;
    }

    void setClock(Clock clock) {
        this.clock = clock;
    }

    @Override
    public List<IdvPurposeDto> purposes() {
        return mapper.selectPurposes();
    }

    @Override
    @Transactional
    public IdvPurposeDto savePurpose(IdvPurposeDto p, String adminId) {
        if (p == null) throw new NiceIdException(NiceIdErrorCode.INVALID_REQUEST);
        if (p.getPurposeCd() == null || !CODE.matcher(p.getPurposeCd()).matches()) {
            throw invalid("업무코드는 영문 대문자로 시작하는 대문자·숫자·_ 2~30자로 입력하세요.");
        }
        String nm = p.getPurposeNm() == null ? "" : p.getPurposeNm().strip();
        if (nm.isEmpty() || nm.length() > 50) throw invalid("업무명을 50자 이내로 입력하세요.");
        p.setPurposeNm(nm);
        String consent = p.getConsentBizCd() == null || p.getConsentBizCd().isBlank() ? null : p.getConsentBizCd().strip();
        if (consent != null && consentMapper.selectBiz(consent) == null) {
            throw invalid("약관동의 업무코드가 없습니다. 약관관리(공통#22)에 먼저 등록하세요. (" + consent + ")");
        }
        p.setConsentBizCd(consent);
        p.setSmsYn(yn(p.getSmsYn(), "SMS 인증"));
        p.setMoYn(yn(p.getMoYn(), "MO 인증"));
        if ("N".equals(p.getSmsYn()) && "N".equals(p.getMoYn())) throw invalid("SMS 와 MO 중 하나 이상은 사용해야 합니다.");
        p.setForeignerYn(yn(p.getForeignerYn(), "외국인 허용"));
        p.setWatchCheckYn(yn(p.getWatchCheckYn(), "유의고객 검사"));
        p.setUseYn(yn(p.getUseYn() == null ? "Y" : p.getUseYn(), "사용여부"));
        range(p.getMinAge(), 0, 120, "최소 나이");
        range(p.getCodeValidSec(), 60, 600, "유효시간(초)");
        range(p.getMaxTry(), 1, 10, "입력 횟수");
        range(p.getResultValidMin(), 1, 1440, "결과 사용시간(분)");
        p.setUpdAt(LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS));
        p.setUpdBy(adminId);
        mapper.mergePurpose(p);
        return mapper.selectPurpose(p.getPurposeCd());
    }

    @Override
    public List<IdvReqDto> requests(String purposeCd, String status, int limit) {
        List<IdvReqDto> list = mapper.selectReqList(blank(purposeCd), blank(status), Math.max(1, Math.min(limit, 500)));
        for (IdvReqDto r : list) {
            // 화면 노출 최소화: CI 앞 10자만, 휴대폰은 마스킹, 세션키 제외
            r.setCiVal(r.getCiVal() == null ? null : r.getCiVal().substring(0, 10) + "…");
            r.setDiVal(null);
            r.setCellNo(NiceIdServiceImpl.mask(r.getCellNo()));
            r.setSessKey(null);
            r.setProviderTx(null);
        }
        return list;
    }

    private static String blank(String v) {
        return v == null || v.isBlank() ? null : v.strip();
    }

    private static String yn(String v, String label) {
        if (!"Y".equals(v) && !"N".equals(v)) throw invalid(label + "는 Y 또는 N 입니다.");
        return v;
    }

    private static void range(Integer v, int min, int max, String label) {
        if (v == null || v < min || v > max) throw invalid(label + "은(는) " + min + "~" + max + " 사이로 입력하세요.");
    }

    private static NiceIdException invalid(String message) {
        return new NiceIdException(NiceIdErrorCode.INVALID_INPUT, message);
    }
}
