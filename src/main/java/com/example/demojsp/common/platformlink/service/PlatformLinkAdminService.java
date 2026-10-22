package com.example.demojsp.common.platformlink.service;

import com.example.demojsp.common.platformlink.dto.LnkCodeMapDto;
import com.example.demojsp.common.platformlink.dto.LnkPartnerDto;
import com.example.demojsp.common.platformlink.dto.LnkProductDto;
import com.example.demojsp.common.platformlink.dto.LnkReqDto;
import com.example.demojsp.common.platformlink.dto.LnkStatDto;

import java.util.List;
import java.util.Map;

/** [디지털어드민] 플랫폼 대출 연계 운영: 제휴사 설정(엔진 전환·타임아웃·중단) · 결과코드 · 처리현황 · 이력 · 차단기 */
public interface PlatformLinkAdminService {

    List<LnkPartnerDto> partners();

    LnkPartnerDto savePartner(LnkPartnerDto p, String adminId);

    List<LnkProductDto> products();

    List<LnkCodeMapDto> codeMaps(String partnerCd);

    List<LnkReqDto> requests(String partnerCd, String status, String resultCd, int limit);

    /** 최근 n 시간 제휴사별 처리현황 */
    List<LnkStatDto> stats(int hours);

    Map<String, Map<String, Object>> circuit();

    void resetCircuit(String engine);
}
