<%--
  =============================================================================
   [CF-BC-20 / 공통#45] 디지털어드민 - 마케팅 > 배너관리 > 배너팝업목록  -  GET /common/banner/admin
  -----------------------------------------------------------------------------
   기존 "배너팝업목록" 화면을 활용해 항목을 추가한 형태 (상단슬라이드배너 APP 개발에 따른 ADMIN 수정개발)
     1. 배너팝업구분 "상단슬라이드배너" 추가          4. 링크 URL 입력
     2. 이미지 파일선택 (업로드 → URL 자동 입력)       5. 배경 색상 선택 (글자색 자동 대비 미리보기)
     3. 이미지 설명 입력 (대체텍스트)                  6. 상품소개 메인텍스트 · 서브텍스트 (글자 수 제한)
   ADMIN API 는 local(common.banner-popup.admin-api-open=true) 또는 ROLE_ADMIN 로그인 시 동작한다.
   주의: JSP 에서는 JS 템플릿 문자열의 달러+중괄호가 EL 로 해석되므로 문자열 더하기를 사용한다.
  =============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>디지털어드민 - 배너팝업목록</title>
    <link rel="shortcut icon" href="/favicon.ico">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css" rel="stylesheet">
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <!-- 미리보기: 고객 화면과 같은 상단슬라이드배너 컴포넌트 -->
    <link rel="stylesheet" href="/css/common/ui-tokens.css">
    <link rel="stylesheet" href="/css/common/top-slide-banner.css">
    <script src="/js/common/top-slide-banner.js"></script>
    <style>
        body { background-color: #f4f6f9; font-family: 'Malgun Gothic', 'Noto Sans KR', sans-serif; }
        [hidden] { display: none !important; }
        .table-sm td, .table-sm th { font-size: 12px; vertical-align: middle; }
        tr.row-off td { color: #9ca3af; }
        .order-pill { display: inline-block; min-width: 22px; text-align: center; border-radius: 11px; background: #e3ecfa; color: #0d47a1; font-weight: 700; }
        .swatch-cell { display: inline-block; width: 14px; height: 14px; border-radius: 3px; border: 1px solid #d1d5db; vertical-align: -2px; margin-right: 4px; }
        .thumb { width: 40px; height: 40px; object-fit: contain; border: 1px solid #e5e7eb; border-radius: 6px; background: #fff; }
        .counter { font-size: 11px; color: #6b7280; }
        .counter.over { color: #c62828; font-weight: 700; }
        .swatches { display: flex; flex-wrap: wrap; gap: 6px; }
        .swatches button { width: 26px; height: 26px; border-radius: 6px; border: 1px solid #d1d5db; padding: 0; }
        .swatches button[aria-pressed="true"] { outline: 2px solid #1d70e8; outline-offset: 2px; }
        .img-box { display: flex; align-items: center; gap: 10px; padding: 8px; border: 1px dashed #c4cad4; border-radius: 8px; background: #fff; min-height: 64px; }
        .img-box img { width: 56px; height: 56px; object-fit: contain; }
        .preview-phone { max-width: 360px; background: #f5f7fb; border-radius: 16px; padding: 12px; border: 1px solid #e5e7eb; }
        .contrast { font-size: 12px; }
        .type-tabs .nav-link { font-size: 13px; padding: .35rem .75rem; }
        pre.result-log { background: #212529; color: #e9ecef; border-radius: 6px; padding: 10px; max-height: 220px; overflow: auto; white-space: pre-wrap; font-size: 12px; margin: 0; }
    </style>
</head>
<body>
<jsp:include page="/WEB-INF/views/common/navbar.jsp" />

<div class="container-xl pb-5">
    <div class="d-flex align-items-center mb-2">
        <a href="/common/test-hub" class="btn btn-sm btn-outline-secondary me-2"><i class="bi bi-arrow-left"></i> 테스트 목록</a>
        <h5 class="fw-bold mb-0">[CF-BC-20 / 공통#45] 마케팅 &gt; 배너관리 &gt; 배너팝업목록</h5>
    </div>
    <div class="alert alert-info small py-2">
        기존 배너팝업목록 화면에 <b>배너팝업구분 "상단슬라이드배너"</b>를 추가했습니다. 구분을 상단슬라이드배너로 고르면
        <b>상품소개 메인텍스트·서브텍스트(글자 수 제한)</b>, <b>이미지 파일선택</b>, <b>대체텍스트</b>, <b>링크 URL</b>, <b>배경 색상</b> 입력란이 나타납니다.
        고객 화면 노출 결과는 <a href="/common/banner/top-slide">공통#44 샘플</a>에서 확인할 수 있습니다.
    </div>

    <!-- 목록 -->
    <div class="card shadow-sm mb-4">
        <div class="card-header d-flex flex-wrap gap-2 justify-content-between align-items-center">
            <ul class="nav nav-pills type-tabs" id="typeTabs">
                <li class="nav-item"><button class="nav-link" data-type="">전체</button></li>
                <li class="nav-item"><button class="nav-link" data-type="BANNER_POPUP">배너팝업</button></li>
                <li class="nav-item"><button class="nav-link active" data-type="TOP_SLIDE">상단슬라이드배너</button></li>
            </ul>
            <div>
                <button id="btnNew" class="btn btn-sm btn-warning fw-bold"><i class="bi bi-plus-lg"></i> 등록</button>
                <button id="btnList" class="btn btn-sm btn-outline-primary">새로고침</button>
            </div>
        </div>
        <div class="card-body p-0">
            <div class="table-responsive">
                <table class="table table-sm table-hover mb-0">
                    <thead class="table-light">
                    <tr><th>ID</th><th>구분</th><th>이미지</th><th>제목 / 메인텍스트</th><th>배경</th><th>채널</th><th>게시기간</th><th>순서</th><th>상태</th><th>노출</th><th>관리</th></tr>
                    </thead>
                    <tbody id="rows"><tr><td colspan="11" class="text-muted">-</td></tr></tbody>
                </table>
            </div>
        </div>
        <div class="card-footer small text-muted">
            <b>노출</b>: 상단슬라이드배너 게시중 건의 현재 고객 화면 순번 (최대 <span id="limitMax">5</span>건).
            <span class="text-danger"><i class="bi bi-eye-slash"></i> 초과</span> = 게시중이지만 표시순서가 밀려 지금은 보이지 않음.
        </div>
    </div>

    <div class="row g-4">
        <!-- 등록/수정 -->
        <div class="col-lg-7">
            <div class="card shadow-sm" id="formCard">
                <div class="card-header fw-bold d-flex justify-content-between align-items-center">
                    <span><i class="bi bi-pencil-square me-1"></i> 배너팝업 <span id="formMode">등록</span></span>
                    <button id="btnReset" class="btn btn-sm btn-outline-secondary">새로 작성</button>
                </div>
                <div class="card-body">
                    <input type="hidden" id="fId">
                    <div class="row g-2">
                        <div class="col-md-4">
                            <label class="form-label small fw-bold" for="fType">배너팝업구분 *</label>
                            <select id="fType" class="form-select form-select-sm"></select>
                        </div>
                        <div class="col-md-4" data-for="BANNER_POPUP">
                            <label class="form-label small fw-bold" for="fCategory">공지유형 *</label>
                            <select id="fCategory" class="form-select form-select-sm"></select>
                        </div>
                        <div class="col-md-4">
                            <span class="form-label small fw-bold d-block">노출채널 *</span>
                            <div id="fChannels" class="pt-1"></div>
                        </div>

                        <!-- 6. 상품소개 메인텍스트 · 서브텍스트 (상단슬라이드배너) / 제목 · 내용 (배너팝업) -->
                        <div class="col-12">
                            <label class="form-label small fw-bold" for="fTitle"><span id="lblTitle">상품소개 메인텍스트</span> *
                                <span class="counter" id="cTitle"></span></label>
                            <input id="fTitle" class="form-control form-control-sm" autocomplete="off">
                        </div>
                        <div class="col-12">
                            <label class="form-label small fw-bold" for="fContent"><span id="lblContent">서브텍스트</span> *
                                <span class="counter" id="cContent"></span></label>
                            <textarea id="fContent" rows="2" class="form-control form-control-sm"></textarea>
                        </div>

                        <!-- 2. 이미지 파일선택 · 3. 대체텍스트 -->
                        <div class="col-md-7">
                            <span class="form-label small fw-bold d-block">이미지</span>
                            <div class="img-box">
                                <img id="imgPreview" alt="" hidden>
                                <div class="flex-grow-1 small">
                                    <input id="fFile" type="file" class="form-control form-control-sm" accept="image/png,image/jpeg,image/gif,image/webp" aria-describedby="imgHelp">
                                    <div id="imgHelp" class="text-muted mt-1">PNG·JPG·GIF·WEBP, 최대 <span id="imgMax">2MB</span> · 권장 정사각 144×144px 이상 (SVG 불가)</div>
                                    <div id="imgInfo" class="text-success"></div>
                                </div>
                                <button id="btnImgClear" class="btn btn-sm btn-outline-danger" type="button" hidden>삭제</button>
                            </div>
                            <input type="hidden" id="fImage">
                        </div>
                        <div class="col-md-5">
                            <label class="form-label small fw-bold" for="fAlt">이미지 설명 (대체텍스트) <span id="altReq" class="text-danger" hidden>*</span>
                                <span class="counter" id="cAlt"></span></label>
                            <input id="fAlt" class="form-control form-control-sm" placeholder="예: 대출 카드와 동전 그림">
                            <div class="form-text">화면낭독기가 읽는 설명입니다. 이미지가 있으면 필수.</div>
                        </div>

                        <!-- 4. 링크 URL -->
                        <div class="col-md-7">
                            <label class="form-label small fw-bold" for="fLink">링크 URL</label>
                            <input id="fLink" class="form-control form-control-sm" placeholder="/product/loan/PS_SMART 또는 https://...">
                            <div class="form-text">배너 터치 시 이동. 같은 사이트 경로(/...) 또는 http(s) 주소만 가능.</div>
                        </div>

                        <!-- 5. 배경 색상 -->
                        <div class="col-md-5" data-for="TOP_SLIDE">
                            <label class="form-label small fw-bold" for="fBgHex">배경 색상</label>
                            <div class="d-flex gap-2 align-items-center mb-1">
                                <input id="fBgPicker" type="color" class="form-control form-control-color form-control-sm" aria-label="배경 색상 선택" value="#E3ECFA">
                                <input id="fBgHex" class="form-control form-control-sm font-monospace" maxlength="7" style="max-width:110px" value="#E3ECFA">
                            </div>
                            <div class="swatches" id="swatches" aria-label="추천 배경색"></div>
                            <div id="contrast" class="contrast mt-1"></div>
                        </div>

                        <div class="col-md-6">
                            <label class="form-label small fw-bold" for="fStart">배너노출 시작일시 * <span class="text-muted fw-normal">(포함)</span></label>
                            <input id="fStart" type="datetime-local" class="form-control form-control-sm">
                        </div>
                        <div class="col-md-6">
                            <label class="form-label small fw-bold" for="fEnd">배너노출 종료일시 * <span class="text-muted fw-normal">(미포함)</span></label>
                            <input id="fEnd" type="datetime-local" class="form-control form-control-sm">
                        </div>
                        <div class="col-md-3">
                            <label class="form-label small fw-bold" for="fOrder">표시순서 *</label>
                            <input id="fOrder" type="number" min="1" max="999" class="form-control form-control-sm" value="1">
                        </div>
                        <div class="col-md-3">
                            <label class="form-label small fw-bold" for="fUse">사용여부</label>
                            <select id="fUse" class="form-select form-select-sm"><option value="true">사용</option><option value="false">미사용</option></select>
                        </div>
                        <div class="col-md-6 d-flex align-items-end justify-content-end">
                            <button id="btnSave" class="btn btn-sm btn-primary fw-bold px-4">저장</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- 미리보기 -->
        <div class="col-lg-5">
            <div class="card shadow-sm mb-4" data-for="TOP_SLIDE">
                <div class="card-header fw-bold"><i class="bi bi-phone me-1"></i> 입력 중인 배너 미리보기 (APP 360px)</div>
                <div class="card-body">
                    <div class="preview-phone"><div id="livePreview"></div></div>
                    <p class="small text-muted mt-2 mb-0">글자색은 배경색에 맞춰 자동으로 정해집니다 (대비 4.5:1 이상).</p>
                </div>
            </div>
            <div class="card shadow-sm mb-4">
                <div class="card-header fw-bold"><i class="bi bi-clock-history me-1"></i> 기준시각 고객 화면 미리보기 (상단슬라이드배너)</div>
                <div class="card-body">
                    <div class="d-flex gap-2 mb-2">
                        <input id="previewAt" type="datetime-local" class="form-control form-control-sm">
                        <button id="btnPreview" class="btn btn-sm btn-outline-primary text-nowrap">미리보기</button>
                    </div>
                    <div class="preview-phone"><div id="atPreview"></div><div id="atEmpty" class="small text-muted text-center py-3" hidden>노출할 배너가 없습니다.</div></div>
                    <div id="atInfo" class="small text-muted mt-2"></div>
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
    function pad(n) { return ('0' + n).slice(-2); }
    function toLocalInput(d) { return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + 'T' + pad(d.getHours()) + ':' + pad(d.getMinutes()); }
    function api(url, opt) {
        opt = opt || {};
        opt.credentials = 'same-origin';
        opt.headers = Object.assign({ 'X-Requested-With': 'XMLHttpRequest' }, opt.headers || {});
        return fetch(url, opt).then(function (r) { return r.json(); });
    }
    var HEX = /^#[0-9A-Fa-f]{6}$/;
    var codes = null, limits = {}, listType = 'TOP_SLIDE';
    var PRESETS = ['#E3ECFA', '#0D47A1', '#FFF4E0', '#E6F4EA', '#FDECEA', '#F3F4F6', '#FFFFFF', '#111827'];

    // ---------------------------------------------------------------- 대비 (서버 BannerSupport.readableTextColor 와 같은 규칙)
    function lum(hex) {
        var n = parseInt(hex.slice(1), 16);
        return [(n >> 16) & 255, (n >> 8) & 255, n & 255].map(function (c) {
            c /= 255; return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
        }).reduce(function (a, v, i) { return a + v * [0.2126, 0.7152, 0.0722][i]; }, 0);
    }
    function ratio(a, b) { var x = lum(a), y = lum(b); return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05); }
    function textColor(bg) {
        if (!HEX.test(bg)) return '#111827';
        var d = ratio(bg, '#111827'), w = ratio(bg, '#FFFFFF');
        if (d >= 4.5 && d >= w) return '#111827';
        return w >= ratio(bg, '#000000') ? '#FFFFFF' : '#000000';
    }

    // ---------------------------------------------------------------- 구분별 입력란
    function type() { return $('fType').value; }
    function applyType() {
        var t = type();
        document.querySelectorAll('[data-for]').forEach(function (n) { n.hidden = n.getAttribute('data-for') !== t; });
        var slide = t === 'TOP_SLIDE';
        $('lblTitle').textContent = slide ? '상품소개 메인텍스트' : '제목';
        $('lblContent').textContent = slide ? '서브텍스트' : '내용';
        $('fContent').rows = slide ? 2 : 4;
        $('fTitle').maxLength = Number(slide ? limits.mainTextMax : limits.titleMax);
        $('fContent').maxLength = Number(slide ? limits.subTextMax : limits.contentMax);
        counters();
        livePreview();
    }

    // 6. 글자 수 제한 - 실시간 카운터 (서버도 같은 값으로 검증)
    function counter(input, out, max) {
        var n = input.value.trim().length;
        out.textContent = n + ' / ' + max + '자';
        out.classList.toggle('over', n > max);
    }
    function counters() {
        var slide = type() === 'TOP_SLIDE';
        counter($('fTitle'), $('cTitle'), Number(slide ? limits.mainTextMax : limits.titleMax));
        counter($('fContent'), $('cContent'), Number(slide ? limits.subTextMax : limits.contentMax));
        counter($('fAlt'), $('cAlt'), Number(limits.altMax));
        $('altReq').hidden = !$('fImage').value;
    }

    // 5. 배경 색상
    function setBg(hex) {
        hex = (hex || '').toUpperCase();
        if (!HEX.test(hex)) { $('contrast').innerHTML = ''; $('contrast').appendChild(el('span', 'text-danger', '#RRGGBB 형식으로 입력하세요')); return; }
        $('fBgHex').value = hex; $('fBgPicker').value = hex;
        document.querySelectorAll('#swatches button').forEach(function (b) { b.setAttribute('aria-pressed', String(b.getAttribute('data-color') === hex)); });
        var fg = textColor(hex);
        var c = $('contrast'); c.textContent = '';
        c.appendChild(document.createTextNode('글자색 자동: '));
        c.appendChild(el('b', null, fg === '#FFFFFF' ? '흰색' : (fg === '#000000' ? '검정' : '진회색')));
        c.appendChild(document.createTextNode(' · 대비 ' + ratio(hex, fg).toFixed(1) + ':1 '));
        c.appendChild(el('span', 'badge bg-success', '적합'));
        livePreview();
    }

    // 2. 이미지 파일선택 → 업로드 → URL 자동 입력
    function setImage(url, info) {
        $('fImage').value = url || '';
        $('imgPreview').hidden = !url;
        if (url) { $('imgPreview').src = url; } else { $('imgPreview').removeAttribute('src'); }
        $('btnImgClear').hidden = !url;
        $('imgInfo').textContent = info || '';
        counters(); livePreview();
    }
    $('fFile').addEventListener('change', function () {
        var f = $('fFile').files[0];
        if (!f) return;
        var max = Number(limits.imageMaxBytes);
        if (f.size > max) { log('이미지 용량 초과: ' + Math.round(f.size / 1024) + 'KB (최대 ' + Math.round(max / 1024) + 'KB)'); $('fFile').value = ''; return; }
        var fd = new FormData(); fd.append('file', f);
        $('imgInfo').textContent = '업로드 중...';
        api('/api/common/banner-admin/images', { method: 'POST', body: fd }).then(function (res) {
            log(res);
            $('fFile').value = '';
            if (!res.success) { $('imgInfo').textContent = ''; return; }
            var d = res.data;
            setImage(d.url, '등록됨 · ' + Math.max(1, Math.round(d.bytes / 1024)) + 'KB' + (d.width ? ' · ' + d.width + '×' + d.height + 'px' : ''));
            if (!$('fAlt').value) $('fAlt').focus();
        });
    });
    $('btnImgClear').addEventListener('click', function () { setImage('', ''); $('fAlt').value = ''; counters(); });

    // 입력 중 미리보기 (고객 화면과 같은 컴포넌트)
    function livePreview() {
        if (type() !== 'TOP_SLIDE') return;
        var bg = HEX.test($('fBgHex').value) ? $('fBgHex').value.toUpperCase() : (limits.defaultBackgroundColor || '#E3ECFA');
        var item = {
            id: 0, mainText: $('fTitle').value.trim() || '상품소개 메인텍스트', subText: $('fContent').value.trim() || '서브텍스트',
            imageUrl: $('fImage').value || null, altText: $('fAlt').value, linkUrl: null,
            backgroundColor: bg, textColor: textColor(bg), displayOrder: 1
        };
        TopSlideBanner.render('#livePreview', { eligible: true, items: [item], total: 1, intervalMs: 2000 }, { autoplay: false });
    }
    ['fTitle', 'fContent', 'fAlt'].forEach(function (id) { $(id).addEventListener('input', function () { counters(); livePreview(); }); });
    $('fBgPicker').addEventListener('input', function () { setBg($('fBgPicker').value); });
    $('fBgHex').addEventListener('input', function () { if (HEX.test($('fBgHex').value)) setBg($('fBgHex').value); });
    $('fBgHex').addEventListener('change', function () { setBg($('fBgHex').value); });
    $('fType').addEventListener('change', applyType);

    // ---------------------------------------------------------------- 목록
    function loadList() {
        document.querySelectorAll('#typeTabs .nav-link').forEach(function (b) { b.classList.toggle('active', b.getAttribute('data-type') === listType); });
        return api('/api/common/banner-admin/popups' + (listType ? '?popupType=' + listType : '')).then(function (res) {
            var tb = $('rows'); tb.innerHTML = '';
            if (!res.success) { var tr0 = el('tr'); var td0 = el('td', 'text-danger', res.code + ' ' + res.message); td0.colSpan = 11; tr0.appendChild(td0); tb.appendChild(tr0); return; }
            if (!res.data.length) { var tr1 = el('tr'); var td1 = el('td', 'text-muted', '등록된 배너가 없습니다.'); td1.colSpan = 11; tr1.appendChild(td1); tb.appendChild(tr1); return; }
            res.data.forEach(function (b) {
                var tr = el('tr', b.use ? '' : 'row-off');
                tr.appendChild(el('td', null, String(b.id)));
                var t = el('td'); t.appendChild(el('span', 'badge ' + (b.popupType === 'TOP_SLIDE' ? 'bg-primary' : 'bg-secondary'), b.popupTypeName)); tr.appendChild(t);
                var im = el('td');
                if (b.imageUrl) { var i = el('img', 'thumb'); i.src = b.imageUrl; i.alt = b.altText || ''; im.appendChild(i); } else { im.textContent = '-'; }
                tr.appendChild(im);
                var ti = el('td'); ti.appendChild(el('div', 'fw-bold', b.title)); ti.appendChild(el('div', 'text-muted', b.content.length > 40 ? b.content.slice(0, 40) + '…' : b.content)); tr.appendChild(ti);
                var bg = el('td', 'text-nowrap');
                if (b.backgroundColor) { var sw = el('span', 'swatch-cell'); sw.style.backgroundColor = b.backgroundColor; bg.appendChild(sw); bg.appendChild(document.createTextNode(b.backgroundColor)); } else { bg.textContent = '-'; }
                tr.appendChild(bg);
                tr.appendChild(el('td', null, b.channels.join(', ')));
                var pd = el('td', 'text-nowrap', b.startAt + ' ~\n' + b.endAt); pd.style.whiteSpace = 'pre'; tr.appendChild(pd);
                var o = el('td'); o.appendChild(el('span', 'order-pill', String(b.displayOrder))); tr.appendChild(o);
                var s = el('td'); s.appendChild(el('span', 'badge ' + ({ '게시중': 'bg-success', '게시예정': 'bg-info text-dark', '게시종료': 'bg-light text-dark', '미사용': 'bg-dark' }[b.status]), b.status)); tr.appendChild(s);
                var ex = el('td', 'text-nowrap');
                if (b.popupType === 'TOP_SLIDE' && b.displayRank) {
                    if (b.overLimit) { var w = el('span', 'text-danger fw-bold'); w.appendChild(el('i', 'bi bi-eye-slash me-1')); w.appendChild(document.createTextNode('초과 (' + b.displayRank + '번째)')); w.title = '최대 ' + limits.topSlideMaxCount + '건만 노출 - 표시순서를 앞당기거나 다른 배너를 미사용 처리하세요'; ex.appendChild(w); }
                    else ex.appendChild(el('span', 'text-success fw-bold', b.displayRank + ' / ' + limits.topSlideMaxCount));
                } else { ex.textContent = '-'; }
                tr.appendChild(ex);
                var act = el('td', 'text-nowrap');
                var bE = el('button', 'btn btn-outline-primary btn-sm py-0 px-1 me-1', '수정'); bE.addEventListener('click', function () { fillForm(b); });
                var bU = el('button', 'btn btn-outline-secondary btn-sm py-0 px-1 me-1', b.use ? '중지' : '사용');
                bU.addEventListener('click', function () { api('/api/common/banner-admin/popups/' + b.id + '/use?use=' + (!b.use), { method: 'POST' }).then(function (r) { log(r); loadList(); preview(); }); });
                var bD = el('button', 'btn btn-outline-danger btn-sm py-0 px-1', '삭제');
                bD.addEventListener('click', function () {
                    if (!confirm('[' + b.id + '] ' + b.title + '\n삭제하시겠습니까?')) return;
                    api('/api/common/banner-admin/popups/' + b.id, { method: 'DELETE' }).then(function (r) { log(r); loadList(); preview(); });
                });
                act.appendChild(bE); act.appendChild(bU); act.appendChild(bD);
                tr.appendChild(act);
                tb.appendChild(tr);
            });
        });
    }
    document.querySelectorAll('#typeTabs .nav-link').forEach(function (b) {
        b.addEventListener('click', function () { listType = b.getAttribute('data-type'); loadList(); });
    });

    // ---------------------------------------------------------------- 폼
    function resetForm() {
        $('fId').value = ''; $('formMode').textContent = '등록';
        $('fType').value = listType || 'TOP_SLIDE';
        $('fCategory').value = 'NOTICE';
        $('fTitle').value = ''; $('fContent').value = ''; $('fAlt').value = ''; $('fLink').value = '';
        var now = new Date(); now.setHours(0, 0, 0, 0);
        $('fStart').value = toLocalInput(now); $('fEnd').value = toLocalInput(new Date(now.getTime() + 14 * 86400000));
        $('fOrder').value = 1; $('fUse').value = 'true';
        document.querySelectorAll('#fChannels input').forEach(function (cb) { cb.checked = cb.value === 'APP'; });
        setImage('', '');
        setBg(limits.defaultBackgroundColor || '#E3ECFA');
        applyType();
    }
    function fillForm(b) {
        $('fId').value = b.id; $('formMode').textContent = '수정 (ID ' + b.id + ')';
        $('fType').value = b.popupType; $('fCategory').value = b.category;
        $('fTitle').value = b.title; $('fContent').value = b.content; $('fAlt').value = b.altText || ''; $('fLink').value = b.linkUrl || '';
        $('fStart').value = b.startAt.replace(' ', 'T'); $('fEnd').value = b.endAt.replace(' ', 'T');
        $('fOrder').value = b.displayOrder; $('fUse').value = String(b.use);
        document.querySelectorAll('#fChannels input').forEach(function (cb) { cb.checked = b.channels.indexOf(cb.value) >= 0; });
        setImage(b.imageUrl || '', b.imageUrl ? '등록된 이미지' : '');
        setBg(b.backgroundColor || limits.defaultBackgroundColor || '#E3ECFA');
        applyType();
        $('formCard').scrollIntoView({ behavior: 'smooth', block: 'start' });
        $('fTitle').focus({ preventScroll: true });
    }
    $('btnNew').addEventListener('click', function () { resetForm(); $('formCard').scrollIntoView({ behavior: 'smooth' }); $('fTitle').focus({ preventScroll: true }); });
    $('btnReset').addEventListener('click', resetForm);

    $('btnSave').addEventListener('click', function () {
        var id = $('fId').value, slide = type() === 'TOP_SLIDE';
        var body = {
            popupType: type(),
            category: slide ? null : $('fCategory').value,
            channels: Array.prototype.filter.call(document.querySelectorAll('#fChannels input'), function (cb) { return cb.checked; }).map(function (cb) { return cb.value; }),
            title: $('fTitle').value, content: $('fContent').value,
            imageUrl: $('fImage').value, altText: $('fAlt').value, linkUrl: $('fLink').value,
            backgroundColor: slide ? $('fBgHex').value : null,
            startAt: $('fStart').value, endAt: $('fEnd').value,
            displayOrder: $('fOrder').value === '' ? null : Number($('fOrder').value),
            use: $('fUse').value === 'true'
        };
        api('/api/common/banner-admin/popups' + (id ? '/' + id : ''), {
            method: id ? 'PUT' : 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body)
        }).then(function (res) {
            log(res);
            if (res.success) { listType = res.data.popupType; resetForm(); loadList(); preview(); }
        });
    });

    // ---------------------------------------------------------------- 기준시각 미리보기
    function preview() {
        var at = $('previewAt').value;
        return api('/api/common/banner-admin/top-slide-preview?channel=APP&at=' + encodeURIComponent(at)).then(function (res) {
            if (!res.success) { log(res); return; }
            var d = res.data;
            TopSlideBanner.render('#atPreview', d, { onLink: function (url) { log('[배너 터치] ' + url); } });
            $('atEmpty').hidden = d.total > 0;
            $('atInfo').textContent = '기준 ' + d.baseAt + ' · 노출 ' + d.total + '건' + (d.truncated ? ' (최대 ' + d.maxCount + '건 초과로 ' + d.truncated + '건 제외)' : '') + ' · ' + (d.intervalMs / 1000) + '초 간격';
        });
    }
    $('btnPreview').addEventListener('click', preview);
    $('btnList').addEventListener('click', loadList);

    // ---------------------------------------------------------------- 초기화
    api('/api/common/banner-admin/codes').then(function (res) {
        if (!res.success) { log(res); return; }
        codes = res.data; limits = codes.limits || {};
        $('limitMax').textContent = limits.topSlideMaxCount;
        $('imgMax').textContent = Math.round(Number(limits.imageMaxBytes) / 1024 / 1024 * 10) / 10 + 'MB';
        var ft = $('fType');
        Object.keys(codes.popupTypes).forEach(function (k) { var o = el('option', null, codes.popupTypes[k]); o.value = k; ft.appendChild(o); });
        var fc = $('fCategory');
        Object.keys(codes.categories).forEach(function (k) { var o = el('option', null, codes.categories[k]); o.value = k; fc.appendChild(o); });
        var box = $('fChannels');
        Object.keys(codes.channels).forEach(function (k) {
            var w = el('div', 'form-check form-check-inline');
            var cb = el('input', 'form-check-input'); cb.type = 'checkbox'; cb.id = 'ch_' + k; cb.value = k;
            var lb = el('label', 'form-check-label small', codes.channels[k]); lb.htmlFor = cb.id;
            w.appendChild(cb); w.appendChild(lb); box.appendChild(w);
        });
        PRESETS.forEach(function (c) {
            var b = el('button'); b.type = 'button'; b.style.backgroundColor = c; b.setAttribute('data-color', c);
            b.setAttribute('aria-label', '배경색 ' + c); b.title = c;
            b.addEventListener('click', function () { setBg(c); });
            $('swatches').appendChild(b);
        });
        $('previewAt').value = toLocalInput(new Date());
        resetForm(); loadList(); preview();
    });
})();
</script>
</body>
</html>
