-- ==============================================================
-- 공통업무 : 1원 계좌인증 초기 기준정보 및 대출신청 계좌 데이터
-- ==============================================================

-- 1. 금융기관(은행/증권) 표준 코드 적재
INSERT INTO TB_BANK_CODE (BANK_CD, BANK_NM, BANK_TYPE, SORT_ORDER, USE_YN) VALUES
('004', 'KB국민은행', 'BANK', 1, 'Y'),
('088', '신한은행', 'BANK', 2, 'Y'),
('020', '우리은행', 'BANK', 3, 'Y'),
('081', '하나은행', 'BANK', 4, 'Y'),
('011', 'NH농협은행', 'BANK', 5, 'Y'),
('003', 'IBK기업은행', 'BANK', 6, 'Y'),
('090', '카카오뱅크', 'BANK', 7, 'Y'),
('092', '토스뱅크', 'BANK', 8, 'Y'),
('089', '케이뱅크', 'BANK', 9, 'Y'),
('023', 'SC제일은행', 'BANK', 10, 'Y'),
('027', '한국씨티은행', 'BANK', 11, 'Y'),
('031', 'DGB대구은행(iM뱅크)', 'BANK', 12, 'Y'),
('032', 'BNK부산은행', 'BANK', 13, 'Y'),
('034', '광주은행', 'BANK', 14, 'Y'),
('039', 'BNK경남은행', 'BANK', 15, 'Y'),
('037', '전북은행', 'BANK', 16, 'Y'),
('035', '제주은행', 'BANK', 17, 'Y'),
('071', '우체국', 'BANK', 18, 'Y'),
('045', '새마을금고', 'BANK', 19, 'Y'),
('048', '신협', 'BANK', 20, 'Y'),
('050', '상호저축은행', 'BANK', 21, 'Y')
ON DUPLICATE KEY UPDATE 
    BANK_NM = VALUES(BANK_NM),
    SORT_ORDER = VALUES(SORT_ORDER),
    USE_YN = VALUES(USE_YN);

-- 2. 대출신청 대출금 입금/출금 계좌 샘플 데이터 (요구사항 1: 자동완성 처리 테스트용)
INSERT INTO TB_LOAN_ACCOUNT (LOAN_NO, MEMBER_ID, CUST_NM, PRODUCT_NM, BANK_CD, BANK_NM, ACCOUNT_NO, ACCOUNT_HOLDER_NM, ACCOUNT_TYPE) VALUES
('LN202610130001', 'customer01', '홍길동', '중금리 직장인 신용대출', '004', 'KB국민은행', '123402-04-567890', '홍길동', 'DISBURSEMENT_REPAYMENT'),
('LN202610130002', 'counselor01', '김상담', '햇살론 (근로자 연계)', '088', '신한은행', '110-333-444555', '김상담', 'DISBURSEMENT_REPAYMENT'),
('LN202610130003', 'locked01', '이잠금', '서민금융 소상공인대출', '020', '우리은행', '1002-888-999000', '이잠금', 'DISBURSEMENT_REPAYMENT'),
('LN202610130004', 'customer01', '홍길동', '모바일 마이너스통장 대출', '090', '카카오뱅크', '3333-01-9876543', '홍길동', 'DISBURSEMENT_REPAYMENT')
ON DUPLICATE KEY UPDATE 
    CUST_NM = VALUES(CUST_NM),
    PRODUCT_NM = VALUES(PRODUCT_NM),
    BANK_CD = VALUES(BANK_CD),
    BANK_NM = VALUES(BANK_NM),
    ACCOUNT_NO = VALUES(ACCOUNT_NO),
    ACCOUNT_HOLDER_NM = VALUES(ACCOUNT_HOLDER_NM);
