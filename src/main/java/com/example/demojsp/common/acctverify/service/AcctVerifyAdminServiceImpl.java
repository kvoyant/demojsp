package com.example.demojsp.common.acctverify.service;

import com.example.demojsp.common.acctverify.dto.AvBizDto;
import com.example.demojsp.common.acctverify.dto.AvReqDto;
import com.example.demojsp.common.acctverify.exception.AcctVerifyErrorCode;
import com.example.demojsp.common.acctverify.exception.AcctVerifyException;
import com.example.demojsp.common.acctverify.mapper.AcctVerifyMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class AcctVerifyAdminServiceImpl implements AcctVerifyAdminService {

    private static final Pattern CODE = Pattern.compile("^[A-Z][A-Z0-9_]{1,29}$");
    /** 입금자명 접두어: 한글·영문 1~6자 (은행 입금자명 표시 길이 제한 고려) */
    private static final Pattern PREFIX = Pattern.compile("^[가-힣A-Za-z]{1,6}$");

    private final AcctVerifyMapper mapper;
    private Clock clock = Clock.systemDefaultZone();

    public AcctVerifyAdminServiceImpl(AcctVerifyMapper mapper) {
        this.mapper = mapper;
    }

    void setClock(Clock clock) {
        this.clock = clock;
    }

    @Override
    public List<AvBizDto> bizList() {
        return mapper.selectBizList();
    }

    @Override
    @Transactional
    public AvBizDto saveBiz(AvBizDto b, String adminId) {
        if (b == null) throw new AcctVerifyException(AcctVerifyErrorCode.INVALID_REQUEST);
        if (b.getBizCd() == null || !CODE.matcher(b.getBizCd()).matches()) throw invalid("업무코드는 영문 대문자로 시작하는 대문자·숫자·_ 2~30자로 입력하세요.");
        String nm = b.getBizNm() == null ? "" : b.getBizNm().strip();
        if (nm.isEmpty() || nm.length() > 50) throw invalid("업무명을 50자 이내로 입력하세요.");
        b.setBizNm(nm);
        if (!AvBizDto.LOAN.equals(b.getScreenType()) && !AvBizDto.COMMON.equals(b.getScreenType())) {
            throw invalid("화면 종류는 LOAN(대출 입출금계좌용) 또는 COMMON(타 업무 공통) 입니다.");
        }
        b.setPrefillYn(yn(b.getPrefillYn(), "자동완성"));
        if (AvBizDto.COMMON.equals(b.getScreenType()) && "Y".equals(b.getPrefillYn())) {
            throw invalid("대출계좌 자동완성은 LOAN 화면에서만 쓸 수 있습니다.");
        }
        b.setOwnerCheckYn(yn(b.getOwnerCheckYn(), "예금주 확인"));
        if (b.getMemoPrefix() == null || !PREFIX.matcher(b.getMemoPrefix()).matches()) throw invalid("입금자명 접두어는 한글·영문 1~6자입니다.");
        range(b.getCodeValidMin(), 3, 30, "유효시간(분)");
        range(b.getMaxTry(), 1, 10, "입력 횟수");
        range(b.getResultValidMin(), 1, 1440, "결과 사용시간(분)");
        b.setUseYn(yn(b.getUseYn() == null ? "Y" : b.getUseYn(), "사용여부"));
        b.setUpdAt(LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS));
        b.setUpdBy(adminId);
        mapper.mergeBiz(b);
        return mapper.selectBiz(b.getBizCd());
    }

    @Override
    public List<AvReqDto> requests(String bizCd, String status, int limit) {
        List<AvReqDto> list = mapper.selectReqList(blank(bizCd), blank(status), Math.max(1, Math.min(limit, 500)));
        for (AvReqDto r : list) {
            r.setAcctNo(null);          // 전체 계좌번호 · 인증코드 해시 · 세션키는 내보내지 않는다
            r.setCodeHash(null);
            r.setSessKey(null);
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

    private static AcctVerifyException invalid(String message) {
        return new AcctVerifyException(AcctVerifyErrorCode.INVALID_VALUE, message);
    }
}
