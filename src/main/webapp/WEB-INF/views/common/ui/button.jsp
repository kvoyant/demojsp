<%--
  =============================================================================
   [CF-UI-01 / 공통#40 샘플] Button 공통 컴포넌트 (press 효과 · 진행 중 상태)  -  GET /common/ui/button
  -----------------------------------------------------------------------------
   업무 화면은 아래 3줄만 포함한다.
     <link rel="stylesheet" href="/css/common/ui-tokens.css">
     <link rel="stylesheet" href="/css/common/ui-button.css">
     <script src="/js/common/ui-button.js"></script>
   주의: JSP 에서는 JS 템플릿 문자열의 달러+중괄호가 EL 로 해석되므로 문자열 더하기를 사용한다.
  =============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>저축은행 포털 - Button 공통 컴포넌트 샘플</title>
    <link rel="shortcut icon" href="/favicon.ico">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css" rel="stylesheet">
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <!-- 공통 UI: Button (이 3줄만 있으면 된다) -->
    <link rel="stylesheet" href="/css/common/ui-tokens.css">
    <link rel="stylesheet" href="/css/common/ui-button.css">
    <script src="/js/common/ui-button.js"></script>
    <style>
        body { background-color: #f4f6f9; font-family: 'Malgun Gothic', 'Noto Sans KR', sans-serif; }
        .card-header-financial { background-color: #0d47a1; color: white; font-weight: bold; }
        pre.result-log { background: #212529; color: #e9ecef; border-radius: 6px; padding: 12px; min-height: 80px; max-height: 260px; overflow: auto; white-space: pre-wrap; font-size: 12px; }
        pre.code { background: #f8f9fb; border: 1px solid #e5e7eb; border-radius: 6px; padding: 10px; font-size: 12px; margin: 0; white-space: pre-wrap; }
        .demo-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 10px; align-items: center; }
        .demo-cap { font-size: 11px; color: #6b7280; margin-top: 4px; text-align: center; }
        .table-sm td, .table-sm th { font-size: 12px; vertical-align: middle; }
        /* APP 목업 */
        .phone { width: 100%; max-width: 360px; height: 640px; margin: 0 auto; border: 10px solid #1f2937; border-radius: 36px; background: #fff; overflow: hidden; position: relative; display: flex; flex-direction: column; }
        .phone-top { background: #fff; padding: 16px 20px 8px; font-weight: 700; border-bottom: 1px solid #f0f1f3; }
        .phone-body { flex: 1; overflow: auto; padding: 18px 20px; font-size: 14px; color: #374151; }
        .phone-body .prod { font-size: 20px; font-weight: 800; color: #111827; margin: 4px 0 12px; }
        .phone-body dl { display: grid; grid-template-columns: 90px 1fr; row-gap: 8px; margin: 16px 0; }
        .phone-body dt { color: #6b7280; font-weight: 400; }
        .phone-body dd { margin: 0; font-weight: 700; color: #111827; }
        .phone-done { display: none; text-align: center; padding-top: 40px; }
        .counter b { font-size: 20px; }
    </style>
</head>
<body>
<jsp:include page="/WEB-INF/views/common/navbar.jsp" />

<div class="container-xl pb-5">
    <div class="d-flex align-items-center mb-3">
        <a href="/common/test-hub" class="btn btn-sm btn-outline-secondary me-2"><i class="bi bi-arrow-left"></i> 테스트 목록</a>
        <h5 class="fw-bold mb-0">[CF-UI-01 / 공통#40] Button 공통 컴포넌트 (press 효과 · 진행 중 상태)</h5>
    </div>
    <% if (Boolean.TRUE.equals(request.getAttribute("navigatedFrom"))) { %>
    <div class="alert alert-success small py-2"><i class="bi bi-check-circle me-1"></i> 페이지 이동 데모에서 도착했습니다. 이동하는 동안 버튼에 "이동 중" 과 화면 상단 진행 막대가 보였는지 확인하세요.</div>
    <% } %>
    <div class="alert alert-info small py-2 mb-4">
        <b>목적</b> 색상 대비 강화 · 누른 효과로 사용자가 감각으로 알아차리게 한다. &nbsp;
        <b>대상</b> 대출 신청하기, 가입하기 등 행동을 유도하는 MAIN 버튼. &nbsp;
        <b>요구</b> 페이지 이동 중 / 업무 실행 중임을 인식시키는 press 디자인 컴포넌트.<br>
        모바일은 실제 손가락 터치처럼 <b>마우스를 누르고 있으면</b> press 상태가 유지됩니다. 키보드 Tab → Space/Enter 로도 확인할 수 있습니다.
    </div>

    <div class="row g-4">
        <!-- 좌: APP 목업 -->
        <div class="col-lg-5">
            <div class="card shadow-sm mb-4">
                <div class="card-header card-header-financial"><i class="bi bi-phone me-1"></i> 1. APP 대출신청 화면 (MAIN 버튼)</div>
                <div class="card-body">
                    <div class="phone">
                        <div class="phone-top">비대면 신용대출</div>
                        <div class="phone-body">
                            <div id="phoneForm">
                                <div class="text-muted small">한도조회 결과</div>
                                <div class="prod">최대 3,000만원</div>
                                <dl>
                                    <dt>금리</dt><dd>연 8.9% ~ 12.4%</dd>
                                    <dt>대출기간</dt><dd>최대 60개월</dd>
                                    <dt>상환방법</dt><dd>원리금균등분할</dd>
                                </dl>
                                <p class="small text-muted mb-0">[대출 신청하기]를 누르면 신청 API 가 2초간 실행됩니다. 진행 중에는 다시 눌러도 중복 신청되지 않습니다.</p>
                            </div>
                            <div id="phoneDone" class="phone-done">
                                <div style="font-size:44px;color:#1b7a3d;"><i class="bi bi-check-circle-fill"></i></div>
                                <div class="fw-bold fs-5 mt-2">신청이 접수되었습니다</div>
                                <div class="small text-muted mt-1">심사 결과는 알림으로 안내드립니다.</div>
                                <button type="button" id="btnAgain" class="ui-btn ui-btn--text ui-btn--sm mt-3">다시 해보기</button>
                            </div>
                        </div>
                        <div class="ui-cta-bar">
                            <div class="ui-btn-group">
                                <a class="ui-btn ui-btn--outline ui-btn--lg" href="/common/ui/button?from=nav-demo&amp;delayMs=1500" data-ui-nav data-busy-text="이동 중">이전</a>
                                <button type="button" id="btnApply" class="ui-btn ui-btn--primary ui-btn--lg ui-btn--main" data-busy-text="신청 중">대출 신청하기</button>
                            </div>
                        </div>
                    </div>
                    <div class="small text-muted mt-2 text-center">[이전] = 페이지 이동(data-ui-nav, 1.5초 지연 응답) / [대출 신청하기] = 업무 실행(UiButton.run)</div>
                </div>
            </div>
            <div class="card shadow-sm">
                <div class="card-header">처리 결과</div>
                <div class="card-body"><pre id="log" class="result-log mb-0">버튼을 눌러 테스트하세요.</pre></div>
            </div>
        </div>

        <!-- 우: 컴포넌트 카탈로그 -->
        <div class="col-lg-7">
            <div class="card shadow-sm mb-4">
                <div class="card-header fw-bold"><i class="bi bi-grid me-1"></i> 2. 변형(variant) · 크기(size)</div>
                <div class="card-body">
                    <div class="demo-grid mb-3">
                        <div><button type="button" class="ui-btn ui-btn--primary ui-btn--block">MAIN</button><div class="demo-cap">ui-btn--primary</div></div>
                        <div><button type="button" class="ui-btn ui-btn--secondary ui-btn--block">보조</button><div class="demo-cap">ui-btn--secondary</div></div>
                        <div><button type="button" class="ui-btn ui-btn--outline ui-btn--block">취소</button><div class="demo-cap">ui-btn--outline</div></div>
                        <div><button type="button" class="ui-btn ui-btn--text ui-btn--block">자세히 보기</button><div class="demo-cap">ui-btn--text</div></div>
                        <div><button type="button" class="ui-btn ui-btn--danger ui-btn--block">해지하기</button><div class="demo-cap">ui-btn--danger</div></div>
                    </div>
                    <div class="d-flex flex-wrap gap-2 align-items-center">
                        <button type="button" class="ui-btn ui-btn--lg">lg 56px</button>
                        <button type="button" class="ui-btn">기본 48px</button>
                        <button type="button" class="ui-btn ui-btn--sm">sm 36px</button>
                        <button type="button" class="ui-btn ui-btn--secondary ui-btn--sm"><i class="bi bi-download" aria-hidden="true"></i> 아이콘+문구</button>
                    </div>
                </div>
            </div>

            <div class="card shadow-sm mb-4">
                <div class="card-header fw-bold"><i class="bi bi-hand-index me-1"></i> 3. 상태 비교 (고정 표시)</div>
                <div class="card-body">
                    <div class="demo-grid">
                        <div><button type="button" class="ui-btn ui-btn--block" tabindex="-1">대출 신청하기</button><div class="demo-cap">기본</div></div>
                        <div><button type="button" class="ui-btn ui-btn--block is-pressed" tabindex="-1">대출 신청하기</button><div class="demo-cap">누름 (.is-pressed)</div></div>
                        <div><button type="button" id="staticBusy" class="ui-btn ui-btn--block" tabindex="-1">대출 신청하기</button><div class="demo-cap">진행 중 (.is-busy)</div></div>
                        <div><button type="button" class="ui-btn ui-btn--block" disabled>대출 신청하기</button><div class="demo-cap">비활성 (:disabled)</div></div>
                    </div>
                </div>
            </div>

            <div class="card shadow-sm mb-4">
                <div class="card-header fw-bold d-flex justify-content-between align-items-center">
                    <span><i class="bi bi-circle-half me-1"></i> 4. 색상 대비 검증 (화면에 실제 적용된 색으로 계산)</span>
                    <button type="button" id="btnContrast" class="btn btn-sm btn-outline-primary">다시 계산</button>
                </div>
                <div class="card-body p-0">
                    <table class="table table-sm mb-0">
                        <thead class="table-light"><tr><th>변형</th><th>글자색</th><th>배경색</th><th>대비</th><th>AA(4.5:1)</th></tr></thead>
                        <tbody id="contrastRows"></tbody>
                    </table>
                </div>
                <div class="card-footer small text-muted">KWCAG 2.2 / WCAG 2.1 AA: 일반 글자 4.5:1 이상. 비활성 버튼은 기준 예외이지만 읽을 수 있도록 4.5:1 이상으로 맞췄습니다.</div>
            </div>

            <div class="card shadow-sm mb-4">
                <div class="card-header fw-bold"><i class="bi bi-shield-check me-1"></i> 5. 중복 실행 차단</div>
                <div class="card-body">
                    <div class="d-flex flex-wrap gap-3 align-items-center mb-3">
                        <button type="button" id="btnTransfer" class="ui-btn" data-busy-text="이체 중">이체 실행 (1.5초)</button>
                        <button type="button" id="btnBurst" class="btn btn-sm btn-outline-secondary">빠르게 5번 클릭 시뮬레이션</button>
                        <div class="counter small">클릭 <b id="cntClick">0</b> 회 / 실제 실행 <b id="cntRun" class="text-primary">0</b> 회</div>
                    </div>
                    <form id="demoForm" data-ui-submit action="/common/ui/button" method="get" class="d-flex flex-wrap gap-2 align-items-center">
                        <input type="hidden" name="from" value="nav-demo">
                        <input type="hidden" name="delayMs" value="1500">
                        <input type="text" class="form-control form-control-sm" style="max-width:200px" placeholder="Enter 를 연타해 보세요" aria-label="이름">
                        <button type="submit" class="ui-btn ui-btn--sm" data-busy-text="가입 중">가입하기 (폼 제출)</button>
                        <span class="small text-muted">form[data-ui-submit] → 제출 중 재제출 차단</span>
                    </form>
                </div>
            </div>

            <div class="card shadow-sm">
                <div class="card-header fw-bold"><i class="bi bi-code-slash me-1"></i> 6. 사용 예</div>
                <div class="card-body">
<pre class="code">&lt;!-- MAIN 버튼 (하단 고정 CTA) --&gt;
&lt;div class="ui-cta-bar"&gt;
  &lt;button type="button" id="btnApply" class="ui-btn ui-btn--lg ui-btn--block" data-busy-text="신청 중"&gt;대출 신청하기&lt;/button&gt;
&lt;/div&gt;

// 업무 실행: Promise 가 끝날 때까지 "신청 중" + 재클릭 차단
UiButton.run('#btnApply', function () { return fetch('/api/loan/apply', { method: 'POST' }); })
    .then(function (res) { ... });

&lt;!-- 페이지 이동: "이동 중" + 상단 진행 막대 --&gt;
&lt;a class="ui-btn" href="/loan/limit" data-ui-nav&gt;한도 조회하기&lt;/a&gt;</pre>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
(function () {
    var logEl = document.getElementById('log');
    function log(msg) {
        var t = new Date().toTimeString().substring(0, 8);
        logEl.textContent = '[' + t + '] ' + msg + '\n' + (logEl.textContent === '버튼을 눌러 테스트하세요.' ? '' : logEl.textContent);
    }
    function wait(ms) { return new Promise(function (r) { setTimeout(r, ms); }); }

    // 1. 업무 실행 (대출 신청)
    var form = document.getElementById('phoneForm'), doneBox = document.getElementById('phoneDone');
    document.getElementById('btnApply').addEventListener('click', function () {
        log('대출 신청하기 클릭 → UiButton.run 시작 (busy)');
        UiButton.run(this, function () { return wait(2000); }).then(function () {
            form.style.display = 'none';
            doneBox.style.display = 'block';
            log('신청 API 완료 → 버튼 원복 (done)');
        });
    });
    document.getElementById('btnAgain').addEventListener('click', function () {
        form.style.display = '';
        doneBox.style.display = 'none';
    });

    // 3. 고정 상태 (진행 중) - timeout 0 = 자동해제 안 함
    UiButton.busy('#staticBusy', { timeout: 0, announce: ' ' });

    // 4. 색상 대비 - 실제 계산된 색으로 검증
    function rgb(str) {
        var m = str.match(/rgba?\(([^)]+)\)/);
        if (!m) return null;
        var p = m[1].split(',').map(function (v) { return parseFloat(v); });
        return { r: p[0], g: p[1], b: p[2], a: p.length > 3 ? p[3] : 1 };
    }
    function lum(c) {
        function ch(v) { v /= 255; return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4); }
        return 0.2126 * ch(c.r) + 0.7152 * ch(c.g) + 0.0722 * ch(c.b);
    }
    function hex(c) { return '#' + [c.r, c.g, c.b].map(function (v) { return ('0' + Math.round(v).toString(16)).slice(-2); }).join(''); }
    function contrastRows() {
        var variants = [['primary', ''], ['primary · 누름', 'is-pressed'], ['secondary', 'ui-btn--secondary'], ['outline', 'ui-btn--outline'],
                        ['text', 'ui-btn--text'], ['danger', 'ui-btn--danger'], ['disabled', 'disabled']];
        var tbody = document.getElementById('contrastRows');
        tbody.textContent = '';
        var probe = document.createElement('div');
        probe.style.cssText = 'position:absolute;left:-9999px;background:#fff;';
        document.body.appendChild(probe);
        variants.forEach(function (v) {
            var b = document.createElement('button');
            b.className = 'ui-btn';
            if (v[1] === 'disabled') b.disabled = true; else if (v[1]) b.classList.add(v[1]);
            b.textContent = 'A';
            b.style.transition = 'none';
            probe.appendChild(b);
            var cs = getComputedStyle(b);
            var fg = rgb(cs.color), bg = rgb(cs.backgroundColor);
            if (!bg || bg.a === 0) bg = { r: 255, g: 255, b: 255, a: 1 };   // 투명 배경 = 흰 화면
            var l1 = lum(fg), l2 = lum(bg);
            var ratio = (Math.max(l1, l2) + 0.05) / (Math.min(l1, l2) + 0.05);
            var tr = document.createElement('tr');
            [v[0], hex(fg), hex(bg), ratio.toFixed(2) + ' : 1', ratio >= 4.5 ? '✔ 통과' : (v[1] === 'disabled' ? '예외(비활성)' : '✘ 미달')].forEach(function (txt, i) {
                var td = document.createElement('td');
                td.textContent = txt;
                if (i === 4) td.className = txt.indexOf('통과') >= 0 ? 'text-success fw-bold' : (txt === '예외' ? 'text-muted' : 'text-danger fw-bold');
                tr.appendChild(td);
            });
            tbody.appendChild(tr);
        });
        document.body.removeChild(probe);
    }
    contrastRows();
    document.getElementById('btnContrast').addEventListener('click', contrastRows);

    // 5. 중복 실행 차단
    var cntClick = 0, cntRun = 0;
    var btnTransfer = document.getElementById('btnTransfer');
    // 클릭 수 집계는 capture 단계보다 먼저 받기 위해 window capture 로 등록
    window.addEventListener('click', function (e) {
        if (e.target.closest && e.target.closest('#btnTransfer')) document.getElementById('cntClick').textContent = ++cntClick;
    }, true);
    btnTransfer.addEventListener('click', function () {
        UiButton.run(btnTransfer, function () {
            document.getElementById('cntRun').textContent = ++cntRun;
            log('이체 실행 #' + cntRun + ' (진행 중 재클릭은 업무 핸들러까지 오지 않음)');
            return wait(1500);
        });
    });
    document.getElementById('btnBurst').addEventListener('click', function () {
        for (var i = 0; i < 5; i++) setTimeout(function () { btnTransfer.click(); }, i * 120);
    });
    document.getElementById('demoForm').addEventListener('submit', function () { log('폼 제출 (이후 Enter/클릭은 차단)'); });
})();
</script>
</body>
</html>
