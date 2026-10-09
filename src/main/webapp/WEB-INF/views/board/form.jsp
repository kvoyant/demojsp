<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>저축은행 포털 - 게시글 작성/수정</title>
    <!-- Bootstrap 5 CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <!-- jQuery -->
    <script src="https://code.jquery.com/jquery-3.7.0.min.js"></script>
    <!-- Common JS -->
    <script src="/js/common.js"></script>
    <style>
        body { background-color: #f4f6f9; font-family: 'Malgun Gothic', sans-serif; }
        .card-header-financial { background-color: #0d47a1; color: white; font-weight: bold; }
    </style>
</head>
<body>
<div class="container mt-5">
    <div class="card shadow-sm">
        <div class="card-header card-header-financial py-3">
            <h4 class="mb-0" id="formHeader">게시글 작성</h4>
        </div>
        <div class="card-body">
            <form id="boardForm">
                <div class="row mb-3">
                    <div class="col-md-6">
                        <label for="category" class="form-label fw-bold">카테고리 <span class="text-danger">*</span></label>
                        <select id="category" class="form-select" required>
                            <option value="NOTICE">공지사항</option>
                            <option value="GUIDE">여신지침</option>
                            <option value="AGENT">모집인공지</option>
                        </select>
                    </div>
                    <div class="col-md-6 d-flex align-items-end">
                        <div class="form-check mb-2">
                            <input class="form-check-input" type="checkbox" id="mustReadYn" value="Y">
                            <label class="form-check-label fw-bold text-danger" for="mustReadYn">
                                [필독] 공지로 상단 고정
                            </label>
                        </div>
                    </div>
                </div>

                <div class="mb-3">
                    <label for="title" class="form-label fw-bold">제목 <span class="text-danger">*</span></label>
                    <input type="text" id="title" class="form-control" placeholder="게시글 제목을 입력하세요" required>
                </div>

                <div class="row mb-3">
                    <div class="col-md-6">
                        <label for="writerName" class="form-label fw-bold">작성자명</label>
                        <input type="text" id="writerName" class="form-control" value="관리자">
                    </div>
                    <div class="col-md-6">
                        <label for="writerId" class="form-label fw-bold">작성자 ID</label>
                        <input type="text" id="writerId" class="form-control" value="admin01">
                    </div>
                </div>

                <div class="mb-4">
                    <label for="content" class="form-label fw-bold">내용 <span class="text-danger">*</span></label>
                    <textarea id="content" class="form-control" rows="10" placeholder="게시글 내용을 입력하세요" required></textarea>
                </div>

                <div class="d-flex justify-content-between">
                    <a href="/board/list" class="btn btn-secondary">취소</a>
                    <button type="button" id="btnSave" class="btn btn-success">저장하기</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script>
    var boardId = "${boardId}";
    var isEdit = !!boardId;

    $(document).ready(function() {
        if (isEdit) {
            $('#formHeader').text('게시글 수정');
            loadBoardData();
        }

        $('#btnSave').click(function() {
            saveBoard();
        });
    });

    function loadBoardData() {
        Common.ajax({
            url: '/api/board/' + boardId,
            type: 'GET',
            success: function(data) {
                $('#category').val(data.category);
                $('#title').val(data.title);
                $('#writerName').val(data.writerName);
                $('#writerId').val(data.writerId);
                $('#content').val(data.content);

                if (data.mustReadYn === 'Y') {
                    $('#mustReadYn').prop('checked', true);
                }
            }
        });
    }

    function saveBoard() {
        var category = $('#category').val();
        var title = $('#title').val();
        var content = $('#content').val();

        if (!title || title.trim() === '') {
            alert('제목을 입력하세요.');
            $('#title').focus();
            return;
        }

        if (!content || content.trim() === '') {
            alert('내용을 입력하세요.');
            $('#content').focus();
            return;
        }

        var boardData = {
            category: category,
            title: title,
            content: content,
            writerName: $('#writerName').val(),
            writerId: $('#writerId').val(),
            mustReadYn: $('#mustReadYn').is(':checked') ? 'Y' : 'N'
        };

        var url = '/api/board';
        var method = 'POST';

        if (isEdit) {
            url = '/api/board/' + boardId;
            method = 'PUT';
        }

        Common.ajax({
            url: url,
            type: method,
            data: boardData,
            success: function(resData, res) {
                alert(res.message || '성공적으로 저장되었습니다.');
                if (isEdit) {
                    location.href = '/board/detail/' + boardId;
                } else {
                    location.href = '/board/list';
                }
            }
        });
    }
</script>
</body>
</html>
