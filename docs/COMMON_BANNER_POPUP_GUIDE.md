# [CF-BC-20 / 공통#39] 배너팝업 복수 노출 가이드 (demojsp)

요구사항정의서(공통) No.39 "배너팝업" — 게시일자가 겹치는 배너팝업을 **표시순서대로 모두** 노출하고 **좌우 슬라이드**로 넘겨 확인한다.
적용 채널: APP 메인 (ADMIN 배너팝업목록 연계). 분석ID RA-034, 공통기능 CF-BC-20(콘텐츠·게시 공통) / CF-UI-01(공통 컴포넌트).

| 구분 | 내용 |
|---|---|
| AS-IS | 디지털어드민에서 같은 시간대에 여러 건 등록 + 표시순서 지정해도 APP 에 **1건만** 노출 → 일부 긴급공지 누락 |
| TO-BE | 기간이 겹치는 배너 2건 이상 → 표시순서 정렬 후 전부 노출, 좌우 슬라이드(스와이프·화살표·인디케이터) |

## 1. 요구사항 매핑

| 요구사항 | 반영 위치 |
|---|---|
| 1. 게시일자가 겹치는 배너 2건 이상일 때 표시순서 적용 | `BannerPopupService.displayable()` 전부 조회 → `BannerPopup.DISPLAY_ORDER` 정렬 |
| 2. 좌우 슬라이드로 여러 건을 넘겨 확인 | `/js/common/banner-popup.js` (스와이프·◀▶·키보드 ←/→, "1 / 3" + 점 인디케이터) |
| 일반 긴급공지·시스템 긴급공지 등 ADMIN 등록 후 앱 노출 | `BannerCategory`, ADMIN API `/api/common/banner-admin/**` |
| (ADMIN 표시순서 기능 기구현) | ADMIN 목록에 **겹침 건수**, **표시순서 중복 경고** 추가 → 등록자가 슬라이드 순서를 바로 확인 |

### 정렬·노출 규칙

1. 노출 대상: `사용여부=Y` + 채널 일치 + `게시시작 <= 현재 < 게시종료` (종료 시각 미포함)
2. 정렬: 표시순서 오름차순 → (같으면) 게시 시작일시 최신 우선 → 배너 ID
3. 1건이면 기존과 같은 단건 팝업(화살표·인디케이터 없음), 2건 이상이면 `slide=true` 로 슬라이드
4. 자동 넘김 없음 (요구사항: 고객이 좌우로 넘겨 확인)
5. 오늘 하루 보지 않기: 그때 노출된 배너 ID 를 기기(localStorage)에 당일만 저장 → 같은 날 **새로 등록된 배너는 다시 노출**
6. 최대 노출 건수 `common.banner-popup.max-display-count` (기본 10, 0=무제한) - 요구사항 미정, 수행사 제안값

## 2. 패키지 구조

```
common/banner
├─ config/     BannerPopupProperties(common.banner-popup.* 바인딩, seed), BannerPopupConfig
├─ domain/     BannerPopup(원장 1건, DISPLAY_ORDER), BannerPopupType, BannerCategory, BannerChannel,
│              BannerPopupRequest, BannerPopupView(고객용), BannerPopupAdminView, BannerDisplayList
├─ store/      BannerPopupRepository + InMemoryBannerPopupRepository (운영: MyBatis 구현체로 교체)
├─ service/    BannerPopupService(노출 조회·ADMIN·서버 검증), BannerSupport(일시·URL)
├─ exception/  BannerErrorCode(BNR-xxx), BannerException
├─ web/        BannerPopupApiController(고객), BannerPopupAdminApiController(ADMIN),
│              BannerRequestContext, BannerApiExceptionHandler
└─ sample/     BannerPopupSampleController (업무 적용 예시)

static/js/common/banner-popup.js        화면 공통 컴포넌트 (jQuery/Bootstrap 비의존)
WEB-INF/views/common/banner/sample.jsp  APP 메인 목업 + ADMIN 배너팝업목록 테스트 화면
```

기존 코드 변경: `application.yaml`(`common.banner-popup` 블록, `common.test-hub.items` 에 CF-BC-20 추가)만. `WebMvcConfig` 무수정 — 신규 경로는 로그인 인터셉터 대상이 아니다.

## 3. API

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/common/banner-popups?channel=APP` | 고객 노출 대상 (서버 시각 기준, 로그인 불필요) |
| GET | `/api/common/banner-admin/codes` | [ADMIN] 배너팝업구분/공지유형/채널 코드 |
| GET | `/api/common/banner-admin/popups` | [ADMIN] 목록 (게시상태, overlapCount, orderConflict) |
| GET | `/api/common/banner-admin/popups/{id}` | [ADMIN] 상세 |
| POST | `/api/common/banner-admin/popups` | [ADMIN] 등록 |
| PUT | `/api/common/banner-admin/popups/{id}` | [ADMIN] 수정 |
| POST | `/api/common/banner-admin/popups/{id}/use?use=false` | [ADMIN] 사용여부 변경 |
| DELETE | `/api/common/banner-admin/popups/{id}` | [ADMIN] 삭제 |
| GET | `/api/common/banner-admin/preview?channel=APP&at=2026-10-20 10:00` | [ADMIN] 기준시각 노출 미리보기 |

고객 응답 예:
```json
{ "success": true, "data": { "channel": "APP", "baseAt": "2026-10-20 10:00:00", "total": 3, "slide": true, "truncated": 0,
  "items": [ { "id": 1002, "category": "EMERGENCY_SYSTEM", "categoryName": "시스템 긴급공지", "title": "...", "content": "...",
               "imageUrl": null, "altText": null, "linkUrl": null, "displayOrder": 1, "startAt": "2026-10-20 00:00", "endAt": "2026-10-23 00:00" } ] } }
```

### ADMIN 서버 검증 (BNR-1xx)

| 코드 | 조건 |
|---|---|
| BNR-101 | 공지유형·노출채널·제목·내용·게시기간·표시순서 누락 |
| BNR-102 | 게시 종료 <= 시작 |
| BNR-103 | 표시순서 1~999 벗어남 |
| BNR-104 | 링크/이미지 URL 이 상대경로(`/...`)·http(s) 가 아님 (`javascript:`, `//host` 차단) |
| BNR-105 | 이미지가 있는데 대체텍스트 없음 (접근성 CF-UI-02) |
| BNR-106 | 제목 50자 / 내용 1,000자 / 대체텍스트 100자 초과 |
| BNR-107 | 배너팝업구분·공지유형 코드 오류 |
| BNR-401 | ADMIN 권한 없음 (`admin-api-open=false` + ROLE_ADMIN 아님) |

## 4. 업무 화면 적용

```html
<script src="/js/common/banner-popup.js"></script>
<script>
  // APP 메인 진입 시 1회
  BannerPopup.open({
    channel: 'APP',
    onLink: function (url) { /* APP: 네이티브 브릿지로 화면 이동, 기본은 location.href */ location.href = url; }
  }).then(function (r) { /* r.shown, r.viewed(넘겨 본 배너 ID), r.closedBy */ });
</script>
```

옵션: `container`(특정 영역 안에 표시), `items`(조회 없이 직접 표시 - ADMIN 미리보기), `respectHidden`(오늘 하루 보지 않기 적용 여부).
화면 문자열은 모두 `textContent` 로 출력(XSS 안전), 팝업은 `role=dialog` + 슬라이드 `aria-roledescription`, 포커스 가둠, Esc 닫기.

## 5. 운영 전환 (DB)

1. 기존 배너팝업 테이블을 조회하는 `MyBatisBannerPopupRepository implements BannerPopupRepository` 작성 후 `@Primary` 등록 → seed 는 자동 미적재
2. **노출 조회 쿼리에 `LIMIT 1` / `ROWNUM = 1` 금지** (AS-IS 1건 노출 원인 후보). 게시기간·사용여부·채널 조건만 두고 정렬은 서비스 규칙을 따른다.
   ```sql
   SELECT ... FROM TB_BANNER_POPUP
    WHERE USE_YN = 'Y' AND POPUP_TYPE = 'BANNER_POPUP'
      AND #{now} >= START_DTM AND #{now} < END_DTM
      AND CHANNEL_CD = #{channel}
    ORDER BY DISP_ORD ASC, START_DTM DESC, BANNER_ID ASC
   ```
3. 운영 yaml: `common.banner-popup.admin-api-open: false`

## 6. 확인 필요사항

| 항목 | 현재 처리 |
|---|---|
| 동시 노출 최대 건수 | 미정 → 기본 10건 (설정값) |
| 표시순서 동일 시 정렬 | 게시 시작일시 최신 우선 (등록은 허용, ADMIN 목록에 경고 아이콘) |
| "오늘 하루 보지 않기" 단위 (배너별 / 팝업 전체) | 팝업 전체(그때 노출된 배너 일괄), 당일 신규 배너는 재노출 |
| 공지유형 TO-BE (REQ-AP-609 검토필요) | 시스템 긴급공지 / 일반 긴급공지 / 일반 공지 / 이벤트·상품안내 (enum 만 조정) |
| 팝업 버튼 구조 (REQ-AP-609 "불필요한 아이콘 삭제 및 버튼 구조 변경") | [오늘 하루 보지 않기] [닫기] + 슬라이드별 [자세히 보기] - Figma 확정 시 반영 |
| 간편모드 노출 위치 (9/30 간편모드 배너 위치 조정) | 미반영 - Figma 확정본 필요 |
| 상단슬라이드배너 (공통#44·45) | `BannerPopupType.TOP_SLIDE` 추가 + 같은 저장소/서비스 재사용 예정 |

## 7. 테스트

- `BannerPopupFlowTest` (standalone MockMvc, 기준시각 고정): 겹침 3건 전부 노출·정렬, 채널 분리, 기간 경계(시작 포함/종료 미포함),
  동일 순서 정렬, ADMIN 수정·사용여부·삭제 반영, 서버 검증 13종, 최대 노출 건수, ADMIN 권한, URL 규칙
- 샘플 화면: http://localhost:8080/common/banner/sample (테스트 허브 `/common/test-hub` 에서 진입)
  - [메인 진입] 오늘 기준 게시중 3건이 표시순서 1→2→3 으로 슬라이드
  - [기준시각으로 미리보기] +3일 이후로 바꾸면 게시예정 배너가 함께 노출
  - ADMIN 폼에서 표시순서 1 로 새 배너 등록 → 메인 진입 시 맨 앞에 추가
