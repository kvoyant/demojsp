<%--
  =============================================================================
   [CF-BC-20 / 공통#28] 디지털어드민 > 고객 > 공지사항 관리  -  GET /common/notice/admin
  -----------------------------------------------------------------------------
   요구사항 (REQ-AP-610)
     1. 전체 / 상품별 / 업무별 구분하여 등록할 수 있도록 설정기능  → 구분 + 대상(상품 / 업무) 선택
     2. 공지사항 유형 AS-IS 업무공지·서비스공지·시스템점검·기타, TO-BE 검토필요 → [공지 유형] 탭에서 추가·변경·미사용
     3. 공지사항 배너 팝업 신규 UI                                → "메인 팝업" 체크 (사용자 화면은 NoticePopup)
   추가 설정: 메인 띠 노출(요구사항 APP 2), 상단 고정, 게시기간(예약·종료), 사용여부
   업무별 공지의 업무 목록은 [업무 구분] 탭에서 관리한다.
   주의: JSP 에서는 JS 템플릿 문자열의 달러+중괄호가 EL 로 해석되므로 문자열 더하기를 사용한다.
  =============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>디지털어드민 - 공지사항 관리</title>
    <link rel="shortcut icon" href="/favicon.ico">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css" rel="stylesheet">
    <link rel="stylesheet" href="/css/common/ui-tokens.css">
    <link rel="stylesheet" href="/css/common/ui-alert.css">
    <script src="/js/common/ui-alert.js"></script>
    <style>
        body { background-color: #f4f6f9; font-family: 'Malgun Gothic', 'Noto Sans KR', sans-serif; }
        [hidden] { display: none !important; }
        .table-sm td, .table-sm th { font-size: 12px; vertical-align: middle; }
        .table-sm th { white-space: nowrap; }
        .form-control-sm, .form-select-sm { font-size: 12px; }
        tr.sel td { background: #e3ecfa !important; }
        tr.clickable { cursor: pointer; }
        .state { font-size: 11px; padding: 1px 6px; border-radius: 8px; white-space: nowrap; }
        .st-게시중 { background: #e6f4ea; color: #1b7a3d; } .st-게시예약 { background: #e3ecfa; color: #0d47a1; }
        .st-게시종료, .st-미사용 { background: #f3f4f6; color: #6b7280; }
        .type { font-size: 11px; font-weight: 700; border: 1px solid currentColor; border-radius: 4px; padding: 0 4px; white-space: nowrap; }
        .flag { font-size: 10px; background: #fff4e0; color: #b45309; border-radius: 4px; padding: 0 4px; margin-left: 2px; }
        textarea.content { min-height: 200px; font-size: 12px; }
        .nav-tabs .nav-link { font-size: 13px; }
    </style>
</head>
<body>
<jsp:include page="/WEB-INF/views/common/navbar.jsp" />

<div class="container-xl pb-5">
    <div class="d-flex align-items-center mb-2">
        <a href="/common/test-hub" class="btn btn-sm btn-outline-secondary me-2"><i class="bi bi-arrow-left"></i> 테스트 목록</a>
        <h5 class="fw-bold mb-0">[공통#28] 디지털어드민 &gt; 고객 &gt; 공지사항 관리</h5>
        <a href="/common/notice/sample" class="btn btn-sm btn-outline-primary ms-auto"><i class="bi bi-phone"></i> 사용자 화면</a>
    </div>
    <div class="alert alert-info small py-2">
        <b>구분</b>(전체 / 상품별 / 업무별)과 <b>대상</b>을 정해 등록합니다. <b>메인 띠</b>를 체크한 공지 중 게시 시작이 가장 최근인 1건이 APP 메인 띠에 나오고,
        <b>메인 팝업</b>을 체크하면 메인 진입 시 팝업으로 뜹니다. 공지 유형은 <b>[공지 유형]</b> 탭에서 추가·변경할 수 있습니다 (TO-BE 확정 시 반영).
    </div>

    <ul class="nav nav-tabs mb-3" id="tabs">
        <li class="nav-item"><button class="nav-link active" data-tab="notice">공지사항</button></li>
        <li class="nav-item"><button class="nav-link" data-tab="type">공지 유형</button></li>
        <li class="nav-item"><button class="nav-link" data-tab="biz">업무 구분</button></li>
    </ul>

    <!-- ================================================================ 탭1 공지사항 -->
    <section id="tab-notice">
        <div class="row g-3">
            <div class="col-lg-7">
                <div class="card shadow-sm mb-2"><div class="card-body py-2 d-flex gap-2">
                    <select id="fScope" class="form-select form-select-sm" style="max-width:120px">
                        <option value="">전체 구분</option><option value="ALL">전체공지</option><option value="PRODUCT">상품별</option><option value="BIZ">업무별</option>
                    </select>
                    <select id="fType" class="form-select form-select-sm" style="max-width:140px"><option value="">모든 유형</option></select>
                    <input id="fQ" class="form-control form-control-sm" placeholder="제목 검색">
                    <button class="btn btn-sm btn-primary text-nowrap" id="btnSearch">조회</button>
                </div></div>
                <div class="card shadow-sm"><div class="card-body p-0">
                    <table class="table table-sm table-hover mb-0">
                        <thead class="table-light"><tr><th>번호</th><th>유형</th><th>구분 · 대상</th><th>제목</th><th>게시 시작</th><th>상태</th><th>조회</th></tr></thead>
                        <tbody id="rows"></tbody>
                    </table>
                </div></div>
            </div>
            <div class="col-lg-5">
                <div class="card shadow-sm">
                    <div class="card-header fw-bold d-flex">공지 등록 / 수정 <span id="mode" class="badge bg-success ms-2">신규</span>
                        <button class="btn btn-sm btn-link ms-auto p-0" id="btnNew">새 공지</button></div>
                    <div class="card-body">
                        <div class="row g-2">
                            <div class="col-12">
                                <label class="small d-block">구분 (요구사항 1)</label>
                                <div class="btn-group btn-group-sm w-100" role="group">
                                    <input type="radio" class="btn-check" name="scope" id="scALL" value="ALL" checked><label class="btn btn-outline-primary" for="scALL">전체</label>
                                    <input type="radio" class="btn-check" name="scope" id="scPRODUCT" value="PRODUCT"><label class="btn btn-outline-primary" for="scPRODUCT">상품별</label>
                                    <input type="radio" class="btn-check" name="scope" id="scBIZ" value="BIZ"><label class="btn btn-outline-primary" for="scBIZ">업무별</label>
                                </div>
                            </div>
                            <div class="col-6"><label class="small">대상</label><select id="nTarget" class="form-select form-select-sm" disabled><option value="">(전체공지 - 대상 없음)</option></select></div>
                            <div class="col-6"><label class="small">유형 (요구사항 2)</label><select id="nType" class="form-select form-select-sm"></select></div>
                            <div class="col-12"><label class="small">제목 (100자, 메인 띠에 노출)</label><input id="nTitle" class="form-control form-control-sm" maxlength="100"></div>
                            <div class="col-12"><label class="small">내용</label><textarea id="nContent" class="form-control content"></textarea></div>
                            <div class="col-6"><label class="small">게시 시작</label><input id="nStart" type="datetime-local" class="form-control form-control-sm"></div>
                            <div class="col-6"><label class="small">게시 종료 (비우면 계속)</label><input id="nEnd" type="datetime-local" class="form-control form-control-sm"></div>
                            <div class="col-12 small">
                                <label class="me-3"><input type="checkbox" id="nStrip"> 메인 띠</label>
                                <label class="me-3"><input type="checkbox" id="nPopup"> 메인 팝업</label>
                                <label class="me-3"><input type="checkbox" id="nFix"> 상단 고정</label>
                                <label>사용 <select id="nUse" class="form-select form-select-sm d-inline-block" style="width:auto"><option>Y</option><option>N</option></select></label>
                            </div>
                        </div>
                        <button class="btn btn-sm btn-primary mt-2" id="btnSave">저장</button>
                    </div>
                </div>
            </div>
        </div>
    </section>

    <!-- ================================================================ 탭2 공지 유형 -->
    <section id="tab-type" hidden>
        <div class="row g-3">
            <div class="col-lg-7"><div class="card shadow-sm"><div class="card-body p-0">
                <table class="table table-sm table-hover mb-0">
                    <thead class="table-light"><tr><th>순서</th><th>유형코드</th><th>유형명</th><th>표시</th><th>사용</th><th>공지 수</th></tr></thead>
                    <tbody id="typeRows"></tbody>
                </table>
            </div></div>
            <div class="small text-muted mt-2">AS-IS: 업무공지 · 서비스공지 · 시스템점검 · 기타 / TO-BE 는 검토 중 - 확정되면 여기서 추가하거나 이름을 바꾸고, 쓰지 않는 유형은 미사용으로 둡니다 (기존 공지는 그대로 표시).</div></div>
            <div class="col-lg-5"><div class="card shadow-sm">
                <div class="card-header fw-bold d-flex">유형 등록 / 수정 <button class="btn btn-sm btn-link ms-auto p-0" id="typeNew">새 유형</button></div>
                <div class="card-body"><div class="row g-2">
                    <div class="col-6"><input id="tCd" class="form-control form-control-sm" placeholder="유형코드 (예: EVENT)"></div>
                    <div class="col-6"><input id="tNm" class="form-control form-control-sm" placeholder="유형명" maxlength="20"></div>
                    <div class="col-4"><input id="tColor" type="color" class="form-control form-control-sm form-control-color w-100" value="#0D47A1"></div>
                    <div class="col-4"><input id="tOrd" type="number" min="1" max="99" class="form-control form-control-sm" placeholder="순서"></div>
                    <div class="col-4"><select id="tUse" class="form-select form-select-sm"><option>Y</option><option>N</option></select></div>
                </div><button class="btn btn-sm btn-primary mt-2" id="typeSave">저장</button></div>
            </div></div>
        </div>
    </section>

    <!-- ================================================================ 탭3 업무 구분 -->
    <section id="tab-biz" hidden>
        <div class="row g-3">
            <div class="col-lg-7"><div class="card shadow-sm"><div class="card-body p-0">
                <table class="table table-sm table-hover mb-0">
                    <thead class="table-light"><tr><th>순서</th><th>업무코드</th><th>업무명</th><th>사용</th></tr></thead>
                    <tbody id="bizRows"></tbody>
                </table>
            </div></div></div>
            <div class="col-lg-5"><div class="card shadow-sm">
                <div class="card-header fw-bold d-flex">업무 등록 / 수정 <button class="btn btn-sm btn-link ms-auto p-0" id="bizNew">새 업무</button></div>
                <div class="card-body"><div class="row g-2">
                    <div class="col-6"><input id="bCd" class="form-control form-control-sm" placeholder="업무코드 (예: CARD)"></div>
                    <div class="col-6"><input id="bNm" class="form-control form-control-sm" placeholder="업무명" maxlength="30"></div>
                    <div class="col-6"><input id="bOrd" type="number" min="1" max="99" class="form-control form-control-sm" placeholder="순서"></div>
                    <div class="col-6"><select id="bUse" class="form-select form-select-sm"><option>Y</option><option>N</option></select></div>
                </div><button class="btn btn-sm btn-primary mt-2" id="bizSave">저장</button></div>
            </div></div>
        </div>
    </section>
</div>

<script>
(function () {
    var API = '/api/common/notice-admin';
    var $ = function (id) { return document.getElementById(id); };
    var targets = { products: [], bizs: [] };
    var cur = null;          // 수정 중인 공지 ID

    // ------------------------------------------------------------------ 공통
    function api(method, url, body) {
        return fetch(API + url, {
            method: method,
            headers: body ? { 'Content-Type': 'application/json' } : {},
            body: body ? JSON.stringify(body) : undefined
        }).then(function (r) { return r.json(); }).then(function (res) {
            if (!res.success) { UiAlert.alert(res.message, { type: 'error' }); throw res; }
            return res;
        });
    }
    function td(tr, v, cls) {
        var c = document.createElement('td');
        if (v instanceof Node) c.appendChild(v); else c.textContent = v == null ? '' : v;
        if (cls) c.className = cls;
        tr.appendChild(c);
        return c;
    }
    function span(cls, text) { var s = document.createElement('span'); s.className = cls; s.textContent = text; return s; }
    function dt(v) { return v ? String(v).substring(0, 16) : ''; }                 // datetime-local 값 (yyyy-MM-ddTHH:mm)
    function nowLocal() { var d = new Date(Date.now() - new Date().getTimezoneOffset() * 60000); return d.toISOString().substring(0, 16); }
    function scope() { return document.querySelector('input[name=scope]:checked').value; }
    function scopeText(n) { return n.scopeCd === 'ALL' ? '전체' : (n.scopeCd === 'PRODUCT' ? '상품 · ' : '업무 · ') + (n.targetNm || n.targetCd); }

    document.querySelectorAll('#tabs .nav-link').forEach(function (b) {
        b.addEventListener('click', function () {
            document.querySelectorAll('#tabs .nav-link').forEach(function (x) { x.classList.toggle('active', x === b); });
            ['notice', 'type', 'biz'].forEach(function (t) { $('tab-' + t).hidden = t !== b.dataset.tab; });
        });
    });

    // ------------------------------------------------------------------ 구분 → 대상 선택 상자
    function fillTargets(value) {
        var s = $('nTarget'), sc = scope();
        s.length = 0;
        if (sc === 'ALL') {
            s.add(new Option('(전체공지 - 대상 없음)', ''));
            s.disabled = true;
            return;
        }
        s.disabled = false;
        (sc === 'PRODUCT' ? targets.products : targets.bizs).forEach(function (t) { s.add(new Option(t.targetNm + ' (' + t.targetCd + ')', t.targetCd)); });
        if (value) s.value = value;
    }
    document.querySelectorAll('input[name=scope]').forEach(function (r) { r.addEventListener('change', function () { fillTargets(); }); });

    // ------------------------------------------------------------------ 탭1 공지 목록 · 폼
    function loadList() {
        var q = '?scope=' + $('fScope').value + '&type=' + $('fType').value + '&q=' + encodeURIComponent($('fQ').value.trim());
        return api('GET', '/notices' + q).then(function (res) {
            var tb = $('rows');
            tb.textContent = '';
            res.data.forEach(function (n) {
                var tr = document.createElement('tr');
                tr.className = 'clickable' + (n.noticeId === cur ? ' sel' : '');
                td(tr, n.noticeId);
                var t = span('type', n.typeNm || n.typeCd); t.style.color = n.typeColor || '#4b5563';
                td(tr, t);
                td(tr, scopeText(n));
                var title = document.createElement('span');
                title.appendChild(document.createTextNode(n.title));
                if (n.stripYn === 'Y') title.appendChild(span('flag', '띠'));
                if (n.popupYn === 'Y') title.appendChild(span('flag', '팝업'));
                if (n.fixYn === 'Y') title.appendChild(span('flag', '고정'));
                td(tr, title);
                td(tr, dt(n.startAt).replace('T', ' '));
                td(tr, span('state st-' + n.status, n.status));
                td(tr, n.viewCnt);
                tr.addEventListener('click', function () { edit(n.noticeId); });
                tb.appendChild(tr);
            });
        });
    }
    function setForm(n) {
        cur = n ? n.noticeId : null;
        $('mode').textContent = n ? '수정 #' + n.noticeId + ' · ' + n.status : '신규';
        $('mode').className = 'badge ms-2 ' + (n ? 'bg-warning text-dark' : 'bg-success');
        $('sc' + (n ? n.scopeCd : 'ALL')).checked = true;
        fillTargets(n && n.targetCd);
        $('nType').value = n ? n.typeCd : $('nType').options[0] && $('nType').options[0].value;
        $('nTitle').value = n ? n.title : '';
        $('nContent').value = n ? n.content : '';
        $('nStart').value = n ? dt(n.startAt) : nowLocal();
        $('nEnd').value = n ? dt(n.endAt) : '';
        $('nStrip').checked = !!n && n.stripYn === 'Y';
        $('nPopup').checked = !!n && n.popupYn === 'Y';
        $('nFix').checked = !!n && n.fixYn === 'Y';
        $('nUse').value = n ? n.useYn : 'Y';
    }
    function edit(id) {
        api('GET', '/notices/' + id).then(function (res) { setForm(res.data); loadList(); });
    }
    $('btnNew').addEventListener('click', function () { setForm(null); loadList(); });
    $('btnSearch').addEventListener('click', loadList);
    $('btnSave').addEventListener('click', function () {
        api('POST', '/notices', {
            noticeId: cur, scopeCd: scope(), targetCd: $('nTarget').value || null, typeCd: $('nType').value,
            title: $('nTitle').value, content: $('nContent').value,
            startAt: $('nStart').value || null, endAt: $('nEnd').value || null,
            stripYn: $('nStrip').checked ? 'Y' : 'N', popupYn: $('nPopup').checked ? 'Y' : 'N', fixYn: $('nFix').checked ? 'Y' : 'N',
            useYn: $('nUse').value
        }).then(function (res) {
            UiAlert.alert(res.message);
            setForm(res.data);
            loadList();
        });
    });

    // ------------------------------------------------------------------ 탭2 공지 유형
    function loadTypes() {
        return api('GET', '/types').then(function (res) {
            var tb = $('typeRows'), ft = $('fType'), nt = $('nType'), keepF = ft.value, keepN = nt.value;
            tb.textContent = '';
            ft.length = 1;
            nt.length = 0;
            res.data.forEach(function (t) {
                var tr = document.createElement('tr');
                tr.className = 'clickable';
                td(tr, t.sortOrd); td(tr, t.typeCd);
                var s = span('type', t.typeNm); s.style.color = t.badgeColor;
                td(tr, t.typeNm); td(tr, s); td(tr, t.useYn); td(tr, t.noticeCnt);
                tr.addEventListener('click', function () {
                    $('tCd').value = t.typeCd; $('tCd').readOnly = true; $('tNm').value = t.typeNm;
                    $('tColor').value = t.badgeColor; $('tOrd').value = t.sortOrd; $('tUse').value = t.useYn;
                });
                tb.appendChild(tr);
                ft.add(new Option(t.typeNm, t.typeCd));
                nt.add(new Option(t.typeNm + (t.useYn === 'Y' ? '' : ' (미사용)'), t.typeCd));
            });
            ft.value = keepF;
            if (keepN) nt.value = keepN;
        });
    }
    $('typeNew').addEventListener('click', function () {
        $('tCd').readOnly = false; $('tCd').value = ''; $('tNm').value = ''; $('tOrd').value = ''; $('tUse').value = 'Y';
    });
    $('typeSave').addEventListener('click', function () {
        api('POST', '/types', { typeCd: $('tCd').value.trim(), typeNm: $('tNm').value, badgeColor: $('tColor').value.toUpperCase(),
                                sortOrd: parseInt($('tOrd').value, 10), useYn: $('tUse').value })
            .then(function (res) { UiAlert.alert(res.message); loadTypes(); });
    });

    // ------------------------------------------------------------------ 탭3 업무 구분
    function loadBizs() {
        return api('GET', '/bizs').then(function (res) {
            var tb = $('bizRows');
            tb.textContent = '';
            res.data.forEach(function (b) {
                var tr = document.createElement('tr');
                tr.className = 'clickable';
                td(tr, b.sortOrd); td(tr, b.bizCd); td(tr, b.bizNm); td(tr, b.useYn);
                tr.addEventListener('click', function () {
                    $('bCd').value = b.bizCd; $('bCd').readOnly = true; $('bNm').value = b.bizNm; $('bOrd').value = b.sortOrd; $('bUse').value = b.useYn;
                });
                tb.appendChild(tr);
            });
        });
    }
    $('bizNew').addEventListener('click', function () { $('bCd').readOnly = false; $('bCd').value = ''; $('bNm').value = ''; $('bOrd').value = ''; $('bUse').value = 'Y'; });
    $('bizSave').addEventListener('click', function () {
        api('POST', '/bizs', { bizCd: $('bCd').value.trim(), bizNm: $('bNm').value, sortOrd: parseInt($('bOrd').value, 10), useYn: $('bUse').value })
            .then(function (res) { UiAlert.alert(res.message); loadBizs(); return loadTargets(); });
    });

    function loadTargets() {
        return api('GET', '/targets').then(function (res) { targets = res.data; fillTargets($('nTarget').value); });
    }

    // ------------------------------------------------------------------ 시작
    Promise.all([loadTypes(), loadTargets(), loadBizs()]).then(function () {
        setForm(null);
        loadList();
    });
})();
</script>
</body>
</html>
