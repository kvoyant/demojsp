<%--
  [CF-BC-13 / 공통#43] [이어하기] 도착 화면 흉내  -  GET /common/resume/landing?biz=&product=&step=
  실제로는 각 상품 신청 화면이 멈춘 단계(step)부터 진행한다. (파라미터는 컨트롤러에서 허용 문자만 통과)
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>저축은행 포털 - 이어하기 진입</title>
    <link rel="stylesheet" href="/css/common/ui-tokens.css">
    <link rel="stylesheet" href="/css/common/ui-button.css">
    <style>
        body { margin: 0; background: #f5f7fb; font-family: var(--ui-font); color: var(--ui-text); }
        .wrap { max-width: 480px; margin: 0 auto; padding: 48px 20px; text-align: center; }
        .box { background: #fff; border-radius: 16px; padding: 28px 20px; box-shadow: 0 2px 10px rgba(0,0,0,.05); }
        h1 { font-size: 19px; margin: 0 0 8px; }
        p { color: var(--ui-text-sub); font-size: 14px; line-height: 1.6; }
        code { background: var(--ui-surface-sub); padding: 1px 6px; border-radius: 4px; }
    </style>
</head>
<body>
<div class="wrap">
    <div class="box">
        <h1>이어하기로 진입했습니다</h1>
        <p>업무 <code>${biz}</code> · 상품 <code>${product}</code><br><b>${step}단계</b>부터 이어서 진행하는 화면입니다.<br>(샘플 - 실제 상품 신청 화면으로 연결)</p>
        <a class="ui-btn ui-btn--primary ui-btn--block" href="/common/resume/sample">홈으로</a>
    </div>
</div>
</body>
</html>
