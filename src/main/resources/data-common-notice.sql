-- =============================================================================
-- [공통#28] 공지사항 샘플 데이터 (= 디지털어드민 등록값이라고 가정)
--   TB_CO_NOTICE_TYPE 이 비어 있을 때 1회만 적재 (common.store.seed-if-empty=true). 운영은 ADMIN 에서 등록.
--   상품별 공지의 상품코드는 TB_PRODUCT 샘플(공통#47·48)과 같다.
--   주의: 문구 안에 세미콜론을 쓰지 않는다 (SQL 문장 구분자).
-- =============================================================================

-- ---------------------------------------------------------------- 공지 유형 (AS-IS 4종, TO-BE 는 검토 후 ADMIN 에서 추가)
INSERT INTO TB_CO_NOTICE_TYPE (TYPE_CD, TYPE_NM, BADGE_COLOR, SORT_ORD, USE_YN, UPD_AT, UPD_BY) VALUES
 ('BIZ',     '업무공지',   '#0D47A1', 1, 'Y', CURRENT_TIMESTAMP, 'SEED'),
 ('SERVICE', '서비스공지', '#1B7A3D', 2, 'Y', CURRENT_TIMESTAMP, 'SEED'),
 ('SYSTEM',  '시스템점검', '#C62828', 3, 'Y', CURRENT_TIMESTAMP, 'SEED'),
 ('ETC',     '기타',       '#4B5563', 4, 'Y', CURRENT_TIMESTAMP, 'SEED');

-- ---------------------------------------------------------------- 업무 구분 (업무별 공지)
INSERT INTO TB_CO_NOTICE_BIZ (BIZ_CD, BIZ_NM, SORT_ORD, USE_YN, UPD_AT, UPD_BY) VALUES
 ('LOAN',     '대출신청',   1, 'Y', CURRENT_TIMESTAMP, 'SEED'),
 ('DEPOSIT',  '예금·적금',  2, 'Y', CURRENT_TIMESTAMP, 'SEED'),
 ('AUTH',     '인증·보안',  3, 'Y', CURRENT_TIMESTAMP, 'SEED'),
 ('TRANSFER', '이체',       4, 'Y', CURRENT_TIMESTAMP, 'SEED');

-- ---------------------------------------------------------------- 공지사항
INSERT INTO TB_CO_NOTICE (SCOPE_CD, TARGET_CD, TYPE_CD, TITLE, CONTENT, START_AT, END_AT, FIX_YN, STRIP_YN, POPUP_YN, USE_YN, VIEW_CNT, REG_AT, REG_BY, UPD_AT, UPD_BY) VALUES
 ('ALL', NULL, 'SYSTEM', '[시스템점검] 10/25(토) 02:00~06:00 전산 시스템 정기점검 안내',
'안정적인 서비스 제공을 위해 아래와 같이 시스템 정기점검을 실시합니다.

■ 점검일시: 2026년 10월 25일(토) 02:00 ~ 06:00 (4시간)
■ 중단 서비스: 모바일앱·인터넷뱅킹 전체 (조회·이체·대출신청)
■ 점검 시간에는 ATM 출금도 일시 중단됩니다.

이용에 불편을 드려 죄송합니다.', '2026-10-15 09:00:00', '2026-10-25 06:00:00', 'Y', 'Y', 'Y', 'Y', 0, CURRENT_TIMESTAMP, 'SEED', CURRENT_TIMESTAMP, 'SEED'),

 ('PRODUCT', 'PS_SMART', 'BIZ', '페퍼스마트대출 최저금리 인하 안내 (연 7.2% → 6.9%)',
'페퍼스마트대출의 최저금리가 아래와 같이 인하됩니다.

■ 적용일: 2026년 10월 18일 신규 신청분부터
■ 최저금리: 연 7.2% → 연 6.9%
■ 기존 대출 고객은 금리인하요구권으로 신청할 수 있습니다.', '2026-10-18 09:00:00', NULL, 'N', 'Y', 'N', 'Y', 0, CURRENT_TIMESTAMP, 'SEED', CURRENT_TIMESTAMP, 'SEED'),

 ('ALL', NULL, 'SERVICE', '디지털페퍼 앱 메인화면 개편 안내',
'더 편리한 이용을 위해 앱 메인화면이 새롭게 바뀌었습니다.
홈·저축·대출 탭과 상품 탭에서 필요한 메뉴를 빠르게 찾아보세요.', '2026-10-10 09:00:00', NULL, 'N', 'N', 'N', 'Y', 0, CURRENT_TIMESTAMP, 'SEED', CURRENT_TIMESTAMP, 'SEED'),

 ('PRODUCT', 'PS_PARKING4', 'SERVICE', '페퍼스 파킹통장4 이자 지급일 변경 안내',
'파킹통장4의 이자 지급일이 매월 셋째 주 금요일에서 매월 말일로 변경됩니다.
■ 변경 적용: 2026년 11월 지급분부터', '2026-10-05 09:00:00', NULL, 'N', 'N', 'N', 'Y', 0, CURRENT_TIMESTAMP, 'SEED', CURRENT_TIMESTAMP, 'SEED'),

 ('BIZ', 'LOAN', 'BIZ', '대출신청 서류 제출 방법 변경 안내',
'대출신청 시 소득서류를 앱에서 촬영해 바로 제출할 수 있습니다.
팩스 제출은 2026년 12월 31일까지만 받습니다.', '2026-10-12 09:00:00', NULL, 'N', 'N', 'N', 'Y', 0, CURRENT_TIMESTAMP, 'SEED', CURRENT_TIMESTAMP, 'SEED'),

 ('BIZ', 'AUTH', 'SYSTEM', '[종료] 공동인증서 갱신 서비스 일시 중단',
'인증기관 작업으로 공동인증서 갱신이 일시 중단되었습니다. (게시 종료된 공지 예시)', '2026-10-16 09:00:00', '2026-10-17 09:00:00', 'N', 'Y', 'N', 'Y', 0, CURRENT_TIMESTAMP, 'SEED', CURRENT_TIMESTAMP, 'SEED'),

 ('ALL', NULL, 'ETC', '[예약] 개인정보처리방침 개정 예정 안내',
'2026년 11월 1일부터 개정된 개인정보처리방침이 시행됩니다. (게시 예약 공지 예시)', '2026-11-01 00:00:00', NULL, 'N', 'Y', 'N', 'Y', 0, CURRENT_TIMESTAMP, 'SEED', CURRENT_TIMESTAMP, 'SEED'),

 ('BIZ', 'DEPOSIT', 'BIZ', '[미사용] 정기예금 만기 자동해지 안내',
'사용여부 N 공지 예시 - 사용자 화면에 나오지 않습니다.', '2026-10-01 09:00:00', NULL, 'N', 'N', 'N', 'N', 0, CURRENT_TIMESTAMP, 'SEED', CURRENT_TIMESTAMP, 'SEED');
