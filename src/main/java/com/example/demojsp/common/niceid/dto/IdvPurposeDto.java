package com.example.demojsp.common.niceid.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 업무별 본인인증 정책 1건 (TB_CO_IDV_PURPOSE).
 * 업무 화면은 purposeCd 만 넘기고, 약관·인증방식·나이·외국인·유의고객 검사 여부는 전부 이 행(DB)이 정한다.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IdvPurposeDto {

    private String purposeCd;
    private String purposeNm;
    private String consentBizCd;     // 공통#22 약관동의 업무코드 (null = 약관 생략)
    private String smsYn;            // 통신사 미지정 SMS
    private String moYn;             // MO 기기 점유 인증 (휴대폰번호 입력 생략)
    private Integer minAge;          // 최소 만 나이 (0 = 제한 없음)
    private String foreignerYn;      // 외국인 허용
    private String watchCheckYn;     // 유의고객 검사
    private Integer codeValidSec;    // 인증번호 / MO 대기 유효시간(초)
    private Integer maxTry;          // 인증번호 입력 최대 횟수
    private Integer resultValidMin;  // 인증 결과 사용 가능 시간(분)
    private String useYn;
    private LocalDateTime updAt;
    private String updBy;
}
