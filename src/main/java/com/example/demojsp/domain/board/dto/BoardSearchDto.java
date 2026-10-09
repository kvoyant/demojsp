package com.example.demojsp.domain.board.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoardSearchDto {

    private String category;
    private String searchType; // TITLE, CONTENT, WRITER
    private String keyword;
}
