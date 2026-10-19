package com.example.demojsp.common.niceid.gateway;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NICE ID 시뮬레이터 (local / 개발 전용). 실제 문자는 보내지 않는다.
 *
 * <ul>
 *   <li>인증번호 6자리를 만들어 메모리에 보관 → 화면에는 testCode 로 알려준다 (expose-test-code=true 일 때)</li>
 *   <li>CI = Base64(SHA-512("성명|생년월일|휴대폰")) → 실제와 같은 88자. 같은 사람이면 항상 같은 CI (유의고객 테스트 가능)</li>
 *   <li>성명에 영문자가 있으면 외국인(F) 으로 돌려준다 (외국인 차단 테스트)</li>
 *   <li>MO: {@link #simulateMoSent} 로 "고객이 문자를 보냈다" 를 흉내 낸다 (/api/common/niceid/mo/demo-send)</li>
 * </ul>
 * 서버를 재기동하면 진행 중이던 시뮬레이션 거래는 사라진다 (DB 의 요청 행은 만료 처리됨).
 */
@Component
@ConditionalOnProperty(name = "common.niceid.gateway", havingValue = "mock", matchIfMissing = true)
public class MockNiceIdGateway implements NiceIdGateway {

    /** NICE MO 수신번호 (시뮬레이터 값 - 실제 번호는 NICE 계약 시 안내) */
    public static final String MO_NUMBER = "1600-1522";

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Pending> pending = new ConcurrentHashMap<>();

    private static final class Pending {
        final String name;
        final String birthDt;
        volatile String phone;      // SMS: 입력값, MO: 문자를 보낸 번호
        final String code;          // SMS 인증번호
        Pending(String name, String birthDt, String phone, String code) {
            this.name = name;
            this.birthDt = birthDt;
            this.phone = phone;
            this.code = code;
        }
    }

    @Override
    public SmsStarted startSms(String name, String birthDt, String phone) {
        String tx = "NICE-SMS-" + hex(8);
        String code = String.format("%06d", random.nextInt(1_000_000));
        pending.put(tx, new Pending(name, birthDt, phone, code));
        return new SmsStarted(tx, code);
    }

    @Override
    public Optional<NiceIdIdentity> verifySms(String providerTx, String code) {
        Pending p = pending.get(providerTx);
        if (p == null) throw new NiceIdGatewayException("NICE 거래번호를 찾을 수 없습니다. (시뮬레이터 재기동 등)");
        if (!p.code.equals(code)) return Optional.empty();
        pending.remove(providerTx);
        return Optional.of(identity(p.name, p.birthDt, p.phone));
    }

    @Override
    public MoStarted startMo(String name, String birthDt) {
        String tx = "NICE-MO-" + hex(8);
        String moCode = "PEPPER " + String.format("%04d", random.nextInt(10_000));
        pending.put(tx, new Pending(name, birthDt, null, moCode));
        return new MoStarted(tx, MO_NUMBER, moCode);
    }

    @Override
    public Optional<NiceIdIdentity> checkMo(String providerTx) {
        Pending p = pending.get(providerTx);
        if (p == null) throw new NiceIdGatewayException("NICE 거래번호를 찾을 수 없습니다. (시뮬레이터 재기동 등)");
        if (p.phone == null) return Optional.empty();            // 아직 문자 안 옴
        pending.remove(providerTx);
        return Optional.of(identity(p.name, p.birthDt, p.phone));
    }

    /** [시뮬레이터 전용] 고객 휴대폰(phone)에서 MO 문자를 보낸 것으로 처리 */
    public boolean simulateMoSent(String providerTx, String phone) {
        Pending p = pending.get(providerTx);
        if (p == null) return false;
        p.phone = phone;
        return true;
    }

    /** 시뮬레이터 본인확인 결과 - 같은 성명·생년월일·휴대폰이면 항상 같은 CI */
    public static NiceIdIdentity identity(String name, String birthDt, String phone) {
        byte[] ci = digest("SHA-512", name + "|" + birthDt + "|" + phone);
        byte[] di = digest("SHA-256", "DI|" + name + "|" + birthDt);
        String national = name.matches(".*[A-Za-z].*") ? "F" : "L";
        return new NiceIdIdentity(Base64.getEncoder().encodeToString(ci), HexFormat.of().formatHex(di),
                name, birthDt, "M", national, phone);
    }

    private static byte[] digest(String alg, String value) {
        try {
            return MessageDigest.getInstance(alg).digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String hex(int bytes) {
        byte[] b = new byte[bytes];
        random.nextBytes(b);
        return HexFormat.of().formatHex(b);
    }
}
