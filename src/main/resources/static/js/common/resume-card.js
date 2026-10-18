/**
 * =============================================================================
 *  [CF-BC-13 / 공통#43] 이어하기 카드 공통 컴포넌트  -  /js/common/resume-card.js
 * -----------------------------------------------------------------------------
 *  [방향] 진행 중인 상품가입 건 개수 등 핵심정보 추가 표시
 *  [목적] 기존의 뒷 화면 가림현상 개선(팝업 → 화면 상단 카드) 및 이어하기 촉진
 *
 *  사용법 (홈 / 저축 탭 / 대출 탭 공통 - scope 만 다르다)
 *    <link rel="stylesheet" href="/css/common/ui-tokens.css">
 *    <link rel="stylesheet" href="/css/common/ui-button.css">     (이어하기 버튼 press·이동 중 표시)
 *    <link rel="stylesheet" href="/css/common/resume-card.css">
 *    <script src="/js/common/ui-button.js"></script>               (없어도 동작 - 있으면 "이동 중" 표시)
 *    <script src="/js/common/resume-card.js"></script>
 *
 *    <div id="resumeCard"></div>                                   ← 화면 상단 (기능6)
 *    ResumeCard.mount('#resumeCard', { scope: 'ALL' });             홈(전체)
 *    ResumeCard.mount('#resumeCard', { scope: 'DEPOSIT' });         저축 탭(수신, 공통#49)
 *    ResumeCard.mount('#resumeCard', { scope: 'LOAN' });            대출 탭(여신, 공통#60)
 *
 *  옵션
 *    scope        ALL | DEPOSIT | LOAN (기본 ALL)
 *    params       API·더보기에 붙일 추가 파라미터 { demoCustomer: 'customer02' } (local 샘플 전용)
 *    title        카드 제목 (기본: ALL "진행 중인 신청" / DEPOSIT "진행 중인 저축 가입" / LOAN "진행 중인 대출 신청")
 *    go(url)      이동 함수 (APP 네이티브 화면전환 브릿지 등, 기본 location.assign)
 *    onLoad(summary) / onEmpty(summary) / onError(err) / onNotResumable(res)
 *
 *  화면 구성 (기능 1~5)
 *    [진행 중인 신청 (3)]                                [더보기 >]   ← 건수 숫자 · 신청현황조회 이동
 *    [로고] 회사명 (제휴)                                            ← 당행 로고 / 대외 연계 기업 로고
 *           상품명                                       [이어하기]  ← 해당 상품 신청 링크
 *           대출신청 · 서류제출 단계 3/5 · D-6
 *
 *  - 이어하기 0건 → 카드를 그리지 않음 (hidden). 조회 실패도 홈 화면을 막지 않도록 조용히 숨김
 *  - [이어하기] 클릭 시 서버가 본인·진행중·만료 전을 다시 확인 → 이미 끝난 건이면 안내 후 카드 갱신
 *  - jQuery / Bootstrap 비의존, 모든 문구는 textContent 로만 출력 (XSS 안전)
 *  - JSP 에서 사용하므로 템플릿 문자열(백틱 + 달러 중괄호)을 쓰지 않는다 (EL 충돌)
 * =============================================================================
 */
(function (global) {
    'use strict';

    var API = '/api/common/resume';
    /** scope 별 기본 제목 (홈 공통#43 / 저축 공통#49 / 대출 공통#60) */
    var TITLES = { ALL: '진행 중인 신청', DEPOSIT: '진행 중인 저축 가입', LOAN: '진행 중인 대출 신청' };
    var SPRITE = '/images/common/ui-icons.svg';
    var SVG_NS = 'http://www.w3.org/2000/svg';
    var seq = 0;
    var mounted = typeof WeakMap === 'function' ? new WeakMap() : null;

    // ------------------------------------------------------------------ util
    function toEl(t) {
        if (!t) return null;
        if (typeof t === 'string') return document.querySelector(t);
        return t.nodeType === 1 ? t : null;
    }
    function el(tag, cls, text) {
        var e = document.createElement(tag);
        if (cls) e.className = cls;
        if (text != null) e.textContent = text;
        return e;
    }
    function query(params) {
        var q = [];
        Object.keys(params || {}).forEach(function (k) {
            if (params[k] != null && params[k] !== '') q.push(encodeURIComponent(k) + '=' + encodeURIComponent(params[k]));
        });
        return q.join('&');
    }
    function withQuery(url, params) {
        var q = query(params);
        return q ? url + (url.indexOf('?') >= 0 ? '&' : '?') + q : url;
    }
    function request(url, method) {
        return fetch(url, {
            method: method || 'GET',
            credentials: 'same-origin',
            headers: { 'X-Requested-With': 'XMLHttpRequest', 'Accept': 'application/json' }
        }).then(function (r) {
            return r.json().catch(function () { return { success: false, code: 'HTTP_' + r.status, message: '응답을 확인할 수 없습니다.' }; });
        });
    }
    function icon(name, cls) {
        var svg = document.createElementNS(SVG_NS, 'svg');
        svg.setAttribute('class', 'ui-icon ' + (cls || ''));
        svg.setAttribute('aria-hidden', 'true');
        svg.setAttribute('focusable', 'false');
        var use = document.createElementNS(SVG_NS, 'use');
        use.setAttribute('href', SPRITE + '#ic-' + name);
        svg.appendChild(use);
        return svg;
    }
    function notify(message) {
        if (global.UiAlert) return global.UiAlert.alert(message, { type: 'info' });
        global.alert(message);
        return Promise.resolve();
    }

    // ------------------------------------------------------------------ 로고 (기능5)
    /**
     * 로고 이미지가 있으면 <img>, 없거나 깨지면 대체 로고.
     *  - 당행 상품: 은행 아이콘 (당행 로고 제작 전)
     *  - 대외 연계 상품: 회사명 첫 글자 (기업 로고 수집 전)
     */
    function logo(item) {
        var box = el('span', 'ui-resume__logo' + (item.own ? ' is-own' : ' is-partner'));
        function fallback() {
            box.textContent = '';
            if (item.own) box.appendChild(icon('bank'));
            else box.appendChild(el('span', 'ui-resume__initial', (item.companyName || '?').charAt(0)));
        }
        if (item.companyLogoUrl) {
            var img = el('img');
            img.src = item.companyLogoUrl;
            img.alt = '';                       // 회사명이 바로 옆에 글자로 있으므로 장식 처리 (중복 낭독 방지)
            img.decoding = 'async';
            img.addEventListener('error', fallback, { once: true });
            box.appendChild(img);
        } else {
            fallback();
        }
        return box;
    }

    function metaText(item) {
        var parts = [item.bizName];
        var step = item.stepName;
        if (item.totalSteps > 0) step += ' ' + item.stepNo + '/' + item.totalSteps + '단계';
        parts.push(step);
        if (item.daysLeft != null) parts.push(item.daysLeft === 0 ? '오늘 마감' : 'D-' + item.daysLeft);
        return parts.join(' · ');
    }

    // ------------------------------------------------------------------ render
    function render(host, s, opts) {
        host.textContent = '';
        if (!s || !s.count || !s.latest) {
            host.hidden = true;
            host.removeAttribute('aria-label');
            return;
        }
        var item = s.latest;
        var id = 'ui-resume-' + (++seq);
        var card = el('section', 'ui-resume');
        card.setAttribute('aria-labelledby', id + '-t');
        card.setAttribute('data-scope', s.scope);

        // 머리: 제목 + 건수(기능2) + 더보기(기능3)
        var head = el('div', 'ui-resume__head');
        var title = el('h2', 'ui-resume__title');
        title.id = id + '-t';
        title.appendChild(document.createTextNode(opts.title || TITLES[s.scope] || TITLES.ALL));
        var cnt = el('span', 'ui-resume__count', String(s.count));
        cnt.setAttribute('aria-label', '총 ' + s.count + '건');
        title.appendChild(cnt);
        head.appendChild(title);

        var more = el('a', 'ui-resume__more');
        more.href = withQuery(s.moreUrl, Object.assign({ scope: s.scope }, opts.params || {}));
        more.appendChild(document.createTextNode('더보기'));
        var chev = el('span', 'ui-resume__chev');
        chev.setAttribute('aria-hidden', 'true');
        more.appendChild(chev);
        more.setAttribute('aria-label', '진행 중인 신청 ' + s.count + '건 전체 보기 (신청현황조회)');
        head.appendChild(more);
        card.appendChild(head);

        // 본문: 로고 + 회사명 + 상품명 + 이어하기 (기능4·5)
        var body = el('div', 'ui-resume__body');
        body.appendChild(logo(item));
        var info = el('div', 'ui-resume__info');
        var company = el('span', 'ui-resume__company', item.companyName);
        if (!item.own) company.appendChild(el('span', 'ui-resume__tag', '제휴'));
        info.appendChild(company);
        info.appendChild(el('strong', 'ui-resume__product', item.productName));
        info.appendChild(el('span', 'ui-resume__meta', metaText(item)));
        body.appendChild(info);

        var go = el('button', 'ui-btn ui-btn--primary ui-btn--sm ui-resume__go', '이어하기');
        go.type = 'button';
        go.setAttribute('data-busy-text', '이동 중');
        go.setAttribute('aria-label', item.productName + ' ' + item.bizName + ' 이어하기');
        go.addEventListener('click', function () { onContinue(host, go, item, opts); });
        body.appendChild(go);
        card.appendChild(body);

        // 진행 단계 막대 (전체 단계를 아는 경우)
        if (item.totalSteps > 0) {
            var bar = el('div', 'ui-resume__progress');
            bar.setAttribute('role', 'progressbar');
            bar.setAttribute('aria-label', '진행 단계');
            bar.setAttribute('aria-valuemin', '0');
            bar.setAttribute('aria-valuemax', String(item.totalSteps));
            bar.setAttribute('aria-valuenow', String(item.stepNo - 1));
            bar.setAttribute('aria-valuetext', item.totalSteps + '단계 중 ' + item.stepNo + '단계 진행 중');
            var fill = el('span');
            fill.style.width = Math.round(((item.stepNo - 1) / item.totalSteps) * 100) + '%';
            bar.appendChild(fill);
            card.appendChild(bar);
        }

        host.appendChild(card);
        host.hidden = false;
    }

    // ------------------------------------------------------------------ 이어하기 클릭
    function onContinue(host, btn, item, opts) {
        if (btn.classList.contains('is-busy')) return;
        var url = API + '/' + encodeURIComponent(item.id) + '/continue';
        var call = function () { return request(withQuery(url, opts.params), 'POST'); };
        var p = global.UiButton ? global.UiButton.run(btn, call, { text: '확인 중' }) : call();
        p.then(function (res) {
            if (res && res.success && res.data && res.data.url) {
                if (global.UiButton) global.UiButton.navigate(btn, res.data.url, { go: opts.go });
                else if (typeof opts.go === 'function') opts.go(res.data.url);
                else global.location.assign(res.data.url);
                return;
            }
            // 다른 기기에서 완료/취소했거나 기간이 지난 경우 → 안내 후 최신 상태로 다시 그림
            if (typeof opts.onNotResumable === 'function') opts.onNotResumable(res);
            notify((res && res.message) || '이어하기를 진행할 수 없습니다.').then(function () { load(host, opts); });
        }).catch(function (err) {
            if (typeof opts.onError === 'function') opts.onError(err);
            notify('일시적으로 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.');
        });
    }

    // ------------------------------------------------------------------ load
    function load(host, opts) {
        var url = withQuery(API, Object.assign({ scope: opts.scope || 'ALL' }, opts.params || {}));
        host.setAttribute('aria-busy', 'true');
        return request(url).then(function (res) {
            host.removeAttribute('aria-busy');
            if (!res || !res.success) {
                render(host, null, opts);
                var err = new Error((res && res.message) || '이어하기 조회 실패');
                err.response = res;
                if (typeof opts.onError === 'function') opts.onError(err);
                return null;
            }
            render(host, res.data, opts);
            if (typeof opts.onLoad === 'function') opts.onLoad(res.data);
            if (!res.data.count && typeof opts.onEmpty === 'function') opts.onEmpty(res.data);
            return res.data;
        }).catch(function (e) {
            host.removeAttribute('aria-busy');
            render(host, null, opts);       // 홈 화면은 막지 않는다
            if (typeof opts.onError === 'function') opts.onError(e);
            return null;
        });
    }

    /** 카드 표시. @return Promise<summary|null> */
    function mount(target, opts) {
        var host = toEl(target);
        if (!host) return Promise.reject(new Error('ResumeCard: 대상 요소 없음 ' + target));
        opts = Object.assign({}, opts || {});
        if (mounted) mounted.set(host, opts);
        else host.__resumeOpts = opts;
        host.hidden = true;                 // 조회 전에는 빈 자리를 보이지 않는다
        host.setAttribute('data-resume-autorefresh', '');
        return load(host, opts);
    }

    /** 같은 옵션으로 다시 조회 (업무 처리 후, 화면 복귀 시) */
    function refresh(target) {
        var host = toEl(target);
        if (!host) return Promise.resolve(null);
        var opts = mounted ? mounted.get(host) : host.__resumeOpts;
        return load(host, opts || {});
    }

    global.ResumeCard = { mount: mount, refresh: refresh, render: function (t, s, o) { render(toEl(t), s, o || {}); } };

    // 뒤로가기(bfcache)로 홈에 돌아오면 다른 화면에서 진행한 내용 반영
    global.addEventListener('pageshow', function (e) {
        if (!e.persisted) return;
        document.querySelectorAll('[data-resume-autorefresh]').forEach(function (h) { refresh(h); });
    });
})(window);
