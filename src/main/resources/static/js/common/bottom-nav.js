/**
 * =============================================================================
 *  [공통#50] APP 하단 탭바 + 저축 이어하기 툴팁  -  /js/common/bottom-nav.js
 * -----------------------------------------------------------------------------
 *  [방향] 수신계좌 개설 이어하기 건 보유 시 탭에 강조 표시   [목적] 이어하기 진행 건 강조
 *  [정책] 1. 제공대상: 수신계좌 이어하기 대상자 모두
 *         2. 이어하기가 있으면 툴팁 상시 활성 - 현재 위치가 홈/저축/대출 탭 어디든 표시
 *  [기능] 1. 하단 '저축' 탭 영역에 아이콘 형태로 숫자(진행중인 건 수) 표시
 *         2. 미노출: 진행중인 계좌개설 건이 없을 때 / 이어하기 진행 중 취소하여 잔여 건이 없을 때
 *
 *  사용법
 *    <link rel="stylesheet" href="/css/common/ui-tokens.css">
 *    <link rel="stylesheet" href="/css/common/ui-icon.css">
 *    <link rel="stylesheet" href="/css/common/bottom-nav.css">
 *    <script src="/js/common/bottom-nav.js"></script>
 *
 *    <nav id="bottomNav"></nav>
 *    BottomNav.mount('#bottomNav', { active: 'SAVINGS' });        // 탭바 + 저축 탭 툴팁 (홈/저축/대출 화면 모두 같은 호출)
 *    ResumeTabBadge.refresh();                                     // 업무 화면에서 이어하기 취소·완료 후 (또는 ResumeCard.changed())
 *
 *  옵션
 *    active        현재 탭 키 (HOME / SAVINGS / LOAN)
 *    tabs          탭 정의 (기본: 홈 /home, 저축 /savings, 대출 /loan)
 *    badges        탭 키 → 건수 범위 (기본 { SAVINGS: 'DEPOSIT' }) - 대출 탭 툴팁이 필요하면 { LOAN: 'LOAN' } 추가
 *    params        API 추가 파라미터 (local 샘플: { demoCustomer: 'customer01' })
 *    onSelect(tab) 탭 터치 (기본: 링크 이동) / onBadge(tabKey, count) 건수 갱신 시 (APP 네이티브 탭바 브릿지 연결용, CF-NA-07)
 *
 *  갱신 시점: 탭바 표시 시, 'resume:changed' 이벤트(ResumeCard·업무 화면), 뒤로가기 복귀(pageshow), 앱 복귀(visibilitychange)
 *  접근성: 숫자 배지는 aria-hidden, 탭 링크 이름에 "진행 중인 저축 가입 N건" 포함 / aria-current="page"
 *  - 조회 실패 시 배지를 숨김 (탭바는 그대로), textContent 만 사용 (XSS 안전), 템플릿 문자열 미사용
 * =============================================================================
 */
(function (global) {
    'use strict';

    var API = '/api/common/resume/counts';
    var SPRITE = '/images/common/ui-icons.svg';
    var SVG_NS = 'http://www.w3.org/2000/svg';
    var DEFAULT_TABS = [
        { key: 'HOME', label: '홈', icon: 'bank', url: '/home' },
        { key: 'SAVINGS', label: '저축', icon: 'savings', url: '/savings' },
        { key: 'LOAN', label: '대출', icon: 'loan', url: '/loan' }
    ];
    var BADGE_LABEL = { DEPOSIT: '진행 중인 저축 가입', LOAN: '진행 중인 대출 신청', ALL: '진행 중인 신청' };
    var navs = [];          // mount 된 탭바들 (같은 화면에 여러 개 가능 - 샘플 미리보기)

    function toEl(t) { return typeof t === 'string' ? document.querySelector(t) : t; }
    function el(tag, cls, text) { var e = document.createElement(tag); if (cls) e.className = cls; if (text != null) e.textContent = text; return e; }
    function query(params) {
        var q = [];
        Object.keys(params || {}).forEach(function (k) { if (params[k]) q.push(encodeURIComponent(k) + '=' + encodeURIComponent(params[k])); });
        return q.length ? '?' + q.join('&') : '';
    }
    function icon(name) {
        var svg = document.createElementNS(SVG_NS, 'svg');
        svg.setAttribute('class', 'ui-icon ui-tab__icon');
        svg.setAttribute('aria-hidden', 'true');
        svg.setAttribute('focusable', 'false');
        var use = document.createElementNS(SVG_NS, 'use');
        use.setAttribute('href', SPRITE + '#ic-' + name);
        svg.appendChild(use);
        return svg;
    }

    function render(host, opts) {
        host.textContent = '';
        var ul = el('ul', 'ui-tabbar');
        var tabEls = {};
        (opts.tabs || DEFAULT_TABS).forEach(function (t) {
            var li = el('li', 'ui-tabbar__cell');
            var a = el('a', 'ui-tab');
            a.href = t.url;
            a.setAttribute('data-tab', t.key);
            if (t.key === opts.active) a.setAttribute('aria-current', 'page');
            var iw = el('span', 'ui-tab__iconwrap');
            iw.appendChild(icon(t.icon));
            var badge = el('span', 'ui-tab__badge');
            badge.setAttribute('aria-hidden', 'true');
            badge.hidden = true;
            iw.appendChild(badge);
            a.appendChild(iw);
            var label = el('span', 'ui-tab__label', t.label);
            a.appendChild(label);
            var sr = el('span', 'ui-tab__sr');
            a.appendChild(sr);
            a.addEventListener('click', function (e) {
                if (typeof opts.onSelect === 'function') { e.preventDefault(); opts.onSelect(t); }
            });
            li.appendChild(a);
            ul.appendChild(li);
            tabEls[t.key] = { badge: badge, sr: sr };
        });
        host.appendChild(ul);
        host.setAttribute('aria-label', '하단 메뉴');
        return tabEls;
    }

    /** 기능1·2: 건수 > 0 이면 숫자 배지, 0 이면 숨김 */
    function setBadge(tab, scope, count) {
        if (!tab) return;
        var n = Math.max(0, count | 0);
        tab.badge.hidden = n === 0;
        tab.badge.textContent = n > 99 ? '99+' : String(n);
        tab.sr.textContent = n ? ', ' + (BADGE_LABEL[scope] || BADGE_LABEL.ALL) + ' ' + n + '건' : '';
    }

    function refreshOne(nav) {
        return fetch(API + query(nav.opts.params), {
            credentials: 'same-origin', headers: { 'X-Requested-With': 'XMLHttpRequest', 'Accept': 'application/json' }
        }).then(function (r) { return r.json(); }).then(function (res) {
            var counts = res && res.success ? res.data : {};
            Object.keys(nav.badges).forEach(function (key) {
                var scope = nav.badges[key];
                var n = counts[scope] || 0;
                setBadge(nav.tabs[key], scope, n);
                if (typeof nav.opts.onBadge === 'function') nav.opts.onBadge(key, n);
            });
            return counts;
        }).catch(function () {
            Object.keys(nav.badges).forEach(function (key) { setBadge(nav.tabs[key], nav.badges[key], 0); });   // 실패 → 숨김
            return null;
        });
    }

    function refresh() {
        return Promise.all(navs.filter(function (n) { return document.contains(n.host); }).map(refreshOne));
    }

    function mount(target, opts) {
        var host = toEl(target);
        opts = Object.assign({}, opts || {});
        navs = navs.filter(function (n) { return n.host !== host; });
        var nav = { host: host, opts: opts, tabs: render(host, opts), badges: opts.badges || { SAVINGS: 'DEPOSIT' } };
        navs.push(nav);
        return refreshOne(nav);
    }

    /** 다른 고객/파라미터로 다시 조회 (샘플·테스트용) */
    function setParams(target, params) {
        var host = toEl(target);
        navs.forEach(function (n) { if (n.host === host) n.opts.params = params; });
        return refresh();
    }

    // 정책2 상시 활성: 이어하기 변화·화면 복귀 시 자동 갱신
    global.addEventListener('resume:changed', function () { refresh(); });
    global.addEventListener('pageshow', function (e) { if (e.persisted) refresh(); });
    document.addEventListener('visibilitychange', function () { if (!document.hidden) refresh(); });

    global.BottomNav = { mount: mount, setParams: setParams };
    global.ResumeTabBadge = { refresh: refresh };
})(window);
