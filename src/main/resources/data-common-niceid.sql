-- =============================================================================
-- [공통#21] 본인인증 업무별 정책 샘플 (= 디지털어드민 등록값이라고 가정)
--   TB_CO_IDV_PURPOSE 가 비어 있을 때 1회만 적재. 운영은 ADMIN 에서 등록.
--   CONSENT_BIZ_CD 'IDENTITY_NICE' = 공통#22 약관동의 샘플 업무 (본인확인서비스 · 고유식별정보 · 비여신금융거래 약관)
--   유의고객 샘플 2건은 NiceIdSeeder 가 TB_WATCHLIST 에 "없을 때만" 넣는다 (기존 데이터 보존).
-- =============================================================================
INSERT INTO TB_CO_IDV_PURPOSE (PURPOSE_CD, PURPOSE_NM, CONSENT_BIZ_CD, SMS_YN, MO_YN, MIN_AGE, FOREIGNER_YN, WATCH_CHECK_YN, CODE_VALID_SEC, MAX_TRY, RESULT_VALID_MIN, USE_YN, UPD_AT, UPD_BY) VALUES
 ('SIGN_UP',    '회원가입',                 'IDENTITY_NICE', 'Y', 'Y', 14, 'Y', 'Y', 180, 5, 30, 'Y', CURRENT_TIMESTAMP, 'SEED'),
 ('NON_CREDIT', '비여신 거래 (조회·증명서)', 'IDENTITY_NICE', 'Y', 'Y', 14, 'Y', 'N', 180, 5, 30, 'Y', CURRENT_TIMESTAMP, 'SEED'),
 ('LOAN_APPLY', '대출신청',                 'IDENTITY_NICE', 'Y', 'Y', 19, 'N', 'Y', 180, 5, 30, 'Y', CURRENT_TIMESTAMP, 'SEED'),
 ('PW_RESET',   '비밀번호 재설정',          NULL,            'Y', 'N',  0, 'Y', 'N', 180, 3, 10, 'Y', CURRENT_TIMESTAMP, 'SEED');
