<%--
  =============================================================================
   [CF-UI-01 / 공통#41 샘플] Icon 공통 컴포넌트 (금융 서비스 · 상태 아이콘)  -  GET /common/ui/icon
  -----------------------------------------------------------------------------
   업무 화면은 아래만 포함한다.
     <link rel="stylesheet" href="/css/common/ui-tokens.css">
     <link rel="stylesheet" href="/css/common/ui-icon.css">
     <script src="/js/common/ui-icon.js"></script>       (data-ui-icon / UiIcon.create 를 쓸 때)
   주의: JSP 에서는 JS 템플릿 문자열의 달러+중괄호가 EL 로 해석되므로 문자열 더하기를 사용한다.
  =============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>저축은행 포털 - Icon 공통 컴포넌트 샘플</title>
    <link rel="shortcut icon" href="/favicon.ico">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css" rel="stylesheet">
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <!-- 공통 UI: Icon -->
    <link rel="stylesheet" href="/css/common/ui-tokens.css">
    <link rel="stylesheet" href="/css/common/ui-icon.css">
    <script src="/js/common/ui-icon.js"></script>
    <style>
        body { background-color: #f4f6f9; font-family: 'Malgun Gothic', 'Noto Sans KR', sans-serif; }
        .card-header-financial { background-color: #0d47a1; color: white; font-weight: bold; }
        pre.code { background: #f8f9fb; border: 1px solid #e5e7eb; border-radius: 6px; padding: 10px; font-size: 12px; margin: 0; white-space: pre-wrap; word-break: break-all; }
        [hidden] { display: none !important; }
        /* APP 목업 */
        .phone { width: 100%; max-width: 360px; height: 640px; margin: 0 auto; border: 10px solid #1f2937; border-radius: 36px; background: #f5f7fb; overflow: auto; position: relative; }
        .phone-top { background: #fff; padding: 16px 20px 10px; font-weight: 700; }
        .phone-sec { background: #fff; border-radius: 14px; margin: 12px; padding: 14px; font-size: 13px; }
        .phone-sec h6 { font-size: 14px; font-weight: 700; margin: 0 0 12px; }
        .menu-grid { display: grid; grid-template-columns: repeat(4, 1fr); row-gap: 14px; justify-items: center; }
        .menu-text { display: grid; grid-template-columns: repeat(4, 1fr); row-gap: 10px; text-align: center; font-size: 12px; color: #374151; }
        .menu-text span { padding: 10px 0; border: 1px solid #e5e7eb; border-radius: 8px; margin: 0 3px; }
        .apply-row { display: flex; align-items: center; justify-content: space-between; gap: 8px; padding: 10px 0; border-top: 1px solid #f0f1f3; }
        .apply-row:first-of-type { border-top: 0; }
        .apply-row .nm { font-weight: 600; color: #111827; }
        .apply-row .dt { font-size: 11px; color: #6b7280; }
        .plain-st { font-size: 12px; color: #6b7280; }
        /* 카탈로그 */
        .cat-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(104px, 1fr)); gap: 8px; }
        .cat-item { border: 1px solid #e5e7eb; border-radius: 10px; background: #fff; padding: 12px 6px 8px; text-align: center; cursor: pointer; font-size: 11px; color: #374151; line-height: 1.3; }
        .cat-item:hover { border-color: #0d47a1; }
        .cat-item.sel { border-color: #0d47a1; box-shadow: 0 0 0 2px #c9daf5; }
        .cat-item code { display: block; font-size: 10px; color: #6b7280; margin-top: 2px; word-break: break-all; }
        .cat-item .ui-icon { margin-bottom: 6px; }
        .result-box { text-align: center; padding: 16px 8px; border: 1px solid #e5e7eb; border-radius: 12px; background: #fff; }
        .result-box b { display: block; font-size: 15px; margin-bottom: 2px; }
        .result-box span { font-size: 12px; color: #6b7280; }
    </style>
</head>
<body>
<jsp:include page="/WEB-INF/views/common/navbar.jsp" />

<div class="container-xl pb-5">
    <div class="d-flex align-items-center mb-3">
        <a href="/common/test-hub" class="btn btn-sm btn-outline-secondary me-2"><i class="bi bi-arrow-left"></i> 테스트 목록</a>
        <h5 class="fw-bold mb-0">[CF-UI-01 / 공통#41] Icon 공통 컴포넌트 (금융 서비스 · 상태 아이콘)</h5>
    </div>
    <div class="alert alert-info small py-2 mb-4">
        <b>목적</b> 단조로운 화면 구성 개선, 화면별 일관성·사용성 향상. &nbsp;
        <b>기능</b> 사용자의 이해를 돕는 보조 이미지 도구. &nbsp;
        <b>요구</b> 1. 금융 서비스 전용 아이콘 추가 (<span id="cntFinance">-</span>종) &nbsp; 2. 상태(성공/실패/주의) 아이콘 추가 (<span id="cntStatus">-</span>종).<br>
        모든 아이콘은 SVG 스프라이트 1개 파일(<code>/images/common/ui-icons.svg</code>)로 관리하며, 글자색을 따라가므로(currentColor) 화면별로 이미지를 따로 만들지 않습니다.
    </div>

    <div class="row g-4">
        <!-- 좌: APP 적용 예 -->
        <div class="col-lg-5">
            <div class="card shadow-sm mb-4">
                <div class="card-header card-header-financial d-flex justify-content-between align-items-center">
                    <span><i class="bi bi-phone me-1"></i> 1. APP 적용 예</span>
                    <div class="btn-group btn-group-sm" role="group" aria-label="아이콘 적용 전후">
                        <button type="button" class="btn btn-light" id="btnBefore">적용 전</button>
                        <button type="button" class="btn btn-warning fw-bold" id="btnAfter">적용 후</button>
                    </div>
                </div>
                <div class="card-body">
                    <div class="phone">
                        <div class="phone-top">전체 메뉴</div>
                        <div class="phone-sec">
                            <h6>대출</h6>
                            <div class="menu-grid view-after">
                                <a href="#" class="ui-icon-tile"><span class="ui-icon-tile__bg"><i data-ui-icon="limit-inquiry"></i></span>한도조회</a>
                                <a href="#" class="ui-icon-tile"><span class="ui-icon-tile__bg"><i data-ui-icon="loan"></i></span>신용대출</a>
                                <a href="#" class="ui-icon-tile"><span class="ui-icon-tile__bg"><i data-ui-icon="auto-loan"></i></span>오토대출</a>
                                <a href="#" class="ui-icon-tile"><span class="ui-icon-tile__bg"><i data-ui-icon="mortgage"></i></span>주택담보</a>
                                <a href="#" class="ui-icon-tile"><span class="ui-icon-tile__bg"><i data-ui-icon="refinance"></i></span>대환대출</a>
                                <a href="#" class="ui-icon-tile"><span class="ui-icon-tile__bg"><i data-ui-icon="repayment"></i></span>대출상환</a>
                                <a href="#" class="ui-icon-tile"><span class="ui-icon-tile__bg"><i data-ui-icon="interest-rate"></i></span>금리인하요구</a>
                                <a href="#" class="ui-icon-tile"><span class="ui-icon-tile__bg"><i data-ui-icon="document"></i></span>서류제출</a>
                            </div>
                            <div class="menu-text view-before" hidden>
                                <span>한도조회</span><span>신용대출</span><span>오토대출</span><span>주택담보</span>
                                <span>대환대출</span><span>대출상환</span><span>금리인하</span><span>서류제출</span>
                            </div>
                        </div>
                        <div class="phone-sec">
                            <h6>예금 · 이체</h6>
                            <div class="menu-grid view-after">
                                <a href="#" class="ui-icon-tile"><span class="ui-icon-tile__bg"><i data-ui-icon="deposit"></i></span>예금</a>
                                <a href="#" class="ui-icon-tile"><span class="ui-icon-tile__bg"><i data-ui-icon="savings"></i></span>적금</a>
                                <a href="#" class="ui-icon-tile"><span class="ui-icon-tile__bg"><i data-ui-icon="transfer"></i></span>이체</a>
                                <a href="#" class="ui-icon-tile"><span class="ui-icon-tile__bg"><i data-ui-icon="account"></i></span>계좌조회</a>
                            </div>
                            <div class="menu-text view-before" hidden><span>예금</span><span>적금</span><span>이체</span><span>계좌조회</span></div>
                        </div>
                        <div class="phone-sec">
                            <h6>신청현황</h6>
                            <div class="view-after">
                                <div class="apply-row"><div><div class="nm">비대면 신용대출</div><div class="dt">2026.10.15 신청</div></div><span class="js-st" data-type="success" data-text="승인 완료"></span></div>
                                <div class="apply-row"><div><div class="nm">오토담보대출</div><div class="dt">2026.10.16 신청</div></div><span class="js-st" data-type="warning" data-text="서류 보완 필요"></span></div>
                                <div class="apply-row"><div><div class="nm">주택담보대출</div><div class="dt">2026.10.17 신청</div></div><span class="js-st" data-type="pending" data-text="심사 중"></span></div>
                                <div class="apply-row"><div><div class="nm">햇살론</div><div class="dt">2026.10.02 신청</div></div><span class="js-st" data-type="error" data-text="심사 거절"></span></div>
                            </div>
                            <div class="view-before" hidden>
                                <div class="apply-row"><div><div class="nm">비대면 신용대출</div><div class="dt">2026.10.15 신청</div></div><span class="plain-st">승인 완료</span></div>
                                <div class="apply-row"><div><div class="nm">오토담보대출</div><div class="dt">2026.10.16 신청</div></div><span class="plain-st">서류 보완 필요</span></div>
                                <div class="apply-row"><div><div class="nm">주택담보대출</div><div class="dt">2026.10.17 신청</div></div><span class="plain-st">심사 중</span></div>
                                <div class="apply-row"><div><div class="nm">햇살론</div><div class="dt">2026.10.02 신청</div></div><span class="plain-st">심사 거절</span></div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div class="card shadow-sm">
                <div class="card-header fw-bold"><i class="bi bi-check2-circle me-1"></i> 2. 결과 화면 · 상태 (채움형)</div>
                <div class="card-body">
                    <div class="row g-2">
                        <div class="col-6"><div class="result-box"><i data-ui-icon="status-success-fill" data-size="xl" data-tone="success" data-label="성공"></i><b class="mt-2">신청 완료</b><span>대출 신청이 접수되었습니다</span></div></div>
                        <div class="col-6"><div class="result-box"><i data-ui-icon="status-error-fill" data-size="xl" data-tone="error" data-label="실패"></i><b class="mt-2">처리 실패</b><span>잠시 후 다시 시도해 주세요</span></div></div>
                        <div class="col-6"><div class="result-box"><i data-ui-icon="status-warning-fill" data-size="xl" data-tone="warning" data-label="주의"></i><b class="mt-2">확인 필요</b><span>서류 보완이 필요합니다</span></div></div>
                        <div class="col-6"><div class="result-box"><i data-ui-icon="status-info-fill" data-size="xl" data-tone="info" data-label="안내"></i><b class="mt-2">안내</b><span>점검 시간에는 이용이 제한됩니다</span></div></div>
                    </div>
                    <div class="small text-muted mt-2">상태는 <b>색상 + 모양(✓ ✕ ! i) + 문구</b>로 함께 표현합니다. 색약 사용자도 모양으로 구분할 수 있습니다.</div>
                </div>
            </div>
        </div>

        <!-- 우: 카탈로그 -->
        <div class="col-lg-7">
            <div class="card shadow-sm mb-4">
                <div class="card-header fw-bold d-flex flex-wrap gap-2 justify-content-between align-items-center">
                    <span><i class="bi bi-grid-3x3-gap me-1"></i> 3. 아이콘 카탈로그 <small class="text-muted fw-normal">(눌러서 사용 코드 확인)</small></span>
                    <div class="d-flex gap-2">
                        <input id="q" type="search" class="form-control form-control-sm" style="width:150px" placeholder="이름 검색 (예: 대출)" aria-label="아이콘 검색">
                        <select id="size" class="form-select form-select-sm" style="width:auto" aria-label="크기">
                            <option value="sm">20px</option><option value="md">24px</option><option value="lg" selected>32px</option><option value="xl">48px</option>
                        </select>
                        <select id="tone" class="form-select form-select-sm" style="width:auto" aria-label="색상">
                            <option value="">글자색</option><option value="primary" selected>primary</option><option value="success">success</option>
                            <option value="error">error</option><option value="warning">warning</option><option value="muted">muted</option><option value="auto">상태 기본색</option>
                        </select>
                    </div>
                </div>
                <div class="card-body">
                    <h6 class="fw-bold small text-primary">금융 서비스 (요구사항 1)</h6>
                    <div id="gridFinance" class="cat-grid mb-4"></div>
                    <h6 class="fw-bold small text-primary">상태 · 성공/실패/주의/안내/진행중 (요구사항 2)</h6>
                    <div id="gridStatus" class="cat-grid"></div>
                </div>
            </div>

            <div class="card shadow-sm mb-4">
                <div class="card-header fw-bold"><i class="bi bi-code-slash me-1"></i> 4. 사용 코드 <span id="selName" class="text-primary"></span></div>
                <div class="card-body">
                    <div class="small fw-bold mb-1">JSP 직접 작성 (JS 불필요)</div>
                    <pre id="codeSvg" class="code mb-3">카탈로그에서 아이콘을 누르세요.</pre>
                    <div class="small fw-bold mb-1">선언형 (ui-icon.js)</div>
                    <pre id="codeData" class="code mb-3">-</pre>
                    <div class="small fw-bold mb-1">상태 문구 (아이콘 + 글자)</div>
<pre class="code">&lt;span class="ui-status ui-status--warning"&gt;&lt;i data-ui-icon="status-warning"&gt;&lt;/i&gt;서류 보완 필요&lt;/span&gt;
// JS: el.appendChild(UiIcon.status('warning', '서류 보완 필요'));</pre>
                </div>
            </div>

            <div class="card shadow-sm">
                <div class="card-header fw-bold"><i class="bi bi-universal-access me-1"></i> 5. 접근성 · 운영 규칙</div>
                <div class="card-body small">
                    <ul class="mb-0">
                        <li>글자 옆 보조 아이콘은 <b>장식용</b>(<code>aria-hidden</code>) → 스크린리더가 같은 말을 두 번 읽지 않음</li>
                        <li>아이콘만 있는 버튼/상태는 <code>data-label="성공"</code> 처럼 <b>대체 문구 필수</b> (<code>role="img"</code> + <code>aria-label</code>)</li>
                        <li>상태 아이콘 글자색 대비 4.5:1 이상 (주의 = 주황 #b45309, 노랑 글자는 사용 금지)</li>
                        <li>새 아이콘은 스프라이트 + <code>UiIcon.CATALOG</code> 에 함께 추가 (단위 테스트가 불일치를 잡아냄). 화면별 PNG 아이콘 신규 제작 금지</li>
                        <li>APP WebView 에서 외부 스프라이트 참조가 막히면 <code>UiIcon.inline()</code> 1회 호출</li>
                    </ul>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
(function () {
    // 1. 적용 전/후 토글
    function show(after) {
        var a = document.querySelectorAll('.view-after'), b = document.querySelectorAll('.view-before');
        for (var i = 0; i < a.length; i++) a[i].hidden = !after;
        for (var j = 0; j < b.length; j++) b[j].hidden = after;
        document.getElementById('btnAfter').className = 'btn ' + (after ? 'btn-warning fw-bold' : 'btn-light');
        document.getElementById('btnBefore').className = 'btn ' + (after ? 'btn-light' : 'btn-warning fw-bold');
    }
    document.getElementById('btnBefore').addEventListener('click', function () { show(false); });
    document.getElementById('btnAfter').addEventListener('click', function () { show(true); });
    var links = document.querySelectorAll('.ui-icon-tile');
    for (var k = 0; k < links.length; k++) links[k].addEventListener('click', function (e) { e.preventDefault(); });

    // 신청현황 상태 문구
    var st = document.querySelectorAll('.js-st');
    for (var s = 0; s < st.length; s++) st[s].parentNode.replaceChild(UiIcon.status(st[s].getAttribute('data-type'), st[s].getAttribute('data-text')), st[s]);

    // 3. 카탈로그
    document.getElementById('cntFinance').textContent = UiIcon.list('FINANCE').length;
    document.getElementById('cntStatus').textContent = UiIcon.list('STATUS').length;
    var selected = null;
    function draw() {
        var q = document.getElementById('q').value.trim().toLowerCase();
        var size = document.getElementById('size').value, tone = document.getElementById('tone').value;
        [['FINANCE', 'gridFinance'], ['STATUS', 'gridStatus']].forEach(function (g) {
            var box = document.getElementById(g[1]);
            box.textContent = '';
            UiIcon.list(g[0]).forEach(function (c) {
                if (q && c.name.indexOf(q) < 0 && c.ko.toLowerCase().indexOf(q) < 0) return;
                var t = tone === 'auto' ? (c.tone || 'primary') : tone;
                var item = document.createElement('button');
                item.type = 'button';
                item.className = 'cat-item' + (selected === c.name ? ' sel' : '');
                item.appendChild(UiIcon.create(c.name, { size: size, tone: t }));
                var ko = document.createElement('div'); ko.textContent = c.ko; item.appendChild(ko);
                var code = document.createElement('code'); code.textContent = c.name; item.appendChild(code);
                item.addEventListener('click', function () { pick(c); });
                box.appendChild(item);
            });
        });
    }
    function pick(c) {
        selected = c.name;
        var size = document.getElementById('size').value, tone = document.getElementById('tone').value;
        var t = tone === 'auto' ? (c.tone || 'primary') : tone;
        var cls = 'ui-icon' + (size !== 'md' ? ' ui-icon--' + size : '') + (t ? ' ui-icon--' + t : '');
        var a11y = c.group === 'STATUS' ? 'role="img" aria-label="' + c.ko + '"' : 'aria-hidden="true"';
        document.getElementById('selName').textContent = '- ' + c.ko + ' (' + c.name + ')';
        document.getElementById('codeSvg').textContent =
            '<svg class="' + cls + '" ' + a11y + ' focusable="false"><use href="/images/common/ui-icons.svg#ic-' + c.name + '"></use></svg>';
        document.getElementById('codeData').textContent =
            '<i data-ui-icon="' + c.name + '"' + (size !== 'md' ? ' data-size="' + size + '"' : '') + (t ? ' data-tone="' + t + '"' : '')
            + (c.group === 'STATUS' ? ' data-label="' + c.ko + '"' : '') + '></i>';
        draw();
    }
    ['q', 'size', 'tone'].forEach(function (id) { document.getElementById(id).addEventListener('input', draw); });
    draw();
})();
</script>
</body>
</html>
