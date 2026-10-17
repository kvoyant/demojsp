package com.example.demojsp.common.identity.provider.nice;

import com.example.demojsp.common.identity.exception.IdentityVerificationException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** NICE 암복호화 단위 테스트 - 키 길이, 왕복 암복호화, 위변조 탐지 */
class NiceIdCryptoTest {

    private final NiceIdCrypto.Keys keys = NiceIdCrypto.deriveKeys("20261018120000", "IDV20261018120000ABCDEFGHJK", "tokenValSample");

    @Test
    void 키는_가이드대로_16_16_32자로_생성된다() {
        assertThat(keys.key()).hasSize(16);
        assertThat(keys.iv()).hasSize(16);
        assertThat(keys.hmacKey()).hasSize(32);
        assertThat(keys.hmacKey()).startsWith(keys.key());
    }

    @Test
    void 같은_입력이면_같은_키() {
        NiceIdCrypto.Keys again = NiceIdCrypto.deriveKeys(" 20261018120000 ", "IDV20261018120000ABCDEFGHJK", "tokenValSample ");
        assertThat(again).isEqualTo(keys); // trim 처리 확인
    }

    @Test
    void 암호화_복호화_왕복() {
        String json = "{\"requestno\":\"IDV1\",\"name\":\"홍길동\"}";
        String enc = NiceIdCrypto.encrypt(keys, json);
        assertThat(NiceIdCrypto.decrypt(keys, enc, StandardCharsets.UTF_8)).isEqualTo(json);
    }

    @Test
    void 무결성값_검증_및_위변조_탐지() {
        String enc = NiceIdCrypto.encrypt(keys, "{\"a\":1}");
        String mac = NiceIdCrypto.hmac(keys, enc);
        assertThat(NiceIdCrypto.verifyIntegrity(keys, enc, mac)).isTrue();
        assertThat(NiceIdCrypto.verifyIntegrity(keys, enc + "x", mac)).isFalse();
        assertThat(NiceIdCrypto.verifyIntegrity(keys, enc, null)).isFalse();
    }

    @Test
    void 다른_키로_복호화하면_위변조_예외() {
        String enc = NiceIdCrypto.encrypt(keys, "{\"a\":1}");
        NiceIdCrypto.Keys other = NiceIdCrypto.deriveKeys("20261018120001", "X", "Y");
        assertThatThrownBy(() -> NiceIdCrypto.decrypt(other, enc, StandardCharsets.UTF_8))
                .isInstanceOf(IdentityVerificationException.class);
    }
}
