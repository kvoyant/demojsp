-- =============================================================================
-- [CF-BC-06 / 공통#25] 1원 계좌인증 (DB 방식)  -  REQ-AP-605
--
--   서비스(1원 송금 · 인증코드 검증)는 하나, 화면만 업무 Context 로 나눈다.
--   TB_CO_ACCT_BIZ      업무 Context   어떤 화면(LOAN 대출 입출금계좌용 / COMMON 타 업무 공통)·자동완성·시도횟수·유효시간을 쓸지
--   TB_CO_ACCT_VERIFY   인증 요청·결과  요청 1건 = 1행 (예금주 확인 → 1원 송금 → 인증코드 확인), 인증 이력·증적
--   TB_BANK_CODE        (기존 테이블)   금융기관 코드 - 이미 있으면 그대로 쓰고, 없을 때만 같은 구조로 만든다
--   TB_LOAN_ACCOUNT     (기존 테이블)   대출신청 대출금 입금/출금 계좌 - 자동완성(요구사항 1)의 원천 데이터
--
--   CREATE TABLE IF NOT EXISTS 이므로 여러 번 실행해도 기존 데이터는 유지된다.
-- =============================================================================

CREATE TABLE IF NOT EXISTS TB_CO_ACCT_BIZ (
    BIZ_CD            VARCHAR(30)  NOT NULL PRIMARY KEY COMMENT '업무코드 (예: LOAN_DISBURSE)',
    BIZ_NM            VARCHAR(50)  NOT NULL COMMENT '업무명',
    SCREEN_TYPE       VARCHAR(10)  NOT NULL COMMENT 'LOAN 대출 입출금계좌 입력용 화면 / COMMON 타 업무 공통 화면',
    PREFILL_YN        CHAR(1)      NOT NULL DEFAULT 'N' COMMENT '대출금 입금/출금계좌 자동완성 (LOAN 화면)',
    OWNER_CHECK_YN    CHAR(1)      NOT NULL DEFAULT 'Y' COMMENT '예금주 = 본인 확인 (타인 계좌 차단)',
    MEMO_PREFIX       VARCHAR(6)   NOT NULL COMMENT '1원 입금자명 앞 글자 (통장에 "페퍼1234" 로 표시)',
    CODE_VALID_MIN    INT          NOT NULL DEFAULT 10 COMMENT '인증코드 유효시간(분)',
    MAX_TRY           INT          NOT NULL DEFAULT 5  COMMENT '인증코드 입력 최대 횟수',
    RESULT_VALID_MIN  INT          NOT NULL DEFAULT 30 COMMENT '인증 결과를 업무에서 쓸 수 있는 시간(분)',
    USE_YN            CHAR(1)      NOT NULL DEFAULT 'Y' COMMENT '사용여부',
    UPD_AT            DATETIME     NOT NULL COMMENT '수정일시',
    UPD_BY            VARCHAR(50)  NOT NULL COMMENT '수정자'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='1원 계좌인증 업무 Context';

CREATE TABLE IF NOT EXISTS TB_CO_ACCT_VERIFY (
    REQ_NO       VARCHAR(32)  NOT NULL PRIMARY KEY COMMENT '요청번호 (추측 불가 난수)',
    BIZ_CD       VARCHAR(30)  NOT NULL COMMENT '업무코드 → TB_CO_ACCT_BIZ',
    CHANNEL      VARCHAR(20)  NOT NULL COMMENT 'APP / PC_WEB / AGENT_WEB',
    CUST_ID      VARCHAR(50)           COMMENT '회원 ID (비로그인 NULL)',
    SESS_KEY     CHAR(64)     NOT NULL COMMENT '세션 식별값 SHA-256',
    CUST_NM      VARCHAR(50)  NOT NULL COMMENT '고객명 (예금주 비교 기준)',
    BANK_CD      VARCHAR(3)   NOT NULL COMMENT '은행코드',
    BANK_NM      VARCHAR(50)  NOT NULL COMMENT '은행명',
    ACCT_NO      VARCHAR(30)  NOT NULL COMMENT '계좌번호 숫자만 (운영: DB 암호화 대상)',
    ACCT_MASK    VARCHAR(40)  NOT NULL COMMENT '마스킹 계좌번호 (화면·이력 표시)',
    HOLDER_NM    VARCHAR(50)           COMMENT '조회된 예금주명',
    SOURCE       VARCHAR(10)  NOT NULL COMMENT 'PREFILL 자동완성 그대로 / INPUT 직접 입력',
    PREFILL_ID   VARCHAR(50)           COMMENT '자동완성 원천 (대출신청번호 등)',
    MODIFIED_YN  CHAR(1)      NOT NULL DEFAULT 'N' COMMENT '자동완성을 [수정]해서 바꿨는지 (요구사항 2)',
    PROVIDER_TX  VARCHAR(64)           COMMENT '펌뱅킹 송금 거래번호',
    CODE_HASH    CHAR(64)              COMMENT '인증코드 SHA-256 (원문 저장 안 함)',
    TRY_CNT      INT          NOT NULL DEFAULT 0 COMMENT '인증코드 오입력 횟수',
    EXPIRE_AT    DATETIME     NOT NULL COMMENT '인증코드 만료일시',
    STATUS       VARCHAR(20)  NOT NULL COMMENT 'SENT 1원 송금됨 / VERIFIED 성공 / FAILED 실패 / EXPIRED 만료',
    RESULT_CD    VARCHAR(30)           COMMENT 'OK / NOT_OWNER / NO_ACCOUNT / TRANSFER_FAIL / TRY_EXCEEDED / EXPIRED',
    CLIENT_IP    VARCHAR(45)           COMMENT '접속 IP',
    REQ_AT       DATETIME     NOT NULL COMMENT '요청일시',
    DONE_AT      DATETIME              COMMENT '완료일시',
    INDEX IDX_ACCT_VERIFY_REQ_AT (REQ_AT),
    INDEX IDX_ACCT_VERIFY_CUST (CUST_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='1원 계좌인증 요청·결과 이력';

-- 기존 기준정보·업무 테이블 (schema-account-cert.sql 과 같은 구조) - 이미 있으면 아무 일도 하지 않는다
CREATE TABLE IF NOT EXISTS TB_BANK_CODE (
    BANK_CD VARCHAR(3) PRIMARY KEY COMMENT '은행코드(금융결제원 3자리 표준코드)',
    BANK_NM VARCHAR(50) NOT NULL COMMENT '은행명',
    BANK_TYPE VARCHAR(20) DEFAULT 'BANK' COMMENT '기관구분 (BANK: 은행, SEC: 증권사)',
    SORT_ORDER INT DEFAULT 0 COMMENT '정렬순서',
    USE_YN CHAR(1) DEFAULT 'Y' COMMENT '사용여부 (Y/N)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='금융기관 표준 코드 마스터';

CREATE TABLE IF NOT EXISTS TB_LOAN_ACCOUNT (
    LOAN_NO VARCHAR(50) PRIMARY KEY COMMENT '대출신청번호',
    MEMBER_ID VARCHAR(50) COMMENT '고객ID',
    CUST_NM VARCHAR(50) NOT NULL COMMENT '고객명',
    PRODUCT_NM VARCHAR(100) DEFAULT '햇살론 / 중금리 신용대출' COMMENT '대출상품명',
    BANK_CD VARCHAR(3) NOT NULL COMMENT '등록 은행코드',
    BANK_NM VARCHAR(50) NOT NULL COMMENT '등록 은행명',
    ACCOUNT_NO VARCHAR(50) NOT NULL COMMENT '대출금 입금/출금 계좌번호',
    ACCOUNT_HOLDER_NM VARCHAR(50) NOT NULL COMMENT '예금주명',
    ACCOUNT_TYPE VARCHAR(30) DEFAULT 'DISBURSEMENT_REPAYMENT' COMMENT '계좌용도(대출금입금/원리금출금)',
    REG_DT DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    INDEX IDX_LOAN_MEMBER (MEMBER_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='대출신청 대출금 입금/출금 계좌 마스터';
