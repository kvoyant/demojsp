/**
 * =============================================================================
 *  [CF-UI-01 / 공통#41] Icon 공통 컴포넌트  -  /js/common/ui-icon.js
 * -----------------------------------------------------------------------------
 *  아이콘 원본은 SVG 스프라이트(/images/common/ui-icons.svg) 1개 파일로 관리한다.
 *  이 스크립트는 "이름만으로" 아이콘을 쓰게 해 주는 도우미다. (JSP 에서 SVG 마크업을 직접 써도 된다)
 *
 *  사용법
 *    <link rel="stylesheet" href="/css/common/ui-tokens.css">
 *    <link rel="stylesheet" href="/css/common/ui-icon.css">
 *    <script src="/js/common/ui-icon.js"></script>
 *
 *    (1) 선언형 - 화면 로드 시 자동 변환
 *        <i data-ui-icon="loan"></i>                              장식용 (스크린리더 무시)
 *        <i data-ui-icon="status-success" data-label="성공"></i>  의미 있는 아이콘 (스크린리더가 "성공" 으로 읽음)
 *        <i data-ui-icon="card" data-size="lg" data-tone="primary"></i>
 *    (2) JS
 *        el.appendChild(UiIcon.create('repayment', { size: 'xl', tone: 'primary', label: '상환' }));
 *        UiIcon.status('warning', '서류 보완 필요')     → <span class="ui-status ui-status--warning">아이콘 + 문구</span>
 *        UiIcon.render(container)                        → 동적으로 추가한 data-ui-icon 변환
 *    (3) APP WebView 등 외부 스프라이트 참조가 안 되는 환경
 *        UiIcon.inline().then(...)  → 스프라이트를 문서에 1회 삽입 후 #ic-xxx 로 참조
 *
 *  - jQuery / Bootstrap 비의존, 라벨은 setAttribute/textContent 로만 출력 (XSS 안전)
 *  - 없는 아이콘 이름은 콘솔 경고 후 빈 자리(같은 크기)를 유지 → 화면 레이아웃이 깨지지 않음
 * =============================================================================
 */
(function (global) {
    'use strict';

    var SVG_NS = 'http://www.w3.org/2000/svg';
    var XLINK_NS = 'http://www.w3.org/1999/xlink';
    var SIZES = { xs: 1, sm: 1, md: 1, lg: 1, xl: 1, '2xl': 1, em: 1 };
    var TONES = { primary: 1, success: 1, error: 1, warning: 1, info: 1, muted: 1 };

    /**
     * 아이콘 목록 (스프라이트 symbol 과 1:1 - UiComponentTest 가 일치 여부 검사).
     * group: FINANCE = 금융 서비스 전용 (요구사항 1) / STATUS = 상태 (요구사항 2)
     */
    var CATALOG = [
        { name: 'loan',               ko: '대출',                 group: 'FINANCE' },
        { name: 'deposit',            ko: '예금',                 group: 'FINANCE' },
        { name: 'savings',            ko: '적금',                 group: 'FINANCE' },
        { name: 'transfer',           ko: '이체',                 group: 'FINANCE' },
        { name: 'account',            ko: '계좌·통장',            group: 'FINANCE' },
        { name: 'bank',               ko: '은행·영업점',          group: 'FINANCE' },
        { name: 'card',               ko: '카드',                 group: 'FINANCE' },
        { name: 'credit-score',       ko: '신용점수',             group: 'FINANCE' },
        { name: 'interest-rate',      ko: '금리',                 group: 'FINANCE' },
        { name: 'limit-inquiry',      ko: '한도조회',             group: 'FINANCE' },
        { name: 'repayment',          ko: '상환',                 group: 'FINANCE' },
        { name: 'refinance',          ko: '대환',                 group: 'FINANCE' },
        { name: 'auto-loan',          ko: '오토(자동차)담보대출', group: 'FINANCE' },
        { name: 'mortgage',           ko: '주택(부동산)담보대출', group: 'FINANCE' },
        { name: 'calculator',         ko: '계산기(이자계산)',     group: 'FINANCE' },
        { name: 'document',           ko: '서류제출',             group: 'FINANCE' },
        { name: 'contract',           ko: '전자약정·서명',        group: 'FINANCE' },
        { name: 'identity',           ko: '본인인증·신분증',      group: 'FINANCE' },
        { name: 'security',           ko: '보안',                 group: 'FINANCE' },
        { name: 'otp',                ko: 'OTP·인증번호',         group: 'FINANCE' },
        { name: 'calendar',           ko: '납입일·만기일',        group: 'FINANCE' },
        { name: 'customer-center',    ko: '고객센터·상담',        group: 'FINANCE' },
        { name: 'notice',             ko: '공지·알림',            group: 'FINANCE' },
        { name: 'event',              ko: '혜택·이벤트',          group: 'FINANCE' },
        { name: 'products',           ko: '전체상품',             group: 'FINANCE' },
        { name: 'open-banking',       ko: '오픈뱅킹',             group: 'FINANCE' },

        { name: 'status-success',      ko: '성공',        group: 'STATUS', tone: 'success' },
        { name: 'status-error',        ko: '실패',        group: 'STATUS', tone: 'error' },
        { name: 'status-warning',      ko: '주의',        group: 'STATUS', tone: 'warning' },
        { name: 'status-info',         ko: '안내',        group: 'STATUS', tone: 'info' },
        { name: 'status-pending',      ko: '진행중·대기', group: 'STATUS', tone: 'muted' },
        { name: 'status-success-fill', ko: '성공',        group: 'STATUS', tone: 'success' },
        { name: 'status-error-fill',   ko: '실패',        group: 'STATUS', tone: 'error' },
        { name: 'status-warning-fill', ko: '주의',        group: 'STATUS', tone: 'warning' },
        { name: 'status-info-fill',    ko: '안내',        group: 'STATUS', tone: 'info' },
        { name: 'status-pending-fill', ko: '진행중·대기', group: 'STATUS', tone: 'muted' }
    ];
    var BY_NAME = {};
    CATALOG.forEach(function (c) { BY_NAME[c.name] = c; });

    /** 상태 코드 → 아이콘 (UiIcon.status / Alert 공통#42 에서 사용) */
    var STATUS = {
        success: { icon: 'status-success', tone: 'success', ko: '성공' },
        error:   { icon: 'status-error',   tone: 'error',   ko: '실패' },
        warning: { icon: 'status-warning', tone: 'warning', ko: '주의' },
        info:    { icon: 'status-info',    tone: 'info',    ko: '안내' },
        pending: { icon: 'status-pending', tone: 'muted',   ko: '진행중' }
    };

    var api = {
        /** 스프라이트 경로 (컨텍스트 패스가 있으면 변경: UiIcon.sprite = '/ctx/images/common/ui-icons.svg') */
        sprite: '/images/common/ui-icons.svg',
        /** inline() 후 true → '#ic-xxx' 로 참조 */
        inlined: false,
        CATALOG: CATALOG,
        STATUS: STATUS
    };

    function href(name) {
        return (api.inlined ? '' : api.sprite) + '#ic-' + name;
    }

    /**
     * 아이콘 SVG element 생성.
     * @param name  카탈로그 이름 (예: 'loan', 'status-success')
     * @param opts  { size: xs|sm|md|lg|xl|2xl|em, tone: primary|success|error|warning|info|muted, label: 스크린리더 문구, className }
     */
    function create(name, opts) {
        opts = opts || {};
        var svg = document.createElementNS(SVG_NS, 'svg');
        var cls = ['ui-icon'];
        if (opts.size && SIZES[opts.size] && opts.size !== 'md') cls.push('ui-icon--' + opts.size);
        if (opts.tone && TONES[opts.tone]) cls.push('ui-icon--' + opts.tone);
        if (opts.className) cls.push(opts.className);
        svg.setAttribute('class', cls.join(' '));
        svg.setAttribute('focusable', 'false');      // IE/구형 Edge 탭 포커스 방지

        if (opts.label) {
            // 의미 있는 아이콘: 스크린리더가 label 로 읽음
            svg.setAttribute('role', 'img');
            svg.setAttribute('aria-label', String(opts.label));
        } else {
            svg.setAttribute('aria-hidden', 'true');  // 장식용: 옆 글자와 중복 낭독 방지
        }

        if (!BY_NAME[name]) {
            if (global.console) console.warn('[UiIcon] 등록되지 않은 아이콘: ' + name);
            svg.setAttribute('data-missing', String(name));
            return svg;                               // 같은 크기의 빈 자리 유지
        }
        var use = document.createElementNS(SVG_NS, 'use');
        use.setAttribute('href', href(name));
        use.setAttributeNS(XLINK_NS, 'xlink:href', href(name));   // 구형 Safari / Android WebView
        svg.appendChild(use);
        return svg;
    }

    /**
     * 상태 문구 (아이콘 + 글자) - 색상만으로 상태를 구분하지 않도록 항상 아이콘 모양과 글자를 함께 쓴다.
     * @param type  success | error | warning | info | pending
     * @param text  표시 문구 (없으면 기본 문구: 성공/실패/주의/안내/진행중)
     */
    function status(type, text) {
        var s = STATUS[type] || STATUS.info;
        var span = document.createElement('span');
        span.className = 'ui-status ui-status--' + (STATUS[type] ? type : 'info');
        span.appendChild(create(s.icon));             // 글자가 있으므로 아이콘은 장식(aria-hidden)
        var label = document.createElement('span');
        label.textContent = text || s.ko;
        span.appendChild(label);
        return span;
    }

    /** data-ui-icon 요소를 SVG 로 변환 (root 기본 document) */
    function render(root) {
        root = root || document;
        var list = root.querySelectorAll('[data-ui-icon]:not([data-ui-icon-done])');
        for (var i = 0; i < list.length; i++) {
            var el = list[i];
            var svg = create(el.getAttribute('data-ui-icon'), {
                size: el.getAttribute('data-size'),
                tone: el.getAttribute('data-tone'),
                label: el.getAttribute('data-label'),
                className: el.getAttribute('data-class')
            });
            svg.setAttribute('data-ui-icon-done', '');
            if (el.id) svg.id = el.id;
            el.parentNode.replaceChild(svg, el);
        }
        return list.length;
    }

    /**
     * 스프라이트를 문서에 직접 삽입 (외부 SVG use 참조가 막힌 WebView / 다른 도메인 CDN 사용 시).
     * @return Promise
     */
    var inlining = null;
    function inline() {
        if (api.inlined) return Promise.resolve();
        if (inlining) return inlining;
        inlining = fetch(api.sprite, { credentials: 'same-origin' })
            .then(function (r) { if (!r.ok) throw new Error('sprite ' + r.status); return r.text(); })
            .then(function (text) {
                var doc = new DOMParser().parseFromString(text, 'image/svg+xml');
                var svg = doc.documentElement;
                if (!svg || svg.nodeName.toLowerCase() !== 'svg') throw new Error('sprite parse error');
                // 스프라이트는 같은 출처의 정적 파일이지만, 혹시 모를 스크립트/이벤트 속성은 제거하고 넣는다
                var bad = svg.querySelectorAll('script, foreignObject');
                for (var i = 0; i < bad.length; i++) bad[i].parentNode.removeChild(bad[i]);
                var holder = document.importNode(svg, true);
                holder.setAttribute('aria-hidden', 'true');
                holder.setAttribute('style', 'position:absolute;width:0;height:0;overflow:hidden');
                holder.setAttribute('id', 'ui-icon-sprite');
                document.body.insertBefore(holder, document.body.firstChild);
                api.inlined = true;
                // 이미 그린 아이콘도 내부 참조로 전환
                var uses = document.querySelectorAll('svg.ui-icon use');
                for (var j = 0; j < uses.length; j++) {
                    var h = uses[j].getAttribute('href') || '';
                    var k = h.indexOf('#');
                    if (k >= 0) {
                        uses[j].setAttribute('href', h.substring(k));
                        uses[j].setAttributeNS(XLINK_NS, 'xlink:href', h.substring(k));
                    }
                }
            });
        return inlining;
    }

    function has(name) { return !!BY_NAME[name]; }
    function list(group) { return CATALOG.filter(function (c) { return !group || c.group === group; }); }

    api.create = create;
    api.status = status;
    api.render = render;
    api.inline = inline;
    api.has = has;
    api.list = list;

    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', function () { render(); });
    else render();

    global.UiIcon = api;
})(window);
