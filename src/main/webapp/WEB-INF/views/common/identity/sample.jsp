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

            <!-- 약관: 서버(/api/common/identity/terms)에서 내려온 목록으로 구성 -->
            <div class="mb-3">
                <div class="form-check border-bottom pb-2 mb-2">
                    <input class="form-check-input" type="checkbox" id="agreeAll">
                    <label class="form-check-label fw-bold" for="agreeAll">전체 동의</label>
                </div>
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

    // 1) 약관 목록 + 상세보기 링크 (textContent 사용 → XSS 안전)
    IdentityAuth.getTerms(METHOD).then(function (terms) {
        var box = document.getElementById('terms');
        terms.forEach(function (t, i) {
            var row = document.createElement('div');
            row.className = 'form-check d-flex justify-content-between';
            var left = document.createElement('div');
            var cb = document.createElement('input');
            cb.type = 'checkbox'; cb.className = 'form-check-input'; cb.name = 'terms'; cb.value = t.code; cb.id = 'terms' + i;
            var label = document.createElement('label');
            label.className = 'form-check-label'; label.htmlFor = cb.id;
            label.textContent = (t.required ? '[필수] ' : '[선택] ') + t.title;
            left.appendChild(cb); left.appendChild(label);
            var view = document.createElement('a');
            view.href = t.contentUrl; view.target = '_blank'; view.className = 'small'; view.textContent = '상세보기';
            row.appendChild(left); row.appendChild(view);
            box.appendChild(row);
        });
    }).catch(log);

    document.getElementById('agreeAll').addEventListener('change', function (e) {
        document.querySelectorAll('input[name=terms]').forEach(function (cb) { cb.checked = e.target.checked; });
    });

    // 2) 본인인증 (반드시 클릭 이벤트 안에서 호출 → 팝업 차단 회피)
    document.getElementById('btnAuth').addEventListener('click', function () {
        var agreed = Array.prototype.map.call(document.querySelectorAll('input[name=terms]:checked'),
            function (cb) { return cb.value; });
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
