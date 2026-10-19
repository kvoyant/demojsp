# [CF-BC-20 / 공통#28] 공지사항 가이드

요구사항정의서(공통) 28번 **공지사항** (REQ-AP-609 APP · REQ-AP-610 ADMIN 통합)
목적: 공지 유형 확대에 대비해 공지사항을 정비한다. 내·외부 시스템 작업, 대고객 안내사항을 게시한다.

| 구분 | 요구사항 | 구현 |
|---|---|---|
| APP 1 | 전체 / 상품별 / 업무별 공지 구분 노출 | `NoticeList` - [전체 · 상품별 · 업무별] 탭 + 상품·업무 칩, 각 공지에 "상품 · 상품명" / "업무 · 업무명" 표시 |
| APP 2 | 메인화면 **띠 형태로 최신공지 제목** 노출 | `NoticeStrip` - 게시 중 + "메인 띠" 체크 공지 중 게시 시작이 가장 최근인 1건 |
| APP 3 | 띠 클릭 시 **공지 상세**로 이동 | 띠 → `common.notice.detail-url?id=` → `NoticeDetail` (조회수 +1) |
| APP 4 | **이어하기 지면과 겹치거나 혼동되지 않게** 배치 | 띠: 헤더 바로 아래 · 회색 한 줄 · "공지" 표시 · 버튼 없음 / 이어하기: 배너 아래 흰 카드 (샘플 참고) |
| ADMIN 1 | 전체/상품별/업무별 **구분하여 등록** 설정기능 | 구분(ALL/PRODUCT/BIZ) + 대상 선택(상품 원장 / 업무 구분), 서버에서 구분-대상 일치 검증 |
| ADMIN 2 | 공지 유형 AS-IS 업무공지·서비스공지·시스템점검·기타, **TO-BE 검토필요** | 유형을 DB 코드(`TB_CO_NOTICE_TYPE`)로 → TO-BE 확정 시 [공지 유형] 탭에서 추가·이름 변경·미사용 (코드 수정 없음) |
| ADMIN 3 | 공지사항 배너 팝업 **불필요한 아이콘 삭제 · 버튼 구조 변경** (신규 UI) | `NoticePopup` - 아이콘·좌우 화살표 없음, 하단 한 줄 [오늘 하루 보지 않기 \| 닫기], 내용 아래 [자세히 보기] |

> 9/30 비고 "간편모드에서 배너 및 메뉴 위치 조정, 공지사항 표시 영역 변경"은 UI/UX 확정 전이라 **확인 필요**.
> 컴포넌트(`NoticeStrip`)는 같고 놓는 위치만 바뀌므로, 확정되면 간편모드 화면에서 `<div id="noticeStrip">` 위치만 옮긴다.

## 1. 테이블 (`src/main/resources/schema-common-notice.sql`)

| 테이블 | 주요 컬럼 | 비고 |
|---|---|---|
| `TB_CO_NOTICE` | NOTICE_ID(자동) · **SCOPE_CD**(ALL/PRODUCT/BIZ) · **TARGET_CD** · TYPE_CD · TITLE(100) · CONTENT · START_AT · END_AT(NULL=계속) · FIX_YN · **STRIP_YN** · **POPUP_YN** · USE_YN · VIEW_CNT | 상품별 TARGET = `TB_PRODUCT.PRODUCT_CD`, 업무별 TARGET = `TB_CO_NOTICE_BIZ.BIZ_CD` |
| `TB_CO_NOTICE_TYPE` | TYPE_CD · TYPE_NM · BADGE_COLOR · SORT_ORD · USE_YN | AS-IS 4종 샘플, 미사용 유형은 새 공지에 선택 불가(기존 공지는 그대로 표시) |
| `TB_CO_NOTICE_BIZ` | BIZ_CD · BIZ_NM · SORT_ORD · USE_YN | 업무별 공지의 업무 목록 (대출신청 · 예금·적금 · 인증·보안 · 이체) |

**게시 중** = `USE_YN='Y' AND START_AT <= 지금 AND (END_AT IS NULL OR END_AT > 지금)` - 사용자 화면은 게시 중 공지만 다룬다.
예약 게시(시작일시를 미래로)·자동 종료(종료일시)는 별도 배치 없이 이 조건으로 처리된다.

샘플 `data-common-notice.sql` (유형 테이블이 비어 있을 때 1회): 공지 8건 = 게시중 5 (전체 2 · 상품별 2 · 업무별 1) + 게시종료 1 + 게시예약 1 + 미사용 1.

## 2. 노출 규칙 (`NoticeServiceImpl`)

| 화면 | 대상 | 정렬 |
|---|---|---|
| 메인 띠 | 게시 중 · `STRIP_YN='Y'` | 게시 시작 최신 1건 (없으면 띠 영역 숨김) |
| 메인 팝업 | 게시 중 · `POPUP_YN='Y'` (오늘 하루 보지 않기 한 공지 제외) | 상단고정 → 최신, 여러 건이면 닫을 때마다 다음 |
| 목록 [전체] | 게시 중 모든 공지 | 상단고정(FIX_YN) → 게시 시작 최신 |
| 목록 [상품별] / [업무별] | 해당 구분 공지 (칩 선택 시 그 상품/업무만) | 같음 |
| 상세 | 게시 중인 공지만 (종료·예약·미사용 → NTC-002) | - |

## 3. 소스 구성 (`com.example.demojsp.common.notice`)

```
controller/  NoticeApiController        /api/common/notice/** (띠 · 목록 · 필터 · 상세 · 팝업)
             NoticeAdminApiController   /api/common/notice-admin/** (공지 · 유형 · 업무 구분 · 대상)
             NoticeAdminAuth            ADMIN 권한 (ROLE_ADMIN 또는 local admin-api-open)
             NoticeSampleController     /common/notice/sample · list · detail · admin
service/     NoticeService(Impl)        사용자 화면 조회 (게시 중 규칙)
             NoticeAdminService(Impl)   등록·수정·검증, 상태(게시중/게시예약/게시종료/미사용)
mapper/      NoticeMapper               + resources/mapper/common/NoticeMapper.xml
dto/         NoticeDto, NoticeTypeDto, NoticeBizDto, NoticeTargetDto, NoticePage, NoticeFilters, NoticeStripView
exception/   NoticeErrorCode(NTC-xxx), NoticeException, NoticeExceptionHandler
config/      NoticeProperties(common.notice.*), NoticeSeeder

static/js/common/notice.js     NoticeStrip · NoticeList · NoticeDetail · NoticePopup
static/css/common/notice.css
WEB-INF/views/common/notice/sample.jsp   APP 메인 배치 (띠 · 이어하기 · 팝업)
WEB-INF/views/common/notice/list.jsp     공지 목록 (?scope= &target= 으로 시작 탭 지정)
WEB-INF/views/common/notice/detail.jsp   공지 상세 (?id=)
WEB-INF/views/common/notice/admin.jsp    디지털어드민 공지사항 관리
```

## 4. 업무 화면 적용

```html
<link rel="stylesheet" href="/css/common/ui-tokens.css">
<link rel="stylesheet" href="/css/common/notice.css">
<script src="/js/common/notice.js"></script>

<!-- APP 메인(홈): 헤더 바로 아래 -->
<div id="noticeStrip"></div>
<script>
NoticeStrip.mount('#noticeStrip');      // 공지가 없거나 조회 실패면 영역째 숨김 (빈 자리 없음)
NoticePopup.open();                     // 메인 진입 시 1회
</script>

<!-- 공지 목록 화면 -->
<div id="noticeList"></div>
<script>NoticeList.mount('#noticeList');</script>

<!-- 상품 안내 화면 안에서 그 상품 공지만 -->
<script>NoticeList.mount('#prdNotice', { scope: 'PRODUCT', target: 'PS_SMART' });</script>

<!-- 공지 상세 화면 (?id=) -->
<div id="noticeDetail"></div>
<script>NoticeDetail.mount('#noticeDetail');</script>
```
- 모든 컴포넌트는 `onNavigate(url, info)` 로 이동을 가로챌 수 있다 (APP 네이티브 화면 전환).
- 상세 화면 주소가 다르면 서버 `common.notice.detail-url` 과 화면 `Notice.setDetailUrl('/app/notice/view')` 를 함께 바꾼다.
- 공지 내용은 `textContent` 로만 출력한다 (HTML 해석 안 함 → XSS 안전, 줄바꿈은 CSS 로 유지).
- 기존 배너 팝업(공통#39, `banner-popup.js`)은 바꾸지 않았다. 공지 팝업은 공지 테이블을 쓰는 별도 컴포넌트다.

## 5. API

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/common/notice/strip` | `{notice, detailUrl}` - notice 가 null 이면 띠 숨김 |
| GET | `/api/common/notice?scope=&target=&type=&page=` | 목록 `{items, total, page, size, hasNext}` (scope 없음 = 전체 탭) |
| GET | `/api/common/notice/filters` | `{allCnt, products[], bizs[]}` 탭 건수·칩 |
| GET | `/api/common/notice/{id}` | 상세 (+조회수) |
| GET | `/api/common/notice/popups` | 메인 팝업 대상 (내용 포함) |
| GET / POST | `/api/common/notice-admin/notices` | 목록(상태 포함) / 등록·수정 |
| GET | `/api/common/notice-admin/notices/{id}` | 수정 폼 |
| GET / POST | `/api/common/notice-admin/types` | 공지 유형 |
| GET / POST | `/api/common/notice-admin/bizs` | 업무 구분 |
| GET | `/api/common/notice-admin/targets` | 대상 선택 상자 (상품 · 업무) |

오류코드: `NTC-001` 요청 오류(구분 값 등), `002` 게시 중인 공지 아님, `101` 입력값 오류(대상 누락·기간·길이·미사용 유형 등), `401` ADMIN 권한 없음.

## 6. 설정 / 테스트

```yaml
common:
  notice:
    page-size: 10                       # 목록 한 페이지 ([더보기])
    detail-url: /common/notice/detail   # 띠 → 상세 (APP 화면 주소로 교체)
    admin-api-open: true                # local 전용! 운영 false
```

```
APP 메인 (띠·팝업)  http://localhost:8080/common/notice/sample
공지 목록           http://localhost:8080/common/notice/list
어드민              http://localhost:8080/common/notice/admin
./mvnw test -Dtest=NoticeDbTest     # 6건 - 실제 DDL·샘플·매퍼를 H2(MySQL 모드)에서 실행
```

## 7. 확인 필요

1. 공지 유형 TO-BE (요구사항 원문 "검토필요") → 확정되면 ADMIN [공지 유형] 탭에서 등록.
2. 간편모드 공지 표시 영역 (9/30 비고) → UI/UX 확정 후 띠 위치만 조정.
3. 공지 팝업 신규 UI 의 최종 디자인 (현재: 아이콘 없음 + 하단 2버튼) → Figma 확정 시 `notice.css` 만 교체.
4. 기존 운영 공지 데이터 이관 시 AS-IS 공지 유형 → `TYPE_CD` 매핑, 구분은 기본 ALL.
