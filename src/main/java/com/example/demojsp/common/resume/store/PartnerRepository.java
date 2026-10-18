package com.example.demojsp.common.resume.store;

import com.example.demojsp.common.resume.domain.Partner;

import java.util.List;
import java.util.Optional;

/**
 * 회사(당행/제휴사) 마스터 저장소 - 공통#43 "회사명: DB 구성 필요".
 *
 * <p>운영 테이블 예시 (DB 구성 시 참고)
 * <pre>
 *  TB_CO_PARTNER (
 *    PARTNER_CD   VARCHAR(20)  PK      -- OWN, 제휴사 코드
 *    PARTNER_NM   VARCHAR(40)  NOT NULL -- 카드에 표시할 회사명
 *    LOGO_URL     VARCHAR(500)          -- 로고 이미지 (수집·제작 후 등록, 없으면 대체 로고)
 *    OWN_YN       CHAR(1)      NOT NULL -- 당행 여부
 *    USE_YN       CHAR(1)      NOT NULL
 *  )
 * </pre>
 */
public interface PartnerRepository {

    Optional<Partner> findByCode(String code);

    List<Partner> findAll();
}
