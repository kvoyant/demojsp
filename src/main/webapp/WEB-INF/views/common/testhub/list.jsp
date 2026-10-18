<%--
  =============================================================================
   공통모듈 테스트 허브  -  GET /common/test-hub
  -----------------------------------------------------------------------------
   요구사항별 공통모듈 샘플 화면 / API 를 목록으로 모아 테스트한다.
   항목 추가: application.yaml 의 common.test-hub.items 에 블록 추가 (이 JSP 수정 불필요)
  =============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>저축은행 포털 - 공통모듈 테스트</title>
    <link rel="shortcut icon" href="/favicon.ico">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css" rel="stylesheet">
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <style>
        body { background-color: #f4f6f9; font-family: 'Malgun Gothic', 'Noto Sans KR', sans-serif; }
        .hub-head { background: linear-gradient(135deg, #0d47a1 0%, #1976d2 100%); color: #fff; border-radius: 10px; padding: 24px; }
        .hub-stat { background: rgba(255,255,255,.14); border-radius: 8px; padding: 8px 16px; text-align: center; min-width: 96px; }
        .hub-stat b { display: block; font-size: 22px; }
        .req-table th { background: #e9eef6; font-size: 13px; white-space: nowrap; }
        .req-table td { font-size: 13px; vertical-align: top; }
        .req-id { font-family: monospace; font-weight: 700; color: #0d47a1; white-space: nowrap; }
        .feature-list { margin: 6px 0 0; padding-left: 18px; color: #555; font-size: 12px; }
        .ch-badge { font-size: 11px; font-weight: 500; }
        pre.api-out { background: #212529; color: #e9ecef; border-radius: 6px; padding: 12px; max-height: 420px; overflow: auto; font-size: 12px; white-space: pre-wrap; margin: 0; }
    </style>
</head>
<body>
<jsp:include page="/WEB-INF/views/common/navbar.jsp" />

<div class="container-xl pb-5">
    <div class="hub-head shadow-sm mb-4 d-flex flex-wrap justify-content-between align-items-center gap-3">
        <div>
            <h4 class="fw-bold mb-1"><i class="bi bi-clipboard2-check me-1"></i> 공통모듈 테스트</h4>
            <div class="small opacity-75">요구사항별 공통모듈 샘플 화면과 API 를 한 곳에서 확인합니다. 목록은 application.yaml 의 <code class="text-white">common.test-hub.items</code> 로 관리합니다.</div>
        </div>
        <div class="d-flex gap-2">
            <div class="hub-stat"><b>${total}</b><span class="small">전체</span></div>
            <div class="hub-stat"><b>${doneCount}</b><span class="small">개발완료</span></div>
        </div>
    </div>

    <div class="card shadow-sm mb-4">
        <div class="card-body py-2 d-flex flex-wrap gap-2 align-items-center">
            <i class="bi bi-search text-muted"></i>
            <input id="q" type="search" class="form-control form-control-sm" style="max-width: 320px;" placeholder="ID / 요구사항명 / 기능 검색">
            <select id="st" class="form-select form-select-sm" style="max-width: 160px;">
                <option value="">상태 전체</option>
                <option>개발완료</option>
                <option>개발중</option>
                <option>대기</option>
            </select>
            <span id="cnt" class="small text-muted ms-auto"></span>
        </div>
    </div>

    <div class="card shadow-sm mb-4">
        <div class="table-responsive">
            <table class="table req-table mb-0">
                <thead>
                <tr>
                    <th style="width: 48px;">No</th>
                    <th>공통기능 ID<br>요구사항 ID</th>
                    <th>요구사항명 / 주요 기능</th>
                    <th>적용채널</th>
                    <th>상태</th>
                    <th style="min-width: 220px;">테스트</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="it" items="${items}" varStatus="s">
                    <tr class="req-row" data-status="<c:out value='${it.status}'/>">
                        <td class="text-muted">${s.count}</td>
                        <td>
                            <div class="req-id"><c:out value="${it.cfId}"/></div>
                            <div class="req-id text-secondary"><c:out value="${it.reqId}"/></div>
                        </td>
                        <td>
                            <div class="fw-bold"><c:out value="${it.title}"/></div>
                            <div class="small text-muted">
                                <c:out value="${it.category}"/>
                                <c:if test="${not empty it.requestedAt}"> · 요건발생 <c:out value="${it.requestedAt}"/></c:if>
                            </div>
                            <c:if test="${not empty it.features}">
                                <ul class="feature-list">
                                    <c:forEach var="f" items="${it.features}"><li><c:out value="${f}"/></li></c:forEach>
                                </ul>
                            </c:if>
                            <c:if test="${not empty it.guide}">
                                <div class="small mt-1"><i class="bi bi-file-earmark-text"></i> 가이드: <code><c:out value="${it.guide}"/></code></div>
                            </c:if>
                        </td>
                        <td>
                            <c:forEach var="ch" items="${it.channels}">
                                <span class="badge text-bg-light border ch-badge mb-1"><c:out value="${ch}"/></span>
                            </c:forEach>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${it.status eq '개발완료'}"><span class="badge bg-success">개발완료</span></c:when>
                                <c:when test="${it.status eq '개발중'}"><span class="badge bg-warning text-dark">개발중</span></c:when>
                                <c:otherwise><span class="badge bg-secondary"><c:out value="${it.status}"/></span></c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <div class="d-flex flex-wrap gap-1">
                                <c:forEach var="lk" items="${it.links}">
                                    <c:choose>
                                        <c:when test="${lk.type eq 'API'}">
                                            <button type="button" class="btn btn-sm btn-outline-secondary api-btn" data-url="<c:out value='${lk.url}'/>">
                                                <i class="bi bi-braces"></i> <c:out value="${lk.label}"/>
                                            </button>
                                        </c:when>
                                        <c:otherwise>
                                            <a class="btn btn-sm btn-primary" href="<c:out value='${lk.url}'/>">
                                                <i class="bi bi-box-arrow-up-right"></i> <c:out value="${lk.label}"/>
                                            </a>
                                        </c:otherwise>
                                    </c:choose>
                                </c:forEach>
                            </div>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty items}">
                    <tr><td colspan="6" class="text-center text-muted py-4">등록된 요구사항이 없습니다. application.yaml 의 common.test-hub.items 를 확인하세요.</td></tr>
                </c:if>
                </tbody>
            </table>
        </div>
    </div>

    <div class="card shadow-sm">
        <div class="card-header d-flex justify-content-between align-items-center">
            <span class="fw-bold"><i class="bi bi-terminal me-1"></i> API 응답</span>
            <code id="apiUrl" class="small text-muted"></code>
        </div>
        <div class="card-body"><pre id="apiOut" class="api-out">[API] 버튼을 누르면 GET 응답이 여기에 표시됩니다.</pre></div>
    </div>
</div>

<script>
(function () {
    var rows = Array.prototype.slice.call(document.querySelectorAll('.req-row'));
    var q = document.getElementById('q'), st = document.getElementById('st'), cnt = document.getElementById('cnt');

    function filter() {
        var kw = q.value.trim().toLowerCase(), status = st.value, shown = 0;
        rows.forEach(function (r) {
            var ok = (!kw || r.textContent.toLowerCase().indexOf(kw) >= 0) && (!status || r.getAttribute('data-status') === status);
            r.style.display = ok ? '' : 'none';
            if (ok) shown++;
        });
        cnt.textContent = shown + ' / ' + rows.length + '건';
    }
    q.addEventListener('input', filter);
    st.addEventListener('change', filter);
    filter();

    // API 버튼: GET 호출 결과를 하단에 표시
    var out = document.getElementById('apiOut'), urlEl = document.getElementById('apiUrl');
    document.querySelectorAll('.api-btn').forEach(function (b) {
        b.addEventListener('click', function () {
            var url = b.getAttribute('data-url');
            urlEl.textContent = 'GET ' + url;
            out.textContent = '호출 중…';
            fetch(url, { credentials: 'same-origin', headers: { 'X-Requested-With': 'XMLHttpRequest' } })
                .then(function (r) { return r.text().then(function (t) { return { status: r.status, text: t }; }); })
                .then(function (res) {
                    var body;
                    try { body = JSON.stringify(JSON.parse(res.text), null, 2); } catch (e) { body = res.text; }
                    out.textContent = 'HTTP ' + res.status + '\n' + body;
                })
                .catch(function (e) { out.textContent = '호출 실패: ' + e; });
            out.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        });
    });
})();
</script>
</body>
</html>
