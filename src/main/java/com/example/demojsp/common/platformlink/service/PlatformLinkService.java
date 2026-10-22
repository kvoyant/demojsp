package com.example.demojsp.common.platformlink.service;

import com.example.demojsp.common.platformlink.dto.LinkTrace;

/**
 * [CF-TC-07 · CF-TC-02 · CF-BC-10 / 공통#1 REQ-AP-074] 플랫폼 대출 연계 - 한도조회 수신 처리.
 *
 * <pre>
 *  제휴사 ──POST /api/link/platform/{제휴사}/limit──▶
 *    ① 인증       허용 IP · HMAC 서명 · 타임스탬프
 *    ② 전문 변환  제휴사 전문 → 공통모델 (PartnerAdapter.parse)      ← 제휴사별 차이는 여기서 끝
 *    ③ 중복 확인  같은 제휴사 요청번호면 저장된 응답을 그대로 재전송
 *    ④ 서비스 중단 스위치 (USE_YN)
 *    ⑤ 상품 결정  차량번호→오토 / 담보→모기지 / 그 외 신용, 제휴사 허용상품 확인
 *    ⑥ CSS 호출  CssRouter: LEGACY/NEW/SHADOW · 남은 시간 안에서만 · 차단기
 *    ⑦ 결과 변환  내부 결과코드 → 제휴사 결과코드(TB_LNK_CODE_MAP) → 응답 전문 (PartnerAdapter.response)
 *    ⑧ 이력 저장  TB_LNK_LIMIT_REQ (CI 해시 · 이름 마스킹)
 * </pre>
 * 어떤 경우에도 제휴사 형식의 응답 전문을 돌려준다 (예외를 밖으로 던지지 않음).
 */
public interface PlatformLinkService {

    LinkResponse handle(String partnerCd, String timestamp, String signature, String body, String clientIp);

    /**
     * @param httpStatus 200 (정상·업무오류 모두) / 401 인증 실패 / 404 미등록 제휴사
     * @param body       제휴사에 보낼 응답 전문
     * @param lnkReqNo   당행 연계 요청번호 (이력이 남은 경우)
     * @param trace      처리 단계 (샘플 화면용)
     */
    record LinkResponse(int httpStatus, String body, String lnkReqNo, LinkTrace trace) {
    }
}
