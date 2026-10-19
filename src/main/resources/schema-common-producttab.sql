-- =============================================================================
-- [CF-BC-20 / 공통#26] 메인화면 상품 탭 (APP / PC Web / ADMIN)  -  REQ-AP-606·607
--
--   화면 구성 (위 → 아래)
--   ┌──────────────────────────────┐
--   │ ① 상단 슬라이드 카드 (신규)   │  TB_PRD_TAB_SLIDE    ADMIN 등록 · 게시기간 · 표시순서
--   ├──────────────────────────────┤
--   │ ② 섹션 (예금·적금 / 대출 ...) │  TB_PRD_TAB_SECTION  섹션 순서 · 사용여부 (불필요 카드는 USE_YN='N')
--   │    └ 상품 목록                │  TB_PRD_TAB_ITEM     섹션별 상품 · 순서 · 배지 (상품 정보는 TB_PRODUCT)
--   └──────────────────────────────┘
--
--   상품 이름·설명·금리·아이콘·상품안내 URL·판매여부는 기존 상품 원장 TB_PRODUCT(공통#47·48) 값을 그대로 쓴다.
--   → 상품 정보를 두 군데서 관리하지 않도록 이 탭 테이블에는 "어느 상품을 어디에 몇 번째로" 만 저장.
--   CREATE TABLE IF NOT EXISTS 이므로 여러 번 실행해도 기존 데이터는 유지된다.
-- =============================================================================

CREATE TABLE IF NOT EXISTS TB_PRD_TAB_SLIDE (
    SLIDE_ID    BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '슬라이드 카드 ID',
    TITLE       VARCHAR(30)  NOT NULL COMMENT '메인 문구 (최대 30자)',
    SUB_TEXT    VARCHAR(40)           COMMENT '서브 문구 (최대 40자)',
    BADGE_TEXT  VARCHAR(10)           COMMENT '배지 (예: NEW, 특판)',
    BG_COLOR    CHAR(7)      NOT NULL COMMENT '카드 배경색 #RRGGBB (글자색은 대비에 맞춰 자동)',
    IMAGE_URL   VARCHAR(500)          COMMENT '이미지 URL (/ 또는 https://)',
    IMAGE_ALT   VARCHAR(100)          COMMENT '이미지 대체텍스트 (이미지가 있으면 필수)',
    PRODUCT_CD  VARCHAR(30)           COMMENT '연결 상품코드 → TB_PRODUCT (있으면 상품안내 URL 로 이동, 판매중지면 미노출)',
    LINK_URL    VARCHAR(500)          COMMENT '이동 URL (상품 연결이 없을 때)',
    CHANNELS    VARCHAR(30)  NOT NULL DEFAULT 'APP,PC_WEB' COMMENT '노출채널 (쉼표: APP,PC_WEB)',
    START_DT    DATE         NOT NULL COMMENT '게시 시작일 (포함)',
    END_DT      DATE         NOT NULL COMMENT '게시 종료일 (포함)',
    SORT_ORD    INT          NOT NULL COMMENT '표시순서',
    USE_YN      CHAR(1)      NOT NULL DEFAULT 'Y' COMMENT '사용여부',
    UPD_AT      DATETIME     NOT NULL COMMENT '수정일시',
    UPD_BY      VARCHAR(50)  NOT NULL COMMENT '수정자'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='상품 탭 - 상단 슬라이드 카드';

CREATE TABLE IF NOT EXISTS TB_PRD_TAB_SECTION (
    SECTION_CD    VARCHAR(30)  NOT NULL PRIMARY KEY COMMENT '섹션코드 (예: DEPOSIT, LOAN, CALC)',
    SECTION_NM    VARCHAR(30)  NOT NULL COMMENT '섹션 제목',
    SECTION_DESC  VARCHAR(60)           COMMENT '섹션 설명 한 줄',
    SECTION_TYPE  VARCHAR(10)  NOT NULL COMMENT 'PRODUCT 상품목록 / CARD 서비스 카드(시세조회·계산기 등)',
    CARD_ICON     VARCHAR(40)           COMMENT 'CARD 아이콘 (공통 아이콘 이름)',
    LINK_URL      VARCHAR(500)          COMMENT 'CARD 이동 URL',
    SORT_ORD      INT          NOT NULL COMMENT '섹션 표시순서',
    USE_YN        CHAR(1)      NOT NULL DEFAULT 'Y' COMMENT '사용여부 (N = 화면에서 삭제)',
    UPD_AT        DATETIME     NOT NULL COMMENT '수정일시',
    UPD_BY        VARCHAR(50)  NOT NULL COMMENT '수정자'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='상품 탭 - 섹션(카드 영역)';

CREATE TABLE IF NOT EXISTS TB_PRD_TAB_ITEM (
    SECTION_CD  VARCHAR(30)  NOT NULL COMMENT '섹션코드 → TB_PRD_TAB_SECTION',
    PRODUCT_CD  VARCHAR(30)  NOT NULL COMMENT '상품코드 → TB_PRODUCT.PRODUCT_CD',
    SORT_ORD    INT          NOT NULL COMMENT '섹션 안 표시순서',
    BADGE_TEXT  VARCHAR(10)           COMMENT '상품 배지 (예: 인기, NEW)',
    USE_YN      CHAR(1)      NOT NULL DEFAULT 'Y' COMMENT '사용여부',
    UPD_AT      DATETIME     NOT NULL COMMENT '수정일시',
    UPD_BY      VARCHAR(50)  NOT NULL COMMENT '수정자',
    PRIMARY KEY (SECTION_CD, PRODUCT_CD)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='상품 탭 - 섹션별 상품 목록';
