<%--
  =============================================================================
   [공통#46 샘플] 홈 - 퀵메뉴 + 전체메뉴 + ADMIN 뱅킹메뉴목록  -  GET /common/menu/sample
  -----------------------------------------------------------------------------
   좌: APP 홈 퀵메뉴(3*2) / 전체메뉴 (업무 화면은 quick-menu.js 포함 + QuickMenu.mount / mountAll)
   우: 회원구분 전환(local) + ADMIN 뱅킹메뉴목록 (전체상품 신규등록 · 표시순서 수정)
   주의: JSP 에서는 JS 템플릿 문자열의 달러+중괄호가 EL 로 해석되므로 문자열 더하기를 사용한다.
  =============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>저축은행 포털 - 퀵메뉴 · 전체메뉴 샘플</title>
    <link rel="shortcut icon" href="/favicon.ico">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css" rel="stylesheet">
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <link rel="stylesheet" href="/css/common/ui-tokens.css">
    <link rel="stylesheet" href="/css/common/ui-icon.css">
    <link rel="stylesheet" href="/css/common/quick-menu.css">
    <script src="/js/common/quick-menu.js"></script>
    <style>
        body { background-color: #f4f6f9; font-family: 'Malgun Gothic', 'Noto Sans KR', sans-serif; }
        [hidden] { display: none !important; }
        pre.result-log { background: #212529; color: #e9ecef; border-radius: 6px; padding: 10px; max-height: 240px; overflow: auto; white-space: pre-wrap; font-size: 12px; margin: 0; }
        .table-sm td, .table-sm th { font-size: 12px; vertical-align: middle; }
        .phone { width: 100%; max-width: 360px; height: 640px; margin: 0 auto; border: 10px solid #1f2937; border-radius: 36px; background: #f5f7fb; overflow: auto; }
        .phone-top { background: #fff; padding: 16px 18px 10px; display: flex; justify-content: space-between; align-items: center; }
        .phone-top b { font-size: 17px; }
        .phone-top button { border: 0; background: none; font-size: 22px; line-height: 1; padding: 4px; }
        .phone-sec { background: #fff; border-radius: 16px; margin: 12px; padding: 14px; }
        .phone-sec h6 { font-size: 15px; font-weight: 700; margin: 0 0 8px; }
        .phone-muted { font-size: 13px; color: #6b7280; }
        .order-pill { display: inline-block; min-width: 22px; text-align: center; border-radius: 11px; background: #e3ecfa; color: #0d47a1; font-weight: 700; }
    </style>
</head>
<body>
<jsp:include page="/WEB-INF/views/common/navbar.jsp" />

<div class="container-xl pb-5">
    <div class="d-flex align-items-center mb-3">
        <a href="/common/test-hub" class="btn btn-sm btn-outline-secondary me-2"><i class="bi bi-arrow-left"></i> 테스트 목록</a>
        <h5 class="fw-bold mb-0">[공통#46] 홈 - 퀵메뉴 (회원구분별 개인화 · 3*2 · 전체메뉴 전체상품)</h5>
    </div>
    <div class="alert alert-info small py-2">
        <b>AS-IS</b> 핫키 → <b>TO-BE</b> 이미지 + 메뉴명 6개(3*2) 퀵메뉴. 메뉴는 하드코딩 6종(전체상품·계좌개설·대출신청·오픈뱅킹·전자서명·서류제출)이며
        회원구분(비로그인/일반회원/금융회원)별 배치를 설정할 수 있고, 비로그인 고객은 로그인 필요 메뉴를 누르면 로그인 후 이동합니다.
        전체메뉴 &gt; 상품에 <b>전체상품</b>이 추가됩니다 (ADMIN 뱅킹메뉴목록에서 등록·순서 수정).
    </div>

    <div class="row g-4">
        <div class="col-lg-5">
            <div class="card shadow-sm">
                <div class="card-header fw-bold text-white" style="background:#0d47a1"><i class="bi bi-phone me-1"></i> 1. APP 홈 / 전체메뉴</div>
                <div class="card-body">
                    <div class="phone">
                        <div class="phone-top"><b id="phoneTitle">페퍼저축은행</b><button id="btnAllMenu" aria-label="전체메뉴 열기" aria-expanded="false"><i class="bi bi-list"></i></button></div>
                        <div id="homeView">
                            <div class="phone-sec phone-muted">상단슬라이드배너 / 계좌정보 영역 (공통#44)</div>
                            <div class="phone-sec">
                                <h6>퀵메뉴</h6>
                                <div id="quickMenu"></div>
                            </div>
                            <div class="phone-sec phone-muted">추천 상품 영역 (공통#47)</div>
                        </div>
                        <div id="allView" class="phone-sec" hidden>
                            <h6>전체메뉴</h6>
                            <div id="allMenu"></div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="col-lg-7">
            <div class="card shadow-sm mb-4">
                <div class="card-header fw-bold"><i class="bi bi-person-gear me-1"></i> 2. 회원구분 (정책2 개인화)</div>
                <div class="card-body">
                    <select id="customer" class="form-select form-select-sm">
                        <option value="">비로그인 (GUEST) - 오픈뱅킹·전자서명·서류제출은 로그인 후 이동</option>
                        <option value="customer02">customer02 - 일반회원 (MEMBER, 계좌 없음)</option>
                        <option value="customer01">customer01 - 금융회원 (FINANCIAL, 계좌 보유)</option>
                    </select>
                    <div id="segInfo" class="small mt-2">-</div>
                </div>
            </div>

            <div class="card shadow-sm mb-4">
                <div class="card-header d-flex justify-content-between align-items-center">
                    <span class="fw-bold"><i class="bi bi-list-ol me-1"></i> 3. ADMIN - 뱅킹메뉴목록</span>
                    <select id="category" class="form-select form-select-sm w-auto"></select>
                </div>
                <div class="card-body p-0">
                    <table class="table table-sm table-hover mb-0">
                        <thead class="table-light"><tr><th>ID</th><th>메뉴명</th><th>URL</th><th>전체메뉴 표시순서</th><th>사용</th><th>신규</th><th>순서 이동</th></tr></thead>
                        <tbody id="rows"><tr><td colspan="7" class="text-muted">-</td></tr></tbody>
                    </table>
                </div>
                <div class="card-footer">
                    <div class="row g-2 align-items-end">
                        <div class="col-md-3"><label class="form-label small fw-bold" for="mName">메뉴명 *</label><input id="mName" class="form-control form-control-sm" maxlength="20" value="전체상품"></div>
                        <div class="col-md-4"><label class="form-label small fw-bold" for="mUrl">URL *</label><input id="mUrl" class="form-control form-control-sm" value="/product/all"></div>
                        <div class="col-md-2"><label class="form-label small fw-bold" for="mOrder">표시순서 *</label><input id="mOrder" type="number" min="1" class="form-control form-control-sm" value="1"></div>
                        <div class="col-md-1"><div class="form-check"><input id="mNew" type="checkbox" class="form-check-input" checked><label class="form-check-label small" for="mNew">신규</label></div></div>
                        <div class="col-md-2 d-grid"><button id="btnAdd" class="btn btn-sm btn-warning fw-bold">신규등록</button></div>
                    </div>
                    <div class="small text-muted mt-1">표시순서에 끼워 넣으면 같은 카테고리의 나머지 메뉴가 뒤로 밀립니다 (1..N 자동 정리). 같은 이름은 등록 불가.</div>
                </div>
            </div>

            <div class="card shadow-sm">
                <div class="card-header">처리 결과</div>
                <div class="card-body"><pre id="log" class="result-log">-</pre></div>
            </div>
        </div>
    </div>
</div>

<script>
(function () {
    var $ = function (id) { return document.getElementById(id); };
    function log(o) { $('log').textContent = typeof o === 'string' ? o : JSON.stringify(o, null, 2); }
    function el(tag, cls, text) { var e = document.createElement(tag); if (cls) e.className = cls; if (text != null) e.textContent = text; return e; }
    function api(url, opt) {
        opt = opt || {};
        opt.credentials = 'same-origin';
        opt.headers = Object.assign({ 'X-Requested-With': 'XMLHttpRequest' }, opt.headers || {});
        return fetch(url, opt).then(function (r) { return r.json(); });
    }
    var nav = { onNavigate: function (url, item) { log('[메뉴 터치] ' + item.name + '\n이동 URL: ' + url + '\n(샘플에서는 이동하지 않고 표시만 합니다)'); } };

    // 업무 화면은 QuickMenu.mount('#quickMenu') / QuickMenu.mountAll('#allMenu') 면 된다 (params 는 local 샘플 전용)
    function loadQuick() {
        var c = $('customer').value;
        return QuickMenu.mount('#quickMenu', Object.assign({}, nav, {
            params: c ? { demoCustomer: c } : {},
            onLoad: function (d) {
                $('segInfo').textContent = '회원구분: ' + d.segmentName + ' (' + d.segment + ') · ' + d.columns + '열 × ' + d.rows + '행 · ' +
                    d.items.map(function (i) { return i.name + (i.loginFirst ? '(로그인)' : ''); }).join(', ');
            }
        }));
    }
    function loadAll() { return QuickMenu.mountAll('#allMenu', nav); }

    $('btnAllMenu').addEventListener('click', function () {
        var open = $('allView').hidden;
        $('allView').hidden = !open; $('homeView').hidden = open;
        $('btnAllMenu').setAttribute('aria-expanded', String(open));
        $('btnAllMenu').setAttribute('aria-label', open ? '전체메뉴 닫기' : '전체메뉴 열기');
        if (open) loadAll();
    });
    $('customer').addEventListener('change', loadQuick);

    // ADMIN 뱅킹메뉴목록
    function category() { return $('category').value; }
    function loadList() {
        return api('/api/common/menu-admin/banking-menus?category=' + category()).then(function (res) {
            var tb = $('rows'); tb.textContent = '';
            if (!res.success) { log(res); return; }
            res.data.forEach(function (m, i, arr) {
                var tr = el('tr');
                tr.appendChild(el('td', null, String(m.id)));
                tr.appendChild(el('td', 'fw-bold', m.name));
                tr.appendChild(el('td', null, m.url));
                var o = el('td'); o.appendChild(el('span', 'order-pill', String(m.displayOrder))); tr.appendChild(o);
                tr.appendChild(el('td', null, m.use ? 'Y' : 'N'));
                tr.appendChild(el('td', null, m.newBadge ? 'N' : '-'));
                var act = el('td', 'text-nowrap');
                [['▲', -1], ['▼', 1]].forEach(function (b) {
                    var btn = el('button', 'btn btn-outline-secondary btn-sm py-0 px-2 me-1', b[0]);
                    btn.setAttribute('aria-label', m.name + (b[1] < 0 ? ' 위로' : ' 아래로'));
                    btn.disabled = (b[1] < 0 && i === 0) || (b[1] > 0 && i === arr.length - 1);
                    btn.addEventListener('click', function () {
                        var ids = arr.map(function (x) { return x.id; });
                        var j = i + b[1]; var t = ids[i]; ids[i] = ids[j]; ids[j] = t;
                        api('/api/common/menu-admin/banking-menus/reorder?category=' + category(), {
                            method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(ids)
                        }).then(function (r) { log(r); loadList(); if (!$('allView').hidden) loadAll(); });
                    });
                    act.appendChild(btn);
                });
                tr.appendChild(act);
                tb.appendChild(tr);
            });
        });
    }
    $('category').addEventListener('change', loadList);
    $('btnAdd').addEventListener('click', function () {
        api('/api/common/menu-admin/banking-menus', {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ category: category(), name: $('mName').value, url: $('mUrl').value,
                displayOrder: $('mOrder').value === '' ? null : Number($('mOrder').value), newBadge: $('mNew').checked })
        }).then(function (r) { log(r); loadList(); if (!$('allView').hidden) loadAll(); });
    });

    api('/api/common/menu-admin/categories').then(function (res) {
        if (!res.success) { log(res); return; }
        Object.keys(res.data).forEach(function (k) { var o = el('option', null, res.data[k] + ' (' + k + ')'); o.value = k; $('category').appendChild(o); });
        loadList();
    });
    loadQuick();
})();
</script>
</body>
</html>
