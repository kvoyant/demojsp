# 홈 추천 상품 가이드 - [공통#47 · 공통#48]

| 항목 | 내용 |
|---|---|
| 요구사항 | 공통#47 홈 - 추천 상품 (APP) · 공통#48 ADMIN 상품설정 (요건발생일 2026-09-18) |
| 공통기능 | CF-BC-20 콘텐츠·게시 공통 (추천상품 홈 표시여부/순서) |
| 패키지 | `com.example.demojsp.common.product`, 회원구분 `common.member` (공통#46 과 공유) |
| 샘플 화면 | `/common/product/recommend` |

## 1. 공통#47 요구사항 → 구현

| 요구사항 | 구현 |
|---|---|
| 방향/목적: 신규상품 가입 유입경로, 여신/수신 상품정보 연결 | 홈 "추천 상품" 카드 목록 `RecommendProducts.mount` |
| 정책1 제공대상: 금융회원 외 모든 회원 | 회원구분 GUEST(비로그인)·MEMBER(일반회원) 노출, FINANCIAL(계좌 보유) 제외 |
| 정책2 상품 노출순서 ADMIN 처리 | 상품 원장의 **홈 표시여부 Y · 홈 표시순서**(공통#48)로 정렬, 판매중지 상품 제외 |
| 기능1 상품목록 (여신·수신·입출금) | 페퍼스마트대출 / 페퍼스 2030적금 / 페퍼스 파킹통장4 (seed), 카드: 아이콘·상품유형·상품명·설명·금리 |
| 기능2 [더보기] → 전체상품목록(상품몰) | `moreUrl` = `common.product.mall-url` (전체메뉴 "전체상품"과 같은 화면) |
| 기능3 상품 터치 → 상품안내페이지 | 카드 전체가 `detailUrl` 링크 |
| 기능4 금융회원 화면: 추천 상품 전체 숨김 | `eligible=false`, 화면은 영역 hidden |

## 2. 업무 화면 적용

```html
<link rel="stylesheet" href="/css/common/ui-tokens.css">
<link rel="stylesheet" href="/css/common/ui-icon.css">
<link rel="stylesheet" href="/css/common/recommend-products.css">
<script src="/js/common/recommend-products.js"></script>

<div id="recommend"></div>
<script>
  RecommendProducts.mount('#recommend');
  // APP 네이티브 화면전환: RecommendProducts.mount('#recommend', { onNavigate: function (url) { NativeBridge.open(url); } });
</script>
```

## 3. API

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/common/products/recommend` | `eligible`, `segment`, `moreUrl`, `items[id,code,kind,kindName,subType,name,summary,rateText,icon,detailUrl,homeOrder]` |

## 4. 확인 필요사항

- **공통#47 상품목록은 4종(여신2·수신1·입출금1)인데 공통#48 은 홈 표시 "총 3개까지"** → 3개 기준으로 개발
  (`common.product.max-home-count: 3`, seed 는 페퍼스 사업자담보대출 N). 4개로 확정되면 설정값만 4 로 변경.
- 금융회원 기준: 수신 또는 여신 계좌 보유 (상단슬라이드배너 공통#44 와 동일 판단).

## 5. 테스트

`RecommendProductTest` (4건): 홈 표시순서·N/판매중지 제외, 더보기·상품안내 링크·원장 내용, 금융회원 숨김, 화면 JS 계약.
