<%--
  =============================================================================
   [CF-UI-01 / 공통#42 샘플] Alert 공통 컴포넌트 (1Btn/2Btn · 가독성 · 반응형)  -  GET /common/ui/alert
  -----------------------------------------------------------------------------
   업무 화면은 아래 1줄만 포함한다 (CSS 는 자동 로드, 이미 포함돼 있으면 그대로 사용).
     <script src="/js/common/ui-alert.js"></script>
     UiAlert.alert('메시지');  /  UiAlert.confirm('메시지').then(function (ok) { ... });
   embed=true : "화면 크기별 미리보기" iframe 용 단독 화면 (preset 알럿 자동 표시)
   주의: JSP 에서는 JS 템플릿 문자열의 달러+중괄호가 EL 로 해석되므로 문자열 더하기를 사용한다.
  =============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
    <title>저축은행 포털 - Alert 공통 컴포넌트 샘플</title>
    <link rel="shortcut icon" href="/favicon.ico">
<c:if test="${not embed}">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css" rel="stylesheet">
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</c:if>
    <!-- 공통 UI: Alert (CSS 를 직접 포함해도 되고, 없으면 ui-alert.js 가 자동으로 불러온다) -->
    <link rel="stylesheet" href="/css/common/ui-tokens.css">
    <link rel="stylesheet" href="/css/common/ui-button.css">
    <link rel="stylesheet" href="/css/common/ui-alert.css">
    <script src="/js/common/ui-button.js"></script>
    <script src="/js/common/ui-alert.js"></script>
    <style>
        body { background-color: #f4f6f9; font-family: 'Malgun Gothic', 'Noto Sans KR', sans-serif; }
        .card-header-financial { background-color: #0d47a1; color: white; font-weight: bold; }
        pre.result-log { background: #212529; color: #e9ecef; border-radius: 6px; padding: 12px; min-height: 80px; max-height: 240px; overflow: auto; white-space: pre-wrap; font-size: 12px; }
        pre.code { background: #f8f9fb; border: 1px solid #e5e7eb; border-radius: 6px; padding: 10px; font-size: 12px; margin: 0; white-space: pre-wrap; }
        .preset-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(190px, 1fr)); gap: 8px; }
        .preset-grid .ui-btn { justify-content: flex-start; text-align: left; white-space: normal; word-break: keep-all; height: auto; padding-top: 6px; padding-bottom: 6px; }
        .preset-grid small { display: block; font-weight: 400; font-size: 11px; opacity: .8; }
        .sim-wrap { overflow: hidden; background: #e5e7eb; border-radius: 8px; padding: 12px; }
        .sim-box { position: relative; margin: 0 auto; }
        .sim-frame { position: absolute; left: 0; top: 0; border: 8px solid #1f2937; border-radius: 22px; background: #fff; transform-origin: 0 0; }
        .sim-meta { font-size: 12px; color: #6b7280; margin-top: 6px; }
        /* embed (iframe) 화면 */
        body.embed { background: #f5f7fb; margin: 0; }
        .embed-page { padding: 20px; font-size: 14px; color: #374151; }
        .embed-page .bar { height: 44px; background: #0d47a1; border-radius: 8px; margin-bottom: 12px; }
        .embed-page .blk { height: 64px; background: #fff; border-radius: 10px; margin-bottom: 10px; }
    </style>
</head>
<c:choose>
<c:when test="${embed}">
<body class="embed">
<div class="embed-page" aria-hidden="false">
    <div class="bar"></div><div class="blk"></div><div class="blk"></div><div class="blk"></div><div class="blk"></div><div class="blk"></div>
    <button type="button" id="btnReopen" class="ui-btn ui-btn--sm ui-btn--secondary">알럿 다시 열기</button>
</div>
</c:when>
<c:otherwise>
<body>
<jsp:include page="/WEB-INF/views/common/navbar.jsp" />

<div class="container-xl pb-5">
    <div class="d-flex align-items-center mb-3">
        <a href="/common/test-hub" class="btn btn-sm btn-outline-secondary me-2"><i class="bi bi-arrow-left"></i> 테스트 목록</a>
        <h5 class="fw-bold mb-0">[CF-UI-01 / 공통#42] Alert 공통 컴포넌트 (1Btn/2Btn · 가독성 · 반응형)</h5>
    </div>
    <div class="alert alert-info small py-2 mb-4">
        <b>목적</b> 사용자에게 주의를 주거나 알림을 주기 위해 사용. &nbsp; <b>기능</b> 1Btn, 2Btn 구성.<br>
        <b>요구 1</b> 알럿 내용이 4~5줄 이상일 경우를 고려하여 가독성 높이는 폰트 개선 &nbsp;→ 16px · 줄간격 1.65 · 진한 글자 · 3줄 이상 왼쪽 정렬 · 문단/목록 · 본문만 스크롤<br>
        <b>요구 2</b> 다양한 화면 크기·해상도에서 일관된 사용성 &nbsp;→ 폭 단계(모바일/태블릿/PC) · dvh · 가로모드 · 버튼 세로 자동 전환 · 글자 확대 대응
    </div>

    <div class="row g-4">
        <div class="col-lg-6">
            <div class="card shadow-sm mb-4">
                <div class="card-header card-header-financial"><i class="bi bi-chat-square-text me-1"></i> 1. 알럿 띄우기 (현재 화면)</div>
                <div class="card-body">
                    <div class="preset-grid" id="presetButtons"></div>
                    <hr>
                    <div class="d-flex flex-wrap gap-2">
                        <button type="button" id="btnQueue" class="ui-btn ui-btn--sm ui-btn--outline">연속 3건 호출 (큐)</button>
                        <button type="button" id="btnBusy" class="ui-btn ui-btn--sm ui-btn--outline">확인 시 업무 실행 (onOk)</button>
                        <button type="button" id="btnBusyFail" class="ui-btn ui-btn--sm ui-btn--outline">업무 실패 → 알럿 유지</button>
                        <button type="button" id="btnCommon" class="ui-btn ui-btn--sm ui-btn--outline">Common.alert (common.js)</button>
                        <button type="button" id="btnNative" class="ui-btn ui-btn--sm ui-btn--text">AS-IS 브라우저 alert 비교</button>
                    </div>
                </div>
            </div>
            <div class="card shadow-sm mb-4">
                <div class="card-header">처리 결과 (Promise 반환값)</div>
                <div class="card-body"><pre id="log" class="result-log mb-0">버튼을 눌러 테스트하세요.</pre></div>
            </div>
            <div class="card shadow-sm">
                <div class="card-header fw-bold"><i class="bi bi-code-slash me-1"></i> 사용 예</div>
                <div class="card-body">
<pre class="code">&lt;script src="/js/common/ui-alert.js"&gt;&lt;/script&gt;

UiAlert.alert('비밀번호가 변경되었습니다.');
UiAlert.alert('본인인증에 실패했습니다.\n잠시 후 다시 시도해 주세요.', { type: 'error', title: '인증 실패' });

UiAlert.confirm('대출 신청을 취소하시겠습니까?').then(function (ok) {
    if (ok) { ... }
});
UiAlert.confirm('해지 후에는 되돌릴 수 없습니다.', { title: '예금 해지', okText: '해지하기', danger: true });

// 확인 버튼에서 업무 실행: 끝날 때까지 알럿 유지 + "처리 중", 실패하면 알럿 유지
UiAlert.confirm('신청서를 제출할까요?', { onOk: function () { return fetch('/api/loan/apply', { method: 'POST' }); } });

// 메시지 규칙: "\n" 줄바꿈 / "\n\n" 문단 / "- " 목록 / "※" 보조 안내</pre>
                </div>
            </div>
        </div>

        <div class="col-lg-6">
            <div class="card shadow-sm">
                <div class="card-header fw-bold d-flex flex-wrap gap-2 justify-content-between align-items-center">
                    <span><i class="bi bi-phone-landscape me-1"></i> 2. 화면 크기별 미리보기 (요구 2)</span>
                    <div class="d-flex gap-2">
                        <select id="simDevice" class="form-select form-select-sm" style="width:auto" aria-label="기기">
                            <option value="280x653">폴드 접힘 280×653</option>
                            <option value="360x740" selected>Android 360×740</option>
                            <option value="390x844">iPhone 390×844</option>
                            <option value="844x390">iPhone 가로 844×390</option>
                            <option value="768x1024">태블릿 768×1024</option>
                            <option value="1280x800">PC WEB / ADMIN 1280×800</option>
                        </select>
                        <select id="simPreset" class="form-select form-select-sm" style="width:auto" aria-label="알럿 종류"></select>
                    </div>
                </div>
                <div class="card-body">
                    <div class="sim-wrap" id="simWrap"><div class="sim-box" id="simBox"><iframe id="sim" class="sim-frame" title="알럿 미리보기"></iframe></div></div>
                    <div class="sim-meta" id="simMeta"></div>
                    <div class="small text-muted mt-2">실제 기기 크기의 화면(iframe)에서 알럿을 띄웁니다. 카드 폭보다 큰 화면은 축소해 보여 줍니다.
                        브라우저 글자 크기를 키우거나(Ctrl +) 200% 확대해도 제목·버튼이 잘리지 않고 본문만 스크롤됩니다.</div>
                </div>
            </div>
        </div>
    </div>
</div>
</c:otherwise>
</c:choose>

<script>
(function () {
    var EMBED = '${embed}' === 'true';
    var PRESET = '${preset}';

    // 알럿 프리셋 (컨트롤러 ALERT_PRESETS 와 동일한 키)
    var PRESETS = {
        short:   { label: '1Btn · 짧은 문구', kind: 'alert', msg: '비밀번호가 변경되었습니다.' },
        long:    { label: '1Btn · 5줄 이상 (요구 1)', kind: 'alert', opt: { title: '대출 신청 전 확인해 주세요', type: 'info' },
                   msg: '대출 신청 시 아래 내용을 꼭 확인해 주세요.\n\n- 신용조회 시 신용점수에 영향이 없습니다.\n- 대출 실행 후 14일 이내 청약철회가 가능합니다.\n- 연체 시 연체이자율(약정금리+3%p)이 적용됩니다.\n- 중도상환수수료는 최대 1.5%입니다.\n\n※ 자세한 내용은 상품설명서를 확인해 주세요.' },
        scroll:  { label: '1Btn · 화면보다 긴 내용 (본문 스크롤)', kind: 'alert', opt: { title: '시스템 점검 안내', type: 'warning' },
                   msg: '보다 안정적인 서비스 제공을 위해 아래 일정으로 시스템 점검을 진행합니다.\n\n점검 일시: 2026년 10월 25일(일) 00:00 ~ 06:00\n\n점검 시간 동안 다음 서비스 이용이 제한됩니다.\n- 대출 신청 및 한도조회\n- 계좌 이체 및 자동이체 등록\n- 예·적금 신규 가입 및 해지\n- 증명서 발급\n- 금리인하요구 신청\n- 서류 제출 및 전자약정\n\n점검 시간 중 만기가 도래하는 예금은 점검 종료 후 자동 처리됩니다. 대출 이자 납입일이 점검 시간과 겹치는 경우 연체로 처리되지 않습니다.\n\n이용에 불편을 드려 죄송합니다. 더 나은 서비스로 보답하겠습니다.\n\n※ 점검 일정은 사정에 따라 변경될 수 있습니다.\n※ 문의: 고객센터 1588-0000 (평일 09:00~18:00)' },
        confirm: { label: '2Btn · 확인창', kind: 'confirm', msg: '작성 중인 대출 신청서가 있습니다.\n이어서 작성하시겠습니까?', opt: { okText: '이어하기', cancelText: '새로 작성' } },
        danger:  { label: '2Btn · 위험 작업 (포커스=취소)', kind: 'confirm', opt: { title: '예금 해지', type: 'warning', okText: '해지하기', danger: true },
                   msg: '정기예금을 중도 해지하면 약정 금리 대신 중도해지 금리(연 0.5%)가 적용됩니다.\n해지 후에는 되돌릴 수 없습니다.' },
        stack:   { label: '2Btn · 긴 버튼 문구 (세로 전환)', kind: 'confirm', msg: '휴대폰 본인인증을 진행할까요?',
                   opt: { okText: '휴대폰 본인인증 진행하기', cancelText: '다른 인증수단 선택하기' } },
        error:   { label: '1Btn · 오류 (제목+아이콘)', kind: 'alert', opt: { title: '본인인증 실패', type: 'error' },
                   msg: '입력하신 정보가 통신사 정보와 일치하지 않습니다.\n이름, 생년월일, 휴대폰번호를 다시 확인해 주세요.' }
    };

    function openPreset(key) {
        var p = PRESETS[key] || PRESETS.long;
        return p.kind === 'confirm' ? UiAlert.confirm(p.msg, p.opt) : UiAlert.alert(p.msg, p.opt);
    }

    if (EMBED) {
        var go = function () { openPreset(PRESET); };
        document.getElementById('btnReopen').addEventListener('click', go);
        go();
        return;
    }

    var logEl = document.getElementById('log');
    function log(msg) {
        var t = new Date().toTimeString().substring(0, 8);
        logEl.textContent = '[' + t + '] ' + msg + '\n' + (logEl.textContent === '버튼을 눌러 테스트하세요.' ? '' : logEl.textContent);
    }

    // 1. 프리셋 버튼
    var box = document.getElementById('presetButtons'), sel = document.getElementById('simPreset');
    Object.keys(PRESETS).forEach(function (key) {
        var p = PRESETS[key];
        var b = document.createElement('button');
        b.type = 'button';
        b.className = 'ui-btn ui-btn--sm ' + (p.kind === 'confirm' ? 'ui-btn--secondary' : 'ui-btn--outline');
        var span = document.createElement('span');
        span.textContent = p.label;
        var small = document.createElement('small');
        small.textContent = p.kind === 'confirm' ? 'UiAlert.confirm' : 'UiAlert.alert';
        span.appendChild(small);
        b.appendChild(span);
        b.addEventListener('click', function () {
            openPreset(key).then(function (v) {
                log(p.label + ' → 반환값: ' + (v === undefined ? 'undefined (확인)' : v));
            });
            setTimeout(function () {
                var s = UiAlert.state();
                if (s) log(p.label + ' 열림: 본문 ' + s.lines + '줄 / 왼쪽정렬=' + s.long + ' / 버튼세로=' + s.stack + ' / 본문스크롤=' + s.scrollable);
            }, 80);
        });
        box.appendChild(b);
        var o = document.createElement('option');
        o.value = key; o.textContent = p.label;
        if (key === 'long') o.selected = true;
        sel.appendChild(o);
    });

    document.getElementById('btnQueue').addEventListener('click', function () {
        UiAlert.alert('첫 번째 알림입니다.').then(function () { log('큐 1 닫힘'); });
        UiAlert.alert('두 번째 알림입니다.\n앞 알림을 닫으면 순서대로 표시됩니다.').then(function () { log('큐 2 닫힘'); });
        UiAlert.confirm('세 번째(확인창)입니다. 계속할까요?').then(function (ok) { log('큐 3 닫힘 → ' + ok); });
    });
    document.getElementById('btnBusy').addEventListener('click', function () {
        UiAlert.confirm('신청서를 제출할까요?\n제출 후에는 수정할 수 없습니다.', {
            title: '신청서 제출', okText: '제출하기', busyText: '제출 중',
            onOk: function () { return new Promise(function (r) { setTimeout(r, 1800); }); }
        }).then(function (ok) { log('제출 확인창 → ' + ok + (ok ? ' (업무 완료 후 닫힘)' : '')); });
    });
    document.getElementById('btnBusyFail').addEventListener('click', function () {
        UiAlert.confirm('서류를 제출할까요?', {
            okText: '제출하기', busyText: '제출 중',
            onOk: function () { return new Promise(function (r, j) { setTimeout(function () { j(new Error('통신 오류')); }, 1200); }); },
            onError: function (e) { log('업무 실패(' + e.message + ') → 알럿 유지, 다시 누르거나 취소 가능'); }
        }).then(function (ok) { log('서류 제출 확인창 → ' + ok); });
    });
    document.getElementById('btnCommon').addEventListener('click', function () {
        // common.js 의 Common.alert 는 ui-alert.js 가 있으면 공통 Alert 로 표시 (이 화면은 common.js 미포함 → 동일 API 직접 호출)
        UiAlert.alert('처리 중 오류가 발생했습니다.', { type: 'error' }).then(function () { log('Common.alert 흐름 → 닫힘'); });
    });
    document.getElementById('btnNative').addEventListener('click', function () {
        window.alert('대출 신청 시 아래 내용을 꼭 확인해 주세요.\n\n- 신용조회 시 신용점수에 영향이 없습니다.\n- 대출 실행 후 14일 이내 청약철회가 가능합니다.\n- 연체 시 연체이자율(약정금리+3%p)이 적용됩니다.\n- 중도상환수수료는 최대 1.5%입니다.\n\n※ 자세한 내용은 상품설명서를 확인해 주세요.');
    });

    // 2. 화면 크기별 미리보기
    var frame = document.getElementById('sim'), wrap = document.getElementById('simWrap'), simBox = document.getElementById('simBox');
    function sim() {
        var wh = document.getElementById('simDevice').value.split('x');
        var w = +wh[0], h = +wh[1];
        frame.style.width = (w + 16) + 'px';
        frame.style.height = (h + 16) + 'px';
        var avail = wrap.clientWidth - 24;
        var scale = Math.min(1, avail / (w + 16), 640 / (h + 16));
        frame.style.transform = 'scale(' + scale + ')';
        simBox.style.width = Math.floor((w + 16) * scale) + 'px';
        simBox.style.height = Math.ceil((h + 16) * scale) + 'px';
        frame.src = '/common/ui/alert?embed=true&preset=' + encodeURIComponent(sel.value);
        document.getElementById('simMeta').textContent = w + ' × ' + h + ' CSS px' + (scale < 1 ? ' (' + Math.round(scale * 100) + '% 축소 표시)' : '');
    }
    document.getElementById('simDevice').addEventListener('change', sim);
    sel.addEventListener('change', sim);
    window.addEventListener('resize', function () { clearTimeout(sim.t); sim.t = setTimeout(sim, 200); });
    sim();
})();
</script>
</body>
</html>
