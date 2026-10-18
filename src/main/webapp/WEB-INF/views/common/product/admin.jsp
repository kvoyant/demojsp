<%--
  =============================================================================
   [공통#48] ADMIN 수신상품목록 / 여신상품목록 - 상품설정 (홈 표시여부 · 홈 표시순서)  -  GET /common/product/admin
  -----------------------------------------------------------------------------
   1. 상품설정 컬럼 추가: 홈 표시여부(Y/N), 홈 표시순서(1/2/3)
   2. 수신+여신 합산 3개까지 - 초과 Y 시도 시 안내팝업 "등록 건수 3개를 초과하였습니다."
   3. Y 이면 표시순서 필수, 수신·여신 간 같은 숫자 불가 - 사용중 순서 음영처리 + 안내팝업 "표시순서를 변경하세요"
   4. 기존 상품설정(상품명·설명·금리·URL·판매여부)을 추천 상품 카드에 연동 - 우측 홈 미리보기에 바로 반영
   서버가 같은 규칙으로 최종 검증한다 (화면 검사만 믿지 않음).
   주의: JSP 에서는 JS 템플릿 문자열의 달러+중괄호가 EL 로 해석되므로 문자열 더하기를 사용한다.
  =============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>디지털어드민 - 수신/여신 상품목록</title>
    <link rel="shortcut icon" href="/favicon.ico">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css" rel="stylesheet">
    <link rel="stylesheet" href="/css/common/ui-tokens.css">
    <link rel="stylesheet" href="/css/common/ui-button.css">
    <link rel="stylesheet" href="/css/common/ui-icon.css">
    <link rel="stylesheet" href="/css/common/ui-alert.css">
    <link rel="stylesheet" href="/css/common/recommend-products.css">
    <script src="/js/common/ui-button.js"></script>
    <script src="/js/common/ui-alert.js"></script>
    <script src="/js/common/recommend-products.js"></script>
    <style>
        body { background-color: #f4f6f9; font-family: 'Malgun Gothic', 'Noto Sans KR', sans-serif; }
        [hidden] { display: none !important; }
        .table-sm td, .table-sm th { font-size: 12px; vertical-align: middle; }
        th.setting, td.setting { background: #fffbea; }
        tr.row-home td { font-weight: 600; }
        tr.row-off td { color: #9ca3af; }
        .slots { display: flex; gap: 8px; flex-wrap: wrap; }
        .slot { border: 1px solid #d1d5db; border-radius: 8px; padding: 6px 10px; background: #fff; font-size: 12px; min-width: 150px; }
        .slot b { color: #0d47a1; margin-right: 4px; }
        .slot.empty { color: #9ca3af; border-style: dashed; }
        select option:disabled { color: #9ca3af; background: #f3f4f6; }   /* 사용중 순서 음영처리 */
        .preview-phone { max-width: 360px; background: #fff; border-radius: 16px; padding: 14px; border: 1px solid #e5e7eb; }
        pre.result-log { background: #212529; color: #e9ecef; border-radius: 6px; padding: 10px; max-height: 200px; overflow: auto; white-space: pre-wrap; font-size: 12px; margin: 0; }
        .kind-tabs .nav-link { font-size: 13px; padding: .35rem .9rem; }
    </style>
</head>
<body>
<jsp:include page="/WEB-INF/views/common/navbar.jsp" />

<div class="container-xl pb-5">
    <div class="d-flex align-items-center mb-2">
        <a href="/common/test-hub" class="btn btn-sm btn-outline-secondary me-2"><i class="bi bi-arrow-left"></i> 테스트 목록</a>
        <h5 class="fw-bold mb-0">[공통#48] 상품관리 &gt; 수신상품목록 / 여신상품목록 - 상품설정 (홈 표시)</h5>
    </div>
    <div class="alert alert-info small py-2">
        <b>상품설정</b>에 <b>홈 표시여부</b>(Y/N)와 <b>홈 표시순서</b>(1/2/3)가 추가되었습니다. 수신상품목록과 여신상품목록을 <b>합쳐 3개까지</b> Y 로 등록할 수 있고,
        같은 표시순서는 수신·여신을 통틀어 한 번만 쓸 수 있습니다 (사용중 순서는 음영처리). 저장하면 우측 홈 미리보기(<a href="/common/product/recommend">공통#47</a>)에 바로 반영됩니다.
    </div>

    <div class="card shadow-sm mb-3">
        <div class="card-body py-2 d-flex flex-wrap align-items-center gap-3">
            <span class="small fw-bold">홈 표시 현황 <span id="homeCount" class="badge bg-primary">-</span></span>
            <div class="slots" id="slots"></div>
        </div>
    </div>

    <div class="row g-4">
        <div class="col-lg-8">
            <div class="card shadow-sm mb-4">
                <div class="card-header d-flex justify-content-between align-items-center">
                    <ul class="nav nav-pills kind-tabs" id="kindTabs">
                        <li class="nav-item"><button class="nav-link active" data-kind="DEPOSIT">수신상품목록</button></li>
                        <li class="nav-item"><button class="nav-link" data-kind="LOAN">여신상품목록</button></li>
                    </ul>
                    <button id="btnReload" class="btn btn-sm btn-outline-primary">새로고침</button>
                </div>
                <div class="card-body p-0">
                    <div class="table-responsive">
                        <table class="table table-sm table-hover mb-0">
                            <thead class="table-light">
                            <tr><th>코드</th><th>유형</th><th>상품명</th><th>금리</th><th>판매</th>
                                <th class="setting">홈 표시여부</th><th class="setting">홈 표시순서</th><th class="setting">저장</th><th>상품설정</th></tr>
                            </thead>
                            <tbody id="rows"><tr><td colspan="9" class="text-muted">-</td></tr></tbody>
                        </table>
                    </div>
                </div>
                <div class="card-footer small text-muted">노란 칸 = 공통#48 추가 컬럼. 판매중지 상품은 Y 여도 홈에 보이지 않습니다.</div>
            </div>

            <div class="card shadow-sm" id="editCard" hidden>
                <div class="card-header fw-bold"><i class="bi bi-pencil-square me-1"></i> 상품설정 - <span id="editTitle"></span> <span class="text-muted small fw-normal">(추천 상품 카드에 연동되는 항목)</span></div>
                <div class="card-body">
                    <input type="hidden" id="eId">
                    <div class="row g-2">
                        <div class="col-md-6"><label class="form-label small fw-bold" for="eName">상품명 *</label><input id="eName" class="form-control form-control-sm" maxlength="30"></div>
                        <div class="col-md-3"><label class="form-label small fw-bold" for="eRate">금리 문구</label><input id="eRate" class="form-control form-control-sm" maxlength="20"></div>
                        <div class="col-md-3"><label class="form-label small fw-bold" for="eSale">판매여부</label>
                            <select id="eSale" class="form-select form-select-sm"><option value="true">판매중</option><option value="false">판매중지</option></select></div>
                        <div class="col-md-7"><label class="form-label small fw-bold" for="eSummary">상품 설명</label><input id="eSummary" class="form-control form-control-sm" maxlength="40"></div>
                        <div class="col-md-5"><label class="form-label small fw-bold" for="eUrl">상품안내페이지 URL *</label><input id="eUrl" class="form-control form-control-sm"></div>
                        <div class="col-12 text-end">
                            <button id="btnEditCancel" class="btn btn-sm btn-outline-secondary">닫기</button>
                            <button id="btnEditSave" class="btn btn-sm btn-primary fw-bold">상품설정 저장</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="col-lg-4">
            <div class="card shadow-sm mb-4">
                <div class="card-header fw-bold"><i class="bi bi-phone me-1"></i> 홈 추천 상품 미리보기 (일반회원)</div>
                <div class="card-body"><div class="preview-phone"><div id="preview"></div><div id="previewEmpty" class="small text-muted text-center" hidden>홈 표시 상품 없음 → 추천 상품 영역 숨김</div></div></div>
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
    function api(url, opt) {
        opt = opt || {};
        opt.credentials = 'same-origin';
        opt.headers = Object.assign({ 'X-Requested-With': 'XMLHttpRequest' }, opt.headers || {});
        return fetch(url, opt).then(function (r) { return r.json(); });
    }
    /** 안내팝업 (공통 Alert 공통#42) */
    function popup(msg) { return window.UiAlert ? UiAlert.alert(msg, { type: 'warning' }) : Promise.resolve(alert(msg)); }

    var kind = 'DEPOSIT', status = null;

    function loadStatus() {
        return api('/api/common/product-admin/home-status').then(function (res) {
            if (!res.success) { log(res); return; }
            status = res.data;
            $('homeCount').textContent = status.count + ' / ' + status.maxCount + '건';
            $('homeCount').className = 'badge ' + (status.count >= status.maxCount ? 'bg-danger' : 'bg-primary');
            var box = $('slots'); box.textContent = '';
            status.slots.forEach(function (s) {
                var d = el('div', 'slot' + (s.productId ? '' : ' empty'));
                d.appendChild(el('b', null, String(s.order)));
                d.appendChild(document.createTextNode(s.productId ? s.name + ' (' + s.kindName + ')' : '비어 있음'));
                box.appendChild(d);
            });
        });
    }

    function slotOwner(order) {
        var s = status && status.slots.filter(function (x) { return x.order === order; })[0];
        return s && s.productId ? s : null;
    }

    function loadList() {
        document.querySelectorAll('#kindTabs .nav-link').forEach(function (b) { b.classList.toggle('active', b.getAttribute('data-kind') === kind); });
        return api('/api/common/product-admin/products?kind=' + kind).then(function (res) {
            var tb = $('rows'); tb.textContent = '';
            if (!res.success) { log(res); return; }
            res.data.forEach(function (p) {
                var tr = el('tr', p.shownOnHome ? 'row-home' : (p.onSale ? '' : 'row-off'));
                tr.appendChild(el('td', null, p.code));
                tr.appendChild(el('td', null, p.subType));
                tr.appendChild(el('td', null, p.name));
                tr.appendChild(el('td', null, p.rateText || '-'));
                tr.appendChild(el('td', null, p.onSale ? '판매중' : '판매중지'));

                // 홈 표시여부 Y/N
                var tdY = el('td', 'setting');
                var selY = el('select', 'form-select form-select-sm');
                selY.setAttribute('aria-label', p.name + ' 홈 표시여부');
                ['N', 'Y'].forEach(function (v) { var o = el('option', null, v); o.value = v; selY.appendChild(o); });
                selY.value = p.homeDisplay ? 'Y' : 'N';
                tdY.appendChild(selY); tr.appendChild(tdY);

                // 홈 표시순서 1/2/3 - 다른 상품이 쓰는 순서는 음영처리(disabled)
                var tdO = el('td', 'setting');
                var selO = el('select', 'form-select form-select-sm');
                selO.setAttribute('aria-label', p.name + ' 홈 표시순서');
                var none = el('option', null, '선택'); none.value = ''; selO.appendChild(none);
                for (var i = 1; i <= status.maxCount; i++) {
                    var owner = slotOwner(i);
                    var used = owner && owner.productId !== p.id;
                    var o = el('option', null, used ? i + ' (사용중: ' + owner.name + ')' : String(i));
                    o.value = String(i); o.disabled = !!used;
                    selO.appendChild(o);
                }
                selO.value = p.homeOrder ? String(p.homeOrder) : '';
                selO.disabled = selY.value !== 'Y';
                tdO.appendChild(selO); tr.appendChild(tdO);

                selY.addEventListener('change', function () {
                    // 기능2 화면 사전 검사: 이미 3개면 Y 불가 → 안내팝업 후 되돌림 (서버도 동일 검증)
                    if (selY.value === 'Y' && !p.homeDisplay && status.count >= status.maxCount) {
                        popup('등록 건수 ' + status.maxCount + '개를 초과하였습니다.').then(function () { selY.value = 'N'; selY.focus(); });
                        return;
                    }
                    selO.disabled = selY.value !== 'Y';
                    if (selY.value !== 'Y') selO.value = '';
                    else if (!selO.value) selO.focus();
                });

                var tdS = el('td', 'setting');
                var bS = el('button', 'btn btn-sm btn-primary py-0 px-2', '저장');
                bS.addEventListener('click', function () {
                    if (selY.value === 'Y' && !selO.value) { popup('홈 표시여부 Y 는 홈 표시순서를 선택해야 합니다.').then(function () { selO.focus(); }); return; }
                    api('/api/common/product-admin/products/' + p.id + '/home', {
                        method: 'PUT', headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ homeDisplay: selY.value, homeOrder: selO.value ? Number(selO.value) : null })
                    }).then(function (r) {
                        log(r);
                        if (!r.success) { popup(r.message).then(refresh); return; }      // 서버 안내팝업 문구 그대로
                        refresh();
                    });
                });
                tdS.appendChild(bS); tr.appendChild(tdS);

                var tdE = el('td');
                var bE = el('button', 'btn btn-sm btn-outline-secondary py-0 px-2', '수정');
                bE.addEventListener('click', function () { openEdit(p); });
                tdE.appendChild(bE); tr.appendChild(tdE);
                tb.appendChild(tr);
            });
        });
    }

    // 기능4 상품설정 → 추천 상품 카드 연동
    function openEdit(p) {
        $('editCard').hidden = false;
        $('eId').value = p.id; $('editTitle').textContent = p.kindName + ' · ' + p.code;
        $('eName').value = p.name; $('eRate').value = p.rateText || ''; $('eSummary').value = p.summary || '';
        $('eUrl').value = p.detailUrl; $('eSale').value = String(p.onSale);
        $('eName').focus();
    }
    $('btnEditCancel').addEventListener('click', function () { $('editCard').hidden = true; });
    $('btnEditSave').addEventListener('click', function () {
        api('/api/common/product-admin/products/' + $('eId').value, {
            method: 'PUT', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name: $('eName').value, summary: $('eSummary').value, rateText: $('eRate').value,
                detailUrl: $('eUrl').value, onSale: $('eSale').value === 'true' })
        }).then(function (r) { log(r); if (!r.success) { popup(r.message); return; } refresh(); });
    });

    function preview() {
        return RecommendProducts.mount('#preview', {
            params: { demoCustomer: 'customer02' },
            onNavigate: function (url, item) { log('[터치] ' + item.name + ' → ' + url); },
            onLoad: function (d) { $('previewEmpty').hidden = d.items.length > 0; }
        });
    }
    function refresh() { return loadStatus().then(loadList).then(preview); }

    document.querySelectorAll('#kindTabs .nav-link').forEach(function (b) {
        b.addEventListener('click', function () { kind = b.getAttribute('data-kind'); loadList(); });
    });
    $('btnReload').addEventListener('click', refresh);
    refresh();
})();
</script>
</body>
</html>
