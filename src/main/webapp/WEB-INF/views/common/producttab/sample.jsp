<%--
  =============================================================================
   [CF-BC-20 / 공통#26] 메인화면 상품 탭 (APP / PC Web)  -  GET /common/product-tab/sample
  -----------------------------------------------------------------------------
   요구사항 (REQ-AP-606 세부항목 1)
     1. 상단 슬라이드 카드 영역 신규 추가
     2. 기존 불필요 카드 삭제 (예: 시세조회)  → ADMIN 섹션 사용여부 N (이 화면 코드에는 카드가 없다)
     3. 디자인 및 콘텐츠 형식 수정
   업무 화면 사용법 (이것만 쓰면 된다)
     <link rel="stylesheet" href="/css/common/product-tab.css">
     <script src="/js/common/product-tab.js"></script>
     ProductTab.mount('#productTab', { channel: 'APP' });     // PC Web: channel: 'PC_WEB'
   노출 내용 변경은 디지털어드민 /common/product-tab/admin (세부항목 2).
   주의: JSP 에서는 JS 템플릿 문자열의 달러+중괄호가 EL 로 해석되므로 문자열 더하기를 사용한다.
  =============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>저축은행 포털 - 메인화면 상품 탭</title>
    <link rel="shortcut icon" href="/favicon.ico">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css" rel="stylesheet">
    <link rel="stylesheet" href="/css/common/ui-tokens.css">
    <link rel="stylesheet" href="/css/common/ui-icon.css">
    <link rel="stylesheet" href="/css/common/bottom-nav.css">
    <link rel="stylesheet" href="/css/common/product-tab.css">
    <script src="/js/common/bottom-nav.js"></script>
    <script src="/js/common/product-tab.js"></script>
    <style>
        body { background-color: #f4f6f9; font-family: 'Malgun Gothic', 'Noto Sans KR', sans-serif; }
        [hidden] { display: none !important; }
        pre.result-log { background: #212529; color: #e9ecef; border-radius: 6px; padding: 10px; max-height: 220px; overflow: auto; white-space: pre-wrap; font-size: 12px; margin: 0; }
        /* APP 목업: 상단 헤더 / 본문 스크롤 / 하단 탭바 */
        .phone { width: 100%; max-width: 360px; height: 720px; margin: 0 auto; border: 10px solid #1f2937; border-radius: 36px; background: #f5f7fb; overflow: hidden; display: flex; flex-direction: column; }
        .phone-top { background: #fff; padding: 16px 18px 12px; font-weight: 700; font-size: 17px; flex: 0 0 auto; }
        .phone-body { flex: 1 1 auto; overflow: auto; padding: 14px 14px 4px; }
        .phone-nav { flex: 0 0 auto; }
        /* PC Web 목업: 넓은 본문 */
        .pc { background: #fff; border-radius: 12px; border: 1px solid #e5e7eb; padding: 24px; }
        .pc-top { font-weight: 800; font-size: 20px; margin-bottom: 16px; }
    </style>
</head>
<body>
<jsp:include page="/WEB-INF/views/common/navbar.jsp" />

<div class="container-xl pb-5">
    <div class="d-flex align-items-center mb-3">
        <a href="/common/test-hub" class="btn btn-sm btn-outline-secondary me-2"><i class="bi bi-arrow-left"></i> 테스트 목록</a>
        <h5 class="fw-bold mb-0">[공통#26] 메인화면 상품 탭 (APP / PC Web)</h5>
        <a href="/common/product-tab/admin" class="btn btn-sm btn-outline-primary ms-auto"><i class="bi bi-gear"></i> 디지털어드민 상품 탭 관리</a>
    </div>
    <div class="alert alert-info small py-2">
        비대면으로 신청/가입 가능한 상품 안내. <b>① 상단 슬라이드 카드</b>(신규)를 좌우로 넘겨 보고, 아래 <b>② 섹션별 상품 목록</b>을 확인하세요.
        AS-IS <b>시세조회</b> 카드는 ADMIN 에서 <b>사용 N</b> 으로 삭제되어 보이지 않습니다. 카드·섹션·상품·순서는 모두 DB(디지털어드민 설정) 값이며,
        <b>판매중지 상품</b>(오토론)은 연결된 카드·목록에서 자동으로 빠집니다.
    </div>

    <div class="row g-4">
        <div class="col-lg-4">
            <div class="phone">
                <div class="phone-top">상품</div>
                <div class="phone-body"><div id="appTab"></div></div>
                <nav id="bottomNav" class="phone-nav"></nav>
            </div>
            <div class="small text-muted text-center mt-2">APP (channel=APP) - 하단 탭 '상품'</div>
        </div>
        <div class="col-lg-8">
            <div class="pc mb-3">
                <div class="pc-top">상품 <small class="text-muted fs-6 fw-normal">PC Web (channel=PC_WEB) - 카드 2장 · 상품 2열</small></div>
                <div id="pcTab"></div>
            </div>
            <div class="card shadow-sm">
                <div class="card-header">터치 / API 응답</div>
                <div class="card-body"><pre id="log" class="result-log">-</pre></div>
            </div>
        </div>
    </div>
</div>

<script>
(function () {
    var $ = function (id) { return document.getElementById(id); };
    function log(o) { $('log').textContent = typeof o === 'string' ? o : JSON.stringify(o, null, 2); }
    // 샘플에서는 실제로 이동하지 않고 어디로 갈지만 표시
    function nav(url, info) { log('[터치] ' + info.type + (info.productCd ? ' ' + info.productCd : '') + (info.section ? ' (섹션 ' + info.section + ')' : '') + '\n이동 URL: ' + url); }

    // ★ 업무 화면에서 쓰는 부분은 ProductTab.mount 한 줄
    ProductTab.mount('#appTab', { channel: 'APP', onNavigate: nav, onLoad: log });
    ProductTab.mount('#pcTab', { channel: 'PC_WEB', onNavigate: nav });

    // 하단 탭바 (공통#50) - 상품 탭을 추가한 구성 예시
    BottomNav.mount('#bottomNav', {
        active: 'PRODUCT',
        tabs: [
            { key: 'HOME', label: '홈', icon: 'bank', url: '/home' },
            { key: 'PRODUCT', label: '상품', icon: 'products', url: '/common/product-tab/sample' },
            { key: 'SAVINGS', label: '저축', icon: 'savings', url: '/savings' },
            { key: 'LOAN', label: '대출', icon: 'loan', url: '/loan' }
        ],
        onSelect: function (t) { log('[하단 탭] ' + t.label + ' → ' + t.url); }
    });
})();
</script>
</body>
</html>
