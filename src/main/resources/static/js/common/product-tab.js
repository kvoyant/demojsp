/*
 * =============================================================================
 *  [CF-BC-20 / 공통#26] 메인화면 상품 탭 (APP / PC Web)  -  /js/common/product-tab.js
 * -----------------------------------------------------------------------------
 *  요구사항 (REQ-AP-606·607)
 *    1. 상단 슬라이드 카드 영역 신규 추가      → ① slides  (좌우로 넘기는 카드)
 *    2. 기존 불필요 카드 삭제 (예: 시세조회)   → ADMIN 에서 섹션 사용여부 N (화면 코드 수정 없음)
 *    3. 디자인 및 콘텐츠 형식 수정             → 카드/섹션/상품행 공통 형식
 *    ADMIN: 하단 상품 목록을 ADMIN 에서 컨트롤 → ② sections · items 는 전부 DB(ADMIN 설정)
 *
 *  사용법 (상품 탭 화면 JSP)
 *    <link rel="stylesheet" href="/css/common/ui-tokens.css">
 *    <link rel="stylesheet" href="/css/common/ui-icon.css">
 *    <link rel="stylesheet" href="/css/common/product-tab.css">
 *    <script src="/js/common/product-tab.js"></script>
 *
 *    <div id="productTab"></div>
 *    ProductTab.mount('#productTab', { channel: 'APP' });            // PC Web 은 channel: 'PC_WEB'
 *
 *  옵션
 *    channel           APP | PC_WEB (기본 APP) - 채널별 슬라이드 노출 여부가 다르다
 *    data              API 대신 이 데이터로 그린다 (ADMIN 미리보기)
 *    onNavigate(url, info)  카드·상품·서비스 카드를 눌렀을 때 (기본: location.href 이동, APP 은 네이티브 브릿지 연결)
 *    onLoad(data) / onError(err)
 *
 *  화면 구성
 *    ┌ ① 상단 슬라이드 카드 ─ 스크롤 스냅으로 손가락/트랙패드로 넘김, PC 는 ‹ › 버튼, "1 / 3" 표시
 *    ├ ② 섹션(상품목록) ─ 제목 + 설명 + 상품행(아이콘 · 유형 · 상품명 · 한줄설명 · 금리 · 배지)
 *    └ ② 섹션(서비스 카드) ─ 아이콘 + 제목 + 설명 → 이동
 *
 *  - 서버 문자열은 textContent 로만 출력 (XSS 안전), 이동 URL 은 / 또는 https:// 만 허용
 *  - 자동 넘김 없음 (고객이 직접 넘김 - 움직이는 콘텐츠 접근성 지침)
 *  - JSP 안에서 쓰지 않고 정적 파일이므로 템플릿 문자열을 써도 되지만, 다른 공통 컴포넌트와 같은 ES5 문법을 유지한다
 * =============================================================================
 */
(function (global) {
    'use strict';

    var API = '/api/common/product-tab?channel=';
    var SPRITE = '/images/common/ui-icons.svg';
    var SVG_NS = 'http://www.w3.org/2000/svg';
    var ICON_NAME = /^[a-z][a-z0-9-]{0,39}$/;

    // ------------------------------------------------------------------ 작은 도우미
    function el(tag, cls, text) {
        var e = document.createElement(tag);
        if (cls) e.className = cls;
        if (text != null) e.textContent = text;
        return e;
    }

    /** 공통 아이콘 (ui-icons.svg 스프라이트의 #ic-이름). 이름이 이상하면 기본 상품 아이콘 */
    function icon(name) {
        var svg = document.createElementNS(SVG_NS, 'svg');
        svg.setAttribute('class', 'ui-icon');
        svg.setAttribute('aria-hidden', 'true');
        svg.setAttribute('focusable', 'false');
        var use = document.createElementNS(SVG_NS, 'use');
        use.setAttribute('href', SPRITE + '#ic-' + (ICON_NAME.test(name || '') ? name : 'products'));
        svg.appendChild(use);
        return svg;
    }

    function safeUrl(url) {
        return typeof url === 'string' && (/^https:\/\//.test(url) || (/^\//.test(url) && !/^\/\//.test(url)));
    }

    /** 누르면 이동하는 링크. href 를 실제로 넣어 두어 새 탭 열기·스크린리더 "링크" 안내가 되게 한다 */
    function link(cls, url, info, opts) {
        var a = el('a', cls);
        a.href = safeUrl(url) ? url : '#';
        a.addEventListener('click', function (e) {
            e.preventDefault();
            if (!safeUrl(url)) return;
            if (typeof opts.onNavigate === 'function') opts.onNavigate(url, info);
            else global.location.href = url;
        });
        return a;
    }

    // ------------------------------------------------------------------ ① 상단 슬라이드 카드
    function renderSlides(slides, opts) {
        var wrap = el('section', 'pt-slides');
        wrap.setAttribute('aria-roledescription', 'carousel');
        wrap.setAttribute('aria-label', '추천 상품 카드');

        var track = el('div', 'pt-track');
        slides.forEach(function (s, i) {
            var card = link('pt-card', s.linkUrl, { type: 'SLIDE', id: s.slideId, productCd: s.productCd }, opts);
            card.style.backgroundColor = s.bgColor;
            card.style.color = s.textColor;                                // 서버가 배경색 대비로 계산 (어떤 색이든 읽힘)
            card.setAttribute('aria-label', (i + 1) + ' / ' + slides.length + ', ' + s.title + (s.subText ? ', ' + s.subText : ''));
            if (s.badgeText) card.appendChild(el('span', 'pt-card-badge', s.badgeText));
            card.appendChild(el('strong', 'pt-card-title', s.title));
            if (s.subText) card.appendChild(el('span', 'pt-card-sub', s.subText));
            if (s.imageUrl && safeUrl(s.imageUrl)) {
                var img = el('img', 'pt-card-img');
                img.src = s.imageUrl;
                img.alt = s.imageAlt || '';
                card.appendChild(img);
            }
            track.appendChild(card);
        });
        wrap.appendChild(track);

        if (slides.length > 1) {
            // 인디케이터 + PC 용 이전/다음 버튼 (모바일은 손가락으로 넘김)
            var nav = el('div', 'pt-nav');
            var prev = el('button', 'pt-arrow', '‹');
            var next = el('button', 'pt-arrow', '›');
            prev.type = next.type = 'button';
            prev.setAttribute('aria-label', '이전 카드');
            next.setAttribute('aria-label', '다음 카드');
            var count = el('span', 'pt-count');
            count.setAttribute('aria-live', 'polite');
            nav.appendChild(prev);
            nav.appendChild(count);
            nav.appendChild(next);
            wrap.appendChild(nav);

            var current = function () {
                var w = track.firstChild.getBoundingClientRect().width || 1;
                return Math.min(slides.length - 1, Math.round(track.scrollLeft / w));
            };
            var update = function () {
                var i = current();
                count.textContent = (i + 1) + ' / ' + slides.length;
                prev.disabled = i === 0;
                next.disabled = track.scrollLeft + track.clientWidth >= track.scrollWidth - 2;
            };
            var go = function (step) {
                var w = track.firstChild.getBoundingClientRect().width;
                track.scrollBy({ left: step * w, behavior: 'smooth' });
            };
            prev.addEventListener('click', function () { go(-1); });
            next.addEventListener('click', function () { go(1); });
            track.addEventListener('scroll', function () { global.requestAnimationFrame(update); });
            global.requestAnimationFrame(update);
        }
        return wrap;
    }

    // ------------------------------------------------------------------ ② 섹션
    function renderProductSection(sec, opts) {
        var box = el('section', 'pt-section');
        box.appendChild(el('h3', 'pt-section-title', sec.sectionNm));
        if (sec.sectionDesc) box.appendChild(el('p', 'pt-section-desc', sec.sectionDesc));
        var ul = el('ul', 'pt-items');
        sec.items.forEach(function (p) {
            var li = el('li');
            var a = link('pt-item', p.detailUrl, { type: 'PRODUCT', section: sec.sectionCd, productCd: p.productCd }, opts);
            var ic = el('span', 'pt-item-icon ' + (p.productKind === 'LOAN' ? 'loan' : 'deposit'));
            ic.appendChild(icon(p.icon));
            a.appendChild(ic);

            var body = el('span', 'pt-item-body');
            var top = el('span', 'pt-item-top');
            if (p.subType) top.appendChild(el('span', 'pt-item-type', p.subType));
            if (p.badgeText) top.appendChild(el('span', 'pt-item-badge', p.badgeText));
            body.appendChild(top);
            body.appendChild(el('strong', 'pt-item-name', p.productNm));
            if (p.summary) body.appendChild(el('span', 'pt-item-summary', p.summary));
            a.appendChild(body);

            if (p.rateText) a.appendChild(el('span', 'pt-item-rate', p.rateText));
            li.appendChild(a);
            ul.appendChild(li);
        });
        box.appendChild(ul);
        return box;
    }

    function renderCardSection(sec, opts) {
        var a = link('pt-service', sec.linkUrl, { type: 'CARD', section: sec.sectionCd }, opts);
        var ic = el('span', 'pt-service-icon');
        ic.appendChild(icon(sec.cardIcon));
        a.appendChild(ic);
        var body = el('span', 'pt-service-body');
        body.appendChild(el('strong', 'pt-service-title', sec.sectionNm));
        if (sec.sectionDesc) body.appendChild(el('span', 'pt-service-desc', sec.sectionDesc));
        a.appendChild(body);
        a.appendChild(el('span', 'pt-service-arrow', '›'));
        return a;
    }

    // ------------------------------------------------------------------ 그리기
    function render(host, data, opts) {
        host.textContent = '';
        host.classList.add('pt-root');
        if (data.slides && data.slides.length) host.appendChild(renderSlides(data.slides, opts));
        var cards = null;
        (data.sections || []).forEach(function (sec) {
            if (sec.sectionType === 'CARD') {
                // 서비스 카드가 연달아 오면 한 줄(그리드)로 묶는다
                if (!cards) { cards = el('div', 'pt-services'); host.appendChild(cards); }
                cards.appendChild(renderCardSection(sec, opts));
            } else {
                cards = null;
                host.appendChild(renderProductSection(sec, opts));
            }
        });
        if (!host.children.length) host.appendChild(el('p', 'pt-empty', '안내할 상품이 없습니다.'));
    }

    function mount(target, options) {
        var opts = options || {};
        var host = typeof target === 'string' ? document.querySelector(target) : target;
        if (!host) throw new Error('ProductTab.mount: 대상 요소를 찾을 수 없습니다.');
        if (opts.data) {
            render(host, opts.data, opts);
            return Promise.resolve(opts.data);
        }
        host.setAttribute('aria-busy', 'true');
        return fetch(API + encodeURIComponent(opts.channel || 'APP'), { credentials: 'same-origin' })
            .then(function (r) { return r.json(); })
            .then(function (res) {
                host.removeAttribute('aria-busy');
                if (!res.success) throw res;
                render(host, res.data, opts);
                if (typeof opts.onLoad === 'function') opts.onLoad(res.data);
                return res.data;
            })
            .catch(function (err) {
                host.removeAttribute('aria-busy');
                host.textContent = '';
                host.appendChild(el('p', 'pt-empty', '상품 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.'));
                if (typeof opts.onError === 'function') opts.onError(err);
            });
    }

    global.ProductTab = { mount: mount };
})(window);
