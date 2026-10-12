package com.example.demojsp.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TermsDto {
    private String termsId;
    private String termsTitle;
    private String termsVersion;
    private String requiredYn;
    private String category;
    private String content;
    private Integer sortOrder;
    private String useYn;
    private LocalDateTime regDt;
}
