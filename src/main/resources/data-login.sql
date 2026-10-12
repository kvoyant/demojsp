-- ==============================================================
-- 공통업무 : 저축은행 로그인 정책 샘플 계정 데이터
-- (모든 계정 기본 비밀번호: Password123!)
-- ==============================================================

INSERT INTO TB_MEMBER (MEMBER_ID, MEMBER_TYPE, ROLE_CD, PASSWORD, MEMBER_NM, CELL_NO, EMAIL, CI_VAL, LOGIN_FAIL_CNT, LOCK_YN, LAST_PW_MOD_DT)
VALUES
-- 1. 개인 고객 계정 (휴대폰 본인인증 CI 매핑 연동)
('customer01', 'INDIVIDUAL', 'ROLE_CUSTOMER', 
 'a109e36947ad56de1dca1cc49f0ef8ac9ad9a7b1aa0df41fb3c4cb73c1ff01ea', 
 '홍길동', '01012345678', 'customer01@bank.co.kr', 
 '2trxaKUfB0CDWOJc8yi1lUlnxgJFZ3nLfzTkBtOml3C42pwmOwBS3N1k7gMoqk9liMzplbHqvlNpAd0b8YOj0w==', 
 0, 'N', NOW()),

-- 2. 상담사 / 대출모집인 계정 (2FA OTP 필수)
('counselor01', 'COUNSELOR', 'ROLE_COUNSELOR', 
 'a109e36947ad56de1dca1cc49f0ef8ac9ad9a7b1aa0df41fb3c4cb73c1ff01ea', 
 '김상담', '01022223333', 'counselor01@bank.co.kr', 
 NULL, 
 0, 'N', NOW()),

-- 3. 관리자 / 심사역 계정 (2FA OTP 필수)
('admin01', 'ADMIN', 'ROLE_ADMIN', 
 'a109e36947ad56de1dca1cc49f0ef8ac9ad9a7b1aa0df41fb3c4cb73c1ff01ea', 
 '박관리', '01033334444', 'admin01@bank.co.kr', 
 NULL, 
 0, 'N', NOW()),

-- 4. 5회 오류로 잠긴 계정 (시뮬레이션 검증용)
('locked01', 'INDIVIDUAL', 'ROLE_CUSTOMER', 
 'a109e36947ad56de1dca1cc49f0ef8ac9ad9a7b1aa0df41fb3c4cb73c1ff01ea', 
 '이잠금', '01044445555', 'locked01@bank.co.kr', 
 '3urxbLVgC1DEXPKd9zj2mVmoxhKG04oMg0UlCuPnm4D53qxnPxCT4O2l8hNprl0mjNAqmcIrvmOqBe1c9ZPj1x==', 
 5, 'Y', NOW())
ON DUPLICATE KEY UPDATE 
    PASSWORD = VALUES(PASSWORD),
    CI_VAL = VALUES(CI_VAL),
    LOGIN_FAIL_CNT = VALUES(LOGIN_FAIL_CNT),
    LOCK_YN = VALUES(LOCK_YN);
