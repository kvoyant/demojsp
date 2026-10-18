# 이어하기(Resume) 공통모듈 가이드 - [CF-BC-13]

| 항목 | 내용 |
|---|---|
| 요구사항 | 공통#43 홈 - 이어하기 (요건발생일 2026-09-18) |
| 공통기능 | CF-BC-13 이어하기(Resume)·신청현황 공통 (보완: CF-UI-01 카드 컴포넌트, CF-NA-07 탭 툴팁 연계) |
| 패키지 | `com.example.demojsp.common.resume` |
| 샘플 화면 | `/common/resume/sample` · 신청현황조회 `/common/resume/status` |
| 재사용 예정 | 공통#49 저축 - 이어하기(`scope=DEPOSIT`), 공통#50 저축 탭 툴팁(`counts.DEPOSIT`), 공통#60 대출 - 이어하기(`scope=LOAN`) |

## 1. 요구사항 → 구현 매핑

| 요구사항 (공통#43) | 구현 |
|---|---|
| 목적: 뒷 화면 가림현상 개선 | 팝업 → **화면 상단 카드** (`position: fixed` 없음, 흐름 안 배치). 0건이면 자리도 차지하지 않음 |
| 정책1 제공대상: 이어하기 대상자 | 진행중·만료 전 건이 0이면 `count=0, latest=null` → 카드 미노출 |
| 정책2 상품별 기존 이어하기 정책 준용 | 업무가 단계 저장 시 `expireDays`(상품별 만료) 지정, 완료/취소/만료 건 자동 제외 |
| 기능1 전체 중 최신 1건 | `ResumePoint.LATEST_FIRST` = 마지막 진행일시 → 최초 신청일시 → ID 내림차순 |
| 기능2 건수 "숫자" 표시 | 카드 제목 옆 배지 `ui-resume__count` (스크린리더 "총 N건") |
| 기능3 [더보기] → 신청현황조회 | `moreUrl` = `common.resume.status-url` (실제 APP 화면 URL 로 교체) |
| 기능4 [이어하기] → 해당 상품 신청 링크 | `POST /api/common/resume/{id}/continue` 로 **본인·진행중·만료 전 재확인** 후 URL 제공 → 이동 중 표시(UiButton) |
| 기능5 로고 + 회사명 + 상품명 + 이어하기 | 회사명·로고는 **회사 마스터**(`PartnerRepository`, 회사명 DB)에서 조회. 당행 → 당행 로고, 대외 연계 → 기업 로고 |
| 기능5 로고 수집·제작 필요 | 로고 미등록/로딩 실패 시 대체 로고: 당행 = 은행 아이콘(공통#41), 제휴사 = 회사명 첫 글자 |
| 기능6 위치: 화면 상단 | `<div id="resumeCard">` 를 홈 최상단에 두고 `ResumeCard.mount` |

## 1-2. 공통#49 저축 - 이어하기 (저축 탭) - 샘플 `/common/savings/sample`

| 요구사항 (공통#49) | 구현 |
|---|---|
| 정책1 제공대상: 이어하기 대상자 | 수신 진행 중 0건이면 카드 미노출 |
| 정책2 기존 이어하기 정책 준용 (**수신상품만 취급**) | `scope=DEPOSIT` → 수신상품만 조회·건수. 수신 이어하기 등록은 **당행 상품만** 허용 (제휴사 코드 RSM-105) |
| 기능1 수신상품 이어하기 중 최신 1건 | 홈과 같은 최신 규칙 (마지막 진행일시) - 홈에서 최신인 여신 건은 저축 탭에서 제외 |
| 기능2 이어하기 건수 표시 | 카드 제목 옆 수신 건수 |
| 기능3 [더보기] → 신청현황조회 | `/common/resume/status?scope=DEPOSIT` (수신 탭으로 열림) |
| 기능4 [이어하기] → 해당상품신청링크 | 홈과 동일 (서버 재확인 후 이동) |
| 기능5 로고(당행) + 회사명(DB) + 상품명 + 이어하기 | 당행 로고 (`own=true`, 로고 미등록 시 은행 아이콘) |

저축 탭 적용: `ResumeCard.mount('#resumeCard', { scope: 'DEPOSIT' })` - 카드 제목 기본값 "진행 중인 저축 가입".

## 1-3. 공통#50 저축 - 이어하기 툴팁 (하단 '저축' 탭 숫자)

| 요구사항 (공통#50) | 구현 |
|---|---|
| 방향: 수신계좌 개설 이어하기 보유 시 탭에 강조 / 목적: 진행 건 강조 | 공통 하단 탭바 `BottomNav` + 저축 탭 숫자 배지 |
| 정책1 제공대상: 수신계좌 이어하기 대상자 모두 | `GET /api/common/resume/counts` 의 `DEPOSIT` 건수 > 0 |
| 정책2 존재 시 상시 활성 - 홈·저축·대출 탭 어디서든 | 세 탭 화면 모두 같은 `BottomNav.mount('#bottomNav', { active: '현재탭' })` → 배지는 항상 '저축' 탭에 |
| 기능1 '저축' 탭에 아이콘 형태로 숫자 | 아이콘 오른쪽 위 빨간 원 숫자 (99+), 스크린리더는 탭 이름에 "진행 중인 저축 가입 N건" |
| 기능2 미노출: 진행 건 없음 / 진행 중 취소로 잔여 0건 | 0 이면 배지 숨김. 취소·완료 후 `ResumeCard.changed()`(또는 `ResumeTabBadge.refresh()`) → 즉시 재조회. 뒤로가기·앱 복귀 시에도 재조회 |

```html
<link rel="stylesheet" href="/css/common/bottom-nav.css">
<script src="/js/common/bottom-nav.js"></script>
<nav id="bottomNav"></nav>
<script>BottomNav.mount('#bottomNav', { active: 'SAVINGS' });</script>
```
- 건수 API 는 비로그인이면 오류 없이 0건 (탭바는 그대로, 배지만 숨김).
- APP 네이티브 탭바를 쓰면 `onBadge: function (tab, n) { NativeBridge.setTabBadge(tab, n); }` 로 같은 건수를 넘긴다 (CF-NA-07).
- 대출 탭 툴팁이 필요해지면 `badges: { SAVINGS: 'DEPOSIT', LOAN: 'LOAN' }`.

## 2. 업무 화면 적용 (APP 홈)

```html
<link rel="stylesheet" href="/css/common/ui-tokens.css">
<link rel="stylesheet" href="/css/common/ui-button.css">
<link rel="stylesheet" href="/css/common/resume-card.css">
<script src="/js/common/ui-button.js"></script>   <!-- 선택: 이어하기 버튼 "이동 중" 표시 -->
<script src="/js/common/resume-card.js"></script>

<div id="resumeCard"></div>   <!-- 화면 상단 -->
<script>
  ResumeCard.mount('#resumeCard', { scope: 'ALL' });     // 저축 탭 'DEPOSIT' / 대출 탭 'LOAN'
  // APP 네이티브 화면전환: ResumeCard.mount('#resumeCard', { scope: 'ALL', go: function (url) { NativeBridge.open(url); } });
</script>
```

- 조회 실패 시에도 홈 화면을 막지 않도록 카드를 조용히 숨긴다 (`onError` 로 로그만).
- [이어하기] 시점에 이미 완료·취소·만료된 건이면 안내 알럿 후 카드를 최신 상태로 다시 그린다.
- 뒤로가기(bfcache)로 홈에 돌아오면 자동 재조회.

## 3. API

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/common/resume?scope=ALL` | 카드: `count`, `counts{ALL,DEPOSIT,LOAN}`, `latest`, `moreUrl` |
| GET | `/api/common/resume/list?scope=ALL` | 신청현황조회: 진행 중 전체(최신순) |
| POST | `/api/common/resume/{id}/continue` | 이동 URL (본인 아니면 RSM-003, 종료/만료면 RSM-002) |
| GET | `/api/common/resume-demo/partners` | (local) 회사 마스터 |
| POST | `/api/common/resume-demo/steps` | (local) 신청 업무 단계 저장 흉내 |
| POST | `/api/common/resume-demo/{id}/complete` · `/cancel` | (local) 완료/취소 흉내 |

고객은 **로그인 세션(LOGIN_USER)으로만** 식별한다. `demo-api-open=true`(local)이고 로그인하지 않았을 때만 `demoCustomer` 파라미터를 쓴다.
응답에는 고객 ID·이동 URL 을 넣지 않는다 (이동 URL 은 continue 응답으로만).

## 4. 신청 업무 연동 (각 업무 개발자)

신청 업무가 단계를 저장할 때 서버 내부에서 호출한다.

```java
long id = resumeService.saveStep(customerId, new ResumePointRequest(
        "LOAN", "LOAN_APPLY", "대출신청", "PS_SMART", "페퍼스마트대출", "PARTNER_A",
        "서류제출", 3, 5, "/loan/apply/PS_SMART?step=3", 7));   // 마지막 = 상품별 이어하기 만료일수
resumeService.complete(customerId, id);   // 신청 완료
resumeService.cancel(customerId, id);     // 신청 취소 (공통#50 툴팁 잔여 0건 → 미노출)
```

- 같은 고객·업무·상품이 진행 중이면 **새로 만들지 않고 갱신** (건수 중복 방지).
- 이어하기 링크는 같은 도메인 상대경로 또는 `allowed-url-prefixes` 의 딥링크만 허용 (`javascript:`, `//외부` 차단).

## 5. 회사명 DB (공통#43 "DB 구성 필요")

`PartnerRepository` 운영 구현 시 테이블 예시 (Javadoc 참고): `TB_CO_PARTNER(PARTNER_CD, PARTNER_NM, LOGO_URL, OWN_YN, USE_YN)`.
local 은 `application.yaml` `common.resume.partners`. 로고는 수집·제작 후 `logo-url` 등록 (샘플: `/images/common/resume/logo-partner-sample.svg` 는 자리표시용).

## 6. 오류 코드 (RSM-)

| 코드 | HTTP | 의미 |
|---|---|---|
| RSM-001 | 400 | scope 오류 |
| RSM-002 | 409 | 이미 완료·취소·만료된 신청 |
| RSM-003 | 404 | 없음(타 고객 포함) |
| RSM-004 / 101~104 | 400 | 요청·필수·글자수·링크·코드 검증 |
| RSM-105 | 400 | 수신 이어하기에 제휴사 코드 (수신은 당행 상품만) |
| RSM-401 | 401 | 로그인 필요 |
| RSM-402 | 403 | 테스트 API (local 전용) |

## 7. 확인 필요사항 (업무 협의)

- "최신" 기준: 현행 정책을 **마지막 진행일시**로 해석함 → 현행이 최초 신청일시 기준이면 `LATEST_FIRST` 만 변경.
- 상품별 이어하기 만료일수, 모집법인 비즈니스옵션 적용 여부(CF-TC-12).
- 회사명 DB 구성 주체, 당행 로고 제작·제휴사 로고 수집.

## 8. 테스트

`ResumeFlowTest` (9건, DB 불필요): 최신 1건·건수·범위별 건수, 완료/만료 제외, scope 재사용, 미대상 카드 미노출,
이어하기 재확인(타 고객·취소 후), 단계 저장 갱신/등록, 링크·코드 검증, 운영 설정 차단, 카드 JS 계약.

`SavingsResumeTest` (5건, 공통#49): 수신 최신 1건·건수(여신 제외), 더보기·당행 로고, 이어하기·취소 후 다음 건, 수신 당행 전용, 저축 탭 제목.

`SavingsTabTooltipTest` (4건, 공통#50): 수신 건수(만료·여신 제외), 미대상·취소로 0건, 비로그인 0건 오류 없음, 탭바 JS 계약.
