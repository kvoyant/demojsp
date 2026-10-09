package com.example.demojsp.domain.board.service;

import com.example.demojsp.domain.board.dto.BoardDto;
import com.example.demojsp.domain.board.dto.BoardSearchDto;

import java.util.List;

public interface BoardService {

    List<BoardDto> getBoardList(BoardSearchDto searchDto);

    BoardDto getBoardDetail(Long boardId);

    Long createBoard(BoardDto boardDto);

    void updateBoard(BoardDto boardDto);

    void deleteBoard(Long boardId);
}
