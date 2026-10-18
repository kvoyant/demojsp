/**
 * =============================================================================
 *  [공통#51] 저축 - 메인 콘텐츠  -  /js/common/savings-main.js
 * -----------------------------------------------------------------------------
 *  [방향] 계좌 미보유 고객에게 계좌개설 프로세스 제공   [목적] 입출금통장 개설 촉진
 *  [정책] 1. 제공대상: 계좌를 보유하지 않은 모든 사용자   2. 로그인 전/후 동일 화면
 *  [기능] 1. 구조: 텍스트 + 이미지 + 버튼   2. "매일 쌓이는 이자 / 모으는 재미가 쏠쏠"
 *         3. [계좌 개설하기] 하단 상시 노출, 터치 시 입출금통장 상품안내페이지
 *
 *  사용법
 *    <link rel="stylesheet" href="/css/common/ui-tokens.css">
 *    <link rel="stylesheet" href="/css/common/ui-button.css">
 *    <link rel="stylesheet" href="/css/common/savings-main.css">
 *    <script src="/js/common/ui-button.js"></script>          (press 효과·"이동 중" 표시)
 *    <script src="/js/common/savings-main.js"></script>
 *
 *    <div id="savingsMain"></div>             ← 저축 탭 본문 (스크롤 영역)
 *    <div id="savingsCta"></div>              ← 본문 밖 하단 (탭바 바로 위) - 스크롤해도 항상 보임
 *    SavingsMain.mount('#savingsMain', { cta: '#savingsCta', onIneligible: function () { ...계좌정보 표시... } });
 *
 *  옵션: cta (버튼 자리, 생략 시 본문 끝에 sticky), params (local 샘플), onNavigate(url) (APP 네이티브 화면전환),
 *        onLoad(data), onIneligible(data) (계좌 보유 고객), onError(err)
 *  - 문구는 textContent 로만 출력 (XSS 안전), 템플릿 문자열 미사용 (JSP EL 충돌 방지)
 * =============================================================================
 */
(function (global) {
    'use strict';

    function toEl(t) { return typeof t === 'string' ? document.querySelector(t) : t; }
    function el(tag, cls, text) { var e = document.createElement(tag); if (cls) e.className = cls; if (text != null) e.textContent = text; return e; }
    function query(params) {
        var q = [];
        Object.keys(params || {}).forEach(function (k) { if (params[k]) q.push(encodeURIComponent(k) + '=' + encodeURIComponent(params[k])); });
        return q.length ? '?' + q.join('&') : '';
    }

    function clear(host, ctaHost) {
        host.textContent = ''; host.hidden = true;
        if (ctaHost) { ctaHost.textContent = ''; ctaHost.hidden = true; }
    }

    function render(host, ctaHost, d, opts) {
        clear(host, ctaHost);
        if (!d || !d.eligible) return;

        // 기능1·2: 텍스트 + 이미지
        var sec = el('section', 'ui-svm');
        sec.setAttribute('aria-labelledby', 'ui-svm-title');
        var h = el('h2', 'ui-svm__title');
        h.id = 'ui-svm-title';
        d.lines.forEach(function (line, i) {
            if (i) h.appendChild(el('br'));
            h.appendChild(el('span', 'ui-svm__line', line));
        });
        sec.appendChild(h);
        if (d.imageUrl) {
            var img = el('img', 'ui-svm__img');
            img.src = d.imageUrl; img.alt = d.imageAlt || ''; img.decoding = 'async';
            img.addEventListener('error', function () { img.remove(); }, { once: true });
            sec.appendChild(img);
        }
        host.appendChild(sec);
        host.hidden = false;

        // 기능3: 하단 상시 노출 버튼 (ctaHost 가 없으면 본문 끝 sticky)
        var bar = el('div', 'ui-cta-bar ui-svm__cta');
        var btn = el('a', 'ui-btn ui-btn--primary ui-btn--lg ui-btn--block', d.ctaText);
        btn.href = d.ctaUrl;
        btn.setAttribute('data-busy-text', '이동 중');
        if (d.productName) btn.setAttribute('aria-label', d.ctaText + ' - ' + d.productName + ' 상품안내');
        btn.addEventListener('click', function (e) {
            e.preventDefault();
            var go = typeof opts.onNavigate === 'function' ? opts.onNavigate : function (u) { global.location.assign(u); };
            if (global.UiButton) global.UiButton.navigate(btn, d.ctaUrl, { go: go });   // press·"이동 중" (공통#40)
            else go(d.ctaUrl);
        });
        bar.appendChild(btn);
        (ctaHost || host).appendChild(bar);
        if (ctaHost) ctaHost.hidden = false;
    }

    function mount(target, opts) {
        var host = toEl(target);
        opts = opts || {};
        var ctaHost = opts.cta ? toEl(opts.cta) : null;
        clear(host, ctaHost);
        return fetch('/api/common/savings/main' + query(opts.params), {
            credentials: 'same-origin', headers: { 'X-Requested-With': 'XMLHttpRequest', 'Accept': 'application/json' }
        }).then(function (r) { return r.json(); }).then(function (res) {
            if (!res || !res.success) throw Object.assign(new Error((res && res.message) || '저축 메인 조회 실패'), { response: res });
            render(host, ctaHost, res.data, opts);
            if (typeof opts.onLoad === 'function') opts.onLoad(res.data);
            if (!res.data.eligible && typeof opts.onIneligible === 'function') opts.onIneligible(res.data);
            return res.data;
        }).catch(function (e) {
            clear(host, ctaHost);
            if (typeof opts.onError === 'function') opts.onError(e);
            return null;
        });
    }

    global.SavingsMain = { mount: mount };
})(window);
