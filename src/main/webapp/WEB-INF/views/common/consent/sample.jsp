<%--
  =============================================================================
   [CF-BC-04 / 공통#22] 약관동의 (DB 방식) 샘플  -  GET /common/consent/sample
  -----------------------------------------------------------------------------
   업무 화면 사용법 (이것만 쓰면 된다)
     <link rel="stylesheet" href="/css/common/consent-agree.css">
     <script src="/js/common/consent-agree.js"></script>
     ConsentAgree.open({ bizCd: 'SIGN_UP', channel: 'APP', onComplete: function (r) { ... r.agreeNo ... } });

   업무 서버 (다음 단계 Controller/Service)
     ConsentResult r = consentService.checkAgreed(agreeNo, "SIGN_UP", request);

   약관 목록·문구·필수여부·순서는 모두 DB (디지털어드민 약관관리 /common/consent/admin) 값이다.
   기존 약관동의 샘플(/common/terms/sample, yaml 방식)은 그대로 두었다.
   주의: JSP 에서는 JS 템플릿 문자열의 달러+중괄호가 EL 로 해석되므로 문자열 더하기를 사용한다.
  =============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>저축은행 포털 - 약관동의 (DB 방식)</title>
    <link rel="shortcut icon" href="/favicon.ico">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css" rel="stylesheet">
    <link rel="stylesheet" href="/css/common/ui-tokens.css">
    <link rel="stylesheet" href="/css/common/ui-alert.css">
    <link rel="stylesheet" href="/css/common/consent-agree.css">
    <script src="/js/common/ui-alert.js"></script>
    <script src="/js/common/consent-agree.js"></script>
    <style>
        body { background-color: #f4f6f9; font-family: 'Malgun Gothic', 'Noto Sans KR', sans-serif; }
        [hidden] { display: none !important; }
        pre.result-log { background: #212529; color: #e9ecef; border-radius: 6px; padding: 10px; max-height: 360px; overflow: auto; white-space: pre-wrap; font-size: 12px; margin: 0; }
        .biz-btn { text-align: left; }
        .biz-btn small { display: block; color: #6b7280; font-size: 11px; }
        .flow li { margin-bottom: 4px; }
        code { font-size: 12px; }
    </style>
</head>
<body>
<jsp:include page="/WEB-INF/views/common/navbar.jsp" />

<div class="container-xl pb-5">
    <div class="d-flex align-items-center mb-3">
        <a href="/common/test-hub" class="btn btn-sm btn-outline-secondary me-2"><i class="bi bi-arrow-left"></i> 테스트 목록</a>
        <h5 class="fw-bold mb-0">[공통#22] 약관동의 - DB 방식 (필수 → 선택 2페이지)</h5>
        <a href="/common/consent/admin" class="btn btn-sm btn-outline-primary ms-auto"><i class="bi bi-gear"></i> 디지털어드민 약관관리</a>
    </div>
    <div class="alert alert-info small py-2 mb-3">
        업무 화면은 <b>업무코드만</b> 넘깁니다. 어떤 약관을 필수/선택으로 몇 번째에 보여줄지, 약관 이름·전문·버전은 모두 <b>DB</b>(디지털어드민 등록값)에서 읽어 옵니다.
        어드민에서 바꾸면 이 화면에 <b>바로</b> 반영됩니다. 동의 결과는 약관별 <b>버전·전문 해시</b>와 함께 저장되어 어드민 동의내역/증적으로 쓰입니다.
    </div>

    <div class="row g-4">
        <div class="col-lg-5">
            <div class="card shadow-sm mb-4">
                <div class="card-header fw-bold"><i class="bi bi-1-circle me-1"></i> 업무 선택 → 약관동의 시작</div>
                <div class="card-body">
                    <div class="d-flex gap-2 align-items-center mb-2">
                        <label class="small text-nowrap" for="channel">채널</label>
                        <select id="channel" class="form-select form-select-sm" style="max-width:160px">
                            <option value="APP">APP</option>
                            <option value="PC_WEB">PC_WEB</option>
                            <option value="AGENT_WEB">AGENT_WEB (모집인)</option>
                        </select>
                    </div>
                    <div id="bizList" class="d-grid gap-2"><span class="small text-muted">업무코드 불러오는 중...</span></div>
                    <div class="small text-muted mt-2">업무코드 목록도 DB(TB_CO_TERMS_BIZ)에서 읽었습니다.</div>
                </div>
            </div>
            <div class="card shadow-sm">
                <div class="card-header fw-bold"><i class="bi bi-2-circle me-1"></i> 다음 단계 (업무 서버 확인)</div>
                <div class="card-body">
                    <div id="resultBox" class="small text-muted">약관동의를 완료하면 동의번호(agreeNo)가 표시됩니다.</div>
                    <button id="btnNext" class="btn btn-primary btn-sm mt-2" disabled>다음 단계 진행 (checkAgreed)</button>
                    <button id="btnFake" class="btn btn-outline-danger btn-sm mt-2" disabled>남의 동의번호로 진행 (실패 예)</button>
                </div>
            </div>
        </div>
        <div class="col-lg-7">
            <div class="card shadow-sm mb-4">
                <div class="card-header">처리 결과 / API 응답</div>
                <div class="card-body"><pre id="log" class="result-log">-</pre></div>
            </div>
            <div class="card shadow-sm">
                <div class="card-header">흐름</div>
                <div class="card-body small">
                    <ol class="flow mb-0">
                        <li><code>GET /api/common/consent/{업무코드}</code> → 필수 목록 / 선택 목록 (DB 조회)</li>
                        <li>1페이지 필수 전체동의 → [다음] (모두 체크해야 활성) → 2페이지 선택 전체동의 → [확인]</li>
                        <li>약관 이름 터치 → <code>GET /api/common/consent/terms/{약관코드}/{버전}</code> 전문 → [동의]</li>
                        <li><code>POST /api/common/consent/{업무코드}/agree</code> → 서버가 DB 기준으로 필수 동의 재검사 → <b>agreeNo</b></li>
                        <li>업무 서버 <code>consentService.checkAgreed(agreeNo, 업무코드, request)</code> → 본인 동의·필수·유효시간 확인</li>
                    </ol>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
(function () {
    var $ = function (id) { return document.getElementById(id); };
    var last = null;   // 마지막 동의 결과
    function log(o) { $('log').textContent = typeof o === 'string' ? o : JSON.stringify(o, null, 2); }

    // ---- 업무코드 버튼 (샘플 편의상 어드민 API 로 목록을 가져온다)
    fetch('/api/common/consent-admin/biz').then(function (r) { return r.json(); }).then(function (res) {
        var box = $('bizList');
        box.textContent = '';
        if (!res.success) { box.textContent = res.message; return; }
        res.data.filter(function (b) { return b.useYn === 'Y'; }).forEach(function (b) {
            var btn = document.createElement('button');
            btn.className = 'btn btn-outline-primary biz-btn';
            btn.textContent = b.bizNm + ' (' + b.bizCd + ')';
            var sm = document.createElement('small');
            sm.textContent = '필수 ' + b.reqCnt + ' · 선택 ' + b.optCnt + (b.bizDesc ? ' · ' + b.bizDesc : '');
            btn.appendChild(sm);
            btn.addEventListener('click', function () { start(b.bizCd); });
            box.appendChild(btn);
        });
    });

    // ---- ★ 업무 화면에서 쓰는 부분은 이것뿐
    function start(bizCd) {
        log('ConsentAgree.open({ bizCd: \'' + bizCd + '\' }) ...');
        ConsentAgree.open({
            bizCd: bizCd,
            channel: $('channel').value,
            onComplete: function (result) {
                last = result;
                log(result);
                $('resultBox').className = 'small';
                $('resultBox').textContent = '동의번호 ' + result.agreeNo + ' · 업무 ' + result.bizCd
                    + ' · 동의 ' + result.agreedTerms.join(', ')
                    + (result.declinedTerms.length ? ' · 미동의(선택) ' + result.declinedTerms.join(', ') : '');
                $('btnNext').disabled = false;
                $('btnFake').disabled = false;
            },
            onCancel: function () { log('사용자가 약관동의를 닫았습니다 (onCancel).'); }
        });
    }

    // ---- 다음 단계: 업무 서버가 DB 동의내역으로 확인
    function next(agreeNo) {
        var body = new URLSearchParams({ bizCd: last.bizCd, agreeNo: agreeNo });
        fetch('/common/consent/sample/next', { method: 'POST', body: body })
            .then(function (r) { return r.json(); }).then(log);
    }
    $('btnNext').addEventListener('click', function () { next(last.agreeNo); });
    $('btnFake').addEventListener('click', function () { next(last.agreeNo + 100000); });
})();
</script>
</body>
</html>
