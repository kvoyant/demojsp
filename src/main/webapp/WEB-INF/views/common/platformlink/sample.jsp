<%--
  =============================================================================
   [CF-TC-07 · CF-TC-02 · CF-BC-10 / 공통#1 REQ-AP-074] 플랫폼 대출 연계 - 제휴사 시뮬레이터  -  GET /common/platform-link/sample
  -----------------------------------------------------------------------------
   이 화면이 "제휴사 서버" 역할을 한다: 제휴사 형식 전문을 만들고 서명해서 실제 수신 서비스(PlatformLinkService)를 호출한다.
   운영 수신 창구는 POST /api/link/platform/{제휴사}/limit (제휴사 서버 → 당행). 이 화면의 API 는 local 전용(demo-api-open).
   주의: JSP 에서는 JS 템플릿 문자열의 달러+중괄호가 EL 로 해석되므로 문자열 더하기를 사용한다.
  =============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>플랫폼 대출 연계 - 제휴사 시뮬레이터</title>
    <link rel="shortcut icon" href="/favicon.ico">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css" rel="stylesheet">
    <style>
        body { background-color: #f4f6f9; font-family: 'Malgun Gothic', 'Noto Sans KR', sans-serif; }
        [hidden] { display: none !important; }
        pre.code { background: #1f2430; color: #e6e9f0; border-radius: 6px; padding: 10px; max-height: 340px; overflow: auto; white-space: pre-wrap; font-size: 12px; margin: 0; }
        textarea.code { font-family: ui-monospace, Menlo, Consolas, monospace; font-size: 12px; min-height: 300px; }
        .flow { display: grid; grid-template-columns: repeat(auto-fit, minmax(118px, 1fr)); gap: 6px; }
        .flow .st { border: 1px solid #cfd6e2; background: #fff; border-radius: 8px; padding: 6px 8px; font-size: 12px; line-height: 1.35; min-height: 62px; }
        .flow .st b { display: block; font-size: 12.5px; }
        .flow .st small { color: #6b7280; }
        .flow .st.ok { border-color: #1b7a3d; background: #eef8f1; }
        .flow .st.ng { border-color: #c62828; background: #fdecea; }
        .flow .st .ms { float: right; font-family: ui-monospace, Menlo, monospace; color: #374151; }
        .partner { cursor: pointer; min-width: 0; overflow-wrap: anywhere; }
        .partner.active { border-color: #0d47a1 !important; box-shadow: 0 0 0 2px #e3ecfa; }
        .eng { font-size: 11px; padding: 1px 6px; border-radius: 8px; }
        .eng-LEGACY { background: #f3f4f6; color: #374151; } .eng-SHADOW { background: #fff4e5; color: #9a5b00; } .eng-NEW { background: #e3ecfa; color: #0d47a1; }
        .bar { height: 10px; background: #e5e7eb; border-radius: 6px; overflow: hidden; }
        .bar > i { display: block; height: 100%; background: #1b7a3d; }
        .bar.over > i { background: #c62828; }
        .trace li { font-size: 12.5px; }
        .trace .ms { font-family: ui-monospace, Menlo, monospace; color: #6b7280; }
    </style>
</head>
<body>
<jsp:include page="/WEB-INF/views/common/navbar.jsp" />

<div class="container-xl pb-5">
    <div class="d-flex align-items-center mb-3 flex-wrap gap-2">
        <a href="/common/test-hub" class="btn btn-sm btn-outline-secondary"><i class="bi bi-arrow-left"></i> 테스트 목록</a>
        <h5 class="fw-bold mb-0">[공통#1] 플랫폼 대출 연계 - 제휴사 시뮬레이터</h5>
        <a href="/common/platform-link/admin" class="btn btn-sm btn-outline-primary ms-auto"><i class="bi bi-gear"></i> 디지털어드민 연계 운영</a>
    </div>
    <div class="alert alert-info small py-2">
        이 화면이 <b>제휴사 서버</b> 역할을 합니다. 제휴사 형식 전문을 만들어 서명한 뒤 당행 수신 창구와 <b>같은 서비스</b>를 호출합니다.
        <b>제휴사 전문은 바꾸지 않고</b>, 안쪽 한도조회 엔진만 <span class="eng eng-LEGACY">LEGACY</span> 현행 →
        <span class="eng eng-SHADOW">SHADOW</span> 병행비교 → <span class="eng eng-NEW">NEW</span> 신규 CSS 로 제휴사별로 전환합니다.
        CSS 는 시뮬레이터입니다 (고객명에 '지연' = 타임아웃, '오류' = CSS 장애).
    </div>

    <!-- 처리 단계 -->
    <div class="card shadow-sm mb-3"><div class="card-body py-2">
        <div class="small text-muted mb-1">수신 처리 단계 (보내면 단계별 결과와 누적 ms 가 표시됩니다)</div>
        <div class="flow" id="flow"></div>
    </div></div>

    <div class="row g-3">
        <div class="col-lg-5">
            <div class="card shadow-sm mb-3">
                <div class="card-header fw-bold"><i class="bi bi-1-circle me-1"></i> 제휴사 선택</div>
                <div class="card-body"><div id="partners" class="d-grid gap-2"><span class="small text-muted">불러오는 중...</span></div></div>
            </div>
            <div class="card shadow-sm">
                <div class="card-header fw-bold d-flex flex-wrap align-items-center gap-2"><i class="bi bi-2-circle"></i> 요청 전문
                    <select id="scenario" class="form-select form-select-sm ms-auto" style="max-width:220px"></select>
                    <button class="btn btn-sm btn-outline-secondary" id="btnSample">새로 만들기</button>
                </div>
                <div class="card-body">
                    <textarea id="body" class="form-control code" spellcheck="false"></textarea>
                    <div class="d-flex flex-wrap gap-3 align-items-center small mt-2">
                        <span class="text-muted">서명</span>
                        <label><input type="radio" name="sign" value="OK" checked> 정상</label>
                        <label><input type="radio" name="sign" value="BAD"> 잘못된 키</label>
                        <label><input type="radio" name="sign" value="OLD"> 오래된 시각</label>
                    </div>
                    <div class="d-flex gap-2 mt-2">
                        <button class="btn btn-primary btn-sm" id="btnSend"><i class="bi bi-send"></i> 보내기</button>
                        <button class="btn btn-outline-primary btn-sm" id="btnResend" disabled>같은 요청 다시 보내기 (중복)</button>
                    </div>
                    <p class="small text-muted mt-2 mb-0">전문을 직접 고쳐 보세요: 필수값을 지우면 전문 오류 코드, 토스에 담보주소를 넣으면 미취급 상품 코드가 나옵니다.</p>
                </div>
            </div>
        </div>
        <div class="col-lg-7">
            <div class="card shadow-sm mb-3">
                <div class="card-header fw-bold"><i class="bi bi-3-circle me-1"></i> 제휴사가 받은 응답</div>
                <div class="card-body">
                    <div id="resultHead" class="small text-muted mb-2">요청을 보내면 결과가 표시됩니다.</div>
                    <div class="bar mb-2" id="timeBar" hidden><i></i></div>
                    <pre class="code" id="resBody">-</pre>
                    <div class="small text-muted mt-2" id="reqHead"></div>
                </div>
            </div>
            <div class="card shadow-sm mb-3">
                <div class="card-header fw-bold">당행 내부 처리 (제휴사에는 보이지 않음)</div>
                <div class="card-body">
                    <ol class="trace ps-3 mb-3" id="trace"><li class="text-muted">-</li></ol>
                    <div class="row g-2">
                        <div class="col-md-6"><div class="small fw-bold mb-1">공통모델 (어댑터 변환 결과)</div><pre class="code" id="model">-</pre></div>
                        <div class="col-md-6"><div class="small fw-bold mb-1">CSS 요청 (공통모델)</div><pre class="code" id="css">-</pre></div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
(function () {
    var API = '/api/common/platform-link-demo';
    var $ = function (id) { return document.getElementById(id); };
    var STEPS = [['제휴사 확인', 'URL 의 제휴사코드 → 설정(DB)'], ['① 인증', 'IP · 서명 · 시각'], ['② 전문 변환', '제휴사 → 공통모델'],
                 ['③ 중복 확인', '같은 요청번호면 재전송'], ['④ 서비스 상태', 'USE_YN 중단 스위치'], ['⑤ 상품 결정', '차량→오토 / 담보→모기지'],
                 ['⑥ CSS 한도조회', 'LEGACY · SHADOW · NEW'], ['⑦ 결과 변환', '내부코드 → 제휴사 코드'], ['⑧ 이력 저장', 'TB_LNK_LIMIT_REQ']];
    var state = { partner: null, lastBody: null };

    function api(method, url, body) {
        return fetch(API + url, { method: method, headers: body ? { 'Content-Type': 'application/json' } : {}, body: body ? JSON.stringify(body) : undefined })
            .then(function (r) { return r.json(); });
    }
    function el(tag, cls, text) { var e = document.createElement(tag); if (cls) e.className = cls; if (text != null) e.textContent = text; return e; }
    function pretty(s) { try { return JSON.stringify(JSON.parse(s), null, 2); } catch (e) { return s; } }

    function drawFlow(steps) {
        var box = $('flow'); box.textContent = '';
        var byName = {};
        (steps || []).forEach(function (s) { byName[s.name] = s; });
        STEPS.forEach(function (d) {
            var s = byName[d[0]];
            var c = el('div', 'st' + (s ? (s.ok ? ' ok' : ' ng') : ''));
            var b = el('b', null, d[0]);
            if (s) b.appendChild(el('span', 'ms', s.atMs + 'ms'));
            c.appendChild(b);
            c.appendChild(el('small', null, d[1]));
            box.appendChild(c);
        });
    }

    function selectPartner(p, card) {
        state.partner = p;
        Array.prototype.forEach.call(document.querySelectorAll('.partner'), function (x) { x.classList.remove('active'); });
        card.classList.add('active');
        makeSample();
    }

    function makeSample() {
        if (!state.partner) return;
        api('POST', '/sample', { partnerCd: state.partner.partnerCd, scenario: $('scenario').value }).then(function (res) {
            $('body').value = res.success ? res.data : res.message;
            $('btnResend').disabled = true;
        });
    }

    function send(body) {
        var mode = document.querySelector('input[name=sign]:checked').value;
        $('btnSend').disabled = true;
        $('resultHead').textContent = '보내는 중... (제휴사 제한시간 ' + state.partner.timeoutMs + 'ms)';
        api('POST', '/send', { partnerCd: state.partner.partnerCd, body: body, signMode: mode }).then(function (res) {
            $('btnSend').disabled = false;
            if (!res.success) { $('resultHead').textContent = res.message; return; }
            var d = res.data;
            state.lastBody = body;
            $('btnResend').disabled = false;
            var over = d.elapsedMs > d.timeoutMs;
            $('resultHead').className = 'small mb-2 ' + (d.httpStatus === 200 ? 'text-success' : 'text-danger');
            $('resultHead').textContent = 'HTTP ' + d.httpStatus + ' · ' + d.elapsedMs + 'ms / 제한 ' + d.timeoutMs + 'ms'
                + (d.lnkReqNo ? ' · 연계번호 ' + d.lnkReqNo : ' · 이력 없음');
            $('timeBar').hidden = false;
            $('timeBar').className = 'bar mb-2' + (over ? ' over' : '');
            $('timeBar').firstChild.style.width = Math.min(100, Math.round(d.elapsedMs * 100 / d.timeoutMs)) + '%';
            $('resBody').textContent = pretty(d.responseBody);
            $('reqHead').textContent = d.url + ' · ' + 'X-Link-Timestamp: ' + d.headers['X-Link-Timestamp'] + ' · X-Link-Signature: ' + d.headers['X-Link-Signature'];
            var t = d.trace || {};
            var ol = $('trace'); ol.textContent = '';
            (t.steps || []).forEach(function (s) {
                var li = el('li', s.ok ? '' : 'text-danger');
                li.appendChild(el('b', null, s.name + ' '));
                li.appendChild(el('span', 'ms', '+' + s.atMs + 'ms '));
                li.appendChild(document.createTextNode(s.detail));
                ol.appendChild(li);
            });
            $('model').textContent = t.commonModel ? JSON.stringify(t.commonModel, null, 2) : '-';
            $('css').textContent = t.cssRequest ? JSON.stringify(t.cssRequest, null, 2) : '-';
            drawFlow(t.steps);
        });
    }

    api('GET', '/setup').then(function (res) {
        var box = $('partners'); box.textContent = '';
        if (!res.success) { box.textContent = res.message; return; }
        res.data.scenarios.forEach(function (s) { $('scenario').add(new Option(s.label, s.code)); });
        res.data.partners.forEach(function (p, i) {
            var c = el('div', 'partner border rounded p-2 bg-white');
            var top = el('div', 'd-flex flex-wrap align-items-center gap-2');
            top.appendChild(el('b', null, p.partnerNm + ' (' + p.partnerCd + ')'));
            top.appendChild(el('span', 'eng eng-' + p.cssEngine, p.cssEngine));
            if (p.useYn !== 'Y') top.appendChild(el('span', 'badge bg-secondary', '중단'));
            top.appendChild(el('span', 'small text-muted ms-auto', p.products + ' · ' + p.timeoutMs + 'ms'));
            c.appendChild(top);
            c.appendChild(el('div', 'small text-muted', p.adapterCd + ' · ' + p.adapterDesc));
            c.addEventListener('click', function () { selectPartner(p, c); });
            box.appendChild(c);
            if (i === 0) selectPartner(p, c);
        });
    });
    $('scenario').addEventListener('change', makeSample);
    $('btnSample').addEventListener('click', makeSample);
    $('btnSend').addEventListener('click', function () { send($('body').value); });
    $('btnResend').addEventListener('click', function () { send(state.lastBody); });
    drawFlow([]);
})();
</script>
</body>
</html>
