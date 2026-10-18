# APP 저축 탭 가이드 - [공통#49 · #50 · #51]

| 항목 | 내용 |
|---|---|
| 요구사항 | 공통#49 저축 - 이어하기 · 공통#50 저축 - 이어하기 툴팁 · 공통#51 저축 - 메인 콘텐츠 (요건발생일 2026-09-18) |
| 샘플 화면 | `/common/savings/sample` (저축 탭 전체 + 하단 탭바) |
| 상세 가이드 | 이어하기·툴팁: `docs/COMMON_RESUME_GUIDE.md` 1-2·1-3절 |

## 1. 저축 탭 화면 구성 (세 요구사항 조립)

```
┌ 저축 ───────────────────────────┐
│ [진행 중인 저축 가입 (2)  더보기>] │ ← 공통#49 ResumeCard scope=DEPOSIT (수신 0건이면 없음)
│                                  │
│   매일 쌓이는 이자                 │ ← 공통#51 SavingsMain (계좌 미보유)
│   모으는 재미가 쏠쏠               │    계좌 보유 고객은 이 자리에 계좌정보
│        [일러스트]                  │
├──────────────────────────────────┤
│        [ 계좌 개설하기 ]           │ ← 공통#51 하단 상시 (본문 스크롤과 무관)
├──────────────────────────────────┤
│   홈      저축(2)      대출        │ ← 공통#50 BottomNav - 저축 탭 숫자 (홈·대출 화면에서도)
└──────────────────────────────────┘
```

```html
<div class="page-body">
  <div id="resumeCard"></div>
  <div id="savingsMain"></div>
  <div id="accountInfo" hidden>...계좌정보...</div>
</div>
<div id="savingsCta"></div>
<nav id="bottomNav"></nav>
<script>
  ResumeCard.mount('#resumeCard', { scope: 'DEPOSIT' });                       // 공통#49
  SavingsMain.mount('#savingsMain', { cta: '#savingsCta',                     // 공통#51
      onIneligible: function () { document.getElementById('accountInfo').hidden = false; } });
  BottomNav.mount('#bottomNav', { active: 'SAVINGS' });                        // 공통#50
</script>
```

## 2. 공통#51 저축 - 메인 콘텐츠

| 요구사항 | 구현 |
|---|---|
| 방향: 계좌 미보유 고객에게 계좌개설 프로세스 제공 / 목적: 입출금통장 개설 촉진 | `GET /api/common/savings/main` + `SavingsMain.mount` |
| 정책1 제공대상: 계좌를 보유하지 않은 모든 사용자 | 회원구분(공통 `MemberSegmentResolver`) GUEST·MEMBER → `eligible=true`, FINANCIAL(계좌 보유) → `false` (같은 자리에 계좌정보) |
| 정책2 로그인 전/후 동일 화면 | GUEST 와 MEMBER 응답 내용 동일 (회원구분 코드만 다름) |
| 기능1 구조: 텍스트 + 이미지 + 버튼 | 큰 문구 2줄(둘째 줄 강조색) + 일러스트(대체텍스트) + 버튼 |
| 기능2 "매일 쌓이는 이자 / 모으는 재미가 쏠쏠" | `common.savings.main.lines` (확정 문구 기본값) |
| 기능3 [계좌 개설하기] 하단 상시 노출 | 본문 밖 `cta` 자리(탭바 바로 위)에 `.ui-cta-bar`(공통#40) - 스크롤해도 항상 보임, press·"이동 중" 표시 |
| 기능3 터치 시 입출금통장 상품안내페이지 | 상품 원장(공통#47·48)의 `product-code: PS_PARKING4` 상품안내 URL → ADMIN 이 URL 을 바꾸면 자동 반영, 판매중지면 `fallback-url` |

### 설정 (`common.savings.main`)
`lines`, `image-url`, `image-alt`, `cta-text`, `product-code`, `fallback-url` - 일러스트는 샘플 자리표시용(`/images/common/savings/main-coins.svg`), 디자인 확정 이미지로 교체.

## 3. 테스트

- `SavingsMainTest` (4건, 공통#51): 텍스트·이미지·버튼·입출금통장 URL, 로그인 전후 동일·계좌 보유 미노출, ADMIN URL 변경·판매중지 대체 URL, 화면 JS 계약
- `SavingsResumeTest` (5건, 공통#49), `SavingsTabTooltipTest` (4건, 공통#50)
