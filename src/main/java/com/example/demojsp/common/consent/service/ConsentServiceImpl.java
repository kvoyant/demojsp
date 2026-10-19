package com.example.demojsp.common.consent.service;

import com.example.demojsp.common.consent.config.ConsentProperties;
import com.example.demojsp.common.consent.dto.ConsentAgreeDto;
import com.example.demojsp.common.consent.dto.ConsentAgreeItemDto;
import com.example.demojsp.common.consent.dto.ConsentAgreeRequest;
import com.example.demojsp.common.consent.dto.ConsentBizDto;
import com.example.demojsp.common.consent.dto.ConsentListDto;
import com.example.demojsp.common.consent.dto.ConsentResult;
import com.example.demojsp.common.consent.dto.ConsentTermsDto;
import com.example.demojsp.common.consent.exception.ConsentErrorCode;
import com.example.demojsp.common.consent.exception.ConsentException;
import com.example.demojsp.common.consent.mapper.ConsentMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ConsentServiceImpl implements ConsentService {

    /** 감사 로그 (운영: 별도 파일 appender 권장) */
    private static final Logger audit = LoggerFactory.getLogger("AUDIT.CONSENT");
    private static final Set<String> CHANNELS = Set.of("APP", "PC_WEB", "AGENT_WEB");

    private final ConsentMapper mapper;
    private final ConsentProperties properties;
    private Clock clock = Clock.systemDefaultZone();

    public ConsentServiceImpl(ConsentMapper mapper, ConsentProperties properties) {
        this.mapper = mapper;
        this.properties = properties;
    }

    /** 테스트용 시계 교체 */
    void setClock(Clock clock) {
        this.clock = clock;
    }

    // ================================================================ 조회

    @Override
    public ConsentListDto getTerms(String bizCd) {
        return getTerms(bizCd, LocalDate.now(clock));
    }

    @Override
    public ConsentListDto getTerms(String bizCd, LocalDate baseDt) {
        ConsentBizDto biz = mapper.selectBiz(bizCd);
        if (biz == null || !"Y".equals(biz.getUseYn())) {
            throw new ConsentException(ConsentErrorCode.BIZ_NOT_FOUND);
        }
        List<ConsentTermsDto> terms = mapper.selectBizTerms(bizCd, baseDt);
        if (terms.isEmpty()) {
            throw new ConsentException(ConsentErrorCode.TERMS_EMPTY);
        }
        ConsentListDto result = new ConsentListDto();
        result.setBizCd(biz.getBizCd());
        result.setBizNm(biz.getBizNm());
        result.setBaseDt(baseDt);
        for (ConsentTermsDto t : terms) {
            if ("Y".equals(t.getReqYn())) {
                result.getRequired().add(t);
            } else {
                result.getOptional().add(t);
            }
        }
        return result;
    }

    @Override
    public ConsentTermsDto getTermsDetail(String termsCd, int termsVer) {
        ConsentTermsDto t = mapper.selectTermsVer(termsCd, termsVer);
        if (t == null || !"Y".equals(t.getUseYn())) {
            throw new ConsentException(ConsentErrorCode.TERMS_NOT_FOUND);
        }
        return t;
    }

    // ================================================================ 동의

    @Override
    @Transactional
    public ConsentResult agree(String bizCd, ConsentAgreeRequest request, ConsentUser user) {
        if (request == null || request.getItems() == null || !CHANNELS.contains(request.getChannel())) {
            throw new ConsentException(ConsentErrorCode.INVALID_REQUEST);
        }
        // 1) 화면이 보낸 값은 믿지 않고, DB 의 현재 약관 목록으로 다시 검사한다
        ConsentListDto current = getTerms(bizCd);
        Map<String, ConsentAgreeItemDto> sent = new LinkedHashMap<>();
        for (ConsentAgreeItemDto item : request.getItems()) {
            if (item == null || item.getTermsCd() == null || item.getTermsVer() == null
                    || sent.put(item.getTermsCd(), item) != null) {
                throw new ConsentException(ConsentErrorCode.INVALID_REQUEST);    // 코드·버전 누락, 같은 약관 두 번
            }
        }
        List<ConsentTermsDto> all = new ArrayList<>(current.getRequired());
        all.addAll(current.getOptional());
        for (String code : sent.keySet()) {
            if (all.stream().noneMatch(t -> t.getTermsCd().equals(code))) {
                throw new ConsentException(ConsentErrorCode.TERMS_NOT_IN_BIZ, ConsentErrorCode.TERMS_NOT_IN_BIZ.getMessage() + " (" + code + ")");
            }
        }

        // 2) 약관별 동의 결과 만들기 (보내지 않은 선택약관 = 미동의)
        List<ConsentAgreeItemDto> rows = new ArrayList<>();
        for (ConsentTermsDto t : all) {
            ConsentAgreeItemDto in = sent.get(t.getTermsCd());
            boolean agreed = in != null && "Y".equals(in.getAgreeYn());
            if (in != null && !in.getTermsVer().equals(t.getTermsVer())) {
                throw new ConsentException(ConsentErrorCode.TERMS_CHANGED);      // 화면을 연 사이 개정본 시행
            }
            if ("Y".equals(t.getReqYn()) && !agreed) {
                throw new ConsentException(ConsentErrorCode.REQUIRED_NOT_AGREED);
            }
            ConsentAgreeItemDto row = new ConsentAgreeItemDto(t.getTermsCd(), t.getTermsVer(), agreed ? "Y" : "N");
            row.setReqYn(t.getReqYn());
            row.setViewYn(in != null && "Y".equals(in.getViewYn()) ? "Y" : "N");
            row.setCnHash(contentHash(t));
            rows.add(row);
        }

        // 3) 저장 (헤더 → 상세). INSERT ONLY - 수정·삭제하지 않는다.
        ConsentAgreeDto head = new ConsentAgreeDto();
        head.setBizCd(bizCd);
        head.setCustId(user.custId());
        head.setSessKey(user.sessKey());
        head.setChannel(request.getChannel());
        head.setClientIp(user.clientIp());
        head.setAgreeAt(LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS));
        mapper.insertAgree(head);
        for (ConsentAgreeItemDto row : rows) {
            row.setAgreeNo(head.getAgreeNo());
            mapper.insertAgreeDtl(row);
        }
        ConsentResult result = toResult(head, rows);
        audit.info("agreeNo={} bizCd={} custId={} channel={} agreed={} declined={}", head.getAgreeNo(), bizCd,
                user.custId(), request.getChannel(), result.getAgreedTerms(), result.getDeclinedTerms());
        return result;
    }

    @Override
    public ConsentResult checkAgreed(long agreeNo, String bizCd, HttpServletRequest request) {
        return checkAgreed(agreeNo, bizCd, ConsentUser.from(request));
    }

    /** 업무 서버 확인: 같은 업무 · 같은 사람(회원ID 또는 세션) · 필수 전부 Y · 유효시간 이내 */
    @Override
    public ConsentResult checkAgreed(long agreeNo, String bizCd, ConsentUser user) {
        ConsentAgreeDto head = mapper.selectAgree(agreeNo);
        boolean mine = head != null && (head.getCustId() != null
                ? head.getCustId().equals(user.custId())
                : head.getSessKey().equals(user.sessKey()));
        if (!mine || !head.getBizCd().equals(bizCd)) {
            throw new ConsentException(ConsentErrorCode.AGREE_NOT_FOUND);
        }
        List<ConsentAgreeItemDto> rows = mapper.selectAgreeDtl(agreeNo);
        if (rows.stream().anyMatch(r -> "Y".equals(r.getReqYn()) && !"Y".equals(r.getAgreeYn()))) {
            throw new ConsentException(ConsentErrorCode.AGREE_NOT_FOUND);
        }
        int valid = properties.getValidMinutes();
        if (valid > 0 && head.getAgreeAt().plusMinutes(valid).isBefore(LocalDateTime.now(clock))) {
            throw new ConsentException(ConsentErrorCode.AGREE_EXPIRED);
        }
        return toResult(head, rows);
    }

    // ================================================================ 내부

    /** 저장된 해시가 없으면(DBA 직접 등록 등) 전문으로 계산해서 채운다 */
    private String contentHash(ConsentTermsDto t) {
        if (t.getCnHash() != null && !t.getCnHash().isBlank()) {
            return t.getCnHash();
        }
        ConsentTermsDto full = mapper.selectTermsVer(t.getTermsCd(), t.getTermsVer());
        String hash = sha256(normalize(full.getTermsCn()));
        mapper.updateTermsHash(t.getTermsCd(), t.getTermsVer(), hash);
        return hash;
    }

    private static ConsentResult toResult(ConsentAgreeDto head, List<ConsentAgreeItemDto> rows) {
        List<String> agreed = new ArrayList<>();
        List<String> declined = new ArrayList<>();
        for (ConsentAgreeItemDto r : rows) {
            ("Y".equals(r.getAgreeYn()) ? agreed : declined).add(r.getTermsCd());
        }
        return new ConsentResult(head.getAgreeNo(), head.getBizCd(), head.getAgreeAt(), agreed, declined);
    }

    /** 줄바꿈(CRLF/LF)·앞뒤 공백 차이로 해시가 달라지지 않게 정리 */
    static String normalize(String content) {
        return content == null ? "" : content.replace("\r\n", "\n").replace('\r', '\n').strip();
    }

    static String sha256(String value) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** ADMIN 등록 시에도 같은 규칙으로 해시 */
    public static String contentHashOf(String content) {
        return sha256(normalize(content));
    }
}
