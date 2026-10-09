<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>저축은행 포털 - 게시글 상세</title>
    <!-- Bootstrap 5 CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <!-- jQuery -->
    <script src="https://code.jquery.com/jquery-3.7.0.min.js"></script>
    <!-- Common JS -->
    <script src="/js/common.js"></script>
    <style>
        body { background-color: #f4f6f9; font-family: 'Malgun Gothic', sans-serif; }
        .card-header-financial { background-color: #0d47a1; color: white; font-weight: bold; }
        .content-area { min-height: 250px; white-space: pre-wrap; word-break: break-all; background-color: #ffffff; padding: 20px; border-radius: 5px; border: 1px solid #e0e0e0; }
    </style>
</head>
<body>
<div class="container mt-5">
    <div class="card shadow-sm">
        <div class="card-header card-header-financial py-3">
            <h4 class="mb-0" id="detailTitle">게시글 상세 정보</h4>
        </div>
        <div class="card-body">
            <div class="row mb-3 border-bottom pb-3">
                <div class="col-md-3"><strong>분류:</strong> <span id="detailCategory"></span></div>
                <div class="col-md-3"><strong>작성자:</strong> <span id="detailWriter"></span></div>
                <div class="col-md-3"><strong>작성일:</strong> <span id="detailRegDate"></span></div>
                <div class="col-md-3"><strong>조회수:</strong> <span id="detailViewCount"></span></div>
            </div>

            <div class="mb-4">
                <h5 class="fw-bold mb-3">내용</h5>
                <div id="detailContent" class="content-area"></div>
            </div>

            <div class="d-flex justify-content-between">
                <a href="/board/list" class="btn btn-secondary">목록으로</a>
                <div>
                    <a id="btnEdit" href="#" class="btn btn-primary">수정</a>
                    <button id="btnDelete" type="button" class="btn btn-danger me-1">삭제</button>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    var boardId = "${boardId}";

    $(document).ready(function() {
        if (!boardId) {
            alert('게시글 번호가 존재하지 않습니다.');
            location.href = '/board/list';
            return;
        }

        loadBoardDetail();

        $('#btnDelete').click(function() {
            if (confirm('정말로 이 게시글을 삭제하시겠습니까?')) {
                deleteBoard();
            }
        });
    });

    function loadBoardDetail() {
        Common.ajax({
            url: '/api/board/' + boardId,
            type: 'GET',
            success: function(data) {
                $('#detailTitle').text(data.title);
                $('#detailCategory').text(getCategoryName(data.category));
                $('#detailWriter').text(data.writerName + ' (' + data.writerId + ')');
                $('#detailRegDate').text(data.regDate ? data.regDate.replace('T', ' ').substring(0, 19) : '');
                $('#detailViewCount').text(data.viewCount);
                $('#detailContent').text(data.content);

                $('#btnEdit').attr('href', '/board/edit/' + boardId);
            }
        });
    }

    function deleteBoard() {
        Common.ajax({
            url: '/api/board/' + boardId,
            type: 'DELETE',
            success: function() {
                alert('게시글이 삭제되었습니다.');
                location.href = '/board/list';
            }
        });
    }

    function getCategoryName(category) {
        if (category === 'NOTICE') return '공지사항';
        if (category === 'GUIDE') return '여신지침';
        if (category === 'AGENT') return '모집인공지';
        return category;
    }
</script>
</body>
</html>
