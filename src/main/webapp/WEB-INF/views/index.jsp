<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>저축은행 소매금융 포털 시스템</title>
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
<div class="container py-5">
    <div class="hero-banner shadow-sm text-center">
        <h2 class="fw-bold mb-2">🏦 저축은행 소매금융 포털 시스템</h2>
        <p class="mb-0 opacity-75">공통업무 | 휴대폰 본인인증 NICE ID 도입 및 9/30 이기종 CI Data 통합 이관</p>
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

        <!-- 3. 포털 게시판 -->
        <div class="col-md-12">
            <div class="card menu-card shadow-sm">
                <div class="card-body p-4 d-flex justify-content-between align-items-center">
                    <div>
                        <h5 class="fw-bold mb-1"><i class="bi bi-journal-text text-secondary me-2"></i>저축은행 소매금융 포털 게시판</h5>
                        <p class="text-muted small mb-0">공지사항, 여신지침, 대출모집인 공지사항 조회 및 관리</p>
                    </div>
                    <a href="/board/list" class="btn btn-outline-dark fw-bold">
                        게시판 바로가기 &rarr;
                    </a>
                </div>
            </div>
        </div>
    </div>
</div>
</body>
</html>
