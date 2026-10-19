<%--
  =============================================================================
   [CF-BC-03 / 공통#21] 디지털어드민 > 본인인증 정책 · 인증 이력  -  GET /common/niceid/admin
  -----------------------------------------------------------------------------
   업무별 정책 (TB_CO_IDV_PURPOSE): 약관 업무코드(공통#22) · SMS/MO 사용 · 최소 만 나이(미성년 차단) · 외국인 허용 ·
                                   유의고객 검사 · 인증번호 유효시간 · 입력 횟수 · 결과 사용시간
   인증 이력 (TB_CO_IDV_REQ): 업무 · 방식 · 상태 · 결과코드 · CI(앞 10자) · 휴대폰(마스킹)
   주의: JSP 에서는 JS 템플릿 문자열의 달러+중괄호가 EL 로 해석되므로 문자열 더하기를 사용한다.
  =============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>디지털어드민 - 본인인증 정책</title>
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
        .st-VERIFIED { background: #e6f4ea; color: #1b7a3d; } .st-BLOCKED { background: #fdecea; color: #c62828; }
        .st-REQUESTED { background: #e3ecfa; color: #0d47a1; } .st-FAILED, .st-EXPIRED { background: #f3f4f6; color: #6b7280; }
    </style>
</head>
<body>
<jsp:include page="/WEB-INF/views/common/navbar.jsp" />

<div class="container-xl pb-5">
    <div class="d-flex align-items-center mb-2">
        <a href="/common/test-hub" class="btn btn-sm btn-outline-secondary me-2"><i class="bi bi-arrow-left"></i> 테스트 목록</a>
        <h5 class="fw-bold mb-0">[공통#21] 디지털어드민 &gt; 본인인증 정책 · 인증 이력</h5>
        <a href="/common/niceid/sample" class="btn btn-sm btn-outline-primary ms-auto"><i class="bi bi-phone"></i> 사용자 화면</a>
    </div>
    <div class="alert alert-info small py-2">
        업무마다 <b>약관</b>(공통#22 업무코드), <b>인증 방식</b>(SMS 통신사 미지정 / MO 기기인증), <b>최소 만 나이</b>(미성년 차단), <b>외국인 허용</b>, <b>유의고객 검사</b>(CI)를 정합니다.
        저장 즉시 적용됩니다. 주민번호 대신 CI·생년월일·NICE 내외국인 값으로 검사하므로 주민번호를 받지 않아도 기존 체크가 동작합니다.
    </div>

    <div class="row g-3">
        <div class="col-lg-7">
            <div class="card shadow-sm"><div class="card-body p-0">
                <table class="table table-sm table-hover mb-0">
                    <thead class="table-light"><tr><th>업무</th><th>약관</th><th>SMS</th><th>MO</th><th>최소나이</th><th>외국인</th><th>유의고객</th><th>유효(초)</th><th>횟수</th><th>사용</th></tr></thead>
                    <tbody id="pRows"></tbody>
                </table>
            </div></div>
        </div>
        <div class="col-lg-5">
            <div class="card shadow-sm">
                <div class="card-header fw-bold d-flex">업무 정책 등록 / 수정 <button class="btn btn-sm btn-link ms-auto p-0" id="pNew">새 업무</button></div>
                <div class="card-body">
                    <div class="row g-2 small">
                        <div class="col-6"><label>업무코드</label><input id="fCd" class="form-control form-control-sm" placeholder="예: OPEN_BANKING"></div>
                        <div class="col-6"><label>업무명</label><input id="fNm" class="form-control form-control-sm" maxlength="50"></div>
                        <div class="col-6"><label>약관 업무코드 (공통#22)</label><input id="fConsent" class="form-control form-control-sm" placeholder="IDENTITY_NICE (비우면 생략)"></div>
                        <div class="col-3"><label>SMS</label><select id="fSms" class="form-select form-select-sm"><option>Y</option><option>N</option></select></div>
                        <div class="col-3"><label>MO</label><select id="fMo" class="form-select form-select-sm"><option>Y</option><option>N</option></select></div>
                        <div class="col-4"><label>최소 만 나이</label><input id="fAge" type="number" min="0" max="120" class="form-control form-control-sm"></div>
                        <div class="col-4"><label>외국인 허용</label><select id="fForeign" class="form-select form-select-sm"><option>Y</option><option>N</option></select></div>
                        <div class="col-4"><label>유의고객 검사</label><select id="fWatch" class="form-select form-select-sm"><option>Y</option><option>N</option></select></div>
                        <div class="col-3"><label>유효시간(초)</label><input id="fSec" type="number" min="60" max="600" class="form-control form-control-sm"></div>
                        <div class="col-3"><label>입력 횟수</label><input id="fTry" type="number" min="1" max="10" class="form-control form-control-sm"></div>
                        <div class="col-3"><label>결과 사용(분)</label><input id="fValid" type="number" min="1" max="1440" class="form-control form-control-sm"></div>
                        <div class="col-3"><label>사용</label><select id="fUse" class="form-select form-select-sm"><option>Y</option><option>N</option></select></div>
                    </div>
                    <button class="btn btn-sm btn-primary mt-2" id="pSave">저장</button>
                </div>
            </div>
        </div>
    </div>

    <div class="card shadow-sm mt-3">
        <div class="card-header fw-bold d-flex align-items-center gap-2">인증 이력
            <select id="qPurpose" class="form-select form-select-sm ms-auto" style="max-width:180px"><option value="">전체 업무</option></select>
            <select id="qStatus" class="form-select form-select-sm" style="max-width:140px">
                <option value="">전체 상태</option><option>VERIFIED</option><option>BLOCKED</option><option>REQUESTED</option><option>FAILED</option><option>EXPIRED</option>
            </select>
            <button class="btn btn-sm btn-primary" id="btnReq">조회</button>
        </div>
        <div class="card-body p-0">
            <table class="table table-sm mb-0">
                <thead class="table-light"><tr><th>요청일시</th><th>업무</th><th>방식</th><th>채널</th><th>성명</th><th>휴대폰</th><th>상태</th><th>결과</th><th>나이</th><th>내외국인</th><th>CI</th><th>약관동의</th><th>오입력</th></tr></thead>
                <tbody id="rRows"></tbody>
            </table>
        </div>
    </div>
</div>

<script>
(function () {
    var API = '/api/common/niceid-admin';
    var $ = function (id) { return document.getElementById(id); };
    var F = { purposeCd: 'fCd', purposeNm: 'fNm', consentBizCd: 'fConsent', smsYn: 'fSms', moYn: 'fMo', minAge: 'fAge', foreignerYn: 'fForeign',
              watchCheckYn: 'fWatch', codeValidSec: 'fSec', maxTry: 'fTry', resultValidMin: 'fValid', useYn: 'fUse' };
    var NUM = ['minAge', 'codeValidSec', 'maxTry', 'resultValidMin'];

    function api(method, url, body) {
        return fetch(API + url, { method: method, headers: body ? { 'Content-Type': 'application/json' } : {}, body: body ? JSON.stringify(body) : undefined })
            .then(function (r) { return r.json(); }).then(function (res) {
                if (!res.success) { UiAlert.alert(res.message, { type: 'error' }); throw res; }
                return res;
            });
    }
    function td(tr, v, cls) { var c = document.createElement('td'); c.textContent = v == null ? '' : v; if (cls) c.className = cls; tr.appendChild(c); return c; }

    function fill(p) {
        Object.keys(F).forEach(function (k) { $(F[k]).value = p ? (p[k] == null ? '' : p[k]) : ''; });
        if (!p) { $('fSms').value = 'Y'; $('fMo').value = 'Y'; $('fForeign').value = 'Y'; $('fWatch').value = 'Y'; $('fUse').value = 'Y';
                  $('fAge').value = 0; $('fSec').value = 180; $('fTry').value = 5; $('fValid').value = 30; }
        $('fCd').readOnly = !!p;
    }
    function loadPurposes() {
        return api('GET', '/purposes').then(function (res) {
            var tb = $('pRows'), q = $('qPurpose'), keep = q.value;
            tb.textContent = '';
            q.length = 1;
            res.data.forEach(function (p) {
                var tr = document.createElement('tr');
                tr.className = 'clickable' + (p.useYn === 'N' ? ' off' : '');
                td(tr, p.purposeNm + ' (' + p.purposeCd + ')'); td(tr, p.consentBizCd || '-'); td(tr, p.smsYn); td(tr, p.moYn);
                td(tr, p.minAge ? p.minAge + '세' : '-'); td(tr, p.foreignerYn === 'Y' ? '허용' : '차단'); td(tr, p.watchCheckYn);
                td(tr, p.codeValidSec); td(tr, p.maxTry); td(tr, p.useYn);
                tr.addEventListener('click', function () { fill(p); });
                tb.appendChild(tr);
                q.add(new Option(p.purposeNm, p.purposeCd));
            });
            q.value = keep;
        });
    }
    $('pNew').addEventListener('click', function () { fill(null); });
    $('pSave').addEventListener('click', function () {
        var b = {};
        Object.keys(F).forEach(function (k) { var v = $(F[k]).value.trim(); b[k] = NUM.indexOf(k) >= 0 ? (v === '' ? null : parseInt(v, 10)) : v; });
        api('POST', '/purposes', b).then(function (res) { UiAlert.alert(res.message); fill(res.data); loadPurposes(); });
    });

    function loadReqs() {
        api('GET', '/requests?purpose=' + $('qPurpose').value + '&status=' + $('qStatus').value).then(function (res) {
            var tb = $('rRows');
            tb.textContent = '';
            if (!res.data.length) { var tr = document.createElement('tr'); td(tr, '인증 이력이 없습니다.', 'text-muted').colSpan = 13; tb.appendChild(tr); }
            res.data.forEach(function (r) {
                var tr = document.createElement('tr');
                td(tr, String(r.reqAt).replace('T', ' ')); td(tr, r.purposeNm || r.purposeCd); td(tr, r.method); td(tr, r.channel);
                td(tr, r.custNm); td(tr, r.cellNo || r.phoneMask || '-');
                var s = td(tr, ''); var sp = document.createElement('span'); sp.className = 'st st-' + r.status; sp.textContent = r.status; s.appendChild(sp);
                td(tr, r.resultCd || '-'); td(tr, r.age == null ? '-' : r.age); td(tr, r.nationalCd || '-'); td(tr, r.ciVal || '-');
                td(tr, r.agreeNo || '-'); td(tr, r.tryCnt);
                tb.appendChild(tr);
            });
        });
    }
    $('btnReq').addEventListener('click', loadReqs);

    fill(null);
    loadPurposes().then(loadReqs);
})();
</script>
</body>
</html>
