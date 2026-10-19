package com.example.demojsp.common.niceid.service;

import com.example.demojsp.common.niceid.dto.IdvPurposeDto;
import com.example.demojsp.common.niceid.dto.IdvReqDto;

import java.util.List;

/** [디지털어드민] 본인인증 업무별 정책 · 인증 이력 */
public interface NiceIdAdminService {

    List<IdvPurposeDto> purposes();

    IdvPurposeDto savePurpose(IdvPurposeDto purpose, String adminId);

    /** 인증 이력 (CI · 휴대폰번호는 마스킹) */
    List<IdvReqDto> requests(String purposeCd, String status, int limit);
}
