package com.example.demojsp.common.niceid.service;

import com.example.demojsp.common.consent.exception.ConsentException;
import com.example.demojsp.common.consent.service.ConsentService;
import com.example.demojsp.common.consent.service.ConsentUser;
import com.example.demojsp.common.niceid.config.NiceIdProperties;
import com.example.demojsp.common.niceid.dto.IdvPurposeDto;
import com.example.demojsp.common.niceid.dto.IdvReqDto;
import com.example.demojsp.common.niceid.dto.IdvResult;
import com.example.demojsp.common.niceid.dto.IdvStartRequest;
import com.example.demojsp.common.niceid.dto.IdvStartResult;
import com.example.demojsp.common.niceid.exception.NiceIdErrorCode;
import com.example.demojsp.common.niceid.exception.NiceIdException;
import com.example.demojsp.common.niceid.gateway.MockNiceIdGateway;
import com.example.demojsp.common.niceid.gateway.NiceIdGateway;
import com.example.demojsp.common.niceid.gateway.NiceIdGatewayException;
import com.example.demojsp.common.niceid.gateway.NiceIdIdentity;
import com.example.demojsp.common.niceid.mapper.NiceIdMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 트랜잭션 메모: verifySms / moStatus 는 일부러 @Transactional 을 붙이지 않는다.
 * 인증번호 오입력 횟수(TRY_CNT)를 올린 뒤 예외를 던지는데, 트랜잭션이면 그 증가분까지 롤백되어 무제한 시도가 가능해진다.
 * 각 UPDATE 는 "STATUS='REQUESTED' 일 때만" 조건이 있어 동시에 눌러도 두 번 완료되지 않는다.
 */
@Service
public class NiceIdServiceImpl implements NiceIdService {

    private static final Logger audit = LoggerFactory.getLogger("AUDIT.NICEID");

    private static final Set<String> CHANNELS = Set.of("APP", "PC_WEB", "AGENT_WEB");
    private static final Pattern NAME = Pattern.compile("^[가-힣A-Za-z][가-힣A-Za-z .]{1,49}$");
    private static final Pattern PHONE = Pattern.compile("^01[016789]\\d{7,8}$");
    private static final DateTimeFormatter BIRTH = DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);

    private final NiceIdMapper mapper;
    private final NiceIdGateway gateway;
    private final ConsentService consentService;
    private final NiceIdProperties properties;
    private final SecureRandom random = new SecureRandom();
    private Clock clock = Clock.systemDefaultZone();

    public NiceIdServiceImpl(NiceIdMapper mapper, NiceIdGateway gateway, ConsentService consentService, NiceIdProperties properties) {
        this.mapper = mapper;
        this.gateway = gateway;
        this.consentService = consentService;
        this.properties = properties;
    }

    /** 테스트용 시계 교체 */
    void setClock(Clock clock) {
        this.clock = clock;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }

    // ================================================================ 정책

    @Override
    public IdvPurposeDto purpose(String purposeCd) {
        IdvPurposeDto p = activePurpose(purposeCd);
        // 화면에 필요한 값만 (검사 기준은 서버에만 둔다)
        IdvPurposeDto v = new IdvPurposeDto();
        v.setPurposeCd(p.getPurposeCd());
        v.setPurposeNm(p.getPurposeNm());
        v.setConsentBizCd(p.getConsentBizCd());
        v.setSmsYn(p.getSmsYn());
        v.setMoYn(p.getMoYn());
        v.setCodeValidSec(p.getCodeValidSec());
        v.setMaxTry(p.getMaxTry());
        return v;
    }

    private IdvPurposeDto activePurpose(String purposeCd) {
        IdvPurposeDto p = purposeCd == null ? null : mapper.selectPurpose(purposeCd);
        if (p == null || !"Y".equals(p.getUseYn())) throw new NiceIdException(NiceIdErrorCode.PURPOSE_NOT_FOUND);
        return p;
    }

    // ================================================================ ② SMS (통신사 미지정)

    @Override
    public IdvStartResult startSms(IdvStartRequest req, ConsentUser user) {
        IdvPurposeDto p = prepare(req, user, IdvReqDto.SMS);
        String phone = phone(req.getPhone());
        NiceIdGateway.SmsStarted s;
        try {
            s = gateway.startSms(req.getName().strip(), req.getBirth(), phone);       // ★ 통신사 코드 없이 요청
        } catch (NiceIdGatewayException e) {
            throw new NiceIdException(NiceIdErrorCode.PROVIDER_ERROR);
        }
        IdvReqDto r = newReq(req, user, p, IdvReqDto.SMS);
        r.setPhoneMask(mask(phone));
        r.setProviderTx(s.providerTx());
        mapper.insertReq(r);
        audit.info("start reqNo={} purpose={} method=SMS custId={} phone={}", r.getReqNo(), p.getPurposeCd(), user.custId(), r.getPhoneMask());
        return IdvStartResult.builder().reqNo(r.getReqNo()).method(IdvReqDto.SMS).expireAt(r.getExpireAt())
                .validSec(p.getCodeValidSec()).maxTry(p.getMaxTry()).phoneMask(r.getPhoneMask())
                .testCode(properties.isExposeTestCode() ? s.testCode() : null).build();
    }

    @Override
    public IdvResult verifySms(String reqNo, String code, ConsentUser user) {
        IdvReqDto r = mine(reqNo, user);
        if (!IdvReqDto.SMS.equals(r.getMethod())) throw new NiceIdException(NiceIdErrorCode.METHOD_NOT_ALLOWED);
        requireRequested(r);
        IdvPurposeDto p = mapper.selectPurpose(r.getPurposeCd());
        if (expired(r)) {
            close(r, IdvReqDto.EXPIRED, "EXPIRED", null, null);
            throw new NiceIdException(NiceIdErrorCode.EXPIRED);
        }
        if (code == null || !code.matches("^\\d{6}$")) throw new NiceIdException(NiceIdErrorCode.INVALID_INPUT, "인증번호 6자리를 입력하세요.");
        Optional<NiceIdIdentity> id;
        try {
            id = gateway.verifySms(r.getProviderTx(), code);
        } catch (NiceIdGatewayException e) {
            close(r, IdvReqDto.FAILED, "PROVIDER_ERROR", null, null);
            throw new NiceIdException(NiceIdErrorCode.PROVIDER_ERROR);
        }
        if (id.isEmpty()) {
            // 오입력 → 횟수 +1, 최대 횟수에 닿으면 이 요청은 실패로 닫는다 (처음부터 다시)
            mapper.increaseTry(reqNo);
            int tries = r.getTryCnt() + 1;
            if (tries >= p.getMaxTry()) {
                close(r, IdvReqDto.FAILED, "TRY_EXCEEDED", null, null);
                throw NiceIdException.format(NiceIdErrorCode.TRY_EXCEEDED, p.getMaxTry());
            }
            throw NiceIdException.format(NiceIdErrorCode.CODE_MISMATCH, tries, p.getMaxTry());
        }
        return finish(r, p, id.get());
    }

    // ================================================================ ② MO (기기 점유 - 휴대폰번호 입력 생략)

    @Override
    public IdvStartResult startMo(IdvStartRequest req, ConsentUser user) {
        IdvPurposeDto p = prepare(req, user, IdvReqDto.MO);
        NiceIdGateway.MoStarted m;
        try {
            m = gateway.startMo(req.getName().strip(), req.getBirth());
        } catch (NiceIdGatewayException e) {
            throw new NiceIdException(NiceIdErrorCode.PROVIDER_ERROR);
        }
        IdvReqDto r = newReq(req, user, p, IdvReqDto.MO);
        r.setProviderTx(m.providerTx());
        r.setMoCode(m.moCode());
        mapper.insertReq(r);
        audit.info("start reqNo={} purpose={} method=MO custId={}", r.getReqNo(), p.getPurposeCd(), user.custId());
        String smsUri = "sms:" + m.moNumber().replace("-", "") + "?body=" + URLEncoder.encode(m.moCode(), StandardCharsets.UTF_8).replace("+", "%20");
        return IdvStartResult.builder().reqNo(r.getReqNo()).method(IdvReqDto.MO).expireAt(r.getExpireAt())
                .validSec(p.getCodeValidSec()).maxTry(p.getMaxTry()).moNumber(m.moNumber()).moCode(m.moCode()).smsUri(smsUri).build();
    }

    @Override
    public IdvResult moStatus(String reqNo, ConsentUser user) {
        IdvReqDto r = mine(reqNo, user);
        if (!IdvReqDto.MO.equals(r.getMethod())) throw new NiceIdException(NiceIdErrorCode.METHOD_NOT_ALLOWED);
        if (!IdvReqDto.REQUESTED.equals(r.getStatus())) return toResult(r);     // 이미 끝난 요청은 결과 그대로
        IdvPurposeDto p = mapper.selectPurpose(r.getPurposeCd());
        if (expired(r)) {
            close(r, IdvReqDto.EXPIRED, "EXPIRED", null, null);
            return toResult(mapper.selectReq(reqNo));
        }
        Optional<NiceIdIdentity> id;
        try {
            id = gateway.checkMo(r.getProviderTx());
        } catch (NiceIdGatewayException e) {
            close(r, IdvReqDto.FAILED, "PROVIDER_ERROR", null, null);
            return toResult(mapper.selectReq(reqNo));
        }
        return id.isPresent() ? finish(r, p, id.get()) : toResult(r);           // 아직이면 REQUESTED (대기)
    }

    @Override
    public void demoMoSend(String reqNo, String phone, ConsentUser user) {
        if (!properties.isDemoApiOpen() || !(gateway instanceof MockNiceIdGateway mock)) {
            throw new NiceIdException(NiceIdErrorCode.DEMO_ONLY);
        }
        IdvReqDto r = mine(reqNo, user);
        requireRequested(r);
        mock.simulateMoSent(r.getProviderTx(), phone(phone));
    }

    // ================================================================ ③ 결과 검사 (업무 정책)

    /** 본인확인 결과를 업무 정책으로 검사하고 저장 */
    private IdvResult finish(IdvReqDto r, IdvPurposeDto p, NiceIdIdentity id) {
        // 0) 입력한 성명·생년월일과 휴대폰 명의가 같은지 (MO 는 번호를 입력하지 않으므로 특히 중요)
        if (!id.name().equals(r.getCustNm()) || !id.birthDt().equals(r.getBirthDt())) {
            close(r, IdvReqDto.FAILED, "NAME_MISMATCH", null, null);
            return toResult(mapper.selectReq(r.getReqNo()));
        }
        int age = Period.between(LocalDate.parse(id.birthDt(), BIRTH), LocalDate.now(clock)).getYears();
        String status = IdvReqDto.VERIFIED;
        String resultCd = "OK";
        try {
            // 1) 유의고객 - 주민번호 대신 CI 로 조회 (기존 TB_WATCHLIST)
            if ("Y".equals(p.getWatchCheckYn()) && mapper.countWatchlist(id.ci()) > 0) {
                status = IdvReqDto.BLOCKED;
                resultCd = "WATCHLIST";
            }
        } catch (DataAccessException e) {
            // 유의고객 확인을 못 하면 통과시키지 않는다 (fail-closed)
            status = IdvReqDto.FAILED;
            resultCd = "CHECK_ERROR";
        }
        // 2) 미성년자 - 생년월일로 만 나이
        if (IdvReqDto.VERIFIED.equals(status) && p.getMinAge() > 0 && age < p.getMinAge()) {
            status = IdvReqDto.BLOCKED;
            resultCd = "MINOR";
        }
        // 3) 외국인 - NICE 내외국인 구분
        if (IdvReqDto.VERIFIED.equals(status) && "F".equals(id.nationalCd()) && !"Y".equals(p.getForeignerYn())) {
            status = IdvReqDto.BLOCKED;
            resultCd = "FOREIGNER";
        }
        close(r, status, resultCd, id, age);
        audit.info("done reqNo={} purpose={} status={} result={} age={} national={}", r.getReqNo(), p.getPurposeCd(), status, resultCd, age, id.nationalCd());
        return toResult(mapper.selectReq(r.getReqNo()));
    }

    /** 요청 행을 결과 상태로 닫는다 (REQUESTED 일 때만 바뀜) */
    private void close(IdvReqDto r, String status, String resultCd, NiceIdIdentity id, Integer age) {
        r.setStatus(status);
        r.setResultCd(resultCd);
        if (id != null) {
            r.setCiVal(id.ci());
            r.setDiVal(id.di());
            r.setCellNo(id.cellNo());
            r.setGender(id.gender());
            r.setNationalCd(id.nationalCd());
            r.setAge(age);
        }
        r.setDoneAt(now());
        if (mapper.updateResult(r) == 0) throw new NiceIdException(NiceIdErrorCode.ALREADY_DONE);
    }

    // ================================================================ ④ 업무 서버 확인

    @Override
    public IdvResult checkVerified(String reqNo, String purposeCd, HttpServletRequest request) {
        return checkVerified(reqNo, purposeCd, ConsentUser.from(request));
    }

    @Override
    public IdvResult checkVerified(String reqNo, String purposeCd, ConsentUser user) {
        IdvReqDto r = mine(reqNo, user);
        if (!r.getPurposeCd().equals(purposeCd)) throw new NiceIdException(NiceIdErrorCode.REQ_NOT_FOUND);
        if (!IdvReqDto.VERIFIED.equals(r.getStatus())) throw new NiceIdException(NiceIdErrorCode.NOT_VERIFIED);
        IdvPurposeDto p = mapper.selectPurpose(purposeCd);
        if (r.getDoneAt().plusMinutes(p.getResultValidMin()).isBefore(now())) throw new NiceIdException(NiceIdErrorCode.RESULT_EXPIRED);
        IdvResult v = toResult(r);
        return IdvResult.builder().reqNo(v.getReqNo()).purposeCd(v.getPurposeCd()).method(v.getMethod()).status(v.getStatus())
                .resultCd(v.getResultCd()).message(v.getMessage()).name(v.getName()).birthDt(v.getBirthDt()).age(v.getAge())
                .gender(v.getGender()).nationalCd(v.getNationalCd()).cellNoMask(v.getCellNoMask()).doneAt(v.getDoneAt())
                .ci(r.getCiVal()).di(r.getDiVal()).cellNo(r.getCellNo()).build();
    }

    // ================================================================ 공통 도우미

    /** 시작 전 공통 검사: 업무 정책 · 인증 방식 · 채널 · 약관동의 · 입력값 · 과다 요청 */
    private IdvPurposeDto prepare(IdvStartRequest req, ConsentUser user, String method) {
        if (req == null) throw new NiceIdException(NiceIdErrorCode.INVALID_REQUEST);
        IdvPurposeDto p = activePurpose(req.getPurposeCd());
        if (!"Y".equals(IdvReqDto.SMS.equals(method) ? p.getSmsYn() : p.getMoYn())) {
            throw new NiceIdException(NiceIdErrorCode.METHOD_NOT_ALLOWED);
        }
        if (!CHANNELS.contains(req.getChannel())) throw new NiceIdException(NiceIdErrorCode.INVALID_REQUEST);
        if (p.getConsentBizCd() != null) {
            // 약관(본인확인서비스 · 고유식별정보 · 비여신금융거래)은 공통#22 약관동의 결과로 확인 - 화면 값은 믿지 않는다
            if (req.getAgreeNo() == null) throw new NiceIdException(NiceIdErrorCode.CONSENT_REQUIRED);
            try {
                consentService.checkAgreed(req.getAgreeNo(), p.getConsentBizCd(), user);
            } catch (ConsentException e) {
                throw new NiceIdException(NiceIdErrorCode.CONSENT_REQUIRED, NiceIdErrorCode.CONSENT_REQUIRED.getMessage() + " (" + e.getMessage() + ")");
            }
        }
        if (req.getName() == null || !NAME.matcher(req.getName().strip()).matches()) {
            throw new NiceIdException(NiceIdErrorCode.INVALID_INPUT, "성명을 정확히 입력하세요.");
        }
        birth(req.getBirth());
        if (mapper.countRecentBySession(user.sessKey(), now().minusMinutes(10)) >= properties.getMaxRequestsPer10Min()) {
            throw new NiceIdException(NiceIdErrorCode.TOO_MANY_REQUESTS);
        }
        return p;
    }

    private IdvReqDto newReq(IdvStartRequest req, ConsentUser user, IdvPurposeDto p, String method) {
        IdvReqDto r = new IdvReqDto();
        byte[] b = new byte[16];
        random.nextBytes(b);
        r.setReqNo(HexFormat.of().formatHex(b));                 // 32자 난수 - 순번이 아니라서 남의 요청을 추측할 수 없다
        r.setPurposeCd(p.getPurposeCd());
        r.setMethod(method);
        r.setChannel(req.getChannel());
        r.setAgreeNo(req.getAgreeNo());
        r.setCustId(user.custId());
        r.setSessKey(user.sessKey());
        r.setCustNm(req.getName().strip());
        r.setBirthDt(req.getBirth());
        r.setClientIp(user.clientIp());
        r.setReqAt(now());
        r.setExpireAt(r.getReqAt().plusSeconds(p.getCodeValidSec()));
        r.setTryCnt(0);
        return r;
    }

    /** 요청 행 조회 + 본인 요청인지 (회원이면 회원ID, 비로그인이면 같은 세션) */
    private IdvReqDto mine(String reqNo, ConsentUser user) {
        IdvReqDto r = reqNo == null ? null : mapper.selectReq(reqNo);
        boolean mine = r != null && (r.getCustId() != null ? r.getCustId().equals(user.custId()) : r.getSessKey().equals(user.sessKey()));
        if (!mine) throw new NiceIdException(NiceIdErrorCode.REQ_NOT_FOUND);
        return r;
    }

    private static void requireRequested(IdvReqDto r) {
        if (!IdvReqDto.REQUESTED.equals(r.getStatus())) throw new NiceIdException(NiceIdErrorCode.ALREADY_DONE);
    }

    private boolean expired(IdvReqDto r) {
        return !r.getExpireAt().isAfter(now());
    }

    private void birth(String value) {
        try {
            LocalDate d = LocalDate.parse(value == null ? "" : value, BIRTH);
            if (d.isAfter(LocalDate.now(clock)) || d.getYear() < 1900) throw new DateTimeParseException("range", value, 0);
        } catch (DateTimeParseException e) {
            throw new NiceIdException(NiceIdErrorCode.INVALID_INPUT, "생년월일 8자리(예: 19900101)를 정확히 입력하세요.");
        }
    }

    private static String phone(String value) {
        String v = value == null ? "" : value.replaceAll("[^0-9]", "");
        if (!PHONE.matcher(v).matches()) throw new NiceIdException(NiceIdErrorCode.INVALID_INPUT, "휴대폰번호를 정확히 입력하세요.");
        return v;
    }

    /** 01012345678 → 010-****-5678 */
    static String mask(String phone) {
        if (phone == null || phone.length() < 10) return null;
        return phone.substring(0, 3) + "-****-" + phone.substring(phone.length() - 4);
    }

    /** 화면용 결과 (CI/DI/전체 휴대폰번호 제외) */
    static IdvResult toResult(IdvReqDto r) {
        return IdvResult.builder().reqNo(r.getReqNo()).purposeCd(r.getPurposeCd()).method(r.getMethod()).status(r.getStatus())
                .resultCd(r.getResultCd()).message(message(r)).name(r.getCustNm()).birthDt(r.getBirthDt()).age(r.getAge())
                .gender(r.getGender()).nationalCd(r.getNationalCd()).cellNoMask(mask(r.getCellNo())).doneAt(r.getDoneAt()).build();
    }

    /** 결과코드 → 고객 안내 문구 (차단 사유를 구체적으로 노출하지 않도록 유의고객은 일반 문구) */
    static String message(IdvReqDto r) {
        if (IdvReqDto.REQUESTED.equals(r.getStatus())) {
            return IdvReqDto.MO.equals(r.getMethod()) ? "문자 수신을 기다리고 있습니다." : "인증번호를 입력하세요.";
        }
        return switch (r.getResultCd() == null ? "" : r.getResultCd()) {
            case "OK" -> "본인인증이 완료되었습니다.";
            case "MINOR" -> "미성년자는 비대면으로 진행할 수 없습니다. 법정대리인 동의 후 영업점을 방문해 주세요.";
            case "FOREIGNER" -> "외국인 고객은 외국인등록증 확인 등 추가 절차가 필요합니다. 고객센터(1588-0000)로 문의해 주세요.";
            case "WATCHLIST" -> "고객님의 안전한 금융거래를 위해 비대면 본인인증이 제한되었습니다. 고객센터(1588-0000)로 문의해 주세요.";
            case "NAME_MISMATCH" -> "입력한 성명·생년월일이 휴대폰 명의와 다릅니다. 본인 명의 휴대폰으로 다시 진행해 주세요.";
            case "TRY_EXCEEDED" -> "인증번호 입력 횟수를 초과했습니다. 처음부터 다시 진행해 주세요.";
            case "EXPIRED" -> "인증 시간이 지났습니다. 처음부터 다시 진행해 주세요.";
            default -> "본인인증을 완료하지 못했습니다. 잠시 후 다시 시도해 주세요.";
        };
    }
}
