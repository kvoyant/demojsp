# [CF-TC-07 · CF-TC-02 · CF-BC-10 / 공통#1] 플랫폼 대출 연계 - 한도조회 가이드

요구사항정의서(공통) 1번 **플랫폼 대출 연계** (REQ-AP-074)
> 현재 서비스 중인 플랫폼 대출연계를 **동일하게 유지**하되, 신규 전략관리시스템(**CSS**) 연동에 따른 **한도조회 기능 정비**

한 줄 요약: **제휴사 쪽 문(전문)은 그대로 두고, 안쪽 한도 엔진만 신규 CSS 로 갈아 끼운다.**

---

## 1. 연계 개발은 어떻게 생각하면 되나

연계 개발은 결국 **"남의 형식으로 들어온 요청을, 우리 형식으로 바꿔서 처리하고, 다시 남의 형식으로 돌려주는 일"** 이다.
그래서 코드를 세 층으로 나눈다.

```
 ┌──────────── 제휴사마다 다름 ────────────┐   ┌──── 제휴사와 무관 (공통) ────┐   ┌── 엔진마다 다름 ──┐
  토스 전문   ─┐                                                                      ┌─ 현행 CSS (LEGACY)
  카카오 전문 ─┼─▶ [어댑터] ─▶ 공통모델 ─▶ [업무 공통 서비스] ─▶ CSS 공통모델 ─▶ [CssRouter] ┤
  핀다 전문   ─┘    ◀─ 응답 전문 ◀─ 결과코드 변환(DB) ◀─────────────────────────────    └─ 신규 CSS (NEW)
```

| 층 | 하는 일 | 이 모듈의 클래스 | 바뀌는 때 |
|---|---|---|---|
| **바깥 (제휴사)** | 전문 형식·단위·코드 변환 | `PartnerAdapter` 구현체 (`TossAdapter` …) | 제휴사 추가·규격 변경 |
| **가운데 (업무 공통)** | 인증·중복·상품결정·시간관리·이력 | `PlatformLinkServiceImpl` | 업무 규칙 변경 (거의 안 바뀜) |
| **안쪽 (엔진)** | 한도 산출 | `CssGateway` 구현체 + `CssRouter` | CSS 교체 ← **이번 요구사항** |

핵심 규칙 3가지 (요구분석서 CF-TC-02 · CF-TC-07)
1. **제휴사별 `if` 분기 금지** - 차이는 어댑터(코드)와 설정 테이블(DB)로만 흡수한다.
2. **전문 형식을 업무 로직까지 끌고 들어가지 않는다** - 어댑터를 지나면 금액은 원, 날짜는 yyyyMMdd, 코드는 당행 코드.
3. **제휴사 제한시간 안에 반드시 응답한다** - 늦은 정답보다 제때 온 "일시 오류" 가 낫다 (늦으면 제휴사 화면에서 페퍼 상품이 빠진다).

---

## 2. 요청 한 건의 흐름

```
POST /api/link/platform/{제휴사}/limit      헤더 X-Link-Timestamp · X-Link-Signature
  제휴사 확인   URL 제휴사코드 → TB_LNK_PARTNER (어댑터 · 엔진 · 제한시간)
  ① 인증       허용 IP · HMAC 서명(본문 포함) · 타임스탬프 ±300초        실패 → 401 + 제휴사 인증실패 코드, 이력 없음
  ② 전문 변환   어댑터.parse() → 공통모델 LimitInquiry + 공통 필수값 검사     실패 → 전문오류 코드
  ③ 중복 확인   (제휴사, 제휴사요청번호) UNIQUE → 이미 처리했으면 저장된 응답 그대로 재전송 (CSS 재호출 없음)
  ④ 서비스 상태 USE_YN=N → 서비스 중단 코드
  ⑤ 상품 결정   제휴사 지정 > 차량번호 → AUTO > 담보주소 → MORTGAGE > CREDIT, 제휴사 허용상품 확인
  ⑥ CSS 호출   CssRouter: 엔진 선택 · 남은 시간(제한 - 여유 - 경과)만 대기 · 차단기
  ⑦ 결과 변환   내부 결과코드 → TB_LNK_CODE_MAP → 어댑터.response()
  ⑧ 이력 저장   TB_LNK_LIMIT_REQ (CI 는 SHA-256, 이름은 마스킹, 응답 전문 보관)
```

내부 결과코드 (모든 제휴사 공통) → 제휴사 코드는 DB 변환표

| 내부 | 의미 | 토스(가정) | 카카오페이(가정) | 핀다(가정) |
|---|---|---|---|---|
| APPROVED | 승인 | SUCCESS | 0000 | OK |
| REJECTED | 심사 거절 | REJECT | 1001 | DECLINED |
| TIMEOUT | 제한시간 내 CSS 응답 없음 | E504 | 9001 | TIMEOUT |
| CSS_ERROR | CSS 장애 · 차단기 열림 | E500 | 9002 | ERROR |
| INVALID | 전문 형식·필수값 오류 | E400 | 8001 | BAD_REQ |
| PRODUCT_NA | 허용하지 않는 상품 | E404 | 8002 | NOT_SUPP |
| STOPPED | 서비스 중단 (USE_YN=N) | E503 | 9999 | SUSPENDED |
| AUTH_FAIL | 인증 실패 (HTTP 401) | E401 | 8401 | UNAUTH |

---

## 3. 신규 CSS 전환 - "끊지 않고 갈아 끼우기"

제휴사별 `CSS_ENGINE` (ADMIN 에서 즉시 변경, 배포 없음)

| 단계 | 값 | 제휴사가 받는 결과 | 신규 CSS |
|---|---|---|---|
| 1. 현행 유지 | `LEGACY` | 현행 CSS | 호출 안 함 |
| 2. 병행 검증 | `SHADOW` | **현행 CSS** | 동시에 호출해 결과만 `SHADOW_RESULT` 에 기록 → 현행과 비교 |
| 3. 전환 | `NEW` | 신규 CSS | - |
| 원복 | `NEW → LEGACY` | 다음 요청부터 현행 | - |

- SHADOW 비교는 **남은 시간 안에서만** 기다린다. 비교 때문에 제휴사 응답이 늦어지지 않는다.
- 전환 순서 예: 트래픽 적은 제휴사 SHADOW 1~2주 → 결과 차이 검토(현업·CSS팀) → NEW → 다음 제휴사.
- 차단기: 엔진이 연속 5회 실패하면 30초간 호출하지 않고 즉시 일시오류 응답 (장애가 스레드 고갈로 번지는 것을 막음). ADMIN 에서 수동으로 닫을 수 있다.

---

## 4. 테이블 (`schema-common-platformlink.sql`)

| 테이블 | 용도 |
|---|---|
| `TB_LNK_PARTNER` | 제휴사: 어댑터 · 엔진(LEGACY/SHADOW/NEW) · 허용상품 · 제한시간 · 응답여유 · 허용 IP · 서명키 참조 · 사용 |
| `TB_LNK_PRODUCT` | CSS 상품유형(CREDIT/AUTO/MORTGAGE) ↔ 당행 상품 · ADMIN 간편한도조회 목록 구분 |
| `TB_LNK_PARTNER_PRD` | 제휴사가 쓰는 상품코드 (같은 오토대출도 제휴사마다 코드가 다름) |
| `TB_LNK_CODE_MAP` | 내부 결과코드 → 제휴사 결과코드·문구 |
| `TB_LNK_LIMIT_REQ` | 요청·응답 이력 1건 = 1행: 결과 · 엔진 · CSS ms · 전체 ms · SHADOW 비교 · 응답 전문 |

**서명키는 DB 에 두지 않는다.** `SECRET_REF` → `common.platform-link.secrets.{참조명}` (운영은 환경변수 `${LINK_SECRET_TOSS}`).

샘플 제휴사 (`data-common-platformlink.sql`, 모두 **가정 형식**)

| 제휴사 | 어댑터 | 엔진 | 상품 | 제한 | 전문 특징 |
|---|---|---|---|---|---|
| 토스 | TOSS_V1 | LEGACY | 신용·오토 | 3초 | camelCase, 날짜 yyyy-MM-dd, 금리 % |
| 카카오페이 | KAKAOPAY_V1 | SHADOW | 신용·오토·모기지 | 5초 | snake_case, **금액 만원**, 직업 숫자코드, 상품 직접 지정 |
| 핀다 | FINDA_V1 | NEW | 모기지 | 4초 | 담보 필수, 금리 문자열 "5.90" |
| 네이버파이낸셜 | TOSS_V1 (공유) | NEW | 오토 | 3초 | 오픈 예정 → USE_YN=N |

---

## 5. 소스 구성 (`com.example.demojsp.common.platformlink`)

```
controller/  PlatformLinkInboundController   POST /api/link/platform/{cd}/limit   ← 제휴사가 호출 (본문은 String 그대로: 서명 검증용)
             PlatformLinkAdminApiController  /api/common/platform-link-admin/**   ← 디지털어드민
             PlatformLinkDemoApiController   /api/common/platform-link-demo/**    ← local 샘플 전용 (서명 대행)
             PlatformLinkAdminAuth, PlatformLinkSampleController
service/     PlatformLinkService(Impl)       ① ~ ⑧ 흐름
             PlatformLinkAdminService(Impl)  설정 저장(검증·감사로그) · 현황 · 이력 · 차단기
adapter/     PartnerAdapter (인터페이스), JsonPartnerAdapter (도우미), TossAdapter, KakaoPayAdapter, FindaAdapter,
             PartnerAdapterRegistry (ADAPTER_CD → 구현체), AdapterParseException
model/       LimitInquiry (한도조회 공통모델), CssRequest/CssResult (CSS 공통모델), CssProduct (상품 결정 규칙), LinkOutcome
css/         CssGateway (엔진 인터페이스), LegacyCssSimulator, NewCssSimulator, CssRouter (엔진·시간·SHADOW), CircuitBreaker
security/    PartnerSignature (HMAC-SHA256 서명·검증)
mapper/      PlatformLinkMapper + resources/mapper/common/PlatformLinkMapper.xml
config/      PlatformLinkProperties (common.platform-link.*), PlatformLinkConfig (CSS 호출 스레드풀), PlatformLinkSeeder
```

---

## 6. 자주 할 일

### 제휴사 추가 (예: 네이버파이낸셜 오픈)
1. 전문 형식이 기존과 같으면 `TB_LNK_PARTNER.ADAPTER_CD` 만 기존 값으로, 다르면 `PartnerAdapter` 구현체 1개 작성 (`@Component`).
2. DB: `TB_LNK_PARTNER` 1행 · `TB_LNK_PARTNER_PRD` 상품코드 · `TB_LNK_CODE_MAP` 8행.
3. yaml/환경변수: `common.platform-link.secrets.{SECRET_REF}`.
4. 시뮬레이터 화면에서 시나리오별로 보내 보고, ADMIN 에서 `USE_YN=Y` → 오픈. **서비스 코드는 수정하지 않는다.**

### 실제 규격을 받았을 때
- 제휴사 규격 → 해당 어댑터의 `parse` / `response` 만 수정 (클래스 주석의 "가정 형식" 을 실제로 교체).
- 인증 방식이 HMAC 이 아니면 (토큰·mTLS·전문 암호화) `PartnerSignature` 대신 제휴사별 인증기를 둔다 - 어댑터처럼 코드로 고른다.
- 신규 CSS I/F 정의서 → `CssRequest`/`CssResult` 필드를 맞추고 `CssGateway` 실연동 구현체(engine()="NEW") 작성 → `NewCssSimulator` 제거.
- 현행 CSS 호출부 → `CssGateway` 실연동 구현체(engine()="LEGACY") 로 감싼다.

---

## 7. API

| Method | URL | 설명 |
|---|---|---|
| POST | `/api/link/platform/{cd}/limit` | **제휴사 수신 창구** - 제휴사 형식 JSON, 헤더 서명 |
| GET / POST | `/api/common/platform-link-admin/partners` | 제휴사 설정 / 저장 (엔진 · 상품 · 제한시간 · 여유 · IP · 사용) |
| GET | `/api/common/platform-link-admin/partners/{cd}/codes` | 결과코드 변환표 |
| GET | `/api/common/platform-link-admin/stats?hours=24` | 처리현황 (승인 · 거절 · 실패 · 타임아웃 · 평균/최대 ms · 제한초과) |
| GET | `/api/common/platform-link-admin/requests?partner=&status=&result=` | 이력 |
| GET / POST | `/api/common/platform-link-admin/circuit` · `/circuit/{engine}/reset` | 차단기 상태 / 닫기 |
| GET / POST | `/api/common/platform-link-demo/setup` · `/sample` · `/send` | [local] 제휴사 시뮬레이터 |

curl 로 제휴사 흉내 (local):
```bash
BODY='{"requestId":"CURL-1","applicant":{"ci":"<CI 20자 이상>","name":"홍길동","birthDate":"1990-01-01"},"income":{"annualIncome":60000000}}'
TS=$(($(date +%s%N)/1000000))
SIG=$(printf 'TOSS\n%s\n%s' "$TS" "$BODY" | openssl dgst -sha256 -hmac local-toss-secret -binary | base64)
curl -X POST localhost:8080/api/link/platform/TOSS/limit -H "Content-Type: application/json" \
     -H "X-Link-Timestamp: $TS" -H "X-Link-Signature: $SIG" --data "$BODY"
```

---

## 8. 실행 / 테스트

```
제휴사 시뮬레이터  http://localhost:8080/common/platform-link/sample
어드민            http://localhost:8080/common/platform-link/admin
./mvnw test -Dtest=PlatformLinkDbTest     # 10건: 제휴사별 변환 · SHADOW · 상품 규칙 · 인증 · 중복 · 타임아웃 · 중단 · 차단기 · 전문오류 · 어드민
```
시뮬레이터 규칙: 고객명에 **'지연'** → CSS 8초 지연(타임아웃), **'오류'** → CSS 장애. 연소득 2천만원 미만·만 19세 미만 → 거절.

운영 설정: `demo-api-open: false`, `admin-api-open: false`, 서명키 환경변수, 허용 IP `*` 제거, 로드밸런서 뒤라면 `server.forward-headers-strategy` 설정 후 IP 검사.

---

## 9. 확인 필요 (요구분석서 IS-001 · IS-056)

1. **연계 플랫폼 목록과 제휴사별 전문 규격** (현재 어댑터는 가정 형식) · 인증 방식
2. **제휴사별 Timeout 기준**, **신규 CSS 목표 응답시간** (RFP 5.2 현행 성능 수준 보장)
3. **CSS I/F 정의서 확정 일정** (별도 프로젝트) → 그 전까지 시뮬레이터로 개발·테스트
4. 현행 플랫폼 연계 소스 위치, 오토 연계 기구현 모듈(REQ-AP-075) 편입 여부
5. 전환 순서·SHADOW 기간·결과 차이 허용 기준 (현업·CSS팀 합의)
6. 연계 경로: 제휴사 → 당행 직접 HTTPS 인지 FEP/LINK 계층 경유인지
