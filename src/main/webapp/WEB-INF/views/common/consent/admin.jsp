<%--
  =============================================================================
   [CF-BC-04 / 공통#22] 디지털어드민 약관관리 (DB 방식)  -  GET /common/consent/admin
  -----------------------------------------------------------------------------
   탭1 업무별 약관 : 업무코드 등록/수정, 업무마다 약관 연결 (필수/선택 · 표시순서 · 사용여부), 기준일 미리보기
   탭2 약관        : 약관 등록 / 개정 (같은 코드 → 다음 버전, 시행일부터 사용자 화면에 적용), 버전 이력
   탭3 동의내역    : 업무·회원별 조회, 약관별 동의여부·버전·전문 해시(증적)
   저장 즉시 사용자 화면(/common/consent/sample)에 반영된다 (캐시 없음).
   주의: JSP 에서는 JS 템플릿 문자열의 달러+중괄호가 EL 로 해석되므로 문자열 더하기를 사용한다.
  =============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>디지털어드민 - 약관관리</title>
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
        tr.sel td { background: #e3ecfa !important; }
        tr.clickable { cursor: pointer; }
        tr.off td { color: #9ca3af; }
        .form-control-sm, .form-select-sm { font-size: 12px; }
        textarea.terms-cn { font-size: 12px; min-height: 220px; }
        .preview { background: #fff; border: 1px solid #e5e7eb; border-radius: 12px; padding: 12px; font-size: 12px; }
        .preview h6 { font-size: 12px; font-weight: 700; margin: 8px 0 4px; }
        .preview li { margin-bottom: 2px; }
        .hash { font-family: monospace; font-size: 11px; color: #6b7280; }
        .nav-tabs .nav-link { font-size: 13px; }
    </style>
</head>
<body>
<jsp:include page="/WEB-INF/views/common/navbar.jsp" />

<div class="container-xl pb-5">
    <div class="d-flex align-items-center mb-2">
        <a href="/common/test-hub" class="btn btn-sm btn-outline-secondary me-2"><i class="bi bi-arrow-left"></i> 테스트 목록</a>
        <h5 class="fw-bold mb-0">[공통#22] 디지털어드민 &gt; 업무관리 &gt; 약관관리 (DB)</h5>
        <a href="/common/consent/sample" class="btn btn-sm btn-outline-primary ms-auto"><i class="bi bi-phone"></i> 사용자 화면</a>
    </div>
    <div class="alert alert-info small py-2">
        약관은 <b>버전 단위</b>로 쌓입니다. 개정하면 새 버전이 추가되고, <b>시행일</b>이 되면 사용자 화면이 자동으로 새 버전을 보여줍니다 (이전 버전·동의내역은 그대로 보관).
        업무마다 어떤 약관을 <b>필수/선택</b>으로 몇 번째에 보여줄지는 <b>업무별 약관</b>에서 정합니다.
    </div>

    <ul class="nav nav-tabs mb-3" id="tabs">
        <li class="nav-item"><button class="nav-link active" data-tab="biz">업무별 약관</button></li>
        <li class="nav-item"><button class="nav-link" data-tab="terms">약관</button></li>
        <li class="nav-item"><button class="nav-link" data-tab="agree">동의내역</button></li>
    </ul>

    <!-- ================================================================ 탭1 업무별 약관 -->
    <section id="tab-biz">
        <div class="row g-3">
            <div class="col-lg-5">
                <div class="card shadow-sm mb-3">
                    <div class="card-header fw-bold">업무코드</div>
                    <div class="card-body p-0">
                        <table class="table table-sm table-hover mb-0">
                            <thead class="table-light"><tr><th>업무코드</th><th>업무명</th><th>필수</th><th>선택</th><th>사용</th></tr></thead>
                            <tbody id="bizRows"></tbody>
                        </table>
                    </div>
                </div>
                <div class="card shadow-sm">
                    <div class="card-header fw-bold d-flex">업무코드 등록 / 수정
                        <button class="btn btn-sm btn-link ms-auto p-0" id="bizNew">새 업무</button></div>
                    <div class="card-body">
                        <div class="row g-2">
                            <div class="col-5"><input id="fBizCd" class="form-control form-control-sm" placeholder="업무코드 (예: CARD_APPLY)"></div>
                            <div class="col-5"><input id="fBizNm" class="form-control form-control-sm" placeholder="업무명"></div>
                            <div class="col-2"><select id="fBizUse" class="form-select form-select-sm"><option>Y</option><option>N</option></select></div>
                            <div class="col-12"><input id="fBizDesc" class="form-control form-control-sm" placeholder="설명 (사용 화면)"></div>
                        </div>
                        <button class="btn btn-sm btn-primary mt-2" id="bizSave">업무코드 저장</button>
                    </div>
                </div>
            </div>
            <div class="col-lg-7">
                <div class="card shadow-sm mb-3">
                    <div class="card-header fw-bold d-flex align-items-center">업무별 약관 <span id="mapTitle" class="ms-2 badge bg-primary"></span>
                        <button class="btn btn-sm btn-outline-secondary ms-auto me-1" id="mapAdd">+ 약관 추가</button>
                        <button class="btn btn-sm btn-primary" id="mapSave">저장</button></div>
                    <div class="card-body p-0">
                        <table class="table table-sm mb-0">
                            <thead class="table-light"><tr><th>약관</th><th style="width:90px">필수여부</th><th style="width:70px">순서</th><th style="width:70px">사용</th><th>현재 버전</th><th></th></tr></thead>
                            <tbody id="mapRows"></tbody>
                        </table>
                    </div>
                </div>
                <div class="card shadow-sm">
                    <div class="card-header fw-bold d-flex align-items-center">사용자 화면 미리보기
                        <input type="date" id="baseDt" class="form-control form-control-sm ms-auto" style="max-width:150px">
                        <button class="btn btn-sm btn-outline-primary ms-1" id="btnPreview">기준일 조회</button></div>
                    <div class="card-body"><div id="preview" class="preview">-</div></div>
                </div>
            </div>
        </div>
    </section>

    <!-- ================================================================ 탭2 약관 -->
    <section id="tab-terms" hidden>
        <div class="row g-3">
            <div class="col-lg-6">
                <div class="card shadow-sm mb-3">
                    <div class="card-header fw-bold">약관 목록 (코드별 최신 버전)</div>
                    <div class="card-body p-0">
                        <table class="table table-sm table-hover mb-0">
                            <thead class="table-light"><tr><th>약관코드</th><th>약관명</th><th>최신</th><th>현재 시행</th><th>최신 시행일</th></tr></thead>
                            <tbody id="termsRows"></tbody>
                        </table>
                    </div>
                </div>
                <div class="card shadow-sm">
                    <div class="card-header fw-bold">버전 이력 <span id="verTitle" class="badge bg-secondary"></span></div>
                    <div class="card-body p-0">
                        <table class="table table-sm mb-0">
                            <thead class="table-light"><tr><th>버전</th><th>시행일</th><th>등록</th><th>전문 해시</th></tr></thead>
                            <tbody id="verRows"><tr><td colspan="4" class="text-muted">약관을 선택하세요.</td></tr></tbody>
                        </table>
                    </div>
                </div>
            </div>
            <div class="col-lg-6">
                <div class="card shadow-sm">
                    <div class="card-header fw-bold d-flex">약관 등록 / 개정 <span id="termsMode" class="badge bg-success ms-2">신규</span>
                        <button class="btn btn-sm btn-link ms-auto p-0" id="termsNew">새 약관</button></div>
                    <div class="card-body">
                        <div class="row g-2">
                            <div class="col-5"><input id="fTermsCd" class="form-control form-control-sm" placeholder="약관코드 (예: CARD_BASIC)"></div>
                            <div class="col-7"><input id="fTermsNm" class="form-control form-control-sm" placeholder="약관명"></div>
                            <div class="col-12"><textarea id="fTermsCn" class="form-control terms-cn" placeholder="약관 전문"></textarea></div>
                            <div class="col-7"><input id="fPdfUrl" class="form-control form-control-sm" placeholder="PDF URL (선택, / 또는 https://)"></div>
                            <div class="col-5"><input id="fEffectDt" type="date" class="form-control form-control-sm" title="시행일"></div>
                        </div>
                        <div class="small text-muted mt-2">기존 약관코드로 저장하면 <b>다음 버전</b>으로 추가됩니다. 시행일은 이전 버전 시행일과 같거나 이후여야 합니다.</div>
                        <button class="btn btn-sm btn-primary mt-2" id="termsSave">저장</button>
                    </div>
                </div>
            </div>
        </div>
    </section>

    <!-- ================================================================ 탭3 동의내역 -->
    <section id="tab-agree" hidden>
        <div class="card shadow-sm mb-3">
            <div class="card-body py-2 d-flex gap-2 align-items-center">
                <select id="qBiz" class="form-select form-select-sm" style="max-width:220px"><option value="">전체 업무</option></select>
                <input id="qCust" class="form-control form-control-sm" style="max-width:180px" placeholder="회원 ID">
                <button class="btn btn-sm btn-primary" id="btnAgree">조회</button>
            </div>
        </div>
        <div class="row g-3">
            <div class="col-lg-6">
                <div class="card shadow-sm"><div class="card-body p-0">
                    <table class="table table-sm table-hover mb-0">
                        <thead class="table-light"><tr><th>동의번호</th><th>업무</th><th>회원</th><th>채널</th><th>동의</th><th>일시</th></tr></thead>
                        <tbody id="agreeRows"></tbody>
                    </table>
                </div></div>
            </div>
            <div class="col-lg-6">
                <div class="card shadow-sm">
                    <div class="card-header fw-bold">상세 (증적) <span id="agreeTitle" class="badge bg-secondary"></span></div>
                    <div class="card-body p-0">
                        <table class="table table-sm mb-0">
                            <thead class="table-light"><tr><th>약관</th><th>구분</th><th>버전</th><th>동의</th><th>열람</th><th>전문 해시</th></tr></thead>
                            <tbody id="agreeDtl"><tr><td colspan="6" class="text-muted">동의내역을 선택하세요.</td></tr></tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    </section>
</div>

<script>
(function () {
    var API = '/api/common/consent-admin';
    var $ = function (id) { return document.getElementById(id); };
    var termsAll = [];      // 약관 목록 (업무별 약관 선택 상자용)
    var curBiz = null;      // 선택한 업무코드

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
    function td(tr, text, cls) {
        var c = document.createElement('td');
        c.textContent = text == null ? '' : text;
        if (cls) c.className = cls;
        tr.appendChild(c);
        return c;
    }
    function shortHash(h) { return h ? h.substring(0, 12) + '…' : '(첫 동의 때 계산)'; }
    function fmt(dt) { return dt ? String(dt).replace('T', ' ') : ''; }

    // 탭 전환
    document.querySelectorAll('#tabs .nav-link').forEach(function (b) {
        b.addEventListener('click', function () {
            document.querySelectorAll('#tabs .nav-link').forEach(function (x) { x.classList.toggle('active', x === b); });
            ['biz', 'terms', 'agree'].forEach(function (t) { $('tab-' + t).hidden = t !== b.dataset.tab; });
            if (b.dataset.tab === 'terms') loadTerms();
            if (b.dataset.tab === 'agree') loadAgree();
        });
    });

    // ------------------------------------------------------------------ 탭1 업무코드
    function loadBiz(select) {
        return api('GET', '/biz').then(function (res) {
            var tb = $('bizRows');
            tb.textContent = '';
            var q = $('qBiz'), keep = q.value;
            q.length = 1;
            res.data.forEach(function (b) {
                var tr = document.createElement('tr');
                tr.className = 'clickable' + (b.useYn === 'N' ? ' off' : '') + (b.bizCd === curBiz ? ' sel' : '');
                td(tr, b.bizCd); td(tr, b.bizNm); td(tr, b.reqCnt); td(tr, b.optCnt); td(tr, b.useYn);
                tr.addEventListener('click', function () { selectBiz(b); });
                tb.appendChild(tr);
                var o = document.createElement('option');
                o.value = b.bizCd; o.textContent = b.bizNm + ' (' + b.bizCd + ')';
                q.appendChild(o);
            });
            q.value = keep;
            if (select) {
                var first = res.data.filter(function (b) { return b.bizCd === select; })[0] || res.data[0];
                if (first) selectBiz(first);
            }
        });
    }

    function selectBiz(b) {
        curBiz = b.bizCd;
        $('fBizCd').value = b.bizCd; $('fBizCd').readOnly = true;
        $('fBizNm').value = b.bizNm; $('fBizDesc').value = b.bizDesc || ''; $('fBizUse').value = b.useYn;
        $('mapTitle').textContent = b.bizNm + ' (' + b.bizCd + ')';
        document.querySelectorAll('#bizRows tr').forEach(function (tr) {
            tr.classList.toggle('sel', tr.firstChild.textContent === b.bizCd);
        });
        loadMap();
        preview();
    }

    $('bizNew').addEventListener('click', function () {
        $('fBizCd').readOnly = false;
        ['fBizCd', 'fBizNm', 'fBizDesc'].forEach(function (id) { $(id).value = ''; });
        $('fBizUse').value = 'Y';
        $('fBizCd').focus();
    });
    $('bizSave').addEventListener('click', function () {
        api('POST', '/biz', { bizCd: $('fBizCd').value.trim(), bizNm: $('fBizNm').value, bizDesc: $('fBizDesc').value, useYn: $('fBizUse').value })
            .then(function (res) { curBiz = res.data.bizCd; UiAlert.alert(res.message); return loadBiz(res.data.bizCd); });
    });

    // ------------------------------------------------------------------ 탭1 업무별 약관
    function termsOptions(selected) {
        var s = document.createElement('select');
        s.className = 'form-select form-select-sm f-terms';
        termsAll.forEach(function (t) {
            var o = document.createElement('option');
            o.value = t.termsCd; o.textContent = t.termsNm + ' (' + t.termsCd + ')';
            s.appendChild(o);
        });
        if (selected) s.value = selected;
        return s;
    }
    function ynSelect(cls, value, yText, nText) {
        var s = document.createElement('select');
        s.className = 'form-select form-select-sm ' + cls;
        [['Y', yText], ['N', nText]].forEach(function (p) {
            var o = document.createElement('option'); o.value = p[0]; o.textContent = p[1]; s.appendChild(o);
        });
        s.value = value || 'Y';
        return s;
    }
    function addMapRow(r) {
        var tr = document.createElement('tr');
        td(tr, '').appendChild(termsOptions(r.termsCd));
        td(tr, '').appendChild(ynSelect('f-req', r.reqYn, '필수', '선택'));
        var ord = document.createElement('input');
        ord.type = 'number'; ord.min = 1; ord.max = 999; ord.className = 'form-control form-control-sm f-ord'; ord.value = r.sortOrd || 1;
        td(tr, '').appendChild(ord);
        td(tr, '').appendChild(ynSelect('f-use', r.mapUseYn, 'Y', 'N'));
        td(tr, r.termsVer ? 'v' + r.termsVer + ' · ' + r.effectDt : (r.termsCd ? '시행 중 버전 없음' : '저장 후 표시'), 'small text-muted');
        var del = document.createElement('button');
        del.className = 'btn btn-sm btn-link text-danger p-0'; del.textContent = '삭제';
        del.addEventListener('click', function () { tr.remove(); });
        td(tr, '').appendChild(del);
        $('mapRows').appendChild(tr);
    }
    function loadMap() {
        if (!curBiz) return;
        api('GET', '/biz/' + curBiz + '/terms').then(function (res) {
            $('mapRows').textContent = '';
            res.data.forEach(addMapRow);
        });
    }
    $('mapAdd').addEventListener('click', function () { if (curBiz) addMapRow({ reqYn: 'N', sortOrd: 9, mapUseYn: 'Y' }); });
    $('mapSave').addEventListener('click', function () {
        if (!curBiz) return;
        var rows = Array.prototype.map.call($('mapRows').children, function (tr) {
            return {
                termsCd: tr.querySelector('.f-terms').value,
                reqYn: tr.querySelector('.f-req').value,
                sortOrd: parseInt(tr.querySelector('.f-ord').value, 10),
                useYn: tr.querySelector('.f-use').value
            };
        });
        api('PUT', '/biz/' + curBiz + '/terms', rows).then(function (res) {
            UiAlert.alert(res.message);
            $('mapRows').textContent = '';
            res.data.forEach(addMapRow);
            loadBiz();
            preview();
        });
    });

    // 미리보기: 사용자 화면 목록 API 와 같은 결과 (기준일 지정 가능)
    function preview() {
        if (!curBiz) return;
        var q = $('baseDt').value ? '?baseDt=' + $('baseDt').value : '';
        fetch(API + '/biz/' + curBiz + '/preview' + q).then(function (r) { return r.json(); }).then(function (res) {
            var box = $('preview');
            box.textContent = '';
            if (!res.success) { box.textContent = res.message; return; }
            var d = res.data;
            var h = document.createElement('div');
            h.className = 'fw-bold';
            h.textContent = d.bizNm + ' · 기준일 ' + d.baseDt;
            box.appendChild(h);
            [['1페이지 필수', d.required], ['2페이지 선택', d.optional]].forEach(function (p) {
                var t = document.createElement('h6'); t.textContent = p[0] + ' (' + p[1].length + ')'; box.appendChild(t);
                var ul = document.createElement('ul'); ul.className = 'mb-0';
                p[1].forEach(function (x) {
                    var li = document.createElement('li');
                    li.textContent = x.termsNm + '  v' + x.termsVer + ' (시행 ' + x.effectDt + ')';
                    ul.appendChild(li);
                });
                if (!p[1].length) { var li = document.createElement('li'); li.className = 'text-muted'; li.textContent = '없음 → 이 페이지는 건너뜀'; ul.appendChild(li); }
                box.appendChild(ul);
            });
        });
    }
    $('btnPreview').addEventListener('click', preview);

    // ------------------------------------------------------------------ 탭2 약관
    function loadTerms(select) {
        return api('GET', '/terms').then(function (res) {
            termsAll = res.data;
            var tb = $('termsRows');
            tb.textContent = '';
            res.data.forEach(function (t) {
                var tr = document.createElement('tr');
                tr.className = 'clickable';
                td(tr, t.termsCd); td(tr, t.termsNm); td(tr, 'v' + t.termsVer + ' (' + t.verCnt + '개)');
                td(tr, t.currentVer ? 'v' + t.currentVer : '시행 전');
                td(tr, t.effectDt);
                tr.addEventListener('click', function () { selectTerms(t.termsCd, tr); });
                tb.appendChild(tr);
                if (t.termsCd === select) selectTerms(t.termsCd, tr);
            });
        });
    }
    function selectTerms(code, row) {
        document.querySelectorAll('#termsRows tr').forEach(function (tr) { tr.classList.toggle('sel', tr === row); });
        api('GET', '/terms/' + code + '/versions').then(function (res) {
            $('verTitle').textContent = code;
            var tb = $('verRows');
            tb.textContent = '';
            res.data.forEach(function (v) {
                var tr = document.createElement('tr');
                td(tr, 'v' + v.termsVer); td(tr, v.effectDt); td(tr, fmt(v.regAt) + ' ' + v.regBy); td(tr, shortHash(v.cnHash), 'hash');
                tb.appendChild(tr);
            });
            // 개정 폼: 최신 버전 내용으로 채움 (전문은 사용자 API 로 조회)
            var latest = res.data[0];
            fetch('/api/common/consent/terms/' + code + '/' + latest.termsVer).then(function (r) { return r.json(); }).then(function (d) {
                $('fTermsCd').value = code; $('fTermsCd').readOnly = true;
                $('fTermsNm').value = latest.termsNm;
                $('fTermsCn').value = d.success ? d.data.termsCn : '';
                $('fPdfUrl').value = latest.pdfUrl || '';
                $('fEffectDt').value = '';
                $('termsMode').textContent = '개정 → v' + (latest.termsVer + 1);
                $('termsMode').className = 'badge bg-warning text-dark ms-2';
            });
        });
    }
    $('termsNew').addEventListener('click', function () {
        $('fTermsCd').readOnly = false;
        ['fTermsCd', 'fTermsNm', 'fTermsCn', 'fPdfUrl', 'fEffectDt'].forEach(function (id) { $(id).value = ''; });
        $('termsMode').textContent = '신규'; $('termsMode').className = 'badge bg-success ms-2';
        $('fTermsCd').focus();
    });
    $('termsSave').addEventListener('click', function () {
        api('POST', '/terms', {
            termsCd: $('fTermsCd').value.trim(), termsNm: $('fTermsNm').value, termsCn: $('fTermsCn').value,
            pdfUrl: $('fPdfUrl').value, effectDt: $('fEffectDt').value || null
        }).then(function (res) {
            UiAlert.alert(res.message);
            loadTerms(res.data.termsCd);
        });
    });

    // ------------------------------------------------------------------ 탭3 동의내역
    function loadAgree() {
        var q = '?bizCd=' + encodeURIComponent($('qBiz').value) + '&custId=' + encodeURIComponent($('qCust').value.trim());
        api('GET', '/agreements' + q).then(function (res) {
            var tb = $('agreeRows');
            tb.textContent = '';
            if (!res.data.length) { var tr = document.createElement('tr'); td(tr, '동의내역이 없습니다.', 'text-muted').colSpan = 6; tb.appendChild(tr); }
            res.data.forEach(function (a) {
                var tr = document.createElement('tr');
                tr.className = 'clickable';
                td(tr, a.agreeNo); td(tr, a.bizNm || a.bizCd); td(tr, a.custId || '(비로그인)'); td(tr, a.channel);
                td(tr, a.agreeCnt + '/' + a.totalCnt); td(tr, fmt(a.agreeAt));
                tr.addEventListener('click', function () { agreeDetail(a.agreeNo, tr); });
                tb.appendChild(tr);
            });
        });
    }
    function agreeDetail(no, row) {
        document.querySelectorAll('#agreeRows tr').forEach(function (tr) { tr.classList.toggle('sel', tr === row); });
        api('GET', '/agreements/' + no).then(function (res) {
            var a = res.data;
            $('agreeTitle').textContent = '#' + a.agreeNo + ' ' + (a.bizNm || a.bizCd) + ' · IP ' + (a.clientIp || '-');
            var tb = $('agreeDtl');
            tb.textContent = '';
            a.items.forEach(function (i) {
                var tr = document.createElement('tr');
                td(tr, i.termsNm || i.termsCd); td(tr, i.reqYn === 'Y' ? '필수' : '선택'); td(tr, 'v' + i.termsVer);
                td(tr, i.agreeYn, i.agreeYn === 'Y' ? 'text-success fw-bold' : 'text-danger');
                td(tr, i.viewYn); td(tr, shortHash(i.cnHash), 'hash').title = i.cnHash;
                tb.appendChild(tr);
            });
        });
    }
    $('btnAgree').addEventListener('click', loadAgree);

    // ------------------------------------------------------------------ 시작
    loadTerms().then(function () { return loadBiz('SIGN_UP'); });
})();
</script>
</body>
</html>
