<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>저축은행 포털 - 9/30 이기종 CI 데이터 이관 관리</title>
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
        body { background-color: #f4f6f9; font-family: 'Malgun Gothic', 'Noto Sans KR', sans-serif; }
        .card-header-financial { background-color: #0d47a1; color: white; font-weight: bold; }
        .stat-card { border-radius: 8px; border: none; }
    </style>
</head>
<body>
<div class="container-fluid py-4 px-md-5">
    <div class="d-flex justify-content-between align-items-center mb-4">
        <div>
            <h3 class="fw-bold mb-1"><i class="bi bi-database-fill-gear text-primary me-2"></i>9/30 이기종 CI Data 이관 관리 및 모니터링</h3>
            <p class="text-muted mb-0 small">이기종 레거시 시스템 잔류 CI 데이터 정합성 검증 및 운영 DB(TB_CUST_CI) 통합 이관</p>
        </div>
        <div>
            <a href="/common/auth/nice-cert" class="btn btn-outline-primary me-2">
                <i class="bi bi-shield-lock me-1"></i>본인인증 테스트 화면
            </a>
            <a href="/board/list" class="btn btn-outline-secondary">
                <i class="bi bi-house me-1"></i>포털 홈
            </a>
        </div>
    </div>

    <!-- DB 요건 및 이관 방안 가이드 카드 -->
    <div class="card shadow-sm mb-4">
        <div class="card-header bg-light fw-bold text-dark d-flex justify-content-between align-items-center">
            <span><i class="bi bi-info-circle-fill text-primary me-2"></i>이기종 CI 데이터 이관 DB 요건 및 프로세스 가이드</span>
            <span class="badge bg-secondary">기준일자: 2026-09-30</span>
        </div>
        <div class="card-body">
            <div class="row g-3">
                <div class="col-md-4">
                    <div class="p-3 border rounded bg-light h-100">
                        <h6 class="fw-bold text-primary mb-2">1. DB 스키마 요건</h6>
                        <ul class="small text-muted ps-3 mb-0">
                            <li><strong>CI 컬럼 규격</strong>: VARCHAR(88) UNIQUE (NICE 표준 88바이트)</li>
                            <li><strong>문자셋</strong>: utf8mb4_general_ci</li>
                            <li><strong>식별자 체계</strong>: 주민등록번호(RRN) 영구 보관 금지 &rarr; CI 기준 유일 식별</li>
                        </ul>
                    </div>
                </div>
                <div class="col-md-4">
                    <div class="p-3 border rounded bg-light h-100">
                        <h6 class="fw-bold text-success mb-2">2. 데이터 정합성 검증 파이프라인</h6>
                        <ul class="small text-muted ps-3 mb-0">
                            <li><strong>1차 검증</strong>: 길이 88자 확인 (`INVALID_LENGTH` 격리)</li>
                            <li><strong>2차 검증</strong>: Base64 인코딩 형식 (`[A-Za-z0-9+/=]{88}`)</li>
                            <li><strong>3차 검증</strong>: 이기종 내 중복 및 기등록 CI 필터링 (`DUPLICATE_CI`)</li>
                        </ul>
                    </div>
                </div>
                <div class="col-md-4">
                    <div class="p-3 border rounded bg-light h-100">
                        <h6 class="fw-bold text-warning mb-2">3. 트랜잭션 및 롤백 안전장치</h6>
                        <ul class="small text-muted ps-3 mb-0">
                            <li><strong>Staging 아키텍처</strong>: 임시 테이블을 통한 무중단 벌크 검증</li>
                            <li><strong>배치 감사로그</strong>: 실행자, 건수, 소요시간 `TB_CI_MIGRATION_HISTORY` 기록</li>
                            <li><strong>정상 데이터만 Upsert</strong>: 오류 데이터는 오류사유 기록 후 보류</li>
                        </ul>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- 실행 제어 바 -->
    <div class="card shadow-sm mb-4">
        <div class="card-body d-flex flex-wrap justify-content-between align-items-center gap-3">
            <div>
                <h5 class="fw-bold mb-1">이관 파이프라인 제어</h5>
                <p class="text-muted small mb-0">스테이징 적재 &rarr; 사전 정합성 검증 &rarr; 운영 DB 마이그레이션</p>
            </div>
            <div class="d-flex gap-2">
                <button type="button" id="btnResetSample" class="btn btn-outline-danger">
                    <i class="bi bi-arrow-counterclockwise me-1"></i>샘플 데이터 초기화
                </button>
                <button type="button" id="btnValidate" class="btn btn-outline-primary">
                    <i class="bi bi-clipboard2-check me-1"></i>정합성 검증 실행
                </button>
                <button type="button" id="btnExecuteMig" class="btn btn-success fw-bold">
                    <i class="bi bi-play-fill me-1"></i>9/30 CI 데이터 이관 실행
                </button>
            </div>
        </div>
    </div>

    <!-- 데이터 그리드 영역 -->
    <div class="row">
        <!-- 스테이징 테이블 현황 -->
        <div class="col-lg-8 mb-4">
            <div class="card shadow-sm h-100">
                <div class="card-header card-header-financial py-3 d-flex justify-content-between align-items-center">
                    <h5 class="mb-0 fs-6"><i class="bi bi-table me-2"></i>스테이징(임시) CI 데이터 적재 현황 [TB_CI_MIGRATION_TEMP]</h5>
                    <button type="button" class="btn btn-light btn-sm" onclick="loadTempList()">새로고침</button>
                </div>
                <div class="card-body p-0">
                    <div class="table-responsive">
                        <table class="table table-hover table-striped align-middle mb-0 small">
                            <thead class="table-light">
                                <tr>
                                    <th>No</th>
                                    <th>시스템</th>
                                    <th>고객명</th>
                                    <th>연계정보 (CI 마스킹)</th>
                                    <th>생년월일</th>
                                    <th>검증상태</th>
                                    <th>이관상태</th>
                                    <th>오류/비고</th>
                                </tr>
                            </thead>
                            <tbody id="tempListBody">
                                <!-- AJAX 렌더링 -->
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>

        <!-- 이관 이력 및 통계 -->
        <div class="col-lg-4 mb-4">
            <div class="card shadow-sm h-100">
                <div class="card-header bg-dark text-white py-3">
                    <h5 class="mb-0 fs-6"><i class="bi bi-clock-history me-2"></i>이관 실행 감사 이력 [TB_CI_MIGRATION_HISTORY]</h5>
                </div>
                <div class="card-body p-0">
                    <div class="table-responsive">
                        <table class="table table-hover align-middle mb-0 small">
                            <thead class="table-light">
                                <tr>
                                    <th>배치 ID</th>
                                    <th>성공/총건</th>
                                    <th>상태</th>
                                    <th>실행일시</th>
                                </tr>
                            </thead>
                            <tbody id="historyListBody">
                                <!-- AJAX 렌더링 -->
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    $(document).ready(function() {
        loadTempList();
        loadHistoryList();

        $('#btnValidate').click(function() {
            Common.ajax({
                url: '/api/common/auth/ci-migration/validate',
                type: 'POST',
                success: function(res) {
                    alert('검증 완료: 총 ' + res.validCount + '건의 유효 CI가 확인되었습니다.');
                    loadTempList();
                }
            });
        });

        $('#btnExecuteMig').click(function() {
            if (!confirm('9/30 이기종 CI 데이터를 운영 DB로 이관하시겠습니까?')) return;

            Common.ajax({
                url: '/api/common/auth/ci-migration/execute',
                type: 'POST',
                data: { execBy: 'MIG_ADMIN' },
                success: function(data, res) {
                    alert(res.message || '이관이 완료되었습니다.');
                    loadTempList();
                    loadHistoryList();
                }
            });
        });

        $('#btnResetSample').click(function() {
            if (!confirm('임시 스테이징 테이블의 샘플 데이터를 초기화하시겠습니까?')) return;

            Common.ajax({
                url: '/api/common/auth/ci-migration/reset',
                type: 'POST',
                success: function() {
                    alert('샘플 데이터가 초기화되었습니다.');
                    loadTempList();
                }
            });
        });
    });

    function loadTempList() {
        Common.ajax({
            url: '/api/common/auth/ci-migration/list',
            type: 'GET',
            success: function(list) {
                var html = '';
                if (!list || list.length === 0) {
                    html = '<tr><td colspan="8" class="text-center py-4 text-muted">적재된 데이터가 없습니다.</td></tr>';
                } else {
                    $.each(list, function(idx, item) {
                        var vBadge = getValidBadge(item.validStatus);
                        var mBadge = getMigBadge(item.migrationStatus);
                        var ciMasked = item.ciVal && item.ciVal.length > 20 ? item.ciVal.substring(0, 10) + '...***...' + item.ciVal.substring(item.ciVal.length - 6) : item.ciVal;

                        html += '<tr>';
                        html += '<td>' + item.tempSeq + '</td>';
                        html += '<td><span class="badge bg-secondary">' + item.legacySystemCd + '</span></td>';
                        html += '<td class="fw-bold">' + escapeHtml(item.custNm) + '</td>';
                        html += '<td class="font-monospace small">' + escapeHtml(ciMasked) + '</td>';
                        html += '<td>' + item.birthDate + '</td>';
                        html += '<td>' + vBadge + '</td>';
                        html += '<td>' + mBadge + '</td>';
                        html += '<td class="text-danger small">' + escapeHtml(item.errorMsg || '-') + '</td>';
                        html += '</tr>';
                    });
                }
                $('#tempListBody').html(html);
            }
        });
    }

    function loadHistoryList() {
        Common.ajax({
            url: '/api/common/auth/ci-migration/history',
            type: 'GET',
            success: function(list) {
                var html = '';
                if (!list || list.length === 0) {
                    html = '<tr><td colspan="4" class="text-center py-3 text-muted">이관 이력이 없습니다.</td></tr>';
                } else {
                    $.each(list, function(idx, item) {
                        var statusBadge = item.status === 'COMPLETED' ? '<span class="badge bg-success">완료</span>' : '<span class="badge bg-warning">진행중</span>';
                        var startDtStr = item.startDt ? item.startDt.substring(0, 16).replace('T', ' ') : '';

                        html += '<tr>';
                        html += '<td class="fw-bold small">' + escapeHtml(item.batchId) + '</td>';
                        html += '<td><span class="text-success fw-bold">' + item.successCount + '</span> / ' + item.totalCount + '</td>';
                        html += '<td>' + statusBadge + '</td>';
                        html += '<td class="text-muted small">' + startDtStr + '</td>';
                        html += '</tr>';
                    });
                }
                $('#historyListBody').html(html);
            }
        });
    }

    function getValidBadge(status) {
        if (status === 'VALID') return '<span class="badge bg-success">유효</span>';
        if (status === 'PENDING') return '<span class="badge bg-warning text-dark">검증대기</span>';
        if (status === 'INVALID_LENGTH') return '<span class="badge bg-danger">길이오류</span>';
        if (status === 'INVALID_BASE64') return '<span class="badge bg-danger">Base64오류</span>';
        if (status === 'DUPLICATE_CI') return '<span class="badge bg-secondary">중복제외</span>';
        return '<span class="badge bg-light text-dark">' + status + '</span>';
    }

    function getMigBadge(status) {
        if (status === 'COMPLETED') return '<span class="badge bg-primary">이관완료</span>';
        if (status === 'FAILED') return '<span class="badge bg-danger">실패</span>';
        return '<span class="badge bg-light text-dark">준비</span>';
    }

    function escapeHtml(text) {
        if (!text) return '';
        return text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
    }
</script>
</body>
</html>
