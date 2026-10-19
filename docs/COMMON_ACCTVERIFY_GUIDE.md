# [CF-BC-06 / 공통#25] 1원 계좌인증 - DB 방식 가이드

요구사항정의서(공통) 25번 **1원 계좌인증** (REQ-AP-605)
목적: 1원 인증 서비스는 하나로 쓰되, 업무 Context 에 따라 화면을 분리한다.

> 기존 구현은 **그대로 두었다** - `domain/accountcert` (AccountCertController 등, TB_ACCOUNT_CERT_HIST).
> 새 모듈 `common.acctverify` 는 패키지·URL·테이블이 달라 서로 영향이 없다. 새 업무는 이 DB 방식을 쓴다.
> 기존 기준 테이블 `TB_BANK_CODE`(은행), `TB_LOAN_ACCOUNT`(대출 입출금계좌)는 **같이 읽기만** 한다.

## 1. 요구사항 → 구현

| 요구사항 | 구현 |
|---|---|
| 하나의 1원 인증 서비스 | `AcctVerifyService` 하나 + 화면 컴포넌트 `AcctVerify.open({ bizCd })` 하나 |
| 업무 Context 에 따라 화면 분리 | 업무코드별 `SCREEN_TYPE` (DB `TB_CO_ACCT_BIZ`) - **LOAN** 대출 입출금계좌용 / **COMMON** 타 업무 공통 |
| 대출 입출금계좌 화면: 대출금 입금/출금계좌 자동완성 | LOAN + `PREFILL_YN='Y'` 이면 로그인 회원의 대출 원장(`TB_LOAN_ACCOUNT`) 계좌를 **마스킹**해 카드로 보여 준다 |
| 자동완성 계좌 수정 가능 | [다른 계좌로 수정] → 은행 선택 + 계좌번호 직접 입력 (`MODIFIED_YN='Y'` 로 이력에 남김), [대출 입출금계좌 불러오기] 로 되돌리기 |
| 타 업무 공통 화면 | COMMON: 은행 선택 + 계좌번호 입력 (인증수단 등록 · 환불계좌 등) |
| 본인 명의 확인 | 펌뱅킹 예금주 조회 → 고객 실명과 다르면 거절 (`OWNER_CHECK_YN`) |

## 2. 화면 흐름

```
업무 화면 AcctVerify.open({ bizCd })
  ① 업무 Context 조회 (GET /context/{bizCd}) ─ 화면 종류 · 은행 목록 · 자동완성 후보(LOAN)
  ② LOAN  : [카카오뱅크 333*******543 | KB국민은행 123********890] 선택  … [다른 계좌로 수정]
     COMMON: 은행 선택 + 계좌번호
     → [1원 보내기]   서버: 계좌 확정 → 예금주 = 고객 실명? → 1원 송금 (입금자명 "페퍼3821")
  ③ 받는 계좌 · 예금주 · 입금자명 "페퍼 ____" 안내 + 남은 시간(업무별 분) → 숫자 4자리 입력 (오입력 n/최대)
  ④ VERIFIED → onComplete(reqNo) / 횟수·시간 초과 → 안내 화면 → onFail
업무 서버 acctVerifyService.checkVerified(reqNo, bizCd, request) → 전체 계좌번호로 등록
```

## 3. 테이블 (`src/main/resources/schema-common-acctverify.sql`)

| 테이블 | 용도 |
|---|---|
| `TB_CO_ACCT_BIZ` | **업무 Context**: 화면 종류(LOAN/COMMON) · 자동완성 · 예금주 확인 · 입금자명 접두어 · 유효시간(분) · 입력 횟수 · 결과 사용시간(분) |
| `TB_CO_ACCT_VERIFY` | **요청·결과 1건 = 1행**: 요청번호(32자 난수) · 업무 · 고객 · 은행 · 계좌(전체/마스킹) · 예금주 · 출처(PREFILL/INPUT) · 수정여부 · 코드 해시 · 오입력 · 상태(SENT/VERIFIED/FAILED/EXPIRED) · 결과코드 |
| `TB_BANK_CODE` · `TB_LOAN_ACCOUNT` | **기존** 테이블 - 있으면 그대로 쓰고 없을 때만 같은 구조로 생성, 비어 있을 때만 샘플 적재 |

샘플 업무 Context (`data-common-acctverify.sql`)

| 업무 | 화면 | 자동완성 | 유효 | 횟수 | 결과 사용 |
|---|---|---|---|---|---|
| LOAN_DISBURSE 대출금 입금계좌 등록 | LOAN | Y | 10분 | 5 | 30분 |
| LOAN_REPAY 원리금 출금계좌 등록 | LOAN | Y | 10분 | 5 | 30분 |
| AUTH_MEANS 인증수단 등록(본인확인) | COMMON | - | 10분 | **3** | **10분** |
| REFUND_ACCT 환불계좌 등록 | COMMON | - | 10분 | 5 | 30분 |

## 4. 고객 실명 (예금주 비교 기준) - `AcctCustomerResolver`

1. **로그인 회원** → 회원 ID · 회원명 (대출계좌 자동완성 가능)
2. **비로그인** → 공통#21 본인인증 결과: `params: { idvReqNo, idvPurposeCd }` → `NiceIdService.checkVerified` 의 성명
3. **local 전용** (`demo-api-open`) → `?demoCustomer=&demoName=` (샘플 화면 고객 전환)

셋 다 없으면 `AVF-101`. 화면이 보낸 이름은 쓰지 않는다 (남의 계좌로 인증하는 것을 막기 위해).

## 5. 펌뱅킹 연동부 (`gateway`)

```
FirmBankingGateway (인터페이스)  checkOwner(예금주 조회) / transferOneWon(1원 송금, 입금자명)
 ├ MockFirmBankingGateway   common.acct-verify.gateway=mock (기본) - 시뮬레이터
 └ (실연동 구현체)          펌뱅킹/오픈뱅킹 계약 후 작성 → gateway 값 교체
```

| 시뮬레이터 계좌번호 끝자리 | 결과 |
|---|---|
| 그 외 | 예금주 = 고객 실명, 송금 성공 |
| …0000 | 없는 계좌 (`AVF-104`) |
| …9999 | 예금주 김타인 → 본인 명의 아님 (`AVF-105`, 예금주명 마스킹 안내) |
| …8888 | 송금 실패 (`AVF-201`) |

샘플 회원 customer01 홍길동: 대출 2건 (KB국민은행 123402-04-567890, 카카오뱅크 3333-01-9876543).

## 6. 보안·운영 규칙 (코드에 반영됨)

- **자동완성 계좌는 서버가 원장에서 다시 읽는다** - 화면에는 마스킹 번호와 대출번호만 내려가고, 요청도 대출번호만 보낸다. 남의 대출번호는 `AVF-103`.
- **인증 숫자 원문은 저장하지 않는다** (`SHA-256(reqNo:code)`), 비교는 상수 시간.
- **오입력 횟수·실패 이력**은 예외를 던지기 전에 DB 에 반영 (트랜잭션 롤백 방지) → 최대 횟수에 닿으면 FAILED.
- **같은 요청을 두 번 완료할 수 없다** (`UPDATE ... WHERE STATUS='SENT'`), 다른 업무코드로 결과 재사용 불가.
- **과다 요청 제한**: 같은 세션 10분에 `max-requests-per10-min` 건 (송금 비용·계좌 조회 남용 방지).
- **전체 계좌번호는 화면 JSON 에 나가지 않는다** (`@JsonIgnore`) - 업무 서버만 `checkVerified` 로 받는다. 어드민 이력도 마스킹.
- 운영: `expose-test-code: false`, `demo-api-open: false`, `admin-api-open: false`, 계좌번호 컬럼 DB 암호화.

## 7. 소스 구성 (`com.example.demojsp.common.acctverify`)

```
controller/  AcctVerifyApiController (/api/common/acct-verify/**), AcctVerifyAdminApiController (/api/common/acct-verify-admin/**),
             AcctVerifyAdminAuth, AcctVerifySampleController (/common/acct-verify/sample, /admin, /sample/next)
service/     AcctVerifyService(Impl)   Context · 1원 송금 · 확인 · checkVerified
             AcctVerifyAdminService(Impl)  업무 Context 저장(검증)·이력(마스킹)
             AcctCustomer, AcctCustomerResolver   고객 실명 결정
gateway/     FirmBankingGateway, MockFirmBankingGateway, FirmBankingException
mapper/      AcctVerifyMapper + resources/mapper/common/AcctVerifyMapper.xml
dto/         AvBizDto, AvBankDto, AvPrefillDto, AvContextView, AvRequest, AvStartResult, AvVerifyRequest, AvReqDto, AvResult
exception/   AcctVerifyErrorCode(AVF-xxx), AcctVerifyException, AcctVerifyExceptionHandler
config/      AcctVerifyProperties(common.acct-verify.*), AcctVerifyConfig, AcctVerifySeeder

static/js/common/acct-verify.js, static/css/common/acct-verify.css
WEB-INF/views/common/acctverify/sample.jsp, admin.jsp
```

## 8. 업무 적용

```html
<link rel="stylesheet" href="/css/common/ui-tokens.css">
<link rel="stylesheet" href="/css/common/acct-verify.css">
<script src="/js/common/acct-verify.js"></script>
<script>
AcctVerify.open({
    bizCd: 'LOAN_DISBURSE', channel: 'APP',
    // 비로그인 업무만: params: { idvReqNo: idvReqNo, idvPurposeCd: 'SIGN_UP' },
    onComplete: function (r) { location.href = '/loan/exec/confirm?avReqNo=' + r.reqNo; },
    onFail: function (r) { },
    onCancel: function () { }
});
</script>
```
```java
AvResult r = acctVerifyService.checkVerified(avReqNo, "LOAN_DISBURSE", request);   // 실패 시 AcctVerifyException (AVF-3xx)
loan.setDisburseBankCd(r.getBankCd());
loan.setDisburseAcctNo(r.getAcctNo());
```
새 업무가 1원 인증을 쓰려면 어드민에서 업무코드를 등록하고(화면 종류 선택) `bizCd` 만 바꾸면 된다. 코드 수정 없음.

## 9. API · 오류코드

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/common/acct-verify/context/{bizCd}` | 화면 종류 · 은행 · 자동완성 후보(마스킹) |
| POST | `/api/common/acct-verify/request` | `{bizCd, channel, prefillId}` 또는 `{bizCd, channel, bankCd, acctNo, modifiedYn}` → reqNo, (local) testCode |
| POST | `/api/common/acct-verify/verify` | `{reqNo, code}` → 결과 |
| GET / POST | `/api/common/acct-verify-admin/bizs` | 업무 Context 목록 / 등록·수정 |
| GET | `/api/common/acct-verify-admin/requests?biz=&status=` | 인증 이력 (계좌 마스킹) |
| GET | `/api/common/acct-verify-admin/banks` | 은행 코드 |

`AVF-001` 요청 오류, `002` 업무 없음, `003` 요청 없음(남의 요청 포함), `004` 이미 처리됨, `005` 과다 요청, `006` 시간 초과,
`007` 숫자 불일치(n/최대), `008` 횟수 초과, `101` 고객 확인 불가, `102` 은행·계좌번호 오류, `103` 자동완성 계좌 없음,
`104` 없는 계좌, `105` 본인 명의 아님, `201` 송금 실패, `301` 인증 미완료, `302` 결과 사용시간 초과, `401` 입력값 오류, `402` ADMIN 권한 없음.

## 10. 실행 / 테스트

```
사용자 화면  http://localhost:8080/common/acct-verify/sample
어드민       http://localhost:8080/common/acct-verify/admin
./mvnw test -Dtest=AcctVerifyDbTest      # 9건 - 실제 DDL·샘플·매퍼를 H2(MySQL 모드)에서 실행
```

## 11. 확인 필요

1. 펌뱅킹(또는 오픈뱅킹) 계약 상품의 예금주 조회·1원 송금 전문, 입금자명 길이 제한 → 실연동 구현체 작성.
2. 대출 입출금계좌 원천: 현재 `TB_LOAN_ACCOUNT` (기존 샘플 원장) → 계정계 대출 원장 연동 시 `selectLoanAccounts` 쿼리만 교체.
3. 업무별 유효시간·입력 횟수 기준 (샘플 값은 가정), 화면 분리 대상 업무 목록 확정.
