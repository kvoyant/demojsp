-- ==============================================================
-- 공통업무 : NICE ID 본인인증 & CI 데이터 샘플 데이터
-- ==============================================================

-- 1. 비여신금융거래 본인인증 약관 등록
INSERT INTO TB_TERMS (TERMS_ID, TERMS_TITLE, TERMS_VERSION, REQUIRED_YN, CATEGORY, CONTENT, SORT_ORDER, USE_YN)
VALUES
('TERMS_NON_CREDIT_01', '비여신금융거래 본인확인서비스 이용약관', '1.0', 'Y', 'NON_CREDIT', 
'제1조 (목적)\n본 약관은 저축은행의 비여신금융거래(조회, 증명서발급, 회원정보관리 등) 시 NICE평가정보(주)가 제공하는 휴대폰 본인확인서비스를 이용함에 있어 필요한 고객과 당행 간의 권리, 의무 및 책임사항을 규정함을 목적으로 합니다.\n\n제2조 (용어의 정의)\n1. "본인확인서비스"란 주민등록번호를 대체하여 CI(연계정보) 및 DI(중복가입확인정보)를 생성하여 본인 여부를 식별하는 서비스를 말합니다.\n2. "통신사 미지정 인증"이란 이용자가 이동통신사를 별도로 선택하지 않고 성명, 생년월일, 휴대폰번호만으로 일괄 확인하여 진행하는 인증 방식을 의미합니다.\n\n제3조 (이용자의 의무)\n이용자는 본인의 정보만을 입력하여야 하며, 타인의 정보를 도용하여 인증을 시도할 경우 전자금융거래법 및 관련 법령에 따라 처벌받을 수 있습니다.', 1, 'Y'),

('TERMS_NICE_01', 'NICE평가정보(주) 본인확인서비스 이용약관', '2.1', 'Y', 'NICE',
'제1조 (목적)\n본 약관은 NICE평가정보 주식회사(이하 "회사")가 제공하는 휴대폰 본인확인서비스(이하 "서비스")의 이용조건 및 절차에 관한 기본사항을 정함을 목적으로 합니다.\n\n제2조 (서비스의 내용)\n회사는 이동통신사(SK텔레콤, KT, LG유플러스 및 알뜰폰 사업자)로부터 제공받은 식별정보를 바탕으로 이용자의 실명 및 본인명의 휴대폰 여부를 확인하고, 연계정보(CI) 및 중복가입확인정보(DI)를 생성하여 제휴 금융기관에 안전하게 전송합니다.\n\n제3조 (개인정보 보호 및 파기)\n회사는 본인확인 목적 달성 후 관계 법령에 따라 보존 의무가 있는 경우를 제외하고는 수집된 본인확인 정보를 즉시 파기합니다.', 2, 'Y'),

('TERMS_TELCO_ALL', '통신사(SKT/KT/LGU+/MVNO) 통합 이용약관', '3.0', 'Y', 'TELCO',
'제1조 (목적)\n본 약관은 통신 3사(SK텔레콤, KT, LG유플러스) 및 각 사의 통신망을 이용하는 알뜰폰(MVNO) 사업자가 공동으로 제공하는 통신사 미지정 휴대폰 본인확인 서비스 이용에 관한 사항을 규정합니다.\n\n제2조 (인증번호 발송 및 본인인증)\n통신사는 이용자가 입력한 휴대폰번호와 명의자 일치 여부를 대조한 후 일회용 인증번호(OTP)를 문자메시지(SMS)로 발송합니다.\n\n제3조 (통신사 선택 생략에 관한 동의)\n이용자는 별도의 통신사 선택 절차 없이 입력된 휴대폰번호의 가입 통신사를 자동 식별하여 본인확인 절차가 진행되는 것에 동의합니다.', 3, 'Y'),

('TERMS_PRIVACY_01', '개인정보 수집·이용 및 고유식별정보 처리 동의', '1.0', 'Y', 'NON_CREDIT',
'1. 수집 및 이용 목적: 비여신금융거래 시 본인 식별, 본인확인 이력 관리, 부정사용(FDS) 및 금융사고 예방\n2. 수집 항목: 성명, 생년월일, 성별, 내외국인 구분, 휴대폰번호, 연계정보(CI), 중복가입확인정보(DI), 접속IP, 단말기정보\n3. 보유 및 이용 기간: 전자금융거래법 및 신용정보법에 따라 거래 종료일로부터 5년 보관 후 파기\n4. 고유식별정보 처리: 본인확인기관을 통한 CI/DI 생성 목적으로 주민등록번호 기반의 연계정보를 처리함에 동의합니다.\n* 귀하는 동의를 거부할 권리가 있으나, 필수 동의 미동의 시 본인확인 서비스 이용이 제한됩니다.', 4, 'Y')
ON DUPLICATE KEY UPDATE TERMS_TITLE = VALUES(TERMS_TITLE);

-- 2. 유의고객(Watchlist) 샘플 데이터 등록 (CI 기반)
INSERT INTO TB_WATCHLIST (CI_VAL, CUST_NM, RISK_GRADE, RISK_TYPE, RISK_REASON, BLOCK_YN, REG_DT, REG_USER_ID)
VALUES
('ci_sample_watchlist_fraud_001_A8f9b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6=', '사고유의자', 'CRITICAL', 'FDS_FRAUD', '보이스피싱 금융사고 연루 계좌 명의인 (FDS 자동 등록)', 'Y', NOW(), 'FDS_SYSTEM'),
('ci_sample_watchlist_theft_002_B1c2d3e4f5g6h7i8j9k0l1m2n3o4p5q6r7s8t9u0v1w2x3y4z5a6b7=', '도용의심자', 'HIGH', 'IDENTITY_THEFT', '휴대폰 대포폰 개통 및 타인 명의도용 신고 접수', 'Y', NOW(), 'COMPLIANCE'),
('ci_sample_watchlist_delin_003_C3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6a7b8c9=', '상습연체자', 'CAUTION', 'LOAN_DELINQUENT', '다중채무 장기연체 유의고객 (모니터링 대상)', 'Y', NOW(), 'CREDIT_RISK')
ON DUPLICATE KEY UPDATE RISK_REASON = VALUES(RISK_REASON);

-- 3. 기존 정상 고객 CI 샘플
INSERT INTO TB_CUST_CI (CI_VAL, DI_VAL, CUST_ID, CUST_NM, BIRTH_DATE, GENDER, NATIONAL_INFO, CELL_NO)
VALUES
('ci_sample_normal_user_001_K8f9b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6a7b8=', 'di_sample_001_normal_1234567890abcdef', 'CUST_10001', '홍길동', '19900515', '1', '1', '01012345678')
ON DUPLICATE KEY UPDATE CUST_NM = VALUES(CUST_NM);

-- 4. 9/30 이기종 CI 데이터 이관 임시(스테이징) 샘플 데이터 등록
INSERT INTO TB_CI_MIGRATION_TEMP (LEGACY_SYSTEM_CD, LEGACY_CUST_ID, CUST_NM, CI_VAL, DI_VAL, BIRTH_DATE, GENDER, NATIONAL_INFO, CELL_NO, RRN_HASH, VALID_STATUS, MIGRATION_STATUS, SOURCE_EXTRACT_DT)
VALUES
-- 정상 건 1
('LEGACY_CORE', 'LEG_00101', '김철수', '2trxaKUfB0CDWOJc8yi1lUlnxgJFZ3nLfzTkBtOml3C42pwmOwBS3N1k7gMoqk9liMzplbHqvlNpAd0b8YOj0w==', 'di_sample_mig_00101', '19850312', '1', '1', '01022223333', 'hash_850312_1xxxxxx', 'PENDING', 'READY', '2026-09-30'),
-- 정상 건 2
('LEGACY_CORE', 'LEG_00102', '이영희', 'r+4QPLWLLwVU4lEeb54aKlPEQKUpMgDDhxt34/EAWq/XrDkvpk0HawfuwPRInQT3ONKaemM/10YhTh8gHdFmfg==', 'di_sample_mig_00102', '19920824', '2', '1', '01033334444', 'hash_920824_2xxxxxx', 'PENDING', 'READY', '2026-09-30'),
-- 정상 건 3 (외국인)
('CRM_OLD', 'LEG_00103', '존스미스', 'CyJ4W+E1mbwaedIipwFs3ExLF00NlnHhdG/GmM4IvvGBg9OxaBwZLM1H3Wx08DHX/3/NV7xU4wIjv+kU37ngUg==', 'di_sample_mig_00103', '19881105', '1', '2', '01044445555', 'hash_881105_5xxxxxx', 'PENDING', 'READY', '2026-09-30'),
-- 정상 건 4
('CRM_OLD', 'LEG_00104', '박지민', 'CXH7Qfq/dLFnHQ0Z78bwq6L5//L1xhBoPyAsw2YIuULlzsLvkuHpOlq/UqDvbLfbGmZvxwNHMmayzYDaqIuxQw==', 'di_sample_mig_00104', '19951210', '1', '1', '01055556666', 'hash_951210_1xxxxxx', 'PENDING', 'READY', '2026-09-30'),
-- 오류 건 1 : CI 길이 부족 (88자 미만)
('EXTERNAL_AGENCY', 'LEG_00105', '최오류', 'INVALID_SHORT_CI_12345', 'di_sample_mig_00105', '19910101', '1', '1', '01066667777', 'hash_910101_1xxxxxx', 'PENDING', 'READY', '2026-09-30'),
-- 오류 건 2 : CI 중복 건 (동일 CI 값으로 이미 추출된 데이터)
('EXTERNAL_AGENCY', 'LEG_00106', '김철수_중복', '2trxaKUfB0CDWOJc8yi1lUlnxgJFZ3nLfzTkBtOml3C42pwmOwBS3N1k7gMoqk9liMzplbHqvlNpAd0b8YOj0w==', 'di_sample_mig_00106', '19850312', '1', '1', '01022223333', 'hash_850312_1xxxxxx', 'PENDING', 'READY', '2026-09-30');
