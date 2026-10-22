package com.example.demojsp.common.platformlink.css;

import com.example.demojsp.common.platformlink.config.PlatformLinkProperties;
import com.example.demojsp.common.platformlink.model.CssProduct;
import org.springframework.stereotype.Component;

/** [시뮬레이터] 현행 CSS - 지금 플랫폼 연계가 쓰는 엔진. 응답이 느린 편(legacy-latency-ms) */
@Component
public class LegacyCssSimulator extends CssSimulator {

    private final PlatformLinkProperties props;

    public LegacyCssSimulator(PlatformLinkProperties props) {
        this.props = props;
    }

    @Override
    public String engine() {
        return LEGACY;
    }

    @Override
    protected int latencyMs() {
        return props.getLegacyLatencyMs();
    }

    @Override
    protected int slowLatencyMs() {
        return props.getSlowLatencyMs();
    }

    @Override
    protected int multiplePct(String product) {
        return switch (product) {
            case CssProduct.AUTO -> 70;
            case CssProduct.MORTGAGE -> 350;
            default -> 40;
        };
    }

    @Override
    protected int baseRateBp(String product) {
        return switch (product) {
            case CssProduct.AUTO -> 1190;
            case CssProduct.MORTGAGE -> 690;
            default -> 1490;
        };
    }
}
