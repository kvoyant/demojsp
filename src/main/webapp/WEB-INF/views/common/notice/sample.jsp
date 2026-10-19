<%--
  =============================================================================
   [CF-BC-20 / 공통#28] 공지사항 - APP 메인(홈) 배치 샘플  -  GET /common/notice/sample
  -----------------------------------------------------------------------------
   요구사항 (REQ-AP-609)
     2. 메인화면 띠 형태로 최신공지 제목 노출      → NoticeStrip.mount('#noticeStrip')
     3. 띠 클릭 시 공지사항 상세 화면으로 이동      → /common/notice/detail?id=
     4. 이어하기 지면과 겹치거나 혼동되지 않게 배치 → 띠는 헤더 바로 아래 한 줄 (회색 바 + "공지" 표시)
                                                    이어하기 카드(공통#43)는 상단 배너 아래 흰 카드 - 위치·모양이 다르다
   (REQ-AP-610) 공지사항 배너 팝업 신규 UI          → NoticePopup.open() (아이콘 없음, 하단 [오늘 하루 보지 않기 | 닫기])
   주의: JSP 에서는 JS 템플릿 문자열의 달러+중괄호가 EL 로 해석되므로 문자열 더하기를 사용한다.
  =============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>저축은행 포털 - 공지사항 (메인 띠)</title>
    <link rel="shortcut icon" href="/favicon.ico">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css" rel="stylesheet">
    <link rel="stylesheet" href="/css/common/ui-tokens.css">
    <link rel="stylesheet" href="/css/common/ui-button.css">
    <link rel="stylesheet" href="/css/common/ui-icon.css">
    <link rel="stylesheet" href="/css/common/resume-card.css">
    <link rel="stylesheet" href="/css/common/bottom-nav.css">
    <link rel="stylesheet" href="/css/common/notice.css">
    <script src="/js/common/ui-button.js"></script>
    <script src="/js/common/resume-card.js"></script>
    <script src="/js/common/bottom-nav.js"></script>
    <script src="/js/common/notice.js"></script>
    <style>
        body { background-color: #f4f6f9; font-family: 'Malgun Gothic', 'Noto Sans KR', sans-serif; }
        [hidden] { display: none !important; }
        pre.result-log { background: #212529; color: #e9ecef; border-radius: 6px; padding: 10px; max-height: 260px; overflow: auto; white-space: pre-wrap; font-size: 12px; margin: 0; }
        .phone { width: 100%; max-width: 360px; height: 700px; margin: 0 auto; border: 10px solid #1f2937; border-radius: 36px; background: #f5f7fb; overflow: hidden; display: flex; flex-direction: column; }
        .phone-top { background: #fff; padding: 16px 18px 12px; font-weight: 800; font-size: 17px; flex: 0 0 auto; }
        .phone-body { flex: 1 1 auto; overflow: auto; }
        .phone-nav { flex: 0 0 auto; }
        .slot { margin: 12px; border-radius: 16px; padding: 14px; font-size: 12px; color: #6b7280; background: #fff; }
        .slot.banner { height: 110px; background: linear-gradient(135deg, #0d47a1, #1e88e5); color: #fff; display: flex; align-items: flex-end; font-weight: 700; font-size: 15px; }
        #resumeCard { margin: 0 12px; }
        .tag { display: inline-block; font-size: 10px; padding: 0 6px; border-radius: 6px; background: #fde68a; color: #92400e; margin-left: 4px; vertical-align: 1px; }
        .layout-note li { margin-bottom: 4px; }
    </style>
</head>
<body>
<jsp:include page="/WEB-INF/views/common/navbar.jsp" />

<div class="container-xl pb-5">
    <div class="d-flex align-items-center mb-3">
        <a href="/common/test-hub" class="btn btn-sm btn-outline-secondary me-2"><i class="bi bi-arrow-left"></i> 테스트 목록</a>
        <h5 class="fw-bold mb-0">[공통#28] 공지사항 - 메인 띠 · 목록 · 상세 · 팝업</h5>
        <a href="/common/notice/admin" class="btn btn-sm btn-outline-primary ms-auto"><i class="bi bi-gear"></i> 디지털어드민 공지사항 관리</a>
    </div>
    <div class="alert alert-info small py-2">
        메인(홈) <b>헤더 바로 아래 띠</b>에 최신공지 제목이 나오고, 띠를 누르면 <b>공지 상세</b>로 이동합니다.
        띠는 회색 한 줄 + "공지" 표시로, 아래쪽 <b>이어하기 카드</b>(흰 카드, 진행단계)와 위치·모양을 분리했습니다.
        진입하면 <b>메인 팝업</b>(신규 UI: 아이콘 없음, 하단 버튼 한 줄)이 뜹니다. 공지는 모두 DB(디지털어드민 등록값)입니다.
    </div>

    <div class="row g-4">
        <div class="col-lg-5">
            <div class="phone">
                <div class="phone-top">디지털페퍼</div>
                <div class="phone-body">
                    <!-- ★ ① 공지 띠: 헤더 바로 아래 (공지가 없으면 영역째 숨김) -->
                    <div id="noticeStrip"></div>
                    <div class="slot banner">상단 슬라이드 배너 (공통#44)</div>
                    <!-- ② 이어하기 카드: 배너 아래 (공통#43) - 띠와 떨어져 있고 모양이 다르다 -->
                    <div id="resumeCard"></div>
                    <div class="slot">퀵메뉴 (공통#46)</div>
                    <div class="slot">추천 상품 (공통#47)</div>
                </div>
                <nav id="bottomNav" class="phone-nav"></nav>
            </div>
        </div>
        <div class="col-lg-7">
            <div class="card shadow-sm mb-3">
                <div class="card-header fw-bold">배치 원칙 (요구사항 4)</div>
                <div class="card-body small">
                    <ul class="layout-note mb-2">
                        <li><b>공지 띠</b>: 헤더 바로 아래 · 높이 한 줄 · 회색 바탕 · 왼쪽 <b>"공지"</b> 표시 · 제목만 (말줄임)</li>
                        <li><b>이어하기</b>: 상단 배너 아래 · 흰 카드 · 상품명·진행단계·[이어하기] 버튼</li>
                        <li>둘 사이에 상단 배너가 있어 붙어 보이지 않고, 띠에는 버튼이 없어 이어하기 버튼과 헷갈리지 않는다</li>
                        <li>간편모드(9/30 비고: 배너·메뉴 위치 조정, 공지 표시 영역 변경)는 UI/UX 확정 후 위치만 옮기면 된다 - 컴포넌트는 같다 <span class="tag">확인 필요</span></li>
                    </ul>
                    <div class="d-flex gap-2 flex-wrap">
                        <button class="btn btn-sm btn-primary" id="btnPopup">메인 팝업 다시 보기</button>
                        <a class="btn btn-sm btn-outline-secondary" href="/common/notice/list">공지 목록 (전체/상품별/업무별)</a>
                        <a class="btn btn-sm btn-outline-secondary" href="/api/common/notice/strip" target="_blank">띠 API</a>
                    </div>
                </div>
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

    // ★ 업무 화면(APP 홈)에서 쓰는 부분 - 띠 · 팝업 각 한 줄
    NoticeStrip.mount('#noticeStrip', { onLoad: log, onNavigate: function (url) { location.href = url; } });   // 띠 → 상세 이동
    NoticePopup.open().then(function (r) { log('메인 팝업 ' + r.shown + '건 표시'); });

    // 함께 배치되는 이어하기 카드 (공통#43) - local 샘플 고객 customer01
    ResumeCard.mount('#resumeCard', { scope: 'ALL', params: { demoCustomer: 'customer01' }, go: function (url) { log('[이어하기] ' + url); } });
    BottomNav.mount('#bottomNav', { active: 'HOME', params: { demoCustomer: 'customer01' }, onSelect: function (t) { log('[하단 탭] ' + t.label); } });

    $('btnPopup').addEventListener('click', function () {
        NoticePopup.open({ ignoreHidden: true }).then(function (r) { log('메인 팝업 ' + r.shown + '건 표시 (오늘 하루 보지 않기 무시)'); });
    });
})();
</script>
</body>
</html>
