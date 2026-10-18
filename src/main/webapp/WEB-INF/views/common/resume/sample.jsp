<%--
  =============================================================================
   [CF-BC-13 / 공통#43 샘플] 홈 - 이어하기  -  GET /common/resume/sample
  -----------------------------------------------------------------------------
   좌: APP 홈 목업 - 화면 상단 이어하기 카드 (업무 화면은 resume-card.js 포함 + ResumeCard.mount)
   우: 테스트 도구 - 고객 전환, scope 전환(홈/저축/대출 재사용), 신청 업무 연동 흉내(단계 저장·완료·취소)
       테스트 도구는 local(common.resume.demo-api-open=true) 에서만 동작한다.
   주의: JSP 에서는 JS 템플릿 문자열의 달러+중괄호가 EL 로 해석되므로 문자열 더하기를 사용한다.
  =============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>저축은행 포털 - 이어하기 공통모듈 샘플</title>
    <link rel="shortcut icon" href="/favicon.ico">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css" rel="stylesheet">
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <!-- 공통 UI + 이어하기 카드 -->
    <link rel="stylesheet" href="/css/common/ui-tokens.css">
    <link rel="stylesheet" href="/css/common/ui-button.css">
    <link rel="stylesheet" href="/css/common/ui-icon.css">
    <link rel="stylesheet" href="/css/common/ui-alert.css">
    <link rel="stylesheet" href="/css/common/resume-card.css">
    <script src="/js/common/ui-button.js"></script>
    <script src="/js/common/ui-alert.js"></script>
    <script src="/js/common/resume-card.js"></script>
    <style>
        body { background-color: #f4f6f9; font-family: 'Malgun Gothic', 'Noto Sans KR', sans-serif; }
        .card-header-financial { background-color: #0d47a1; color: white; font-weight: bold; }
        pre.result-log { background: #212529; color: #e9ecef; border-radius: 6px; padding: 12px; min-height: 80px; max-height: 320px; overflow: auto; white-space: pre-wrap; font-size: 12px; margin: 0; }
        .table-sm td, .table-sm th { font-size: 12px; vertical-align: middle; }
        [hidden] { display: none !important; }
        /* APP 홈 목업 */
        .phone { width: 100%; max-width: 360px; height: 680px; margin: 0 auto; border: 10px solid #1f2937; border-radius: 36px; background: #f5f7fb; overflow: auto; position: relative; }
        .phone-top { background: #fff; padding: 16px 18px 10px; display: flex; justify-content: space-between; align-items: center; }
        .phone-top b { font-size: 17px; }
        .phone-tabs { display: flex; gap: 16px; padding: 0 18px; background: #fff; border-bottom: 1px solid #eef0f3; font-size: 14px; }
        .phone-tabs span { padding: 8px 0; color: #6b7280; }
        .phone-tabs span.on { color: #111827; font-weight: 700; border-bottom: 2px solid #111827; }
        .phone-slot { padding: 0 12px; }
        #resumeCard { margin-top: 12px; }      /* hidden 이면 여백도 사라짐 */
        .phone-sec { background: #fff; border-radius: 14px; margin: 12px; padding: 14px; font-size: 13px; color: #6b7280; }
        .phone-sec h6 { color: #111827; font-size: 14px; font-weight: 700; }
        .menu-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; text-align: center; font-size: 12px; color: #374151; }
        .menu-grid i { display: block; font-size: 22px; color: #0d47a1; }
        .empty-note { font-size: 12px; color: #6b7280; text-align: center; padding: 8px 0 0; }
        .scope-badge { font-size: 11px; }
    </style>
</head>
<body>
<jsp:include page="/WEB-INF/views/common/navbar.jsp" />

<div class="container-xl pb-5">
    <div class="d-flex align-items-center mb-3">
        <a href="/common/test-hub" class="btn btn-sm btn-outline-secondary me-2"><i class="bi bi-arrow-left"></i> 테스트 목록</a>
        <h5 class="fw-bold mb-0">[CF-BC-13 / 공통#43] 홈 - 이어하기</h5>
    </div>
    <div class="alert alert-info small py-2">
        <b>AS-IS</b> 이어하기가 홈 화면을 덮는 팝업으로 떠서 뒷 화면을 가리고 진행 중 건수를 알 수 없음 →
        <b>TO-BE</b> 화면 <b>상단 카드</b>로 <b>최신 1건</b> + <b>건수 숫자</b> + [더보기](신청현황조회) + [이어하기](해당 상품 신청 단계).
        카드는 <b>로고 + 회사명 + 상품명 + 이어하기</b> 구조이며 회사명·로고는 회사 마스터(DB)에서 조회합니다.
    </div>

    <div class="row g-4">
        <!-- 좌: APP 홈 -->
        <div class="col-lg-5">
            <div class="card shadow-sm">
                <div class="card-header card-header-financial"><i class="bi bi-phone me-1"></i> 1. APP 홈 (고객 화면)</div>
                <div class="card-body">
                    <div id="phone" class="phone">
                        <div class="phone-top"><b>페퍼저축은행</b><i class="bi bi-bell"></i></div>
                        <div class="phone-tabs" id="phoneTabs">
                            <span data-scope="ALL" class="on">홈</span><span data-scope="DEPOSIT">저축</span><span data-scope="LOAN">대출</span>
                        </div>
                        <!-- ★ 화면 상단 이어하기 카드 (공통#43 기능6) - 0건이면 자리를 차지하지 않는다 -->
                        <div class="phone-slot" id="resumeSlot">
                            <div id="resumeCard"></div>
                        </div>
                        <div id="emptyNote" class="empty-note" hidden>이어하기 대상이 아니면 카드가 나타나지 않습니다.</div>
                        <div class="phone-sec">
                            <h6>퀵메뉴</h6>
                            <div class="menu-grid">
                                <span><i class="bi bi-grid"></i>전체상품</span><span><i class="bi bi-wallet2"></i>계좌개설</span><span><i class="bi bi-cash-coin"></i>대출신청</span>
                                <span><i class="bi bi-bank"></i>오픈뱅킹</span><span><i class="bi bi-pen"></i>전자서명</span><span><i class="bi bi-file-earmark-arrow-up"></i>서류제출</span>
                            </div>
                        </div>
                        <div class="phone-sec"><h6>추천 상품</h6>추천 상품 영역 (공통#47)</div>
                        <div class="phone-sec"><h6>공지사항</h6>공지 영역</div>
                    </div>
                    <p class="small text-muted mt-2 mb-0">목업의 탭(홈/저축/대출)을 누르면 같은 카드 컴포넌트가 scope 만 바꿔 다시 조회합니다 (공통#49·#60 재사용 준비).</p>
                </div>
            </div>
        </div>

        <!-- 우: 테스트 도구 -->
        <div class="col-lg-7">
            <div class="card shadow-sm mb-4">
                <div class="card-header fw-bold"><i class="bi bi-person-gear me-1"></i> 2. 고객 · 조회 범위</div>
                <div class="card-body">
                    <div class="row g-2 align-items-end">
                        <div class="col-md-6">
                            <label class="form-label small fw-bold" for="customer">고객 (local 테스트 - 로그인 시 로그인 고객 우선)</label>
                            <select id="customer" class="form-select form-select-sm">
                                <option value="customer01">customer01 - 진행중 3건 (여신2·수신1) + 완료·만료 각 1</option>
                                <option value="customer02">customer02 - 수신 1건</option>
                                <option value="customer03">customer03 - 이어하기 없음</option>
                            </select>
                        </div>
                        <div class="col-md-3">
                            <label class="form-label small fw-bold" for="scope">scope</label>
                            <select id="scope" class="form-select form-select-sm">
                                <option value="ALL">ALL (홈)</option>
                                <option value="DEPOSIT">DEPOSIT (저축)</option>
                                <option value="LOAN">LOAN (대출)</option>
                            </select>
                        </div>
                        <div class="col-md-3 d-grid">
                            <button id="btnReload" class="btn btn-sm btn-primary"><i class="bi bi-arrow-clockwise me-1"></i>홈 다시 조회</button>
                        </div>
                    </div>
                    <div class="mt-2 small" id="countsLine">범위별 건수: -</div>
                </div>
            </div>

            <div class="card shadow-sm mb-4">
                <div class="card-header fw-bold"><i class="bi bi-diagram-3 me-1"></i> 3. 신청 업무 연동 흉내 (ResumeService.saveStep / complete / cancel)</div>
                <div class="card-body">
                    <div class="row g-2">
                        <div class="col-md-4">
                            <label class="form-label small fw-bold" for="dProduct">상품</label>
                            <select id="dProduct" class="form-select form-select-sm">
                                <option value="LOAN|LOAN_APPLY|대출신청|PS_SMART|페퍼스마트대출|PARTNER_A|5">페퍼스마트대출 (여신·제휴플랫폼A)</option>
                                <option value="LOAN|LOAN_APPLY|대출신청|PS_BIZ_MORTGAGE|페퍼스 사업자담보대출|OWN|6">페퍼스 사업자담보대출 (여신·당행)</option>
                                <option value="DEPOSIT|DEPOSIT_OPEN|계좌개설|PS_PARKING4|페퍼스 파킹통장4|OWN|4">페퍼스 파킹통장4 (수신·당행)</option>
                                <option value="DEPOSIT|DEPOSIT_OPEN|계좌개설|PS_2030|페퍼스 2030적금|OWN|4">페퍼스 2030적금 (수신·당행)</option>
                                <option value="LOAN|LOAN_APPLY|대출신청|PB_CREDIT|제휴 신용대출|PARTNER_B|5">제휴 신용대출 (여신·제휴플랫폼B·로고 미등록)</option>
                            </select>
                        </div>
                        <div class="col-md-3">
                            <label class="form-label small fw-bold" for="dStep">멈춘 단계</label>
                            <select id="dStep" class="form-select form-select-sm">
                                <option value="1|약관동의">1 약관동의</option><option value="2|본인인증">2 본인인증</option>
                                <option value="3|서류제출" selected>3 서류제출</option><option value="4|전자서명">4 전자서명</option>
                            </select>
                        </div>
                        <div class="col-md-2">
                            <label class="form-label small fw-bold" for="dExpire">만료(일)</label>
                            <input id="dExpire" type="number" min="0" max="365" value="7" class="form-control form-control-sm">
                        </div>
                        <div class="col-md-3 d-grid">
                            <button id="btnStep" class="btn btn-sm btn-warning fw-bold">단계 저장</button>
                        </div>
                    </div>
                    <p class="small text-muted mt-2 mb-0">같은 업무·상품이 진행 중이면 새로 만들지 않고 단계만 갱신 → 건수가 늘지 않고 최신 1건이 바뀝니다.</p>
                </div>
            </div>

            <div class="card shadow-sm mb-4">
                <div class="card-header d-flex justify-content-between align-items-center">
                    <span class="fw-bold"><i class="bi bi-list-check me-1"></i> 4. 진행 중 전체 (신청현황조회 데이터 · 최신순)</span>
                    <a id="statusLink" class="btn btn-sm btn-outline-primary" href="/common/resume/status">신청현황조회 화면</a>
                </div>
                <div class="card-body p-0">
                    <div class="table-responsive">
                        <table class="table table-sm table-hover mb-0">
                            <thead class="table-light"><tr><th>ID</th><th>구분</th><th>회사</th><th>상품명</th><th>단계</th><th>마지막 진행</th><th>만료</th><th>처리</th></tr></thead>
                            <tbody id="rows"><tr><td colspan="8" class="text-muted">-</td></tr></tbody>
                        </table>
                    </div>
                </div>
                <div class="card-footer small text-muted">1행 = 홈 카드에 노출되는 최신 1건. 완료/취소/만료 건은 목록·건수에서 빠집니다.</div>
            </div>

            <div class="card shadow-sm">
                <div class="card-header">처리 결과 (API 응답)</div>
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
    function scope() { return $('scope').value; }
    function q() { return 'demoCustomer=' + encodeURIComponent(customer()); }
    function api(url, opt) {
        opt = opt || {};
        opt.credentials = 'same-origin';
        opt.headers = Object.assign({ 'X-Requested-With': 'XMLHttpRequest' }, opt.headers || {});
        return fetch(url, opt).then(function (r) { return r.json(); });
    }

    // 1) 업무 화면은 이 호출 하나면 된다 (params 는 local 샘플 전용)
    function mountCard() {
        document.querySelectorAll('#phoneTabs span').forEach(function (t) { t.classList.toggle('on', t.getAttribute('data-scope') === scope()); });
        return ResumeCard.mount('#resumeCard', {
            scope: scope(),
            params: { demoCustomer: customer() },
            onLoad: function (s) {
                log(s);
                $('emptyNote').hidden = s.count > 0;
                $('countsLine').textContent = '범위별 건수: 홈(ALL) ' + s.counts.ALL + '건 · 저축(DEPOSIT) ' + s.counts.DEPOSIT + '건 · 대출(LOAN) ' + s.counts.LOAN + '건  /  현재 scope ' + s.scope + ' = ' + s.count + '건';
            },
            onError: function (e) { log(e.response || String(e)); }
        });
    }

    // 4) 진행 중 전체
    function loadList() {
        $('statusLink').href = '/common/resume/status?scope=' + scope() + '&' + q();
        return api('/api/common/resume/list?scope=' + scope() + '&' + q()).then(function (res) {
            var tb = $('rows'); tb.innerHTML = '';
            if (!res.success) { var tr0 = el('tr'); var td0 = el('td', 'text-danger', res.code + ' ' + res.message); td0.colSpan = 8; tr0.appendChild(td0); tb.appendChild(tr0); return; }
            if (!res.data.length) { var tr1 = el('tr'); var td1 = el('td', 'text-muted', '진행 중인 신청 없음'); td1.colSpan = 8; tr1.appendChild(td1); tb.appendChild(tr1); return; }
            res.data.forEach(function (r, i) {
                var tr = el('tr', i === 0 ? 'table-primary' : '');
                tr.appendChild(el('td', null, String(r.id)));
                var c = el('td'); c.appendChild(el('span', 'badge scope-badge ' + (r.productType === 'LOAN' ? 'bg-primary' : 'bg-success'), r.productTypeName)); tr.appendChild(c);
                tr.appendChild(el('td', null, r.companyName + (r.own ? '' : ' (제휴)')));
                tr.appendChild(el('td', null, r.productName));
                tr.appendChild(el('td', null, r.stepName + (r.totalSteps ? ' ' + r.stepNo + '/' + r.totalSteps : '')));
                tr.appendChild(el('td', 'text-nowrap', r.lastActivityAt));
                tr.appendChild(el('td', 'text-nowrap', r.expiresAt ? r.expiresAt + ' (D-' + r.daysLeft + ')' : '없음'));
                var act = el('td', 'text-nowrap');
                [['완료', 'complete', 'btn-outline-success'], ['취소', 'cancel', 'btn-outline-danger']].forEach(function (b) {
                    var btn = el('button', 'btn btn-sm py-0 px-1 me-1 ' + b[2], b[0]);
                    btn.addEventListener('click', function () {
                        api('/api/common/resume-demo/' + r.id + '/' + b[1] + '?' + q(), { method: 'POST' }).then(function (x) { log(x); reloadAll(); });
                    });
                    act.appendChild(btn);
                });
                tr.appendChild(act);
                tb.appendChild(tr);
            });
        });
    }

    function reloadAll() { return mountCard().then(loadList); }

    // 3) 신청 업무 연동 흉내 - 단계 저장
    $('btnStep').addEventListener('click', function () {
        var p = $('dProduct').value.split('|');
        var st = $('dStep').value.split('|');
        var total = Number(p[6]);
        var body = {
            productType: p[0], bizCode: p[1], bizName: p[2], productCode: p[3], productName: p[4], partnerCode: p[5],
            stepNo: Math.min(Number(st[0]), total), stepName: st[1], totalSteps: total,
            resumeUrl: '/common/resume/landing?biz=' + p[1] + '&product=' + p[3] + '&step=' + st[0],
            expireDays: $('dExpire').value === '' ? null : Number($('dExpire').value)
        };
        api('/api/common/resume-demo/steps?' + q(), { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })
            .then(function (res) { log(res); if (res.success) reloadAll(); });
    });

    $('btnReload').addEventListener('click', reloadAll);
    $('customer').addEventListener('change', reloadAll);
    $('scope').addEventListener('change', reloadAll);
    document.querySelectorAll('#phoneTabs span').forEach(function (t) {
        t.style.cursor = 'pointer';
        t.addEventListener('click', function () { $('scope').value = t.getAttribute('data-scope'); reloadAll(); });
    });
    reloadAll();
})();
</script>
</body>
</html>
