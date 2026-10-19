package com.example.demojsp.common.acctverify.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 1원 송금 요청.
 * <ul>
 *   <li>자동완성 그대로 : prefillId 만 (계좌는 서버가 DB 에서 읽음)</li>
 *   <li>직접 입력 / 수정 : bankCd + acctNo (+ modifiedYn=Y : 자동완성 후보가 있었는데 바꾼 경우)</li>
 * </ul>
 * 비로그인 업무는 공통#21 본인인증 결과(idvReqNo, idvPurposeCd)의 성명으로 예금주를 비교한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class AvRequest {

    private String bizCd;
    private String channel;
    private String prefillId;
    private String bankCd;
    private String acctNo;
    private String modifiedYn;
    private String idvReqNo;
    private String idvPurposeCd;
}
