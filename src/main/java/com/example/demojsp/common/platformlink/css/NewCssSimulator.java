package com.example.demojsp.common.platformlink.css;

import com.example.demojsp.common.platformlink.config.PlatformLinkProperties;
import com.example.demojsp.common.platformlink.model.CssProduct;
import org.springframework.stereotype.Component;

/**
 * [시뮬레이터] 신규 CSS (전략관리시스템). CSS 프로젝트 I/F 정의서가 나오기 전 선행 개발용 (요구분석서 CF-BC-10 권고).
 * 실연동 구현체를 만들면 이 클래스 대신 engine()="NEW" 인 구현체를 등록한다.
 */
@Component
public class NewCssSimulator extends CssSimulator {

    private final PlatformLinkProperties props;

    public NewCssSimulator(PlatformLinkProperties props) {
        this.props = props;
    }

    @Override
    public String engine() {
        return NEW;
    }

    @Override
    protected int latencyMs() {
        return props.getNewLatencyMs();
    }

    @Override
    protected int slowLatencyMs() {
        return props.getSlowLatencyMs();
    }

    @Override
    protected int multiplePct(String product) {
        return switch (product) {
            case CssProduct.AUTO -> 80;
            case CssProduct.MORTGAGE -> 400;
            default -> 50;
        };
    }

    @Override
    protected int baseRateBp(String product) {
        return switch (product) {
            case CssProduct.AUTO -> 1090;
            case CssProduct.MORTGAGE -> 640;
            default -> 1390;
        };
    }
}
