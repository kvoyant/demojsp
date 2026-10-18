<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.example.demojsp.domain.login.dto.UserSessionDto" %>
<%@ page import="com.example.demojsp.domain.login.service.LoginServiceImpl" %>
<%
    UserSessionDto loginUser = (UserSessionDto) session.getAttribute(LoginServiceImpl.SESSION_USER_KEY);
    boolean isLoggedIn = (loginUser != null);
%>
<nav class="navbar navbar-expand-lg navbar-dark shadow-sm" style="background-color: #0d47a1; margin-bottom: 25px;">
    <div class="container-fluid px-4">
        <a class="navbar-brand fw-bold" href="/">
            <i class="bi bi-bank2 me-1"></i> 저축은행 소매금융 포털
        </a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#topNavbar">
            <span class="navbar-toggler-icon"></span>
        </button>

        <div class="collapse navbar-collapse" id="topNavbar">
            <ul class="navbar-nav me-auto mb-2 mb-lg-0">
                <li class="nav-item">
                    <a class="nav-link" href="/board/list"><i class="bi bi-journal-text me-1"></i>포털 게시판</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="/common/auth/nice-cert"><i class="bi bi-shield-lock me-1"></i>NICE 본인인증</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link text-warning fw-bold" href="/common/auth/account-cert"><i class="bi bi-coin me-1"></i>1원 계좌인증</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="/common/test-hub"><i class="bi bi-clipboard2-check me-1"></i>공통모듈 테스트</a>
                </li>
                <% if (isLoggedIn && "ROLE_ADMIN".equals(loginUser.getRoleCd())) { %>
                <li class="nav-item">
                    <a class="nav-link text-warning fw-bold" href="/common/auth/ci-migration">
                        <i class="bi bi-database-gear me-1"></i>CI 이관 관리 (관리자)
                    </a>
                </li>
                <% } %>
            </ul>

            <div class="d-flex align-items-center text-white">
                <% if (isLoggedIn) { 
                    String roleBadgeClass = "bg-primary-subtle text-primary";
                    String roleName = "개인고객";
                    if ("ROLE_COUNSELOR".equals(loginUser.getRoleCd())) {
                        roleBadgeClass = "bg-warning text-dark";
                        roleName = "대출상담사";
                    } else if ("ROLE_ADMIN".equals(loginUser.getRoleCd())) {
                        roleBadgeClass = "bg-danger text-white";
                        roleName = "심사역/관리자";
                    }
                %>
                    <div class="me-3 small">
                        <span class="badge <%= roleBadgeClass %> px-2 py-1 me-1"><%= roleName %></span>
                        <strong><%= loginUser.getMemberNm() %></strong>님 환영합니다.
                    </div>

                    <!-- 세션 남은 시간 & 연장 위젯 -->
                    <div class="bg-white bg-opacity-10 rounded px-2 py-1 me-3 d-flex align-items-center small">
                        <i class="bi bi-clock me-1 text-warning"></i>
                        <span id="navSessionTimer" class="font-monospace fw-bold me-2">--:--</span>
                        <button type="button" class="btn btn-outline-light btn-sm py-0 px-2" style="font-size: 11px;" onclick="extendNavSession()">
                            연장
                        </button>
                    </div>

                    <a href="/logout" class="btn btn-outline-light btn-sm">
                        <i class="bi bi-box-arrow-right me-1"></i>로그아웃
                    </a>
                <% } else { %>
                    <a href="/login" class="btn btn-light btn-sm text-primary fw-bold">
                        <i class="bi bi-box-arrow-in-right me-1"></i>로그인
                    </a>
                <% } %>
            </div>
        </div>
    </div>
</nav>

<% if (isLoggedIn) { %>
<script>
    var sessionRemainingSec = <%= (session.getMaxInactiveInterval() > 0 ? session.getMaxInactiveInterval() : 1800) %>;
    var sessionTimerInterval = null;

    $(document).ready(function() {
        startNavSessionTimer();
    });

    function startNavSessionTimer() {
        clearInterval(sessionTimerInterval);
        updateNavTimerDisplay();

        sessionTimerInterval = setInterval(function() {
            sessionRemainingSec--;
            if (sessionRemainingSec <= 0) {
                clearInterval(sessionTimerInterval);
                alert('로그인 세션이 만료되었습니다. 다시 로그인해 주세요.');
                location.href = '/login';
            } else {
                if (sessionRemainingSec === 60) {
                    if (confirm('로그인 세션 만료 1분 전입니다. 세션을 연장하시겠습니까?')) {
                        extendNavSession();
                    }
                }
                updateNavTimerDisplay();
            }
        }, 1000);
    }

    function updateNavTimerDisplay() {
        var min = Math.floor(sessionRemainingSec / 60);
        var sec = sessionRemainingSec % 60;
        var display = (min < 10 ? '0' : '') + min + ':' + (sec < 10 ? '0' : '') + sec;
        $('#navSessionTimer').text(display);
    }

    function extendNavSession() {
        Common.ajax({
            url: '/api/auth/session/extend',
            type: 'POST',
            success: function(res) {
                sessionRemainingSec = res.maxInactiveInterval || 1800;
                updateNavTimerDisplay();
                alert(res.message || '세션이 연장되었습니다.');
            }
        });
    }
</script>
<% } %>
