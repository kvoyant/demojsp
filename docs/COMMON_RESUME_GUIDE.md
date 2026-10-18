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
| RSM-401 | 401 | 로그인 필요 |
| RSM-402 | 403 | 테스트 API (local 전용) |

## 7. 확인 필요사항 (업무 협의)

- "최신" 기준: 현행 정책을 **마지막 진행일시**로 해석함 → 현행이 최초 신청일시 기준이면 `LATEST_FIRST` 만 변경.
- 상품별 이어하기 만료일수, 모집법인 비즈니스옵션 적용 여부(CF-TC-12).
- 회사명 DB 구성 주체, 당행 로고 제작·제휴사 로고 수집.

## 8. 테스트

`ResumeFlowTest` (9건, DB 불필요): 최신 1건·건수·범위별 건수, 완료/만료 제외, scope 재사용, 미대상 카드 미노출,
이어하기 재확인(타 고객·취소 후), 단계 저장 갱신/등록, 링크·코드 검증, 운영 설정 차단, 카드 JS 계약.
