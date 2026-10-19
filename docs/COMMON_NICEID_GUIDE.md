# [CF-BC-03 / 공통#21] 휴대폰 본인인증 NICE ID - DB 방식 가이드

요구사항정의서(공통) 21번 **휴대폰 본인인증 NICE ID 도입** (REQ-AP-601)
목적: NICE ID 서비스를 활용해 휴대폰 본인인증을 쉽게 만든다.

> 기존 구현은 **그대로 두었다** - `domain/auth`(`/common/auth/nice-cert`, TB_CERT_LOG 등), `common/identity`(`/common/identity/sample`).
> 새 모듈 `common.niceid` 는 패키지·URL·테이블이 달라 서로 영향이 없다. 새 업무는 이 DB 방식을 쓴다.

## 1. 요구사항 → 구현

| 요구사항 | 구현 |
|---|---|
| 통신사 미지정 방식: 성명 + 생년월일 + 휴대폰번호만 입력 | SMS 인증 화면에 **통신사 선택 항목 없음**, 서버도 통신사 코드 없이 NICE 요청 (`NiceIdGateway.startSms`) |
| MO 인증 결합 시 휴대폰번호 입력 생략, 기기 소지 확인 | MO 인증: 성명·생년월일만 입력 → 고객 휴대폰에서 NICE 번호로 문자 발송 → 그 번호·기기로 확인 (`startMo` → `moStatus`) |
| 약관 추가 (비여신금융거래 약관) | 공통#22 약관동의(DB) 연동 - 업무 정책의 `CONSENT_BIZ_CD`(샘플 `IDENTITY_NICE` = 본인확인서비스·고유식별정보·**비여신금융거래 약관**). 서버가 약관동의 번호를 다시 확인 |
| AS-IS 통신사 선택 필수 → TO-BE 불필요 | 위와 같음 |
| 기존 디자인 컴포넌트 활용 | 약관동의 레이어와 같은 모양·색상(ui-tokens) |
| 주민번호 → CI 변경 시 유의고객·미성년자·외국인 체크 영향 검토 | 주민번호 없이 **CI**(유의고객 `TB_WATCHLIST`), **생년월일**(만 나이), **NICE 내외국인 값**(외국인)으로 검사. 업무별 기준은 DB 정책 |
| 9/30 이기종 CI 이관 | 기존 `domain/auth` CI 이관 기능 유지 (이번 범위 아님) |

## 2. 화면 흐름

```
업무 화면 NiceIdAuth.open({ purposeCd })
  ① 업무 정책 조회 (GET /purposes/{cd})  ─ 약관 업무코드 · SMS/MO 사용 여부
  ② 약관동의 (공통#22 ConsentAgree, 필수 → 선택 2페이지) → agreeNo
  ③ [휴대폰번호로 인증 | 번호 입력 없이 인증]
       SMS : 성명·생년월일·휴대폰 → 인증번호 6자리 (3:00) → 확인 (오입력 n/5)
       MO  : 성명·생년월일 → "1600-1522 로 PEPPER 1234 보내기" [문자앱 열기] → 자동 확인(3초마다)
  ④ 서버 검사: 성명·생년월일 일치 → 유의고객(CI) → 미성년(만 나이) → 외국인
       VERIFIED → onComplete(reqNo) / BLOCKED·FAILED·EXPIRED → 안내 화면 → onFail
업무 서버 niceIdService.checkVerified(reqNo, purposeCd, request) → CI 로 회원 식별
```

## 3. 테이블 (`src/main/resources/schema-common-niceid.sql`)

| 테이블 | 용도 |
|---|---|
| `TB_CO_IDV_PURPOSE` | **업무별 정책**: 약관 업무코드 · SMS_YN · MO_YN · MIN_AGE(미성년 기준) · FOREIGNER_YN · WATCH_CHECK_YN · 유효시간(초) · 입력 횟수 · 결과 사용시간(분) |
| `TB_CO_IDV_REQ` | **요청·결과 1건 = 1행**: 요청번호(32자 난수) · 방식 · 약관동의번호 · 상태(REQUESTED/VERIFIED/BLOCKED/FAILED/EXPIRED) · 결과코드 · CI/DI · 나이 · 내외국인 · 오입력 횟수 · IP |
| `TB_WATCHLIST` | **기존** 유의고객(CI 기반) - 있으면 그대로 쓰고 없을 때만 같은 구조로 생성 |

샘플 정책 (`data-common-niceid.sql`)

| 업무 | 약관 | SMS | MO | 최소 나이 | 외국인 | 유의고객 |
|---|---|---|---|---|---|---|
| SIGN_UP 회원가입 | IDENTITY_NICE | Y | Y | 14 | 허용 | 검사 |
| NON_CREDIT 비여신 거래 | IDENTITY_NICE | Y | Y | 14 | 허용 | - |
| LOAN_APPLY 대출신청 | IDENTITY_NICE | Y | Y | **19** | **차단** | 검사 |
| PW_RESET 비밀번호 재설정 | 없음 | Y | N | - | 허용 | - (입력 3회) |

## 4. NICE 연동부 (`gateway`)

```
NiceIdGateway (인터페이스)  startSms / verifySms / startMo / checkMo
 ├ MockNiceIdGateway   common.niceid.gateway=mock (기본) - 시뮬레이터, 실제 문자 없음
 └ (실연동 구현체)     NICE 계약 후 작성 → gateway=nice 로 교체. 기존 domain/auth/client/NiceApiClientImpl 의 토큰·암호화 코드 재사용
```
시뮬레이터: 인증번호를 응답 `testCode` 로 알려 주고(local), CI = Base64(SHA-512("성명|생년월일|휴대폰")) 88자,
성명에 영문이 있으면 외국인, MO 는 `POST /mo/demo-send` 로 "문자 보냄" 을 흉내 낸다.

| 시뮬레이터 테스트 인물 | 결과 |
|---|---|
| 홍길동 / 19900101 / 01012345678 | 성공 |
| 김미성 / 20100505 | 대출신청: BLOCKED MINOR (회원가입은 성공) |
| John Smith / 19850101 | 대출신청: BLOCKED FOREIGNER |
| 김유의 / 19800101 / 01099990000 | BLOCKED WATCHLIST (샘플 유의고객 - NiceIdSeeder 가 TB_WATCHLIST 에 없을 때만 추가) |

## 5. 보안·운영 규칙 (코드에 반영됨)

- **요청번호는 32자 난수** - 순번이 아니라 남의 요청을 추측할 수 없고, 같은 회원(로그인) 또는 같은 세션만 조회·완료 가능.
- **약관동의는 서버가 다시 확인** (`ConsentService.checkAgreed`) - 화면이 보낸 동의 여부를 믿지 않는다.
- **오입력 횟수**는 예외를 던지기 전에 DB 에 반영(트랜잭션 롤백 방지) → 최대 횟수에 닿으면 FAILED, 처음부터.
- **같은 요청을 두 번 완료할 수 없다** (`UPDATE ... WHERE STATUS='REQUESTED'`).
- **과다 요청 제한**: 같은 세션 10분에 `max-requests-per10-min` 건 (문자 비용·폭탄 방지).
- **CI/DI/전체 휴대폰번호는 화면 JSON 에 나가지 않는다** (`@JsonIgnore`) - 업무 서버만 `checkVerified` 로 받는다.
- 유의고객 조회가 실패하면 **통과시키지 않는다**(fail-closed, FAILED CHECK_ERROR). 차단 사유는 고객 화면에 직접 노출하지 않는다.
- 운영: `expose-test-code: false`, `demo-api-open: false`, `admin-api-open: false`, CI·휴대폰 컬럼 DB 암호화.

## 6. 소스 구성 (`com.example.demojsp.common.niceid`)

```
controller/  NiceIdApiController (/api/common/niceid/**), NiceIdAdminApiController (/api/common/niceid-admin/**),
             NiceIdAdminAuth, NiceIdSampleController (/common/niceid/sample, /admin, /sample/next)
service/     NiceIdService(Impl)   시작·확인·MO·정책검사·checkVerified
             NiceIdAdminService(Impl)  정책 저장(약관 업무코드 존재 확인)·이력(마스킹)
gateway/     NiceIdGateway, MockNiceIdGateway, NiceIdIdentity, NiceIdGatewayException
mapper/      NiceIdMapper + resources/mapper/common/NiceIdMapper.xml
dto/         IdvPurposeDto, IdvReqDto, IdvStartRequest, IdvStartResult, IdvVerifyRequest, IdvResult
exception/   NiceIdErrorCode(NID-xxx), NiceIdException, NiceIdExceptionHandler
config/      NiceIdProperties(common.niceid.*), NiceIdSeeder

static/js/common/nice-id.js, static/css/common/nice-id.css
WEB-INF/views/common/niceid/sample.jsp, admin.jsp
```

## 7. 업무 적용

```html
<link rel="stylesheet" href="/css/common/ui-tokens.css">
<link rel="stylesheet" href="/css/common/consent-agree.css">
<link rel="stylesheet" href="/css/common/nice-id.css">
<script src="/js/common/consent-agree.js"></script>
<script src="/js/common/nice-id.js"></script>
<script>
NiceIdAuth.open({
    purposeCd: 'SIGN_UP', channel: 'APP',
    onComplete: function (r) { location.href = '/member/join/info?idvReqNo=' + r.reqNo; },
    onFail: function (r) { /* 차단·실패 안내를 본 뒤 */ },
    onCancel: function () { }
});
</script>
```
```java
IdvResult r = niceIdService.checkVerified(idvReqNo, "SIGN_UP", request);   // 실패 시 NiceIdException (NID-3xx)
member.setCi(r.getCi());
member.setCellNo(r.getCellNo());
```

## 8. API · 오류코드

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/common/niceid/purposes/{cd}` | 업무 정책 (화면용 값만) |
| POST | `/api/common/niceid/sms/request` | `{purposeCd, agreeNo, channel, name, birth, phone}` → reqNo, phoneMask, (local) testCode |
| POST | `/api/common/niceid/sms/verify` | `{reqNo, code}` → 결과 |
| POST | `/api/common/niceid/mo/request` | `{purposeCd, agreeNo, channel, name, birth}` → moNumber, moCode, smsUri |
| GET | `/api/common/niceid/mo/status?reqNo=` | 대기(REQUESTED) 또는 결과 |
| POST | `/api/common/niceid/mo/demo-send` | [local] `{reqNo, phone}` 문자 보낸 것으로 처리 |
| GET / POST | `/api/common/niceid-admin/purposes` | 정책 목록 / 등록·수정 |
| GET | `/api/common/niceid-admin/requests?purpose=&status=` | 인증 이력 (CI 앞 10자 · 휴대폰 마스킹) |

`NID-001` 요청 오류, `002` 업무 없음, `003` 요청 없음(남의 요청 포함), `004` 이미 처리됨, `005` 과다 요청, `006` 시간 초과,
`007` 인증번호 불일치(n/최대), `008` 횟수 초과, `009` 사용할 수 없는 방식, `101` 약관 미동의, `102` 입력값 오류,
`201` 본인확인기관 오류, `301` 인증 미완료, `302` 결과 사용시간 초과, `901` local 전용 기능, `401` ADMIN 권한 없음.

## 9. 실행 / 테스트

```
사용자 화면  http://localhost:8080/common/niceid/sample
어드민       http://localhost:8080/common/niceid/admin
./mvnw test -Dtest=NiceIdDbTest      # 8건 - 실제 DDL·샘플·매퍼 + 약관동의 연동을 H2(MySQL 모드)에서 실행
```

## 10. 확인 필요

1. NICE ID 계약 상품의 실제 파라미터(통신사 미지정·MO 옵션명), MO 수신번호 → 실연동 구현체 작성.
2. 업무별 최소 나이·외국인 허용 기준 (샘플 값은 가정).
3. 유의고객 등급별 처리(CRITICAL 차단 / CAUTION 경고 등) - 현재는 `BLOCK_YN='Y'` 이면 차단.
