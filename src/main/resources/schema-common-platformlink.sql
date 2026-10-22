-- =============================================================================
-- [CF-TC-07 · CF-TC-02 · CF-BC-10 / 공통#1 REQ-AP-074] 플랫폼 대출 연계 - 한도조회
--   "제휴 플랫폼(토스·카카오페이·핀다 등)과 주고받는 전문은 그대로, 안쪽 한도조회 엔진만 신규 CSS 로 교체"
--   제휴사별 차이(전문 형식·결과코드·타임아웃·엔진 전환)는 전부 이 테이블들로 관리한다 (제휴사별 if 분기 코드 금지).
--   기동 시 CommonStoreInitializer 가 CREATE TABLE IF NOT EXISTS 로 생성한다.
-- =============================================================================

-- 제휴사 (연계 채널) 마스터
CREATE TABLE IF NOT EXISTS TB_LNK_PARTNER (
    PARTNER_CD     VARCHAR(20)  NOT NULL COMMENT '제휴사코드 (URL 경로에 사용: /api/link/platform/{PARTNER_CD}/limit)',
    PARTNER_NM     VARCHAR(50)  NOT NULL COMMENT '제휴사명',
    ADAPTER_CD     VARCHAR(30)  NOT NULL COMMENT '전문 변환기 (PartnerAdapter.adapterCd) - 제휴사 전문 ↔ 공통모델',
    CSS_ENGINE     VARCHAR(10)  NOT NULL DEFAULT 'LEGACY' COMMENT '한도조회 엔진: LEGACY 현행 / NEW 신규 CSS / SHADOW 현행 응답 + 신규 병행비교',
    PRODUCTS       VARCHAR(60)  NOT NULL COMMENT '허용 상품유형 (쉼표: CREDIT,AUTO,MORTGAGE)',
    TIMEOUT_MS     INT          NOT NULL COMMENT '제휴사가 기다려 주는 시간(ms) - 이 안에 반드시 응답',
    MARGIN_MS      INT          NOT NULL DEFAULT 300 COMMENT '응답 여유(ms): CSS 제한시간 = TIMEOUT_MS - MARGIN_MS',
    ALLOW_IPS      VARCHAR(300) NOT NULL DEFAULT '*' COMMENT '허용 송신 IP (쉼표, * = 제한 없음 - local 전용)',
    SECRET_REF     VARCHAR(50)  NOT NULL COMMENT '서명키 참조명 → application.yaml common.platform-link.secrets.{SECRET_REF} (키 원문은 DB에 두지 않음)',
    USE_YN         CHAR(1)      NOT NULL DEFAULT 'Y' COMMENT '서비스 사용 (N = 즉시 중단: 제휴사에 "서비스 중단" 코드 응답)',
    UPD_AT         DATETIME     NOT NULL COMMENT '수정일시',
    UPD_BY         VARCHAR(50)  NOT NULL COMMENT '수정자',
    PRIMARY KEY (PARTNER_CD)
) COMMENT '플랫폼 대출 연계 - 제휴사';

-- 상품유형 ↔ 당행 상품 (CSS 상품유형별 당행 상품코드, ADMIN 간편한도조회 목록 구분)
CREATE TABLE IF NOT EXISTS TB_LNK_PRODUCT (
    CSS_PRODUCT    VARCHAR(10)  NOT NULL COMMENT 'CSS 상품유형: CREDIT 신용 / AUTO 오토 / MORTGAGE 모기지',
    PRODUCT_CD     VARCHAR(30)  NOT NULL COMMENT '당행 상품코드',
    PRODUCT_NM     VARCHAR(50)  NOT NULL COMMENT '당행 상품명',
    ADMIN_LIST     VARCHAR(30)  NOT NULL COMMENT 'ADMIN 간편한도조회 목록 (신용 / 오토 / 모기지)',
    USE_YN         CHAR(1)      NOT NULL DEFAULT 'Y' COMMENT '사용',
    PRIMARY KEY (CSS_PRODUCT)
) COMMENT '플랫폼 대출 연계 - 상품유형';

-- 제휴사가 쓰는 상품코드 (같은 오토대출도 제휴사마다 부르는 코드가 다르다)
CREATE TABLE IF NOT EXISTS TB_LNK_PARTNER_PRD (
    PARTNER_CD     VARCHAR(20)  NOT NULL COMMENT '제휴사코드',
    CSS_PRODUCT    VARCHAR(10)  NOT NULL COMMENT 'CSS 상품유형',
    OUTER_PRD_CD   VARCHAR(30)  NOT NULL COMMENT '제휴사 전문에 실을 상품코드',
    PRIMARY KEY (PARTNER_CD, CSS_PRODUCT)
) COMMENT '플랫폼 대출 연계 - 제휴사 상품코드';

-- 결과코드 변환 (내부 결과코드 → 제휴사 결과코드·문구)
CREATE TABLE IF NOT EXISTS TB_LNK_CODE_MAP (
    PARTNER_CD     VARCHAR(20)  NOT NULL COMMENT '제휴사코드',
    INNER_CD       VARCHAR(20)  NOT NULL COMMENT '내부 결과코드 (APPROVED, REJECTED, TIMEOUT, CSS_ERROR, INVALID, PRODUCT_NA, STOPPED, AUTH_FAIL)',
    OUTER_CD       VARCHAR(20)  NOT NULL COMMENT '제휴사 결과코드',
    OUTER_MSG      VARCHAR(100) NOT NULL COMMENT '제휴사에 보낼 문구',
    PRIMARY KEY (PARTNER_CD, INNER_CD)
) COMMENT '플랫폼 대출 연계 - 결과코드 변환';

-- 한도조회 요청·응답 이력 (1건 = 1행). 개인정보는 해시·마스킹만 저장
CREATE TABLE IF NOT EXISTS TB_LNK_LIMIT_REQ (
    LNK_REQ_NO     CHAR(32)     NOT NULL COMMENT '연계 요청번호 (32자 난수)',
    PARTNER_CD     VARCHAR(20)  NOT NULL COMMENT '제휴사코드',
    PARTNER_REQ_NO VARCHAR(64)  NOT NULL COMMENT '제휴사 요청번호 (중복 요청 판단 키)',
    CSS_PRODUCT    VARCHAR(10)           COMMENT '결정된 상품유형',
    CI_HASH        CHAR(64)              COMMENT 'CI SHA-256 (CI 원문 저장 안 함)',
    CUST_NM_MASK   VARCHAR(50)           COMMENT '고객명 마스킹',
    ANNUAL_INCOME  BIGINT                COMMENT '연소득 (원 단위로 환산한 값)',
    CSS_ENGINE     VARCHAR(10)           COMMENT '사용한 엔진 (LEGACY / NEW)',
    STATUS         VARCHAR(10)  NOT NULL COMMENT 'DONE 응답완료 / FAILED 실패응답',
    RESULT_CD      VARCHAR(20)           COMMENT '내부 결과코드',
    OUTER_CD       VARCHAR(20)           COMMENT '제휴사에 보낸 결과코드',
    LIMIT_AMT      BIGINT                COMMENT '승인 한도 (원)',
    RATE_BP        INT                   COMMENT '금리 (bp, 1250 = 12.50%)',
    CSS_TX_NO      VARCHAR(40)           COMMENT 'CSS 거래번호',
    CSS_MS         INT                   COMMENT 'CSS 소요시간(ms)',
    TOTAL_MS       INT                   COMMENT '전체 처리시간(ms) - 제휴사 TIMEOUT_MS 이내여야 함',
    SHADOW_RESULT  VARCHAR(300)          COMMENT 'SHADOW 모드: 신규 CSS 병행 결과 요약 (비교용, 제휴사에는 안 보냄)',
    RES_BODY       TEXT                  COMMENT '제휴사에 보낸 응답 전문 (중복 요청 시 그대로 재전송)',
    CLIENT_IP      VARCHAR(45)           COMMENT '송신 IP',
    REQ_AT         DATETIME(3)  NOT NULL COMMENT '수신일시',
    RES_AT         DATETIME(3)           COMMENT '응답일시',
    PRIMARY KEY (LNK_REQ_NO),
    UNIQUE KEY UK_LNK_LIMIT_REQ (PARTNER_CD, PARTNER_REQ_NO)
) COMMENT '플랫폼 대출 연계 - 한도조회 이력';
