package com.example.demojsp.common.consent.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** POST /api/common/consent/{bizCd}/agree 요청 */
@Getter
@Setter
@NoArgsConstructor
public class ConsentAgreeRequest {

    private String channel;                                  // APP / PC_WEB / AGENT_WEB
    private List<ConsentAgreeItemDto> items = new ArrayList<>();
}
