<%--
  =============================================================================
   [CF-BC-20 / 공통#44 샘플] 홈 - 상단슬라이드배너  -  GET /common/banner/top-slide
  -----------------------------------------------------------------------------
   좌: APP 홈 목업 - 상단슬라이드배너 (업무 화면은 top-slide-banner.js 포함 + TopSlideBanner.mount)
       계좌 보유 회원은 같은 영역에 계좌정보 (onIneligible)
   우: 테스트 도구 - 고객 전환(비로그인/계좌 미보유/계좌 보유), 노출 건수 1~5 시뮬레이션(빈 슬롯 숨김), 상태 확인
   배너 등록·수정은 공통#45 ADMIN (배너팝업목록 > 배너팝업구분 "상단슬라이드배너").
   주의: JSP 에서는 JS 템플릿 문자열의 달러+중괄호가 EL 로 해석되므로 문자열 더하기를 사용한다.
  =============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>저축은행 포털 - 상단슬라이드배너 샘플</title>
    <link rel="shortcut icon" href="/favicon.ico">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css" rel="stylesheet">
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <!-- 상단슬라이드배너 공통 -->
    <link rel="stylesheet" href="/css/common/ui-tokens.css">
    <link rel="stylesheet" href="/css/common/top-slide-banner.css">
    <script src="/js/common/top-slide-banner.js"></script>
    <style>
        body { background-color: #f4f6f9; font-family: 'Malgun Gothic', 'Noto Sans KR', sans-serif; }
        .card-header-financial { background-color: #0d47a1; color: white; font-weight: bold; }
        pre.result-log { background: #212529; color: #e9ecef; border-radius: 6px; padding: 12px; min-height: 80px; max-height: 320px; overflow: auto; white-space: pre-wrap; font-size: 12px; margin: 0; }
        [hidden] { display: none !important; }
        .phone { width: 100%; max-width: 360px; height: 640px; margin: 0 auto; border: 10px solid #1f2937; border-radius: 36px; background: #f5f7fb; overflow: auto; }
        .phone-top { background: #fff; padding: 16px 18px 10px; display: flex; justify-content: space-between; align-items: center; }
        .phone-top b { font-size: 17px; }
        .phone-tabs { display: flex; gap: 16px; padding: 0 18px; background: #fff; border-bottom: 1px solid #eef0f3; font-size: 14px; }
        .phone-tabs span { padding: 8px 0; color: #6b7280; }
        .phone-tabs span.on { color: #111827; font-weight: 700; border-bottom: 2px solid #111827; }
        .slot { padding: 12px 12px 0; }
        .acct { background: #fff; border-radius: 16px; padding: 16px 18px; box-shadow: 0 1px 4px rgba(0,0,0,.06); }
        .acct small { color: #6b7280; }
        .acct .amt { font-size: 22px; font-weight: 800; }
        .phone-sec { background: #fff; border-radius: 14px; margin: 12px; padding: 14px; font-size: 13px; color: #6b7280; }
        .phone-sec h6 { color: #111827; font-size: 14px; font-weight: 700; }
        .rule { font-size: 12px; }
        .rule td, .rule th { padding: 4px 6px; }
    </style>
</head>
<body>
<jsp:include page="/WEB-INF/views/common/navbar.jsp" />

<div class="container-xl pb-5">
    <div class="d-flex align-items-center mb-3">
        <a href="/common/test-hub" class="btn btn-sm btn-outline-secondary me-2"><i class="bi bi-arrow-left"></i> 테스트 목록</a>
        <h5 class="fw-bold mb-0">[CF-BC-20 / 공통#44] 홈 - 상단슬라이드배너</h5>
    </div>
    <div class="alert alert-info small py-2">
        <b>AS-IS</b> 메인 "슬라이드 배너팝업" (클릭 전환율 1.6%) → <b>TO-BE</b> 홈 상단에 상품소개 배너를 <b>2초 간격</b>으로 롤링 (최대 <b>5건</b>).
        <b>계좌 미보유</b> 사용자 대상이며, 계좌 보유 회원은 같은 영역에 계좌정보가 나옵니다. 배너 등록은 공통#45 ADMIN.
    </div>

    <div class="row g-4">
        <div class="col-lg-5">
            <div class="card shadow-sm">
                <div class="card-header card-header-financial"><i class="bi bi-phone me-1"></i> 1. APP 홈 (고객 화면)</div>
                <div class="card-body">
                    <div class="phone">
                        <div class="phone-top"><b>페퍼저축은행</b><i class="bi bi-bell"></i></div>
                        <div class="phone-tabs"><span class="on">홈</span><span>저축</span><span>대출</span></div>
                        <div class="slot">
                            <!-- ★ 홈 상단: 계좌 미보유 → 상단슬라이드배너 / 계좌 보유 → 계좌정보 (동일 영역) -->
                            <div id="topSlide"></div>
                            <div id="acctInfo" class="acct" hidden>
                                <small>입출금 · 페퍼스 파킹통장4</small>
                                <div class="amt">1,234,567원</div>
                                <small>계좌 보유 회원 - 상단슬라이드배너 대신 계좌정보 노출</small>
                            </div>
                        </div>
                        <div class="phone-sec"><h6>퀵메뉴</h6>퀵메뉴 영역 (공통#46)</div>
                        <div class="phone-sec"><h6>추천 상품</h6>추천 상품 영역 (공통#47)</div>
                    </div>
                </div>
            </div>
        </div>

        <div class="col-lg-7">
            <div class="card shadow-sm mb-4">
                <div class="card-header fw-bold"><i class="bi bi-person-gear me-1"></i> 2. 제공대상 (정책1)</div>
                <div class="card-body">
                    <div class="row g-2 align-items-end">
                        <div class="col-md-8">
                            <label class="form-label small fw-bold" for="customer">고객 (local 테스트 - 로그인 시 로그인 고객 우선)</label>
                            <select id="customer" class="form-select form-select-sm">
                                <option value="">비로그인 - 계좌 확인 불가 → 노출</option>
                                <option value="customer02">customer02 - 계좌 미보유 → 노출</option>
                                <option value="customer01">customer01 - 계좌 보유 → 미노출 (계좌정보)</option>
                            </select>
                        </div>
                        <div class="col-md-4 d-grid">
                            <button id="btnLoad" class="btn btn-sm btn-primary"><i class="bi bi-arrow-clockwise me-1"></i>홈 진입 (API 조회)</button>
                        </div>
                    </div>
                </div>
            </div>

            <div class="card shadow-sm mb-4">
                <div class="card-header fw-bold"><i class="bi bi-collection-play me-1"></i> 3. 노출 건수별 화면 (기능1·2 - 빈 슬롯 숨김)</div>
                <div class="card-body">
                    <div class="d-flex flex-wrap gap-2 mb-2" id="countBtns"></div>
                    <table class="table table-sm table-bordered rule mb-0">
                        <thead class="table-light"><tr><th>노출 건수</th><th>슬롯·인디케이터</th><th>자동 슬라이딩</th></tr></thead>
                        <tbody>
                        <tr><td>0건</td><td colspan="2">배너 영역 숨김 (자리 차지 안 함)</td></tr>
                        <tr><td>1건</td><td>슬롯 1개, 인디케이터 없음</td><td>없음 (고정)</td></tr>
                        <tr><td>2~4건</td><td>등록 건수만큼만 (빈 슬롯 숨김), "01/0N"</td><td>2초</td></tr>
                        <tr><td>5건 이상</td><td>표시순서 상위 5건, "01/05"</td><td>2초</td></tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <div class="card shadow-sm mb-4">
                <div class="card-header fw-bold"><i class="bi bi-activity me-1"></i> 4. 슬라이드 상태 (기능4)</div>
                <div class="card-body">
                    <div class="d-flex flex-wrap gap-2 mb-2">
                        <button id="btnPause" class="btn btn-sm btn-outline-secondary">일시정지 (API)</button>
                        <button id="btnPlay" class="btn btn-sm btn-outline-secondary">재생 (API)</button>
                    </div>
                    <div id="state" class="small font-monospace">-</div>
                    <p class="small text-muted mt-2 mb-0">배너 위에 마우스를 올리거나 키보드 포커스·터치 중에는 자동 넘김이 멈춥니다 (읽는 중 넘어가지 않게). ←/→ 키, 스와이프로 넘길 수 있습니다.</p>
                </div>
            </div>

            <div class="card shadow-sm">
                <div class="card-header">처리 결과 (API 응답 / 배너 터치)</div>
                <div class="card-body"><pre id="log" class="result-log">-</pre></div>
            </div>
        </div>
    </div>
</div>

<script>
(function () {
    var $ = function (id) { return document.getElementById(id); };
    function log(o) { $('log').textContent = typeof o === 'string' ? o : JSON.stringify(o, null, 2); }
    var last = null;
    var opts = {
        channel: 'APP',
        onLink: function (url, item) { log('[배너 터치] ' + item.mainText + '\n이동 URL: ' + url + '\n(샘플에서는 이동하지 않고 표시만 합니다)'); },
        onIneligible: function () { $('acctInfo').hidden = false; },
        onChange: function () { showState(); },
        onError: function (e) { log(e.response || String(e)); }
    };

    function showState() {
        var s = TopSlideBanner.state('#topSlide');
        $('state').textContent = s ? ('현재 ' + (s.index + 1) + ' / ' + s.total + ' · ' + (s.playing ? '자동 넘김 중' : '멈춤') + (s.userPaused ? ' (사용자 일시정지)' : '') + ' · 간격 ' + s.interval + 'ms') : '배너 없음';
    }

    // 업무 화면은 이 호출 하나면 된다 (params 는 local 샘플 전용)
    function load() {
        $('acctInfo').hidden = true;
        var c = $('customer').value;
        return TopSlideBanner.mount('#topSlide', Object.assign({}, opts, { params: c ? { demoCustomer: c } : {} }))
            .then(function (data) { if (data) { last = data; log(data); buildCountBtns(); } showState(); });
    }

    // 노출 건수 시뮬레이션: 서버 응답을 n건으로 잘라 같은 컴포넌트로 렌더
    function buildCountBtns() {
        var box = $('countBtns'); box.innerHTML = '';
        if (!last || !last.eligible) { box.textContent = '계좌 보유 고객은 배너가 없습니다.'; return; }
        for (var n = 0; n <= last.items.length; n++) {
            (function (k) {
                var b = document.createElement('button');
                b.className = 'btn btn-sm ' + (k === last.items.length ? 'btn-primary' : 'btn-outline-primary');
                b.textContent = k + '건';
                b.addEventListener('click', function () {
                    var d = Object.assign({}, last, { items: last.items.slice(0, k), total: k });
                    TopSlideBanner.render('#topSlide', d, opts);
                    box.querySelectorAll('button').forEach(function (x) { x.className = 'btn btn-sm btn-outline-primary'; });
                    b.className = 'btn btn-sm btn-primary';
                    showState();
                });
                box.appendChild(b);
            })(n);
        }
    }

    $('btnLoad').addEventListener('click', load);
    $('customer').addEventListener('change', load);
    $('btnPause').addEventListener('click', function () { TopSlideBanner.pause('#topSlide'); showState(); });
    $('btnPlay').addEventListener('click', function () { TopSlideBanner.play('#topSlide'); showState(); });
    setInterval(showState, 500);
    load();
})();
</script>
</body>
</html>
