# [CF-BC-04 / 공통#22] 약관동의 - DB 방식 가이드

REQ-AP-602 "약관동의 필수/선택 항목" 을 **DB 테이블 + MyBatis** 로 다시 만든 공통모듈이다.
약관 목록·문구·필수/선택·순서·버전은 **전부 DB 값**이고, 업무 화면과 Java 코드에는 업무코드만 들어간다.

> 기존 yaml 방식(`common.terms`, `/common/terms/sample`, `COMMON_TERMS_GUIDE.md`)은 **그대로 두었다.**
> 새 업무는 이 DB 방식을 쓴다. 두 모듈은 패키지·URL·테이블이 달라 서로 영향이 없다.

## 1. 기존 방식과 무엇이 다른가

| | 기존 (common.terms) | DB 방식 (common.consent) |
|---|---|---|
| 약관 데이터 | application.yaml seed → 메모리 | **DB 테이블 5개** (ADMIN 에서 바로 수정) |
| 구조 | Repository 인터페이스 + InMemory 구현 + 세션 매니저 등 20여 클래스 | 프로젝트 표준 **Controller → Service → Mapper(XML)** |
| 동의 결과 전달 | HTTP 세션에 저장 (TTL) | **동의번호(agreeNo)** 를 다음 단계로 넘기고 서버가 DB 로 확인 |
| 개정 | 등록 즉시 교체 | **시행일** 지정 → 그날부터 자동 적용 (미리 등록 가능) |
| ADMIN | API 만 | 업무코드 · 업무별 약관 · 약관 등록/개정 · 동의내역 **화면** 포함 |

## 2. 테이블 (`src/main/resources/schema-common-consent.sql`)

```
TB_CO_TERMS_BIZ        업무코드            SIGN_UP / LOAN_APPLY / DEPOSIT_OPEN / IDENTITY_NICE / ID_CARD
TB_CO_TERMS            약관 (버전별 1행)   PK(TERMS_CD, TERMS_VER) · 전문 · PDF · 시행일(EFFECT_DT) · 전문 해시
TB_CO_BIZ_TERMS        업무별 약관          PK(BIZ_CD, TERMS_CD) · 필수여부(REQ_YN) · 표시순서 · 사용여부
TB_CO_TERMS_AGREE      동의내역 헤더        AGREE_NO(자동증가) · 업무 · 회원ID · 세션키(해시) · 채널 · IP · 일시
TB_CO_TERMS_AGREE_DTL  동의내역 상세        약관별 동의여부 · 동의 당시 버전/해시/필수여부 · 열람여부
```

- **현재 약관** = 같은 약관코드 중 `USE_YN='Y'` 이고 `EFFECT_DT <= 오늘` 인 가장 높은 버전 (`ConsentMapper.xml` 의 `currentVer`).
- 같은 약관을 여러 업무에 연결할 수 있고, 업무마다 필수/선택을 다르게 줄 수 있다 (예: 마케팅 동의 = 회원가입 선택, 다른 업무 필수).
- 동의내역은 **INSERT ONLY**. 업무별 약관을 나중에 바꿔도 상세에 동의 당시 버전·필수여부·해시가 남아 증적이 유지된다.
- 샘플 데이터: `data-common-consent.sql` (TB_CO_TERMS_BIZ 가 비어 있을 때 1회). 전문 해시는 NULL 로 두면 첫 동의 때 서버가 계산해 채운다.
- 기동 시 생성/적재는 기존 공통 DB 설정을 따른다: `common.store.init-schema`, `common.store.seed-if-empty` (운영은 둘 다 false, DDL 은 DBA 반영).

## 3. 소스 구성 (`com.example.demojsp.common.consent`)

```
controller/  ConsentApiController       사용자 API  /api/common/consent
             ConsentAdminApiController  ADMIN API   /api/common/consent-admin
             ConsentAdminAuth           ADMIN 권한 (ROLE_ADMIN 또는 local admin-api-open)
             ConsentSampleController    샘플/어드민 화면, "다음 단계" 예시
service/     ConsentService(Impl)       목록 · 전문 · 동의 저장 · checkAgreed
             ConsentAdminService(Impl)  업무코드 · 업무별 약관 · 약관 등록/개정 · 동의내역
             ConsentUser                동의한 사람 (회원ID / 세션키 / IP)
mapper/      ConsentMapper              + resources/mapper/common/ConsentMapper.xml
dto/         ConsentBizDto, ConsentTermsDto, ConsentListDto, ConsentAgreeDto, ConsentAgreeItemDto, ConsentResult ...
exception/   ConsentErrorCode(CNS-xxx), ConsentException, ConsentExceptionHandler
config/      ConsentProperties(common.consent.*), ConsentSeeder(샘플 적재)

static/js/common/consent-agree.js     화면 공통 컴포넌트
static/css/common/consent-agree.css
WEB-INF/views/common/consent/sample.jsp   사용자 화면 샘플
WEB-INF/views/common/consent/admin.jsp    디지털어드민 약관관리
```

## 4. 업무 개발자 사용법

**① 화면 (JSP)** - 체크박스·전체동의·상세보기 HTML 을 만들지 않는다.
```html
<link rel="stylesheet" href="/css/common/consent-agree.css">
<script src="/js/common/consent-agree.js"></script>
<script>
ConsentAgree.open({
    bizCd: 'SIGN_UP',              // 업무코드만
    channel: 'APP',                // APP | PC_WEB | AGENT_WEB
    onComplete: function (r) {     // r.agreeNo, r.agreedTerms, r.declinedTerms
        location.href = '/member/join/info?agreeNo=' + r.agreeNo;
    },
    onCancel: function () { history.back(); }
});
</script>
```
- 1페이지 필수 → [다음] → 2페이지 선택 → [확인]. 선택약관이 없는 업무는 1페이지에서 끝난다.
- 약관 이름을 누르면 전문 → [동의]. 열람 여부도 저장된다(VIEW_YN).

**② 서버 (다음 단계 Controller/Service)** - 화면이 보낸 agreeNo 를 그대로 믿지 않고 DB 로 확인한다.
```java
ConsentResult r = consentService.checkAgreed(agreeNo, "SIGN_UP", request);  // 실패 시 ConsentException
member.setTermsAgreeNo(r.getAgreeNo());                                    // 업무 데이터 ↔ 동의내역 연결
member.setMarketingYn(r.isAgreed("MARKETING_USE") ? "Y" : "N");            // 선택약관 반영
```
확인 항목: 같은 업무코드 · 같은 사람(로그인 회원ID, 비로그인은 같은 세션) · 필수 전부 동의 · `valid-minutes` 이내.

**③ 새 업무 추가** - 코드 수정 없음. 어드민 화면에서 업무코드 등록 → 업무별 약관 연결 → 화면에서 `bizCd` 만 바꿔 호출.

## 5. API

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/common/consent/{bizCd}` | 약관 목록 `{bizNm, required[], optional[]}` (전문 제외) |
| GET | `/api/common/consent/terms/{termsCd}/{termsVer}` | 약관 전문 |
| POST | `/api/common/consent/{bizCd}/agree` | 동의 저장 → `{agreeNo, agreedTerms, declinedTerms}` |
| GET/POST | `/api/common/consent-admin/biz` | [ADMIN] 업무코드 목록 / 등록·수정 |
| GET/PUT | `/api/common/consent-admin/biz/{bizCd}/terms` | [ADMIN] 업무별 약관 조회 / 목록 전체 저장 |
| GET | `/api/common/consent-admin/biz/{bizCd}/preview?baseDt=` | [ADMIN] 기준일 사용자 화면 미리보기 |
| GET/POST | `/api/common/consent-admin/terms` | [ADMIN] 약관 목록 / 등록·개정 (다음 버전) |
| GET | `/api/common/consent-admin/terms/{termsCd}/versions` | [ADMIN] 버전 이력 |
| GET | `/api/common/consent-admin/agreements?bizCd=&custId=` | [ADMIN] 동의내역 |
| GET | `/api/common/consent-admin/agreements/{agreeNo}` | [ADMIN] 동의내역 상세 (증적) |

동의 요청 예:
```json
{ "channel": "APP",
  "items": [ {"termsCd":"SVC_USE","termsVer":1,"agreeYn":"Y","viewYn":"Y"},
             {"termsCd":"PRIVACY_COLLECT","termsVer":2,"agreeYn":"Y"},
             {"termsCd":"MARKETING_USE","termsVer":1,"agreeYn":"N"} ] }
```

오류코드: `CNS-001` 업무코드 없음/미사용, `002` 연결된 약관 없음, `003` 약관 없음, `004` 요청 오류,
`101` 필수 미동의, `102` 개정됨(화면이 새 약관으로 다시 표시), `103` 다른 업무 약관,
`201` 동의내역 없음(남의 번호 포함), `202` 유효시간 초과, `301`·`302` ADMIN 입력 오류, `401` ADMIN 권한 없음.

## 6. 설정 (`application.yaml`)

```yaml
common:
  consent:
    valid-minutes: 30      # 동의 후 업무 서버 확인까지 허용 시간(분), 0 = 제한 없음
    admin-api-open: true   # local 전용! 운영 false (ROLE_ADMIN 세션 필요)
```

## 7. 실행 / 테스트

```
사용자 화면  http://localhost:8080/common/consent/sample
어드민       http://localhost:8080/common/consent/admin
테스트 허브  http://localhost:8080/common/test-hub

./mvnw test -Dtest=ConsentDbTest   # 실제 DDL·샘플 SQL·매퍼 XML 을 H2(MySQL 모드)에서 실행, 8건
```

## 8. 운영 반영 체크리스트

1. DDL `schema-common-consent.sql` DBA 반영, `init-schema`/`seed-if-empty` false, 약관은 어드민에서 등록.
2. `admin-api-open: false` - ADMIN API 는 ROLE_ADMIN 세션 필요.
3. 동의내역 테이블은 수정·삭제 권한을 막고 법정 보존기간 동안 보관 (파기 배치는 별도).
4. 프록시 뒤라면 X-Forwarded-For 를 L4/WAF 가 덮어쓰는지 확인 (CLIENT_IP).
5. 기존 yaml 방식 화면을 옮길 때: 화면별 약관 → 업무코드 매핑표 작성 → 어드민 등록 → JSP 의 `TermsAgree.open` 을 `ConsentAgree.open` 으로 교체.
