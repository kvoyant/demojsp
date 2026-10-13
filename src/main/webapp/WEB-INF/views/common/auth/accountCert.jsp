<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>저축은행 포털 - 1원 계좌인증 (본인확인)</title>
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
            background-color: #f4f6f9;
            font-family: 'Malgun Gothic', 'Noto Sans KR', sans-serif;
            -webkit-font-smoothing: antialiased;
        }
        .cert-card {
            max-width: 620px;
            margin: 0 auto;
            border: none;
            border-radius: 12px;
            box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
            background: #ffffff;
        }
        .header-financial {
            background: linear-gradient(135deg, #0d47a1 0%, #1565c0 100%);
            color: #ffffff;
            border-radius: 12px 12px 0 0;
            padding: 22px 24px;
        }
        .mode-tab .nav-link {
            font-weight: 600;
            color: #495057;
            border: none;
            border-bottom: 3px solid transparent;
            padding: 12px 10px;
        }
        .mode-tab .nav-link.active {
            color: #0d47a1;
            font-weight: bold;
            border-bottom: 3px solid #0d47a1;
            background: transparent;
        }
        .loan-autofill-box {
            background-color: #e8f0fe;
            border: 1px solid #c2e7ff;
            border-radius: 8px;
            padding: 14px 16px;
        }
        .passbook-preview {
            background: #fdfdfd;
            border: 2px dashed #b0bec5;
            border-radius: 8px;
            padding: 14px;
            text-align: center;
        }
        .timer-badge {
            font-size: 14px;
            font-weight: bold;
            color: #d32f2f;
        }
        .letter-spacing-3 {
            letter-spacing: 6px;
        }
        .preset-chip {
            font-size: 12px;
            cursor: pointer;
            transition: all 0.2s;
        }
        .preset-chip:hover {
            transform: translateY(-2px);
        }
    </style>
</head>
<body>

<!-- 상단 공통 네비게이션 -->
<jsp:include page="/WEB-INF/views/common/navbar.jsp" />

<div class="container py-4">
    <div class="cert-card shadow-sm">
        <!-- 헤더 영역 -->
        <div class="header-financial">
            <div class="d-flex justify-content-between align-items-center">
                <h4 class="fw-bold mb-1 fs-5">
                    <i class="bi bi-coin text-warning me-2"></i>본인확인 | 1원 계좌인증
                </h4>
                <span class="badge bg-light text-primary fw-bold" id="channelBadge">APP / Web 공용</span>
            </div>
            <p class="mb-0 small opacity-75">
                타행 본인 명의 계좌로 1원을 송금하여 입금자명의 인증코드를 확인합니다. (REQ-AP-605)
            </p>
        </div>

        <div class="p-4">
            <!-- 시뮬레이션 프리셋 바 -->
            <div class="card bg-light border-0 mb-3 p-2">
                <div class="d-flex justify-content-between align-items-center mb-1">
                    <small class="fw-bold text-dark"><i class="bi bi-sliders me-1"></i>검증 프리셋 원클릭 자동완성:</small>
                    <small class="text-muted" style="font-size: 11px;">요구사항 1, 2 테스트</small>
                </div>
                <div class="d-flex flex-wrap gap-1">
                    <button type="button" class="btn btn-outline-primary btn-sm py-0 preset-chip" onclick="applyLoanPreset('LN202610130001')">
                        📋 대출계좌 1 (국민/홍길동)
                    </button>
                    <button type="button" class="btn btn-outline-success btn-sm py-0 preset-chip" onclick="applyLoanPreset('LN202610130002')">
                        📋 대출계좌 2 (신한/김상담)
                    </button>
                    <button type="button" class="btn btn-outline-info btn-sm py-0 preset-chip" onclick="applyLoanPreset('LN202610130004')">
                        📋 대출계좌 3 (카카오/홍길동)
                    </button>
                    <button type="button" class="btn btn-outline-danger btn-sm py-0 preset-chip" onclick="applyMockErrorPreset('SUSPENDED')">
                        🚫 거래정지 계좌 (오류)
                    </button>
                </div>
            </div>

            <!-- 화면 분리 운영 목적 탭 (대출신청 연계 vs 타 업무 공통) -->
            <ul class="nav nav-tabs nav-fill mode-tab mb-3" id="purposeTab" role="tablist">
                <li class="nav-item" role="presentation">
                    <button class="nav-link ${purpose == 'LOAN_APPLY' ? 'active' : ''}" id="tab-loan" data-bs-toggle="tab" data-bs-target="#pane-loan" type="button" role="tab" onclick="switchPurpose('LOAN_APPLY')">
                        <i class="bi bi-file-earmark-text-fill me-1"></i>대출신청 연계 모드
                    </button>
                </li>
                <li class="nav-item" role="presentation">
                    <button class="nav-link ${purpose != 'LOAN_APPLY' ? 'active' : ''}" id="tab-common" data-bs-toggle="tab" data-bs-target="#pane-common" type="button" role="tab" onclick="switchPurpose('COMMON')">
                        <i class="bi bi-shield-check me-1"></i>타 업무 공통 모드
                    </button>
                </li>
            </ul>

            <!-- 1. 대출신청 연계 모드 안내 배너 -->
            <div id="loanModeBanner" class="loan-autofill-box mb-3" style="display: none;">
                <div class="d-flex justify-content-between align-items-center mb-1">
                    <div class="fw-bold text-primary small">
                        <i class="bi bi-check-circle-fill me-1"></i>대출신청 대출금입금/출금계좌 자동완성 모드
                    </div>
                    <span class="badge bg-primary small" id="loanBadgeNo">대출번호: LN202610130001</span>
                </div>
                <div class="small text-muted" id="loanProductDesc">
                    • 대출신청 단계에서 등록된 대출금 입금/출금 계좌정보를 불러와 자동완성 처리하였습니다.<br>
                    • 다른 계좌로 인증을 진행하시려면 아래 <strong>[계좌 변경하기]</strong>를 클릭하세요.
                </div>
            </div>

            <!-- 2. 공통 모드 안내 배너 -->
            <div id="commonModeBanner" class="alert alert-secondary py-2 small mb-3">
                <i class="bi bi-info-circle me-1"></i>
                본인 명의의 타행 계좌 정보를 입력해 주시면 <strong>1원 송금</strong>을 통해 본인 소유 여부를 확인합니다.
            </div>

            <!-- 계좌 입력 폼 -->
            <form id="accountForm" onsubmit="return false;">
                <!-- 고객명 (실명) -->
                <div class="mb-3">
                    <label for="custNm" class="form-label small fw-bold">
                        예금주명 (본인 실명) <span class="text-danger">*</span>
                    </label>
                    <div class="input-group">
                        <span class="input-group-text"><i class="bi bi-person"></i></span>
                        <input type="text" id="custNm" class="form-control" value="${custNm}" placeholder="본인 성명을 입력하세요" required>
                    </div>
                </div>

                <!-- 은행 선택 및 변경 스위치 -->
                <div class="mb-3">
                    <div class="d-flex justify-content-between align-items-center mb-1">
                        <label for="bankCd" class="form-label small fw-bold mb-0">
                            입금 은행 <span class="text-danger">*</span>
                        </label>
                        <!-- 요구사항 2: 수정기능 제공 토글 버튼 -->
                        <div id="modifyToggleWrapper" style="display: none;">
                            <button type="button" id="btnToggleModify" class="btn btn-outline-secondary btn-sm py-0" style="font-size: 12px;" onclick="toggleAccountModification()">
                                <i class="bi bi-pencil-square me-1"></i>계좌 변경하기
                            </button>
                            <button type="button" id="btnRestoreOriginal" class="btn btn-outline-primary btn-sm py-0 ms-1" style="font-size: 12px; display: none;" onclick="restoreOriginalLoanAccount()">
                                <i class="bi bi-arrow-counterclockwise me-1"></i>대출계좌로 복원
                            </button>
                        </div>
                    </div>
                    <select id="bankCd" class="form-select form-select-lg" required>
                        <option value="">은행을 선택해 주세요</option>
                        <!-- AJAX 동적 렌더링 -->
                    </select>
                </div>

                <!-- 계좌번호 -->
                <div class="mb-3">
                    <div class="d-flex justify-content-between align-items-center mb-1">
                        <label for="accountNo" class="form-label small fw-bold mb-0">
                            계좌번호 <span class="text-danger">*</span>
                        </label>
                        <span class="small text-muted" id="accountStatusTag" style="font-size: 11px;">
                            '-' 없이 숫자만 입력
                        </span>
                    </div>
                    <div class="input-group">
                        <span class="input-group-text"><i class="bi bi-credit-card-2-front"></i></span>
                        <input type="tel" id="accountNo" class="form-control form-control-lg font-monospace" 
                               placeholder="계좌번호 입력 (예: 12340204567890)" maxlength="16" required>
                    </div>
                    <div id="modifyAlert" class="form-text text-warning small mt-1" style="display: none;">
                        <i class="bi bi-exclamation-triangle me-1"></i>대출금 입금/출금 계좌와 다른 계좌로 변경 입력 중입니다.
                    </div>
                </div>

                <!-- 1원 송금 요청 버튼 -->
                <div class="d-grid mb-3" id="reqTransferBtnWrapper">
                    <button type="button" id="btnRequestTransfer" class="btn btn-primary btn-lg fw-bold" onclick="requestOneWonTransfer()">
                        <i class="bi bi-send-fill me-1"></i>1원 송금하기
                    </button>
                </div>

                <!-- 1원 인증번호 입력 섹션 (송금 후 슬라이드 다운) -->
                <div id="verifySection" class="card border-primary p-3 mb-3 bg-light" style="display: none;">
                    <div class="d-flex justify-content-between align-items-center mb-2">
                        <h6 class="fw-bold text-primary mb-0">
                            <i class="bi bi-shield-check me-1"></i>1원 입금자명 확인 및 인증코드 입력
                        </h6>
                        <span class="timer-badge" id="otpTimer"><i class="bi bi-clock me-1"></i>10:00</span>
                    </div>

                    <!-- 통장 거래내역 미리보기 일러스트 -->
                    <div class="passbook-preview mb-3">
                        <div class="small text-muted mb-1">고객님의 은행 앱 거래내역 표기 예시</div>
                        <div class="d-flex justify-content-center align-items-center gap-2">
                            <span class="badge bg-secondary">입금 1원</span>
                            <span class="fw-bold fs-5 text-dark" id="displayPassbookText">저축[ 4자리 ]</span>
                        </div>
                        <small class="text-secondary" style="font-size: 11px;">
                            * 입금자명 뒤의 <strong>숫자 4자리</strong>를 아래에 입력하세요.
                        </small>
                    </div>

                    <!-- 개발/테스트 편의 자동입력 알림 -->
                    <div id="testAuthGuide" class="alert alert-info py-2 small mb-3"></div>

                    <!-- 4자리 입력창 -->
                    <div class="mb-3">
                        <div class="d-flex justify-content-between align-items-center mb-1">
                            <label for="authCode" class="form-label small fw-bold mb-0">인증코드 (숫자 4자리)</label>
                            <span class="small text-muted" id="failCountDisplay">오류 횟수: 0 / 5회</span>
                        </div>
                        <div class="input-group">
                            <span class="input-group-text fw-bold text-primary">저축</span>
                            <input type="text" id="authCode" class="form-control form-control-lg text-center fw-bold letter-spacing-3 font-monospace" 
                                   placeholder="0000" maxlength="4">
                            <button type="button" class="btn btn-outline-secondary" onclick="requestOneWonTransfer()">재전송</button>
                        </div>
                    </div>

                    <button type="button" id="btnVerify" class="btn btn-success btn-lg fw-bold w-100" onclick="verifyOneWon()">
                        <i class="bi bi-check2-circle me-1"></i>인증 완료
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>

<!-- ================= 1원 계좌인증 성공 모달 ================= -->
<div class="modal fade" id="certSuccessModal" tabindex="-1" aria-labelledby="certSuccessModalLabel" aria-hidden="true" data-bs-backdrop="static">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
            <div class="modal-header bg-success text-white">
                <h5 class="modal-title fw-bold" id="certSuccessModalLabel">
                    <i class="bi bi-check-circle-fill me-1"></i>계좌 본인확인 완료
                </h5>
            </div>
            <div class="modal-body p-4 text-center">
                <div class="display-4 text-success mb-2"><i class="bi bi-patch-check-fill"></i></div>
                <h5 class="fw-bold mb-1" id="resCustNm">홍길동 고객님</h5>
                <p class="text-muted small mb-3">타행 본인계좌 1원 인증이 정상 완료되었습니다.</p>

                <div class="table-responsive text-start">
                    <table class="table table-bordered table-sm small mb-3">
                        <tbody>
                            <tr>
                                <th class="table-light text-nowrap" style="width: 35%;">인증 은행</th>
                                <td id="resBankNm" class="fw-bold"></td>
                            </tr>
                            <tr>
                                <th class="table-light">인증 계좌번호</th>
                                <td id="resAccountNo" class="font-monospace"></td>
                            </tr>
                            <tr>
                                <th class="table-light">예금주명</th>
                                <td id="resHolderNm"></td>
                            </tr>
                            <tr>
                                <th class="table-light">검증 확인토큰</th>
                                <td id="resToken" class="small text-break font-monospace text-muted"></td>
                            </tr>
                        </tbody>
                    </table>
                </div>

                <div id="loanNextActionGuide" class="alert alert-primary py-2 small mb-0" style="display: none;">
                    <i class="bi bi-arrow-right-circle me-1"></i>대출신청 대출금 입출금계좌 확인이 완료되어 다음 단계로 진행할 수 있습니다.
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-primary w-100 fw-bold" onclick="onCompleteConfirm()">
                    확인
                </button>
            </div>
        </div>
    </div>
</div>

<script>
    var currentPurpose = '${purpose}' || 'COMMON';
    var currentLoanNo = '${loanNo}' || 'LN202610130001';
    var originalLoanAccount = null; // 대출신청 등록 원본 계좌
    var currentTxSeq = '';
    var timerInterval = null;
    var remainingSeconds = 600;
    var isAccountModified = 'N';

    $(document).ready(function() {
        // 1. 은행 목록 로드
        loadBankList(function() {
            // 목적에 따라 초기화
            if (currentPurpose === 'LOAN_APPLY') {
                switchPurpose('LOAN_APPLY');
            } else {
                switchPurpose('COMMON');
            }
        });

        // 계좌번호 수정 시 원본과 비교 감지
        $('#accountNo, #bankCd').on('input change', function() {
            checkIfModified();
        });
    });

    // 은행 목록 불러오기
    function loadBankList(callback) {
        Common.ajax({
            url: '/api/common/auth/account-cert/banks',
            type: 'GET',
            success: function(list) {
                var html = '<option value="">은행을 선택해 주세요</option>';
                $.each(list, function(idx, bank) {
                    html += '<option value="' + bank.bankCd + '">' + escapeHtml(bank.bankNm) + '</option>';
                });
                $('#bankCd').html(html);
                if (callback) callback();
            }
        });
    }

    // 모드 전환 (대출신청 연계 vs 타 업무 공통)
    function switchPurpose(purpose) {
        currentPurpose = purpose;
        resetVerifySection();

        if (purpose === 'LOAN_APPLY') {
            $('#loanModeBanner').slideDown();
            $('#commonModeBanner').slideUp();
            $('#modifyToggleWrapper').show();
            loadLoanAccount(currentLoanNo);
        } else {
            $('#loanModeBanner').slideUp();
            $('#commonModeBanner').slideDown();
            $('#modifyToggleWrapper').hide();
            $('#btnRestoreOriginal').hide();
            $('#modifyAlert').hide();
            enableInputs(true);
            isAccountModified = 'N';
        }
    }

    // 대출신청 계좌 정보 불러와 자동완성 처리 (요구사항 1)
    function loadLoanAccount(loanNo) {
        Common.ajax({
            url: '/api/common/auth/account-cert/loan-account?loanNo=' + encodeURIComponent(loanNo),
            type: 'GET',
            success: function(data) {
                if (data) {
                    originalLoanAccount = data;
                    currentLoanNo = data.loanNo;
                    $('#loanBadgeNo').text('대출번호: ' + data.loanNo);
                    $('#loanProductDesc').html('• 신청 상품: <strong>' + escapeHtml(data.productNm) + '</strong><br>' +
                                               '• 대출신청 단계에 등록된 계좌정보를 <strong>자동완성</strong> 처리하였습니다.');
                    
                    $('#custNm').val(data.accountHolderNm);
                    $('#bankCd').val(data.bankCd);
                    $('#accountNo').val(data.accountNo);

                    // 요구사항 1: 불러온 후 기본적으로 폼 잠금(자동완성 상태)
                    enableInputs(false);
                    $('#btnToggleModify').show();
                    $('#btnRestoreOriginal').hide();
                    $('#modifyAlert').hide();
                    isAccountModified = 'N';
                }
            },
            error: function() {
                alert('대출신청 계좌 정보를 불러오지 못했습니다. 일반 입력 모드로 전환합니다.');
                switchPurpose('COMMON');
            }
        });
    }

    // 수정기능 토글 (요구사항 2: 은행/계좌 선택 및 입력 가능)
    function toggleAccountModification() {
        var isDisabled = $('#bankCd').prop('disabled');
        if (isDisabled) {
            // 수정 활성화
            enableInputs(true);
            $('#btnToggleModify').removeClass('btn-outline-secondary').addClass('btn-secondary').text('수정 중');
            $('#btnRestoreOriginal').show();
            $('#accountNo').focus();
        } else {
            // 수정 취소
            restoreOriginalLoanAccount();
        }
    }

    // 원래 대출계좌로 복원
    function restoreOriginalLoanAccount() {
        if (originalLoanAccount) {
            $('#bankCd').val(originalLoanAccount.bankCd);
            $('#accountNo').val(originalLoanAccount.accountNo);
            $('#custNm').val(originalLoanAccount.accountHolderNm);
            enableInputs(false);
            $('#btnToggleModify').removeClass('btn-secondary').addClass('btn-outline-secondary').html('<i class="bi bi-pencil-square me-1"></i>계좌 변경하기');
            $('#btnRestoreOriginal').hide();
            $('#modifyAlert').hide();
            isAccountModified = 'N';
        }
    }

    function checkIfModified() {
        if (currentPurpose === 'LOAN_APPLY' && originalLoanAccount) {
            var currentBank = $('#bankCd').val();
            var currentAcc = $('#accountNo').val().replace(/[^0-9]/g, '');
            var origAcc = originalLoanAccount.accountNo.replace(/[^0-9]/g, '');

            if (currentBank !== originalLoanAccount.bankCd || currentAcc !== origAcc) {
                $('#modifyAlert').show();
                isAccountModified = 'Y';
            } else {
                $('#modifyAlert').hide();
                isAccountModified = 'N';
            }
        }
    }

    function enableInputs(enabled) {
        $('#bankCd').prop('disabled', !enabled);
        $('#accountNo').prop('readonly', !enabled);
    }

    // 프리셋 적용 함수 (개발/검증 편의)
    function applyLoanPreset(loanNo) {
        $('#tab-loan').tab('show');
        currentLoanNo = loanNo;
        switchPurpose('LOAN_APPLY');
    }

    function applyMockErrorPreset(errorType) {
        $('#tab-common').tab('show');
        switchPurpose('COMMON');
        $('#custNm').val('홍길동');
        $('#bankCd').val('004'); // KB국민은행
        if (errorType === 'SUSPENDED') {
            $('#accountNo').val('12345678888'); // 뒷자리 8888: 거래정지 시뮬레이션
        }
    }

    // 1원 송금 요청
    function requestOneWonTransfer() {
        var custNm = $('#custNm').val().trim();
        var bankCd = $('#bankCd').val();
        var accountNo = $('#accountNo').val().trim();

        if (!custNm) { alert('예금주명을 입력하세요.'); $('#custNm').focus(); return; }
        if (!bankCd) { alert('입금 은행을 선택하세요.'); $('#bankCd').focus(); return; }
        if (!accountNo) { alert('계좌번호를 입력하세요.'); $('#accountNo').focus(); return; }

        var reqData = {
            purpose: currentPurpose,
            loanNo: (currentPurpose === 'LOAN_APPLY') ? currentLoanNo : null,
            custNm: custNm,
            bankCd: bankCd,
            accountNo: accountNo,
            isModified: isAccountModified
        };

        Common.ajax({
            url: '/api/common/auth/account-cert/request-transfer',
            type: 'POST',
            data: reqData,
            success: function(res) {
                currentTxSeq = res.reqTxSeq;
                $('#verifySection').slideDown();
                $('#reqTransferBtnWrapper').hide();
                $('#displayPassbookText').text(res.printText);
                $('#testAuthGuide').html('🧪 [테스트 자동입력] 통장 인자문구: <strong>' + escapeHtml(res.printText) + '</strong> | 인증번호: <strong>' + res.testAuthCode + '</strong>');
                $('#authCode').val(res.testAuthCode).focus();
                $('#failCountDisplay').text('오류 횟수: 0 / 5회');

                startTimer(res.expiresInSeconds || 600);
            }
        });
    }

    // 1원 인증코드 검증
    function verifyOneWon() {
        var authCode = $('#authCode').val().trim();
        if (!authCode || authCode.length !== 4) {
            alert('인증코드 4자리를 정확히 입력해 주세요.');
            $('#authCode').focus();
            return;
        }

        var verifyData = {
            reqTxSeq: currentTxSeq,
            authCode: authCode
        };

        Common.ajax({
            url: '/api/common/auth/account-cert/verify',
            type: 'POST',
            data: verifyData,
            success: function(res) {
                clearInterval(timerInterval);
                showSuccessModal(res);
            },
            error: function(err) {
                // 실패 카운트 증가 표시
                if (err && err.responseJSON && err.responseJSON.code === 'AC06') {
                    // 오류 횟수 갱신
                }
            }
        });
    }

    function showSuccessModal(data) {
        $('#resCustNm').text(data.accountHolderNm + ' 고객님');
        $('#resBankNm').text(data.bankNm);
        $('#resAccountNo').text(data.accountNoMasked);
        $('#resHolderNm').text(data.accountHolderNm);
        $('#resToken').text(data.authConfirmToken);

        if (currentPurpose === 'LOAN_APPLY') {
            $('#loanNextActionGuide').show();
        } else {
            $('#loanNextActionGuide').hide();
        }

        var modal = new bootstrap.Modal(document.getElementById('certSuccessModal'));
        modal.show();
    }

    function onCompleteConfirm() {
        if (currentPurpose === 'LOAN_APPLY') {
            alert('대출신청 프로세스로 이동합니다.');
            location.href = '/board/list'; // 포털 메인/대시보드로 복귀
        } else {
            location.reload();
        }
    }

    function resetVerifySection() {
        clearInterval(timerInterval);
        $('#verifySection').hide();
        $('#reqTransferBtnWrapper').show();
        $('#authCode').val('');
    }

    function startTimer(seconds) {
        clearInterval(timerInterval);
        remainingSeconds = seconds;
        updateTimerDisplay();

        timerInterval = setInterval(function() {
            remainingSeconds--;
            if (remainingSeconds <= 0) {
                clearInterval(timerInterval);
                $('#otpTimer').text('시간 만료');
                alert('1원 계좌인증 유효시간(10분)이 만료되었습니다. 다시 송금 요청해 주세요.');
                resetVerifySection();
            } else {
                updateTimerDisplay();
            }
        }, 1000);
    }

    function updateTimerDisplay() {
        var min = Math.floor(remainingSeconds / 60);
        var sec = remainingSeconds % 60;
        var display = (min < 10 ? '0' : '') + min + ':' + (sec < 10 ? '0' : '') + sec;
        $('#otpTimer').html('<i class="bi bi-clock me-1"></i>' + display);
    }

    function escapeHtml(text) {
        if (!text) return '';
        return text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
    }
</script>

</body>
</html>
