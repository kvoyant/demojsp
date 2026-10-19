package com.example.demojsp.common.acctverify.service;

import com.example.demojsp.common.acctverify.dto.AvBizDto;
import com.example.demojsp.common.acctverify.dto.AvReqDto;

import java.util.List;

/** [디지털어드민] 1원 계좌인증 업무 Context (화면 분리 대상 업무 목록) · 인증 이력 */
public interface AcctVerifyAdminService {

    List<AvBizDto> bizList();

    AvBizDto saveBiz(AvBizDto biz, String adminId);

    /** 인증 이력 (계좌번호 마스킹) */
    List<AvReqDto> requests(String bizCd, String status, int limit);
}
