<%--
  =============================================================================
   [CF-BC-06 / 공통#25] 디지털어드민 > 1원 계좌인증 업무 Context · 인증 이력  -  GET /common/acct-verify/admin
  -----------------------------------------------------------------------------
   업무 Context (TB_CO_ACCT_BIZ): 화면 종류(LOAN/COMMON) · 대출계좌 자동완성 · 예금주 본인 확인 · 입금자명 접두어 ·
                                  유효시간(분) · 입력 횟수 · 결과 사용시간(분)
   인증 이력 (TB_CO_ACCT_VERIFY): 업무 · 고객 · 은행 · 계좌(마스킹) · 자동완성/직접입력/수정 · 상태 · 결과코드
   주의: JSP 에서는 JS 템플릿 문자열의 달러+중괄호가 EL 로 해석되므로 문자열 더하기를 사용한다.
  =============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>디지털어드민 - 1원 계좌인증</title>
    <link rel="shortcut icon" href="/favicon.ico">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css" rel="stylesheet">
    <link rel="stylesheet" href="/css/common/ui-tokens.css">
    <link rel="stylesheet" href="/css/common/ui-alert.css">
    <script src="/js/common/ui-alert.js"></script>
    <style>
        body { background-color: #f4f6f9; font-family: 'Malgun Gothic', 'Noto Sans KR', sans-serif; }
        .table-sm td, .table-sm th { font-size: 12px; vertical-align: middle; white-space: nowrap; }
        .form-control-sm, .form-select-sm { font-size: 12px; }
        tr.clickable { cursor: pointer; }
        tr.off td { color: #9ca3af; }
        .st { font-size: 11px; padding: 1px 6px; border-radius: 8px; }
        .st-VERIFIED { background: #e6f4ea; color: #1b7a3d; } .st-FAILED { background: #fdecea; color: #c62828; }
        .st-SENT { background: #e3ecfa; color: #0d47a1; } .st-EXPIRED { background: #f3f4f6; color: #6b7280; }
    </style>
</head>
<body>
<jsp:include page="/WEB-INF/views/common/navbar.jsp" />

<div class="container-xl pb-5">
    <div class="d-flex align-items-center mb-2">
        <a href="/common/test-hub" class="btn btn-sm btn-outline-secondary me-2"><i class="bi bi-arrow-left"></i> 테스트 목록</a>
        <h5 class="fw-bold mb-0">[공통#25] 디지털어드민 &gt; 1원 계좌인증 업무 Context · 인증 이력</h5>
        <a href="/common/acct-verify/sample" class="btn btn-sm btn-outline-primary ms-auto"><i class="bi bi-bank"></i> 사용자 화면</a>
    </div>
    <div class="alert alert-info small py-2">
        업무마다 <b>화면 종류</b>(LOAN 대출 입출금계좌용 / COMMON 공통), <b>대출계좌 자동완성</b>, <b>예금주 본인 확인</b>, <b>입금자명 접두어</b>, <b>유효시간 · 입력 횟수</b>를 정합니다.
        저장 즉시 적용됩니다. 새 업무가 1원 인증을 쓰려면 여기서 업무코드를 등록하고 화면에서 <code>AcctVerify.open({ bizCd })</code> 만 호출하면 됩니다.
    </div>

    <div class="row g-3">
        <div class="col-lg-7">
            <div class="card shadow-sm"><div class="card-body p-0" style="overflow-x:auto">
                <table class="table table-sm table-hover mb-0">
                    <thead class="table-light"><tr><th>업무</th><th>화면</th><th>자동완성</th><th>예금주확인</th><th>입금자명</th><th>유효(분)</th><th>횟수</th><th>결과(분)</th><th>사용</th></tr></thead>
                    <tbody id="bRows"></tbody>
                </table>
            </div></div>
        </div>
        <div class="col-lg-5">
            <div class="card shadow-sm">
                <div class="card-header fw-bold d-flex">업무 Context 등록 / 수정 <button class="btn btn-sm btn-link ms-auto p-0" id="bNew">새 업무</button></div>
                <div class="card-body">
                    <div class="row g-2 small">
                        <div class="col-6"><label>업무코드</label><input id="fCd" class="form-control form-control-sm" placeholder="예: AUTO_TRANSFER"></div>
                        <div class="col-6"><label>업무명</label><input id="fNm" class="form-control form-control-sm" maxlength="50"></div>
                        <div class="col-4"><label>화면 종류</label><select id="fScreen" class="form-select form-select-sm"><option value="LOAN">LOAN 대출</option><option value="COMMON">COMMON 공통</option></select></div>
                        <div class="col-4"><label>대출계좌 자동완성</label><select id="fPrefill" class="form-select form-select-sm"><option>Y</option><option>N</option></select></div>
                        <div class="col-4"><label>예금주 본인 확인</label><select id="fOwner" class="form-select form-select-sm"><option>Y</option><option>N</option></select></div>
                        <div class="col-4"><label>입금자명 접두어</label><input id="fPrefix" class="form-control form-control-sm" maxlength="6"></div>
                        <div class="col-4"><label>유효시간(분)</label><input id="fMin" type="number" min="3" max="30" class="form-control form-control-sm"></div>
                        <div class="col-4"><label>입력 횟수</label><input id="fTry" type="number" min="1" max="10" class="form-control form-control-sm"></div>
                        <div class="col-4"><label>결과 사용(분)</label><input id="fValid" type="number" min="1" max="1440" class="form-control form-control-sm"></div>
                        <div class="col-4"><label>사용</label><select id="fUse" class="form-select form-select-sm"><option>Y</option><option>N</option></select></div>
                    </div>
                    <button class="btn btn-sm btn-primary mt-2" id="bSave">저장</button>
                </div>
            </div>
        </div>
    </div>

    <div class="card shadow-sm mt-3">
        <div class="card-header fw-bold d-flex align-items-center gap-2">인증 이력
            <select id="qBiz" class="form-select form-select-sm ms-auto" style="max-width:180px"><option value="">전체 업무</option></select>
            <select id="qStatus" class="form-select form-select-sm" style="max-width:140px">
                <option value="">전체 상태</option><option>VERIFIED</option><option>SENT</option><option>FAILED</option><option>EXPIRED</option>
            </select>
            <button class="btn btn-sm btn-primary" id="btnReq">조회</button>
        </div>
        <div class="card-body p-0" style="overflow-x:auto">
            <table class="table table-sm mb-0">
                <thead class="table-light"><tr><th>요청일시</th><th>업무</th><th>채널</th><th>고객</th><th>은행</th><th>계좌</th><th>예금주</th><th>계좌 출처</th><th>상태</th><th>결과</th><th>오입력</th><th>완료일시</th></tr></thead>
                <tbody id="rRows"></tbody>
            </table>
        </div>
    </div>
</div>

<script>
(function () {
    var API = '/api/common/acct-verify-admin';
    var $ = function (id) { return document.getElementById(id); };
    var F = { bizCd: 'fCd', bizNm: 'fNm', screenType: 'fScreen', prefillYn: 'fPrefill', ownerCheckYn: 'fOwner', memoPrefix: 'fPrefix',
              codeValidMin: 'fMin', maxTry: 'fTry', resultValidMin: 'fValid', useYn: 'fUse' };
    var NUM = ['codeValidMin', 'maxTry', 'resultValidMin'];

    function api(method, url, body) {
        return fetch(API + url, { method: method, headers: body ? { 'Content-Type': 'application/json' } : {}, body: body ? JSON.stringify(body) : undefined })
            .then(function (r) { return r.json(); }).then(function (res) {
                if (!res.success) { UiAlert.alert(res.message, { type: 'error' }); throw res; }
                return res;
            });
    }
    function td(tr, v, cls) { var c = document.createElement('td'); c.textContent = v == null ? '' : v; if (cls) c.className = cls; tr.appendChild(c); return c; }
    function dt(v) { return v ? String(v).replace('T', ' ') : '-'; }

    function fill(b) {
        Object.keys(F).forEach(function (k) { $(F[k]).value = b ? (b[k] == null ? '' : b[k]) : ''; });
        if (!b) { $('fScreen').value = 'COMMON'; $('fPrefill').value = 'N'; $('fOwner').value = 'Y'; $('fPrefix').value = '페퍼';
                  $('fMin').value = 10; $('fTry').value = 5; $('fValid').value = 30; $('fUse').value = 'Y'; }
        $('fCd').readOnly = !!b;
    }
    // COMMON 화면은 자동완성을 쓸 수 없다 (서버도 검증)
    $('fScreen').addEventListener('change', function () { if ($('fScreen').value === 'COMMON') $('fPrefill').value = 'N'; });

    function loadBizs() {
        return api('GET', '/bizs').then(function (res) {
            var tb = $('bRows'), q = $('qBiz'), keep = q.value;
            tb.textContent = '';
            q.length = 1;
            res.data.forEach(function (b) {
                var tr = document.createElement('tr');
                tr.className = 'clickable' + (b.useYn === 'N' ? ' off' : '');
                td(tr, b.bizNm + ' (' + b.bizCd + ')'); td(tr, b.screenType); td(tr, b.prefillYn); td(tr, b.ownerCheckYn);
                td(tr, b.memoPrefix + '0000'); td(tr, b.codeValidMin); td(tr, b.maxTry); td(tr, b.resultValidMin); td(tr, b.useYn);
                tr.addEventListener('click', function () { fill(b); });
                tb.appendChild(tr);
                q.add(new Option(b.bizNm, b.bizCd));
            });
            q.value = keep;
        });
    }
    $('bNew').addEventListener('click', function () { fill(null); });
    $('bSave').addEventListener('click', function () {
        var b = {};
        Object.keys(F).forEach(function (k) { var v = $(F[k]).value.trim(); b[k] = NUM.indexOf(k) >= 0 ? (v === '' ? null : parseInt(v, 10)) : v; });
        api('POST', '/bizs', b).then(function (res) { UiAlert.alert(res.message); fill(res.data); loadBizs(); });
    });

    function loadReqs() {
        api('GET', '/requests?biz=' + $('qBiz').value + '&status=' + $('qStatus').value).then(function (res) {
            var tb = $('rRows');
            tb.textContent = '';
            if (!res.data.length) { var tr = document.createElement('tr'); td(tr, '인증 이력이 없습니다.', 'text-muted').colSpan = 12; tb.appendChild(tr); }
            res.data.forEach(function (r) {
                var tr = document.createElement('tr');
                td(tr, dt(r.reqAt)); td(tr, r.bizNm || r.bizCd); td(tr, r.channel); td(tr, (r.custId ? r.custId + ' ' : '') + r.custNm);
                td(tr, r.bankNm); td(tr, r.acctMask); td(tr, r.holderNm || '-');
                td(tr, r.source === 'PREFILL' ? '자동완성' : '직접 입력' + (r.modifiedYn === 'Y' ? ' (수정)' : ''));
                var s = td(tr, ''); var sp = document.createElement('span'); sp.className = 'st st-' + r.status; sp.textContent = r.status; s.appendChild(sp);
                td(tr, r.resultCd || '-'); td(tr, r.tryCnt); td(tr, dt(r.doneAt));
                tb.appendChild(tr);
            });
        });
    }
    $('btnReq').addEventListener('click', loadReqs);

    fill(null);
    loadBizs().then(loadReqs);
})();
</script>
</body>
</html>
