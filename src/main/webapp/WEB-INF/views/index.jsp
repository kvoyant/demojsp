<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>저축은행 소매금융 포털 시스템</title>
    <!-- Favicon -->
    <link rel="shortcut icon" href="/favicon.ico">
    <!-- Bootstrap 5 CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <!-- Bootstrap Icons -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css" rel="stylesheet">
    <style>
        body { background-color: #f4f6f9; font-family: 'Malgun Gothic', 'Noto Sans KR', sans-serif; }
        .hero-banner { background: linear-gradient(135deg, #0d47a1 0%, #1976d2 100%); color: white; padding: 40px 20px; border-radius: 10px; margin-bottom: 30px; }
        .menu-card { transition: transform 0.2s, box-shadow 0.2s; border: none; border-radius: 10px; }
        .menu-card:hover { transform: translateY(-5px); box-shadow: 0 8px 20px rgba(0,0,0,0.1); }
        .badge-new { background-color: #ff5722; color: white; }
    </style>
</head>
<body>
<jsp:include page="/WEB-INF/views/common/navbar.jsp" />
<div class="container pb-5">
    <div class="hero-banner shadow-sm text-center">
        <h2 class="fw-bold mb-2">🏦 저축은행 소매금융 포털 시스템</h2>
        <p class="mb-0 opacity-75">공통업무 | 휴대폰 본인인증 NICE ID 도입 및 9/30 이기종 CI Data 통합 이관 & 로그인 정책(개인, 상담사, 권한)</p>
    </div>

    <div class="row g-4">
        <!-- 1. 공통업무 : NICE ID 휴대폰 본인인증 -->
        <div class="col-md-6">
            <div class="card menu-card shadow-sm h-100">
                <div class="card-body p-4 d-flex flex-column">
                    <div class="d-flex justify-content-between align-items-start mb-3">
                        <div class="fs-1 text-primary"><i class="bi bi-shield-lock-fill"></i></div>
                        <span class="badge badge-new px-3 py-2">NEW 신규 기능</span>
                    </div>
                    <h4 class="fw-bold mb-2">공통 본인인증 (NICE ID)</h4>
                    <p class="text-muted small flex-grow-1">
                        • <strong>통신사 미지정 방식</strong>: 성명 + 생년월일 + 휴대폰번호만 입력<br>
                        • <strong>기기 점유 MO 인증</strong>: 원터치 문자 발송으로 기기 점유 및 유심 소지 검증<br>
                        • <strong>비여신금융거래 약관</strong>: 통합 약관 동의 체계<br>
                        • <strong>리스크 체크 영향도 검토</strong>: CI 기반 유의고객 차단, 미성년자(만 19세), 외국인 체크
                    </p>
                    <a href="/common/auth/nice-cert" class="btn btn-primary fw-bold py-2 mt-3">
                        <i class="bi bi-box-arrow-in-right me-1"></i>본인인증 화면 바로가기 &rarr;
                    </a>
                </div>
            </div>
        </div>

        <!-- 2. 공통업무 : 9/30 이기종 CI 데이터 이관 관리 -->
        <div class="col-md-6">
            <div class="card menu-card shadow-sm h-100">
                <div class="card-body p-4 d-flex flex-column">
                    <div class="d-flex justify-content-between align-items-start mb-3">
                        <div class="fs-1 text-success"><i class="bi bi-database-fill-gear"></i></div>
                        <span class="badge bg-success px-3 py-2">DB 이관 관리</span>
                    </div>
                    <h4 class="fw-bold mb-2">9/30 이기종 CI 데이터 이관</h4>
                    <p class="text-muted small flex-grow-1">
                        • <strong>9/30 추출 잔류 데이터</strong>: 이기종 레거시 시스템 CI 데이터 관리<br>
                        • <strong>DB 요건</strong>: TB_CUST_CI (CI 88byte UNIQUE, UTF-8)<br>
                        • <strong>정합성 파이프라인</strong>: 88자 길이, Base64 형식, 중복 CI 검출<br>
                        • <strong>배치 및 모니터링</strong>: 벌크 마이그레이션 실행 및 이력 관리
                    </p>
                    <a href="/common/auth/ci-migration" class="btn btn-success fw-bold py-2 mt-3">
                        <i class="bi bi-gear-fill me-1"></i>CI 이관 관리 화면 바로가기 &rarr;
                    </a>
                </div>
            </div>
        </div>

        <!-- 3. 로그인 정책 (개인, 상담사, 권한 등) -->
        <div class="col-md-6">
            <div class="card menu-card shadow-sm h-100">
                <div class="card-body p-4 d-flex flex-column">
                    <div class="d-flex justify-content-between align-items-start mb-3">
                        <div class="fs-1 text-info"><i class="bi bi-person-badge-fill"></i></div>
                        <span class="badge bg-primary px-3 py-2">보안 규정 준수</span>
                    </div>
                    <h4 class="fw-bold mb-2">로그인 정책 (개인 / 상담사 / 권한)</h4>
                    <p class="text-muted small flex-grow-1">
                        • <strong>개인 고객</strong>: 30분 세션, 5회 비밀번호 오류 잠금, NICE ID CI 간편 로그인<br>
                        • <strong>대출 상담사</strong>: 15분 세션, 2차 추가인증(SMS OTP) 필수, 중복 로그인 차단<br>
                        • <strong>심사역 / 관리자</strong>: 15분 세션, 2FA 필수, RBAC 권한 분리 통제<br>
                        • <strong>전자금융 감사 로그</strong>: 시도 IP, User-Agent, 실패 사유 전수 영속화
                    </p>
                    <a href="/login" class="btn btn-primary fw-bold py-2 mt-3">
                        <i class="bi bi-box-arrow-in-right me-1"></i>로그인 화면 바로가기 &rarr;
                    </a>
                </div>
            </div>
        </div>

        <!-- 4. 공통 본인확인 : 1원 계좌인증 (REQ-AP-605) -->
        <div class="col-md-6">
            <div class="card menu-card shadow-sm h-100">
                <div class="card-body p-4 d-flex flex-column">
                    <div class="d-flex justify-content-between align-items-start mb-3">
                        <div class="fs-1 text-warning"><i class="bi bi-coin"></i></div>
                        <span class="badge bg-warning text-dark px-3 py-2 fw-bold">REQ-AP-605 신규</span>
                    </div>
                    <h4 class="fw-bold mb-2">1원 계좌인증 (본인확인)</h4>
                    <p class="text-muted small flex-grow-1">
                        • <strong>화면 분리 운영</strong>: 대출신청 연계 목적 화면과 타 업무 공통 화면 분리<br>
                        • <strong>대출계좌 자동완성</strong>: 대출금입금/출금계좌 정보를 불러와 자동완성 처리<br>
                        • <strong>계좌 변경/수정기능</strong>: 은행/계좌 선택 및 직접 수정 기능 제공<br>
                        • <strong>1원 펌뱅킹 이체 & 검증</strong>: 입금자명 4자리 인증코드(10분 유효, 5회 잠금)
                    </p>
                    <div class="d-flex gap-2 mt-3">
                        <a href="/common/auth/account-cert?purpose=LOAN_APPLY" class="btn btn-warning fw-bold flex-fill py-2 text-dark">
                            <i class="bi bi-file-earmark-check me-1"></i>대출연계 모드 &rarr;
                        </a>
                        <a href="/common/auth/account-cert?purpose=COMMON" class="btn btn-outline-secondary fw-bold flex-fill py-2">
                            <i class="bi bi-box-arrow-in-right me-1"></i>공통업무 모드 &rarr;
                        </a>
                    </div>
                </div>
            </div>
        </div>

        <!-- 6. 공통모듈 테스트 허브 (CF-BC-03 본인인증, CF-BC-04 약관동의 ...) -->
        <div class="col-md-6">
            <div class="card menu-card shadow-sm h-100">
                <div class="card-body p-4 d-flex flex-column">
                    <div class="d-flex justify-content-between align-items-start mb-3">
                        <div class="fs-1 text-primary"><i class="bi bi-clipboard2-check"></i></div>
                        <span class="badge bg-info text-dark px-3 py-2 fw-bold">공통모듈</span>
                    </div>
                    <h4 class="fw-bold mb-2">공통모듈 테스트</h4>
                    <p class="text-muted small flex-grow-1">
                        • <strong>CF-BC-03 / REQ-AP-601</strong>: 휴대폰 본인인증 NICE ID 공통모듈<br>
                        • <strong>CF-BC-04 / REQ-AP-602</strong>: 약관동의 공통모듈 (필수/선택 2단계)<br>
                        • 요구사항이 추가될 때마다 목록에 이어서 등록
                    </p>
                    <a href="/common/test-hub" class="btn btn-primary fw-bold py-2 mt-3">
                        <i class="bi bi-list-check me-1"></i>공통모듈 테스트 목록 &rarr;
                    </a>
                </div>
            </div>
        </div>

        <!-- 5. 포털 게시판 -->
        <div class="col-md-6">
            <div class="card menu-card shadow-sm h-100">
                <div class="card-body p-4 d-flex flex-column">
                    <div class="d-flex justify-content-between align-items-start mb-3">
                        <div class="fs-1 text-secondary"><i class="bi bi-journal-text"></i></div>
                        <span class="badge bg-secondary px-3 py-2">공지 & 지침</span>
                    </div>
                    <h4 class="fw-bold mb-2">소매금융 포털 게시판</h4>
                    <p class="text-muted small flex-grow-1">
                        • <strong>공지사항 및 여신지침</strong>: 실시간 금융 규정 및 업무 지침 공유<br>
                        • <strong>대출모집인 전용 공지</strong>: 영업 채널 공지사항 및 Q&A<br>
                        • <strong>권한 제어</strong>: 로그인 세션 및 사용자 역할 기반 게시글 열람 통제
                    </p>
                    <a href="/board/list" class="btn btn-outline-dark fw-bold py-2 mt-3">
                        <i class="bi bi-card-list me-1"></i>게시판 바로가기 &rarr;
                    </a>
                </div>
            </div>
        </div>
    </div>
</div>
</body>
</html>
