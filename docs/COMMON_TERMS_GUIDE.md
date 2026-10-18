# [CF-BC-04] 약관동의 공통모듈 가이드 (demojsp)

REQ-AP-602 "약관동의 필수/선택 항목" — 각 페이지에 개별 코딩된 약관동의를 **공통 컴포넌트 + 공통 API** 로 통일한다.
적용 채널: APP / PC WEB / 모집인WEB (ADMIN 은 약관관리 API 만, Batch 제외)

> 엑셀 "구현 방안(Java/Spring 설계 가이드)" 의 `TermsService.getTerms(bizCode) / agree(bizCode, agreements)` 는 참고로만 사용했다.
> 본인인증 모듈에 이미 `termsService` 빈이 있어 이름 충돌을 피하려고 서비스명은 `TermsAgreeService` 로 했다.

## 1. 요구사항 매핑

| 요구사항 (REQ-AP-602 / CF-BC-04) | 반영 위치 |
|---|---|
| 필수/선택 항목 페이지 분리 (AS-IS 한 화면 → TO-BE 필수 전체동의 → 선택 전체동의, 단일 페이지 2개) | `terms-agree.js` 1단계/2단계, `TermsSet.required / optional` |
| 각 항목을 눌러 상세내용 확인 | `terms-agree.js` 상세보기 화면, `GET /api/common/terms/{bizCode}/items/{termsCode}` |
| 필수 항목에 동의해야 다음으로 이동 | 화면: [다음] 비활성 / 서버: `TermsAgreeService.agree` 필수 재검증(TRM-002) |
| 업무코드 단위 약관 목록 조회 (하드코딩 금지) | `GET /api/common/terms/{bizCode}`, `TermsRepository` |
| ADMIN 등록 약관 호출 | `TermsAdminApiController` (등록/개정, 버전 이력) |
| 동의 결과 증적 저장 (약관 버전·해시) | `AgreementRecord` (version + SHA-256), `AUDIT.TERMS` 로거 |
| 약관 버전 저장 및 ADMIN 조회 | `GET /api/common/terms-admin/{bizCode}/versions`, `/agreements` |
| 신분증·안면인증 화면 약관 컴포넌트 통일 | 업무코드 `IDENTITY_ID_CARD`, 본인인증 샘플은 `IDENTITY_NICE_MOBILE` 로 전환 완료 |
| PDF/약관 Viewer (CF-NA-03) | 상세보기에 `pdfUrl` 링크만 제공 (Viewer 는 CF-NA-03 담당) |

## 2. 패키지 구조

```
common/terms
├─ config/     TermsProperties(common.terms.* 바인딩, seed), TermsConfig
├─ domain/     TermsItem(약관 원장 1버전), TermsSummary, TermsSet(필수/선택 분리),
│              AgreeRequest, AgreeResult(세션 저장), AgreementRecord(증적), TermsRegisterRequest, BizCodeInfo
├─ store/      TermsRepository / TermsAgreementRepository + InMemory 구현 (운영: MyBatis 구현체로 교체)
├─ service/    TermsAgreeService(Facade), TermsSessionManager, TermsSupport(해시·거래번호)
├─ exception/  TermsErrorCode(TRM-xxx), TermsException
├─ web/        TermsApiController(사용자), TermsAdminApiController(ADMIN), TermsRequestContext, TermsApiExceptionHandler
└─ sample/     TermsSampleController (업무 적용 예시, 적용 후 삭제 가능)

common/testhub   TestHubController + TestHubProperties  → /common/test-hub (공통모듈 테스트 목록)

static/js/common/terms-agree.js            화면 공통 컴포넌트 (jQuery/Bootstrap 비의존)
WEB-INF/views/common/terms/sample.jsp      샘플 + ADMIN 테스트 화면
WEB-INF/views/common/testhub/list.jsp      공통모듈 테스트 목록
```

기존 코드 변경: `application.yaml`(맨 아래 `common.terms`, `common.test-hub` 블록 추가), `navbar.jsp`/`index.jsp`(테스트 메뉴 링크),
`common/identity/sample.jsp`(약관 영역을 공통 컴포넌트로 교체). `WebMvcConfig` 무수정 — 신규 경로는 로그인 인터셉터 대상이 아니다.

## 3. API

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/common/terms/{bizCode}` | 약관 목록 (필수/선택 분리, 전문 제외) |
| GET | `/api/common/terms/{bizCode}/items/{termsCode}` | 약관 상세 (전문, 버전, 해시, pdfUrl) |
| POST | `/api/common/terms/{bizCode}/agree` | 동의 → 증적 저장 + 세션 반영 |
| GET | `/api/common/terms/{bizCode}/status` | 현재 세션 동의 결과 (없으면 `data: null`) |
| GET | `/api/common/terms-admin/biz-codes` | [ADMIN] 업무코드 목록 |
| GET | `/api/common/terms-admin/{bizCode}/versions` | [ADMIN] 버전 이력 |
| POST | `/api/common/terms-admin/terms` | [ADMIN] 약관 등록/개정 (새 버전 → 즉시 현재 약관) |
| GET | `/api/common/terms-admin/agreements?bizCode=&subjectId=&limit=` | [ADMIN] 동의 이력(증적) |

동의 요청 예:
```json
{ "channel": "APP",
  "items": [ {"termsCode":"SVC_USE","version":"1.0","agreed":true},
             {"termsCode":"PRIVACY_COLLECT","version":"1.0","agreed":true},
             {"termsCode":"MARKETING_USE","version":"1.0","agreed":false} ] }
```

## 4. 업무 개발자 사용법

**화면 (JSP)** — 기존에 화면마다 작성하던 체크박스/전체동의/상세보기 코드를 지우고 아래로 대체
```html
<script src="/js/common/terms-agree.js"></script>
<script>
TermsAgree.open({ bizCode: 'SIGN_UP', channel: 'APP' })          // 기본: 전체화면 레이어
  .then(function (r) {
      if (!r.success) return;                // 사용자가 닫음 (USER_CANCELED)
      location.href = '/member/join/step2';  // r.data.agreementId, agreedCodes, declinedCodes
  })
  .catch(function (err) { alert(err.message); });
// 화면 안에 삽입: TermsAgree.open({ bizCode:'LOAN_APPLY', mode:'inline', container:'#termsArea' })
</script>
```

**서버 (업무 Controller/Service)**
```java
AgreeResult terms = termsSessionManager.require(session, "SIGN_UP");   // 미동의/만료 → TRM-301(401)
member.setTermsAgreementId(terms.agreementId());                       // 업무 데이터 ↔ 동의 이력 연결
member.setMarketingYn(terms.isAgreed("MARKETING_USE") ? "Y" : "N");    // 선택약관 반영
termsSessionManager.clear(session, "SIGN_UP");                         // 1회 사용 후 제거
```

**본인인증과 함께 쓸 때** — 본인인증 팝업은 클릭 이벤트 안에서 동기적으로 열려야 하므로(팝업 차단),
`약관동의 완료 → [휴대폰 본인인증] 버튼 클릭` 2단계로 구성한다. (`/common/identity/sample` 참고)

## 5. 설계 포인트

- **하드코딩 금지**: 화면은 `bizCode` 만 안다. 약관 코드·문구·필수여부·순서는 서버(ADMIN 등록 약관)에서 내려온다.
- **개정 대응**: 화면이 본 버전을 동의 요청에 포함 → 서버 현재 버전과 다르면 `TRM-103` → 컴포넌트가 새 약관으로 재조회 후 재동의.
- **증적**: 동의/미동의 모두 기록. 전문 대신 `version + SHA-256(전문)` 저장 → 원장에서 그 버전 원문의 해시를 재계산해 동일성 입증.
  해시는 CRLF/LF·앞뒤 공백을 정규화한 뒤 계산한다.
- **세션 분리**: `COMMON_TERMS_AGREED.{bizCode}` — 다른 업무의 동의 결과 재사용 차단, `agreement-ttl` 초과 시 무효.
- **XSS**: 컴포넌트는 서버 문자열을 `textContent` 로만 출력. `pdfUrl` 은 `/` 상대경로·`https://` 만 허용(화면·서버 모두 검증).
- **오류코드**: `TRM-001` 미등록 업무, `002` 필수 미동의, `003` 약관 없음, `004` 요청 오류, `101` 미등록 약관, `102` 중복,
  `103` 개정됨, `201` 중복 버전, `301` 미동의(업무 API), `401` ADMIN 권한 없음.

## 6. 실행 / 테스트

```bash
./mvnw spring-boot:run
# 공통모듈 테스트 목록: http://localhost:8080/common/test-hub
# 약관동의 샘플:        http://localhost:8080/common/terms/sample

./mvnw test -Dtest='TermsFlowTest'        # DB 불필요 (standalone MockMvc)
```

## 7. 적용 전 체크리스트

1. **저장소 교체** — `InMemoryTermsRepository` / `InMemoryTermsAgreementRepository` 는 local 용. 운영은 MyBatis 구현체를 `@Primary` 로 등록.
   기존 `TB_TERMS` 는 버전 이력이 없으므로(PK=TERMS_ID) `BIZ_CODE, TERMS_CODE, VERSION` 복합키 테이블로 확장 필요.
   동의 이력은 `TB_TERMS_AGREE_LOG` 에 `BIZ_CODE, TERMS_VERSION, CONTENT_HASH, SUBJECT_ID, CHANNEL, CLIENT_IP` 컬럼 추가(INSERT ONLY).
2. **ADMIN API 잠금** — 운영 yaml `common.terms.admin-api-open: false` (ROLE_ADMIN 세션 필요).
3. **기존 화면 전환** — 화면별 약관 코드 → 업무코드 매핑표 작성 후 seed(→DB) 등록, JSP 의 개별 약관 HTML/JS 제거.
   기존 NICE 화면(`/common/auth/nice-cert`, `TB_TERMS` 사용)은 이번에 변경하지 않았다.
4. **본인인증 서버 검증 통합(다음 단계 권장)** — 현재 본인인증 시작 API 는 화면이 보낸 `agreedTerms` 를 yaml 약관과 대조한다.
   `TermsSessionManager.require(session, "IDENTITY_NICE_MOBILE")` 로 바꾸면 증적과 연결되고 클라이언트 값을 믿지 않아도 된다.
5. **X-Forwarded-For** — 프록시가 헤더를 덮어쓰는지 확인 (위조 가능).
6. **보존기간** — 동의 이력은 법정 보존기간 동안 보관, 삭제/수정 금지.
