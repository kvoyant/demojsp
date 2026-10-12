<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>저축은행 포털 - 로그인</title>
    <!-- Favicon -->
    <link rel="shortcut icon" href="/favicon.ico">
    <!-- Bootstrap 5 CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <!-- Bootstrap Icons -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css" rel="stylesheet">
    <!-- jQuery -->
    <script src="https://code.jquery.com/jquery-3.7.0.min.js"></script>
    <!-- Bootstrap 5 Bundle JS -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <!-- Common JS -->
    <script src="/js/common.js"></script>
    <style>
        body {
            background: linear-gradient(135deg, #f0f4f8 0%, #d9e2ec 100%);
            font-family: 'Malgun Gothic', 'Noto Sans KR', sans-serif;
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 30px 15px;
        }
        .login-card {
            max-width: 540px;
            width: 100%;
            border: none;
            border-radius: 12px;
            box-shadow: 0 10px 25px rgba(0, 51, 102, 0.12);
            overflow: hidden;
            background: #ffffff;
        }
        .login-header {
            background: linear-gradient(135deg, #0d47a1 0%, #1565c0 100%);
            color: white;
            padding: 28px 24px;
            text-align: center;
        }
        .nav-tabs .nav-link {
            font-weight: 600;
            color: #555;
            padding: 12px 10px;
            border-radius: 0;
            border: none;
            border-bottom: 3px solid transparent;
        }
        .nav-tabs .nav-link.active {
            color: #0d47a1;
            font-weight: bold;
            background: transparent;
            border-bottom: 3px solid #0d47a1;
        }
        .preset-badge {
            cursor: pointer;
            transition: all 0.2s;
        }
        .preset-badge:hover {
            transform: scale(1.03);
        }
        .timer-badge {
            font-size: 14px;
            color: #d32f2f;
            font-weight: bold;
        }
        .security-banner {
            font-size: 12px;
            background-color: #f8f9fa;
            border-left: 4px solid #0d47a1;
            padding: 10px 12px;
            color: #495057;
            margin-bottom: 20px;
        }
    </style>
</head>
<body>

<div class="login-card shadow">
    <!-- 헤더 영역 -->
    <div class="login-header">
        <h4 class="fw-bold mb-1"><i class="bi bi-bank2 me-2"></i>저축은행 소매금융 포털</h4>
        <p class="mb-0 small opacity-75">전자금융감독규정 준수 통합 로그인 체계</p>
    </div>

    <div class="p-4">
        <!-- 보안 정책 안내 배너 -->
        <div class="security-banner">
            <div class="fw-bold text-dark mb-1"><i class="bi bi-shield-check text-primary me-1"></i>로그인 보안 정책 안내</div>
            • <strong>개인고객</strong>: 30분 세션, 5회 비밀번호 오류 계정 잠금, NICE ID CI 간편 로그인<br>
            • <strong>상담사 / 관리자</strong>: 15분 세션, 2차 추가인증(SMS OTP) 필수, 중복로그인 차단
        </div>

        <!-- 테스트 시뮬레이션 프리셋 바 -->
        <div class="card bg-light border-0 mb-3 p-2">
            <div class="d-flex justify-content-between align-items-center mb-1">
                <small class="fw-bold text-dark"><i class="bi bi-lightning-charge-fill text-warning me-1"></i>검증 프리셋 원클릭 설정:</small>
                <small class="text-muted" style="font-size: 11px;">공통 PW: Password123!</small>
            </div>
            <div class="d-flex flex-wrap gap-1">
                <button type="button" class="btn btn-outline-primary btn-sm py-0 preset-badge" onclick="setPreset('INDIVIDUAL', 'customer01', 'Password123!')">
                    👤 개인(customer01)
                </button>
                <button type="button" class="btn btn-outline-success btn-sm py-0 preset-badge" onclick="setPreset('COUNSELOR', 'counselor01', 'Password123!')">
                    💼 상담사(2FA)
                </button>
                <button type="button" class="btn btn-outline-dark btn-sm py-0 preset-badge" onclick="setPreset('ADMIN', 'admin01', 'Password123!')">
                    🛡️ 관리자(2FA)
                </button>
                <button type="button" class="btn btn-outline-danger btn-sm py-0 preset-badge" onclick="setPreset('INDIVIDUAL', 'locked01', 'Password123!')">
                    🔒 잠금계정(5회실패)
                </button>
            </div>
        </div>

        <!-- 회원 유형 탭 -->
        <ul class="nav nav-tabs nav-fill mb-3" id="loginTab" role="tablist">
            <li class="nav-item" role="presentation">
                <button class="nav-link active" id="tab-cust" data-bs-toggle="tab" data-bs-target="#pane-cust" type="button" role="tab">
                    <i class="bi bi-person-fill me-1"></i>개인 고객
                </button>
            </li>
            <li class="nav-item" role="presentation">
                <button class="nav-link" id="tab-counselor" data-bs-toggle="tab" data-bs-target="#pane-counselor" type="button" role="tab">
                    <i class="bi bi-headset me-1"></i>대출 상담사
                </button>
            </li>
            <li class="nav-item" role="presentation">
                <button class="nav-link" id="tab-admin" data-bs-toggle="tab" data-bs-target="#pane-admin" type="button" role="tab">
                    <i class="bi bi-person-gear me-1"></i>심사역/관리자
                </button>
            </li>
        </ul>

        <!-- 탭 본문 -->
        <div class="tab-content" id="loginTabContent">
            <!-- 1. 개인 고객 탭 -->
            <div class="tab-pane fade show active" id="pane-cust" role="tabpanel">
                <form id="custLoginForm" onsubmit="return false;">
                    <div class="mb-3">
                        <label for="custMemberId" class="form-label small fw-bold">아이디</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="bi bi-person"></i></span>
                            <input type="text" id="custMemberId" class="form-control" placeholder="아이디 입력 (예: customer01)" value="customer01" required>
                        </div>
                    </div>
                    <div class="mb-3">
                        <label for="custPassword" class="form-label small fw-bold">비밀번호</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="bi bi-lock"></i></span>
                            <input type="password" id="custPassword" class="form-control" placeholder="비밀번호 입력" value="Password123!" required>
                        </div>
                    </div>
                    <button type="button" class="btn btn-primary w-100 py-2 fw-bold mb-3" onclick="doLogin('INDIVIDUAL', 'custMemberId', 'custPassword')">
                        <i class="bi bi-box-arrow-in-right me-1"></i>로그인
                    </button>
                </form>

                <div class="position-relative text-center my-3">
                    <hr class="text-secondary opacity-25">
                    <span class="position-absolute top-50 start-50 translate-middle bg-white px-2 small text-muted">간편 인증</span>
                </div>

                <!-- NICE ID 간편 본인인증 로그인 버튼 -->
                <button type="button" class="btn btn-outline-primary w-100 py-2 fw-bold" onclick="openNiceLoginModal()">
                    <i class="bi bi-shield-lock-fill me-1"></i>NICE ID 휴대폰 본인인증 로그인
                </button>
                <div class="text-center mt-2">
                    <small class="text-muted" style="font-size: 11px;">
                        * ID/PW 없이 NICE 본인인증 CI값으로 안전하게 간편 로그인합니다.
                    </small>
                </div>
            </div>

            <!-- 2. 대출상담사 탭 -->
            <div class="tab-pane fade" id="pane-counselor" role="tabpanel">
                <div class="alert alert-warning py-2 px-3 small mb-3">
                    <i class="bi bi-shield-exclamation me-1"></i>대출상담사는 전자금융감독규정에 따라 <strong>2차 추가인증(SMS OTP)</strong>이 필수입니다.
                </div>
                <form id="counselorLoginForm" onsubmit="return false;">
                    <div class="mb-3">
                        <label for="counselorMemberId" class="form-label small fw-bold">상담사 사번/ID</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="bi bi-headset"></i></span>
                            <input type="text" id="counselorMemberId" class="form-control" placeholder="상담사 사번 (예: counselor01)" value="counselor01" required>
                        </div>
                    </div>
                    <div class="mb-3">
                        <label for="counselorPassword" class="form-label small fw-bold">비밀번호</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="bi bi-lock"></i></span>
                            <input type="password" id="counselorPassword" class="form-control" placeholder="비밀번호 입력" value="Password123!" required>
                        </div>
                    </div>
                    <button type="button" class="btn btn-success w-100 py-2 fw-bold" onclick="doLogin('COUNSELOR', 'counselorMemberId', 'counselorPassword')">
                        <i class="bi bi-shield-check me-1"></i>상담사 로그인 (2차 인증 발송)
                    </button>
                </form>
            </div>

            <!-- 3. 심사역/관리자 탭 -->
            <div class="tab-pane fade" id="pane-admin" role="tabpanel">
                <div class="alert alert-dark py-2 px-3 small mb-3">
                    <i class="bi bi-lock-fill me-1"></i>관리자 포털 접근을 위해 <strong>ID/PW 검증 후 2차 SMS OTP</strong> 인증이 진행됩니다.
                </div>
                <form id="adminLoginForm" onsubmit="return false;">
                    <div class="mb-3">
                        <label for="adminMemberId" class="form-label small fw-bold">관리자 계정</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="bi bi-person-gear"></i></span>
                            <input type="text" id="adminMemberId" class="form-control" placeholder="관리자 ID (예: admin01)" value="admin01" required>
                        </div>
                    </div>
                    <div class="mb-3">
                        <label for="adminPassword" class="form-label small fw-bold">비밀번호</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="bi bi-lock"></i></span>
                            <input type="password" id="adminPassword" class="form-control" placeholder="비밀번호 입력" value="Password123!" required>
                        </div>
                    </div>
                    <button type="button" class="btn btn-dark w-100 py-2 fw-bold" onclick="doLogin('ADMIN', 'adminMemberId', 'adminPassword')">
                        <i class="bi bi-shield-lock-fill me-1"></i>관리자 로그인 (2차 인증 발송)
                    </button>
                </form>
            </div>
        </div>

        <!-- 안내 푸터 -->
        <div class="text-center mt-4 pt-3 border-top">
            <a href="/" class="text-decoration-none text-muted small me-3"><i class="bi bi-house-door me-1"></i>포털 홈</a>
            <a href="/common/auth/nice-cert" class="text-decoration-none text-muted small me-3"><i class="bi bi-shield-lock me-1"></i>본인인증 화면</a>
            <a href="/board/list" class="text-decoration-none text-muted small"><i class="bi bi-journal-text me-1"></i>게시판</a>
        </div>
    </div>
</div>

<!-- ================= 2차 추가인증 (SMS OTP) 모달 ================= -->
<div class="modal fade" id="twoFactorModal" tabindex="-1" aria-labelledby="twoFactorModalLabel" aria-hidden="true" data-bs-backdrop="static">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
            <div class="modal-header bg-primary text-white">
                <h5 class="modal-title fw-bold" id="twoFactorModalLabel">
                    <i class="bi bi-phone-vibrate me-1"></i>2차 추가인증 (SMS OTP)
                </h5>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body p-4">
                <p class="text-muted small mb-3">
                    전자금융감독규정에 따라 등록된 휴대폰(<strong id="modalMaskedPhone" class="text-dark"></strong>)으로 
                    전송된 <strong>6자리 인증번호</strong>를 입력해 주세요.
                </p>

                <div class="mb-3">
                    <div class="d-flex justify-content-between align-items-center mb-1">
                        <label for="twoFactorOtpCode" class="form-label small fw-bold mb-0">인증번호 입력</label>
                        <span class="timer-badge" id="modalOtpTimer"><i class="bi bi-clock me-1"></i>03:00</span>
                    </div>
                    <input type="text" id="twoFactorOtpCode" class="form-control form-control-lg text-center fw-bold letter-spacing-2" placeholder="000000" maxlength="6">
                </div>

                <!-- 개발/테스트 편의 자동입력 안내 -->
                <div id="modalTestOtpGuide" class="alert alert-info py-2 small mb-3"></div>

                <button type="button" class="btn btn-primary w-100 py-2 fw-bold" onclick="doVerifyTwoFactor()">
                    <i class="bi bi-check2-circle me-1"></i>인증 및 로그인 완료
                </button>
            </div>
        </div>
    </div>
</div>

<!-- ================= NICE ID 본인확인 간편 로그인 모달 ================= -->
<div class="modal fade" id="niceLoginModal" tabindex="-1" aria-labelledby="niceLoginModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
            <div class="modal-header bg-primary text-white">
                <h5 class="modal-title fw-bold" id="niceLoginModalLabel">
                    <i class="bi bi-shield-lock-fill me-1"></i>NICE ID 본인인증 로그인
                </h5>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body p-4">
                <p class="small text-muted mb-3">
                    성명, 생년월일, 휴대폰번호 기반의 <strong>통신사 미지정 본인인증</strong>을 통해 인증된 CI값으로 즉시 로그인합니다.
                </p>

                <div class="mb-3">
                    <label class="form-label small fw-bold">성명</label>
                    <input type="text" id="niceCustNm" class="form-control" value="홍길동" placeholder="성명">
                </div>
                <div class="mb-3">
                    <label class="form-label small fw-bold">휴대폰번호</label>
                    <input type="tel" id="niceCellNo" class="form-control" value="01012345678" placeholder="휴대폰번호">
                </div>
                <div class="mb-3">
                    <label class="form-label small fw-bold">연계정보 CI (샘플 또는 직접입력)</label>
                    <input type="text" id="niceCiVal" class="form-control font-monospace small" 
                           value="MOCK_CI_NORMAL_ADULT_19900515_01012345678_ABCDEF12345678901234567890123456789012345678" 
                           placeholder="88자리 CI값">
                </div>

                <button type="button" class="btn btn-success w-100 py-2 fw-bold" onclick="doLoginWithNiceCi()">
                    <i class="bi bi-box-arrow-in-right me-1"></i>본인인증 완료 및 로그인
                </button>
            </div>
        </div>
    </div>
</div>

<script>
    var currentTwoFactorTxSeq = '';
    var modalTimerInterval = null;
    var modalRemainingSeconds = 180;
    var redirectUrl = '${redirectUrl}';

    function setPreset(memberType, memberId, password) {
        if (memberType === 'INDIVIDUAL') {
            $('#tab-cust').tab('show');
            $('#custMemberId').val(memberId);
            $('#custPassword').val(password);
        } else if (memberType === 'COUNSELOR') {
            $('#tab-counselor').tab('show');
            $('#counselorMemberId').val(memberId);
            $('#counselorPassword').val(password);
        } else if (memberType === 'ADMIN') {
            $('#tab-admin').tab('show');
            $('#adminMemberId').val(memberId);
            $('#adminPassword').val(password);
        }
    }

    // 1차 로그인 요청
    function doLogin(memberType, idField, pwField) {
        var memberId = $('#' + idField).val().trim();
        var password = $('#' + pwField).val().trim();

        if (!memberId) { alert('아이디를 입력하세요.'); $('#' + idField).focus(); return; }
        if (!password) { alert('비밀번호를 입력하세요.'); $('#' + pwField).focus(); return; }

        var reqData = {
            memberId: memberId,
            password: password,
            memberType: memberType
        };

        Common.ajax({
            url: '/api/auth/login',
            type: 'POST',
            data: reqData,
            success: function(res) {
                if (res.requiresTwoFactor) {
                    // 상담사 또는 관리자 2차 인증 요구
                    currentTwoFactorTxSeq = res.twoFactorTxSeq;
                    $('#modalMaskedPhone').text(res.maskedPhone);
                    $('#modalTestOtpGuide').html('🧪 [테스트 자동입력] 전송된 2차 OTP: <strong>' + res.testOtpCode + '</strong>');
                    $('#twoFactorOtpCode').val(res.testOtpCode);

                    var modal = new bootstrap.Modal(document.getElementById('twoFactorModal'));
                    modal.show();

                    startModalTimer(180);
                } else {
                    // 개인고객 즉시 로그인 완료
                    alert(res.message || '로그인되었습니다.');
                    location.href = res.redirectUrl || redirectUrl || '/board/list';
                }
            }
        });
    }

    // 2차 인증 OTP 검증
    function doVerifyTwoFactor() {
        var otpCode = $('#twoFactorOtpCode').val().trim();
        if (!otpCode || otpCode.length !== 6) {
            alert('인증번호 6자리를 입력하세요.');
            $('#twoFactorOtpCode').focus();
            return;
        }

        var verifyData = {
            twoFactorTxSeq: currentTwoFactorTxSeq,
            otpCode: otpCode
        };

        Common.ajax({
            url: '/api/auth/login/2fa',
            type: 'POST',
            data: verifyData,
            success: function(res) {
                clearInterval(modalTimerInterval);
                alert(res.message || '2차 인증 성공! 로그인되었습니다.');
                location.href = res.redirectUrl || redirectUrl || '/board/list';
            }
        });
    }

    // NICE ID 모달 열기
    function openNiceLoginModal() {
        var modal = new bootstrap.Modal(document.getElementById('niceLoginModal'));
        modal.show();
    }

    // NICE ID CI 간편 로그인
    function doLoginWithNiceCi() {
        var ciVal = $('#niceCiVal').val().trim();
        var custNm = $('#niceCustNm').val().trim();
        var cellNo = $('#niceCellNo').val().trim();

        if (!ciVal) {
            alert('CI 값을 입력하거나 선택하세요.');
            return;
        }

        var niceReq = {
            ciVal: ciVal,
            custNm: custNm,
            cellNo: cellNo
        };

        Common.ajax({
            url: '/api/auth/login/nice-ci',
            type: 'POST',
            data: niceReq,
            success: function(res) {
                alert(res.message || 'NICE 본인인증으로 로그인되었습니다.');
                location.href = res.redirectUrl || redirectUrl || '/board/list';
            }
        });
    }

    function startModalTimer(seconds) {
        clearInterval(modalTimerInterval);
        modalRemainingSeconds = seconds;
        updateModalTimer();

        modalTimerInterval = setInterval(function() {
            modalRemainingSeconds--;
            if (modalRemainingSeconds <= 0) {
                clearInterval(modalTimerInterval);
                $('#modalOtpTimer').text('시간 만료');
                alert('2차 인증 유효시간이 만료되었습니다. 다시 시도해 주세요.');
                bootstrap.Modal.getInstance(document.getElementById('twoFactorModal')).hide();
            } else {
                updateModalTimer();
            }
        }, 1000);
    }

    function updateModalTimer() {
        var min = Math.floor(modalRemainingSeconds / 60);
        var sec = modalRemainingSeconds % 60;
        var display = (min < 10 ? '0' : '') + min + ':' + (sec < 10 ? '0' : '') + sec;
        $('#modalOtpTimer').html('<i class="bi bi-clock me-1"></i>' + display);
    }
</script>

</body>
</html>
