/**
 * =============================================================================
 *  [공통#47] 홈 추천 상품  -  /js/common/recommend-products.js
 * -----------------------------------------------------------------------------
 *  [방향] 신규상품 가입 프로세스 유입경로 제공   [목적] 여신/수신 상품정보 및 가입프로세스 연결로 유입량 확대
 *  [정책] 제공대상 금융회원 외 모든 회원 / 상품 노출순서 ADMIN 처리 (공통#48)
 *  [기능] 1. 상품목록  2. [더보기] → 전체상품목록(상품몰)  3. 상품 터치 → 상품안내페이지  4. 금융회원은 영역 전체 숨김
 *
 *  사용법
 *    <link rel="stylesheet" href="/css/common/ui-tokens.css">
 *    <link rel="stylesheet" href="/css/common/ui-icon.css">
 *    <link rel="stylesheet" href="/css/common/recommend-products.css">
 *    <script src="/js/common/recommend-products.js"></script>
 *
 *    <div id="recommend"></div>
 *    RecommendProducts.mount('#recommend');     (회원구분은 서버가 로그인 세션으로 판단)
 *
 *  옵션: params (local 샘플: { demoCustomer: 'customer01' }), title (기본 "추천 상품"),
 *        onNavigate(url, item) (APP 네이티브 화면전환, 기본 링크 이동), onLoad(data), onHidden(data), onError(err)
 *  - 금융회원 / 노출 상품 0건 / 조회 실패 → 영역 전체 hidden (홈 화면을 막지 않음)
 *  - 문구는 textContent 로만 출력 (XSS 안전), 템플릿 문자열 미사용 (JSP EL 충돌 방지)
 * =============================================================================
 */
(function (global) {
    'use strict';

    var SPRITE = '/images/common/ui-icons.svg';
    var SVG_NS = 'http://www.w3.org/2000/svg';
    var ICON = /^[a-z0-9-]{1,40}$/;
    var seq = 0;

    function toEl(t) { return typeof t === 'string' ? document.querySelector(t) : t; }
    function el(tag, cls, text) { var e = document.createElement(tag); if (cls) e.className = cls; if (text != null) e.textContent = text; return e; }
    function query(params) {
        var q = [];
        Object.keys(params || {}).forEach(function (k) { if (params[k]) q.push(encodeURIComponent(k) + '=' + encodeURIComponent(params[k])); });
        return q.length ? '?' + q.join('&') : '';
    }
    function icon(name) {
        var svg = document.createElementNS(SVG_NS, 'svg');
        svg.setAttribute('class', 'ui-icon');
        svg.setAttribute('aria-hidden', 'true');
        svg.setAttribute('focusable', 'false');
        var use = document.createElementNS(SVG_NS, 'use');
        use.setAttribute('href', SPRITE + '#ic-' + (ICON.test(name || '') ? name : 'products'));
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

    function render(host, data, opts) {
        host.textContent = '';
        if (!data || !data.eligible || !data.items || !data.items.length) {
            host.hidden = true;                       // 기능4 금융회원: "추천 상품" 전체 숨김
            return;
        }
        var id = 'ui-rec-' + (++seq);
        var sec = el('section', 'ui-rec');
        sec.setAttribute('aria-labelledby', id);
        var head = el('div', 'ui-rec__head');
        var h = el('h2', 'ui-rec__title', opts.title || '추천 상품');
        h.id = id;
        head.appendChild(h);
        var more = link('ui-rec__more', data.moreUrl, { name: '전체상품' }, opts);    // 기능2
        more.appendChild(document.createTextNode('더보기'));
        var chev = el('span', 'ui-rec__chev'); chev.setAttribute('aria-hidden', 'true'); more.appendChild(chev);
        more.setAttribute('aria-label', '전체상품 더보기');
        head.appendChild(more);
        sec.appendChild(head);

        var ul = el('ul', 'ui-rec__list');
        data.items.forEach(function (p) {
            var li = el('li');
            var a = link('ui-rec__item', p.detailUrl, p, opts);                        // 기능3
            a.setAttribute('data-kind', p.kind);
            var tile = el('span', 'ui-rec__icon'); tile.appendChild(icon(p.icon));
            a.appendChild(tile);
            var info = el('span', 'ui-rec__info');
            var badge = el('span', 'ui-rec__badge', p.subType || p.kindName);
            info.appendChild(badge);
            info.appendChild(el('strong', 'ui-rec__name', p.name));
            if (p.summary) info.appendChild(el('span', 'ui-rec__summary', p.summary));
            a.appendChild(info);
            if (p.rateText) a.appendChild(el('span', 'ui-rec__rate', p.rateText));
            li.appendChild(a);
            ul.appendChild(li);
        });
        sec.appendChild(ul);
        host.appendChild(sec);
        host.hidden = false;
    }

    function mount(target, opts) {
        var host = toEl(target);
        opts = opts || {};
        host.hidden = true;
        return fetch('/api/common/products/recommend' + query(opts.params), {
            credentials: 'same-origin', headers: { 'X-Requested-With': 'XMLHttpRequest', 'Accept': 'application/json' }
        }).then(function (r) { return r.json(); }).then(function (res) {
            if (!res || !res.success) throw Object.assign(new Error((res && res.message) || '추천 상품 조회 실패'), { response: res });
            render(host, res.data, opts);
            if (typeof opts.onLoad === 'function') opts.onLoad(res.data);
            if (host.hidden && typeof opts.onHidden === 'function') opts.onHidden(res.data);
            return res.data;
        }).catch(function (e) {
            render(host, null, opts);
            if (typeof opts.onError === 'function') opts.onError(e);
            return null;
        });
    }

    global.RecommendProducts = { mount: mount, render: function (t, d, o) { render(toEl(t), d, o || {}); } };
})(window);
