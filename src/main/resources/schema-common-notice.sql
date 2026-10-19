-- =============================================================================
-- [CF-BC-20 / 공통#28] 공지사항 (APP / ADMIN)  -  REQ-AP-609·610
--
--   TB_CO_NOTICE_TYPE  공지 유형   AS-IS 업무공지 / 서비스공지 / 시스템점검 / 기타 (TO-BE 검토중 → ADMIN 에서 추가·변경)
--   TB_CO_NOTICE_BIZ   업무 구분   "업무별 공지" 의 업무 목록 (대출신청, 예금·적금, 인증·보안 ...)
--   TB_CO_NOTICE       공지사항    노출 범위(SCOPE_CD) = ALL 전체 / PRODUCT 상품별 / BIZ 업무별
--                                  TARGET_CD = 상품별이면 TB_PRODUCT.PRODUCT_CD, 업무별이면 TB_CO_NOTICE_BIZ.BIZ_CD
--
--   게시 중 = USE_YN='Y' AND START_AT <= 지금 AND (END_AT IS NULL OR END_AT > 지금)
--   메인 띠 = 게시 중 + STRIP_YN='Y' 중 게시 시작이 가장 최근인 1건 (요구사항 2)
--   CREATE TABLE IF NOT EXISTS 이므로 여러 번 실행해도 기존 데이터는 유지된다.
-- =============================================================================

CREATE TABLE IF NOT EXISTS TB_CO_NOTICE_TYPE (
    TYPE_CD     VARCHAR(20)  NOT NULL PRIMARY KEY COMMENT '유형코드 (예: SYSTEM)',
    TYPE_NM     VARCHAR(20)  NOT NULL COMMENT '유형명 (예: 시스템점검)',
    BADGE_COLOR CHAR(7)      NOT NULL COMMENT '목록 유형 표시 색 #RRGGBB',
    SORT_ORD    INT          NOT NULL COMMENT '표시순서',
    USE_YN      CHAR(1)      NOT NULL DEFAULT 'Y' COMMENT '사용여부 (N = 새 공지에 선택 불가, 기존 공지는 표시)',
    UPD_AT      DATETIME     NOT NULL COMMENT '수정일시',
    UPD_BY      VARCHAR(50)  NOT NULL COMMENT '수정자'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='공지사항 유형';

CREATE TABLE IF NOT EXISTS TB_CO_NOTICE_BIZ (
    BIZ_CD    VARCHAR(20)  NOT NULL PRIMARY KEY COMMENT '업무코드 (예: LOAN)',
    BIZ_NM    VARCHAR(30)  NOT NULL COMMENT '업무명 (예: 대출신청)',
    SORT_ORD  INT          NOT NULL COMMENT '표시순서',
    USE_YN    CHAR(1)      NOT NULL DEFAULT 'Y' COMMENT '사용여부',
    UPD_AT    DATETIME     NOT NULL COMMENT '수정일시',
    UPD_BY    VARCHAR(50)  NOT NULL COMMENT '수정자'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='공지사항 업무 구분 (업무별 공지)';

CREATE TABLE IF NOT EXISTS TB_CO_NOTICE (
    NOTICE_ID  BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '공지 ID',
    SCOPE_CD   VARCHAR(10)   NOT NULL COMMENT '노출 범위 ALL 전체 / PRODUCT 상품별 / BIZ 업무별',
    TARGET_CD  VARCHAR(30)            COMMENT '대상 (상품별=상품코드, 업무별=업무코드, 전체=NULL)',
    TYPE_CD    VARCHAR(20)   NOT NULL COMMENT '공지 유형 → TB_CO_NOTICE_TYPE',
    TITLE      VARCHAR(100)  NOT NULL COMMENT '제목 (메인 띠에 노출)',
    CONTENT    MEDIUMTEXT    NOT NULL COMMENT '내용',
    START_AT   DATETIME      NOT NULL COMMENT '게시 시작일시 (포함)',
    END_AT     DATETIME               COMMENT '게시 종료일시 (미포함, NULL = 종료 없음)',
    FIX_YN     CHAR(1)       NOT NULL DEFAULT 'N' COMMENT '목록 상단 고정',
    STRIP_YN   CHAR(1)       NOT NULL DEFAULT 'N' COMMENT '메인 띠 노출 대상',
    POPUP_YN   CHAR(1)       NOT NULL DEFAULT 'N' COMMENT '메인 팝업 노출',
    USE_YN     CHAR(1)       NOT NULL DEFAULT 'Y' COMMENT '사용여부',
    VIEW_CNT   INT           NOT NULL DEFAULT 0 COMMENT '조회수',
    REG_AT     DATETIME      NOT NULL COMMENT '등록일시',
    REG_BY     VARCHAR(50)   NOT NULL COMMENT '등록자',
    UPD_AT     DATETIME      NOT NULL COMMENT '수정일시',
    UPD_BY     VARCHAR(50)   NOT NULL COMMENT '수정자',
    INDEX IDX_NOTICE_POST (USE_YN, START_AT),
    INDEX IDX_NOTICE_SCOPE (SCOPE_CD, TARGET_CD)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='공지사항';
