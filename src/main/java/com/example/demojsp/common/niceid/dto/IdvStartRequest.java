package com.example.demojsp.common.niceid.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 본인인증 시작 요청 (SMS / MO 공통).
 * <ul>
 *   <li>SMS : 성명 + 생년월일 + 휴대폰번호 (통신사 선택 없음 - 요구사항 TO-BE)</li>
 *   <li>MO  : 성명 + 생년월일 (휴대폰번호 입력 생략 - 고객 휴대폰에서 보낸 문자로 번호·기기 소지 확인)</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
public class IdvStartRequest {

    private String purposeCd;   // 업무코드 (TB_CO_IDV_PURPOSE)
    private Long agreeNo;       // 약관동의 번호 (공통#22, 약관이 있는 업무만)
    private String channel;     // APP / PC_WEB / AGENT_WEB
    private String name;        // 성명
    private String birth;       // 생년월일 yyyyMMdd
    private String phone;       // 휴대폰번호 (SMS 만)
}
