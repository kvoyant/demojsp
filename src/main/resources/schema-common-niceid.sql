-- =============================================================================
-- [CF-BC-03 / 공통#21] 휴대폰 본인인증 NICE ID (DB 방식)  -  REQ-AP-601
--
--   TB_CO_IDV_PURPOSE  업무별 본인인증 정책   어떤 업무에서 어떤 약관 · 인증방식(SMS/MO) · 나이 · 외국인 · 유의고객 검사를 쓸지
--   TB_CO_IDV_REQ      본인인증 요청·결과     요청 1건 = 1행 (요청 → 검증 → 결과/차단), 인증 이력·증적
--   TB_WATCHLIST       (기존 테이블)          CI 기반 유의고객 - 이미 있으면 그대로 쓰고, 없을 때만 같은 구조로 만든다
--
--   요구사항: 통신사 미지정(성명+생년월일+휴대폰번호), MO인증 결합 시 휴대폰번호 입력 생략,
--             비여신금융거래 약관 추가(→ 공통#22 약관동의 업무코드 IDENTITY_NICE),
--             주민번호 → CI 전환 후에도 유의고객 · 미성년자 · 외국인 체크가 동작하도록 CI·생년월일·내외국인 기준으로 검사
--   CREATE TABLE IF NOT EXISTS 이므로 여러 번 실행해도 기존 데이터는 유지된다.
-- =============================================================================

CREATE TABLE IF NOT EXISTS TB_CO_IDV_PURPOSE (
    PURPOSE_CD        VARCHAR(30)  NOT NULL PRIMARY KEY COMMENT '업무코드 (예: SIGN_UP, LOAN_APPLY)',
    PURPOSE_NM        VARCHAR(50)  NOT NULL COMMENT '업무명',
    CONSENT_BIZ_CD    VARCHAR(30)           COMMENT '본인인증 전 받을 약관 업무코드 → TB_CO_TERMS_BIZ (NULL = 약관 생략)',
    SMS_YN            CHAR(1)      NOT NULL DEFAULT 'Y' COMMENT '통신사 미지정 SMS 인증 사용',
    MO_YN             CHAR(1)      NOT NULL DEFAULT 'Y' COMMENT 'MO(기기 점유) 인증 사용 - 휴대폰번호 입력 생략',
    MIN_AGE           INT          NOT NULL DEFAULT 0   COMMENT '최소 만 나이 (0 = 제한 없음, 미성년자 차단은 19)',
    FOREIGNER_YN      CHAR(1)      NOT NULL DEFAULT 'Y' COMMENT '외국인 허용 (N = 외국인은 추가 절차 안내 후 차단)',
    WATCH_CHECK_YN    CHAR(1)      NOT NULL DEFAULT 'Y' COMMENT '유의고객(TB_WATCHLIST) 검사',
    CODE_VALID_SEC    INT          NOT NULL DEFAULT 180 COMMENT '인증번호 / MO 대기 유효시간(초)',
    MAX_TRY           INT          NOT NULL DEFAULT 5   COMMENT '인증번호 입력 최대 횟수',
    RESULT_VALID_MIN  INT          NOT NULL DEFAULT 30  COMMENT '인증 결과를 업무에서 쓸 수 있는 시간(분)',
    USE_YN            CHAR(1)      NOT NULL DEFAULT 'Y' COMMENT '사용여부',
    UPD_AT            DATETIME     NOT NULL COMMENT '수정일시',
    UPD_BY            VARCHAR(50)  NOT NULL COMMENT '수정자'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='본인인증 업무별 정책';

CREATE TABLE IF NOT EXISTS TB_CO_IDV_REQ (
    REQ_NO       VARCHAR(32)  NOT NULL PRIMARY KEY COMMENT '요청번호 (추측 불가 난수 - 화면이 들고 다닌다)',
    PURPOSE_CD   VARCHAR(30)  NOT NULL COMMENT '업무코드 → TB_CO_IDV_PURPOSE',
    METHOD       VARCHAR(10)  NOT NULL COMMENT 'SMS 통신사 미지정 / MO 기기 점유',
    CHANNEL      VARCHAR(20)  NOT NULL COMMENT 'APP / PC_WEB / AGENT_WEB',
    AGREE_NO     BIGINT                COMMENT '약관동의 번호 → TB_CO_TERMS_AGREE (공통#22)',
    CUST_ID      VARCHAR(50)           COMMENT '회원 ID (비로그인 NULL)',
    SESS_KEY     CHAR(64)     NOT NULL COMMENT '세션 식별값 SHA-256 (같은 사람 확인)',
    CUST_NM      VARCHAR(50)  NOT NULL COMMENT '입력 성명',
    BIRTH_DT     CHAR(8)      NOT NULL COMMENT '입력 생년월일 yyyyMMdd',
    PHONE_MASK   VARCHAR(20)           COMMENT '입력 휴대폰번호 마스킹 (MO 는 NULL)',
    PROVIDER_TX  VARCHAR(64)           COMMENT 'NICE 거래번호',
    MO_CODE      VARCHAR(20)           COMMENT 'MO 문자에 보낼 코드',
    TRY_CNT      INT          NOT NULL DEFAULT 0 COMMENT '인증번호 오입력 횟수',
    EXPIRE_AT    DATETIME     NOT NULL COMMENT '인증번호 / MO 대기 만료일시',
    STATUS       VARCHAR(20)  NOT NULL COMMENT 'REQUESTED 진행중 / VERIFIED 성공 / BLOCKED 정책차단 / FAILED 실패 / EXPIRED 만료',
    RESULT_CD    VARCHAR(30)           COMMENT 'OK / MINOR / FOREIGNER / WATCHLIST / CODE_MISMATCH / TRY_EXCEEDED / NAME_MISMATCH / PROVIDER_ERROR',
    CI_VAL       VARCHAR(88)           COMMENT '연계정보 CI (운영: DB 암호화 대상)',
    DI_VAL       VARCHAR(64)           COMMENT '중복가입확인정보 DI',
    CELL_NO      VARCHAR(20)           COMMENT '인증된 휴대폰번호 (운영: DB 암호화 대상)',
    GENDER       CHAR(1)               COMMENT '성별 M/F',
    NATIONAL_CD  CHAR(1)               COMMENT '내외국인 L 내국인 / F 외국인',
    AGE          INT                   COMMENT '인증 시점 만 나이',
    CLIENT_IP    VARCHAR(45)           COMMENT '접속 IP',
    REQ_AT       DATETIME     NOT NULL COMMENT '요청일시',
    DONE_AT      DATETIME              COMMENT '완료(성공·차단·실패)일시',
    INDEX IDX_IDV_REQ_AT (REQ_AT),
    INDEX IDX_IDV_CUST (CUST_ID),
    INDEX IDX_IDV_CI (CI_VAL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='본인인증 요청·결과 이력';

-- 기존 유의고객 테이블 (schema-nice-auth.sql 과 같은 구조) - 이미 있으면 아무 일도 하지 않는다
CREATE TABLE IF NOT EXISTS TB_WATCHLIST (
    WATCH_SEQ BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '일련번호',
    CI_VAL VARCHAR(88) NOT NULL COMMENT '연계정보(CI)',
    CUST_NM VARCHAR(50) NOT NULL COMMENT '고객명',
    RISK_GRADE VARCHAR(20) NOT NULL DEFAULT 'HIGH' COMMENT '위험등급 (CRITICAL, HIGH, CAUTION)',
    RISK_TYPE VARCHAR(30) NOT NULL COMMENT '유의유형',
    RISK_REASON VARCHAR(200) NOT NULL COMMENT '유의사유 상세',
    BLOCK_YN CHAR(1) DEFAULT 'Y' COMMENT '거래차단여부(Y/N)',
    REG_DT DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    REG_USER_ID VARCHAR(50) DEFAULT 'SYSTEM' COMMENT '등록자ID',
    UNIQUE KEY UK_WATCHLIST_CI (CI_VAL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='유의고객 관리(CI기반)';
