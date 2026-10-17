<%--
  =============================================================================
   본인인증 결과 전달 페이지 (공통)  -  /common/identity/callback/{method} 의 View
  -----------------------------------------------------------------------------
   인증기관(NICE) 인증창 → 이 페이지로 돌아온다. 서버 처리는 이미 끝난 상태이며
   여기서는 "결과를 원래 화면에 알려주는 일"만 한다.

   - 팝업 채널(PC WEB / 모집인WEB) : opener(원래 화면)로 postMessage 후 팝업 닫기
   - APP 채널(WebView)            : returnUrl 로 이동 (?identityResult=success|fail&code=...)

   ※ CI 등 개인정보는 이 페이지에 절대 출력하지 않는다. (결과 성공/실패 신호만 전달)
   ※ 모든 출력값은 fn:escapeXml / JS 문자열 escape 처리 (XSS 방지)
  =============================================================================
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>본인인증</title>
</head>
<body>
<p id="msg">본인인증 결과를 처리하고 있습니다...</p>

<%-- 서버 값을 data 속성으로 넘기면 JS 문자열 인젝션 걱정 없이 안전하게 읽을 수 있다 --%>
<div id="identityResult"
     data-success="${success}"
     data-popup="${popup}"
     data-purpose="${fn:escapeXml(purpose)}"
     data-return-url="${fn:escapeXml(returnUrl)}"
     data-code="${fn:escapeXml(errorCode)}"
     data-message="${fn:escapeXml(errorMessage)}"
     hidden></div>

<script>
(function () {
    var el = document.getElementById('identityResult');
    var d = el.dataset;
    var result = {
        type: 'COMMON_IDENTITY_RESULT',          // /js/common/identity-auth.js 의 MESSAGE_TYPE 과 동일
        success: d.success === 'true',
        purpose: d.purpose,
        code: d.code,
        message: d.message
    };

    if (d.popup === 'true') {
        // ---- 팝업 채널 ----
        if (window.opener && !window.opener.closed) {
            // targetOrigin 을 우리 도메인으로 한정 (다른 출처로 결과가 새지 않도록)
            window.opener.postMessage(result, window.location.origin);
            window.close();
        } else {
            // 부모창을 닫아버린 경우
            document.getElementById('msg').textContent = result.success
                ? '본인인증이 완료되었습니다. 이 창을 닫고 원래 화면에서 진행해 주세요.'
                : (result.message || '본인인증에 실패했습니다.');
        }
        return;
    }

    // ---- APP 채널 (WebView) ----
    var returnUrl = d.returnUrl || '/';
    var sep = returnUrl.indexOf('?') >= 0 ? '&' : '?';
    var target = returnUrl + sep + 'identityResult=' + (result.success ? 'success' : 'fail')
        + (result.code ? '&code=' + encodeURIComponent(result.code) : '');

    // 앱 네이티브 브릿지가 있으면 우선 사용 (앱팀과 인터페이스명 협의 필요)
    // if (window.AppBridge && window.AppBridge.identityResult) { window.AppBridge.identityResult(JSON.stringify(result)); return; }
    location.replace(target);   // replace: 뒤로가기로 콜백 페이지 재진입 방지
})();
</script>
</body>
</html>
