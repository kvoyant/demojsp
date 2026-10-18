# 홈 퀵메뉴 · 전체메뉴 가이드 - [공통#46]

| 항목 | 내용 |
|---|---|
| 요구사항 | 공통#46 홈 - 퀵메뉴 (요건발생일 2026-09-18) |
| 공통기능 | CF-UI-01 공통 컴포넌트, CF-NA-07 Navigation |
| 패키지 | `com.example.demojsp.common.menu`, 회원구분 `com.example.demojsp.common.member` |
| 샘플 화면 | `/common/menu/sample` |

## 1. 요구사항 → 구현 매핑

| 요구사항 | 구현 |
|---|---|
| 방향: 회원구분 별 개인화 메뉴 노출 / 목적: 핫키 UI/UX 개선 | 홈 퀵메뉴 `QuickMenu.mount` (아이콘 타일 + 메뉴명 링크) |
| 정책1 제공대상: 모든 사용자 | 비로그인 포함 조회 (로그인 불필요 API) |
| 정책2 사용자 유형별 개인화 | 회원구분 `MemberSegment` (GUEST 비로그인 / MEMBER 일반회원 / FINANCIAL 금융회원=계좌 보유) 별 배치 `common.menu.quick.layouts`. 비로그인 고객이 로그인 필요 메뉴(오픈뱅킹·전자서명·서류제출)를 누르면 `/login?returnUrl=...` |
| 기능1 6개 노출, 3*2 | `max-count: 6`, `columns: 3` (7번째 이상 무시) |
| 기능2 구조: 이미지 + 메뉴명 | 공통 아이콘(공통#41) 스프라이트 사용 - `products`(전체상품), `open-banking`(오픈뱅킹) 아이콘 추가 |
| 기능3 메뉴 하드코딩 6종 | yaml `quick.items` 고정 (ADMIN 관리 아님, 배포로만 변경) |
| 기능4 전체메뉴 > 상품 > 전체상품 신규 | `GET /api/common/menus/all`, `QuickMenu.mountAll` - 상품: 전체상품(N)·예금상품·대출상품·금융계산기 |
| 기능5 ADMIN 뱅킹메뉴목록: 전체상품 신규등록, 표시순서 수정 | `/api/common/menu-admin/banking-menus` 등록·수정·`reorder`. 표시순서는 카테고리 안에서 항상 1..N (끼워 넣으면 나머지 밀림) |

## 2. 업무 화면 적용

```html
<link rel="stylesheet" href="/css/common/ui-tokens.css">
<link rel="stylesheet" href="/css/common/ui-icon.css">
<link rel="stylesheet" href="/css/common/quick-menu.css">
<script src="/js/common/quick-menu.js"></script>

<div id="quickMenu"></div>
<script>
  QuickMenu.mount('#quickMenu');                                   // 홈
  QuickMenu.mountAll('#allMenu');                                  // 전체메뉴
  // APP 네이티브 화면전환: QuickMenu.mount('#quickMenu', { onNavigate: function (url) { NativeBridge.open(url); } });
</script>
```

## 3. API

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/common/menus/quick` | `segment`, `columns`, `rows`, `items[code,name,icon,url,requiresLogin,loginFirst]` |
| GET | `/api/common/menus/all` | `categories[code,name,menus[id,name,url,newBadge]]` |
| GET | `/api/common/menu-admin/categories` | 카테고리 |
| GET | `/api/common/menu-admin/banking-menus?category=PRODUCT` | 뱅킹메뉴목록 |
| POST / PUT | `/api/common/menu-admin/banking-menus[/{id}]` | 등록 / 수정 (`category,name,url,displayOrder,use,newBadge`) |
| POST | `/api/common/menu-admin/banking-menus/reorder?category=PRODUCT` | body `[id,...]` 카테고리 전체 순서 |

오류 코드 MNU-001(없음) 002(요청) 101(필수) 102(글자수) 103(URL: 같은 도메인 상대경로만) 104(카테고리) 105(순서) 106(같은 이름) 401(권한).

## 4. 회원구분 (공통#47 추천 상품과 공유)

`MemberSegmentResolver` - 로그인 세션 고객 → 계좌 보유(`AccountHoldingChecker`, 상단슬라이드배너와 같은 판단)면 금융회원.
local 은 `common.member.demo-customer-param: true` 로 `demoCustomer` 파라미터 흉내 (운영 false).

## 5. 확인 필요사항

- "사용자 유형별 개인화" 와 "메뉴 하드코딩 6종" 이 함께 있어, 6종은 고정하고 **회원구분별 배치·로그인 경유**로 개인화함.
  유형별로 다른 메뉴가 필요하면 `quick.items` 에 메뉴를 추가하고 `layouts.{구분}` 만 바꾸면 된다.
- 운영 뱅킹메뉴 테이블 구조 확인 후 `BankingMenuRepository` MyBatis 구현.

## 6. 테스트

`HomeMenuTest` (6건): 6개·3*2·하드코딩 순서, 비로그인 로그인 경유·금융회원 배치, 전체상품 신규등록→전체메뉴 반영,
위아래 이동·끼워넣기·미사용 제외, ADMIN 검증(이름 중복·URL·카테고리·권한), 아이콘/JS 계약.
