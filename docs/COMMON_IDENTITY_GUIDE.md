# [CF-BC-03] 본인인증 공통모듈 가이드 (demojsp)

REQ-AP-601 "휴대폰 본인인증 NICE ID 도입"을 **기존 NICE ID 구현(`domain/auth`)과 별개의 공통모듈**로 구성했다.
적용 채널: APP / PC WEB / 모집인WEB (ADMIN, Batch 제외)

## 1. 기존 NICE 구현과의 분리

| 구분 | 기존 NICE 구현 (변경 없음) | 공통모듈 (신규) |
|---|---|---|
| 패키지 | `com.example.demojsp.domain.auth` | `com.example.demojsp.common.identity` |
| 방식 | OTP 발송/검증 API 직접 호출 | NICE 표준창(팝업/WebView) + 콜백 |
| 설정 | `nice.api.*` (`mock-mode`) | `common.identity.*` (`mock-enabled`) |
| 화면 | `/common/auth/nice-cert` | `/common/identity/sample` (샘플) |
| API | `/api/common/auth/nice/**` | `/api/common/identity/**` |
| 콜백 | - | `/common/identity/callback/{method}` |
| 암복호화 | `NiceCryptoUtil` (해시 바이트 기반) | `NiceIdCrypto` (Base64 해시 문자열 기반, 표준창 가이드) |
| 오류코드 | `ErrorCode` A001~A007 | `IdentityErrorCode` IDV-xxx |
| 세션 키 | 기존 키 | `COMMON_IDENTITY_VERIFIED.{목적}` |

- 공통모듈은 기존 `domain.auth` 클래스를 **참조하지 않는다**. 재사용하는 기존 코드는 `ApiResponse`, `MaskingUtil` 뿐이다.
- 빈 이름은 `identityNiceRestClient`, `identityClock` 등 `identity` 접두어로 충돌을 피했다.
- 변경된 기존 파일: `application.yaml` (맨 아래 `common.identity` 블록 추가) **1개뿐**.
- `WebMvcConfig` 무수정: 신규 경로는 LoginCheckInterceptor 대상 목록에 없어 비로그인 접근 가능,
  RoleCheckInterceptor 는 `@RequireRole` 이 없으면 통과한다.

## 2. 패키지 구조

```
common/identity
├─ config/       IdentityProperties(common.identity.* 바인딩), IdentityConfig(RestClient, Clock)
├─ domain/       AuthMethod, AuthChannel, AuthPurpose, StartRequest/Response,
│                IdentityTx(진행거래), VerifiedIdentity(결과), TermsDocument
├─ spi/          IdentityProvider  ← 인증수단 확장 포인트
├─ provider/
│   ├─ nice/     NiceIdProvider(흐름), NiceIdApiClient(HTTP), NiceIdCrypto(암복호화)
│   ├─ mock/     MockIdentityProvider (mock-enabled=true 일 때)
│   └─ privatecert/ PrivateCertProvider (사설인증 확장 골격, 기본 비활성)
├─ store/        IdentityTxStore + InMemory 구현 (이중화 시 Redis 로 교체)
├─ service/      IdentityVerificationService(Facade), IdentitySessionManager,
│                TermsService, IdentityProviderRegistry
├─ exception/    IdentityErrorCode(IDV-xxx), IdentityVerificationException
├─ web/          IdentityApiController(JSON), IdentityCallbackController(JSP), IdentityApiExceptionHandler
└─ sample/       IdentitySampleController (업무 적용 예시, 적용 후 삭제 가능)

static/js/common/identity-auth.js                 화면 공통 JS (jQuery 비의존)
WEB-INF/views/common/identity/callback.jsp        결과 전달 (팝업 postMessage / APP redirect)
WEB-INF/views/common/identity/sample.jsp          샘플 화면
```

## 3. 처리 흐름

```
[업무화면] IdentityAuth.open()
   │ (팝업 채널: 빈 팝업 먼저 오픈 → 팝업차단 회피)
   ▼
POST /api/common/identity/start
   ├ 필수값 / 필수약관 동의 재검증 / returnUrl 화이트리스트(APP)
   ├ 거래번호 생성 (IDV + yyyyMMddHHmmss + 랜덤10 = 27자)
   ├ NiceIdProvider.prepare: 암호화토큰 발급 → 대칭키 생성 → enc_data, integrity_value
   └ 거래(대칭키 포함) TxStore 저장 → ApiResponse.data = form 정보
   ▼
NICE 표준창 (성명+생년월일+휴대폰, 통신사 미지정 / MO인증)
   ▼
GET /common/identity/callback/nice_mobile
   ├ token_version_id 로 거래 1회성 조회(consume) → 재전송 차단
   ├ 만료 / 세션 일치 검증
   ├ HMAC 무결성 → 복호화 → resultcode=0000 → requestno 일치
   ├ VerifiedIdentity 세션 저장(목적별) + 세션ID 재발급
   └ callback.jsp: 팝업 → opener.postMessage + close / APP → returnUrl 이동
   ▼
[업무 API] identitySessionManager.require(session, AuthPurpose.SIGN_UP).ci()
```

## 4. 업무 개발자 사용법 (2단계)

**화면 (JSP)**
```html
<script src="/js/common/identity-auth.js"></script>
<script>
IdentityAuth.open({ method:'NICE_MOBILE', channel:'PC_WEB', purpose:'SIGN_UP',
                    agreedTerms:[...], returnUrl:'/member/join/step2' })
  .then(function (r) { if (r.success) { /* 다음 단계 */ } else alert(r.message); })
  .catch(function (err) { alert(err.message); });   // 약관 미동의(IDV-002) 등
</script>
```

**서버 (업무 Controller/Service)**
```java
VerifiedIdentity idv = identitySessionManager.require(session, AuthPurpose.SIGN_UP); // 미인증 → IDV-301(401)
memberService.join(form, idv.ci(), idv.di(), idv.mobileNo());
identitySessionManager.clear(session, AuthPurpose.SIGN_UP);   // 1회 사용 후 제거
```

## 5. 요구사항 매핑

| 요구사항 (CF-BC-03 / REQ-AP-601) | 반영 위치 |
|---|---|
| NICE ID 휴대폰 본인인증(성명+생년월일+휴대폰) | `NiceIdProvider`, `NiceIdApiClient`, `NiceIdCrypto` |
| 통신사 미지정 / MO인증 결합 시 번호입력 생략 | `common.identity.nice.extra-request-params` (NICE 상품 옵션값) |
| 사설인증(통신사·민간) | `PrivateCertProvider` 골격 (SPI 구현만 추가) |
| 인증 결과 CI 획득 및 Session 반영 | `IdentityVerificationService.complete`, `IdentitySessionManager` |
| 인증별 약관 상세보기 / 비여신금융거래 약관 추가 | `TermsService`, `GET /api/common/identity/terms`, yaml `terms` |
| AS-IS 통신사 선택 필수 → TO-BE 불필요 | 통신사 선택 UI·파라미터 없음, 통신사 무관 약관 구성 |

## 6. 실행 / 테스트

```bash
./mvnw spring-boot:run          # mock-enabled: true → NICE 없이 즉시 인증
# http://localhost:8080/common/identity/sample

# 공통모듈 테스트만 (DB 불필요 - standalone MockMvc)
./mvnw test -Dtest='IdentityFlowTest,NiceIdProviderTest,NiceIdCryptoTest'
```
실연동: `common.identity.mock-enabled: false` + 환경변수 `NICE_ID_CLIENT_ID`, `NICE_ID_CLIENT_SECRET`,
`NICE_ID_PRODUCT_ID`, (`NICE_ID_ACCESS_TOKEN`), `IDENTITY_BASE_URL`.

## 7. 적용 전 체크리스트

1. **NICE 가이드 대조** — API URL, `ProductID` 헤더, 요청 필드명, 결과코드표, 응답 문자셋(EUC-KR)은 표준창 개발가이드의 일반 사양 기준이다. 계약 시 수령한 최신 가이드와 반드시 대조.
2. **통신사 미지정 / MO인증 옵션** — 상품 계약 옵션이므로 NICE 안내 파라미터를 `extra-request-params` 에 기입 (소스 수정 불필요).
3. **이중화** — `InMemoryIdentityTxStore` 는 단일 서버용. Redis 구현체(`GETDEL` + TTL)를 `@Primary` 로 등록.
4. **SameSite 쿠키** — 기본 `method-type: get` 이면 Lax 기본값으로 동작. `post` 로 바꾸면 세션 쿠키 `SameSite=None; Secure` 필요.
5. **Spring Security 도입 시** — `/common/identity/callback/**` permitAll + CSRF 예외, `/api/common/identity/start` 는 CSRF 헤더 추가.
6. **Mock 운영 차단** — 운영 프로필에서 `mock-enabled: false` 확인.
7. **개인정보** — CI 는 DB 저장 시 암호화 컬럼, 로그는 `MaskingUtil` 마스킹만. 감사로그는 `AUDIT.IDENTITY` 로거 분리.
8. **APP 연동** — 네이티브 브릿지로 결과를 받을 경우 `callback.jsp` 의 브릿지 주석을 앱팀과 협의해 활성화.
