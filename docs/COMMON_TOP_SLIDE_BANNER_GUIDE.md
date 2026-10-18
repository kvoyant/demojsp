# 홈 상단슬라이드배너 가이드 - [CF-BC-20]

| 항목 | 내용 |
|---|---|
| 요구사항 | 공통#44 홈 - 상단슬라이드배너 (APP) · 공통#45 ADMIN 수정개발 (요건발생일 2026-09-18) |
| 공통기능 | CF-BC-20 콘텐츠·게시 공통 (배너팝업 공통모듈 확장), CF-UI-02 접근성 |
| 패키지 | `com.example.demojsp.common.banner` (배너팝업구분 `TOP_SLIDE`) |
| 샘플 화면 | `/common/banner/top-slide` |

배너팝업(공통#39)과 **같은 저장소·게시기간·표시순서 규칙**을 쓰고, 배너팝업구분(`popupType`)으로 노출 API 를 분리한다.
배너팝업 API(`/api/common/banner-popups`)에는 상단슬라이드배너가 섞이지 않는다.

## 1. 요구사항 → 구현 매핑 (공통#44)

| 요구사항 | 구현 |
|---|---|
| 방향/목적: 상품소개 배너 롤링, 기존 메인 "슬라이드 배너팝업" 대체 | 홈 상단 흐름 안 배너(팝업 아님) `TopSlideBanner.mount` |
| 정책1 제공대상: 계좌 미보유 사용자 | `AccountHoldingChecker.hasAccount` → 보유 시 `eligible=false, audience=ACCOUNT_HOLDER`, 배너 없음. 비로그인은 미보유로 보고 노출 |
| ※ 계좌보유 회원: 같은 영역에 계좌정보 | 화면이 `onIneligible` 에서 같은 자리에 계좌정보 컴포넌트 표시 |
| 정책2 배너노출기간 (00:00:00 ~ 00:00:00) | 시작 포함 · 종료 미포함 (`BannerPopup.inPeriod`) |
| 기능1 최대 5건 | `top-slide.max-count: 5`, 표시순서 상위 5건 (`truncated` 로 제외 건수 확인) |
| 기능2 자동 슬라이딩 2초 | `top-slide.interval-ms: 2000` → 응답 `intervalMs` 로 화면이 따름 (끝 → 처음 순환) |
| 기능2 2·3·4개 셋팅 시 빈 슬롯 숨김 | 받은 건수만큼만 슬라이드·카운트 생성 (고정 5칸 없음). 1건 = 고정(자동 넘김·인디케이터 없음), 0건 = 영역 숨김 |
| 기능3 구조: 메인텍스트 + 서브텍스트 + 배너 수 | `mainText`(ADMIN 제목) / `subText`(ADMIN 내용) / 하단 `01/05` |
| 기능4 Indicator 자동 변경, 일시정지/재생, "01/05" | 하단 pill: [❚❚/▶] + `현재/전체` 2자리, 슬라이드 이동 시 자동 갱신 |
| ※ 어드민 셋팅 기능 | 공통#45 (배너팝업목록 > 배너팝업구분 "상단슬라이드배너") |

### 배경색 · 글자색
ADMIN 이 고른 배경색(#RRGGBB, 서버 검증)은 그대로 쓰고, **글자색은 서버가 대비 4.5:1 이상이 되는 색을 골라** `textColor` 로 내려준다
(#111827 → 부족하면 #FFFFFF / #000000 중 높은 쪽). 어떤 배경색을 골라도 문구를 읽을 수 있다.

### 접근성 (움직이는 콘텐츠)
- 일시정지/재생 버튼(WCAG 2.2.2), 마우스 올림·키보드 포커스·터치 중·화면 숨김 시 자동 넘김 멈춤
- carousel / slide 역할, 슬라이드 라벨 "3 / 5", 사용자가 직접 넘길 때만 스크린리더 안내
- 보이지 않는 슬라이드 링크는 Tab 이동 제외, ←/→ 키 · 스와이프 지원, 움직임 줄이기 설정 시 애니메이션 없음

## 2. 업무 화면 적용 (APP 홈)

```html
<link rel="stylesheet" href="/css/common/ui-tokens.css">
<link rel="stylesheet" href="/css/common/top-slide-banner.css">
<script src="/js/common/top-slide-banner.js"></script>

<div id="topSlide"></div>          <!-- 홈 상단 -->
<div id="accountInfo" hidden></div> <!-- 계좌 보유 회원용 (같은 영역) -->
<script>
  TopSlideBanner.mount('#topSlide', {
      channel: 'APP',
      onIneligible: function () { document.getElementById('accountInfo').hidden = false; },
      onLink: function (url) { NativeBridge.open(url); }   // 생략 시 location.assign
  });
</script>
```

## 3. API

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/common/top-slide-banners?channel=APP` | `eligible`, `audience`, `total`, `maxCount`, `intervalMs`, `truncated`, `items[]` |
| GET | `/api/common/banner-admin/popups?popupType=TOP_SLIDE` | ADMIN 목록 (구분 필터) |

`items[]`: `id, mainText, subText, imageUrl, altText, linkUrl, backgroundColor, textColor, displayOrder`

## 4. 설정 (`application.yaml` `common.banner-popup.top-slide`)

| 키 | 기본 | 설명 |
|---|---|---|
| `max-count` | 5 | 최대 노출 건수 |
| `interval-ms` | 2000 | 자동 슬라이딩 간격 |
| `default-background-color` | #E3ECFA | 배경색 미지정 시 |
| `demo-customer-param` | false | **local 전용** - 비로그인 시 `demoCustomer` 파라미터로 고객 흉내 |
| `account-holders` | [] | local 흉내용 계좌 보유 고객 (운영은 `AccountHoldingChecker` 계좌원장 구현체로 교체) |

## 5. 운영 전환

- `AccountHoldingChecker` 구현체(수신/여신 계좌원장 조회)를 `@Primary` 로 등록 - 홈 진입마다 호출되므로 로그인 세션 캐시 권장
- `demo-customer-param: false`

## 6. 테스트

`TopSlideBannerTest` (7건): 최대 5건·표시순서·간격·기간 경계·미사용 제외, 배경/글자색, 계좌 보유 제외,
운영 설정에서 파라미터 흉내 차단, 배너팝업 API 와 분리, 글자 대비 4.5:1 보장, 화면 JS 계약.
