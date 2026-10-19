-- =============================================================================
-- [공통#25] 1원 계좌인증 업무 Context 샘플 (= 디지털어드민 등록값이라고 가정)
--   TB_CO_ACCT_BIZ 가 비어 있을 때 1회만 적재. 운영은 ADMIN 에서 등록.
--   화면 분리 대상 업무 목록은 "확인 필요" 사항 → 업무가 정해지면 ADMIN 에서 추가 (코드 수정 없음)
--   은행코드 · 대출계좌 샘플은 AcctVerifySeeder 가 기존 테이블이 비어 있을 때만 넣는다.
-- =============================================================================
INSERT INTO TB_CO_ACCT_BIZ (BIZ_CD, BIZ_NM, SCREEN_TYPE, PREFILL_YN, OWNER_CHECK_YN, MEMO_PREFIX, CODE_VALID_MIN, MAX_TRY, RESULT_VALID_MIN, USE_YN, UPD_AT, UPD_BY) VALUES
 ('LOAN_DISBURSE', '대출금 입금계좌 등록',   'LOAN',   'Y', 'Y', '페퍼', 10, 5, 30, 'Y', CURRENT_TIMESTAMP, 'SEED'),
 ('LOAN_REPAY',    '원리금 출금계좌 등록',   'LOAN',   'Y', 'Y', '페퍼', 10, 5, 30, 'Y', CURRENT_TIMESTAMP, 'SEED'),
 ('AUTH_MEANS',    '인증수단 등록 (본인확인)', 'COMMON', 'N', 'Y', '페퍼', 10, 3, 10, 'Y', CURRENT_TIMESTAMP, 'SEED'),
 ('REFUND_ACCT',   '환불계좌 등록',          'COMMON', 'N', 'Y', '페퍼', 10, 5, 30, 'Y', CURRENT_TIMESTAMP, 'SEED');
