# 공통모듈 DB 저장소 가이드 (MySQL demo_db02 · MyBatis)

대상: 배너팝업·상단슬라이드배너(공통#39·44·45), 이어하기·회사 마스터(공통#43·49·50), 뱅킹메뉴(공통#46), 상품·홈 표시설정(공통#47·48·51)

## 1. 설정 (`application.yaml`)

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/demo_db02?useUnicode=true&allowMultiQueries=true&serverTimezone=UTC
    username: yhkim
    password: yhkim

common:
  store:
    type: db             # db = MySQL + MyBatis (기본) / memory = DB 없이 메모리 (재기동 시 초기화)
    init-schema: true    # 기동 시 schema-common-home.sql 실행 (CREATE TABLE IF NOT EXISTS → 기존 데이터 유지). 운영 false
    seed-if-empty: true  # 테이블이 비어 있을 때만 yaml seed 적재 (샘플 데이터). 운영 false
```

- 첫 기동 시 테이블 6개 생성 + 샘플 데이터 적재 → 이후 재기동해도 **데이터 유지** (ADMIN 에서 바꾼 내용 그대로).
- 샘플 데이터로 되돌리려면 해당 테이블을 비우고(`TRUNCATE`) 재기동.
- DB 연결 실패 시 앱 기동은 계속되고 경고 로그만 남는다 (공통모듈 화면만 오류). DB 없이 화면만 볼 때는 `type: memory`.

## 2. 테이블 (`src/main/resources/schema-common-home.sql`)

| 테이블 | 용도 | 공통# |
|---|---|---|
| `TB_CO_SEQ` | 채번 (MySQL 시퀀스 대용, 행 잠금 → 서버 여러 대에서도 중복 없음) | - |
| `TB_CO_PARTNER` | 회사(당행/제휴사) 마스터 - 회사명 DB | 43 |
| `TB_RESUME_POINT` | 이어하기 (신청 진행 단계) | 43·49·50 |
| `TB_BANNER_POPUP` | 배너팝업목록 (배너팝업 / 상단슬라이드배너, 배경색) | 39·44·45 |
| `TB_BANKING_MENU` | 뱅킹메뉴목록 (전체메뉴) | 46 |
| `TB_PRODUCT` | 수신/여신 상품 + 홈 표시여부·순서 | 47·48·51 |
| `TB_CO_TERMS_BIZ` 외 4개 | 약관동의 DB 방식 - `schema-common-consent.sql` ([COMMON_CONSENT_DB_GUIDE.md](COMMON_CONSENT_DB_GUIDE.md)) | 22 |

매퍼: `src/main/resources/mapper/common/*.xml`, 인터페이스 `com.example.demojsp.common.*.mapper` (`@MapperScan` 추가).
저장소 구현은 `common.store.type` 으로 하나만 등록된다 (`MyBatis*Repository` ↔ `InMemory*Repository`). 서비스·API·화면은 변경 없음.

## 3. 운영 반영 시

- `init-schema: false`, `seed-if-empty: false` → DDL 은 DBA 가 `schema-common-home.sql` 로 반영.
- 기존 운영 테이블(배너팝업·상품·메뉴)이 있으면 같은 컬럼을 매퍼 XML 에서 기존 테이블로 바꿔 연결 (서비스 코드 수정 불필요).
- 상품 홈 표시순서 동시 저장 대비: 서비스 검증에 더해 DB 제약 권장 (예: `HOME_DISP_YN='Y'` 행만 순서 유니크 - 생성 컬럼 + UNIQUE).

## 4. 테스트

`CommonStoreDbTest` (6건) - 실제 `schema-common-home.sql` + 매퍼 XML + `application.yaml` seed 를 H2(MySQL 호환 모드)에서 실행:
초기화·재기동 중복 적재 없음, 이어하기·배너·뱅킹메뉴·상품 DB 반영과 재기동 후 유지, 채번 동시 200건 중복 없음.
