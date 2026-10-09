package com.example.demojsp.domain.board.service;

import com.example.demojsp.common.exception.BusinessException;
import com.example.demojsp.common.exception.ErrorCode;
import com.example.demojsp.domain.board.dto.BoardDto;
import com.example.demojsp.domain.board.dto.BoardSearchDto;
import com.example.demojsp.domain.board.mapper.BoardMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardServiceImpl implements BoardService {

    private final BoardMapper boardMapper;

    @Override
    public List<BoardDto> getBoardList(BoardSearchDto searchDto) {
        return boardMapper.selectBoardList(searchDto);
    }

    @Override
    @Transactional
    public BoardDto getBoardDetail(Long boardId) {
        BoardDto board = boardMapper.selectBoardById(boardId);
        if (board == null) {
            throw new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "존재하지 않는 게시글입니다.");
        }
        boardMapper.increaseViewCount(boardId);
        board.setViewCount(board.getViewCount() + 1);
        return board;
    }

    @Override
    @Transactional
    public Long createBoard(BoardDto boardDto) {
        if (boardDto.getWriterId() == null || boardDto.getWriterId().trim().isEmpty()) {
            boardDto.setWriterId("admin");
        }
        if (boardDto.getWriterName() == null || boardDto.getWriterName().trim().isEmpty()) {
            boardDto.setWriterName("관리자");
        }
        boardMapper.insertBoard(boardDto);
        return boardDto.getBoardId();
    }

    @Override
    @Transactional
    public void updateBoard(BoardDto boardDto) {
        BoardDto existing = boardMapper.selectBoardById(boardDto.getBoardId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "수정할 게시글이 존재하지 않습니다.");
        }
        boardMapper.updateBoard(boardDto);
    }

    @Override
    @Transactional
    public void deleteBoard(Long boardId) {
        BoardDto existing = boardMapper.selectBoardById(boardId);
        if (existing == null) {
            throw new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "삭제할 게시글이 존재하지 않습니다.");
        }
        boardMapper.deleteBoard(boardId);
    }
}
