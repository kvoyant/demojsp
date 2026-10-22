<%--
  =============================================================================
   [CF-TC-07 / 공통#1 REQ-AP-074] 디지털어드민 > 플랫폼 대출 연계 운영  -  GET /common/platform-link/admin
  -----------------------------------------------------------------------------
   제휴사별: 한도조회 엔진 전환(LEGACY → SHADOW → NEW, 문제 시 즉시 원복) · 제한시간 · 허용상품 · 허용 IP · 서비스 중단
   처리현황(승인·거절·실패·타임아웃·평균/최대 ms) · 차단기 상태 · 결과코드 변환표 · 이력
   주의: JSP 에서는 JS 템플릿 문자열의 달러+중괄호가 EL 로 해석되므로 문자열 더하기를 사용한다.
  =============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>디지털어드민 - 플랫폼 대출 연계</title>
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
        tr.sel td { background: #e3ecfa; }
        .eng { font-size: 11px; padding: 1px 6px; border-radius: 8px; }
        .eng-LEGACY { background: #f3f4f6; color: #374151; } .eng-SHADOW { background: #fff4e5; color: #9a5b00; } .eng-NEW { background: #e3ecfa; color: #0d47a1; }
        .st { font-size: 11px; padding: 1px 6px; border-radius: 8px; }
        .st-APPROVED { background: #e6f4ea; color: #1b7a3d; } .st-REJECTED { background: #f3f4f6; color: #374151; }
        .st-TIMEOUT, .st-CSS_ERROR { background: #fdecea; color: #c62828; } .st-INVALID, .st-PRODUCT_NA, .st-STOPPED { background: #fff4e5; color: #9a5b00; }
        .num { text-align: right; font-variant-numeric: tabular-nums; }
        .over { color: #c62828; font-weight: 700; }
        pre.code { background: #1f2430; color: #e6e9f0; border-radius: 6px; padding: 8px; white-space: pre-wrap; font-size: 12px; margin: 0; }
    </style>
</head>
<body>
<jsp:include page="/WEB-INF/views/common/navbar.jsp" />

<div class="container-xl pb-5">
    <div class="d-flex align-items-center mb-2 flex-wrap gap-2">
        <a href="/common/test-hub" class="btn btn-sm btn-outline-secondary"><i class="bi bi-arrow-left"></i> 테스트 목록</a>
        <h5 class="fw-bold mb-0">[공통#1] 디지털어드민 &gt; 플랫폼 대출 연계 운영</h5>
        <a href="/common/platform-link/sample" class="btn btn-sm btn-outline-primary ms-auto"><i class="bi bi-plug"></i> 제휴사 시뮬레이터</a>
    </div>
    <div class="alert alert-info small py-2">
        신규 CSS 전환은 제휴사별로 <span class="eng eng-LEGACY">LEGACY</span> → <span class="eng eng-SHADOW">SHADOW</span>(현행으로 응답하면서 신규 결과를 비교 기록)
        → <span class="eng eng-NEW">NEW</span> 순서로 진행합니다. 문제가 생기면 NEW → LEGACY 로 바꾸면 <b>다음 요청부터 즉시 원복</b>됩니다 (배포 없음).
        <b>사용 N</b> 은 제휴사에 "서비스 중단" 코드로 응답합니다.
    </div>

    <div class="row g-3">
        <div class="col-lg-8">
            <div class="card shadow-sm">
                <div class="card-header fw-bold d-flex align-items-center">처리현황
                    <select id="hours" class="form-select form-select-sm ms-auto" style="max-width:120px">
                        <option value="1">최근 1시간</option><option value="24" selected>최근 24시간</option><option value="168">최근 7일</option>
                    </select>
                </div>
                <div class="card-body p-0" style="overflow-x:auto">
                    <table class="table table-sm mb-0">
                        <thead class="table-light"><tr><th>제휴사</th><th class="num">전체</th><th class="num">승인</th><th class="num">거절</th><th class="num">실패</th><th class="num">타임아웃</th><th class="num">평균ms</th><th class="num">최대ms</th><th class="num">제한ms</th><th class="num">제한초과</th></tr></thead>
                        <tbody id="statRows"></tbody>
                    </table>
                </div>
            </div>
        </div>
        <div class="col-lg-4">
            <div class="card shadow-sm">
                <div class="card-header fw-bold">CSS 차단기</div>
                <div class="card-body p-0">
                    <table class="table table-sm mb-0">
                        <thead class="table-light"><tr><th>엔진</th><th>상태</th><th class="num">연속실패</th><th></th></tr></thead>
                        <tbody id="cbRows"></tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>

    <div class="row g-3 mt-0">
        <div class="col-lg-7">
            <div class="card shadow-sm"><div class="card-header fw-bold">제휴사 (행을 누르면 수정)</div>
                <div class="card-body p-0" style="overflow-x:auto">
                    <table class="table table-sm table-hover mb-0">
                        <thead class="table-light"><tr><th>제휴사</th><th>전문 변환기</th><th>엔진</th><th>허용상품</th><th class="num">제한ms</th><th class="num">여유ms</th><th>허용IP</th><th>사용</th><th>수정</th></tr></thead>
                        <tbody id="pRows"></tbody>
                    </table>
                </div>
            </div>
            <div class="card shadow-sm mt-3"><div class="card-header fw-bold" id="codeTitle">결과코드 변환표</div>
                <div class="card-body p-0">
                    <table class="table table-sm mb-0">
                        <thead class="table-light"><tr><th>내부 결과</th><th>제휴사 코드</th><th>제휴사 문구</th></tr></thead>
                        <tbody id="codeRows"></tbody>
                    </table>
                </div>
            </div>
        </div>
        <div class="col-lg-5">
            <div class="card shadow-sm">
                <div class="card-header fw-bold" id="fTitle">제휴사 설정</div>
                <div class="card-body">
                    <div class="row g-2 small">
                        <div class="col-12"><label>한도조회 엔진</label>
                            <select id="fEngine" class="form-select form-select-sm">
                                <option value="LEGACY">LEGACY - 현행 CSS</option><option value="SHADOW">SHADOW - 현행 응답 + 신규 병행비교</option><option value="NEW">NEW - 신규 CSS</option>
                            </select></div>
                        <div class="col-12"><label>허용상품</label><div>
                            <label class="me-3"><input type="checkbox" class="fPrd" value="CREDIT"> 신용</label>
                            <label class="me-3"><input type="checkbox" class="fPrd" value="AUTO"> 오토</label>
                            <label><input type="checkbox" class="fPrd" value="MORTGAGE"> 모기지</label></div></div>
                        <div class="col-6"><label>제휴사 제한시간(ms)</label><input id="fTimeout" type="number" min="500" max="30000" class="form-control form-control-sm"></div>
                        <div class="col-6"><label>응답 여유(ms)</label><input id="fMargin" type="number" min="0" class="form-control form-control-sm"></div>
                        <div class="col-12"><label>허용 송신 IP (쉼표, * = 제한 없음)</label><input id="fIps" class="form-control form-control-sm"></div>
                        <div class="col-6"><label>사용</label><select id="fUse" class="form-select form-select-sm"><option>Y</option><option>N</option></select></div>
                    </div>
                    <p class="small text-muted mt-2 mb-2">CSS 에 주는 최대 시간 = 제한시간 - 응답 여유 - 앞 단계 처리시간. 전문 변환기·서명키는 배포와 함께 바뀌는 값이라 여기서 바꾸지 않습니다.</p>
                    <button class="btn btn-sm btn-primary" id="pSave" disabled>저장</button>
                </div>
            </div>
        </div>
    </div>

    <div class="card shadow-sm mt-3">
        <div class="card-header fw-bold d-flex align-items-center gap-2 flex-wrap">한도조회 이력
            <select id="qPartner" class="form-select form-select-sm ms-auto" style="max-width:160px"><option value="">전체 제휴사</option></select>
            <select id="qResult" class="form-select form-select-sm" style="max-width:150px">
                <option value="">전체 결과</option><option>APPROVED</option><option>REJECTED</option><option>TIMEOUT</option><option>CSS_ERROR</option><option>INVALID</option><option>PRODUCT_NA</option><option>STOPPED</option>
            </select>
            <button class="btn btn-sm btn-primary" id="btnReq">조회</button>
        </div>
        <div class="card-body p-0" style="overflow-x:auto">
            <table class="table table-sm table-hover mb-0">
                <thead class="table-light"><tr><th>수신일시</th><th>제휴사</th><th>제휴사 요청번호</th><th>고객</th><th>상품</th><th>엔진</th><th>결과</th><th>제휴사 코드</th><th class="num">한도(만원)</th><th class="num">금리</th><th class="num">CSS ms</th><th class="num">전체 ms</th><th>SHADOW 비교</th></tr></thead>
                <tbody id="rRows"></tbody>
            </table>
        </div>
        <div class="card-body border-top" id="resBox" hidden><div class="small fw-bold mb-1" id="resTitle"></div><pre class="code" id="resBody"></pre></div>
    </div>
</div>

<script>
(function () {
    var API = '/api/common/platform-link-admin';
    var $ = function (id) { return document.getElementById(id); };
    var cur = null;

    function api(method, url, body) {
        return fetch(API + url, { method: method, headers: body ? { 'Content-Type': 'application/json' } : {}, body: body ? JSON.stringify(body) : undefined })
            .then(function (r) { return r.json(); }).then(function (res) {
                if (!res.success) { UiAlert.alert(res.message, { type: 'error' }); throw res; }
                return res;
            });
    }
    function td(tr, v, cls) { var c = document.createElement('td'); c.textContent = v == null ? '' : v; if (cls) c.className = cls; tr.appendChild(c); return c; }
    function badge(tr, text, cls) { var c = td(tr, ''); var s = document.createElement('span'); s.className = cls; s.textContent = text; c.appendChild(s); return c; }
    function n(v) { return v == null ? '-' : Number(v).toLocaleString(); }
    function empty(tb, cols, text) { var tr = document.createElement('tr'); td(tr, text, 'text-muted').colSpan = cols; tb.appendChild(tr); }

    function loadStats() {
        api('GET', '/stats?hours=' + $('hours').value).then(function (res) {
            var tb = $('statRows'); tb.textContent = '';
            res.data.forEach(function (s) {
                var tr = document.createElement('tr');
                td(tr, s.partnerNm + ' (' + s.partnerCd + ')'); td(tr, n(s.total), 'num'); td(tr, n(s.approved), 'num'); td(tr, n(s.rejected), 'num');
                td(tr, n(s.failed), 'num'); td(tr, n(s.timeouts), 'num' + (s.timeouts ? ' over' : '')); td(tr, n(s.avgMs), 'num'); td(tr, n(s.maxMs), 'num');
                td(tr, n(s.timeoutMs), 'num'); td(tr, n(s.overTimeout), 'num' + (s.overTimeout ? ' over' : ''));
                tb.appendChild(tr);
            });
        });
    }
    function loadCircuit() {
        api('GET', '/circuit').then(function (res) {
            var tb = $('cbRows'); tb.textContent = '';
            Object.keys(res.data).forEach(function (e) {
                var s = res.data[e], tr = document.createElement('tr');
                badge(tr, e, 'eng eng-' + e);
                td(tr, s.state === 'OPEN' ? '열림 (호출 차단)' : '정상', s.state === 'OPEN' ? 'over' : 'text-success');
                td(tr, s.fails, 'num');
                var c = td(tr, '');
                if (s.state === 'OPEN' || s.fails > 0) {
                    var b = document.createElement('button'); b.className = 'btn btn-sm btn-outline-danger py-0'; b.textContent = '닫기';
                    b.addEventListener('click', function () { api('POST', '/circuit/' + e + '/reset').then(function (r) { UiAlert.alert(r.message); loadCircuit(); }); });
                    c.appendChild(b);
                }
                tb.appendChild(tr);
            });
        });
    }
    function fill(p) {
        cur = p;
        $('fTitle').textContent = '제휴사 설정 - ' + p.partnerNm + ' (' + p.partnerCd + ')';
        $('fEngine').value = p.cssEngine; $('fTimeout').value = p.timeoutMs; $('fMargin').value = p.marginMs; $('fIps').value = p.allowIps; $('fUse').value = p.useYn;
        var prd = (p.products || '').split(',');
        Array.prototype.forEach.call(document.querySelectorAll('.fPrd'), function (c) { c.checked = prd.indexOf(c.value) >= 0; });
        $('pSave').disabled = false;
        api('GET', '/partners/' + p.partnerCd + '/codes').then(function (res) {
            $('codeTitle').textContent = '결과코드 변환표 - ' + p.partnerNm;
            var tb = $('codeRows'); tb.textContent = '';
            res.data.forEach(function (c) { var tr = document.createElement('tr'); badge(tr, c.innerCd, 'st st-' + c.innerCd); td(tr, c.outerCd); td(tr, c.outerMsg); tb.appendChild(tr); });
        });
    }
    function loadPartners() {
        return api('GET', '/partners').then(function (res) {
            var tb = $('pRows'), q = $('qPartner'), keep = q.value; tb.textContent = ''; q.length = 1;
            res.data.forEach(function (p) {
                var tr = document.createElement('tr');
                tr.className = 'clickable' + (p.useYn === 'N' ? ' off' : '') + (cur && cur.partnerCd === p.partnerCd ? ' sel' : '');
                td(tr, p.partnerNm + ' (' + p.partnerCd + ')'); td(tr, p.adapterCd); badge(tr, p.cssEngine, 'eng eng-' + p.cssEngine); td(tr, p.products);
                td(tr, n(p.timeoutMs), 'num'); td(tr, n(p.marginMs), 'num'); td(tr, p.allowIps); td(tr, p.useYn);
                td(tr, (p.updAt || '').replace('T', ' ').substring(0, 16) + ' ' + (p.updBy || ''));
                tr.addEventListener('click', function () { fill(p); loadPartners(); });
                tb.appendChild(tr);
                q.add(new Option(p.partnerNm, p.partnerCd));
            });
            q.value = keep;
            if (!cur && res.data.length) fill(res.data[0]);
        });
    }
    $('pSave').addEventListener('click', function () {
        var prd = Array.prototype.filter.call(document.querySelectorAll('.fPrd'), function (c) { return c.checked; }).map(function (c) { return c.value; });
        api('POST', '/partners', { partnerCd: cur.partnerCd, cssEngine: $('fEngine').value, products: prd.join(','),
            timeoutMs: parseInt($('fTimeout').value, 10), marginMs: parseInt($('fMargin').value, 10), allowIps: $('fIps').value, useYn: $('fUse').value })
            .then(function (res) { UiAlert.alert(res.message); fill(res.data); loadPartners(); });
    });

    function loadReqs() {
        api('GET', '/requests?partner=' + $('qPartner').value + '&result=' + $('qResult').value).then(function (res) {
            var tb = $('rRows'); tb.textContent = ''; $('resBox').hidden = true;
            if (!res.data.length) empty(tb, 13, '이력이 없습니다. 제휴사 시뮬레이터에서 요청을 보내 보세요.');
            res.data.forEach(function (r) {
                var tr = document.createElement('tr'); tr.className = 'clickable';
                td(tr, String(r.reqAt).replace('T', ' ').substring(0, 19)); td(tr, r.partnerNm || r.partnerCd); td(tr, r.partnerReqNo); td(tr, r.custNmMask || '-');
                td(tr, r.cssProduct || '-'); if (r.cssEngine) badge(tr, r.cssEngine, 'eng eng-' + r.cssEngine); else td(tr, '-');
                badge(tr, r.resultCd || r.status, 'st st-' + r.resultCd); td(tr, r.outerCd || '-');
                td(tr, r.limitAmt == null ? '-' : n(r.limitAmt / 10000), 'num'); td(tr, r.rateBp == null ? '-' : (r.rateBp / 100).toFixed(2) + '%', 'num');
                td(tr, n(r.cssMs), 'num'); td(tr, n(r.totalMs), 'num' + (r.totalMs > r.timeoutMs ? ' over' : '')); td(tr, r.shadowResult || '');
                tr.addEventListener('click', function () {
                    $('resBox').hidden = false;
                    $('resTitle').textContent = '제휴사에 보낸 응답 전문 - 연계번호 ' + r.lnkReqNo + (r.clientIp ? ' · 송신 IP ' + r.clientIp : '');
                    try { $('resBody').textContent = JSON.stringify(JSON.parse(r.resBody), null, 2); } catch (e) { $('resBody').textContent = r.resBody || '-'; }
                });
                tb.appendChild(tr);
            });
        });
    }
    $('btnReq').addEventListener('click', loadReqs);
    $('hours').addEventListener('change', loadStats);

    loadStats(); loadCircuit();
    loadPartners().then(loadReqs);
})();
</script>
</body>
</html>
