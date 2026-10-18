# 공통 UI 컴포넌트 가이드 (CF-UI-01)

요구사항정의서(공통) No.40 Button · No.41 Icon · No.42 Alert 를 **전 화면 공통 디자인 컴포넌트**로 제공한다.
요구사항정의서 비고는 "APP의 Main으로 이동" 이지만, 공통기능 정의서(RA-035)의 권고대로 메인 화면에 귀속시키지 않고
공통 UI 표준(CF-UI-01)으로 관리한다. → 다른 화면에서 같은 컴포넌트를 중복 구현하지 않는다.
(확인필요 IS-029: 공통 컴포넌트 관리 귀속(메인 vs 공통) 확정 - Open)

| 구분 | 파일 | 비고 |
|---|---|---|
| 디자인 토큰 | `/css/common/ui-tokens.css` | 색상·글자·간격 값. Figma 확정 시 이 파일 값만 교체 |
| Button (No.40) | `/css/common/ui-button.css`, `/js/common/ui-button.js` | 샘플 `/common/ui/button` |
| Icon (No.41) | `/images/common/ui-icons.svg`, `/css/common/ui-icon.css`, `/js/common/ui-icon.js` | 샘플 `/common/ui/icon` |
| Alert (No.42) | `/css/common/ui-alert.css`, `/js/common/ui-alert.js` (+ `common.js` 의 `Common.alert/confirm`) | 샘플 `/common/ui/alert` |

- 접두어 `ui-` 를 사용해 Bootstrap(`.btn`)·기존 화면 CSS 와 충돌하지 않는다.
- jQuery / Bootstrap 비의존. 화면 문구는 `textContent` 로만 출력한다 (XSS 안전).
- 서버 로직 없음. `UiComponentSampleController` 는 샘플·테스트 허브 확인용이다.

---

## 1. Button (공통#40)

### 요구사항

| 항목 | 내용 |
|---|---|
| 목적 | 사용자의 감각으로 알아차릴 수 있도록 색상 대비 높이기 또는 누른 효과 주기 등 효과 개선 |
| 기능설명 | 대출 신청하기, 가입하기 등 사용자의 행동을 유도하는 MAIN 버튼 |
| 요구사항 | 페이지 이동 중 또는 업무 실행 중이라는 인식을 줄 수 있는 press 디자인 컴포넌트 |

### 구현 내용

| 요구 | 구현 |
|---|---|
| 색상 대비 | 버튼 글자/배경 4.5:1 이상 (KWCAG 2.2 / WCAG 2.1 AA). primary 8.6:1, 누름 12.9:1, danger 5.6:1, 비활성 5.1:1 |
| 누른 효과 | `:active` / `.is-pressed` → 한 단계 진한 색 + `scale(.97)` + 안쪽 그림자(80ms), 누른 위치 물결(ripple) |
| 터치 기기 | iOS 등 `:active` 가 늦는 기기도 `pointerdown` 즉시 `.is-pressed`, 탭 하이라이트 제거, 300ms 지연 제거 |
| 업무 실행 중 | `UiButton.run()` → 스피너 + "신청 중" 문구 + 버튼 하단 진행 막대, `aria-busy`, 최소 400ms 표시(깜빡임 방지) |
| 페이지 이동 중 | `data-ui-nav` / `UiButton.navigate()` → "이동 중" + 화면 상단 진행 막대 |
| 중복 실행 차단 | 진행 중 버튼 클릭은 업무 핸들러까지 전달되지 않음, `form[data-ui-submit]` 재제출 차단 |
| 안전장치 | 응답이 오지 않아도 30초 후 자동 해제(이동은 15초), 뒤로가기(bfcache) 복원 시 자동 해제 |
| 접근성 | `:focus-visible` 2중 포커스 링, 스크린리더 "처리 중입니다" 안내(aria-live), 움직임 줄이기 설정 시 애니메이션 최소화, 고대비 모드 |

> 진행 중에는 버튼을 회색(비활성)으로 바꾸지 않는다. 회색은 "불가/실패" 로 오인되므로 누른 색을 유지한다.

### 사용법

```html
<link rel="stylesheet" href="/css/common/ui-tokens.css">
<link rel="stylesheet" href="/css/common/ui-button.css">
<script src="/js/common/ui-button.js"></script>

<!-- APP 하단 고정 MAIN 버튼 -->
<div class="ui-cta-bar">
  <div class="ui-btn-group">
    <a class="ui-btn ui-btn--outline ui-btn--lg" href="/loan/limit" data-ui-nav>이전</a>
    <button type="button" id="btnApply" class="ui-btn ui-btn--lg ui-btn--main" data-busy-text="신청 중">대출 신청하기</button>
  </div>
</div>
```

```javascript
// 업무 실행 (Promise 가 끝날 때까지 진행 중 표시, 재클릭 무시)
document.getElementById('btnApply').addEventListener('click', function () {
    UiButton.run(this, function () {
        return fetch('/api/loan/apply', { method: 'POST' }).then(function (r) { return r.json(); });
    }).then(function (res) { /* 완료 화면 */ });
});

// 페이지 이동 (APP 은 go 에 네이티브 화면전환 브릿지를 넘긴다)
UiButton.navigate(btn, '/loan/apply', { go: function (url) { NativeBridge.open(url); } });

// 직접 제어
UiButton.busy(btn, { text: '확인 중' });
UiButton.done(btn);
```

| 클래스 | 설명 |
|---|---|
| `ui-btn--primary`(기본) / `--secondary` / `--outline` / `--text` / `--danger` | 변형 |
| `ui-btn--lg`(56px) / 기본(48px) / `--sm`(36px) | 크기 (터치 영역 44px 이상은 기본·lg 사용) |
| `ui-btn--block`, `.ui-btn-group`(+ `.ui-btn--main` 1:2 비율), `.ui-cta-bar` | 배치 |
| `data-busy-text="신청 중"` | 진행 중 문구 (기본 "처리 중", 이동 "이동 중") |
| `data-ui-nav[="/url"]` | 클릭 시 이동 중 표시 후 이동 (값 없으면 href) |
| `form[data-ui-submit]` | 제출 버튼 진행 중 + 재제출 차단 (화면 검증에서 preventDefault 하면 적용 안 함) |
| `data-ripple="false"` | 물결 효과 끄기 |

### 테스트

- 샘플: `http://localhost:8080/common/ui/button` (테스트 허브 `/common/test-hub` → 공통#40)
  - APP 목업 [대출 신청하기] → 2초간 "신청 중", 진행 중 재클릭 무시
  - [이전] → 1.5초 지연 응답 페이지 이동, "이동 중" + 상단 진행 막대
  - 색상 대비 표: 화면에 실제 적용된 색으로 대비 계산
  - 중복 실행 차단: "빠르게 5번 클릭" → 클릭 5회 / 실행 1회
- 단위 테스트: `UiComponentTest` (샘플 컨트롤러, CSS/JS 요구 계약, 토큰 색상 대비 계산)

---

## 2. Icon (공통#41)

### 요구사항

| 항목 | 내용 |
|---|---|
| 목적 | 단조로운 화면 구성 개선 및 화면 별 일관성과 사용성 향상 |
| 기능설명 | 사용자의 이해를 돕는 보조 이미지 도구 |
| 요구사항 | 1. 금융 서비스 전용 아이콘 종류 추가 2. 상태(성공/실패/주의) 아이콘 종류 추가 |

### 구현 내용

| 요구 | 구현 |
|---|---|
| 1. 금융 서비스 아이콘 | 24종: 대출·예금·적금·이체·계좌·은행·카드·신용점수·금리·한도조회·상환·대환·오토담보·주택담보·계산기·서류제출·전자약정·본인인증·보안·OTP·납입일·고객센터·공지·혜택 |
| 2. 상태 아이콘 | 성공·실패·주의·안내·진행중 × 선형(목록·문구 옆) / 채움형(결과 화면·Alert 상단) = 10종 |
| 일관성 | SVG 스프라이트 1개 파일, 24 그리드·선 굵기 1.75 통일, 색은 `currentColor` → 화면별 PNG 제작 금지 |
| 사용성 | 크기 6단계(16~64px), 톤(primary/success/error/warning/info/muted), 메뉴 타일, 상태 문구 |
| 접근성 | 장식 아이콘 `aria-hidden`, 의미 아이콘 `role="img"`+`aria-label`, 상태는 색+모양+글자 함께 표현, 글자 대비 4.5:1 |

### 사용법

```html
<link rel="stylesheet" href="/css/common/ui-tokens.css">
<link rel="stylesheet" href="/css/common/ui-icon.css">
<script src="/js/common/ui-icon.js"></script>   <!-- data-ui-icon / UiIcon.* 사용 시 -->

<!-- JS 없이 -->
<svg class="ui-icon ui-icon--lg ui-icon--primary" aria-hidden="true" focusable="false">
  <use href="/images/common/ui-icons.svg#ic-loan"></use>
</svg>

<!-- 선언형 -->
<i data-ui-icon="repayment" data-size="lg" data-tone="primary"></i>
<i data-ui-icon="status-success-fill" data-size="xl" data-tone="success" data-label="성공"></i>

<!-- 메뉴 타일 / 상태 문구 -->
<a href="/loan/limit" class="ui-icon-tile"><span class="ui-icon-tile__bg"><i data-ui-icon="limit-inquiry"></i></span>한도조회</a>
<span class="ui-status ui-status--warning"><i data-ui-icon="status-warning"></i>서류 보완 필요</span>
```

```javascript
el.appendChild(UiIcon.create('auto-loan', { size: 'xl', tone: 'primary', label: '오토담보대출' }));
el.appendChild(UiIcon.status('success', '승인 완료'));
UiIcon.render(container);     // 동적으로 추가한 data-ui-icon 변환
UiIcon.inline();              // 외부 스프라이트 참조가 막힌 WebView 에서 1회
```

### 아이콘 추가 절차

1. `ui-icons.svg` 에 `symbol id="ic-영문명" viewBox="0 0 24 24"` + `title`(한글명) 추가 (선 굵기 1.75, `currentColor`)
2. `ui-icon.js` 의 `CATALOG` 에 `{ name, ko, group }` 추가
3. `UiComponentTest` 실행 → 스프라이트·CATALOG 불일치, viewBox, title, 스크립트 포함 여부를 검사

### 테스트

- 샘플: `http://localhost:8080/common/ui/icon` → APP 메뉴/신청현황 아이콘 적용 전·후 비교, 카탈로그(검색·크기·색), 사용 코드 생성
- 단위 테스트: `UiComponentTest.icon*` (스프라이트 ⇄ CATALOG 일치, 요구 아이콘 존재, 스프라이트 품질, 상태 색상 대비)

---

## 3. Alert (공통#42)

### 요구사항

| 항목 | 내용 |
|---|---|
| 목적 | 사용자에게 주의를 주거나 알림을 주기 위해 사용 |
| 기능설명 | 1Btn, 2Btn 구성 |
| 요구사항 | 1. 알럿 내용이 4~5줄 이상일 경우를 고려하여 가독성 높이는 폰트 개선 2. 다양한 화면 크기 및 해상도에서도 UI가 일관된 사용성을 제공하도록 반응형 레이아웃 개선 |

### 구현 내용

| 요구 | 구현 |
|---|---|
| 1Btn / 2Btn | `UiAlert.alert()` → `Promise<void>`, `UiAlert.confirm()` → `Promise<boolean>`. 위험 작업(`danger`)은 빨간 확인 버튼 + 기본 포커스 [취소] |
| 1. 가독성 | 본문 16px(1rem)·줄간격 1.65·진한 글자(#111827, 대비 17:1)·단어 단위 줄바꿈. **3줄 이상이면 왼쪽 정렬로 자동 전환** (여러 줄 가운데 정렬은 줄 시작점이 매번 달라 읽기 어려움) |
| | 메시지 규칙: `\n` 줄바꿈, `\n\n` 문단 간격, `- `/`· ` 목록(들여쓰기), `※` 보조 안내(작은 회색 글자) |
| | 화면보다 길면 **본문만 스크롤** (제목·버튼 고정), 위/아래 그림자로 "더 있음" 표시, 키보드로도 스크롤 가능 |
| 2. 반응형 | 폭: 모바일 = 화면-40px(최대 360) / 600px↑ 400 / 1024px↑ 420. 높이 `100dvh` 기준(모바일 주소창 변화 대응) |
| | 가로모드·낮은 화면(≤480px) 여백·아이콘 축소, 폴드 접힘(≤280px) 버튼 세로, 노치(safe-area) 여백 |
| | 2Btn 문구가 두 줄로 접히면 **버튼 세로 배치 자동 전환** (주 버튼 위), rem 단위 → 200% 글자 확대에도 잘림 없음 |
| 접근성 | `role="alertdialog"`, `aria-modal`, 제목/본문 연결, Tab 포커스 가두기, Esc(=취소), 배경 `inert` + 스크롤 잠금, 닫으면 원래 위치로 포커스 복귀 |
| 운영 | 연속 호출 시 **큐**로 하나씩 표시, `onOk` 업무 실행 중 알럿 유지(실패 시 유지), HTML 미지원(textContent) → XSS 안전, CSS 자동 로드 |

> 상태 아이콘은 공통#41 스프라이트(`ic-status-*-fill`)를 사용한다. `type: success | error | warning | info`

### 사용법

```html
<script src="/js/common/ui-alert.js"></script>   <!-- CSS 3종(tokens/button/alert)은 없으면 자동 로드 -->
```

```javascript
UiAlert.alert('비밀번호가 변경되었습니다.');
UiAlert.alert('입력하신 정보가 통신사 정보와 일치하지 않습니다.\n다시 확인해 주세요.', { type: 'error', title: '본인인증 실패' });

UiAlert.confirm('작성 중인 신청서가 있습니다.\n이어서 작성하시겠습니까?', { okText: '이어하기', cancelText: '새로 작성' })
    .then(function (ok) { if (ok) { ... } });

UiAlert.confirm('해지 후에는 되돌릴 수 없습니다.', { title: '예금 해지', type: 'warning', okText: '해지하기', danger: true });

// 확인 버튼에서 업무 실행 (끝날 때까지 알럿 유지 + "제출 중", 실패 시 알럿 유지 + onError)
UiAlert.confirm('신청서를 제출할까요?', {
    okText: '제출하기', busyText: '제출 중',
    onOk: function () { return fetch('/api/loan/apply', { method: 'POST' }); },
    onError: function (e) { ... }
});

// common.js 를 쓰는 기존 화면: Common.ajax 오류 알럿도 ui-alert.js 를 포함하면 공통 Alert 로 바뀐다
Common.alert('메시지').then(function () { ... });       // ui-alert.js 미포함 화면은 브라우저 기본 alert (기존과 동일)
Common.confirm('메시지').then(function (ok) { ... });
```

| 옵션 | 설명 |
|---|---|
| `title` | 제목 (없으면 화면에서 숨기고 스크린리더에만 "알림/확인") |
| `type` | `success` / `error` / `warning` / `info` → 상단 상태 아이콘 |
| `okText`, `cancelText` | 버튼 문구 (기본 확인 / 취소) |
| `danger` | 위험 작업: 빨간 확인 버튼, 기본 포커스 [취소] |
| `onOk`, `busyText`, `onError` | 확인 버튼 업무 실행 |
| `escClose`(기본 true), `dimClose`(기본 false) | Esc / 배경 눌러 닫기 |
| `stack`, `focus` | 버튼 세로 강제, 기본 포커스(`ok`/`cancel`) |

> `window.alert()` 는 동기 함수라 그대로 바꿔치기하지 않는다. 기존 화면은 `alert(msg); next();` 를
> `Common.alert(msg).then(next);` 로 바꾸면서 순차 전환한다 (현재 JSP 내 `alert(` 34건).

### 테스트

- 샘플: `http://localhost:8080/common/ui/alert`
  - 프리셋 7종 (짧은 문구 / 5줄 이상 / 화면보다 긴 내용 / 확인창 / 위험 작업 / 긴 버튼 문구 / 오류) → 열릴 때 줄 수·왼쪽정렬·버튼세로·본문스크롤 판단 결과 로그
  - **화면 크기별 미리보기**: 폴드 280 / Android 360 / iPhone 390 / 가로 844×390 / 태블릿 768 / PC 1280 실제 크기 iframe
  - 연속 3건(큐), 확인 시 업무 실행, 업무 실패 시 유지, AS-IS 브라우저 alert 비교
- 단위 테스트: `UiComponentTest.alert*`, `commonJsAlertHook` (프리셋 허용 목록, 가독성·반응형 CSS 계약, 접근성·큐 JS 계약, common.js 연동)
