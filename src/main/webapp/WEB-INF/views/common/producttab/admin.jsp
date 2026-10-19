<%--
  =============================================================================
   [CF-BC-20 / 공통#26] 디지털어드민 > 상품 > 메인화면 상품 탭 관리  -  GET /common/product-tab/admin
  -----------------------------------------------------------------------------
   요구사항 (REQ-AP-607 세부항목 2): 하단 상품 목록 정비 - ADMIN 컨트롤 가능하게 로직 구현
     탭1 상단 슬라이드 카드 : 문구 · 배지 · 배경색 · 이미지 · 연결 상품/URL · 채널 · 게시기간 · 표시순서 · 사용여부
     탭2 섹션 · 상품 목록   : 섹션(예금·적금 / 대출 / 서비스 카드) 순서·사용여부 + 섹션별 상품 순서·배지·사용여부
     오른쪽 미리보기        : 사용자 화면 API 와 같은 결과 (기준일 · 채널 바꿔 보기)
   상품 이름·설명·금리·URL·판매여부는 상품 원장(공통#48 상품설정)에서 관리한다 - 여기서는 "무엇을 어디에 몇 번째로" 만.
   주의: JSP 에서는 JS 템플릿 문자열의 달러+중괄호가 EL 로 해석되므로 문자열 더하기를 사용한다.
  =============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>디지털어드민 - 상품 탭 관리</title>
    <link rel="shortcut icon" href="/favicon.ico">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css" rel="stylesheet">
    <link rel="stylesheet" href="/css/common/ui-tokens.css">
    <link rel="stylesheet" href="/css/common/ui-icon.css">
    <link rel="stylesheet" href="/css/common/ui-alert.css">
    <link rel="stylesheet" href="/css/common/product-tab.css">
    <script src="/js/common/ui-alert.js"></script>
    <script src="/js/common/product-tab.js"></script>
    <style>
        body { background-color: #f4f6f9; font-family: 'Malgun Gothic', 'Noto Sans KR', sans-serif; }
        [hidden] { display: none !important; }
        .table-sm td, .table-sm th { font-size: 12px; vertical-align: middle; }
        .form-control-sm, .form-select-sm { font-size: 12px; }
        tr.sel td { background: #e3ecfa !important; }
        tr.clickable { cursor: pointer; }
        tr.off td { color: #9ca3af; }
        .swatch { display: inline-block; width: 14px; height: 14px; border-radius: 3px; border: 1px solid #d1d5db; vertical-align: -2px; margin-right: 4px; }
        .state { font-size: 11px; padding: 1px 6px; border-radius: 8px; }
        .state.on { background: #e6f4ea; color: #1b7a3d; } .state.wait { background: #e3ecfa; color: #0d47a1; } .state.off { background: #f3f4f6; color: #6b7280; }
        .preview-phone { max-width: 360px; margin: 0 auto; background: #f5f7fb; border-radius: 20px; padding: 14px; border: 8px solid #1f2937; max-height: 760px; overflow: auto; }
        .nav-tabs .nav-link { font-size: 13px; }
        .table-sm th, td.small { white-space: nowrap; }
    </style>
</head>
<body>
<jsp:include page="/WEB-INF/views/common/navbar.jsp" />

<div class="container-xl pb-5">
    <div class="d-flex align-items-center mb-2">
        <a href="/common/test-hub" class="btn btn-sm btn-outline-secondary me-2"><i class="bi bi-arrow-left"></i> 테스트 목록</a>
        <h5 class="fw-bold mb-0">[공통#26] 디지털어드민 &gt; 상품 &gt; 메인화면 상품 탭 관리</h5>
        <a href="/common/product-tab/sample" class="btn btn-sm btn-outline-primary ms-auto"><i class="bi bi-phone"></i> 사용자 화면</a>
    </div>
    <div class="alert alert-info small py-2">
        저장하면 사용자 화면에 <b>바로</b> 반영됩니다. 불필요한 카드(예: 시세조회)는 섹션의 <b>사용여부를 N</b> 으로 바꾸면 화면에서 사라집니다.
        상품명·금리·상품안내 URL·판매여부는 <a href="/common/product/admin">상품설정(공통#48)</a> 값을 그대로 쓰며, 판매중지 상품은 자동으로 빠집니다.
    </div>

    <div class="row g-3">
        <div class="col-xl-8">
            <ul class="nav nav-tabs mb-3" id="tabs">
                <li class="nav-item"><button class="nav-link active" data-tab="slide">① 상단 슬라이드 카드</button></li>
                <li class="nav-item"><button class="nav-link" data-tab="section">② 섹션 · 상품 목록</button></li>
            </ul>

            <!-- ================================================================ 탭1 슬라이드 -->
            <section id="tab-slide">
                <div class="card shadow-sm mb-3"><div class="card-body p-0">
                    <table class="table table-sm table-hover mb-0">
                        <thead class="table-light"><tr><th>순서</th><th>메인 문구</th><th>연결</th><th>채널</th><th>게시기간</th><th>상태</th></tr></thead>
                        <tbody id="slideRows"></tbody>
                    </table>
                </div></div>
                <div class="card shadow-sm">
                    <div class="card-header fw-bold d-flex">슬라이드 카드 <span id="slideMode" class="badge bg-success ms-2">신규</span>
                        <button class="btn btn-sm btn-link ms-auto p-0" id="slideNew">새 카드</button></div>
                    <div class="card-body">
                        <div class="row g-2">
                            <div class="col-md-6"><label class="small">메인 문구 (30자)</label><input id="sTitle" class="form-control form-control-sm" maxlength="30"></div>
                            <div class="col-md-6"><label class="small">서브 문구 (40자)</label><input id="sSub" class="form-control form-control-sm" maxlength="40"></div>
                            <div class="col-md-3"><label class="small">배지 (10자)</label><input id="sBadge" class="form-control form-control-sm" maxlength="10"></div>
                            <div class="col-md-3"><label class="small">배경색</label><input id="sColor" type="color" class="form-control form-control-sm form-control-color w-100" value="#0D47A1"></div>
                            <div class="col-md-3"><label class="small">표시순서</label><input id="sOrd" type="number" min="1" max="99" class="form-control form-control-sm" value="1"></div>
                            <div class="col-md-3"><label class="small">사용</label><select id="sUse" class="form-select form-select-sm"><option>Y</option><option>N</option></select></div>
                            <div class="col-md-6"><label class="small">연결 상품 (선택 시 상품안내 URL 로 이동)</label><select id="sProduct" class="form-select form-select-sm"><option value="">(연결 안 함 - 이동 URL 사용)</option></select></div>
                            <div class="col-md-6"><label class="small">이동 URL (상품 연결이 없을 때)</label><input id="sLink" class="form-control form-control-sm" placeholder="/event/... 또는 https://"></div>
                            <div class="col-md-6"><label class="small">이미지 URL (선택)</label><input id="sImg" class="form-control form-control-sm" placeholder="/images/... 또는 https://"></div>
                            <div class="col-md-6"><label class="small">이미지 대체텍스트 (이미지가 있으면 필수)</label><input id="sAlt" class="form-control form-control-sm"></div>
                            <div class="col-md-4"><label class="small">게시 시작일</label><input id="sStart" type="date" class="form-control form-control-sm"></div>
                            <div class="col-md-4"><label class="small">게시 종료일 (포함)</label><input id="sEnd" type="date" class="form-control form-control-sm"></div>
                            <div class="col-md-4"><label class="small d-block">노출채널</label>
                                <label class="small me-2"><input type="checkbox" id="sApp" checked> APP</label>
                                <label class="small"><input type="checkbox" id="sPc" checked> PC_WEB</label></div>
                        </div>
                        <button class="btn btn-sm btn-primary mt-2" id="slideSave">저장</button>
                    </div>
                </div>
            </section>

            <!-- ================================================================ 탭2 섹션 · 상품 -->
            <section id="tab-section" hidden>
                <div class="row g-3">
                    <div class="col-md-5">
                        <div class="card shadow-sm mb-3"><div class="card-body p-0">
                            <table class="table table-sm table-hover mb-0">
                                <thead class="table-light"><tr><th>순서</th><th>섹션</th><th>유형</th><th>상품</th><th>사용</th></tr></thead>
                                <tbody id="secRows"></tbody>
                            </table>
                        </div></div>
                        <div class="card shadow-sm">
                            <div class="card-header fw-bold d-flex">섹션 등록 / 수정 <button class="btn btn-sm btn-link ms-auto p-0" id="secNew">새 섹션</button></div>
                            <div class="card-body">
                                <div class="row g-2">
                                    <div class="col-6"><input id="cCd" class="form-control form-control-sm" placeholder="섹션코드 (예: EVENT)"></div>
                                    <div class="col-6"><input id="cNm" class="form-control form-control-sm" placeholder="섹션 제목" maxlength="30"></div>
                                    <div class="col-12"><input id="cDesc" class="form-control form-control-sm" placeholder="설명 한 줄 (60자)" maxlength="60"></div>
                                    <div class="col-4"><select id="cType" class="form-select form-select-sm"><option value="PRODUCT">상품목록</option><option value="CARD">서비스 카드</option></select></div>
                                    <div class="col-4"><input id="cOrd" type="number" min="1" max="99" class="form-control form-control-sm" placeholder="순서"></div>
                                    <div class="col-4"><select id="cUse" class="form-select form-select-sm"><option value="Y">사용</option><option value="N">미사용(삭제)</option></select></div>
                                    <div class="col-4 card-only"><input id="cIcon" class="form-control form-control-sm" placeholder="아이콘 (calculator)"></div>
                                    <div class="col-8 card-only"><input id="cLink" class="form-control form-control-sm" placeholder="이동 URL"></div>
                                </div>
                                <button class="btn btn-sm btn-primary mt-2" id="secSave">섹션 저장</button>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-7">
                        <div class="card shadow-sm">
                            <div class="card-header fw-bold d-flex align-items-center">섹션 상품 목록 <span id="itemTitle" class="badge bg-primary ms-2"></span>
                                <button class="btn btn-sm btn-outline-secondary ms-auto me-1" id="itemAdd">+ 상품</button>
                                <button class="btn btn-sm btn-primary" id="itemSave">저장</button></div>
                            <div class="card-body p-0">
                                <table class="table table-sm mb-0">
                                    <thead class="table-light"><tr><th>상품</th><th style="width:70px">순서</th><th style="width:80px">배지</th><th style="width:66px">사용</th><th>판매</th><th></th></tr></thead>
                                    <tbody id="itemRows"><tr><td colspan="6" class="text-muted">상품목록 섹션을 선택하세요.</td></tr></tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>
            </section>
        </div>

        <!-- ================================================================ 미리보기 -->
        <div class="col-xl-4">
            <div class="card shadow-sm">
                <div class="card-header fw-bold d-flex align-items-center gap-1">미리보기
                    <select id="pvChannel" class="form-select form-select-sm ms-auto" style="max-width:100px"><option>APP</option><option>PC_WEB</option></select>
                    <input type="date" id="pvDate" class="form-control form-control-sm" style="max-width:140px">
                </div>
                <div class="card-body"><div class="preview-phone"><div id="preview"></div></div></div>
            </div>
        </div>
    </div>
</div>

<script>
(function () {
    var API = '/api/common/product-tab-admin';
    var $ = function (id) { return document.getElementById(id); };
    var products = [];        // 상품 원장 (선택 상자)
    var curSlide = null;      // 수정 중인 슬라이드 ID
    var curSection = null;    // 선택한 상품목록 섹션

    // ------------------------------------------------------------------ 공통
    function api(method, url, body) {
        return fetch(API + url, {
            method: method,
            headers: body ? { 'Content-Type': 'application/json' } : {},
            body: body ? JSON.stringify(body) : undefined
        }).then(function (r) { return r.json(); }).then(function (res) {
            if (!res.success) { UiAlert.alert(res.message, { type: 'error' }); throw res; }
            return res;
        });
    }
    function td(tr, text, cls) {
        var c = document.createElement('td');
        if (text instanceof Node) c.appendChild(text); else c.textContent = text == null ? '' : text;
        if (cls) c.className = cls;
        tr.appendChild(c);
        return c;
    }
    function span(cls, text) { var s = document.createElement('span'); s.className = cls; s.textContent = text; return s; }
    function today() { var d = new Date(); return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0'); }

    document.querySelectorAll('#tabs .nav-link').forEach(function (b) {
        b.addEventListener('click', function () {
            document.querySelectorAll('#tabs .nav-link').forEach(function (x) { x.classList.toggle('active', x === b); });
            ['slide', 'section'].forEach(function (t) { $('tab-' + t).hidden = t !== b.dataset.tab; });
        });
    });

    // ------------------------------------------------------------------ 미리보기 (사용자 API 와 같은 결과를 컴포넌트로 그림)
    function preview() {
        var q = '?channel=' + $('pvChannel').value + ($('pvDate').value ? '&baseDt=' + $('pvDate').value : '');
        fetch(API + '/preview' + q).then(function (r) { return r.json(); }).then(function (res) {
            if (!res.success) { $('preview').textContent = res.message; return; }
            ProductTab.mount('#preview', { data: res.data, onNavigate: function (url) { UiAlert.alert('이동: ' + url); } });
        });
    }
    $('pvChannel').addEventListener('change', preview);
    $('pvDate').addEventListener('change', preview);

    // ------------------------------------------------------------------ 탭1 슬라이드
    function slideState(s) {
        var t = today();
        if (s.useYn !== 'Y') return span('state off', '미사용');
        if (s.endDt < t) return span('state off', '게시종료');
        if (s.startDt > t) return span('state wait', '게시예정');
        if (s.productCd && s.productOnSaleYn !== 'Y') return span('state off', '상품 판매중지');
        return span('state on', '게시중');
    }
    function loadSlides() {
        return api('GET', '/slides').then(function (res) {
            var tb = $('slideRows');
            tb.textContent = '';
            res.data.forEach(function (s) {
                var tr = document.createElement('tr');
                tr.className = 'clickable' + (s.slideId === curSlide ? ' sel' : '');
                td(tr, s.sortOrd);
                var title = document.createElement('span');
                var sw = span('swatch', ''); sw.style.background = s.bgColor;
                title.appendChild(sw); title.appendChild(document.createTextNode(s.title));
                td(tr, title);
                td(tr, s.productCd ? (s.productNm || s.productCd) : s.linkUrl);
                td(tr, s.channels);
                td(tr, s.startDt + ' ~ ' + s.endDt);
                td(tr, slideState(s));
                tr.addEventListener('click', function () { editSlide(s); });
                tb.appendChild(tr);
            });
        });
    }
    function editSlide(s) {
        curSlide = s ? s.slideId : null;
        $('slideMode').textContent = s ? '수정 #' + s.slideId : '신규';
        $('slideMode').className = 'badge ms-2 ' + (s ? 'bg-warning text-dark' : 'bg-success');
        $('sTitle').value = s ? s.title : '';
        $('sSub').value = s && s.subText || '';
        $('sBadge').value = s && s.badgeText || '';
        $('sColor').value = s ? s.bgColor : '#0D47A1';
        $('sOrd').value = s ? s.sortOrd : 1;
        $('sUse').value = s ? s.useYn : 'Y';
        $('sProduct').value = s && s.productCd || '';
        $('sLink').value = s && s.linkUrl || '';
        $('sImg').value = s && s.imageUrl || '';
        $('sAlt').value = s && s.imageAlt || '';
        $('sStart').value = s ? s.startDt : today();
        $('sEnd').value = s ? s.endDt : '';
        $('sApp').checked = !s || s.channels.indexOf('APP') >= 0;
        $('sPc').checked = !s || s.channels.indexOf('PC_WEB') >= 0;
        document.querySelectorAll('#slideRows tr').forEach(function (tr, i) { tr.classList.remove('sel'); });
    }
    $('slideNew').addEventListener('click', function () { editSlide(null); });
    $('slideSave').addEventListener('click', function () {
        var ch = [];
        if ($('sApp').checked) ch.push('APP');
        if ($('sPc').checked) ch.push('PC_WEB');
        api('POST', '/slides', {
            slideId: curSlide, title: $('sTitle').value, subText: $('sSub').value, badgeText: $('sBadge').value,
            bgColor: $('sColor').value.toUpperCase(), sortOrd: parseInt($('sOrd').value, 10), useYn: $('sUse').value,
            productCd: $('sProduct').value, linkUrl: $('sLink').value, imageUrl: $('sImg').value, imageAlt: $('sAlt').value,
            startDt: $('sStart').value || null, endDt: $('sEnd').value || null, channels: ch.join(',')
        }).then(function (res) {
            UiAlert.alert(res.message);
            editSlide(res.data);
            loadSlides();
            preview();
        });
    });

    // ------------------------------------------------------------------ 탭2 섹션
    function loadSections() {
        return api('GET', '/sections').then(function (res) {
            var tb = $('secRows');
            tb.textContent = '';
            res.data.forEach(function (s) {
                var tr = document.createElement('tr');
                tr.className = 'clickable' + (s.useYn === 'N' ? ' off' : '') + (s.sectionCd === curSection ? ' sel' : '');
                td(tr, s.sortOrd); td(tr, s.sectionNm + ' (' + s.sectionCd + ')');
                td(tr, s.sectionType === 'CARD' ? '서비스 카드' : '상품목록');
                td(tr, s.sectionType === 'CARD' ? '-' : s.itemCnt);
                td(tr, s.useYn === 'Y' ? '사용' : '미사용');
                tr.addEventListener('click', function () { editSection(s); });
                tb.appendChild(tr);
            });
        });
    }
    function toggleCardFields() {
        document.querySelectorAll('.card-only').forEach(function (e) { e.hidden = $('cType').value !== 'CARD'; });
    }
    $('cType').addEventListener('change', toggleCardFields);
    function editSection(s) {
        $('cCd').value = s ? s.sectionCd : ''; $('cCd').readOnly = !!s;
        $('cNm').value = s ? s.sectionNm : ''; $('cDesc').value = s && s.sectionDesc || '';
        $('cType').value = s ? s.sectionType : 'PRODUCT'; $('cOrd').value = s ? s.sortOrd : '';
        $('cUse').value = s ? s.useYn : 'Y'; $('cIcon').value = s && s.cardIcon || ''; $('cLink').value = s && s.linkUrl || '';
        toggleCardFields();
        document.querySelectorAll('#secRows tr').forEach(function (tr) { tr.classList.remove('sel'); });
        if (s && s.sectionType === 'PRODUCT') loadItems(s); else { curSection = null; $('itemTitle').textContent = ''; $('itemRows').textContent = ''; }
    }
    $('secNew').addEventListener('click', function () { editSection(null); });
    $('secSave').addEventListener('click', function () {
        api('POST', '/sections', {
            sectionCd: $('cCd').value.trim(), sectionNm: $('cNm').value, sectionDesc: $('cDesc').value, sectionType: $('cType').value,
            sortOrd: parseInt($('cOrd').value, 10), useYn: $('cUse').value, cardIcon: $('cIcon').value, linkUrl: $('cLink').value
        }).then(function (res) {
            UiAlert.alert(res.message);
            loadSections();
            editSection(res.data);
            preview();
        });
    });

    // ------------------------------------------------------------------ 탭2 섹션별 상품
    function productSelect(value) {
        var s = document.createElement('select');
        s.className = 'form-select form-select-sm f-prd';
        products.forEach(function (p) {
            var o = document.createElement('option');
            o.value = p.productCd;
            o.textContent = (p.productKind === 'LOAN' ? '[여신] ' : '[수신] ') + p.productNm + (p.onSaleYn === 'Y' ? '' : ' (판매중지)');
            s.appendChild(o);
        });
        if (value) s.value = value;
        return s;
    }
    function addItemRow(i) {
        var tr = document.createElement('tr');
        td(tr, productSelect(i.productCd));
        var ord = document.createElement('input'); ord.type = 'number'; ord.min = 1; ord.max = 99; ord.value = i.sortOrd || 1; ord.className = 'form-control form-control-sm f-ord';
        td(tr, ord);
        var badge = document.createElement('input'); badge.maxLength = 10; badge.value = i.badgeText || ''; badge.className = 'form-control form-control-sm f-badge';
        td(tr, badge);
        var use = document.createElement('select'); use.className = 'form-select form-select-sm f-use';
        ['Y', 'N'].forEach(function (v) { var o = document.createElement('option'); o.textContent = v; use.appendChild(o); });
        use.value = i.useYn || 'Y';
        td(tr, use);
        td(tr, i.onSaleYn ? (i.onSaleYn === 'Y' ? '판매중' : '판매중지 (화면 제외)') : '', 'small ' + (i.onSaleYn === 'N' ? 'text-danger' : 'text-muted'));
        var del = document.createElement('button'); del.className = 'btn btn-sm btn-link text-danger p-0'; del.textContent = '삭제';
        del.addEventListener('click', function () { tr.remove(); });
        td(tr, del);
        $('itemRows').appendChild(tr);
    }
    function loadItems(sec) {
        curSection = sec.sectionCd;
        $('itemTitle').textContent = sec.sectionNm;
        api('GET', '/sections/' + sec.sectionCd + '/items').then(function (res) {
            $('itemRows').textContent = '';
            res.data.forEach(addItemRow);
        });
    }
    $('itemAdd').addEventListener('click', function () { if (curSection) addItemRow({ sortOrd: $('itemRows').children.length + 1 }); });
    $('itemSave').addEventListener('click', function () {
        if (!curSection) return;
        var rows = Array.prototype.map.call($('itemRows').children, function (tr) {
            return { productCd: tr.querySelector('.f-prd').value, sortOrd: parseInt(tr.querySelector('.f-ord').value, 10),
                     badgeText: tr.querySelector('.f-badge').value, useYn: tr.querySelector('.f-use').value };
        });
        api('PUT', '/sections/' + curSection + '/items', rows).then(function (res) {
            UiAlert.alert(res.message);
            $('itemRows').textContent = '';
            res.data.forEach(addItemRow);
            loadSections();
            preview();
        });
    });

    // ------------------------------------------------------------------ 시작
    api('GET', '/products').then(function (res) {
        products = res.data;
        products.forEach(function (p) {
            var o = document.createElement('option');
            o.value = p.productCd;
            o.textContent = p.productNm + ' (' + p.productCd + ')' + (p.onSaleYn === 'Y' ? '' : ' - 판매중지');
            $('sProduct').appendChild(o);
        });
        editSlide(null);
        loadSlides();
        loadSections();
        preview();
    });
})();
</script>
</body>
</html>
