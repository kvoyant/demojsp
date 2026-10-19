-- =============================================================================
-- [CF-BC-04 / 공통#22] 약관동의 (DB 방식) 테이블  -  REQ-AP-602
--   화면·Java 에는 약관 코드·문구를 하드코딩하지 않는다. 아래 테이블 값만 읽어서 보여준다.
--
--   TB_CO_TERMS_BIZ        업무코드            (SIGN_UP, LOAN_APPLY ...)
--   TB_CO_TERMS            약관 (버전별 1행)   전문 · PDF · 시행일 · 해시
--   TB_CO_BIZ_TERMS        업무별 약관          어떤 업무에서 어떤 약관을 필수/선택, 몇 번째로 보여줄지
--   TB_CO_TERMS_AGREE      동의 내역(헤더)      누가 · 언제 · 어느 업무 · 어느 채널
--   TB_CO_TERMS_AGREE_DTL  동의 내역(상세)      약관별 동의여부 + 동의 당시 버전·해시 (증적)
--
--   "현재 약관" = 같은 약관코드 중 시행일(EFFECT_DT)이 오늘 이전인 가장 높은 버전 (USE_YN='Y')
--   → ADMIN 이 시행일을 미래로 개정본을 등록해 두면 그날부터 자동으로 바뀐다.
--   CREATE TABLE IF NOT EXISTS 이므로 여러 번 실행해도 기존 데이터는 그대로 유지된다.
-- =============================================================================

CREATE TABLE IF NOT EXISTS TB_CO_TERMS_BIZ (
    BIZ_CD    VARCHAR(30)  NOT NULL PRIMARY KEY COMMENT '업무코드 (예: SIGN_UP)',
    BIZ_NM    VARCHAR(50)  NOT NULL COMMENT '업무명',
    BIZ_DESC  VARCHAR(200)          COMMENT '설명 (어느 화면에서 쓰는지)',
    USE_YN    CHAR(1)      NOT NULL DEFAULT 'Y' COMMENT '사용여부',
    REG_AT    DATETIME     NOT NULL COMMENT '등록일시',
    REG_BY    VARCHAR(50)  NOT NULL COMMENT '등록자',
    UPD_AT    DATETIME     NOT NULL COMMENT '수정일시',
    UPD_BY    VARCHAR(50)  NOT NULL COMMENT '수정자'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='약관동의 업무코드';

CREATE TABLE IF NOT EXISTS TB_CO_TERMS (
    TERMS_CD   VARCHAR(30)  NOT NULL COMMENT '약관코드 (예: SVC_USE)',
    TERMS_VER  INT          NOT NULL COMMENT '버전 (1, 2, 3 ... 개정할 때마다 +1)',
    TERMS_NM   VARCHAR(100) NOT NULL COMMENT '약관명 (목록에 보이는 이름)',
    TERMS_CN   MEDIUMTEXT   NOT NULL COMMENT '약관 전문',
    PDF_URL    VARCHAR(500)          COMMENT 'PDF 원문 URL (/ 또는 https:// 만)',
    CN_HASH    CHAR(64)              COMMENT '전문 SHA-256 (증적용, NULL 이면 첫 동의 때 자동 계산)',
    EFFECT_DT  DATE         NOT NULL COMMENT '시행일 (이 날짜부터 현재 약관)',
    USE_YN     CHAR(1)      NOT NULL DEFAULT 'Y' COMMENT '사용여부 (N = 이 버전 사용 안 함)',
    REG_AT     DATETIME     NOT NULL COMMENT '등록일시',
    REG_BY     VARCHAR(50)  NOT NULL COMMENT '등록자',
    PRIMARY KEY (TERMS_CD, TERMS_VER)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='약관 (버전별)';

CREATE TABLE IF NOT EXISTS TB_CO_BIZ_TERMS (
    BIZ_CD    VARCHAR(30)  NOT NULL COMMENT '업무코드 → TB_CO_TERMS_BIZ',
    TERMS_CD  VARCHAR(30)  NOT NULL COMMENT '약관코드 → TB_CO_TERMS',
    REQ_YN    CHAR(1)      NOT NULL COMMENT '필수여부 (Y 필수 / N 선택)',
    SORT_ORD  INT          NOT NULL COMMENT '표시순서 (필수·선택 각각 안에서 오름차순)',
    USE_YN    CHAR(1)      NOT NULL DEFAULT 'Y' COMMENT '사용여부',
    UPD_AT    DATETIME     NOT NULL COMMENT '수정일시',
    UPD_BY    VARCHAR(50)  NOT NULL COMMENT '수정자',
    PRIMARY KEY (BIZ_CD, TERMS_CD)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='업무별 약관';

CREATE TABLE IF NOT EXISTS TB_CO_TERMS_AGREE (
    AGREE_NO   BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '동의번호',
    BIZ_CD     VARCHAR(30)  NOT NULL COMMENT '업무코드',
    CUST_ID    VARCHAR(50)           COMMENT '회원 ID (비로그인 NULL)',
    SESS_KEY   CHAR(64)     NOT NULL COMMENT '세션 식별값 SHA-256 (세션ID 원문은 저장하지 않음)',
    CHANNEL    VARCHAR(20)  NOT NULL COMMENT '채널 APP / PC_WEB / AGENT_WEB',
    CLIENT_IP  VARCHAR(45)           COMMENT '접속 IP',
    AGREE_AT   DATETIME     NOT NULL COMMENT '동의일시',
    INDEX IDX_TERMS_AGREE_CUST (CUST_ID, BIZ_CD),
    INDEX IDX_TERMS_AGREE_AT (AGREE_AT)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='약관동의 내역 (헤더, INSERT ONLY)';

CREATE TABLE IF NOT EXISTS TB_CO_TERMS_AGREE_DTL (
    AGREE_NO   BIGINT       NOT NULL COMMENT '동의번호 → TB_CO_TERMS_AGREE',
    TERMS_CD   VARCHAR(30)  NOT NULL COMMENT '약관코드',
    TERMS_VER  INT          NOT NULL COMMENT '동의 당시 약관 버전',
    CN_HASH    CHAR(64)     NOT NULL COMMENT '동의 당시 전문 SHA-256',
    REQ_YN     CHAR(1)      NOT NULL COMMENT '동의 당시 필수여부',
    AGREE_YN   CHAR(1)      NOT NULL COMMENT '동의여부 (선택약관 미동의 = N)',
    VIEW_YN    CHAR(1)      NOT NULL DEFAULT 'N' COMMENT '상세 열람여부',
    PRIMARY KEY (AGREE_NO, TERMS_CD)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='약관동의 내역 (상세, INSERT ONLY)';
