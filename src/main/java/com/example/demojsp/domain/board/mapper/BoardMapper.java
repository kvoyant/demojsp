package com.example.demojsp.domain.board.mapper;

import com.example.demojsp.domain.board.dto.BoardDto;
import com.example.demojsp.domain.board.dto.BoardSearchDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BoardMapper {

    List<BoardDto> selectBoardList(BoardSearchDto searchDto);

    BoardDto selectBoardById(Long boardId);

    int insertBoard(BoardDto boardDto);

    int updateBoard(BoardDto boardDto);

    int deleteBoard(Long boardId);

    int increaseViewCount(Long boardId);
}
