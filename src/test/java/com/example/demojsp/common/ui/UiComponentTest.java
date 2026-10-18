package com.example.demojsp.common.ui;

import com.example.demojsp.common.ui.sample.UiComponentSampleController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * [CF-UI-01] 공통 UI 컴포넌트 테스트 (standalone MockMvc - DB 불필요).
 *
 * <p>공통 UI 컴포넌트는 정적 자원(CSS/JS/SVG)이므로, 요구사항이 정적 자원에 빠짐없이 반영돼 있는지
 * "계약(contract)" 형태로 검증한다. (화면 동작은 샘플 화면에서 수동 확인 - docs/COMMON_UI_COMPONENT_GUIDE.md)
 */
class UiComponentTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new UiComponentSampleController()).build();
    }

    static String resource(String path) throws IOException {
        try (InputStream in = UiComponentTest.class.getResourceAsStream("/static" + path)) {
            assertThat(in).as("정적 자원 존재: " + path).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // ===================================================================== 공통#40 Button

    @Test
    @DisplayName("공통#40 샘플 화면 - 뷰 이름, 페이지 이동 데모 안내 플래그")
    void buttonSamplePage() throws Exception {
        mvc.perform(get("/common/ui/button"))
                .andExpect(status().isOk())
                .andExpect(view().name("common/ui/button"))
                .andExpect(model().attribute("navigatedFrom", false));
        mvc.perform(get("/common/ui/button").param("from", "nav-demo"))
                .andExpect(model().attribute("navigatedFrom", true));
    }

    @Test
    @DisplayName("공통#40 샘플 지연은 최대 3초로 제한 (요청 스레드 점유 방지)")
    void buttonSampleDelayCapped() throws Exception {
        long start = System.nanoTime();
        mvc.perform(get("/common/ui/button").param("delayMs", "-500")).andExpect(status().isOk());
        assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(1000);
        assertThat(UiComponentSampleControllerAccess.maxDelay()).isEqualTo(3000);
    }

    @Test
    @DisplayName("공통#40 CSS - press 효과 · 진행 중 · 비활성 · 포커스 · 움직임 줄이기 상태가 정의돼 있다")
    void buttonCssContract() throws Exception {
        String css = resource("/css/common/ui-button.css");
        assertThat(css)
                .contains(".ui-btn:active", ".ui-btn.is-pressed", "transform: scale(.97)")      // 누른 효과
                .contains(".ui-btn.is-busy", ".ui-btn__spinner", ".ui-nav-progress")           // 업무 실행 / 페이지 이동 중
                .contains(".ui-btn:disabled", ":focus-visible")
                .contains("prefers-reduced-motion", "touch-action: manipulation")
                .contains(".ui-btn--primary", ".ui-btn--secondary", ".ui-btn--outline", ".ui-btn--text", ".ui-btn--danger")
                .contains(".ui-btn--lg", ".ui-btn--sm", ".ui-cta-bar");
        assertThat(css).as("busy 중에는 회색(비활성)으로 바꾸지 않는다").doesNotContain(".is-busy:disabled {");
    }

    @Test
    @DisplayName("공통#40 디자인 토큰 - 버튼 글자/배경 색상 대비 4.5:1 이상 (WCAG AA)")
    void buttonContrast() throws Exception {
        String tokens = resource("/css/common/ui-tokens.css");
        String primary = token(tokens, "--ui-primary");
        String primaryPress = token(tokens, "--ui-primary-press");
        String onPrimary = token(tokens, "--ui-on-primary");
        assertThat(contrast(onPrimary, primary)).isGreaterThanOrEqualTo(4.5);
        assertThat(contrast(onPrimary, primaryPress)).isGreaterThanOrEqualTo(4.5);
        assertThat(contrast(primary, token(tokens, "--ui-primary-soft"))).isGreaterThanOrEqualTo(4.5);   // secondary
        assertThat(contrast(token(tokens, "--ui-text"), token(tokens, "--ui-surface"))).isGreaterThanOrEqualTo(4.5);
        assertThat(contrast("#ffffff", token(tokens, "--ui-danger"))).isGreaterThanOrEqualTo(4.5);
        assertThat(contrast(token(tokens, "--ui-text-disabled"), token(tokens, "--ui-disabled-bg"))).isGreaterThanOrEqualTo(4.5);
        // press 색은 기본 색보다 확실히 진해야 "눌림" 을 알아챈다 (명도 대비 1.3배 이상)
        assertThat(contrast(primary, primaryPress)).isGreaterThanOrEqualTo(1.3);
    }

    @Test
    @DisplayName("공통#40 JS - run/navigate/busy/done API 와 중복 실행 차단 · bfcache 복원 처리가 있다")
    void buttonJsContract() throws Exception {
        String js = resource("/js/common/ui-button.js");
        assertThat(js)
                .contains("global.UiButton", "run: run", "navigate: navigate", "busy: busy", "done: done")
                .contains("stopImmediatePropagation")       // busy 버튼 클릭이 업무 핸들러까지 가지 않음
                .contains("data-ui-submit", "data-ui-nav")
                .contains("pageshow", "aria-busy", "aria-live")
                .doesNotContain("innerHTML");               // XSS 안전 (textContent 만 사용)
    }

    // ===================================================================== util

    static String token(String css, String name) {
        int i = css.indexOf(name + ":");
        assertThat(i).as("토큰 존재: " + name).isGreaterThanOrEqualTo(0);
        int s = css.indexOf('#', i);
        return css.substring(s, s + 7);
    }

    /** WCAG 2.x 상대 명도 대비 */
    static double contrast(String hexA, String hexB) {
        double a = luminance(hexA), b = luminance(hexB);
        return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
    }

    static double luminance(String hex) {
        int v = Integer.parseInt(hex.substring(1), 16);
        double[] c = {(v >> 16 & 0xff) / 255.0, (v >> 8 & 0xff) / 255.0, (v & 0xff) / 255.0};
        for (int i = 0; i < 3; i++) {
            c[i] = c[i] <= 0.03928 ? c[i] / 12.92 : Math.pow((c[i] + 0.055) / 1.055, 2.4);
        }
        return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2];
    }

    /** 패키지 private 상수 접근용 */
    static final class UiComponentSampleControllerAccess {
        static long maxDelay() throws Exception {
            java.lang.reflect.Field f = UiComponentSampleController.class.getDeclaredField("MAX_DEMO_DELAY_MS");
            f.setAccessible(true);
            return f.getLong(null);
        }
    }
}
