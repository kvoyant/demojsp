/**
 * =============================================================================
 *  [CF-UI-01 / 공통#40] Button 공통 컴포넌트  -  /js/common/ui-button.js
 * -----------------------------------------------------------------------------
 *  CSS(/css/common/ui-button.css) 만으로도 버튼 모양·press 효과는 동작한다.
 *  이 스크립트는 다음을 보완한다.
 *    1) 터치 기기 press 보장   : iOS Safari 등은 :active 가 늦거나 안 걸림 → pointerdown 즉시 .is-pressed
 *    2) 누른 위치 물결(ripple) : 손가락이 닿은 지점에서 퍼지는 피드백
 *    3) 진행 중(busy) 상태     : "페이지 이동 중 / 업무 실행 중" 을 스피너 + 문구 + 진행 막대로 표시
 *    4) 중복 실행 차단         : busy 버튼의 클릭·폼 재제출을 막음 (이중 신청·이중 이체 방지)
 *
 *  사용법
 *    <link rel="stylesheet" href="/css/common/ui-tokens.css">
 *    <link rel="stylesheet" href="/css/common/ui-button.css">
 *    <script src="/js/common/ui-button.js"></script>
 *
 *    (1) 업무 실행 - Promise 가 끝날 때까지 진행 중 표시
 *        UiButton.run(btn, function () { return fetch('/api/...'); }, { text: '신청 중' });
 *    (2) 페이지 이동 - 이동 중 표시 + 화면 상단 진행 막대
 *        <a class="ui-btn ui-btn--primary" href="/loan/apply" data-ui-nav data-busy-text="이동 중">대출 신청하기</a>
 *        UiButton.navigate(btn, '/loan/apply');
 *    (3) 폼 제출 - 제출 버튼 진행 중 + 재제출 차단
 *        <form data-ui-submit> ... <button type="submit" class="ui-btn">가입하기</button></form>
 *    (4) 직접 제어
 *        UiButton.busy(btn, { text: '확인 중' });  ...  UiButton.done(btn);
 *
 *  - jQuery / Bootstrap 비의존, 문구는 textContent 로만 출력 (XSS 안전)
 *  - 스크린리더: busy 시작 시 "처리 중입니다" 안내(aria-live), 버튼에 aria-busy="true"
 *  - 뒤로가기(bfcache) 복귀 시 busy 자동 해제, timeout 경과 시 자동 해제(기본 30초)
 * =============================================================================
 */
(function (global) {
    'use strict';

    var SELECTOR = '.ui-btn';
    var DEFAULT_BUSY_TEXT = '처리 중';
    var DEFAULT_NAV_TEXT = '이동 중';
    var DEFAULT_TIMEOUT = 30000;      // 응답이 끝내 오지 않아도 버튼이 영원히 잠기지 않도록
    var MIN_BUSY_MS = 400;            // 너무 빨리 끝나면 깜빡임만 보이므로 최소 표시시간 (사용자가 "눌렸다" 인지)

    var state = typeof WeakMap === 'function' ? new WeakMap() : null;
    var liveRegion = null;
    var navBar = null;

    // ------------------------------------------------------------------ util
    function toEl(target) {
        if (!target) return null;
        if (typeof target === 'string') return document.querySelector(target);
        return target.nodeType === 1 ? target : null;
    }
    function closestBtn(node) {
        while (node && node.nodeType === 1) {
            if (node.classList && node.classList.contains('ui-btn')) return node;
            node = node.parentNode;
        }
        return null;
    }
    function isUnavailable(btn) {
        return btn.disabled || btn.getAttribute('aria-disabled') === 'true';
    }
    function reducedMotion() {
        return !!(global.matchMedia && global.matchMedia('(prefers-reduced-motion: reduce)').matches);
    }
    function announce(text) {
        if (!liveRegion) {
            liveRegion = document.createElement('div');
            liveRegion.setAttribute('aria-live', 'polite');
            liveRegion.setAttribute('role', 'status');
            liveRegion.style.cssText = 'position:absolute;width:1px;height:1px;margin:-1px;padding:0;overflow:hidden;clip:rect(0,0,0,0);border:0;white-space:nowrap;';
            document.body.appendChild(liveRegion);
        }
        liveRegion.textContent = '';
        // 같은 문구 연속 안내를 위해 다음 tick 에 설정
        setTimeout(function () { liveRegion.textContent = text; }, 30);
    }

    // ------------------------------------------------------------------ press (터치 / 마우스 / 키보드)
    function press(btn, ev) {
        if (isUnavailable(btn) || btn.classList.contains('is-busy')) return;
        btn.classList.add('is-pressed');
        if (ev && ev.clientX != null && !reducedMotion() && btn.getAttribute('data-ripple') !== 'false') {
            ripple(btn, ev.clientX, ev.clientY);
        }
    }
    function release(btn) {
        if (btn) btn.classList.remove('is-pressed');
    }
    function ripple(btn, x, y) {
        var rect = btn.getBoundingClientRect();
        var size = Math.max(rect.width, rect.height) * 2;
        var dot = document.createElement('span');
        dot.className = 'ui-btn__ripple';
        dot.setAttribute('aria-hidden', 'true');
        dot.style.width = dot.style.height = size + 'px';
        dot.style.left = (x - rect.left - size / 2) + 'px';
        dot.style.top = (y - rect.top - size / 2) + 'px';
        btn.appendChild(dot);
        setTimeout(function () { if (dot.parentNode) dot.parentNode.removeChild(dot); }, 520);
    }

    var pressed = null;
    function onPointerDown(e) {
        if (e.button && e.button !== 0) return;
        var btn = closestBtn(e.target);
        if (!btn) return;
        pressed = btn;
        press(btn, e);
    }
    function onPointerEnd(e) {
        // pointerleave 는 버튼 안의 아이콘/문구에서도 발생 → 버튼 자체를 벗어날 때만 해제
        if (e && e.type === 'pointerleave' && e.target !== pressed) return;
        if (pressed) { release(pressed); pressed = null; }
    }
    function onKeyDown(e) {
        if (e.key !== ' ' && e.key !== 'Enter') return;
        var btn = closestBtn(e.target);
        if (btn && !e.repeat) press(btn, null);
    }
    function onKeyUp(e) {
        if (e.key !== ' ' && e.key !== 'Enter') return;
        release(closestBtn(e.target));
    }

    // ------------------------------------------------------------------ busy
    /**
     * 진행 중 상태로 전환.
     * @param target  버튼 element 또는 selector
     * @param opts    { text: 진행 중 문구(기본 data-busy-text 또는 "처리 중"), timeout: 자동해제 ms(0=해제안함), announce: 스크린리더 문구 }
     * @return true = 전환됨, false = 이미 진행 중이거나 버튼 없음
     */
    function busy(target, opts) {
        var btn = toEl(target);
        if (!btn || btn.classList.contains('is-busy')) return false;
        opts = opts || {};
        var text = opts.text || btn.getAttribute('data-busy-text') || DEFAULT_BUSY_TEXT;

        // 원래 내용 보관 (DOM 노드 그대로 보관 - HTML 문자열 재파싱 없음)
        var saved = { nodes: [], minWidth: btn.style.minWidth, startedAt: Date.now(), timer: null };
        while (btn.firstChild) saved.nodes.push(btn.removeChild(btn.firstChild));

        // 문구가 짧아져도 버튼 폭이 줄지 않게 (레이아웃 흔들림 방지)
        btn.style.minWidth = btn.getBoundingClientRect().width + 'px';

        var spinner = document.createElement('span');
        spinner.className = 'ui-btn__spinner';
        spinner.setAttribute('aria-hidden', 'true');
        var label = document.createElement('span');
        label.className = 'ui-btn__label';
        label.textContent = text;
        btn.appendChild(spinner);
        btn.appendChild(label);

        btn.classList.remove('is-pressed');
        btn.classList.add('is-busy');
        btn.setAttribute('aria-busy', 'true');
        if (btn.tagName === 'A') btn.setAttribute('aria-disabled', 'true');

        var timeout = opts.timeout == null ? DEFAULT_TIMEOUT : opts.timeout;
        if (timeout > 0) {
            saved.timer = setTimeout(function () {
                done(btn);
                if (typeof opts.onTimeout === 'function') opts.onTimeout(btn);
            }, timeout);
        }
        if (state) state.set(btn, saved); else btn.__uiBtnState = saved;
        announce(opts.announce || (text + '입니다. 잠시만 기다려 주세요.'));
        return true;
    }

    /** 진행 중 해제 → 원래 문구/상태로 복원 */
    function done(target) {
        var btn = toEl(target);
        if (!btn) return;
        var saved = state ? state.get(btn) : btn.__uiBtnState;
        if (!saved) return;
        if (saved.timer) clearTimeout(saved.timer);
        while (btn.firstChild) btn.removeChild(btn.firstChild);
        for (var i = 0; i < saved.nodes.length; i++) btn.appendChild(saved.nodes[i]);
        btn.style.minWidth = saved.minWidth;
        btn.classList.remove('is-busy', 'is-pressed');
        btn.removeAttribute('aria-busy');
        if (btn.tagName === 'A') btn.removeAttribute('aria-disabled');
        if (state) state['delete'](btn); else btn.__uiBtnState = null;
    }

    function isBusy(target) {
        var btn = toEl(target);
        return !!(btn && btn.classList.contains('is-busy'));
    }

    /**
     * 업무 실행: fn 이 반환한 Promise(또는 값)가 끝날 때까지 진행 중 표시. 진행 중 재클릭은 무시.
     * @return Promise (fn 결과). 이미 진행 중이면 Promise.reject({ code: 'UI_BTN_BUSY' })
     */
    function run(target, fn, opts) {
        var btn = toEl(target);
        if (!busy(btn, opts)) {
            return Promise.reject({ code: 'UI_BTN_BUSY', message: '이미 처리 중입니다.' });
        }
        var saved = state ? state.get(btn) : btn.__uiBtnState;
        var minMs = (opts && opts.minDuration != null) ? opts.minDuration : MIN_BUSY_MS;
        function finish() {
            var rest = minMs - (Date.now() - saved.startedAt);
            return new Promise(function (resolve) {
                setTimeout(function () { done(btn); resolve(); }, rest > 0 ? rest : 0);
            });
        }
        var p;
        try { p = Promise.resolve(fn(btn)); } catch (e) { p = Promise.reject(e); }
        return p.then(function (v) { return finish().then(function () { return v; }); },
                      function (err) { return finish().then(function () { throw err; }); });
    }

    // ------------------------------------------------------------------ navigate
    function showNavBar() {
        if (navBar) return;
        navBar = document.createElement('div');
        navBar.className = 'ui-nav-progress';
        navBar.setAttribute('aria-hidden', 'true');
        document.body.appendChild(navBar);
    }
    function hideNavBar() {
        if (navBar && navBar.parentNode) navBar.parentNode.removeChild(navBar);
        navBar = null;
    }

    /**
     * 페이지 이동: 버튼 "이동 중" + 화면 상단 진행 막대 표시 후 이동.
     * @param opts { text, timeout, go: function(url) - APP 네이티브 화면전환 브릿지 등 (기본 location.assign) }
     */
    function navigate(target, url, opts) {
        var btn = toEl(target);
        opts = opts || {};
        url = url || (btn && (btn.getAttribute('data-ui-nav') || btn.getAttribute('href')));
        if (!url || isBusy(btn)) return false;
        busy(btn, {
            text: opts.text || (btn && btn.getAttribute('data-busy-text')) || DEFAULT_NAV_TEXT,
            timeout: opts.timeout == null ? 15000 : opts.timeout,
            announce: '페이지로 이동 중입니다.',
            onTimeout: hideNavBar
        });
        showNavBar();
        // 진행 중 화면이 그려진 다음 이동 (요구사항: 이동 중이라는 인식)
        setTimeout(function () {
            if (typeof opts.go === 'function') opts.go(url);
            else global.location.assign(url);
        }, 60);
        return true;
    }

    // ------------------------------------------------------------------ 선언형 바인딩 (data-ui-nav / form[data-ui-submit])
    function onClickCapture(e) {
        var btn = closestBtn(e.target);
        if (!btn) return;
        if (btn.classList.contains('is-busy') || btn.getAttribute('aria-disabled') === 'true') {
            e.preventDefault();
            e.stopImmediatePropagation();     // 업무 화면 click 핸들러까지 막는다 (중복 실행 차단)
            return;
        }
        if (btn.hasAttribute('data-ui-nav')) {
            // 새 탭 열기(ctrl/cmd/shift 클릭)는 브라우저 기본 동작 유지
            if (e.ctrlKey || e.metaKey || e.shiftKey || btn.getAttribute('target') === '_blank') return;
            e.preventDefault();
            navigate(btn);
        }
    }
    function onSubmitCapture(e) {
        var form = e.target;
        if (!form || !form.hasAttribute || !form.hasAttribute('data-ui-submit')) return;
        if (form.getAttribute('data-ui-submitting') === 'Y') {
            e.preventDefault();
            e.stopImmediatePropagation();
            return;
        }
        var btn = (e.submitter && closestBtn(e.submitter)) || form.querySelector('.ui-btn[type="submit"], button.ui-btn:not([type])');
        // 화면의 submit 핸들러가 검증 실패로 preventDefault 할 수 있으므로 다음 tick 에 판단
        setTimeout(function () {
            if (e.defaultPrevented) return;
            form.setAttribute('data-ui-submitting', 'Y');
            if (btn) busy(btn, { text: btn.getAttribute('data-busy-text') || '처리 중' });
        }, 0);
    }
    function onPageShow(e) {
        // 뒤로가기로 bfcache 복원된 화면에 "이동 중" 이 남아 있지 않게
        if (!e.persisted) return;
        hideNavBar();
        var list = document.querySelectorAll(SELECTOR + '.is-busy');
        for (var i = 0; i < list.length; i++) done(list[i]);
        var forms = document.querySelectorAll('form[data-ui-submitting]');
        for (var j = 0; j < forms.length; j++) forms[j].removeAttribute('data-ui-submitting');
    }

    var bound = false;
    function init() {
        if (bound) return;
        bound = true;
        var hasPointer = 'PointerEvent' in global;
        document.addEventListener(hasPointer ? 'pointerdown' : 'touchstart', onPointerDown, { passive: true });
        if (!hasPointer) document.addEventListener('mousedown', onPointerDown);
        ['pointerup', 'pointercancel', 'pointerleave', 'touchend', 'touchcancel', 'mouseup', 'dragstart'].forEach(function (t) {
            document.addEventListener(t, onPointerEnd, true);
        });
        document.addEventListener('scroll', onPointerEnd, { passive: true, capture: true });   // 스크롤하면 press 해제
        document.addEventListener('keydown', onKeyDown);
        document.addEventListener('keyup', onKeyUp);
        document.addEventListener('click', onClickCapture, true);
        document.addEventListener('submit', onSubmitCapture, true);
        global.addEventListener('pageshow', onPageShow);
        global.addEventListener('blur', onPointerEnd);
    }

    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init);
    else init();

    global.UiButton = {
        init: init,
        busy: busy,
        done: done,
        isBusy: isBusy,
        run: run,
        navigate: navigate,
        /** 테스트·문서용 상수 */
        DEFAULTS: { busyText: DEFAULT_BUSY_TEXT, navText: DEFAULT_NAV_TEXT, timeout: DEFAULT_TIMEOUT, minBusyMs: MIN_BUSY_MS }
    };
})(window);
