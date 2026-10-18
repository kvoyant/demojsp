<%--
  =============================================================================
   [공통#49~ 샘플] APP 저축 탭  -  GET /common/savings/sample
  -----------------------------------------------------------------------------
   공통#49 저축 - 이어하기 : 저축 탭 상단 이어하기 카드 (수신상품만, 당행 로고)
                             업무 화면은 ResumeCard.mount('#resumeCard', { scope: 'DEPOSIT' }) 한 줄
   공통#50 이어하기 툴팁   : 하단 '저축' 탭 숫자 배지 - 홈/저축/대출 어느 탭에서든 표시, 0건·취소 시 숨김
                             업무 화면은 BottomNav.mount('#bottomNav', { active: 'SAVINGS' }) 한 줄
   공통#51 저축 메인 콘텐츠 : 계좌 미보유 - 텍스트 + 이미지 + [계좌 개설하기] 하단 상시 (계좌 보유 - 계좌정보)
                             업무 화면은 SavingsMain.mount('#savingsMain', { cta: '#savingsCta' }) 한 줄
   주의: JSP 에서는 JS 템플릿 문자열의 달러+중괄호가 EL 로 해석되므로 문자열 더하기를 사용한다.
  =============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>저축은행 포털 - 저축 탭 샘플</title>
    <link rel="shortcut icon" href="/favicon.ico">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css" rel="stylesheet">
    <link rel="stylesheet" href="/css/common/ui-tokens.css">
    <link rel="stylesheet" href="/css/common/ui-button.css">
    <link rel="stylesheet" href="/css/common/ui-icon.css">
    <link rel="stylesheet" href="/css/common/ui-alert.css">
    <link rel="stylesheet" href="/css/common/resume-card.css">
    <link rel="stylesheet" href="/css/common/bottom-nav.css">
    <link rel="stylesheet" href="/css/common/savings-main.css">
    <script src="/js/common/ui-button.js"></script>
    <script src="/js/common/ui-alert.js"></script>
    <script src="/js/common/resume-card.js"></script>
    <script src="/js/common/bottom-nav.js"></script>
    <script src="/js/common/savings-main.js"></script>
    <style>
        body { background-color: #f4f6f9; font-family: 'Malgun Gothic', 'Noto Sans KR', sans-serif; }
        [hidden] { display: none !important; }
        pre.result-log { background: #212529; color: #e9ecef; border-radius: 6px; padding: 10px; max-height: 300px; overflow: auto; white-space: pre-wrap; font-size: 12px; margin: 0; }
        .table-sm td, .table-sm th { font-size: 12px; vertical-align: middle; }
        /* APP 목업: 상단 헤더 / 본문 스크롤 / 하단 탭바 */
        .phone { width: 100%; max-width: 360px; height: 680px; margin: 0 auto; border: 10px solid #1f2937; border-radius: 36px; background: #f5f7fb; overflow: hidden; display: flex; flex-direction: column; }
        .phone-top { background: #fff; padding: 16px 18px 12px; font-weight: 700; font-size: 17px; flex: 0 0 auto; }
        .phone-body { flex: 1 1 auto; overflow: auto; position: relative; }
        .phone-slot { padding: 0 12px; }
        #resumeCard { margin-top: 12px; }
        .phone-sec { background: #fff; border-radius: 16px; margin: 12px; padding: 14px; font-size: 13px; color: #6b7280; }
        .phone-nav { flex: 0 0 auto; }
        .phone-cta { flex: 0 0 auto; }                    /* 본문 밖 → 스크롤해도 탭바 위에 항상 보임 */
        .phone-cta .ui-cta-bar { position: static; background: #fff; border-top: 1px solid #eef0f3; padding: 10px 14px; }
        .acct { background: #fff; border-radius: 16px; margin: 12px; padding: 16px 18px; box-shadow: 0 1px 4px rgba(0,0,0,.06); }
        .acct small { color: #6b7280; }
        .acct .amt { font-size: 22px; font-weight: 800; }
        .phone-sec-wide { height: 260px; }
    </style>
</head>
<body>
<jsp:include page="/WEB-INF/views/common/navbar.jsp" />

<div class="container-xl pb-5">
    <div class="d-flex align-items-center mb-3">
        <a href="/common/test-hub" class="btn btn-sm btn-outline-secondary me-2"><i class="bi bi-arrow-left"></i> 테스트 목록</a>
        <h5 class="fw-bold mb-0">[공통#49·50·51] APP 저축 탭</h5>
    </div>
    <div class="alert alert-info small py-2">
        <b>공통#49 저축 - 이어하기</b>: 저축 탭 상단에 <b>수신상품</b> 이어하기 중 최신 1건 + 건수 + [더보기](신청현황조회) + [이어하기].
        로고는 당행 로고(수신상품은 당행 상품만). 홈 이어하기(공통#43)와 같은 카드 컴포넌트를 <code>scope: 'DEPOSIT'</code> 으로 재사용합니다.<br>
        <b>공통#50 이어하기 툴팁</b>: 수신 이어하기가 있으면 하단 <b>'저축' 탭에 숫자</b>(진행 중 건수). 현재 탭이 홈·저축·대출 어디든 표시되고,
        진행 건이 없거나 취소로 남은 건이 없으면 사라집니다. 목업 하단 탭을 눌러 이동해 보세요.<br>
        <b>공통#51 저축 메인 콘텐츠</b>: <b>계좌 미보유</b> 고객(로그인 전·후 같은 화면)에게 "매일 쌓이는 이자 / 모으는 재미가 쏠쏠" + 이미지 +
        <b>[계좌 개설하기]</b>(하단 상시, 입출금통장 상품안내페이지). 계좌 보유 고객은 같은 자리에 계좌정보.
    </div>

    <div class="row g-4">
        <div class="col-lg-5">
            <div class="card shadow-sm">
                <div class="card-header fw-bold text-white" style="background:#0d47a1"><i class="bi bi-phone me-1"></i> APP 저축 탭</div>
                <div class="card-body">
                    <div class="phone">
                        <div class="phone-top" id="phoneTitle">저축</div>
                        <div class="phone-body" id="phoneBody">
                            <!-- ★ 공통#49 저축 이어하기 (수신 0건이면 자리 차지 안 함) -->
                            <div class="phone-slot"><div id="resumeCard"></div></div>
                            <!-- ★ 공통#51 저축 메인 콘텐츠 (계좌 미보유) / 계좌정보 (계좌 보유) -->
                            <div id="savingsMain"></div>
                            <div id="accountInfo" class="acct" hidden>
                                <small>입출금 · 페퍼스 파킹통장4</small>
                                <div class="amt">1,234,567원</div>
                                <small>계좌 보유 고객 - 저축 메인 콘텐츠 대신 계좌정보</small>
                            </div>
                            <div id="otherTab" class="phone-sec phone-sec-wide" hidden>탭 본문 영역</div>
                        </div>
                        <!-- 공통#51 [계좌 개설하기] 하단 상시 (본문 스크롤과 무관) -->
                        <div class="phone-cta" id="savingsCta" hidden></div>
                        <!-- ★ 공통#50 하단 탭바 + 저축 탭 툴팁 -->
                        <nav class="phone-nav" id="bottomNav"></nav>
                    </div>
                </div>
            </div>
        </div>
        <div class="col-lg-7">
            <div class="card shadow-sm mb-4">
                <div class="card-header fw-bold"><i class="bi bi-person-gear me-1"></i> 고객 (local 테스트)</div>
                <div class="card-body">
                    <select id="customer" class="form-select form-select-sm">
                        <option value="customer01">customer01 - 수신 이어하기 2건 (+ 여신 2건은 저축 탭 제외) · 계좌 보유 → 계좌정보</option>
                        <option value="customer02" selected>customer02 - 수신 이어하기 1건 · 계좌 미보유 → 저축 메인 콘텐츠</option>
                        <option value="customer03">customer03 - 이어하기 없음 · 계좌 미보유</option>
                    </select>
                    <div id="info" class="small mt-2">-</div>
                    <div id="badgeInfo" class="small fw-bold text-danger mt-1">-</div>
                </div>
            </div>
            <div class="card shadow-sm mb-4">
                <div class="card-header fw-bold"><i class="bi bi-list-check me-1"></i> 수신 이어하기 전체 (신청 업무 연동 흉내)</div>
                <div class="card-body p-0">
                    <table class="table table-sm mb-0">
                        <thead class="table-light"><tr><th>ID</th><th>상품명</th><th>단계</th><th>마지막 진행</th><th>처리</th></tr></thead>
                        <tbody id="rows"></tbody>
                    </table>
                </div>
            </div>
            <div class="card shadow-sm">
                <div class="card-header">처리 결과</div>
                <div class="card-body"><pre id="log" class="result-log">-</pre></div>
            </div>
        </div>
    </div>
</div>

<script>
(function () {
    var $ = function (id) { return document.getElementById(id); };
    function log(o) { $('log').textContent = typeof o === 'string' ? o : JSON.stringify(o, null, 2); }
    function el(tag, cls, text) { var e = document.createElement(tag); if (cls) e.className = cls; if (text != null) e.textContent = text; return e; }
    function customer() { return $('customer').value; }
    function q() { return 'demoCustomer=' + encodeURIComponent(customer()); }
    function api(url, opt) {
        opt = opt || {}; opt.credentials = 'same-origin';
        opt.headers = Object.assign({ 'X-Requested-With': 'XMLHttpRequest' }, opt.headers || {});
        return fetch(url, opt).then(function (r) { return r.json(); });
    }

    var TAB = { HOME: { title: '홈', scope: 'ALL' }, SAVINGS: { title: '저축', scope: 'DEPOSIT' }, LOAN: { title: '대출', scope: 'LOAN' } };
    var active = 'SAVINGS';

    // 공통#50 - 업무 화면은 BottomNav.mount('#bottomNav', { active: 'SAVINGS' }) 한 줄 (params·onSelect 는 샘플 전용)
    function mountNav() {
        return BottomNav.mount('#bottomNav', {
            active: active,
            params: { demoCustomer: customer() },
            onSelect: function (t) { active = t.key; mountNav(); loadResume(); },
            onBadge: function (key, n) { $('badgeInfo').textContent = "'저축' 탭 툴팁: " + (n ? n + '건 표시' : '미노출 (진행 중 수신 이어하기 없음)') + ' · 현재 탭 ' + TAB[active].title; }
        });
    }

    // 공통#49 - 업무 화면은 이 한 줄 (params 는 local 샘플 전용)
    function loadResume() {
        $('phoneTitle').textContent = TAB[active].title;
        loadMain();
        return ResumeCard.mount('#resumeCard', {
            scope: TAB[active].scope,
            params: { demoCustomer: customer() },
            onLoad: function (s) { log(s); $('info').textContent = TAB[active].title + ' 탭 이어하기 ' + s.count + '건 (수신 ' + s.counts.DEPOSIT + ' · 여신 ' + s.counts.LOAN + ')'; },
            onError: function (e) { log(e.response || String(e)); }
        });
    }

    // 공통#51 - 업무 화면은 SavingsMain.mount('#savingsMain', { cta: '#savingsCta' }) (params·onNavigate 는 샘플 전용)
    function loadMain() {
        $('accountInfo').hidden = true;
        $('otherTab').hidden = active === 'SAVINGS';
        if (active !== 'SAVINGS') { $('savingsMain').hidden = true; $('savingsCta').hidden = true; return Promise.resolve(); }
        return SavingsMain.mount('#savingsMain', {
            cta: '#savingsCta',
            params: { demoCustomer: customer() },
            onNavigate: function (url) { log('[계좌 개설하기] 이동 URL: ' + url + '\n(샘플에서는 이동하지 않고 표시만 합니다)'); document.querySelectorAll('#savingsCta .ui-btn').forEach(function (b) { if (window.UiButton) UiButton.done(b); }); },
            onIneligible: function () { $('accountInfo').hidden = false; }
        });
    }

    function loadRows() {
        return api('/api/common/resume/list?scope=DEPOSIT&' + q()).then(function (res) {
            var tb = $('rows'); tb.textContent = '';
            if (!res.success) return;
            if (!res.data.length) { var tr0 = el('tr'); var td0 = el('td', 'text-muted', '수신 이어하기 없음'); td0.colSpan = 5; tr0.appendChild(td0); tb.appendChild(tr0); }
            res.data.forEach(function (r, i) {
                var tr = el('tr', i === 0 ? 'table-primary' : '');
                tr.appendChild(el('td', null, String(r.id)));
                tr.appendChild(el('td', null, r.productName));
                tr.appendChild(el('td', null, r.stepName + ' ' + r.stepNo + '/' + r.totalSteps));
                tr.appendChild(el('td', null, r.lastActivityAt));
                var td = el('td');
                var b = el('button', 'btn btn-sm btn-outline-danger py-0 px-1', '가입 취소');
                b.addEventListener('click', function () {
                    // 업무 화면: 취소 처리 후 ResumeCard.changed() → 하단 탭 툴팁 자동 갱신 (기능2 잔여 0건이면 미노출)
                    api('/api/common/resume-demo/' + r.id + '/cancel?' + q(), { method: 'POST' }).then(function (x) { log(x); ResumeCard.changed(); reload(); });
                });
                td.appendChild(b); tr.appendChild(td);
                tb.appendChild(tr);
            });
        });
    }

    function reload() { return loadResume().then(loadRows); }
    $('customer').addEventListener('change', function () { mountNav(); reload(); });
    mountNav();
    reload();
})();
</script>
</body>
</html>
