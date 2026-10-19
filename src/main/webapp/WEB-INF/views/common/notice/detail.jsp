<%--
  =============================================================================
   [CF-BC-20 / 공통#28] 공지사항 상세 (APP)  -  GET /common/notice/detail?id=
   요구사항 3: 메인 띠 · 목록 · 팝업 [자세히 보기] 를 누르면 이 화면으로 온다. 게시 중인 공지만 보이고 조회수가 1 오른다.
   업무 화면 사용법: NoticeDetail.mount('#noticeDetail');   (?id= 를 읽는다)
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
        .bar { display: flex; align-items: center; height: 56px; padding: 0 8px; border-bottom: 1px solid #f3f4f6; }
        .bar a { width: 44px; height: 44px; display: flex; align-items: center; justify-content: center; font-size: 28px; color: #111827; text-decoration: none; }
        .bar h1 { font-size: 17px; margin: 0; }
        .content { padding: 18px 16px 24px; }
        .to-list { display: block; text-align: center; padding: 14px; border: 1px solid #d1d5db; border-radius: 10px; color: #111827; text-decoration: none; font-size: 14px; }
    </style>
</head>
<body>
<div class="page">
    <div class="bar"><a href="javascript:history.back()" aria-label="뒤로">‹</a><h1>공지사항</h1></div>
    <div class="content">
        <div id="noticeDetail"></div>
        <a class="to-list" href="/common/notice/list">목록 보기</a>
    </div>
</div>
<script>
    NoticeDetail.mount('#noticeDetail');
</script>
</body>
</html>
