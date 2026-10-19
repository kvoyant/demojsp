-- =============================================================================
-- [공통#26] 메인화면 상품 탭 샘플 데이터 (= 디지털어드민 등록값이라고 가정)
--   TB_PRD_TAB_SECTION 이 비어 있을 때 1회만 적재 (common.store.seed-if-empty=true). 운영은 ADMIN 에서 등록.
--   상품코드는 TB_PRODUCT 샘플(공통#47·48)과 같다: PS_SMART, PS_BIZ_MORTGAGE, PS_2030, PS_PARKING4, PS_TIME_DEPOSIT, PS_AUTO
-- =============================================================================

-- ---------------------------------------------------------------- ① 상단 슬라이드 카드 (요구사항 1: 신규 추가)
INSERT INTO TB_PRD_TAB_SLIDE (TITLE, SUB_TEXT, BADGE_TEXT, BG_COLOR, IMAGE_URL, IMAGE_ALT, PRODUCT_CD, LINK_URL, CHANNELS, START_DT, END_DT, SORT_ORD, USE_YN, UPD_AT, UPD_BY) VALUES
 ('하루만 맡겨도 연 3.2%',       '페퍼스 파킹통장4 - 언제든 넣고 빼도 이자', 'NEW',  '#0D47A1', NULL, NULL, 'PS_PARKING4', NULL, 'APP,PC_WEB', '2026-01-01', '2099-12-31', 1, 'Y', CURRENT_TIMESTAMP, 'SEED'),
 ('모바일로 한도조회부터 대출까지', '페퍼스마트대출 최저 연 6.9%',             NULL,   '#FFE082', NULL, NULL, 'PS_SMART',    NULL, 'APP,PC_WEB', '2026-01-01', '2099-12-31', 2, 'Y', CURRENT_TIMESTAMP, 'SEED'),
 ('2030 우대금리 적금',           '최고 연 4.5%, 매달 차곡차곡',             '특판', '#E8F5E9', NULL, NULL, 'PS_2030',     NULL, 'APP',        '2026-01-01', '2099-12-31', 3, 'Y', CURRENT_TIMESTAMP, 'SEED'),
 ('오토론 특별 금리',             '판매중지 상품과 연결된 카드는 자동 숨김', NULL,   '#FCE4EC', NULL, NULL, 'PS_AUTO',     NULL, 'APP,PC_WEB', '2026-01-01', '2099-12-31', 4, 'Y', CURRENT_TIMESTAMP, 'SEED'),
 ('[게시종료] 봄맞이 이벤트',      '게시기간이 지난 카드 (미노출)',           NULL,   '#FFF3E0', NULL, NULL, NULL, '/event/spring', 'APP,PC_WEB', '2026-03-01', '2026-03-31', 5, 'Y', CURRENT_TIMESTAMP, 'SEED'),
 ('[게시예정] 새해 정기예금 특판', '2027-01-01 부터 노출 (ADMIN 미리보기로 확인)', '예정', '#EDE7F6', NULL, NULL, 'PS_TIME_DEPOSIT', NULL, 'APP,PC_WEB', '2027-01-01', '2027-01-31', 1, 'Y', CURRENT_TIMESTAMP, 'SEED');

-- ---------------------------------------------------------------- ② 섹션 (요구사항 2: 불필요 카드 삭제 = USE_YN 'N')
INSERT INTO TB_PRD_TAB_SECTION (SECTION_CD, SECTION_NM, SECTION_DESC, SECTION_TYPE, CARD_ICON, LINK_URL, SORT_ORD, USE_YN, UPD_AT, UPD_BY) VALUES
 ('DEPOSIT',    '예금·적금',     '비대면으로 바로 가입할 수 있어요',   'PRODUCT', NULL,         NULL,                1, 'Y', CURRENT_TIMESTAMP, 'SEED'),
 ('LOAN',       '대출',          '한도조회부터 신청까지 앱에서',       'PRODUCT', NULL,         NULL,                2, 'Y', CURRENT_TIMESTAMP, 'SEED'),
 ('CALC',       '금융계산기',    '예상 이자와 월 상환액을 계산해 보세요', 'CARD',  'calculator', '/product/calculator', 3, 'Y', CURRENT_TIMESTAMP, 'SEED'),
 ('AUTO_PRICE', '내 차 시세조회', 'AS-IS 카드 - 요구사항에 따라 삭제(미사용)', 'CARD', 'auto-loan', '/product/auto-price', 4, 'N', CURRENT_TIMESTAMP, 'SEED');

-- ---------------------------------------------------------------- ③ 섹션별 상품 (요구사항: 하단 상품 목록 ADMIN 컨트롤)
INSERT INTO TB_PRD_TAB_ITEM (SECTION_CD, PRODUCT_CD, SORT_ORD, BADGE_TEXT, USE_YN, UPD_AT, UPD_BY) VALUES
 ('DEPOSIT', 'PS_PARKING4',     1, '인기', 'Y', CURRENT_TIMESTAMP, 'SEED'),
 ('DEPOSIT', 'PS_2030',         2, NULL,   'Y', CURRENT_TIMESTAMP, 'SEED'),
 ('DEPOSIT', 'PS_TIME_DEPOSIT', 3, NULL,   'Y', CURRENT_TIMESTAMP, 'SEED'),
 ('LOAN',    'PS_SMART',        1, '인기', 'Y', CURRENT_TIMESTAMP, 'SEED'),
 ('LOAN',    'PS_BIZ_MORTGAGE', 2, NULL,   'Y', CURRENT_TIMESTAMP, 'SEED'),
 ('LOAN',    'PS_AUTO',         3, NULL,   'Y', CURRENT_TIMESTAMP, 'SEED');
