<%--
  =============================================================================
   [CF-BC-20 / 공통#28] 공지사항 목록 (APP)  -  GET /common/notice/list[?scope=PRODUCT|BIZ]
  -----------------------------------------------------------------------------
   요구사항 1: 전체 / 상품별 / 업무별 공지사항 구분 노출
     [전체] 탭   : 게시 중 공지 모두 (상단 고정 → 최신순)
     [상품별] 탭 : 상품별 공지 + 상품 칩 (공지가 있는 상품만)
     [업무별] 탭 : 업무별 공지 + 업무 칩
   업무 화면 사용법: NoticeList.mount('#noticeList');   상품 안내 화면 안에서 그 상품 공지만: { scope:'PRODUCT', target:'PS_SMART' }
  =============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>공지사항</title>
    <link rel="shortcut icon" href="/favicon.ico">
    <link rel="stylesheet" href="/css/common/ui-tokens.css">
    <link rel="stylesheet" href="/css/common/notice.css">
    <script src="/js/common/notice.js"></script>
    <style>
        body { margin: 0; background: #f4f6f9; font-family: 'Noto Sans KR', 'Malgun Gothic', sans-serif; }
        .page { max-width: 480px; margin: 0 auto; background: #fff; min-height: 100vh; }
        .bar { display: flex; align-items: center; height: 56px; padding: 0 8px; border-bottom: 1px solid #f3f4f6; position: sticky; top: 0; background: #fff; }
        .bar a { width: 44px; height: 44px; display: flex; align-items: center; justify-content: center; font-size: 28px; color: #111827; text-decoration: none; }
        .bar h1 { font-size: 17px; margin: 0; }
        .content { padding: 0 16px 24px; }
    </style>
</head>
<body>
<div class="page">
    <div class="bar"><a href="/common/notice/sample" aria-label="뒤로">‹</a><h1>공지사항</h1></div>
    <div class="content"><div id="noticeList"></div></div>
</div>
<script>
    // 주소의 ?scope= / ?target= 으로 처음 탭을 정할 수 있다 (메뉴·상품 화면에서 바로 연결)
    var q = new URLSearchParams(location.search);
    NoticeList.mount('#noticeList', { scope: q.get('scope') || '', target: q.get('target') || '' });
</script>
</body>
</html>
