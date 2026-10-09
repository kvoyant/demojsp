<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>저축은행 포털 - 게시판 목록</title>
    <!-- Bootstrap 5 CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <!-- jQuery -->
    <script src="https://code.jquery.com/jquery-3.7.0.min.js"></script>
    <!-- Common JS -->
    <script src="/js/common.js"></script>
    <style>
        body { background-color: #f4f6f9; font-family: 'Malgun Gothic', sans-serif; }
        .card-header-financial { background-color: #0d47a1; color: white; font-weight: bold; }
        .badge-must-read { background-color: #d32f2f; color: white; }
        .badge-notice { background-color: #1976d2; color: white; }
        .badge-guide { background-color: #388e3c; color: white; }
        .badge-agent { background-color: #f57c00; color: white; }
    </style>
</head>
<body>
<div class="container mt-5">
    <div class="card shadow-sm">
        <div class="card-header card-header-financial d-flex justify-content-between align-items-center py-3">
            <h4 class="mb-0">🏦 저축은행 소매금융 포털 게시판</h4>
            <a href="/board/form" class="btn btn-light btn-sm font-weight-bold">✏️ 글쓰기</a>
        </div>
        <div class="card-body">
            <!-- 검색 필터 -->
            <form id="searchForm" class="row g-3 mb-4">
                <div class="col-md-3">
                    <select id="category" class="form-select">
                        <option value="">전체 카테고리</option>
                        <option value="NOTICE">공지사항</option>
                        <option value="GUIDE">여신지침</option>
                        <option value="AGENT">모집인공지</option>
                    </select>
                </div>
                <div class="col-md-3">
                    <select id="searchType" class="form-select">
                        <option value="TITLE">제목</option>
                        <option value="CONTENT">내용</option>
                        <option value="WRITER">작성자</option>
                    </select>
                </div>
                <div class="col-md-4">
                    <input type="text" id="keyword" class="form-control" placeholder="검색어를 입력하세요">
                </div>
                <div class="col-md-2">
                    <button type="button" id="btnSearch" class="btn btn-primary w-100">조회</button>
                </div>
            </form>

            <!-- 게시글 테이블 -->
            <div class="table-responsive">
                <table class="table table-hover align-middle">
                    <thead class="table-light">
                    <tr>
                        <th style="width: 80px;">번호</th>
                        <th style="width: 120px;">분류</th>
                        <th>제목</th>
                        <th style="width: 120px;">작성자</th>
                        <th style="width: 140px;">작성일</th>
                        <th style="width: 90px;" class="text-center">조회수</th>
                    </tr>
                    </thead>
                    <tbody id="boardListBody">
                        <!-- jQuery AJAX 로딩 데이터 -->
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>

<script>
    $(document).ready(function() {
        loadBoardList();

        $('#btnSearch').click(function() {
            loadBoardList();
        });

        $('#keyword').keypress(function(e) {
            if (e.which === 13) {
                e.preventDefault();
                loadBoardList();
            }
        });
    });

    function loadBoardList() {
        var params = {
            category: $('#category').val(),
            searchType: $('#searchType').val(),
            keyword: $('#keyword').val()
        };

        Common.ajax({
            url: '/api/board',
            type: 'GET',
            data: params,
            success: function(data) {
                var html = '';
                if (!data || data.length === 0) {
                    html = '<tr><td colspan="6" class="text-center text-muted py-4">등록된 게시글이 없습니다.</td></tr>';
                } else {
                    $.each(data, function(idx, item) {
                        var categoryBadge = getCategoryBadge(item.category);
                        var mustReadBadge = item.mustReadYn === 'Y' ? '<span class="badge badge-must-read me-1">필독</span>' : '';
                        var regDateStr = item.regDate ? item.regDate.substring(0, 10) : '';

                        html += '<tr style="cursor:pointer;" onclick="location.href=\'/board/detail/' + item.boardId + '\'">';
                        html += '<td>' + item.boardId + '</td>';
                        html += '<td>' + categoryBadge + '</td>';
                        html += '<td>' + mustReadBadge + ' ' + escapeHtml(item.title) + '</td>';
                        html += '<td>' + escapeHtml(item.writerName) + '</td>';
                        html += '<td>' + regDateStr + '</td>';
                        html += '<td class="text-center">' + item.viewCount + '</td>';
                        html += '</tr>';
                    });
                }
                $('#boardListBody').html(html);
            }
        });
    }

    function getCategoryBadge(category) {
        if (category === 'NOTICE') return '<span class="badge badge-notice">공지사항</span>';
        if (category === 'GUIDE') return '<span class="badge badge-guide">여신지침</span>';
        if (category === 'AGENT') return '<span class="badge badge-agent">모집인공지</span>';
        return '<span class="badge bg-secondary">' + category + '</span>';
    }

    function escapeHtml(text) {
        if (!text) return '';
        return text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
    }
</script>
</body>
</html>
