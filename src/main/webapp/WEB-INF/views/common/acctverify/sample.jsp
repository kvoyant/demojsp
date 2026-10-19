<%--
  =============================================================================
   [CF-BC-06 / 공통#25] 1원 계좌인증 (DB 방식) 샘플  -  GET /common/acct-verify/sample
  -----------------------------------------------------------------------------
   요구사항 (REQ-AP-605)
     - 1원 인증 서비스는 하나, 업무 Context 에 따라 화면 분리
         LOAN   (대출 입출금계좌용) : 대출금 입금/출금계좌 자동완성 → [수정] 시 은행·계좌 직접 입력
         COMMON (타 업무 공통)      : 은행 선택 + 계좌번호 입력
     - 업무 Context(화면 종류 · 자동완성 · 예금주 확인 · 입금자명 접두어 · 유효시간 · 횟수)는 DB (TB_CO_ACCT_BIZ)
   업무 화면 사용법:  AcctVerify.open({ bizCd: 'LOAN_DISBURSE', channel: 'APP', onComplete: function (r) { ... r.reqNo ... } });
   업무 서버:        AvResult r = acctVerifyService.checkVerified(reqNo, "LOAN_DISBURSE", request);   // r.getAcctNo()
   기존 1원 인증 화면(domain/accountcert)은 그대로 두었다.
   주의: JSP 에서는 JS 템플릿 문자열의 달러+중괄호가 EL 로 해석되므로 문자열 더하기를 사용한다.
  =============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>저축은행 포털 - 1원 계좌인증</title>
    <link rel="shortcut icon" href="/favicon.ico">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css" rel="stylesheet">
    <link rel="stylesheet" href="/css/common/ui-tokens.css">
    <link rel="stylesheet" href="/css/common/acct-verify.css">
    <script src="/js/common/acct-verify.js"></script>
    <style>
        body { background-color: #f4f6f9; font-family: 'Malgun Gothic', 'Noto Sans KR', sans-serif; }
        [hidden] { display: none !important; }
        pre.result-log { background: #212529; color: #e9ecef; border-radius: 6px; padding: 10px; max-height: 360px; overflow: auto; white-space: pre-wrap; font-size: 12px; margin: 0; }
        .table-sm td, .table-sm th { font-size: 12px; vertical-align: middle; }
        .biz-btn { text-align: left; }
        .biz-btn small { display: block; color: #6b7280; font-size: 11px; }
        .ctx-badge { font-size: 11px; padding: 1px 6px; border-radius: 8px; margin-right: 4px; }
        .ctx-LOAN { background: #e3ecfa; color: #0d47a1; } .ctx-COMMON { background: #e6f4ea; color: #1b7a3d; }
    </style>
</head>
<body>
<jsp:include page="/WEB-INF/views/common/navbar.jsp" />

<div class="container-xl pb-5">
    <div class="d-flex align-items-center mb-3">
        <a href="/common/test-hub" class="btn btn-sm btn-outline-secondary me-2"><i class="bi bi-arrow-left"></i> 테스트 목록</a>
        <h5 class="fw-bold mb-0">[공통#25] 1원 계좌인증 - DB 방식</h5>
        <a href="/common/acct-verify/admin" class="btn btn-sm btn-outline-primary ms-auto"><i class="bi bi-gear"></i> 디지털어드민 계좌인증 관리</a>
    </div>
    <div class="alert alert-info small py-2">
        1원 인증 <b>서비스는 하나</b>이고 <b>업무 Context</b>(DB)에 따라 화면이 달라집니다.
        <span class="ctx-badge ctx-LOAN">LOAN</span> 대출 입출금계좌 등록은 <b>대출금 입금/출금계좌를 자동완성</b>하고, 다른 계좌는 <b>[다른 계좌로 수정]</b>으로 직접 입력합니다.
        <span class="ctx-badge ctx-COMMON">COMMON</span> 인증수단 등록·환불계좌 등은 은행 선택 + 계좌번호 입력입니다.
        local 에서는 펌뱅킹 <b>시뮬레이터</b>가 입금자명(예: 페퍼1234)을 화면에 알려 줍니다.
    </div>

    <div class="row g-4">
        <div class="col-lg-5">
            <div class="card shadow-sm mb-4">
                <div class="card-header fw-bold"><i class="bi bi-person me-1"></i> 고객 (local 샘플 전환)</div>
                <div class="card-body">
                    <select id="customer" class="form-select form-select-sm mb-2">
                        <option value="customer01|홍길동">customer01 홍길동 - 대출 2건 보유 (자동완성 O)</option>
                        <option value="|김신규">비회원 김신규 - 대출 없음 (자동완성 X)</option>
                    </select>
                    <div class="d-flex gap-2 align-items-center">
                        <label class="small text-nowrap" for="channel">채널</label>
                        <select id="channel" class="form-select form-select-sm" style="max-width:160px">
                            <option value="APP">APP</option><option value="PC_WEB">PC_WEB</option><option value="AGENT_WEB">AGENT_WEB</option>
                        </select>
                    </div>
                    <p class="small text-muted mb-0 mt-2">실제 업무: 로그인 회원은 세션, 비로그인은 공통#21 본인인증 결과(<code>params: {idvReqNo, idvPurposeCd}</code>)로 고객 실명을 확인합니다.</p>
                </div>
            </div>
            <div class="card shadow-sm mb-4">
                <div class="card-header fw-bold"><i class="bi bi-1-circle me-1"></i> 업무 선택 → 1원 계좌인증 시작</div>
                <div class="card-body"><div id="bizs" class="d-grid gap-2"><span class="small text-muted">업무 Context 불러오는 중...</span></div></div>
            </div>
            <div class="card shadow-sm mb-4">
                <div class="card-header fw-bold">시뮬레이터 테스트 계좌 (직접 입력)</div>
                <div class="card-body p-0">
                    <table class="table table-sm mb-0">
                        <thead class="table-light"><tr><th>계좌번호 끝자리</th><th>예상 결과</th></tr></thead>
                        <tbody>
                            <tr><td>그 외 (예: 1002123456789)</td><td>예금주 = 고객 → 1원 송금 성공</td></tr>
                            <tr><td>…0000</td><td>없는 계좌 (AVF-104)</td></tr>
                            <tr><td>…9999</td><td>예금주 김타인 → 본인 명의 아님 (AVF-105)</td></tr>
                            <tr><td>…8888</td><td>1원 송금 실패 (AVF-201)</td></tr>
                        </tbody>
                    </table>
                </div>
            </div>
            <div class="card shadow-sm">
                <div class="card-header fw-bold"><i class="bi bi-2-circle me-1"></i> 다음 단계 (업무 서버 확인)</div>
                <div class="card-body">
                    <div id="resultBox" class="small text-muted">계좌인증을 완료하면 요청번호(reqNo)가 표시됩니다.</div>
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
    function demoParams() {
        var v = $('customer').value.split('|');
        return { demoCustomer: v[0], demoName: v[1] };
    }

    // 업무 Context 버튼 (샘플 편의상 어드민 API 로 목록을 가져온다)
    fetch('/api/common/acct-verify-admin/bizs').then(function (r) { return r.json(); }).then(function (res) {
        var box = $('bizs');
        box.textContent = '';
        if (!res.success) { box.textContent = res.message; return; }
        res.data.filter(function (b) { return b.useYn === 'Y'; }).forEach(function (b) {
            var btn = document.createElement('button');
            btn.className = 'btn btn-outline-primary biz-btn';
            var badge = document.createElement('span');
            badge.className = 'ctx-badge ctx-' + b.screenType;
            badge.textContent = b.screenType;
            btn.appendChild(badge);
            btn.appendChild(document.createTextNode(b.bizNm + ' (' + b.bizCd + ')'));
            var sm = document.createElement('small');
            sm.textContent = [b.prefillYn === 'Y' ? '대출계좌 자동완성' : '직접 입력', b.ownerCheckYn === 'Y' ? '예금주 본인 확인' : '',
                '입금자명 ' + b.memoPrefix + '0000', b.codeValidMin + '분', '입력 ' + b.maxTry + '회'].filter(Boolean).join(' · ');
            btn.appendChild(sm);
            btn.addEventListener('click', function () { start(b.bizCd); });
            box.appendChild(btn);
        });
    });

    // ★ 업무 화면에서 쓰는 부분은 이것뿐 (params 의 demo* 는 local 샘플 고객 전환용)
    function start(bizCd) {
        log('AcctVerify.open({ bizCd: \'' + bizCd + '\' }) ...');
        AcctVerify.open({
            bizCd: bizCd,
            channel: $('channel').value,
            params: demoParams(),
            onComplete: function (r) {
                last = r;
                log(r);
                $('resultBox').className = 'small';
                $('resultBox').textContent = '인증 성공 · reqNo ' + r.reqNo + ' · ' + r.bankNm + ' ' + r.acctMask + ' (' + r.holderNm + ')'
                    + ' · ' + (r.source === 'PREFILL' ? '자동완성' : '직접 입력' + (r.modifiedYn === 'Y' ? '(수정)' : ''));
                $('btnNext').disabled = false;
            },
            onFail: function (r) { log(r); $('resultBox').textContent = '인증 실패: ' + (r.resultCd || ''); $('btnNext').disabled = true; },
            onCancel: function () { log('사용자가 계좌인증을 닫았습니다 (onCancel).'); }
        });
    }

    $('btnNext').addEventListener('click', function () {
        var p = demoParams();
        p.reqNo = last.reqNo;
        p.bizCd = last.bizCd;
        fetch('/common/acct-verify/sample/next', { method: 'POST', body: new URLSearchParams(p) })
            .then(function (r) { return r.json(); }).then(log);
    });
})();
</script>
</body>
</html>
