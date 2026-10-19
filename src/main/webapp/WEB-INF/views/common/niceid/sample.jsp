<%--
  =============================================================================
   [CF-BC-03 / 공통#21] 휴대폰 본인인증 NICE ID (DB 방식) 샘플  -  GET /common/niceid/sample
  -----------------------------------------------------------------------------
   요구사항 (REQ-AP-601)
     - 통신사 미지정: 성명 + 생년월일 + 휴대폰번호 (통신사 선택 없음)
     - MO 인증 결합: 휴대폰번호 입력 생략 + 기기 소지 확인
     - 비여신금융거래 약관 추가: 공통#22 약관동의(업무코드 IDENTITY_NICE) → 약관 목록은 DB
     - 주민번호 → CI: 유의고객(CI) · 미성년자(생년월일) · 외국인(NICE 내외국인) 검사 - 업무별 기준은 DB 정책
   업무 화면 사용법:  NiceIdAuth.open({ purposeCd: 'SIGN_UP', channel: 'APP', onComplete: function (r) { ... r.reqNo ... } });
   업무 서버:        IdvResult r = niceIdService.checkVerified(reqNo, "SIGN_UP", request);   // r.getCi()
   기존 NICE 화면(/common/auth/nice-cert, /common/identity/sample)은 그대로 두었다.
   주의: JSP 에서는 JS 템플릿 문자열의 달러+중괄호가 EL 로 해석되므로 문자열 더하기를 사용한다.
  =============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>저축은행 포털 - 휴대폰 본인인증 (NICE ID)</title>
    <link rel="shortcut icon" href="/favicon.ico">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css" rel="stylesheet">
    <link rel="stylesheet" href="/css/common/ui-tokens.css">
    <link rel="stylesheet" href="/css/common/ui-alert.css">
    <link rel="stylesheet" href="/css/common/consent-agree.css">
    <link rel="stylesheet" href="/css/common/nice-id.css">
    <script src="/js/common/ui-alert.js"></script>
    <script src="/js/common/consent-agree.js"></script>
    <script src="/js/common/nice-id.js"></script>
    <style>
        body { background-color: #f4f6f9; font-family: 'Malgun Gothic', 'Noto Sans KR', sans-serif; }
        [hidden] { display: none !important; }
        pre.result-log { background: #212529; color: #e9ecef; border-radius: 6px; padding: 10px; max-height: 320px; overflow: auto; white-space: pre-wrap; font-size: 12px; margin: 0; }
        .table-sm td, .table-sm th { font-size: 12px; vertical-align: middle; }
        .biz-btn { text-align: left; }
        .biz-btn small { display: block; color: #6b7280; font-size: 11px; }
        code { font-size: 12px; }
    </style>
</head>
<body>
<jsp:include page="/WEB-INF/views/common/navbar.jsp" />

<div class="container-xl pb-5">
    <div class="d-flex align-items-center mb-3">
        <a href="/common/test-hub" class="btn btn-sm btn-outline-secondary me-2"><i class="bi bi-arrow-left"></i> 테스트 목록</a>
        <h5 class="fw-bold mb-0">[공통#21] 휴대폰 본인인증 NICE ID - DB 방식</h5>
        <a href="/common/niceid/admin" class="btn btn-sm btn-outline-primary ms-auto"><i class="bi bi-gear"></i> 디지털어드민 본인인증 정책</a>
    </div>
    <div class="alert alert-info small py-2">
        <b>통신사 선택 없이</b> 성명·생년월일·휴대폰번호로 인증하거나, <b>번호 입력 없이</b>(MO) 고객 휴대폰에서 문자를 보내 인증합니다.
        먼저 <b>비여신금융거래 약관</b> 등 본인인증 약관(공통#22, DB)에 동의하고, 인증 결과는 업무별 정책(DB)에 따라 <b>유의고객 · 미성년자 · 외국인</b>을 CI·생년월일·내외국인으로 검사합니다.
        local 에서는 NICE <b>시뮬레이터</b>가 인증번호를 자동 입력합니다.
    </div>

    <div class="row g-4">
        <div class="col-lg-5">
            <div class="card shadow-sm mb-4">
                <div class="card-header fw-bold"><i class="bi bi-1-circle me-1"></i> 업무 선택 → 본인인증 시작</div>
                <div class="card-body">
                    <div class="d-flex gap-2 align-items-center mb-2">
                        <label class="small text-nowrap" for="channel">채널</label>
                        <select id="channel" class="form-select form-select-sm" style="max-width:160px">
                            <option value="APP">APP</option><option value="PC_WEB">PC_WEB</option>
                        </select>
                    </div>
                    <div id="purposes" class="d-grid gap-2"><span class="small text-muted">업무 정책 불러오는 중...</span></div>
                </div>
            </div>
            <div class="card shadow-sm mb-4">
                <div class="card-header fw-bold">시뮬레이터 테스트 인물</div>
                <div class="card-body p-0">
                    <table class="table table-sm mb-0">
                        <thead class="table-light"><tr><th>성명</th><th>생년월일</th><th>휴대폰</th><th>예상 결과</th></tr></thead>
                        <tbody>
                            <tr><td>홍길동</td><td>19900101</td><td>01012345678</td><td>성공</td></tr>
                            <tr><td>김미성</td><td>20100505</td><td>01022223333</td><td>대출신청: 미성년 차단 (만 19세)</td></tr>
                            <tr><td>John Smith</td><td>19850101</td><td>01033334444</td><td>대출신청: 외국인 차단</td></tr>
                            <tr><td>김유의</td><td>19800101</td><td>01099990000</td><td>유의고객 차단 (TB_WATCHLIST CI)</td></tr>
                        </tbody>
                    </table>
                </div>
            </div>
            <div class="card shadow-sm">
                <div class="card-header fw-bold"><i class="bi bi-2-circle me-1"></i> 다음 단계 (업무 서버 확인)</div>
                <div class="card-body">
                    <div id="resultBox" class="small text-muted">본인인증을 완료하면 요청번호(reqNo)가 표시됩니다.</div>
                    <button id="btnNext" class="btn btn-primary btn-sm mt-2" disabled>다음 단계 진행 (checkVerified)</button>
                </div>
            </div>
        </div>
        <div class="col-lg-7">
            <div class="card shadow-sm">
                <div class="card-header">처리 결과 / API 응답</div>
                <div class="card-body"><pre id="log" class="result-log">-</pre></div>
            </div>
        </div>
    </div>
</div>

<script>
(function () {
    var $ = function (id) { return document.getElementById(id); };
    var last = null;
    function log(o) { $('log').textContent = typeof o === 'string' ? o : JSON.stringify(o, null, 2); }
    function yn(v, t) { return v === 'Y' ? t : ''; }

    // 업무 정책 버튼 (샘플 편의상 어드민 API 로 목록을 가져온다)
    fetch('/api/common/niceid-admin/purposes').then(function (r) { return r.json(); }).then(function (res) {
        var box = $('purposes');
        box.textContent = '';
        if (!res.success) { box.textContent = res.message; return; }
        res.data.filter(function (p) { return p.useYn === 'Y'; }).forEach(function (p) {
            var b = document.createElement('button');
            b.className = 'btn btn-outline-primary biz-btn';
            b.textContent = p.purposeNm + ' (' + p.purposeCd + ')';
            var sm = document.createElement('small');
            sm.textContent = [p.consentBizCd ? '약관 ' + p.consentBizCd : '약관 없음', yn(p.smsYn, 'SMS'), yn(p.moYn, 'MO'),
                p.minAge ? '만 ' + p.minAge + '세 이상' : '', p.foreignerYn === 'N' ? '외국인 차단' : '', p.watchCheckYn === 'Y' ? '유의고객 검사' : '']
                .filter(Boolean).join(' · ');
            b.appendChild(sm);
            b.addEventListener('click', function () { start(p.purposeCd); });
            box.appendChild(b);
        });
    });

    // ★ 업무 화면에서 쓰는 부분은 이것뿐 (demo: true 는 local 시뮬레이터 MO 버튼 표시용)
    function start(purposeCd) {
        log('NiceIdAuth.open({ purposeCd: \'' + purposeCd + '\' }) ...');
        NiceIdAuth.open({
            purposeCd: purposeCd,
            channel: $('channel').value,
            demo: true,
            onComplete: function (r) {
                last = r;
                log(r);
                $('resultBox').className = 'small';
                $('resultBox').textContent = '인증 성공 · reqNo ' + r.reqNo + ' · ' + r.name + ' (만 ' + r.age + '세) · ' + r.cellNoMask;
                $('btnNext').disabled = false;
            },
            onFail: function (r) { log(r); $('resultBox').textContent = '인증 실패/차단: ' + r.resultCd; $('btnNext').disabled = true; },
            onCancel: function () { log('사용자가 본인인증을 닫았습니다 (onCancel).'); }
        });
    }

    $('btnNext').addEventListener('click', function () {
        fetch('/common/niceid/sample/next', { method: 'POST', body: new URLSearchParams({ reqNo: last.reqNo, purposeCd: last.purposeCd }) })
            .then(function (r) { return r.json(); }).then(log);
    });
})();
</script>
</body>
</html>
