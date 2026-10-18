<%--
  =============================================================================
   [CF-BC-03 샘플] 공통 본인인증 사용 예시 화면  -  GET /common/identity/sample
  -----------------------------------------------------------------------------
   업무 화면은 공통 JS 하나(/js/common/identity-auth.js)만 포함하고 IdentityAuth.open() 을 호출한다.
   common.identity.mock-enabled=true 이면 NICE 없이 즉시 인증 완료된다.
   ※ 기존 NICE 화면(/common/auth/nice-cert)과 별개의 화면이다.
  =============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>저축은행 포털 - 공통 본인인증 샘플</title>
    <link rel="shortcut icon" href="/favicon.ico">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css" rel="stylesheet">
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <!-- 본인인증 공통 JS (이 한 줄만 있으면 된다) -->
    <script src="/js/common/identity-auth.js"></script>
    <!-- [CF-BC-04] 약관동의 공통 컴포넌트 - 본인인증 화면 약관도 공통 컴포넌트로 통일 -->
    <script src="/js/common/terms-agree.js"></script>
    <style>
        body { background-color: #f4f6f9; font-family: 'Malgun Gothic', 'Noto Sans KR', sans-serif; }
        .card-header-financial { background-color: #0d47a1; color: white; font-weight: bold; }
        pre.result-log { background: #212529; color: #e9ecef; border-radius: 6px; padding: 12px; min-height: 80px; white-space: pre-wrap; }
    </style>
</head>
<body>
<jsp:include page="/WEB-INF/views/common/navbar.jsp" />

<div class="container" style="max-width: 720px;">
    <div class="card shadow-sm mb-4">
        <div class="card-header card-header-financial">
            <i class="bi bi-person-check me-1"></i> [CF-BC-03] 공통 본인인증 샘플 - 회원가입
        </div>
        <div class="card-body">
            <div class="mb-3">
                <label class="form-label fw-bold" for="channel">호출 채널</label>
                <select id="channel" class="form-select">
                    <option value="PC_WEB">PC WEB (팝업)</option>
                    <option value="RECRUIT_WEB">모집인WEB (팝업)</option>
                    <option value="APP">APP (페이지 이동)</option>
                </select>
            </div>

            <!-- 약관: [CF-BC-04] 약관동의 공통 컴포넌트 (업무코드 IDENTITY_NICE_MOBILE) 로 통일 -->
            <div class="mb-3">
                <div id="terms"></div>
            </div>

            <button id="btnAuth" class="btn btn-primary"><i class="bi bi-phone me-1"></i>휴대폰 본인인증</button>
            <button id="btnJoin" class="btn btn-outline-secondary">가입하기 (업무 API)</button>
        </div>
    </div>

    <div class="card shadow-sm">
        <div class="card-header">처리 결과</div>
        <div class="card-body"><pre id="log" class="result-log mb-0"></pre></div>
    </div>
</div>

<script>
(function () {
    var METHOD = 'NICE_MOBILE', PURPOSE = 'SIGN_UP';
    var logEl = document.getElementById('log');
    function log(o) { logEl.textContent = typeof o === 'string' ? o : JSON.stringify(o, null, 2); }

    // 1) 약관동의 - 공통 컴포넌트를 화면에 삽입 (약관 목록/문구는 서버 ADMIN 등록 약관)
    //    ※ 본인인증 팝업은 클릭 이벤트 안에서 동기적으로 열어야 하므로(팝업 차단),
    //      약관동의 완료 → 사용자가 [휴대폰 본인인증] 버튼을 다시 누르는 2단계로 구성한다.
    var agreedTerms = null;
    TermsAgree.open({ bizCode: 'IDENTITY_NICE_MOBILE', mode: 'inline', container: '#terms', title: '본인인증 약관 동의' })
        .then(function (r) {
            if (r.success) { agreedTerms = r.data.agreedCodes; log({ '약관동의 완료': r.data }); }
        }).catch(log);

    // 2) 본인인증 (반드시 클릭 이벤트 안에서 호출 → 팝업 차단 회피)
    document.getElementById('btnAuth').addEventListener('click', function () {
        if (!agreedTerms) { alert('본인인증 약관에 먼저 동의해 주세요.'); return; }
        var agreed = agreedTerms;
        IdentityAuth.open({
            method: METHOD,
            channel: document.getElementById('channel').value,
            purpose: PURPOSE,
            agreedTerms: agreed,
            returnUrl: '/common/identity/sample'
        }).then(function (result) {
            log(result);
            if (result.success) showResult();
        }).catch(log);
    });

    // 3) 인증 결과 조회 (마스킹된 값만 내려옴, CI 미포함)
    function showResult() {
        IdentityAuth.getResult(PURPOSE).then(function (data) {
            log(data ? { '인증결과(마스킹)': data } : '인증 결과 없음');
        }).catch(log);
    }

    // 4) 업무 API 호출 - 서버가 세션의 인증결과(CI)를 직접 사용
    document.getElementById('btnJoin').addEventListener('click', function () {
        fetch('/api/common/identity/sample/join', {
            method: 'POST', credentials: 'same-origin', headers: { 'X-Requested-With': 'XMLHttpRequest' }
        }).then(function (r) { return r.json(); }).then(log);
    });

    // APP 채널로 돌아온 경우 (?identityResult=success|fail&code=...)
    if (location.search.indexOf('identityResult=') >= 0) showResult();
})();
</script>
</body>
</html>
