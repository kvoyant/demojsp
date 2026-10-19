# [CF-BC-20 / 공통#26] 메인화면 상품 탭 가이드

요구사항정의서(공통) 26번 **메인화면 상품 탭** (REQ-AP-606 · 607 통합)
디지털페퍼 앱/PC Web 에서 비대면으로 신청·가입할 수 있는 상품을 안내하는 탭이다.

| 세부항목 | 요구사항 | 구현 |
|---|---|---|
| 1. APP / PC Web | 1) 상단 슬라이드 카드 영역 **신규 추가** | `TB_PRD_TAB_SLIDE` + `product-tab.js` 카드 영역 (좌우로 넘김, PC 는 ‹ › 버튼) |
| | 2) 기존 불필요 카드 **삭제** (예: 시세조회) | 섹션 `TB_PRD_TAB_SECTION.USE_YN='N'` - 화면 코드 수정 없이 ADMIN 에서 숨김 (샘플: `AUTO_PRICE` 내 차 시세조회) |
| | 3) 디자인·콘텐츠 형식 수정 | 카드(문구 30자/서브 40자/배지/배경색) · 섹션(제목+설명) · 상품행(아이콘·유형·상품명·한줄설명·금리·배지) 공통 형식 |
| 2. ADMIN | 하단 상품 목록 정비 - **ADMIN 컨트롤** | `/common/product-tab/admin` : 섹션 순서·사용, 섹션별 상품 순서·배지·사용, 슬라이드 카드 관리, 기준일 미리보기 |

## 1. 화면 구조와 데이터 위치

```
┌ 상품 탭 ───────────────────────────────┐
│ ① 상단 슬라이드 카드   ← TB_PRD_TAB_SLIDE    (게시기간 · 채널 · 표시순서 · 사용여부)
│                          └ 연결 상품 → TB_PRODUCT (상품안내 URL, 판매중지면 카드 자동 숨김)
│ ② 섹션 "예금·적금"     ← TB_PRD_TAB_SECTION  (PRODUCT 유형)
│     └ 상품행 ×N        ← TB_PRD_TAB_ITEM     (어떤 상품을 몇 번째로, 배지)
│                          └ 상품 정보 → TB_PRODUCT (이름·설명·금리·아이콘·URL·판매여부)
│ ② 섹션 "대출"          ← (PRODUCT 유형)
│ ② 섹션 "금융계산기"    ← (CARD 유형: 아이콘 + 제목 + 설명 → 이동)
│ ✕ 섹션 "내 차 시세조회" ← USE_YN='N' → 화면에 없음 (요구사항 2)
└────────────────────────────────────────┘
```

**상품 정보는 상품 원장 하나에서만 관리한다.**
상품 탭 테이블에는 "어떤 상품을 어디에 몇 번째로" 만 있고, 이름·금리·URL·판매여부는 `TB_PRODUCT`(공통#47·48 상품설정)를 조인해서 읽는다.
→ 상품설정에서 금리를 바꾸거나 판매중지하면 홈 추천상품(공통#47)과 상품 탭에 **동시에** 반영된다.

## 2. 테이블 (`src/main/resources/schema-common-producttab.sql`)

| 테이블 | 주요 컬럼 | 비고 |
|---|---|---|
| `TB_PRD_TAB_SLIDE` | SLIDE_ID(자동) · TITLE(30) · SUB_TEXT(40) · BADGE_TEXT(10) · BG_COLOR · IMAGE_URL/ALT · PRODUCT_CD · LINK_URL · CHANNELS · START_DT~END_DT(포함) · SORT_ORD · USE_YN | 상품 연결 시 LINK_URL 대신 상품안내 URL 사용 |
| `TB_PRD_TAB_SECTION` | SECTION_CD(PK) · SECTION_NM · SECTION_DESC · SECTION_TYPE(PRODUCT/CARD) · CARD_ICON · LINK_URL · SORT_ORD · USE_YN | CARD 는 아이콘·이동 URL 필수 |
| `TB_PRD_TAB_ITEM` | PK(SECTION_CD, PRODUCT_CD) · SORT_ORD · BADGE_TEXT · USE_YN | PRODUCT 섹션에만 |

샘플 데이터 `data-common-producttab.sql` (TB_PRD_TAB_SECTION 이 비어 있을 때 1회): 카드 6장(게시중 4·게시종료 1·게시예정 1, 그중 1장은 판매중지 상품 연결), 섹션 4개(시세조회는 미사용), 상품 6건.

## 3. 사용자 화면 노출 규칙 (`ProductTabServiceImpl`)

| 대상 | 노출 조건 | 순서 |
|---|---|---|
| 슬라이드 카드 | `USE_YN='Y'` · `START_DT <= 오늘 <= END_DT` · 채널 포함 · (연결 상품이 있으면) 판매중 · 최대 `max-slides` 장 | SORT_ORD, SLIDE_ID |
| 섹션 | `USE_YN='Y'` · PRODUCT 유형은 보여줄 상품이 1건 이상 (없으면 제목도 숨김) | SORT_ORD |
| 섹션 상품 | 항목 `USE_YN='Y'` · 상품 `ON_SALE_YN='Y'` | SORT_ORD |

- 카드 글자색은 서버가 배경색 대비로 계산해서 내려준다 (`textColor`, 공통#45 와 같은 WCAG 4.5:1 규칙) → ADMIN 이 어떤 색을 골라도 읽힌다.
- 자동 넘김은 하지 않는다 (움직이는 콘텐츠는 고객이 직접 넘기게 - 접근성).
- APP(좁은 영역)은 카드 1장·상품 1열, PC Web(영역 폭 600px 이상)은 카드 2장·상품 2열. 화면 폭이 아니라 **컴포넌트가 놓인 영역 폭** 기준(container query).

## 4. 소스 구성 (`com.example.demojsp.common.producttab`)

```
controller/  ProductTabApiController        GET /api/common/product-tab?channel=APP|PC_WEB
             ProductTabAdminApiController   /api/common/product-tab-admin/** (슬라이드·섹션·상품·미리보기)
             ProductTabAdminAuth            ADMIN 권한 (ROLE_ADMIN 또는 local admin-api-open)
             ProductTabSampleController     /common/product-tab/sample, /common/product-tab/admin
service/     ProductTabService(Impl)        사용자 화면 조회 (노출 규칙)
             ProductTabAdminService(Impl)   등록·수정·검증
mapper/      ProductTabMapper               + resources/mapper/common/ProductTabMapper.xml
dto/         PtSlideDto, PtSectionDto, PtItemDto, ProductTabView, PtProductDto
exception/   ProductTabErrorCode(PTB-xxx), ProductTabException, ProductTabExceptionHandler
config/      ProductTabProperties(common.product-tab.*), ProductTabSeeder

static/js/common/product-tab.js     화면 공통 컴포넌트 (APP / PC Web 공용)
static/css/common/product-tab.css
WEB-INF/views/common/producttab/sample.jsp   APP 목업(하단 탭 '상품') + PC Web 목업
WEB-INF/views/common/producttab/admin.jsp    디지털어드민 상품 탭 관리
```

## 5. 업무 화면 적용

```html
<link rel="stylesheet" href="/css/common/ui-tokens.css">
<link rel="stylesheet" href="/css/common/ui-icon.css">
<link rel="stylesheet" href="/css/common/product-tab.css">
<script src="/js/common/product-tab.js"></script>

<div id="productTab"></div>
<script>
ProductTab.mount('#productTab', {
    channel: 'APP',                                    // PC Web 은 'PC_WEB'
    onNavigate: function (url, info) {                 // 생략하면 location.href 이동
        // APP: 네이티브 브릿지로 화면 전환. info = {type:'SLIDE'|'PRODUCT'|'CARD', productCd, section}
        location.href = url;
    }
});
</script>
```
하단 탭에 '상품'을 넣을 때는 `BottomNav.mount('#bottomNav', { active: 'PRODUCT', tabs: [...] })` (샘플 참고).

## 6. API

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/common/product-tab?channel=APP` | `{slides[], sections[ {sectionType, items[]} ]}` |
| GET / POST | `/api/common/product-tab-admin/slides` | 슬라이드 목록 / 등록(slideId 없음)·수정 |
| GET / POST | `/api/common/product-tab-admin/sections` | 섹션 목록(연결 상품 수) / 등록·수정 |
| GET / PUT | `/api/common/product-tab-admin/sections/{cd}/items` | 섹션 상품 목록 / 통째 저장 |
| GET | `/api/common/product-tab-admin/products` | 상품 원장 (선택 상자) |
| GET | `/api/common/product-tab-admin/preview?channel=&baseDt=` | 기준일 사용자 화면 미리보기 |

오류코드: `PTB-001` 요청 오류(채널 등), `002` 슬라이드 없음, `003` 섹션 없음, `101` 입력값 오류(길이·URL·색상·기간·중복 상품 등), `401` ADMIN 권한 없음.

ADMIN 검증 규칙: 문구 30/40자, 배지 10자, 배경색 `#RRGGBB`, URL 은 `/경로` 또는 `https://` 만(`javascript:` 차단),
이미지가 있으면 대체텍스트 필수, 상품 연결 또는 이동 URL 중 하나 필수, 종료일 ≥ 시작일, 순서 1~99,
서비스 카드는 이동 URL 필수·상품 연결 불가, 한 섹션에 같은 상품 중복 불가. **검증을 모두 통과해야 저장**(하나라도 틀리면 기존 목록 유지).

## 7. 설정 / 테스트

```yaml
common:
  product-tab:
    max-slides: 5          # 상단 카드 최대 노출 수 (0 = 제한 없음)
    admin-api-open: true   # local 전용! 운영 false
```

```
사용자 화면  http://localhost:8080/common/product-tab/sample
어드민       http://localhost:8080/common/product-tab/admin
./mvnw test -Dtest=ProductTabDbTest    # 6건 - 실제 DDL·샘플·매퍼를 H2(MySQL 모드)에서 실행
```

## 8. 확인 필요 / 운영 반영

- AS-IS 상품 탭의 실제 카드 목록(시세조회 외 삭제 대상)과 섹션 구성은 업무 확인 후 ADMIN 에 등록.
- 운영: DDL 은 DBA 반영(`schema-common-producttab.sql`), `seed-if-empty: false`, `admin-api-open: false`.
- 이미지 업로드는 공통#45 배너 이미지 업로드(`BannerImageStore`)와 같은 방식으로 붙일 수 있다 (현재는 URL 입력).
