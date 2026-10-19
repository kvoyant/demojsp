/*
 * =============================================================================
 *  [CF-BC-20 / 공통#28] 공지사항 공통 컴포넌트 (APP)  -  /js/common/notice.js
 * -----------------------------------------------------------------------------
 *  요구사항 (REQ-AP-609 APP)
 *    1. 전체 / 상품별 / 업무별 공지 구분 노출        → NoticeList  (탭 + 상품·업무 칩)
 *    2. 메인화면 띠 형태로 최신공지 제목 노출         → NoticeStrip
 *    3. 띠 영역 클릭 시 공지사항 상세 화면으로 이동   → NoticeStrip → detailUrl (?id=) → NoticeDetail
 *    4. 이어하기 지면과 겹치거나 혼동되지 않게 배치   → 띠는 헤더 바로 아래 한 줄(회색 바 + "공지" 표시),
 *                                                     이어하기는 아래쪽 카드 - 모양·위치가 다르다 (샘플 참고)
 *  요구사항 (REQ-AP-610 ADMIN 3) 공지사항 배너 팝업: 불필요한 아이콘 삭제 · 버튼 구조 변경 → NoticePopup (신규 UI)
 *
 *  사용법
 *    <link rel="stylesheet" href="/css/common/ui-tokens.css">
 *    <link rel="stylesheet" href="/css/common/notice.css">
 *    <script src="/js/common/notice.js"></script>
 *
 *    NoticeStrip.mount('#noticeStrip');                    // 메인(홈) 헤더 아래 - 공지가 없으면 영역째 숨김
 *    NoticePopup.open();                                   // 메인 진입 시 1회 - 팝업 공지가 없으면 아무것도 안 함
 *    NoticeList.mount('#noticeList', { scope: 'PRODUCT' }); // 공지 목록 화면 (scope 생략 = 전체 탭)
 *    NoticeDetail.mount('#noticeDetail', { id: 3 });        // 공지 상세 화면 (생략하면 주소의 ?id= 사용)
 *
 *  공통 옵션
 *    onNavigate(url, info)  화면 이동 (기본 location.href, APP 은 네이티브 브릿지 연결)
 *
 *  - 서버 문자열은 textContent 로만 출력 (공지 내용의 HTML 은 해석하지 않는다 → XSS 안전)
 *  - ES5 문법 유지 (다른 공통 컴포넌트와 동일, 구형 웹뷰 대응)
 * =============================================================================
 */
(function (global) {
    'use strict';

    var API = '/api/common/notice';
    var DETAIL_URL = '/common/notice/detail';
    var HIDE_KEY = 'noticePopup.hideToday.';

    // ------------------------------------------------------------------ 작은 도우미
    function el(tag, cls, text) {
        var e = document.createElement(tag);
        if (cls) e.className = cls;
        if (text != null) e.textContent = text;
        return e;
    }

    function getJson(url) {
        return fetch(url, { credentials: 'same-origin' }).then(function (r) { return r.json(); }).then(function (res) {
            if (!res.success) throw res;
            return res.data;
        });
    }

    function host(target) {
        var h = typeof target === 'string' ? document.querySelector(target) : target;
        if (!h) throw new Error('notice.js: 대상 요소를 찾을 수 없습니다. (' + target + ')');
        return h;
    }

    function go(url, info, opts) {
        if (opts && typeof opts.onNavigate === 'function') opts.onNavigate(url, info);
        else global.location.href = url;
    }

    /** "2026-10-18T09:00:00" → "2026.10.18" */
    function day(dt) {
        return dt ? String(dt).substring(0, 10).replace(/-/g, '.') : '';
    }

    /** 유형 배지 (아이콘 없이 글자만 - 색은 ADMIN 유형 관리 값) */
    function typeBadge(n) {
        var b = el('span', 'ntc-type', n.typeNm || '');
        if (n.typeColor) { b.style.color = n.typeColor; b.style.borderColor = n.typeColor; }
        return b;
    }

    /** 범위 표시: 전체공지 / 상품명 / 업무명 */
    function scopeLabel(n) {
        if (n.scopeCd === 'PRODUCT') return '상품 · ' + (n.targetNm || n.targetCd);
        if (n.scopeCd === 'BIZ') return '업무 · ' + (n.targetNm || n.targetCd);
        return '전체';
    }

    function detailUrl(id) {
        return DETAIL_URL + '?id=' + encodeURIComponent(id);
    }

    // ================================================================== ① 메인 띠 (요구사항 2·3·4)
    var NoticeStrip = {
        mount: function (target, options) {
            var opts = options || {};
            var h = host(target);
            h.hidden = true;                                   // 불러오기 전·공지 없음 → 영역째 숨김 (빈 자리 없음)
            return getJson(API + '/strip').then(function (d) {
                h.textContent = '';
                if (!d || !d.notice) return null;
                var n = d.notice;
                var a = el('a', 'ntc-strip');
                a.href = d.detailUrl;
                a.setAttribute('aria-label', '공지사항: ' + n.title + ' - 상세 보기');
                a.appendChild(el('span', 'ntc-strip-label', '공지'));       // 이어하기 카드와 구분되는 표시
                a.appendChild(el('span', 'ntc-strip-title', n.title));
                a.appendChild(el('span', 'ntc-strip-arrow', '›'));
                a.addEventListener('click', function (e) {
                    e.preventDefault();
                    go(d.detailUrl, { type: 'STRIP', noticeId: n.noticeId }, opts);
                });
                h.appendChild(a);
                h.hidden = false;
                if (typeof opts.onLoad === 'function') opts.onLoad(n);
                return n;
            }).catch(function (err) {
                h.hidden = true;                               // 실패해도 메인 화면은 그대로 (띠만 숨김)
                if (typeof opts.onError === 'function') opts.onError(err);
            });
        }
    };

    // ================================================================== ② 목록 (요구사항 1)
    var TABS = [
        { key: '', label: '전체' },
        { key: 'PRODUCT', label: '상품별' },
        { key: 'BIZ', label: '업무별' }
    ];

    var NoticeList = {
        mount: function (target, options) {
            var opts = options || {};
            var h = host(target);
            var state = { scope: opts.scope || '', target: opts.target || '', page: 1, filters: null };
            h.textContent = '';
            h.classList.add('ntc-root');

            var tabs = el('div', 'ntc-tabs');
            tabs.setAttribute('role', 'tablist');
            var chips = el('div', 'ntc-chips');
            var list = el('ul', 'ntc-list');
            var more = el('button', 'ntc-more', '더보기');
            more.type = 'button';
            more.hidden = true;
            h.appendChild(tabs);
            h.appendChild(chips);
            h.appendChild(list);
            h.appendChild(more);

            function renderTabs() {
                tabs.textContent = '';
                TABS.forEach(function (t) {
                    var cnt = !state.filters ? '' : t.key === '' ? state.filters.allCnt
                        : (t.key === 'PRODUCT' ? state.filters.products : state.filters.bizs).reduce(function (s, x) { return s + x.cnt; }, 0);
                    var b = el('button', 'ntc-tab' + (state.scope === t.key ? ' on' : ''), t.label + (cnt === '' ? '' : ' ' + cnt));
                    b.type = 'button';
                    b.setAttribute('role', 'tab');
                    b.setAttribute('aria-selected', state.scope === t.key ? 'true' : 'false');
                    b.addEventListener('click', function () { state.scope = t.key; state.target = ''; reload(); });
                    tabs.appendChild(b);
                });
            }

            function renderChips() {
                chips.textContent = '';
                if (!state.filters || !state.scope) { chips.hidden = true; return; }
                var targets = state.scope === 'PRODUCT' ? state.filters.products : state.filters.bizs;
                chips.hidden = targets.length === 0;
                var all = [{ targetCd: '', targetNm: state.scope === 'PRODUCT' ? '모든 상품' : '모든 업무' }].concat(targets);
                all.forEach(function (t) {
                    var c = el('button', 'ntc-chip' + (state.target === t.targetCd ? ' on' : ''), t.targetNm + (t.cnt ? ' ' + t.cnt : ''));
                    c.type = 'button';
                    c.setAttribute('aria-pressed', state.target === t.targetCd ? 'true' : 'false');
                    c.addEventListener('click', function () { state.target = t.targetCd; reload(); });
                    chips.appendChild(c);
                });
            }

            function item(n) {
                var li = el('li');
                var a = el('a', 'ntc-item' + (n.fixYn === 'Y' ? ' fixed' : ''));
                a.href = detailUrl(n.noticeId);
                var top = el('span', 'ntc-item-top');
                if (n.fixYn === 'Y') top.appendChild(el('span', 'ntc-fix', '중요'));
                top.appendChild(typeBadge(n));
                top.appendChild(el('span', 'ntc-scope', scopeLabel(n)));
                a.appendChild(top);
                a.appendChild(el('strong', 'ntc-item-title', n.title));
                a.appendChild(el('span', 'ntc-item-date', day(n.startAt)));
                a.addEventListener('click', function (e) {
                    e.preventDefault();
                    go(detailUrl(n.noticeId), { type: 'LIST', noticeId: n.noticeId }, opts);
                });
                li.appendChild(a);
                return li;
            }

            function load() {
                var q = '?page=' + state.page + (state.scope ? '&scope=' + state.scope : '') + (state.target ? '&target=' + encodeURIComponent(state.target) : '');
                more.disabled = true;
                return getJson(API + q).then(function (p) {
                    if (state.page === 1) list.textContent = '';
                    p.items.forEach(function (n) { list.appendChild(item(n)); });
                    if (p.total === 0) list.appendChild(el('li', 'ntc-empty', '등록된 공지사항이 없습니다.'));
                    more.hidden = !p.hasNext;
                    more.disabled = false;
                    if (typeof opts.onLoad === 'function') opts.onLoad(p, state);
                }).catch(function (err) {
                    list.textContent = '';
                    list.appendChild(el('li', 'ntc-empty', '공지사항을 불러오지 못했습니다.'));
                    if (typeof opts.onError === 'function') opts.onError(err);
                });
            }

            function reload() {
                state.page = 1;
                renderTabs();
                renderChips();
                return load();
            }

            more.addEventListener('click', function () { state.page += 1; load(); });

            return getJson(API + '/filters').then(function (f) {
                state.filters = f;
                return reload();
            }).catch(function () { return reload(); });
        }
    };

    // ================================================================== ③ 상세
    var NoticeDetail = {
        mount: function (target, options) {
            var opts = options || {};
            var h = host(target);
            var id = opts.id || new URLSearchParams(global.location.search).get('id');
            h.textContent = '';
            h.classList.add('ntc-root');
            return getJson(API + '/' + encodeURIComponent(id)).then(function (n) {
                var head = el('div', 'ntc-detail-head');
                var top = el('div', 'ntc-item-top');
                top.appendChild(typeBadge(n));
                top.appendChild(el('span', 'ntc-scope', scopeLabel(n)));
                head.appendChild(top);
                head.appendChild(el('h2', 'ntc-detail-title', n.title));
                head.appendChild(el('p', 'ntc-detail-meta', day(n.startAt) + ' · 조회 ' + n.viewCnt));
                h.appendChild(head);
                h.appendChild(el('div', 'ntc-detail-body', n.content));   // 줄바꿈은 CSS pre-wrap 으로 유지
                if (typeof opts.onLoad === 'function') opts.onLoad(n);
                return n;
            }).catch(function (err) {
                h.appendChild(el('p', 'ntc-empty', (err && err.message) || '공지사항을 불러오지 못했습니다.'));
                if (typeof opts.onError === 'function') opts.onError(err);
            });
        }
    };

    // ================================================================== ④ 메인 팝업 (신규 UI)
    //  AS-IS 배너 팝업(공통#39)과 다른 점: 유형 아이콘·좌우 화살표 없음, 버튼은 하단 한 줄 [오늘 하루 보지 않기 | 닫기],
    //  내용 아래 [자세히 보기] 텍스트 링크. 팝업 공지가 여러 건이면 닫을 때마다 다음 공지를 보여준다.
    function hiddenToday(id) {
        try {
            return global.localStorage.getItem(HIDE_KEY + id) === new Date().toDateString();
        } catch (e) { return false; }
    }

    function hideToday(id) {
        try { global.localStorage.setItem(HIDE_KEY + id, new Date().toDateString()); } catch (e) { /* 저장 불가 브라우저 - 무시 */ }
    }

    var NoticePopup = {
        /** @return Promise<{ shown: 보여준 건수 }> */
        open: function (options) {
            var opts = options || {};
            var source = opts.items ? Promise.resolve(opts.items) : getJson(API + '/popups');
            return source.then(function (items) {
                var queue = (items || []).filter(function (n) { return opts.ignoreHidden || !hiddenToday(n.noticeId); });
                var shown = 0;
                return new Promise(function (resolve) {
                    function next() {
                        var n = queue.shift();
                        if (!n) { resolve({ shown: shown }); return; }
                        shown += 1;
                        show(n, next);
                    }
                    next();
                });
            }).catch(function () { return { shown: 0 }; });

            function show(n, done) {
                var dim = el('div', 'ntc-pop-dim');
                var box = el('div', 'ntc-pop');
                box.setAttribute('role', 'dialog');
                box.setAttribute('aria-modal', 'true');
                box.setAttribute('aria-label', '공지사항');
                box.tabIndex = -1;

                var body = el('div', 'ntc-pop-body');
                body.appendChild(el('p', 'ntc-pop-kind', n.typeNm ? '공지사항 · ' + n.typeNm : '공지사항'));
                body.appendChild(el('h2', 'ntc-pop-title', n.title));
                body.appendChild(el('div', 'ntc-pop-content', n.content));
                var more = el('a', 'ntc-pop-more', '자세히 보기 ›');
                more.href = detailUrl(n.noticeId);
                body.appendChild(more);
                box.appendChild(body);

                var foot = el('div', 'ntc-pop-foot');
                var hide = el('button', 'ntc-pop-btn sub', '오늘 하루 보지 않기');
                var close = el('button', 'ntc-pop-btn main', '닫기');
                hide.type = close.type = 'button';
                foot.appendChild(hide);
                foot.appendChild(close);
                box.appendChild(foot);
                dim.appendChild(box);
                document.body.appendChild(dim);
                box.focus();

                function finish() {
                    document.removeEventListener('keydown', onKey);
                    if (dim.parentNode) dim.parentNode.removeChild(dim);
                    done();
                }
                function onKey(e) { if (e.key === 'Escape') finish(); }
                document.addEventListener('keydown', onKey);
                close.addEventListener('click', finish);
                hide.addEventListener('click', function () { hideToday(n.noticeId); finish(); });
                more.addEventListener('click', function (e) {
                    e.preventDefault();
                    document.removeEventListener('keydown', onKey);
                    if (dim.parentNode) dim.parentNode.removeChild(dim);
                    go(detailUrl(n.noticeId), { type: 'POPUP', noticeId: n.noticeId }, opts);
                });
            }
        }
    };

    /** 상세 화면 주소를 바꿀 때 (application.yaml common.notice.detail-url 과 맞춘다) */
    function setDetailUrl(url) { DETAIL_URL = url; }

    global.NoticeStrip = NoticeStrip;
    global.NoticeList = NoticeList;
    global.NoticeDetail = NoticeDetail;
    global.NoticePopup = NoticePopup;
    global.Notice = { setDetailUrl: setDetailUrl };
})(window);
