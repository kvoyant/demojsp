<%--
  =============================================================================
   [CF-BC-13 / 공통#43] 신청현황조회 (샘플)  -  GET /common/resume/status?scope=ALL
  -----------------------------------------------------------------------------
   홈 이어하기 카드의 [더보기] 이동 화면. 실제 화면은 REQ-IA-APP-0015 신청현황조회이며,
   같은 데이터(GET /api/common/resume/list)와 같은 카드 표현(로고·회사명·상품명·이어하기)을 사용한다.
  =============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>저축은행 포털 - 신청현황조회</title>
    <link rel="shortcut icon" href="/favicon.ico">
    <link rel="stylesheet" href="/css/common/ui-tokens.css">
    <link rel="stylesheet" href="/css/common/ui-button.css">
    <link rel="stylesheet" href="/css/common/ui-icon.css">
    <link rel="stylesheet" href="/css/common/ui-alert.css">
    <link rel="stylesheet" href="/css/common/resume-card.css">
    <script src="/js/common/ui-button.js"></script>
    <script src="/js/common/ui-alert.js"></script>
    <script src="/js/common/resume-card.js"></script>
    <style>
        body { margin: 0; background: #f5f7fb; font-family: var(--ui-font); color: var(--ui-text); }
        .wrap { max-width: 480px; margin: 0 auto; min-height: 100vh; background: #f5f7fb; }
        header { position: sticky; top: 0; background: #fff; display: flex; align-items: center; gap: 8px; padding: 12px 16px; border-bottom: 1px solid #eef0f3; z-index: 1; }
        header a { color: var(--ui-text); text-decoration: none; font-size: 20px; line-height: 1; padding: 4px; }
        header h1 { font-size: 17px; margin: 0; }
        .tabs { display: flex; gap: 6px; padding: 12px 16px 4px; }
        .tabs a { padding: 6px 12px; border-radius: 999px; border: 1px solid var(--ui-line); background: #fff; font-size: 14px; color: var(--ui-text-sub); text-decoration: none; }
        .tabs a[aria-current="page"] { background: var(--ui-primary); border-color: var(--ui-primary); color: #fff; font-weight: 700; }
        .list { padding: 8px 16px 32px; display: flex; flex-direction: column; gap: 10px; }
        .list .ui-resume__head { display: none; }       /* 목록에서는 건별 카드 - 머리(건수·더보기) 생략 */
        .empty { text-align: center; color: var(--ui-text-sub); padding: 48px 0; font-size: 14px; }
        .total { padding: 4px 16px 0; font-size: 13px; color: var(--ui-text-sub); }
    </style>
</head>
<body>
<div class="wrap">
    <header>
        <a href="/common/resume/sample" aria-label="뒤로">&#8249;</a>
        <h1>신청현황조회</h1>
    </header>
    <nav class="tabs" id="tabs" aria-label="상품 구분">
        <a data-scope="ALL" href="#">전체</a><a data-scope="DEPOSIT" href="#">수신</a><a data-scope="LOAN" href="#">여신</a>
    </nav>
    <p class="total" id="total" aria-live="polite"></p>
    <div class="list" id="list"></div>
</div>
<script>
(function () {
    var scope = '${scope}';
    var demoCustomer = '${demoCustomer}';
    var params = demoCustomer ? { demoCustomer: demoCustomer } : {};
    var qs = demoCustomer ? '&demoCustomer=' + encodeURIComponent(demoCustomer) : '';

    document.querySelectorAll('#tabs a').forEach(function (a) {
        var s = a.getAttribute('data-scope');
        a.href = '/common/resume/status?scope=' + s + qs;
        if (s === scope) a.setAttribute('aria-current', 'page');
    });

    fetch('/api/common/resume/list?scope=' + encodeURIComponent(scope) + qs, { credentials: 'same-origin', headers: { 'X-Requested-With': 'XMLHttpRequest' } })
        .then(function (r) { return r.json(); })
        .then(function (res) {
            var list = document.getElementById('list');
            if (!res.success) { list.appendChild(Object.assign(document.createElement('p'), { className: 'empty', textContent: res.message })); return; }
            document.getElementById('total').textContent = '진행 중 ' + res.data.length + '건 (최근 진행 순)';
            if (!res.data.length) { list.appendChild(Object.assign(document.createElement('p'), { className: 'empty', textContent: '진행 중인 신청이 없습니다.' })); return; }
            // 같은 카드 컴포넌트로 건별 렌더 (count=1 요약을 만들어 전달)
            res.data.forEach(function (item) {
                var host = document.createElement('div');
                list.appendChild(host);
                ResumeCard.render(host, { scope: scope, count: 1, latest: item, moreUrl: '#' }, { params: params });
            });
        });
})();
</script>
</body>
</html>
