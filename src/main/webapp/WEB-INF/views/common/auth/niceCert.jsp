<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>저축은행 포털 - 휴대폰 본인인증 (NICE ID)</title>
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
        body { background-color: #f4f6f9; font-family: 'Malgun Gothic', 'Noto Sans KR', sans-serif; }
        .card-header-financial { background-color: #0d47a1; color: white; font-weight: bold; }
        .nav-tabs .nav-link { font-weight: 600; color: #495057; }
        .nav-tabs .nav-link.active { color: #0d47a1; font-weight: bold; border-bottom: 3px solid #0d47a1; }
        .terms-box { background-color: #f8f9fa; border: 1px solid #dee2e6; border-radius: 6px; padding: 15px; }
        .timer-text { font-weight: bold; color: #d32f2f; }
        .preset-btn { font-size: 13px; margin-right: 4px; margin-bottom: 4px; }
        .badge-as-is { background-color: #6c757d; }
        .badge-to-be { background-color: #0d47a1; }
    </style>
</head>
<body>
<div class="container py-5" style="max-width: 680px;">
    <!-- 상단 헤더 카드 -->
    <div class="card shadow-sm mb-4">
        <div class="card-header card-header-financial py-3 d-flex justify-content-between align-items-center">
            <h4 class="mb-0 fs-5"><i class="bi bi-shield-lock-fill me-2"></i>공통업무 | 휴대폰 본인인증 (NICE ID)</h4>
            <span class="badge bg-light text-primary fw-bold">APP / PC Web 공용</span>
        </div>
        <div class="card-body">
            <!-- 개선 안내 배너 -->
            <div class="alert alert-primary mb-4" role="alert">
                <div class="d-flex align-items-center mb-2">
                    <span class="badge badge-to-be me-2">TO-BE 개편</span>
                    <strong>통신사 미지정 방식 & 기기 점유 MO 인증 도입</strong>
                </div>
                <ul class="mb-0 ps-3 small text-secondary">
                    <li><strong class="text-dark">통신사 선택 불필요</strong>: 통신 3사 및 알뜰폰 선택 과정 없이 성명+생년월일+전화번호만 입력합니다.</li>
                    <li><strong class="text-dark">비여신금융거래 약관</strong>: 통신사별 개별 동의 대신 통합 본인확인 약관으로 간소화되었습니다.</li>
                    <li><strong class="text-dark">기기 점유 MO 인증</strong>: 모바일에서 원터치 문자 전송으로 휴대폰번호 수기 입력 생략 및 단말 점유를 검증합니다.</li>
                </ul>
            </div>

            <!-- 테스트 시뮬레이션 프리셋 (개발/검증용) -->
            <div class="card bg-light border-0 mb-4">
                <div class="card-body py-2 px-3">
                    <div class="d-flex justify-content-between align-items-center mb-1">
                        <small class="fw-bold text-dark"><i class="bi bi-sliders me-1"></i>영향도 검토 시뮬레이션 프리셋:</small>
                        <small class="text-muted">원클릭 자동 입력</small>
                    </div>
                    <div class="d-flex flex-wrap">
                        <button type="button" class="btn btn-outline-primary btn-sm preset-btn" onclick="applyPreset('NORMAL')">
                            ✅ 1. 정상 성인 고객
                        </button>
                        <button type="button" class="btn btn-outline-warning btn-sm preset-btn" onclick="applyPreset('MINOR')">
                            ⚠️ 2. 미성년자 (만 14세)
                        </button>
                        <button type="button" class="btn btn-outline-info btn-sm preset-btn" onclick="applyPreset('FOREIGNER')">
                            🌐 3. 외국인 고객
                        </button>
                        <button type="button" class="btn btn-outline-danger btn-sm preset-btn" onclick="applyPreset('WATCHLIST')">
                            🚫 4. 사고유의고객 (차단)
                        </button>
                    </div>
                </div>
            </div>

            <!-- 인증 방식 선택 탭 -->
            <ul class="nav nav-tabs nav-fill mb-3" id="authMethodTab" role="tablist">
                <li class="nav-item" role="presentation">
                    <button class="nav-link active" id="sms-tab" data-bs-toggle="tab" data-bs-target="#sms-pane" type="button" role="tab">
                        <i class="bi bi-chat-dots-fill me-1"></i>통신사 미지정 SMS 인증
                    </button>
                </li>
                <li class="nav-item" role="presentation">
                    <button class="nav-link" id="mo-tab" data-bs-toggle="tab" data-bs-target="#mo-pane" type="button" role="tab">
                        <i class="bi bi-phone-fill me-1"></i>기기 점유 MO 인증 (원터치 문자)
                    </button>
                </li>
            </ul>

            <!-- 탭 본문 -->
            <div class="tab-content" id="authTabContent">
                <!-- 탭 1: 통신사 미지정 SMS 인증 -->
                <div class="tab-pane fade show active" id="sms-pane" role="tabpanel">
                    <form id="smsAuthForm">
                        <div class="mb-3">
                            <label for="custNm" class="form-label fw-bold">성명 <span class="text-danger">*</span></label>
                            <input type="text" id="custNm" class="form-control" placeholder="성명을 입력하세요" required>
                        </div>

                        <div class="row mb-3">
                            <div class="col-8">
                                <label for="birthDate" class="form-label fw-bold">생년월일 <span class="text-danger">*</span></label>
                                <input type="text" id="birthDate" class="form-control" placeholder="YYYYMMDD (예: 19900515)" maxlength="8" required>
                            </div>
                            <div class="col-4">
                                <label for="gender" class="form-label fw-bold">성별</label>
                                <select id="gender" class="form-select">
                                    <option value="1">남성</option>
                                    <option value="2">여성</option>
                                </select>
                            </div>
                        </div>

                        <!-- TO-BE: 통신사 선택 필드가 완전히 생략됨 -->
                        <div class="mb-3">
                            <div class="d-flex justify-content-between align-items-center mb-1">
                                <label for="cellNo" class="form-label fw-bold mb-0">휴대폰번호 <span class="text-danger">*</span></label>
                                <span class="badge bg-success-subtle text-success border border-success-subtle small">
                                    <i class="bi bi-check-circle me-1"></i>통신사 자동식별
                                </span>
                            </div>
                            <input type="tel" id="cellNo" class="form-control" placeholder="'-' 없이 숫자만 입력 (01012345678)" maxlength="11" required>
                        </div>

                        <!-- 비여신금융거래 약관 영역 -->
                        <div class="terms-box mb-4">
                            <div class="form-check pb-2 mb-2 border-bottom">
                                <input class="form-check-input" type="checkbox" id="checkAllTerms">
                                <label class="form-check-label fw-bold text-dark" for="checkAllTerms">
                                    비여신금융거래 본인확인 전체 약관에 동의합니다. (필수)
                                </label>
                            </div>
                            <div id="termsListContainer">
                                <!-- AJAX 동적 렌더링 -->
                            </div>
                        </div>

                        <!-- 인증번호 요청 버튼 -->
                        <div class="d-grid mb-3" id="reqOtpBtnWrapper">
                            <button type="button" id="btnRequestOtp" class="btn btn-primary btn-lg fw-bold">
                                <i class="bi bi-send-fill me-1"></i>인증번호 발송
                            </button>
                        </div>

                        <!-- 인증번호 입력 영역 (요청 후 노출) -->
                        <div id="otpInputSection" class="card bg-light p-3 mb-3" style="display: none;">
                            <label for="otpCode" class="form-label fw-bold d-flex justify-content-between">
                                <span>인증번호 입력 (6자리)</span>
                                <span class="timer-text" id="otpTimer"><i class="bi bi-clock me-1"></i>03:00</span>
                            </label>
                            <div class="input-group mb-2">
                                <input type="text" id="otpCode" class="form-control form-control-lg text-center letter-spacing-2" placeholder="000000" maxlength="6">
                                <button type="button" id="btnResendOtp" class="btn btn-outline-secondary">재발송</button>
                            </div>
                            <div id="testOtpGuide" class="small text-muted mb-3"></div>

                            <button type="button" id="btnVerifyOtp" class="btn btn-success btn-lg fw-bold w-100">
                                <i class="bi bi-check2-circle me-1"></i>본인인증 완료
                            </button>
                        </div>
                    </form>
                </div>

                <!-- 탭 2: 기기 점유 MO 인증 -->
                <div class="tab-pane fade" id="mo-pane" role="tabpanel">
                    <div class="p-3 bg-light rounded mb-3">
                        <h6 class="fw-bold mb-2 text-primary"><i class="bi bi-phone-vibrate me-1"></i>MO(Mobile Originated) 인증이란?</h6>
                        <p class="small text-muted mb-0">
                            스마트폰에서 지정된 가상 번호로 인증 문자를 전송하여, <strong>휴대폰번호 입력 없이도</strong> 
                            고객 단말기의 실제 점유/소지 여부와 명의를 동시에 검증하는 최고 보안 인증 방식입니다.
                        </p>
                    </div>

                    <form id="moAuthForm">
                        <div class="mb-3">
                            <label for="moCustNm" class="form-label fw-bold">성명 <span class="text-danger">*</span></label>
                            <input type="text" id="moCustNm" class="form-control" placeholder="성명을 입력하세요" required>
                        </div>

                        <div class="mb-3">
                            <label for="moBirthDate" class="form-label fw-bold">생년월일 <span class="text-danger">*</span></label>
                            <input type="text" id="moBirthDate" class="form-control" placeholder="YYYYMMDD (예: 19900515)" maxlength="8" required>
                        </div>

                        <div class="alert alert-secondary small mb-4">
                            <i class="bi bi-info-circle me-1"></i>MO 인증 방식은 고객님의 휴대폰 발신 번호를 통해 번호가 자동 확인되므로 번호 입력이 필요 없습니다.
                        </div>

                        <div class="d-grid mb-3" id="reqMoBtnWrapper">
                            <button type="button" id="btnRequestMo" class="btn btn-primary btn-lg fw-bold">
                                <i class="bi bi-envelope-paper-fill me-1"></i>MO 인증 세션 생성
                            </button>
                        </div>

                        <!-- MO 발송 안내 영역 (요청 후 노출) -->
                        <div id="moActionSection" class="card border-primary p-3 mb-3 text-center" style="display: none;">
                            <h6 class="fw-bold text-primary mb-2">아래 번호로 인증 문자를 전송해 주세요</h6>
                            <div class="bg-white p-3 border rounded mb-3">
                                <div class="text-muted small">수신 번호</div>
                                <div class="fs-4 fw-bold text-dark mb-2" id="moVirtualPhone">1600-1588</div>
                                <div class="text-muted small">메시지 내용</div>
                                <div class="badge bg-light text-primary border fs-6 py-2 px-3" id="moMessageBody">[NICE본인확인 0000]</div>
                            </div>

                            <a id="moSmsLink" href="#" class="btn btn-warning btn-lg fw-bold mb-2">
                                <i class="bi bi-send me-1"></i>스마트폰 문자 전송하기 (원터치 앱 열기)
                            </a>

                            <button type="button" id="btnCheckMoStatus" class="btn btn-success fw-bold">
                                <i class="bi bi-arrow-repeat me-1"></i>문자 전송 완료 후 확인
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>
</div>

<!-- 약관 전문 보기 모달 -->
<div class="modal fade" id="termsModal" tabindex="-1" aria-labelledby="termsModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-scrollable modal-lg">
        <div class="modal-content">
            <div class="modal-header bg-light">
                <h5 class="modal-title fw-bold" id="termsModalLabel">약관 전문</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body" id="termsModalBody" style="white-space: pre-line; font-size: 14px; line-height: 1.6;">
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-primary" data-bs-dismiss="modal">확인</button>
            </div>
        </div>
    </div>
</div>

<!-- 본인인증 성공 및 종합 리스크 체크 결과 모달 -->
<div class="modal fade" id="certResultModal" tabindex="-1" aria-labelledby="certResultModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
            <div class="modal-header bg-primary text-white">
                <h5 class="modal-title fw-bold" id="certResultModalLabel">
                    <i class="bi bi-shield-check me-1"></i>NICE 본인인증 및 리스크 검증 결과
                </h5>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <div class="text-center mb-4">
                    <div class="display-5 text-success mb-2"><i class="bi bi-check-circle-fill"></i></div>
                    <h5 class="fw-bold mb-1" id="resCustNm">홍길동 고객님</h5>
                    <p class="text-muted small" id="resAuthSummary">통신사 미지정 본인인증이 완료되었습니다.</p>
                </div>

                <div class="table-responsive">
                    <table class="table table-bordered table-sm small">
                        <tbody>
                            <tr>
                                <th class="table-light text-nowrap" style="width: 35%;">인증 방식</th>
                                <td id="resAuthMethod">통신사 미지정 SMS</td>
                            </tr>
                            <tr>
                                <th class="table-light">연계정보(CI)</th>
                                <td id="resCiMasked" class="text-break font-monospace"></td>
                            </tr>
                            <tr>
                                <th class="table-light">생년월일/성별</th>
                                <td id="resBirthGender"></td>
                            </tr>
                            <tr>
                                <th class="table-light">휴대폰번호</th>
                                <td id="resCellNo"></td>
                            </tr>
                        </tbody>
                    </table>
                </div>

                <!-- 비즈니스 체크 로직 영향도 검증 결과 영역 -->
                <div class="card border-0 bg-light p-3 mt-3">
                    <h6 class="fw-bold mb-2 text-dark"><i class="bi bi-clipboard-check me-1"></i>체크로직 영향도 판정:</h6>
                    
                    <!-- 1. 유의고객 체크 -->
                    <div class="d-flex justify-content-between align-items-center mb-1 small">
                        <span>• 유의고객(Watchlist/FDS):</span>
                        <span id="badgeWatchlist" class="badge bg-success">정상 (미해당)</span>
                    </div>

                    <!-- 2. 미성년자 체크 -->
                    <div class="d-flex justify-content-between align-items-center mb-1 small">
                        <span>• 미성년자(만 19세 기준):</span>
                        <span id="badgeMinor" class="badge bg-success">성인 (만 36세)</span>
                    </div>
                    <div id="guideMinor" class="text-warning small ps-2 mb-1" style="display:none;"></div>

                    <!-- 3. 외국인 체크 -->
                    <div class="d-flex justify-content-between align-items-center small">
                        <span>• 내외국인 구분:</span>
                        <span id="badgeForeigner" class="badge bg-primary">내국인</span>
                    </div>
                    <div id="guideForeigner" class="text-info small ps-2 mt-1" style="display:none;"></div>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-primary w-100" data-bs-dismiss="modal">확인</button>
            </div>
        </div>
    </div>
</div>

<script>
    var termsDataMap = {};
    var currentTxSeq = '';
    var currentSimulationMode = 'NORMAL';
    var timerInterval = null;
    var remainingSeconds = 180;

    $(document).ready(function() {
        loadTerms();

        // 약관 전체 동의 체크박스 이벤트
        $('#checkAllTerms').change(function() {
            var checked = $(this).is(':checked');
            $('.term-item-check').prop('checked', checked);
        });

        $(document).on('change', '.term-item-check', function() {
            var total = $('.term-item-check').length;
            var checked = $('.term-item-check:checked').length;
            $('#checkAllTerms').prop('checked', total === checked);
        });

        // 탭 전환 시 폼 리셋
        $('button[data-bs-toggle="tab"]').on('shown.bs.tab', function(e) {
            clearInterval(timerInterval);
            $('#otpInputSection').hide();
            $('#reqOtpBtnWrapper').show();
            $('#moActionSection').hide();
            $('#reqMoBtnWrapper').show();
        });

        // SMS 인증번호 요청 버튼
        $('#btnRequestOtp').click(function() {
            requestSmsOtp();
        });

        // SMS 인증번호 재발송 버튼
        $('#btnResendOtp').click(function() {
            requestSmsOtp();
        });

        // SMS 인증번호 검증 버튼
        $('#btnVerifyOtp').click(function() {
            verifySmsOtp();
        });

        // MO 인증 요청 버튼
        $('#btnRequestMo').click(function() {
            requestMoAuth();
        });

        // MO 인증 확인 버튼
        $('#btnCheckMoStatus').click(function() {
            checkMoStatus();
        });
    });

    // 1. 비여신금융거래 약관 목록 불러오기
    function loadTerms() {
        Common.ajax({
            url: '/api/common/auth/nice/terms',
            type: 'GET',
            success: function(list) {
                var html = '';
                $.each(list, function(idx, item) {
                    termsDataMap[item.termsId] = item;
                    var reqBadge = item.requiredYn === 'Y' ? '<span class="text-danger fw-bold">[필수]</span>' : '<span class="text-muted">[선택]</span>';

                    html += '<div class="d-flex justify-content-between align-items-center py-1">';
                    html += '  <div class="form-check mb-0">';
                    html += '    <input class="form-check-input term-item-check" type="checkbox" id="chk_' + item.termsId + '" value="' + item.termsId + '">';
                    html += '    <label class="form-check-label small" for="chk_' + item.termsId + '">';
                    html += '      ' + reqBadge + ' ' + escapeHtml(item.termsTitle);
                    html += '    </label>';
                    html += '  </div>';
                    html += '  <button type="button" class="btn btn-link btn-sm text-decoration-none py-0 small" onclick="openTermsModal(\'' + item.termsId + '\')">전문보기 &gt;</button>';
                    html += '</div>';
                });
                $('#termsListContainer').html(html);
            }
        });
    }

    function openTermsModal(termsId) {
        var term = termsDataMap[termsId];
        if (term) {
            $('#termsModalLabel').text(term.termsTitle);
            $('#termsModalBody').text(term.content);
            var modal = new bootstrap.Modal(document.getElementById('termsModal'));
            modal.show();
        }
    }

    // 2. 테스트 프리셋 적용 (영향도 검토 편의 기능)
    function applyPreset(mode) {
        currentSimulationMode = mode;
        if (mode === 'NORMAL') {
            $('#custNm, #moCustNm').val('홍길동');
            $('#birthDate, #moBirthDate').val('19900515');
            $('#cellNo').val('01012345678');
            $('#gender').val('1');
        } else if (mode === 'MINOR') {
            $('#custNm, #moCustNm').val('이학생');
            $('#birthDate, #moBirthDate').val('20120301'); // 만 14세 미성년자
            $('#cellNo').val('01022223333');
            $('#gender').val('1');
        } else if (mode === 'FOREIGNER') {
            $('#custNm, #moCustNm').val('존스미스');
            $('#birthDate, #moBirthDate').val('19881105');
            $('#cellNo').val('01044445555');
            $('#gender').val('1');
        } else if (mode === 'WATCHLIST') {
            $('#custNm, #moCustNm').val('사고유의자');
            $('#birthDate, #moBirthDate').val('19820710');
            $('#cellNo').val('01099991111');
            $('#gender').val('1');
        }
        $('#checkAllTerms').prop('checked', true).trigger('change');
    }

    // 3. 통신사 미지정 SMS 인증 요청
    function requestSmsOtp() {
        var custNm = $('#custNm').val().trim();
        var birthDate = $('#birthDate').val().trim();
        var cellNo = $('#cellNo').val().trim();

        if (!custNm) { alert('성명을 입력하세요.'); $('#custNm').focus(); return; }
        if (!birthDate || birthDate.length !== 8) { alert('생년월일 8자리를 입력하세요 (예: 19900515).'); $('#birthDate').focus(); return; }
        if (!cellNo || cellNo.length < 10) { alert('휴대폰번호를 정확히 입력하세요.'); $('#cellNo').focus(); return; }

        var agreedTermsIds = [];
        $('.term-item-check:checked').each(function() {
            agreedTermsIds.push($(this).val());
        });

        if (agreedTermsIds.length < $('.term-item-check').length) {
            alert('필수 약관에 모두 동의해 주세요.');
            return;
        }

        var reqData = {
            custNm: custNm,
            birthDate: birthDate,
            gender: $('#gender').val(),
            cellNo: cellNo,
            channelType: 'PC_WEB',
            authMethod: 'SMS_NO_CARRIER',
            agreedTermsIds: agreedTermsIds,
            simulationMode: currentSimulationMode
        };

        Common.ajax({
            url: '/api/common/auth/nice/request-otp',
            type: 'POST',
            data: reqData,
            success: function(res) {
                currentTxSeq = res.reqTxSeq;
                $('#reqOtpBtnWrapper').hide();
                $('#otpInputSection').slideDown();
                $('#otpCode').val(res.testOtpCode).focus();
                $('#testOtpGuide').html('🧪 [테스트 자동입력] 인증번호: <strong>' + res.testOtpCode + '</strong>');
                startOtpTimer(res.expiresInSeconds || 180);
            }
        });
    }

    function startOtpTimer(seconds) {
        clearInterval(timerInterval);
        remainingSeconds = seconds;
        updateTimerDisplay();

        timerInterval = setInterval(function() {
            remainingSeconds--;
            if (remainingSeconds <= 0) {
                clearInterval(timerInterval);
                $('#otpTimer').text('시간 만료');
                alert('인증번호 유효시간이 만료되었습니다. 재발송을 눌러주세요.');
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

    // 4. SMS 인증번호 검증
    function verifySmsOtp() {
        var otpCode = $('#otpCode').val().trim();
        if (!otpCode || otpCode.length !== 6) {
            alert('인증번호 6자리를 입력하세요.');
            $('#otpCode').focus();
            return;
        }

        var verifyData = {
            reqTxSeq: currentTxSeq,
            otpCode: otpCode
        };

        Common.ajax({
            url: '/api/common/auth/nice/verify-otp',
            type: 'POST',
            data: verifyData,
            success: function(data) {
                clearInterval(timerInterval);
                showResultModal(data);
            }
        });
    }

    // 5. MO 인증 요청
    function requestMoAuth() {
        var custNm = $('#moCustNm').val().trim();
        var birthDate = $('#moBirthDate').val().trim();

        if (!custNm) { alert('성명을 입력하세요.'); $('#moCustNm').focus(); return; }
        if (!birthDate || birthDate.length !== 8) { alert('생년월일 8자리를 입력하세요.'); $('#moBirthDate').focus(); return; }

        var agreedTermsIds = [];
        $('.term-item-check:checked').each(function() {
            agreedTermsIds.push($(this).val());
        });

        var reqData = {
            custNm: custNm,
            birthDate: birthDate,
            channelType: 'APP',
            agreedTermsIds: agreedTermsIds,
            simulationMode: currentSimulationMode
        };

        Common.ajax({
            url: '/api/common/auth/nice/request-mo',
            type: 'POST',
            data: reqData,
            success: function(res) {
                currentTxSeq = res.reqTxSeq;
                $('#reqMoBtnWrapper').hide();
                $('#moActionSection').slideDown();
                $('#moVirtualPhone').text(res.moVirtualPhone);
                $('#moMessageBody').text(res.moMessageBody);
                $('#moSmsLink').attr('href', res.smsUri);
            }
        });
    }

    function checkMoStatus() {
        Common.ajax({
            url: '/api/common/auth/nice/check-mo-status',
            type: 'GET',
            data: { reqTxSeq: currentTxSeq },
            success: function(data) {
                showResultModal(data);
            }
        });
    }

    // 6. 결과 모달 표시 및 영향도 검토 판정 매핑
    function showResultModal(data) {
        $('#resCustNm').text(data.custNm + ' 고객님');
        $('#resAuthMethod').text(data.authMethod === 'SMS_NO_CARRIER' ? '통신사 미지정 SMS 인증' : '기기 점유 MO 인증');
        $('#resCiMasked').text(data.ciMasked);
        $('#resBirthGender').text(data.birthDate + ' (' + (data.gender === '1' ? '남성' : '여성') + ')');
        $('#resCellNo').text(data.cellNoMasked);

        var risk = data.riskCheck;
        if (risk) {
            // 유의고객 배지
            if (risk.watchlist) {
                $('#badgeWatchlist').attr('class', 'badge bg-danger').text('유의고객 (' + risk.riskGrade + ')');
            } else {
                $('#badgeWatchlist').attr('class', 'badge bg-success').text('정상 (미해당)');
            }

            // 미성년자 배지 & 가이드
            if (risk.minor) {
                $('#badgeMinor').attr('class', 'badge bg-warning text-dark').text('미성년자 (만 ' + risk.exactAge + '세)');
                $('#guideMinor').show().text('⚠️ ' + risk.minorGuideMessage);
            } else {
                $('#badgeMinor').attr('class', 'badge bg-success').text('성인 (만 ' + risk.exactAge + '세)');
                $('#guideMinor').hide();
            }

            // 외국인 배지 & 가이드
            if (risk.foreigner) {
                $('#badgeForeigner').attr('class', 'badge bg-info text-dark').text('외국인');
                $('#guideForeigner').show().text('ℹ️ ' + risk.foreignerGuideMessage);
            } else {
                $('#badgeForeigner').attr('class', 'badge bg-primary').text('내국인');
                $('#guideForeigner').hide();
            }
        }

        var resultModal = new bootstrap.Modal(document.getElementById('certResultModal'));
        resultModal.show();
    }

    function escapeHtml(text) {
        if (!text) return '';
        return text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
    }
</script>
</body>
</html>
