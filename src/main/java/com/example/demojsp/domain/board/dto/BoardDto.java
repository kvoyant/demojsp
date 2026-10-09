package com.example.demojsp.domain.board.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardDto {

    private Long boardId;
    private String category;
    private String title;
    private String content;
    private String writerId;
    private String writerName;
    private Integer viewCount;
    private String mustReadYn;
    private LocalDateTime regDate;
    private LocalDateTime modDate;
}
