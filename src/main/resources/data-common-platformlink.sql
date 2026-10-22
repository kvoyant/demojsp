-- [공통#1] 플랫폼 대출 연계 샘플 (TB_LNK_PARTNER 가 비어 있을 때 1회 적재)
-- 운영 중인 제휴사: 토스(오토·신용), 카카오페이(오토·신용·모기지), 핀다(모기지) / 오픈 예정: 네이버파이낸셜 (USE_YN=N)
-- 전문 형식은 제휴사별 연동 규격을 받기 전 "가정" 값이다 (어댑터 주석 참고).
INSERT INTO TB_LNK_PARTNER (PARTNER_CD, PARTNER_NM, ADAPTER_CD, CSS_ENGINE, PRODUCTS, TIMEOUT_MS, MARGIN_MS, ALLOW_IPS, SECRET_REF, USE_YN, UPD_AT, UPD_BY) VALUES
('TOSS',     '토스',           'TOSS_V1',     'LEGACY', 'CREDIT,AUTO',          3000, 300, '*', 'TOSS',     'Y', CURRENT_TIMESTAMP, 'SYSTEM'),
('KAKAOPAY', '카카오페이',     'KAKAOPAY_V1', 'SHADOW', 'CREDIT,AUTO,MORTGAGE', 5000, 500, '*', 'KAKAOPAY', 'Y', CURRENT_TIMESTAMP, 'SYSTEM'),
('FINDA',    '핀다',           'FINDA_V1',    'NEW',    'MORTGAGE',             4000, 400, '*', 'FINDA',    'Y', CURRENT_TIMESTAMP, 'SYSTEM'),
('NAVERFIN', '네이버파이낸셜', 'TOSS_V1',     'NEW',    'AUTO',                 3000, 300, '*', 'NAVERFIN', 'N', CURRENT_TIMESTAMP, 'SYSTEM');

INSERT INTO TB_LNK_PRODUCT (CSS_PRODUCT, PRODUCT_CD, PRODUCT_NM, ADMIN_LIST, USE_YN) VALUES
('CREDIT',   'LN_PEPPER_SMART', '페퍼스마트대출',     '신용 > 간편한도조회',   'Y'),
('AUTO',     'LN_PEPPER_AUTO',  '페퍼오토론',         '오토 > 간편한도조회',   'Y'),
('MORTGAGE', 'LN_PEPPER_HOME',  '페퍼아파트담보대출', '모기지 > 간편한도조회', 'Y');

INSERT INTO TB_LNK_PARTNER_PRD (PARTNER_CD, CSS_PRODUCT, OUTER_PRD_CD) VALUES
('TOSS', 'CREDIT', 'PEPPER-CREDIT-01'), ('TOSS', 'AUTO', 'PEPPER-AUTO-01'),
('KAKAOPAY', 'CREDIT', 'KP0101'), ('KAKAOPAY', 'AUTO', 'KP0201'), ('KAKAOPAY', 'MORTGAGE', 'KP0301'),
('FINDA', 'MORTGAGE', 'FD-HOME-PEPPER'),
('NAVERFIN', 'AUTO', 'NF-AUTO-PEPPER');

-- 결과코드 변환: 같은 "타임아웃"도 토스는 E504, 카카오페이는 9001, 핀다는 TIMEOUT 으로 받는다 (가정)
INSERT INTO TB_LNK_CODE_MAP (PARTNER_CD, INNER_CD, OUTER_CD, OUTER_MSG) VALUES
('TOSS', 'APPROVED',   'SUCCESS', '승인'),
('TOSS', 'REJECTED',   'REJECT',  '대출 가능한 상품이 없습니다.'),
('TOSS', 'TIMEOUT',    'E504',    '일시적으로 한도를 조회할 수 없습니다.'),
('TOSS', 'CSS_ERROR',  'E500',    '일시적으로 한도를 조회할 수 없습니다.'),
('TOSS', 'INVALID',    'E400',    '요청 값이 올바르지 않습니다.'),
('TOSS', 'PRODUCT_NA', 'E404',    '취급하지 않는 상품입니다.'),
('TOSS', 'STOPPED',    'E503',    '서비스 점검 중입니다.'),
('TOSS', 'AUTH_FAIL',  'E401',    '인증에 실패했습니다.'),
('KAKAOPAY', 'APPROVED',   '0000', '정상'),
('KAKAOPAY', 'REJECTED',   '1001', '심사 거절'),
('KAKAOPAY', 'TIMEOUT',    '9001', '응답 시간 초과'),
('KAKAOPAY', 'CSS_ERROR',  '9002', '심사 시스템 오류'),
('KAKAOPAY', 'INVALID',    '8001', '필수값 오류'),
('KAKAOPAY', 'PRODUCT_NA', '8002', '미취급 상품'),
('KAKAOPAY', 'STOPPED',    '9999', '서비스 중단'),
('KAKAOPAY', 'AUTH_FAIL',  '8401', '서명 검증 실패'),
('FINDA', 'APPROVED',   'OK',        '승인'),
('FINDA', 'REJECTED',   'DECLINED',  '승인 불가'),
('FINDA', 'TIMEOUT',    'TIMEOUT',   '응답 지연'),
('FINDA', 'CSS_ERROR',  'ERROR',     '시스템 오류'),
('FINDA', 'INVALID',    'BAD_REQ',   '요청 오류'),
('FINDA', 'PRODUCT_NA', 'NOT_SUPP',  '미지원 상품'),
('FINDA', 'STOPPED',    'SUSPENDED', '서비스 중단'),
('FINDA', 'AUTH_FAIL',  'UNAUTH',    '인증 실패'),
('NAVERFIN', 'APPROVED',   'SUCCESS', '승인'),
('NAVERFIN', 'REJECTED',   'REJECT',  '대출 가능한 상품이 없습니다.'),
('NAVERFIN', 'TIMEOUT',    'E504',    '일시적으로 한도를 조회할 수 없습니다.'),
('NAVERFIN', 'CSS_ERROR',  'E500',    '일시적으로 한도를 조회할 수 없습니다.'),
('NAVERFIN', 'INVALID',    'E400',    '요청 값이 올바르지 않습니다.'),
('NAVERFIN', 'PRODUCT_NA', 'E404',    '취급하지 않는 상품입니다.'),
('NAVERFIN', 'STOPPED',    'E503',    '서비스 준비 중입니다.'),
('NAVERFIN', 'AUTH_FAIL',  'E401',    '인증에 실패했습니다.');
