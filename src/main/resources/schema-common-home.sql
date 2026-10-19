-- ==============================================================
-- 공통모듈 (공통#39·43~51) 테이블 DDL - MySQL 8 (demo_db02)
--  - 기동 시 CommonStoreInitializer 가 실행 (common.store.init-schema=true, CREATE TABLE IF NOT EXISTS → 기존 데이터 유지)
--  - 운영은 init-schema=false 로 두고 DBA 가 이 DDL 을 반영
-- ==============================================================

-- 0. 공통 채번 (MySQL 시퀀스 대용 - 행 잠금으로 서버 여러 대에서도 중복 없음)
CREATE TABLE IF NOT EXISTS TB_CO_SEQ (
    SEQ_NM   VARCHAR(50) NOT NULL PRIMARY KEY COMMENT '채번 이름',
    NEXT_VAL BIGINT      NOT NULL            COMMENT '마지막 발급 번호'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='공통 채번';

-- 1. 회사(당행/제휴사) 마스터 - 공통#43 "회사명 DB 구성" (이어하기 로고·회사명)
CREATE TABLE IF NOT EXISTS TB_CO_PARTNER (
    PARTNER_CD VARCHAR(30)  NOT NULL PRIMARY KEY COMMENT '회사코드 (OWN=당행)',
    PARTNER_NM VARCHAR(40)  NOT NULL             COMMENT '회사명',
    LOGO_URL   VARCHAR(500)                      COMMENT '로고 이미지 URL (없으면 대체 로고)',
    OWN_YN     CHAR(1)      NOT NULL DEFAULT 'N' COMMENT '당행 여부',
    USE_YN     CHAR(1)      NOT NULL DEFAULT 'Y' COMMENT '사용 여부'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='회사(당행/제휴사) 마스터';

-- 2. 이어하기 - 공통#43 홈 / #49 저축 / #50 저축 탭 툴팁
CREATE TABLE IF NOT EXISTS TB_RESUME_POINT (
    RESUME_ID        BIGINT       NOT NULL PRIMARY KEY COMMENT '이어하기 ID',
    CUSTOMER_ID      VARCHAR(50)  NOT NULL COMMENT '고객 ID',
    PRODUCT_TYPE     VARCHAR(10)  NOT NULL COMMENT 'DEPOSIT 수신 / LOAN 여신',
    BIZ_CD           VARCHAR(30)  NOT NULL COMMENT '업무코드',
    BIZ_NM           VARCHAR(20)  NOT NULL COMMENT '업무명',
    PRODUCT_CD       VARCHAR(30)  NOT NULL COMMENT '상품코드',
    PRODUCT_NM       VARCHAR(40)  NOT NULL COMMENT '상품명',
    PARTNER_CD       VARCHAR(30)  NOT NULL COMMENT '회사코드 → TB_CO_PARTNER',
    STEP_NM          VARCHAR(20)  NOT NULL COMMENT '멈춘 단계명',
    STEP_NO          INT          NOT NULL COMMENT '멈춘 단계 순번',
    TOTAL_STEPS      INT          NOT NULL DEFAULT 0 COMMENT '전체 단계 수 (0=표시 안 함)',
    RESUME_URL       VARCHAR(500) NOT NULL COMMENT '이어하기 링크',
    STATUS           VARCHAR(20)  NOT NULL COMMENT 'IN_PROGRESS / COMPLETED / CANCELED',
    STARTED_AT       DATETIME     NOT NULL COMMENT '최초 신청일시',
    LAST_ACTIVITY_AT DATETIME     NOT NULL COMMENT '마지막 진행일시 (최신 1건 기준)',
    EXPIRES_AT       DATETIME              COMMENT '만료일시 (NULL=만료 없음)',
    INDEX IDX_RESUME_CUST (CUSTOMER_ID, STATUS)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='이어하기(신청 진행 단계)';

-- 3. 배너팝업 / 상단슬라이드배너 - 공통#39 · #44 · #45 (디지털어드민 배너팝업목록)
CREATE TABLE IF NOT EXISTS TB_BANNER_POPUP (
    BANNER_ID     BIGINT        NOT NULL PRIMARY KEY COMMENT '배너 ID',
    POPUP_TYPE    VARCHAR(20)   NOT NULL COMMENT '배너팝업구분 BANNER_POPUP / TOP_SLIDE',
    CATEGORY      VARCHAR(30)   NOT NULL COMMENT '공지유형',
    CHANNELS      VARCHAR(100)  NOT NULL COMMENT '노출채널 (쉼표 구분: APP,PC_WEB)',
    TITLE         VARCHAR(50)   NOT NULL COMMENT '제목 / 상품소개 메인텍스트',
    CONTENT       VARCHAR(1000) NOT NULL COMMENT '내용 / 서브텍스트',
    IMAGE_URL     VARCHAR(500)           COMMENT '이미지 URL',
    ALT_TEXT      VARCHAR(100)           COMMENT '이미지 대체텍스트',
    LINK_URL      VARCHAR(500)           COMMENT '링크 URL',
    BG_COLOR      CHAR(7)                COMMENT '배경 색상 #RRGGBB (상단슬라이드배너)',
    START_AT      DATETIME      NOT NULL COMMENT '게시 시작 (포함)',
    END_AT        DATETIME      NOT NULL COMMENT '게시 종료 (미포함)',
    DISPLAY_ORDER INT           NOT NULL COMMENT '표시순서',
    USE_YN        CHAR(1)       NOT NULL DEFAULT 'Y' COMMENT '사용여부',
    REG_AT        DATETIME      NOT NULL COMMENT '등록일시',
    REG_BY        VARCHAR(50)   NOT NULL COMMENT '등록자',
    UPD_AT        DATETIME      NOT NULL COMMENT '수정일시',
    UPD_BY        VARCHAR(50)   NOT NULL COMMENT '수정자',
    INDEX IDX_BANNER_PERIOD (POPUP_TYPE, START_AT, END_AT)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='배너팝업목록 (배너팝업/상단슬라이드배너)';

-- 4. 뱅킹메뉴 (전체메뉴) - 공통#46 ADMIN 뱅킹메뉴목록
CREATE TABLE IF NOT EXISTS TB_BANKING_MENU (
    MENU_ID       BIGINT       NOT NULL PRIMARY KEY COMMENT '메뉴 ID',
    CATEGORY_CD   VARCHAR(30)  NOT NULL COMMENT '카테고리 코드',
    CATEGORY_NM   VARCHAR(30)  NOT NULL COMMENT '카테고리명',
    MENU_NM       VARCHAR(20)  NOT NULL COMMENT '메뉴명',
    MENU_URL      VARCHAR(300) NOT NULL COMMENT '이동 URL',
    DISPLAY_ORDER INT          NOT NULL COMMENT '카테고리 안 표시순서 (1..N)',
    USE_YN        CHAR(1)      NOT NULL DEFAULT 'Y' COMMENT '사용여부',
    NEW_YN        CHAR(1)      NOT NULL DEFAULT 'N' COMMENT '신규 표시',
    UPD_AT        DATETIME     NOT NULL COMMENT '수정일시',
    UPD_BY        VARCHAR(50)  NOT NULL COMMENT '수정자',
    INDEX IDX_MENU_CATEGORY (CATEGORY_CD, DISPLAY_ORDER)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='뱅킹메뉴목록 (전체메뉴)';

-- 5. 수신/여신 상품 - 공통#47 추천 상품 · #48 상품설정(홈 표시여부·순서) · #51 입출금통장 연결
CREATE TABLE IF NOT EXISTS TB_PRODUCT (
    PRODUCT_ID    BIGINT       NOT NULL PRIMARY KEY COMMENT '상품 ID',
    PRODUCT_CD    VARCHAR(30)  NOT NULL COMMENT '상품코드',
    PRODUCT_KIND  VARCHAR(10)  NOT NULL COMMENT 'DEPOSIT 수신 / LOAN 여신',
    SUB_TYPE      VARCHAR(20)           COMMENT '상품유형명 (입출금, 적금, 신용대출 ...)',
    PRODUCT_NM    VARCHAR(30)  NOT NULL COMMENT '상품명',
    SUMMARY       VARCHAR(40)           COMMENT '한줄 설명',
    RATE_TEXT     VARCHAR(20)           COMMENT '금리 문구',
    ICON          VARCHAR(40)           COMMENT '공통 아이콘 이름',
    DETAIL_URL    VARCHAR(300) NOT NULL COMMENT '상품안내페이지 URL',
    ON_SALE_YN    CHAR(1)      NOT NULL DEFAULT 'Y' COMMENT '판매여부',
    HOME_DISP_YN  CHAR(1)      NOT NULL DEFAULT 'N' COMMENT '홈 표시여부 (공통#48)',
    HOME_DISP_ORD INT                   COMMENT '홈 표시순서 1~3 (공통#48)',
    UPD_AT        DATETIME     NOT NULL COMMENT '수정일시',
    UPD_BY        VARCHAR(50)  NOT NULL COMMENT '수정자',
    UNIQUE KEY UK_PRODUCT_CD (PRODUCT_CD)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='수신/여신 상품 (상품설정)';
