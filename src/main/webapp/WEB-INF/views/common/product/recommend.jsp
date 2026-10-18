<%--
  =============================================================================
   [공통#47 샘플] 홈 - 추천 상품  -  GET /common/product/recommend
  -----------------------------------------------------------------------------
   좌: APP 홈 목업 - 추천 상품 (업무 화면은 recommend-products.js 포함 + RecommendProducts.mount)
   우: 회원구분 전환(local) - 금융회원이면 영역 전체 숨김 / 노출 순서는 공통#48 ADMIN 에서 설정
   주의: JSP 에서는 JS 템플릿 문자열의 달러+중괄호가 EL 로 해석되므로 문자열 더하기를 사용한다.
  =============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>저축은행 포털 - 추천 상품 샘플</title>
    <link rel="shortcut icon" href="/favicon.ico">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css" rel="stylesheet">
    <link rel="stylesheet" href="/css/common/ui-tokens.css">
    <link rel="stylesheet" href="/css/common/ui-icon.css">
    <link rel="stylesheet" href="/css/common/quick-menu.css">
    <link rel="stylesheet" href="/css/common/recommend-products.css">
    <script src="/js/common/quick-menu.js"></script>
    <script src="/js/common/recommend-products.js"></script>
    <style>
        body { background-color: #f4f6f9; font-family: 'Malgun Gothic', 'Noto Sans KR', sans-serif; }
        [hidden] { display: none !important; }
        pre.result-log { background: #212529; color: #e9ecef; border-radius: 6px; padding: 10px; max-height: 360px; overflow: auto; white-space: pre-wrap; font-size: 12px; margin: 0; }
        .phone { width: 100%; max-width: 360px; height: 680px; margin: 0 auto; border: 10px solid #1f2937; border-radius: 36px; background: #f5f7fb; overflow: auto; }
        .phone-top { background: #fff; padding: 16px 18px 10px; font-weight: 700; font-size: 17px; }
        .phone-sec { background: #fff; border-radius: 16px; margin: 12px; padding: 14px; }
        .phone-sec h6 { font-size: 15px; font-weight: 700; margin: 0 0 8px; }
        .phone-muted { font-size: 13px; color: #6b7280; }
    </style>
</head>
<body>
<jsp:include page="/WEB-INF/views/common/navbar.jsp" />

<div class="container-xl pb-5">
    <div class="d-flex align-items-center mb-3">
        <a href="/common/test-hub" class="btn btn-sm btn-outline-secondary me-2"><i class="bi bi-arrow-left"></i> 테스트 목록</a>
        <h5 class="fw-bold mb-0">[공통#47] 홈 - 추천 상품</h5>
    </div>
    <div class="alert alert-info small py-2">
        <b>금융회원 외 모든 회원</b>(비로그인·일반회원)에게 ADMIN 에서 <b>홈 표시여부 Y</b> 로 지정한 상품을 <b>홈 표시순서</b>대로 보여줍니다.
        [더보기] → 전체상품목록(상품몰), 상품 터치 → 상품안내페이지. <b>금융회원</b>은 추천 상품 영역 전체가 숨겨집니다.
        노출 상품·순서 변경은 <a href="/common/product/admin">공통#48 ADMIN</a> (수신/여신상품목록 홈 표시설정).
    </div>

    <div class="row g-4">
        <div class="col-lg-5">
            <div class="card shadow-sm">
                <div class="card-header fw-bold text-white" style="background:#0d47a1"><i class="bi bi-phone me-1"></i> APP 홈</div>
                <div class="card-body">
                    <div class="phone">
                        <div class="phone-top">페퍼저축은행</div>
                        <div class="phone-sec phone-muted">상단슬라이드배너 / 계좌정보 영역 (공통#44)</div>
                        <div class="phone-sec"><h6>퀵메뉴</h6><div id="quickMenu"></div></div>
                        <!-- ★ 추천 상품 (금융회원이면 영역째 숨김) -->
                        <div class="phone-sec" id="recommendWrap"><div id="recommend"></div></div>
                        <div class="phone-sec phone-muted">공지사항 영역</div>
                    </div>
                </div>
            </div>
        </div>
        <div class="col-lg-7">
            <div class="card shadow-sm mb-4">
                <div class="card-header fw-bold"><i class="bi bi-person-gear me-1"></i> 회원구분 (정책1 제공대상)</div>
                <div class="card-body">
                    <select id="customer" class="form-select form-select-sm">
                        <option value="">비로그인 (GUEST) - 노출</option>
                        <option value="customer02">customer02 - 일반회원 (MEMBER) - 노출</option>
                        <option value="customer01">customer01 - 금융회원 (FINANCIAL) - 영역 전체 숨김</option>
                    </select>
                    <div id="info" class="small mt-2">-</div>
                </div>
            </div>
            <div class="card shadow-sm">
                <div class="card-header">처리 결과 (API 응답 / 터치)</div>
                <div class="card-body"><pre id="log" class="result-log">-</pre></div>
            </div>
        </div>
    </div>
</div>

<script>
(function () {
    var $ = function (id) { return document.getElementById(id); };
    function log(o) { $('log').textContent = typeof o === 'string' ? o : JSON.stringify(o, null, 2); }
    function nav(url, item) { log('[터치] ' + item.name + '\n이동 URL: ' + url + '\n(샘플에서는 이동하지 않고 표시만 합니다)'); }

    // 업무 화면은 RecommendProducts.mount('#recommend') 면 된다 (params 는 local 샘플 전용)
    function load() {
        var c = $('customer').value, params = c ? { demoCustomer: c } : {};
        QuickMenu.mount('#quickMenu', { params: params, onNavigate: nav });
        RecommendProducts.mount('#recommend', {
            params: params, onNavigate: nav,
            onLoad: function (d) {
                log(d);
                // 기능4: 금융회원이면 감싼 카드 영역까지 숨김
                $('recommendWrap').hidden = !d.eligible || !d.items.length;
                $('info').textContent = '회원구분 ' + d.segment + ' · ' + (d.eligible ? '노출 ' + d.items.length + '건 (' + d.items.map(function (i) { return i.homeOrder + '.' + i.name; }).join(', ') + ')' : '금융회원 → 추천 상품 영역 숨김');
            },
            onError: function (e) { log(e.response || String(e)); $('recommendWrap').hidden = true; }
        });
    }
    $('customer').addEventListener('change', load);
    load();
})();
</script>
</body>
</html>
