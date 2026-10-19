package com.example.demojsp.common.acctverify.service;

import com.example.demojsp.common.acctverify.config.AcctVerifyProperties;
import com.example.demojsp.common.acctverify.dto.AvBankDto;
import com.example.demojsp.common.acctverify.dto.AvBizDto;
import com.example.demojsp.common.acctverify.dto.AvContextView;
import com.example.demojsp.common.acctverify.dto.AvPrefillDto;
import com.example.demojsp.common.acctverify.dto.AvReqDto;
import com.example.demojsp.common.acctverify.dto.AvRequest;
import com.example.demojsp.common.acctverify.dto.AvResult;
import com.example.demojsp.common.acctverify.dto.AvStartResult;
import com.example.demojsp.common.acctverify.exception.AcctVerifyErrorCode;
import com.example.demojsp.common.acctverify.exception.AcctVerifyException;
import com.example.demojsp.common.acctverify.gateway.FirmBankingException;
import com.example.demojsp.common.acctverify.gateway.FirmBankingGateway;
import com.example.demojsp.common.acctverify.mapper.AcctVerifyMapper;
import com.example.demojsp.common.consent.service.ConsentUser;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;

/**
 * 트랜잭션 메모: request / verify 는 일부러 @Transactional 을 붙이지 않는다.
 * 실패 이력(타인 계좌·없는 계좌·송금 실패)과 오입력 횟수를 저장한 뒤 예외를 던지는데, 트랜잭션이면 그 기록까지 롤백된다.
 */
@Service
public class AcctVerifyServiceImpl implements AcctVerifyService {

    private static final Logger audit = LoggerFactory.getLogger("AUDIT.ACCT_VERIFY");
    private static final Set<String> CHANNELS = Set.of("APP", "PC_WEB", "AGENT_WEB");

    private final AcctVerifyMapper mapper;
    private final FirmBankingGateway gateway;
    private final AcctVerifyProperties properties;
    private final SecureRandom random = new SecureRandom();
    private Clock clock = Clock.systemDefaultZone();

    public AcctVerifyServiceImpl(AcctVerifyMapper mapper, FirmBankingGateway gateway, AcctVerifyProperties properties) {
        this.mapper = mapper;
        this.gateway = gateway;
        this.properties = properties;
    }

    void setClock(Clock clock) {
        this.clock = clock;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }

    // ================================================================ 화면 Context

    @Override
    public AvContextView context(String bizCd, AcctCustomer c) {
        AvBizDto biz = activeBiz(bizCd);
        AvContextView v = new AvContextView();
        v.setBizCd(biz.getBizCd());
        v.setBizNm(biz.getBizNm());
        v.setScreenType(biz.getScreenType());
        v.setCustNm(c.custNm());
        v.setCodeValidMin(biz.getCodeValidMin());
        v.setMaxTry(biz.getMaxTry());
        v.setBanks(mapper.selectBanks());
        // 요구사항 1: 대출 입출금계좌 화면이면 대출금 입금/출금계좌를 불러와 자동완성 (회원만 - 대출 원장은 회원 ID 기준)
        if (prefillAllowed(biz) && c.custId() != null) {
            for (Map<String, Object> row : mapper.selectLoanAccounts(c.custId())) {
                AvPrefillDto p = new AvPrefillDto();
                p.setPrefillId(str(row, "LOAN_NO"));
                p.setProductNm(str(row, "PRODUCT_NM"));
                p.setBankCd(str(row, "BANK_CD"));
                p.setBankNm(str(row, "BANK_NM"));
                p.setAcctMask(mask(digits(str(row, "ACCOUNT_NO"))));     // 전체 계좌번호는 화면에 보내지 않는다
                p.setUsage("대출금 입금 · 원리금 출금");
                v.getPrefills().add(p);
            }
        }
        return v;
    }

    // ================================================================ 1원 송금

    @Override
    public AvStartResult request(AvRequest req, AcctCustomer c) {
        if (req == null || !CHANNELS.contains(req.getChannel())) throw new AcctVerifyException(AcctVerifyErrorCode.INVALID_REQUEST);
        AvBizDto biz = activeBiz(req.getBizCd());
        if (mapper.countRecentBySession(c.sessKey(), now().minusMinutes(10)) >= properties.getMaxRequestsPer10Min()) {
            throw new AcctVerifyException(AcctVerifyErrorCode.TOO_MANY_REQUESTS);
        }

        // ① 계좌 확정 - 자동완성 그대로면 서버가 DB 에서 다시 읽는다 (화면 값 불신)
        AvReqDto r = new AvReqDto();
        if (req.getPrefillId() != null && !req.getPrefillId().isBlank()) {
            Map<String, Object> loan = prefillAllowed(biz) && c.custId() != null ? mapper.selectLoanAccount(c.custId(), req.getPrefillId()) : null;
            if (loan == null) throw new AcctVerifyException(AcctVerifyErrorCode.PREFILL_NOT_FOUND);
            r.setBankCd(str(loan, "BANK_CD"));
            r.setBankNm(str(loan, "BANK_NM"));
            r.setAcctNo(digits(str(loan, "ACCOUNT_NO")));
            r.setSource("PREFILL");
            r.setPrefillId(req.getPrefillId());
            r.setModifiedYn("N");
        } else {
            // 요구사항 2: [수정] 후 은행 선택 · 계좌 직접 입력
            AvBankDto bank = req.getBankCd() == null ? null : mapper.selectBank(req.getBankCd());
            String acct = digits(req.getAcctNo());
            if (bank == null || acct.length() < 10 || acct.length() > 16) throw new AcctVerifyException(AcctVerifyErrorCode.INVALID_ACCOUNT);
            r.setBankCd(bank.getBankCd());
            r.setBankNm(bank.getBankNm());
            r.setAcctNo(acct);
            r.setSource("INPUT");
            r.setModifiedYn("Y".equals(req.getModifiedYn()) ? "Y" : "N");
        }
        r.setAcctMask(mask(r.getAcctNo()));
        fill(r, biz, req, c);

        // ② 예금주 확인 - 본인 명의만
        FirmBankingGateway.OwnerCheck owner;
        try {
            owner = gateway.checkOwner(r.getBankCd(), r.getAcctNo(), c.custNm());
        } catch (FirmBankingException e) {
            failRow(r, "TRANSFER_FAIL");
            throw new AcctVerifyException(AcctVerifyErrorCode.TRANSFER_FAIL);
        }
        if (!owner.exists()) {
            failRow(r, "NO_ACCOUNT");
            throw new AcctVerifyException(AcctVerifyErrorCode.NO_ACCOUNT);
        }
        r.setHolderNm(owner.holderNm());
        if ("Y".equals(biz.getOwnerCheckYn()) && !sameName(owner.holderNm(), c.custNm())) {
            failRow(r, "NOT_OWNER");
            throw new AcctVerifyException(AcctVerifyErrorCode.NOT_OWNER,
                    AcctVerifyErrorCode.NOT_OWNER.getMessage() + " (예금주 " + maskName(owner.holderNm()) + ")");
        }

        // ③ 1원 송금 - 입금자명 = 접두어 + 숫자 4자리 (예: 페퍼3821)
        String code = String.format("%04d", random.nextInt(10_000));
        try {
            r.setProviderTx(gateway.transferOneWon(r.getBankCd(), r.getAcctNo(), biz.getMemoPrefix() + code));
        } catch (FirmBankingException e) {
            failRow(r, "TRANSFER_FAIL");
            throw new AcctVerifyException(AcctVerifyErrorCode.TRANSFER_FAIL);
        }
        r.setCodeHash(sha256(r.getReqNo() + ":" + code));            // 인증코드 원문은 저장하지 않는다
        r.setStatus(AvReqDto.SENT);
        mapper.insertReq(r);
        audit.info("sent reqNo={} biz={} custId={} bank={} acct={} source={} modified={}", r.getReqNo(), biz.getBizCd(), c.custId(),
                r.getBankCd(), r.getAcctMask(), r.getSource(), r.getModifiedYn());
        return AvStartResult.builder().reqNo(r.getReqNo()).bankNm(r.getBankNm()).acctMask(r.getAcctMask()).holderNm(r.getHolderNm())
                .memoPrefix(biz.getMemoPrefix()).expireAt(r.getExpireAt()).validSec(biz.getCodeValidMin() * 60).maxTry(biz.getMaxTry())
                .testCode(properties.isExposeTestCode() ? code : null).build();
    }

    // ================================================================ 인증코드 확인

    @Override
    public AvResult verify(String reqNo, String code, AcctCustomer c) {
        AvReqDto r = mine(reqNo, new ConsentUser(c.custId(), c.sessKey(), c.clientIp()));
        if (!AvReqDto.SENT.equals(r.getStatus())) throw new AcctVerifyException(AcctVerifyErrorCode.ALREADY_DONE);
        AvBizDto biz = mapper.selectBiz(r.getBizCd());
        if (!r.getExpireAt().isAfter(now())) {
            close(r, AvReqDto.EXPIRED, "EXPIRED");
            throw new AcctVerifyException(AcctVerifyErrorCode.EXPIRED);
        }
        if (code == null || !code.matches("^\\d{4}$")) {
            throw new AcctVerifyException(AcctVerifyErrorCode.INVALID_VALUE, "입금자명 '" + biz.getMemoPrefix() + "' 뒤 숫자 4자리를 입력하세요.");
        }
        if (!MessageDigest.isEqual(sha256(reqNo + ":" + code).getBytes(StandardCharsets.UTF_8), r.getCodeHash().getBytes(StandardCharsets.UTF_8))) {
            mapper.increaseTry(reqNo);
            int tries = r.getTryCnt() + 1;
            if (tries >= biz.getMaxTry()) {
                close(r, AvReqDto.FAILED, "TRY_EXCEEDED");
                throw AcctVerifyException.format(AcctVerifyErrorCode.TRY_EXCEEDED, biz.getMaxTry());
            }
            throw AcctVerifyException.format(AcctVerifyErrorCode.CODE_MISMATCH, tries, biz.getMaxTry());
        }
        close(r, AvReqDto.VERIFIED, "OK");
        audit.info("verified reqNo={} biz={} acct={}", reqNo, r.getBizCd(), r.getAcctMask());
        return toResult(mapper.selectReq(reqNo), false);
    }

    // ================================================================ 업무 서버 확인

    @Override
    public AvResult checkVerified(String reqNo, String bizCd, HttpServletRequest request) {
        return checkVerified(reqNo, bizCd, ConsentUser.from(request));
    }

    @Override
    public AvResult checkVerified(String reqNo, String bizCd, ConsentUser user) {
        AvReqDto r = mine(reqNo, user);
        if (!r.getBizCd().equals(bizCd)) throw new AcctVerifyException(AcctVerifyErrorCode.REQ_NOT_FOUND);
        if (!AvReqDto.VERIFIED.equals(r.getStatus())) throw new AcctVerifyException(AcctVerifyErrorCode.NOT_VERIFIED);
        AvBizDto biz = mapper.selectBiz(bizCd);
        if (r.getDoneAt().plusMinutes(biz.getResultValidMin()).isBefore(now())) throw new AcctVerifyException(AcctVerifyErrorCode.RESULT_EXPIRED);
        return toResult(r, true);
    }

    // ================================================================ 도우미

    private AvBizDto activeBiz(String bizCd) {
        AvBizDto b = bizCd == null ? null : mapper.selectBiz(bizCd);
        if (b == null || !"Y".equals(b.getUseYn())) throw new AcctVerifyException(AcctVerifyErrorCode.BIZ_NOT_FOUND);
        return b;
    }

    private static boolean prefillAllowed(AvBizDto biz) {
        return AvBizDto.LOAN.equals(biz.getScreenType()) && "Y".equals(biz.getPrefillYn());
    }

    /** 요청 행 공통 값 */
    private void fill(AvReqDto r, AvBizDto biz, AvRequest req, AcctCustomer c) {
        byte[] b = new byte[16];
        random.nextBytes(b);
        r.setReqNo(HexFormat.of().formatHex(b));
        r.setBizCd(biz.getBizCd());
        r.setChannel(req.getChannel());
        r.setCustId(c.custId());
        r.setSessKey(c.sessKey());
        r.setCustNm(c.custNm());
        r.setClientIp(c.clientIp());
        r.setReqAt(now());
        r.setExpireAt(r.getReqAt().plusMinutes(biz.getCodeValidMin()));
    }

    /** 송금 전에 끝난 실패도 이력으로 남긴다 (타인 계좌 · 없는 계좌 · 송금 실패) */
    private void failRow(AvReqDto r, String resultCd) {
        r.setStatus(AvReqDto.FAILED);
        r.setResultCd(resultCd);
        r.setDoneAt(now());
        mapper.insertReq(r);
        audit.info("failed reqNo={} biz={} acct={} result={}", r.getReqNo(), r.getBizCd(), r.getAcctMask(), resultCd);
    }

    private void close(AvReqDto r, String status, String resultCd) {
        if (mapper.updateResult(r.getReqNo(), status, resultCd, now()) == 0) throw new AcctVerifyException(AcctVerifyErrorCode.ALREADY_DONE);
    }

    private AvReqDto mine(String reqNo, ConsentUser u) {
        AvReqDto r = reqNo == null ? null : mapper.selectReq(reqNo);
        boolean mine = r != null && (r.getCustId() != null ? r.getCustId().equals(u.custId()) : r.getSessKey().equals(u.sessKey()));
        if (!mine) throw new AcctVerifyException(AcctVerifyErrorCode.REQ_NOT_FOUND);
        return r;
    }

    static AvResult toResult(AvReqDto r, boolean withAcctNo) {
        return AvResult.builder().reqNo(r.getReqNo()).bizCd(r.getBizCd()).status(r.getStatus()).resultCd(r.getResultCd())
                .message(AvReqDto.VERIFIED.equals(r.getStatus()) ? "계좌인증이 완료되었습니다." : "계좌인증을 완료하지 못했습니다.")
                .bankCd(r.getBankCd()).bankNm(r.getBankNm()).acctMask(r.getAcctMask()).holderNm(r.getHolderNm())
                .source(r.getSource()).modifiedYn(r.getModifiedYn()).doneAt(r.getDoneAt())
                .acctNo(withAcctNo ? r.getAcctNo() : null).build();
    }

    static boolean sameName(String a, String b) {
        return a != null && b != null && a.replace(" ", "").equalsIgnoreCase(b.replace(" ", ""));
    }

    static String digits(String v) {
        return v == null ? "" : v.replaceAll("[^0-9]", "");
    }

    /** 1234020456789 → 123*******789 */
    static String mask(String digits) {
        if (digits.length() <= 6) return digits;
        return digits.substring(0, 3) + "*".repeat(digits.length() - 6) + digits.substring(digits.length() - 3);
    }

    /** 김타인 → 김*인 */
    static String maskName(String n) {
        if (n == null || n.length() < 2) return n;
        return n.length() == 2 ? n.charAt(0) + "*" : n.charAt(0) + "*".repeat(n.length() - 2) + n.charAt(n.length() - 1);
    }

    private static String str(Map<String, Object> row, String key) {
        Object v = row.get(key);
        if (v == null) v = row.get(key.toLowerCase());
        return v == null ? null : v.toString();
    }

    private static String sha256(String v) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(v.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
