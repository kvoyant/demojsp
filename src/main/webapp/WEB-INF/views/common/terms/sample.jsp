<%--
  =============================================================================
   [CF-BC-04 샘플] 약관동의 공통모듈 사용 예시 / 테스트 화면  -  GET /common/terms/sample
  -----------------------------------------------------------------------------
   업무 화면은 공통 JS 하나(/js/common/terms-agree.js)만 포함하고 TermsAgree.open({ bizCode }) 을 호출한다.
   아래 "ADMIN 테스트" 영역은 local(common.terms.admin-api-open=true) 또는 관리자 로그인 시 동작한다.
  =============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>저축은행 포털 - 약관동의 공통모듈 샘플</title>
    <link rel="shortcut icon" href="/favicon.ico">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css" rel="stylesheet">
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <!-- 약관동의 공통 JS (이 한 줄만 있으면 된다) -->
    <script src="/js/common/terms-agree.js"></script>
    <style>
        body { background-color: #f4f6f9; font-family: 'Malgun Gothic', 'Noto Sans KR', sans-serif; }
        .card-header-financial { background-color: #0d47a1; color: white; font-weight: bold; }
        pre.result-log { background: #212529; color: #e9ecef; border-radius: 6px; padding: 12px; min-height: 80px; max-height: 360px; overflow: auto; white-space: pre-wrap; font-size: 12px; }
        .table-sm td, .table-sm th { font-size: 12px; vertical-align: middle; }
        .hash { font-family: monospace; font-size: 11px; color: #6c757d; }
    </style>
</head>
<body>
<jsp:include page="/WEB-INF/views/common/navbar.jsp" />

<div class="container pb-5" style="max-width: 1100px;">
    <div class="d-flex align-items-center mb-3">
        <a href="/common/test-hub" class="btn btn-sm btn-outline-secondary me-2"><i class="bi bi-arrow-left"></i> 테스트 목록</a>
        <h5 class="fw-bold mb-0">[CF-BC-04 / REQ-AP-602] 약관동의 공통모듈</h5>
    </div>

    <div class="row g-4">
        <!-- 좌: 사용자 화면 테스트 -->
        <div class="col-lg-6">
            <div class="card shadow-sm mb-4">
                <div class="card-header card-header-financial"><i class="bi bi-ui-checks me-1"></i> 1. 약관동의 (업무 화면 관점)</div>
                <div class="card-body">
                    <div class="row g-2 mb-3">
                        <div class="col-6">
                            <label class="form-label small fw-bold" for="bizCode">업무코드 (bizCode)</label>
                            <select id="bizCode" class="form-select form-select-sm"></select>
                        </div>
                        <div class="col-6">
                            <label class="form-label small fw-bold" for="channel">채널</label>
                            <select id="channel" class="form-select form-select-sm">
                                <option value="PC_WEB">PC WEB</option>
                                <option value="APP">APP</option>
                                <option value="RECRUIT_WEB">모집인WEB</option>
                            </select>
                        </div>
                    </div>
                    <div class="mb-3 small">
                        <span class="fw-bold me-2">표시 방식</span>
                        <div class="form-check form-check-inline">
                            <input class="form-check-input" type="radio" name="mode" id="modeLayer" value="layer" checked>
                            <label class="form-check-label" for="modeLayer">전체화면 레이어 (APP/모바일)</label>
                        </div>
                        <div class="form-check form-check-inline">
                            <input class="form-check-input" type="radio" name="mode" id="modeInline" value="inline">
                            <label class="form-check-label" for="modeInline">화면 내 삽입 (inline)</label>
                        </div>
                    </div>
                    <div class="d-flex flex-wrap gap-2">
                        <button id="btnOpen" class="btn btn-primary btn-sm"><i class="bi bi-file-earmark-check me-1"></i>약관동의 열기</button>
                        <button id="btnStatus" class="btn btn-outline-secondary btn-sm">세션 동의상태</button>
                        <button id="btnSubmit" class="btn btn-outline-dark btn-sm">업무 처리 (1회 사용)</button>
                    </div>
                    <div id="inlineArea" class="mt-3"></div>
                </div>
            </div>

            <div class="card shadow-sm">
                <div class="card-header">처리 결과</div>
                <div class="card-body"><pre id="log" class="result-log mb-0">약관동의 열기를 눌러 테스트하세요.</pre></div>
            </div>
        </div>

        <!-- 우: ADMIN 테스트 -->
        <div class="col-lg-6">
            <div class="card shadow-sm mb-4">
                <div class="card-header d-flex justify-content-between align-items-center">
                    <span class="fw-bold"><i class="bi bi-gear me-1"></i> 2. ADMIN - 약관 버전 (현재/이력)</span>
                    <button id="btnVersions" class="btn btn-sm btn-outline-primary">새로고침</button>
                </div>
                <div class="card-body p-0">
                    <div class="table-responsive">
                        <table class="table table-sm mb-0">
                            <thead class="table-light"><tr><th>약관코드</th><th>약관명</th><th>구분</th><th>버전</th><th>등록</th><th>SHA-256</th></tr></thead>
                            <tbody id="versionRows"><tr><td colspan="6" class="text-muted">-</td></tr></tbody>
                        </table>
                    </div>
                </div>
            </div>

            <div class="card shadow-sm mb-4">
                <div class="card-header fw-bold"><i class="bi bi-pencil-square me-1"></i> 3. ADMIN - 약관 등록/개정</div>
                <div class="card-body">
                    <p class="small text-muted mb-2">기존 약관코드에 새 버전을 등록하면 즉시 현재 약관이 됩니다. 이미 열려 있던 동의화면에서 완료하면 <code>TRM-103</code>(개정됨)으로 재동의를 유도합니다.</p>
                    <div class="row g-2">
                        <div class="col-6"><input id="regCode" class="form-control form-control-sm" placeholder="약관코드 (예: MARKETING_USE)"></div>
                        <div class="col-3"><input id="regVersion" class="form-control form-control-sm" placeholder="버전 (예: 1.1)"></div>
                        <div class="col-3">
                            <select id="regRequired" class="form-select form-select-sm">
                                <option value="">필수여부 유지</option><option value="true">필수</option><option value="false">선택</option>
                            </select>
                        </div>
                        <div class="col-12"><input id="regTitle" class="form-control form-control-sm" placeholder="약관명"></div>
                        <div class="col-12"><textarea id="regContent" class="form-control form-control-sm" rows="3" placeholder="약관 전문"></textarea></div>
                        <div class="col-12 text-end"><button id="btnRegister" class="btn btn-sm btn-warning fw-bold">등록</button></div>
                    </div>
                </div>
            </div>

            <div class="card shadow-sm">
                <div class="card-header d-flex justify-content-between align-items-center">
                    <span class="fw-bold"><i class="bi bi-shield-check me-1"></i> 4. ADMIN - 동의 이력 (증적)</span>
                    <button id="btnHistory" class="btn btn-sm btn-outline-primary">새로고침</button>
                </div>
                <div class="card-body p-0">
                    <div class="table-responsive" style="max-height: 320px;">
                        <table class="table table-sm mb-0">
                            <thead class="table-light"><tr><th>동의번호</th><th>약관</th><th>버전</th><th>동의</th><th>주체</th><th>채널</th><th>일시</th></tr></thead>
                            <tbody id="historyRows"><tr><td colspan="7" class="text-muted">-</td></tr></tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
(function () {
    var $ = function (id) { return document.getElementById(id); };
    var logEl = $('log');
    function log(o) { logEl.textContent = typeof o === 'string' ? o : JSON.stringify(o, null, 2); }
    function bizCode() { return $('bizCode').value; }

    function api(url, opt) {
        opt = opt || {};
        opt.credentials = 'same-origin';
        opt.headers = Object.assign({ 'X-Requested-With': 'XMLHttpRequest' }, opt.headers || {});
        return fetch(url, opt).then(function (r) { return r.json(); });
    }

    /** 표 행 만들기 (textContent 만 사용 → XSS 안전) */
    function row(cells) {
        var tr = document.createElement('tr');
        cells.forEach(function (c) {
            var td = document.createElement('td');
            if (c && c.nodeType) td.appendChild(c); else td.textContent = c == null ? '' : c;
            tr.appendChild(td);
        });
        return tr;
    }
    function badge(text, cls) { var s = document.createElement('span'); s.className = 'badge ' + cls; s.textContent = text; return s; }
    function fill(tbodyId, rows, cols, emptyMsg) {
        var tb = $(tbodyId); tb.innerHTML = '';
        if (!rows.length) { tb.appendChild(row([emptyMsg])).firstChild.colSpan = cols; return; }
        rows.forEach(function (r) { tb.appendChild(r); });
    }

    // 0) 업무코드 목록 (ADMIN API). 권한이 없으면 기본 목록 사용
    function loadBizCodes() {
        return api('/api/common/terms-admin/biz-codes').then(function (res) {
            var list = res.success ? res.data : [
                { bizCode: 'SIGN_UP', bizName: '회원가입' }, { bizCode: 'LOAN_APPLY', bizName: '대출신청' },
                { bizCode: 'IDENTITY_NICE_MOBILE', bizName: '휴대폰 본인인증' }, { bizCode: 'IDENTITY_ID_CARD', bizName: '신분증·안면인증' }];
            var sel = $('bizCode'); sel.innerHTML = '';
            list.forEach(function (b) {
                var o = document.createElement('option');
                o.value = b.bizCode;
                o.textContent = b.bizName + ' (' + b.bizCode + ')' + (b.requiredCount != null ? ' 필수' + b.requiredCount + '/선택' + b.optionalCount : '');
                sel.appendChild(o);
            });
            if (!res.success) log({ 'ADMIN API': res });
        });
    }

    // 1) 약관동의 열기 - 업무 화면은 이 호출 하나면 된다
    $('btnOpen').addEventListener('click', function () {
        var inline = $('modeInline').checked;
        TermsAgree.open({
            bizCode: bizCode(),
            channel: $('channel').value,
            mode: inline ? 'inline' : 'layer',
            container: inline ? '#inlineArea' : null
        }).then(function (r) {
            log(r);
            loadHistory();
        }).catch(log);
    });

    $('btnStatus').addEventListener('click', function () {
        TermsAgree.status(bizCode()).then(function (d) { log(d ? { '세션 동의결과': d } : '동의 결과 없음 (미동의 또는 만료/사용됨)'); }).catch(log);
    });

    // 2) 업무 API - 서버가 세션의 동의 결과를 require() 로 확인
    $('btnSubmit').addEventListener('click', function () {
        api('/api/common/terms-sample/submit?bizCode=' + encodeURIComponent(bizCode()), { method: 'POST' }).then(log);
    });

    // 3) ADMIN - 버전 이력
    function loadVersions() {
        api('/api/common/terms-admin/' + encodeURIComponent(bizCode()) + '/versions').then(function (res) {
            if (!res.success) { fill('versionRows', [], 6, res.code + ' ' + res.message); return; }
            fill('versionRows', res.data.map(function (t) {
                var h = document.createElement('span'); h.className = 'hash'; h.textContent = t.contentHash.substring(0, 12) + '…'; h.title = t.contentHash;
                return row([t.termsCode, t.title, badge(t.required ? '필수' : '선택', t.required ? 'bg-danger' : 'bg-secondary'),
                    t.version, t.registeredAt + ' / ' + t.registeredBy, h]);
            }), 6, '등록된 약관 없음');
        });
    }

    // 4) ADMIN - 약관 등록/개정
    $('btnRegister').addEventListener('click', function () {
        var req = $('regRequired').value;
        api('/api/common/terms-admin/terms', {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                bizCode: bizCode(), termsCode: $('regCode').value.trim(), version: $('regVersion').value.trim(),
                title: $('regTitle').value, content: $('regContent').value,
                required: req === '' ? null : req === 'true'
            })
        }).then(function (res) { log(res); loadVersions(); });
    });

    // 5) ADMIN - 동의 이력
    function loadHistory() {
        api('/api/common/terms-admin/agreements?limit=50&bizCode=' + encodeURIComponent(bizCode())).then(function (res) {
            if (!res.success) { fill('historyRows', [], 7, res.code + ' ' + res.message); return; }
            fill('historyRows', res.data.map(function (a) {
                return row([a.agreementId, a.termsCode, a.version, badge(a.agreed ? 'Y' : 'N', a.agreed ? 'bg-success' : 'bg-light text-dark'),
                    a.subjectId, a.channel, a.agreedAt]);
            }), 7, '이력 없음');
        });
    }

    $('btnVersions').addEventListener('click', loadVersions);
    $('btnHistory').addEventListener('click', loadHistory);
    $('bizCode').addEventListener('change', function () { loadVersions(); loadHistory(); });

    loadBizCodes().then(function () { loadVersions(); loadHistory(); });
})();
</script>
</body>
</html>
