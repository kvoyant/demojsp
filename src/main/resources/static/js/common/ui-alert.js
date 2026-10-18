/**
 * =============================================================================
 *  [CF-UI-01 / 공통#42] Alert 공통 컴포넌트  -  /js/common/ui-alert.js
 * -----------------------------------------------------------------------------
 *  브라우저 기본 alert()/confirm() 대신 사용하는 공통 알럿 (1Btn / 2Btn).
 *    - 요구 1: 4~5줄 이상 내용의 가독성 (16px · 줄간격 1.65 · 3줄 이상 왼쪽 정렬 · 문단/목록 · 본문만 스크롤)
 *    - 요구 2: 화면 크기·해상도별 반응형 (폭 단계, dvh, 가로모드, 버튼 세로 자동 전환, 글자 확대)
 *
 *  사용법
 *    <script src="/js/common/ui-alert.js"></script>     (CSS 3종은 없으면 자동으로 불러온다)
 *
 *    UiAlert.alert('비밀번호가 변경되었습니다.');                                 → Promise<void>
 *    UiAlert.alert('본인인증에 실패했습니다.', { type: 'error', title: '인증 실패' });
 *    UiAlert.confirm('대출 신청을 취소하시겠습니까?').then(function (ok) { if (ok) { ... } });   → Promise<boolean>
 *    UiAlert.confirm('해지하면 되돌릴 수 없습니다.', { okText: '해지하기', danger: true });
 *
 *    // 확인 버튼에서 업무 실행 (끝날 때까지 알럿 유지 + 버튼 "처리 중", 실패 시 알럿 유지)
 *    UiAlert.confirm('신청서를 제출할까요?', { onOk: function () { return fetch('/api/...'); } });
 *
 *  message 작성 규칙 (문자열, HTML 미지원 → XSS 안전)
 *    "\n"       줄바꿈          "\n\n"   문단 간격
 *    "- 항목"   목록 (· • 도 가능)          "※ 안내"  보조 안내 (작은 회색 글자)
 *
 *  옵션 { title, type: success|error|warning|info, okText, cancelText, danger, onOk,
 *         escClose(기본 true), dimClose(기본 false), stack(true=버튼 세로 강제), focus: 'ok'|'cancel' }
 *
 *  - 여러 개를 연속 호출하면 순서대로 하나씩 표시 (큐)
 *  - 접근성: role="alertdialog", aria-modal, 제목/본문 연결, 포커스 가두기(Tab), Esc, 닫으면 원래 위치로 포커스 복귀,
 *            배경 inert, 배경 스크롤 잠금. 위험 작업(danger)은 기본 포커스를 [취소]에 둔다.
 *  - jQuery / Bootstrap 비의존
 * =============================================================================
 */
(function (global) {
    'use strict';

    var CSS_FILES = ['/css/common/ui-tokens.css', '/css/common/ui-button.css', '/css/common/ui-alert.css'];
    var LONG_LINES = 3;          // 이 줄 수 이상이면 왼쪽 정렬
    var seq = 0;
    var queue = [];
    var current = null;

    var api = {
        /** 상태 아이콘 스프라이트 (공통#41) */
        sprite: '/images/common/ui-icons.svg',
        /** CSS 자동 로드 경로 (컨텍스트 패스가 있으면 변경) */
        cssFiles: CSS_FILES,
        DEFAULTS: { okText: '확인', cancelText: '취소', longLines: LONG_LINES }
    };

    // ------------------------------------------------------------------ CSS 자동 로드
    var cssReady = null;
    function ensureCss() {
        if (cssReady) return cssReady;
        var waits = [];
        api.cssFiles.forEach(function (href) {
            var file = href.substring(href.lastIndexOf('/') + 1);
            var links = document.querySelectorAll('link[rel="stylesheet"]');
            for (var i = 0; i < links.length; i++) {
                if ((links[i].getAttribute('href') || '').indexOf(file) >= 0) return;
            }
            var link = document.createElement('link');
            link.rel = 'stylesheet';
            link.href = href;
            waits.push(new Promise(function (resolve) {
                link.onload = link.onerror = resolve;
                setTimeout(resolve, 1500);        // 네트워크가 느려도 알럿은 띄운다
            }));
            document.head.appendChild(link);
        });
        cssReady = Promise.all(waits);
        return cssReady;
    }

    // ------------------------------------------------------------------ message → DOM (가독성)
    var BULLET = /^\s*[-·•]\s+/;
    function buildBody(message) {
        var frag = document.createDocumentFragment();
        var text = message == null ? '' : String(message).replace(/\r\n?/g, '\n');
        var blocks = text.split(/\n\s*\n/);              // 빈 줄 = 문단
        blocks.forEach(function (block) {
            var lines = block.split('\n');
            var p = null, ul = null;
            var wrap = document.createElement('div');      // 문단 묶음 (빈 줄 단위 → 문단 간격)
            wrap.className = 'ui-alert__block';
            frag.appendChild(wrap);
            lines.forEach(function (line) {
                if (BULLET.test(line)) {
                    p = null;
                    if (!ul) { ul = document.createElement('ul'); wrap.appendChild(ul); }
                    var li = document.createElement('li');
                    li.textContent = line.replace(BULLET, '');
                    ul.appendChild(li);
                    return;
                }
                ul = null;
                if (/^\s*※/.test(line)) {
                    var note = document.createElement('p');
                    note.className = 'ui-alert__note';
                    note.textContent = line.trim();
                    wrap.appendChild(note);
                    p = null;
                    return;
                }
                if (!p) { p = document.createElement('p'); wrap.appendChild(p); }
                else p.appendChild(document.createElement('br'));
                p.appendChild(document.createTextNode(line));
            });
        });
        return frag;
    }

    function svgIcon(type) {
        var NS = 'http://www.w3.org/2000/svg';
        var svg = document.createElementNS(NS, 'svg');
        svg.setAttribute('class', 'ui-alert__icon ui-alert__icon--' + type);
        svg.setAttribute('aria-hidden', 'true');     // 의미는 제목/본문 글자로 전달
        svg.setAttribute('focusable', 'false');
        var use = document.createElementNS(NS, 'use');
        var base = (global.UiIcon && global.UiIcon.inlined) ? '' : api.sprite;
        use.setAttribute('href', base + '#ic-status-' + type + '-fill');
        use.setAttributeNS('http://www.w3.org/1999/xlink', 'xlink:href', base + '#ic-status-' + type + '-fill');
        svg.appendChild(use);
        return svg;
    }

    // ------------------------------------------------------------------ layout 판단 (줄 수 / 버튼 세로 전환)
    function relayout(st) {
        var dlg = st.dialog, body = st.body;
        // 1) 줄 수: 가운데 정렬 상태에서 측정
        dlg.classList.remove('is-long');
        var lh = parseFloat(getComputedStyle(body).lineHeight) || 26;
        var contentH = 0;
        for (var i = 0; i < body.children.length; i++) contentH += body.children[i].getBoundingClientRect().height;
        var lines = Math.round(contentH / lh);
        st.lines = lines;
        if (lines >= api.DEFAULTS.longLines || body.querySelector('ul')) dlg.classList.add('is-long');

        // 2) 본문 스크롤 여부 → 키보드로도 스크롤할 수 있게 포커스 허용
        if (body.scrollHeight > body.clientHeight + 1) {
            body.setAttribute('tabindex', '0');
            st.scrollable = true;
        } else {
            body.removeAttribute('tabindex');
            st.scrollable = false;
        }

        // 3) 2Btn: 문구가 두 줄로 접히면 세로 배치
        if (st.opts.stack) { dlg.classList.add('is-stack'); return; }
        dlg.classList.remove('is-stack');
        if (st.buttons.length > 1) {
            for (var j = 0; j < st.buttons.length; j++) {
                var b = st.buttons[j];
                var lineH = parseFloat(getComputedStyle(b).lineHeight) || 22;
                var label = b.firstElementChild || b;
                if (label.getBoundingClientRect().height > lineH * 1.5 || b.scrollWidth > b.clientWidth + 1) {
                    dlg.classList.add('is-stack');
                    break;
                }
            }
        }
    }

    // ------------------------------------------------------------------ open / close
    function open(opts) {
        return new Promise(function (resolve) {
            queue.push({ opts: opts || {}, resolve: resolve });
            if (!current) next();
        });
    }

    function next() {
        var item = queue.shift();
        if (!item) return;
        // CSS 로드(비동기) 동안 다른 호출이 끼어들어 알럿이 겹치지 않도록 자리를 먼저 잡는다
        current = { pending: true };
        ensureCss().then(function () { render(item); });
    }

    function render(item) {
        var opts = item.opts;
        var id = 'ui-alert-' + (++seq);
        var st = { opts: opts, resolve: item.resolve, buttons: [], returnFocus: document.activeElement, inerted: [] };
        current = st;

        var dim = document.createElement('div');
        dim.className = 'ui-alert-dim';
        var dlg = document.createElement('div');
        dlg.className = 'ui-alert';
        dlg.setAttribute('role', 'alertdialog');
        dlg.setAttribute('aria-modal', 'true');
        dlg.setAttribute('aria-labelledby', id + '-title');
        dlg.setAttribute('aria-describedby', id + '-body');
        dlg.setAttribute('tabindex', '-1');

        var head = document.createElement('div');
        head.className = 'ui-alert__head';
        if (opts.type && /^(success|error|warning|info)$/.test(opts.type)) head.appendChild(svgIcon(opts.type));
        var title = document.createElement('h2');
        title.id = id + '-title';
        title.className = 'ui-alert__title';
        if (opts.title) title.textContent = opts.title;
        else { title.textContent = opts.kind === 'confirm' ? '확인' : '알림'; title.classList.add('is-sr'); }
        head.appendChild(title);
        if (!opts.title && !opts.type) head.classList.add('is-empty');

        var body = document.createElement('div');
        body.id = id + '-body';
        body.className = 'ui-alert__body';
        body.appendChild(buildBody(opts.message));

        var foot = document.createElement('div');
        foot.className = 'ui-alert__foot';
        (opts.buttons || []).forEach(function (bd) {
            var b = document.createElement('button');
            b.type = 'button';
            b.className = 'ui-btn ' + (bd.variant || 'ui-btn--primary');
            var label = document.createElement('span');
            label.textContent = bd.text;
            b.appendChild(label);
            b.setAttribute('data-value', String(bd.value));
            b.addEventListener('click', function () { onButton(st, b, bd); });
            foot.appendChild(b);
            st.buttons.push(b);
        });

        dlg.appendChild(head);
        dlg.appendChild(body);
        dlg.appendChild(foot);
        dim.appendChild(dlg);
        st.dim = dim; st.dialog = dlg; st.body = body;

        // 배경: inert + 스크롤 잠금
        var kids = document.body.children;
        for (var i = 0; i < kids.length; i++) {
            var k = kids[i];
            if (k.tagName === 'SCRIPT' || k.hasAttribute('inert') || k.getAttribute('aria-live')) continue;
            k.setAttribute('inert', '');
            k.setAttribute('aria-hidden', 'true');
            st.inerted.push(k);
        }
        document.documentElement.classList.add('ui-alert-lock');
        document.body.appendChild(dim);

        relayout(st);
        st.onResize = function () { relayout(st); };
        global.addEventListener('resize', st.onResize);
        // 글자 크기 변경·웹폰트 로드 등 창 크기 변화 없이 내용 높이가 바뀌는 경우도 다시 판단
        if (typeof ResizeObserver === 'function') {
            st.ro = new ResizeObserver(function () { relayout(st); });
            st.ro.observe(dlg);
        }
        st.onKey = function (e) { onKey(st, e); };
        document.addEventListener('keydown', st.onKey, true);
        dim.addEventListener('click', function (e) {
            if (e.target === dim && opts.dimClose) close(st, opts.cancelValue);
        });

        // 기본 포커스: danger 확인창은 [취소], 그 외 주 버튼
        var focusBtn = st.buttons[st.buttons.length - 1];
        if (st.buttons.length > 1 && (opts.focus === 'cancel' || (opts.danger && opts.focus !== 'ok'))) focusBtn = st.buttons[0];
        requestAnimationFrame(function () {
            dim.classList.add('is-open');
            (focusBtn || dlg).focus();
        });
    }

    function onButton(st, btn, bd) {
        if (st.closing) return;
        if (typeof bd.action === 'function') {
            var runner = global.UiButton
                ? global.UiButton.run(btn, bd.action, { text: bd.busyText || '처리 중' })
                : Promise.resolve().then(bd.action);
            st.working = true;
            st.buttons.forEach(function (b) { if (b !== btn) b.setAttribute('aria-disabled', 'true'); });
            runner.then(function (result) {
                st.working = false;
                close(st, bd.value, result);
            }, function (err) {
                // 실패: 알럿 유지 (화면에서 오류 안내), 버튼 원복
                st.working = false;
                st.buttons.forEach(function (b) { b.removeAttribute('aria-disabled'); });
                if (err && err.code === 'UI_BTN_BUSY') return;
                if (typeof st.opts.onError === 'function') st.opts.onError(err);
            });
            return;
        }
        close(st, bd.value);
    }

    function onKey(st, e) {
        if (e.key === 'Escape' || e.key === 'Esc') {
            e.preventDefault();
            e.stopPropagation();
            if (st.opts.escClose !== false && !st.working) close(st, st.opts.cancelValue);
            return;
        }
        if (e.key !== 'Tab') return;
        // 포커스 가두기
        var f = [];
        if (st.scrollable) f.push(st.body);
        st.buttons.forEach(function (b) { f.push(b); });
        if (!f.length) { e.preventDefault(); return; }
        var idx = f.indexOf(document.activeElement);
        e.preventDefault();
        var n = e.shiftKey ? (idx <= 0 ? f.length - 1 : idx - 1) : (idx === f.length - 1 ? 0 : idx + 1);
        f[n].focus();
    }

    function close(st, value) {
        if (st.closing) return;
        st.closing = true;
        global.removeEventListener('resize', st.onResize);
        if (st.ro) st.ro.disconnect();
        document.removeEventListener('keydown', st.onKey, true);
        st.inerted.forEach(function (k) { k.removeAttribute('inert'); k.removeAttribute('aria-hidden'); });
        document.documentElement.classList.remove('ui-alert-lock');
        if (st.dim.parentNode) st.dim.parentNode.removeChild(st.dim);
        if (st.returnFocus && st.returnFocus.focus && document.contains(st.returnFocus)) {
            try { st.returnFocus.focus({ preventScroll: true }); } catch (e) { st.returnFocus.focus(); }
        }
        current = null;
        st.resolve(value);
        if (queue.length) setTimeout(next, 0);
    }

    // ------------------------------------------------------------------ public
    function alert(message, opts) {
        opts = opts || {};
        return open({
            kind: 'alert', message: message, title: opts.title, type: opts.type, stack: opts.stack,
            escClose: opts.escClose, dimClose: opts.dimClose,
            buttons: [{ text: opts.okText || api.DEFAULTS.okText, value: undefined, variant: 'ui-btn--primary', action: opts.onOk }]
        }).then(function () { return undefined; });
    }

    function confirm(message, opts) {
        opts = opts || {};
        return open({
            kind: 'confirm', message: message, title: opts.title, type: opts.type, stack: opts.stack,
            escClose: opts.escClose, dimClose: opts.dimClose, danger: opts.danger, focus: opts.focus, onError: opts.onError,
            cancelValue: false,
            buttons: [
                { text: opts.cancelText || api.DEFAULTS.cancelText, value: false, variant: 'ui-btn--outline' },
                { text: opts.okText || api.DEFAULTS.okText, value: true, variant: opts.danger ? 'ui-btn--danger' : 'ui-btn--primary',
                  action: opts.onOk, busyText: opts.busyText }
            ]
        }).then(function (v) { return v === true; });
    }

    /** 현재 열린 알럿 정보 (테스트·디버깅용) */
    function state() {
        if (!current || !current.dialog) return null;
        return {
            lines: current.lines, long: current.dialog.classList.contains('is-long'),
            stack: current.dialog.classList.contains('is-stack'), scrollable: !!current.scrollable,
            queued: queue.length
        };
    }

    api.alert = alert;
    api.confirm = confirm;
    api.open = open;
    api.state = state;
    api.isOpen = function () { return !!current; };   // 표시 중 또는 표시 준비 중

    global.UiAlert = api;
})(window);
