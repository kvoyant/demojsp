/**
 * =============================================================================
 *  [공통#46] 홈 퀵메뉴 · 전체메뉴  -  /js/common/quick-menu.js
 * -----------------------------------------------------------------------------
 *  [방향] 회원구분 별 개인화 메뉴 노출   [목적] 기존 핫키 UI/UX 개선 및 활용성 강화
 *  [기능] 1. 6개 노출 3*2 배열   2. 구조: 이미지 + 메뉴명   3. 하드코딩 6종 (서버 yaml 고정)
 *         4. 전체메뉴 > 상품 > 전체상품(신규) · 예금상품 · 대출상품 · 금융계산기
 *
 *  사용법
 *    <link rel="stylesheet" href="/css/common/ui-tokens.css">
 *    <link rel="stylesheet" href="/css/common/ui-icon.css">
 *    <link rel="stylesheet" href="/css/common/quick-menu.css">
 *    <script src="/js/common/quick-menu.js"></script>
 *
 *    QuickMenu.mount('#quickMenu');            홈 퀵메뉴 (회원구분은 서버가 로그인 세션으로 판단)
 *    QuickMenu.mountAll('#allMenu');           전체메뉴
 *
 *  옵션: params (local 샘플: { demoCustomer: 'customer01' }), onNavigate(url, item) (APP 네이티브 화면전환 브릿지, 기본 링크 이동),
 *        onLoad(data), onError(err)
 *  - 링크(<a>)로 그려 키보드·스크린리더로 쓸 수 있고, 로그인 필요 메뉴는 "로그인 필요" 를 함께 읽어 준다
 *  - 문구는 textContent 로만 출력 (XSS 안전), JSP EL 충돌 방지를 위해 템플릿 문자열 미사용
 * =============================================================================
 */
(function (global) {
    'use strict';

    var SPRITE = '/images/common/ui-icons.svg';
    var SVG_NS = 'http://www.w3.org/2000/svg';
    var ICON_NAME = /^[a-z0-9-]{1,40}$/;

    function toEl(t) { return typeof t === 'string' ? document.querySelector(t) : t; }
    function el(tag, cls, text) { var e = document.createElement(tag); if (cls) e.className = cls; if (text != null) e.textContent = text; return e; }
    function query(params) {
        var q = [];
        Object.keys(params || {}).forEach(function (k) { if (params[k]) q.push(encodeURIComponent(k) + '=' + encodeURIComponent(params[k])); });
        return q.length ? '?' + q.join('&') : '';
    }
    function get(url) {
        return fetch(url, { credentials: 'same-origin', headers: { 'X-Requested-With': 'XMLHttpRequest', 'Accept': 'application/json' } })
            .then(function (r) { return r.json(); })
            .then(function (res) { if (!res || !res.success) throw Object.assign(new Error((res && res.message) || '메뉴 조회 실패'), { response: res }); return res.data; });
    }
    function icon(name) {
        var svg = document.createElementNS(SVG_NS, 'svg');
        svg.setAttribute('class', 'ui-icon ui-qm__icon');
        svg.setAttribute('aria-hidden', 'true');
        svg.setAttribute('focusable', 'false');
        var use = document.createElementNS(SVG_NS, 'use');
        use.setAttribute('href', SPRITE + '#ic-' + (ICON_NAME.test(name || '') ? name : 'products'));
        svg.appendChild(use);
        return svg;
    }
    function link(cls, url, item, opts) {
        var a = el('a', cls);
        a.href = url;
        a.addEventListener('click', function (e) {
            if (typeof opts.onNavigate === 'function') { e.preventDefault(); opts.onNavigate(url, item); }
        });
        return a;
    }

    /** 홈 퀵메뉴 그리기 (data = /api/common/menus/quick 응답) */
    function render(host, data, opts) {
        host.textContent = '';
        var nav = el('nav', 'ui-qm');
        nav.setAttribute('aria-label', '퀵메뉴');
        nav.setAttribute('data-segment', data.segment);
        var ul = el('ul', 'ui-qm__grid');
        ul.style.setProperty('--qm-cols', String(data.columns || 3));
        data.items.forEach(function (item) {
            var li = el('li', 'ui-qm__cell');
            var a = link('ui-qm__item', item.url, item, opts);
            var tile = el('span', 'ui-qm__tile');
            tile.appendChild(icon(item.icon));
            a.appendChild(tile);
            a.appendChild(el('span', 'ui-qm__name', item.name));
            if (item.loginFirst) {
                a.classList.add('is-login-first');
                var sr = el('span', 'ui-qm__sr', ' (로그인 필요)');
                a.appendChild(sr);
            }
            li.appendChild(a);
            ul.appendChild(li);
        });
        nav.appendChild(ul);
        host.appendChild(nav);
        host.hidden = data.items.length === 0;
    }

    /** 전체메뉴 그리기 (data = /api/common/menus/all 응답) */
    function renderAll(host, data, opts) {
        host.textContent = '';
        var wrap = el('nav', 'ui-am');
        wrap.setAttribute('aria-label', '전체메뉴');
        data.categories.forEach(function (c) {
            var sec = el('section', 'ui-am__cat');
            var h = el('h3', 'ui-am__title', c.name);
            h.id = 'ui-am-' + c.code;
            sec.setAttribute('aria-labelledby', h.id);
            sec.appendChild(h);
            var ul = el('ul', 'ui-am__list');
            c.menus.forEach(function (m) {
                var li = el('li');
                var a = link('ui-am__item', m.url, m, opts);
                a.appendChild(el('span', null, m.name));
                if (m.newBadge) {
                    var n = el('span', 'ui-am__new', 'N');
                    n.setAttribute('aria-label', '신규');
                    a.appendChild(n);
                }
                li.appendChild(a);
                ul.appendChild(li);
            });
            sec.appendChild(ul);
            wrap.appendChild(sec);
        });
        host.appendChild(wrap);
    }

    function mount(target, opts) {
        var host = toEl(target);
        opts = opts || {};
        return get('/api/common/menus/quick' + query(opts.params)).then(function (d) {
            render(host, d, opts);
            if (typeof opts.onLoad === 'function') opts.onLoad(d);
            return d;
        }).catch(function (e) { if (typeof opts.onError === 'function') opts.onError(e); return null; });
    }

    function mountAll(target, opts) {
        var host = toEl(target);
        opts = opts || {};
        return get('/api/common/menus/all').then(function (d) {
            renderAll(host, d, opts);
            if (typeof opts.onLoad === 'function') opts.onLoad(d);
            return d;
        }).catch(function (e) { if (typeof opts.onError === 'function') opts.onError(e); return null; });
    }

    global.QuickMenu = { mount: mount, mountAll: mountAll, render: function (t, d, o) { render(toEl(t), d, o || {}); } };
})(window);
