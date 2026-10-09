package com.example.demojsp.domain.board.controller;

import com.example.demojsp.common.response.ApiResponse;
import com.example.demojsp.domain.board.dto.BoardDto;
import com.example.demojsp.domain.board.dto.BoardSearchDto;
import com.example.demojsp.domain.board.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/board")
@RequiredArgsConstructor
public class BoardApiController {

    private final BoardService boardService;

    @GetMapping
    public ApiResponse<List<BoardDto>> getBoardList(BoardSearchDto searchDto) {
        List<BoardDto> list = boardService.getBoardList(searchDto);
        return ApiResponse.success(list);
    }

    @GetMapping("/{id}")
    public ApiResponse<BoardDto> getBoardDetail(@PathVariable("id") Long id) {
        BoardDto detail = boardService.getBoardDetail(id);
        return ApiResponse.success(detail);
    }

    @PostMapping
    public ApiResponse<Long> createBoard(@RequestBody BoardDto boardDto) {
        Long createdId = boardService.createBoard(boardDto);
        return ApiResponse.success("게시글이 성공적으로 등록되었습니다.", createdId);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updateBoard(@PathVariable("id") Long id, @RequestBody BoardDto boardDto) {
        boardDto.setBoardId(id);
        boardService.updateBoard(boardDto);
        return ApiResponse.success("게시글이 성공적으로 수정되었습니다.", null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteBoard(@PathVariable("id") Long id) {
        boardService.deleteBoard(id);
        return ApiResponse.success("게시글이 삭제되었습니다.", null);
    }
}
