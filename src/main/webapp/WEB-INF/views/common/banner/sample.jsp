<%--
  =============================================================================
   [CF-BC-20 / 공통#39 샘플] 배너팝업 복수 노출 (표시순서 · 좌우 슬라이드)  -  GET /common/banner/sample
  -----------------------------------------------------------------------------
   좌: APP 메인 진입 시 배너팝업 (업무 화면은 /js/common/banner-popup.js 포함 + BannerPopup.open({ channel:'APP' }))
   우: 디지털어드민 배너팝업목록 (등록/수정/사용여부/삭제, 기준시각 노출 미리보기)
       ADMIN 영역은 local(common.banner-popup.admin-api-open=true) 또는 관리자 로그인 시 동작한다.
  =============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>저축은행 포털 - 배너팝업 공통모듈 샘플</title>
    <link rel="shortcut icon" href="/favicon.ico">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css" rel="stylesheet">
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <!-- 배너팝업 공통 JS (이 한 줄만 있으면 된다) -->
    <script src="/js/common/banner-popup.js"></script>
    <style>
        body { background-color: #f4f6f9; font-family: 'Malgun Gothic', 'Noto Sans KR', sans-serif; }
        .card-header-financial { background-color: #0d47a1; color: white; font-weight: bold; }
        pre.result-log { background: #212529; color: #e9ecef; border-radius: 6px; padding: 12px; min-height: 80px; max-height: 300px; overflow: auto; white-space: pre-wrap; font-size: 12px; }
        .table-sm td, .table-sm th { font-size: 12px; vertical-align: middle; }
        /* APP 메인 목업 */
        .phone { width: 100%; max-width: 360px; height: 640px; margin: 0 auto; border: 10px solid #1f2937; border-radius: 36px; background: #f5f7fb; overflow: hidden; position: relative; }
        .phone-top { background: #0d47a1; color: #fff; padding: 18px 16px 14px; }
        .phone-top .greet { font-size: 13px; opacity: .8; }
        .phone-card { background: #fff; border-radius: 14px; margin: 12px; padding: 14px; box-shadow: 0 1px 4px rgba(0,0,0,.06); font-size: 13px; }
        .phone-card .amt { font-size: 20px; font-weight: 700; }
        .phone-menu { display: grid; grid-template-columns: repeat(4, 1fr); gap: 6px; text-align: center; font-size: 11px; color: #444; }
        .phone-menu i { display: block; font-size: 22px; color: #0d47a1; }
        .status-badge { font-size: 11px; }
        .order-pill { display: inline-block; min-width: 22px; text-align: center; border-radius: 11px; background: #e3ecfa; color: #0d47a1; font-weight: 700; }
        .preview-list li { font-size: 13px; }
        tr.row-off td { color: #9ca3af; }
    </style>
</head>
<body>
<jsp:include page="/WEB-INF/views/common/navbar.jsp" />

<div class="container-xl pb-5">
    <div class="d-flex align-items-center mb-3">
        <a href="/common/test-hub" class="btn btn-sm btn-outline-secondary me-2"><i class="bi bi-arrow-left"></i> 테스트 목록</a>
        <h5 class="fw-bold mb-0">[CF-BC-20 / 공통#39] 배너팝업 복수 노출 (표시순서 · 좌우 슬라이드)</h5>
    </div>
    <div class="alert alert-info small py-2">
        <b>AS-IS</b> 같은 시간대에 배너팝업을 여러 건 등록하고 표시순서를 지정해도 APP 에 1건만 노출 →
        <b>TO-BE</b> 게시일자가 겹치는 배너를 표시순서대로 <b>모두</b> 노출하고 좌우로 넘겨 확인.
        seed 에는 오늘 기준 게시중 3건(표시순서 2·1·3 순서로 등록)과 예정/종료/PC WEB 전용/미사용 배너가 있습니다.
    </div>

    <div class="row g-4">
        <!-- 좌: 고객 화면 -->
        <div class="col-lg-5">
            <div class="card shadow-sm mb-4">
                <div class="card-header card-header-financial"><i class="bi bi-phone me-1"></i> 1. APP 메인 진입 (고객 화면)</div>
                <div class="card-body">
                    <div class="row g-2 mb-2">
                        <div class="col-5">
                            <label class="form-label small fw-bold" for="channel">채널</label>
                            <select id="channel" class="form-select form-select-sm">
                                <option value="APP">APP</option>
                                <option value="PC_WEB">PC WEB</option>
                                <option value="RECRUIT_WEB">모집인WEB</option>
                            </select>
                        </div>
                        <div class="col-7">
                            <label class="form-label small fw-bold" for="previewAt">기준시각 (ADMIN 미리보기)</label>
                            <input id="previewAt" type="datetime-local" class="form-control form-control-sm">
                        </div>
                    </div>
                    <div class="d-flex flex-wrap gap-2 mb-3">
                        <button id="btnOpen" class="btn btn-primary btn-sm"><i class="bi bi-box-arrow-in-right me-1"></i>메인 진입 (현재시각)</button>
                        <button id="btnPreview" class="btn btn-outline-primary btn-sm"><i class="bi bi-clock-history me-1"></i>기준시각으로 미리보기</button>
                        <button id="btnReset" class="btn btn-outline-secondary btn-sm">오늘 하루 보지 않기 초기화</button>
                    </div>

                    <div id="phone" class="phone">
                        <div class="phone-top">
                            <div class="greet">안녕하세요, 홍길동님</div>
                            <div class="fw-bold fs-5">페퍼저축은행</div>
                        </div>
                        <div class="phone-card">
                            <div class="text-muted">내 예금 총액</div>
                            <div class="amt">12,345,678원</div>
                        </div>
                        <div class="phone-card">
                            <div class="phone-menu">
                                <span><i class="bi bi-arrow-left-right"></i>이체</span>
                                <span><i class="bi bi-piggy-bank"></i>예금</span>
                                <span><i class="bi bi-cash-coin"></i>대출</span>
                                <span><i class="bi bi-three-dots"></i>전체</span>
                            </div>
                        </div>
                        <div class="phone-card text-muted">추천 상품 영역</div>
                    </div>
                </div>
            </div>
            <div class="card shadow-sm">
                <div class="card-header">처리 결과</div>
                <div class="card-body"><pre id="log" class="result-log mb-0">[메인 진입]을 눌러 테스트하세요.</pre></div>
            </div>
        </div>

        <!-- 우: ADMIN -->
        <div class="col-lg-7">
            <div class="card shadow-sm mb-4">
                <div class="card-header d-flex justify-content-between align-items-center">
                    <span class="fw-bold"><i class="bi bi-list-ol me-1"></i> 2. ADMIN - 배너팝업목록</span>
                    <button id="btnList" class="btn btn-sm btn-outline-primary">새로고침</button>
                </div>
                <div class="card-body p-0">
                    <div class="table-responsive">
                        <table class="table table-sm table-hover mb-0">
                            <thead class="table-light">
                            <tr><th>ID</th><th>공지유형</th><th>제목</th><th>채널</th><th>게시기간</th><th>순서</th><th>상태</th><th title="같은 채널에서 기간이 겹치는 사용중 배너 수">겹침</th><th>관리</th></tr>
                            </thead>
                            <tbody id="rows"><tr><td colspan="9" class="text-muted">-</td></tr></tbody>
                        </table>
                    </div>
                </div>
                <div class="card-footer small text-muted">
                    <b>겹침</b>: 기간이 겹쳐 고객 화면에 함께 슬라이드되는 다른 배너 수 /
                    <i class="bi bi-exclamation-triangle text-warning"></i> 겹치는 배너와 표시순서가 같음 (게시 시작일시가 최신인 배너가 먼저)
                </div>
            </div>

            <div class="card shadow-sm mb-4">
                <div class="card-header fw-bold d-flex justify-content-between align-items-center">
                    <span><i class="bi bi-pencil-square me-1"></i> 3. ADMIN - 배너팝업 <span id="formMode">등록</span></span>
                    <button id="btnNew" class="btn btn-sm btn-outline-secondary">새로 작성</button>
                </div>
                <div class="card-body">
                    <input type="hidden" id="fId">
                    <div class="row g-2">
                        <div class="col-md-4">
                            <label class="form-label small fw-bold" for="fCategory">공지유형 *</label>
                            <select id="fCategory" class="form-select form-select-sm"></select>
                        </div>
                        <div class="col-md-5">
                            <span class="form-label small fw-bold d-block">노출채널 *</span>
                            <div id="fChannels" class="pt-1"></div>
                        </div>
                        <div class="col-md-3">
                            <label class="form-label small fw-bold" for="fOrder">표시순서 *</label>
                            <input id="fOrder" type="number" min="1" max="999" class="form-control form-control-sm" value="1">
                        </div>
                        <div class="col-12">
                            <label class="form-label small fw-bold" for="fTitle">제목 * <span class="text-muted fw-normal">(50자)</span></label>
                            <input id="fTitle" maxlength="50" class="form-control form-control-sm">
                        </div>
                        <div class="col-12">
                            <label class="form-label small fw-bold" for="fContent">내용 * <span class="text-muted fw-normal">(1,000자)</span></label>
                            <textarea id="fContent" maxlength="1000" rows="3" class="form-control form-control-sm"></textarea>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label small fw-bold" for="fStart">게시 시작일시 *</label>
                            <input id="fStart" type="datetime-local" class="form-control form-control-sm">
                        </div>
                        <div class="col-md-6">
                            <label class="form-label small fw-bold" for="fEnd">게시 종료일시 * <span class="text-muted fw-normal">(미포함)</span></label>
                            <input id="fEnd" type="datetime-local" class="form-control form-control-sm">
                        </div>
                        <div class="col-md-6">
                            <label class="form-label small fw-bold" for="fLink">링크 URL</label>
                            <input id="fLink" class="form-control form-control-sm" placeholder="/board/list 또는 https://...">
                        </div>
                        <div class="col-md-6">
                            <label class="form-label small fw-bold" for="fImage">이미지 URL</label>
                            <input id="fImage" class="form-control form-control-sm" placeholder="이미지 등록 시 대체텍스트 필수">
                        </div>
                        <div class="col-md-9">
                            <label class="form-label small fw-bold" for="fAlt">대체텍스트 <span class="text-muted fw-normal">(100자)</span></label>
                            <input id="fAlt" maxlength="100" class="form-control form-control-sm">
                        </div>
                        <div class="col-md-3">
                            <label class="form-label small fw-bold" for="fUse">사용여부</label>
                            <select id="fUse" class="form-select form-select-sm"><option value="true">사용</option><option value="false">미사용</option></select>
                        </div>
                        <div class="col-12 text-end">
                            <button id="btnSave" class="btn btn-sm btn-warning fw-bold">저장</button>
                        </div>
                    </div>
                </div>
            </div>

            <div class="card shadow-sm">
                <div class="card-header fw-bold"><i class="bi bi-eye me-1"></i> 4. 노출 순서 (마지막 조회 결과)</div>
                <div class="card-body">
                    <ol id="previewList" class="preview-list mb-0 ps-3"><li class="text-muted">-</li></ol>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
(function () {
    var $ = function (id) { return document.getElementById(id); };
    function log(o) { $('log').textContent = typeof o === 'string' ? o : JSON.stringify(o, null, 2); }
    function channel() { return $('channel').value; }

    function api(url, opt) {
        opt = opt || {};
        opt.credentials = 'same-origin';
        opt.headers = Object.assign({ 'X-Requested-With': 'XMLHttpRequest' }, opt.headers || {});
        return fetch(url, opt).then(function (r) { return r.json(); });
    }
    function el(tag, cls, text) { var e = document.createElement(tag); if (cls) e.className = cls; if (text != null) e.textContent = text; return e; }
    function badge(text, cls) { return el('span', 'badge status-badge ' + cls, text); }
    function pad(n) { return ('0' + n).slice(-2); }
    function toLocalInput(d) { return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + 'T' + pad(d.getHours()) + ':' + pad(d.getMinutes()); }
    /** "yyyy-MM-dd HH:mm" → input datetime-local 값 */
    function toInput(s) { return s ? s.replace(' ', 'T') : ''; }

    var codes = { categories: {}, channels: {} };

    /** 노출 순서 목록 표시 */
    function showOrder(data) {
        var ol = $('previewList'); ol.innerHTML = '';
        if (!data.items.length) { ol.appendChild(el('li', 'text-muted', '노출 대상 없음 (' + data.channel + ', ' + data.baseAt + ')')); return; }
        data.items.forEach(function (b) {
            var li = el('li');
            li.appendChild(el('span', 'order-pill me-1', String(b.displayOrder)));
            li.appendChild(document.createTextNode(b.title + '  '));
            li.appendChild(el('span', 'text-muted small', b.startAt + ' ~ ' + b.endAt));
            ol.appendChild(li);
        });
        var info = el('li', 'list-unstyled small text-muted mt-1',
            '기준 ' + data.baseAt + ' / ' + data.total + '건' + (data.slide ? ' → 좌우 슬라이드' : '') +
            (data.truncated ? ' / 최대 노출 건수 초과로 ' + data.truncated + '건 제외' : ''));
        ol.appendChild(info);
    }

    var popupOpts = {
        container: '#phone',
        onLink: function (url) { log('[자세히 보기] 이동 URL: ' + url + '\n(샘플에서는 이동하지 않고 표시만 합니다)'); }
    };

    // 1) 고객 화면 - 업무 화면은 이 호출 하나면 된다
    $('btnOpen').addEventListener('click', function () {
        api('/api/common/banner-popups?channel=' + encodeURIComponent(channel())).then(function (res) {
            if (res.success) showOrder(res.data);
        });
        BannerPopup.open(Object.assign({ channel: channel() }, popupOpts))
            .then(function (r) {
                if (r.shown === 0) r.message = r.hiddenCount ? '오늘 하루 보지 않기로 숨긴 배너만 있음 (' + r.hiddenCount + '건)' : '노출할 배너 없음';
                log({ '팝업 결과': r, '오늘 하루 숨김 ID': BannerPopup.hiddenIds(channel()) });
            })
            .catch(log);
    });

    // 2) ADMIN 미리보기 - 기준시각의 노출 대상을 같은 컴포넌트로 표시 (숨김 무시)
    $('btnPreview').addEventListener('click', function () {
        var at = $('previewAt').value;
        api('/api/common/banner-admin/preview?channel=' + encodeURIComponent(channel()) + '&at=' + encodeURIComponent(at)).then(function (res) {
            if (!res.success) { log(res); return; }
            showOrder(res.data);
            BannerPopup.open(Object.assign({ channel: channel(), items: res.data.items, respectHidden: false }, popupOpts))
                .then(function (r) { log({ '미리보기 기준': res.data.baseAt, '팝업 결과': r }); });
        });
    });

    $('btnReset').addEventListener('click', function () {
        BannerPopup.resetHidden(channel());
        log(channel() + ' 채널 "오늘 하루 보지 않기" 를 초기화했습니다.');
    });

    // 3) ADMIN 목록
    function loadList() {
        api('/api/common/banner-admin/popups?popupType=BANNER_POPUP').then(function (res) {
            var tb = $('rows'); tb.innerHTML = '';
            if (!res.success) { var tr = el('tr'); var td = el('td', 'text-danger', res.code + ' ' + res.message); td.colSpan = 9; tr.appendChild(td); tb.appendChild(tr); return; }
            res.data.forEach(function (b) {
                var tr = el('tr', b.use ? '' : 'row-off');
                tr.appendChild(el('td', null, String(b.id)));
                var c = el('td'); c.appendChild(badge(b.categoryName, b.category.indexOf('EMERGENCY') === 0 ? 'bg-danger' : 'bg-secondary')); tr.appendChild(c);
                tr.appendChild(el('td', null, b.title));
                tr.appendChild(el('td', null, b.channels.join(', ')));
                tr.appendChild(el('td', 'text-nowrap', b.startAt + ' ~\n' + b.endAt)).style.whiteSpace = 'pre';
                var o = el('td'); o.appendChild(el('span', 'order-pill', String(b.displayOrder))); tr.appendChild(o);
                var s = el('td');
                s.appendChild(badge(b.status, { '게시중': 'bg-success', '게시예정': 'bg-info text-dark', '게시종료': 'bg-light text-dark', '미사용': 'bg-dark' }[b.status]));
                tr.appendChild(s);
                var ov = el('td', null, b.overlapCount ? String(b.overlapCount) : '-');
                if (b.orderConflict) { var w = el('i', 'bi bi-exclamation-triangle text-warning ms-1'); w.title = '겹치는 배너와 표시순서 같음'; ov.appendChild(w); }
                tr.appendChild(ov);
                var act = el('td', 'text-nowrap');
                var bEdit = el('button', 'btn btn-outline-primary btn-sm py-0 px-1 me-1', '수정');
                bEdit.addEventListener('click', function () { fillForm(b); });
                var bUse = el('button', 'btn btn-outline-secondary btn-sm py-0 px-1 me-1', b.use ? '중지' : '사용');
                bUse.addEventListener('click', function () {
                    api('/api/common/banner-admin/popups/' + b.id + '/use?use=' + (!b.use), { method: 'POST' }).then(function (r) { log(r); loadList(); });
                });
                var bDel = el('button', 'btn btn-outline-danger btn-sm py-0 px-1', '삭제');
                bDel.addEventListener('click', function () {
                    if (!confirm('[' + b.id + '] ' + b.title + '\n삭제하시겠습니까?')) return;
                    api('/api/common/banner-admin/popups/' + b.id, { method: 'DELETE' }).then(function (r) { log(r); loadList(); });
                });
                act.appendChild(bEdit); act.appendChild(bUse); act.appendChild(bDel);
                tr.appendChild(act);
                $('rows').appendChild(tr);
            });
        });
    }

    // 4) ADMIN 등록/수정 폼
    function resetForm() {
        $('fId').value = ''; $('formMode').textContent = '등록';
        $('fCategory').selectedIndex = 0; $('fOrder').value = 1; $('fTitle').value = ''; $('fContent').value = '';
        var now = new Date(); now.setMinutes(0, 0, 0);
        var end = new Date(now.getTime() + 3 * 86400000);
        $('fStart').value = toLocalInput(now); $('fEnd').value = toLocalInput(end);
        $('fLink').value = ''; $('fImage').value = ''; $('fAlt').value = ''; $('fUse').value = 'true';
        document.querySelectorAll('#fChannels input').forEach(function (cb) { cb.checked = cb.value === 'APP'; });
    }
    function fillForm(b) {
        $('fId').value = b.id; $('formMode').textContent = '수정 (ID ' + b.id + ')';
        $('fCategory').value = b.category; $('fOrder').value = b.displayOrder; $('fTitle').value = b.title; $('fContent').value = b.content;
        $('fStart').value = toInput(b.startAt); $('fEnd').value = toInput(b.endAt);
        $('fLink').value = b.linkUrl || ''; $('fImage').value = b.imageUrl || ''; $('fAlt').value = b.altText || ''; $('fUse').value = String(b.use);
        document.querySelectorAll('#fChannels input').forEach(function (cb) { cb.checked = b.channels.indexOf(cb.value) >= 0; });
        $('fTitle').focus();
    }
    $('btnNew').addEventListener('click', resetForm);
    $('btnSave').addEventListener('click', function () {
        var id = $('fId').value;
        var body = {
            popupType: 'BANNER_POPUP',
            category: $('fCategory').value,
            channels: Array.prototype.filter.call(document.querySelectorAll('#fChannels input'), function (cb) { return cb.checked; }).map(function (cb) { return cb.value; }),
            title: $('fTitle').value, content: $('fContent').value,
            startAt: $('fStart').value, endAt: $('fEnd').value,
            displayOrder: $('fOrder').value === '' ? null : Number($('fOrder').value),
            linkUrl: $('fLink').value, imageUrl: $('fImage').value, altText: $('fAlt').value,
            use: $('fUse').value === 'true'
        };
        api('/api/common/banner-admin/popups' + (id ? '/' + id : ''), {
            method: id ? 'PUT' : 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body)
        }).then(function (res) {
            log(res);
            if (res.success) { resetForm(); loadList(); }
        });
    });

    function loadCodes() {
        return api('/api/common/banner-admin/codes').then(function (res) {
            if (res.success) codes = res.data;
            else codes = { categories: { EMERGENCY_SYSTEM: '시스템 긴급공지', EMERGENCY_GENERAL: '일반 긴급공지', NOTICE: '일반 공지', EVENT: '이벤트·상품안내' },
                           channels: { APP: 'APP', PC_WEB: 'PC WEB', RECRUIT_WEB: '모집인WEB' } };
            var sel = $('fCategory'); sel.innerHTML = '';
            Object.keys(codes.categories).forEach(function (k) { var o = el('option', null, codes.categories[k]); o.value = k; sel.appendChild(o); });
            var box = $('fChannels'); box.innerHTML = '';
            Object.keys(codes.channels).forEach(function (k) {
                var w = el('div', 'form-check form-check-inline');
                var cb = el('input', 'form-check-input'); cb.type = 'checkbox'; cb.id = 'ch_' + k; cb.value = k;
                var lb = el('label', 'form-check-label small', codes.channels[k]); lb.htmlFor = cb.id;
                w.appendChild(cb); w.appendChild(lb); box.appendChild(w);
            });
        });
    }

    $('btnList').addEventListener('click', loadList);
    $('previewAt').value = toLocalInput(new Date());
    loadCodes().then(function () { resetForm(); loadList(); });
})();
</script>
</body>
</html>
