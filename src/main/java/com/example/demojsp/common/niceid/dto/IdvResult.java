package com.example.demojsp.common.niceid.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 본인인증 결과.
 * <ul>
 *   <li>화면 응답: 상태 · 결과코드 · 안내문구 · 성명 · 나이 · 마스킹 휴대폰 (CI/DI/전체 휴대폰번호는 JSON 에 나가지 않는다)</li>
 *   <li>업무 서버: {@code niceIdService.checkVerified(...)} 로 받으면 getCi() 등 전체 값을 쓸 수 있다</li>
 * </ul>
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IdvResult {

    private final String reqNo;
    private final String purposeCd;
    private final String method;
    private final String status;      // REQUESTED(MO 대기) / VERIFIED / BLOCKED / FAILED / EXPIRED
    private final String resultCd;    // OK / MINOR / FOREIGNER / WATCHLIST / ...
    private final String message;     // 화면 안내 문구
    private final String name;
    private final String birthDt;
    private final Integer age;
    private final String gender;
    private final String nationalCd;
    private final String cellNoMask;
    private final LocalDateTime doneAt;

    // ---- 서버 전용 (JSON 제외)
    @JsonIgnore private final String ci;
    @JsonIgnore private final String di;
    @JsonIgnore private final String cellNo;

    @JsonIgnore
    public boolean isVerified() {
        return "VERIFIED".equals(status);
    }
}
