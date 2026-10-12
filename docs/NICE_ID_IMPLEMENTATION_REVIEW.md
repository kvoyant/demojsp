# 📱 NICE ID 휴대폰 본인인증 도입 및 공통업무 구현 설계·검토 보고서

---

## 1. 📌 개요 및 도입 목적

### 1.1 도입 배경 및 목적
* **목적**: NICE평가정보(주) 본인확인 서비스를 활용한 **휴대폰 본인인증 사용 편의성 대폭 강화** 및 **비대면 금융거래 보안성 제고**.
* **채널**: APP / PC Web 공용 (반응형 웹 UI 제공).
* **적용 업무**: 저축은행 소매금융 포털 **공통업무(Common Module)**.

### 1.2 핵심 기능
1. **통신사 미지정 방식 (성명 + 생년월일 + 휴대폰번호만 입력)**:
   * 고객이 SKT, KT, LGU+, 알뜰폰 3사 중 가입 통신사를 일일이 선택할 필요 없이, 기본 식별정보(성명, 생년월일 8자리, 전화번호)만 입력하면 통신사 망에서 자동으로 번호를 식별하여 인증을 수행합니다.
2. **기기 점유 MO(Mobile Originated) 인증 결합**:
   * 모바일 단말기(APP/모바일웹)에서 원터치 문자 전송(`sms:1600-XXXX?body=...`)을 통해 **휴대폰번호 입력 자체를 생략**할 수 있으며, 단말기 점유/유심 소지 여부를 동시에 검증하여 대포폰/명의도용을 원천 차단합니다.
3. **비여신금융거래 약관 개편**:
   * 통신사별 개별 약관 선택 체계에서 통신 3사 및 알뜰폰 통합 본인확인 약관 체계로 개편.
4. **기존 디자인 컴포넌트 100% 활용**:
   * 포털 공통 CSS(`card-header-financial`, `#0d47a1`), Bootstrap 5, `common.js` AJAX 및 공통 로딩 스피너와 일관된 UI/UX 제공.

---

## 2. ⚖️ 약관 개정 사항 (비여신금융거래 약관)

| 구분 | AS-IS (기존 방식) | TO-BE (NICE ID 도입) | 비고 |
| :--- | :--- | :--- | :--- |
| **통신사 선택** | **필수** (SKT, KT, LGU+, 알뜰폰 중 택 1) | **불필요 (선택 UI 완전 제거)** | 사용자 이탈률 대폭 감소 |
| **약관 구성** | 각 통신사별 상이한 개별 약관 노출 | **비여신금융거래 본인확인 통합약관** 일괄 제공 | 법적 효력 완비 |
| **필수 약관 4종** | • SKT/KT/LGU+ 통신사 이용약관<br>• 통신사별 개인정보 수집동의 | ① **[필수] 비여신금융거래 본인확인서비스 이용약관**<br>② **[필수] NICE평가정보(주) 본인확인서비스 이용약관**<br>③ **[필수] 통신사(SKT/KT/LGU+/MVNO) 통합 이용약관**<br>④ **[필수] 개인정보 수집·이용 및 고유식별정보 처리 동의** | 일괄 전체동의 및 개별 전문보기 지원 |

---

## 3. 🔍 핵심 검토과제 ①: 주민번호 &rarr; CI 전환에 따른 체크로직 영향도 검토

개인정보보호법 제24조의2(주민등록번호 처리의 제한) 및 금융감독당국의 가이드라인에 따라, 법령 근거 없는 주민등록번호 수집/보관이 엄격히 금지됩니다. 이에 따라 고유식별자를 주민번호에서 **88바이트 연계정보(CI, Connecting Information)**로 전환하였으며, 기존 핵심 비즈니스 체크로직에 미치는 영향도를 종합 검토·구현하였습니다.

```
┌───────────────────────────────────────────────────────────────────────────────┐
│                    NICE ID 본인인증 응답 데이터 (복호화)                     │
│  - CI (88바이트 고유 식별키)                                                  │
│  - DI (중복가입확인정보 64바이트)                                             │
│  - birthDate (YYYYMMDD 8자리)                                                 │
│  - gender (1: 남성, 2: 여성)                                                  │
│  - nationalInfo (0/1: 내국인, 2: 외국인)                                      │
└──────────────┬────────────────────────┬────────────────────────┬──────────────┘
               ▼                        ▼                        ▼
       [1. 유의고객 체크]        [2. 미성년자 체크]        [3. 외국인 체크]
       - TB_WATCHLIST 조회       - LocalDate 만 나이 계산  - nationalInfo 판별
       - CI 일치 시 즉시 차단   - 만 19세 미만 거래제한   - 외국인등록증 추가안내
```

### 3.1 유의고객 (Watchlist / FDS / 금융사고의심자 / 연체자) 체크
* **영향도 분석**:
  * **AS-IS**: 주민등록번호(13자리)로 유의고객 테이블을 조회하여 차단 여부 결정.
  * **TO-BE**: 본인인증 완료 시 발급되는 **88바이트 고유 CI(`CI_VAL`)**로 `TB_WATCHLIST` 테이블을 조회.
* **결론 및 조치 사항**:
  * CI는 기관 간, 온라인/오프라인 간 1:1로 유일하게 매핑되는 암호학적 고유 식별자이므로, **유의고객 식별 정확도가 100% 동일하게 유지**됩니다.
  * `BLOCK_YN = 'Y'`인 경우 `AUTH_WATCHLIST_BLOCKED` 예외를 즉시 발생시켜 거래를 차단하고, `TB_CERT_LOG`에 감사 추적 이력을 기록합니다.
  * **과도기 대응**: 기존 레거시 주민번호 유의고객 데이터는 9/30 이관 파이프라인에서 NICE 일괄 변환을 거쳐 CI로 변환 등록되었습니다.

### 3.2 미성년자 (만 19세 미만) 체크
* **영향도 분석**:
  * **AS-IS**: 주민번호 앞 6자리(YYMMDD)와 7번째 자리(성별/세기 코드 1~4)를 파싱하여 생년을 유추한 뒤 나이 계산.
  * **TO-BE**: NICE ID 표준 응답 파라미터로 **전체 연도 8자리 `birthDate` (예: `19900515`)**가 공식 제공됨.
* **결론 및 조치 사항**:
  * **영향도 없음 (오히려 정확도 및 안정성 대폭 향상)**.
  * 2000년대생 주민번호 뒷자리 코드(3, 4) 파싱 시 발생할 수 있는 세기 판별 오류 가능성이 완전히 제거됩니다.
  * `CustomerCheckService`에서 `LocalDate.now()` 기준 만 나이(만 19세 미만)를 정밀 계산하여, 미성년자일 경우 `isMinor = true` 및 법정대리인 동의 안내를 반환합니다.

### 3.3 외국인 고객 체크
* **영향도 분석**:
  * **AS-IS**: 주민번호 7번째 자리(5, 6: 1900년대 외국인, 7, 8: 2000년대 외국인)로 외국인 등록번호 여부 판별.
  * **TO-BE**: NICE ID 표준 응답 파라미터로 **`nationalInfo` (0 또는 1: 내국인, 2: 외국인)**가 공식 제공됨.
* **결론 및 조치 사항**:
  * **영향도 없음**.
  * `nationalInfo == '2'`인 경우 외국인 고객으로 즉시 식별되며, 외국인등록증(ARC) 또는 여권 원본 실명확인증표 진위확인 및 체류자격 심사 가이드로 분기 처리됩니다.

---

## 4. 🗄️ 핵심 검토과제 ②: 9/30 이기종 CI Data 이관 전략 및 DB 요건

### 4.1 DB 설계 요건 정의
1. **고객 CI 마스터 (`TB_CUST_CI`)**:
   * `CI_VAL VARCHAR(88) PRIMARY KEY`: NICE 표준 88바이트 Base64 고유키.
   * `DI_VAL VARCHAR(64)`: 중복가입확인정보.
   * `INDEX IDX_CUST_CI_CELL (CELL_NO)`, `INDEX IDX_CUST_CI_CUST_ID (CUST_ID)`
2. **이기종 스테이징 임시 테이블 (`TB_CI_MIGRATION_TEMP`)**:
   * 이기종 원천 DB(레거시 코어뱅킹, 구 CRM, 외부 대행사)에서 9/30 기준으로 추출된 미정제 데이터를 격리 적재.
   * `VALID_STATUS` (`PENDING`, `VALID`, `INVALID_LENGTH`, `INVALID_BASE64`, `DUPLICATE_CI`).
   * `MIGRATION_STATUS` (`READY`, `COMPLETED`, `FAILED`).
3. **이관 감사 및 통계 테이블 (`TB_CI_MIGRATION_HISTORY`)**:
   * 배치ID, 총건수, 성공, 중복제외, 오류건수, 소요시간, 실행자 감사 추적.

### 4.2 Data 이관 4단계 파이프라인
```
[이기종 소스 데이터 (9/30 추출)]
              │
              ▼
[1단계: Staging 적재 (TB_CI_MIGRATION_TEMP)]
              │
              ▼
[2단계: 정합성 사전 검증 (validateStagingRecords)]
   ├─ 길이 검증: length == 88 아니면 INVALID_LENGTH
   ├─ 형식 검증: Base64 표준 정규식 아니면 INVALID_BASE64
   └─ 중복 검증: 원천 내 중복 또는 TB_CUST_CI 기등록 시 DUPLICATE_CI
              │
              ▼
[3단계: 벌크 이관 (executeMigration)]
   └─ VALID & READY 건만 TB_CUST_CI로 Upsert
              │
              ▼
[4단계: 결과 리포팅 및 감사 기록 (TB_CI_MIGRATION_HISTORY)]
```

---

## 5. 🛠️ 구현된 아키텍처 및 소스 파일 명세

```text
src/main/
├── java/com/example/demojsp/
│   ├── common/
│   │   ├── exception/ErrorCode.java        # 본인인증, 약관, 유의고객, CI이관 에러코드 추가
│   │   └── util/MaskingUtil.java           # CI 88자리 마스킹 (maskCi) 추가
│   └── domain/auth/                        # 공통 본인인증 & CI 관리 도메인
│       ├── controller/
│       │   ├── NiceCertController.java     # JSP 화면 매핑 (/common/auth/nice-cert, ci-migration)
│       │   └── NiceCertApiController.java  # REST API (/api/common/auth/nice/*, ci-migration/*)
│       ├── dto/
│       │   ├── NiceCertReqDto.java         # 통신사 미지정 인증 요청 DTO
│       │   ├── NiceCertVerifyDto.java      # 인증번호 OTP 검증 DTO
│       │   ├── NiceCertMoReqDto.java       # MO 기기점유 인증 요청 DTO
│       │   ├── NiceCertResultDto.java      # 인증 완료 및 리스크 체크 결과 DTO
│       │   ├── CustomerRiskCheckDto.java   # 유의고객/미성년자/외국인 종합 검증 DTO
│       │   ├── TermsDto.java               # 비여신금융거래 약관 DTO
│       │   └── CiMigrationDto.java         # 9/30 이기종 CI 이관 DTO
│       ├── mapper/
│       │   ├── NiceAuthMapper.java         # MyBatis 매퍼
│       │   └── CiMigrationMapper.java      # CI 이관 MyBatis 매퍼
│       └── service/
│           ├── CustomerCheckService(Impl)  # CI 기반 유의고객, 만19세 미성년자, 외국인 체크
│           ├── NiceCertService(Impl)       # 통신사 미지정 SMS & MO 기기점유 인증 로직
│           └── CiMigrationService(Impl)    # 9/30 이기종 CI 정합성 검증 및 마이그레이션
├── resources/
│   ├── schema-nice-auth.sql                # 7개 테이블 DDL
│   ├── data-nice-auth.sql                  # 약관 4종, 유의고객, 9/30 샘플 데이터
│   └── mapper/
│       ├── NiceAuthMapper.xml              # 본인인증 SQL XML
│       └── CiMigrationMapper.xml           # CI 이관 SQL XML
└── webapp/WEB-INF/views/
    ├── index.jsp                           # 포털 메인 홈 (공통업무 바로가기 배너 추가)
    └── common/auth/
        ├── niceCert.jsp                    # APP/PC Web 반응형 본인인증 UI (통신사 미지정 / MO / 약관)
        └── ciMigration.jsp                 # 9/30 이기종 CI 데이터 이관 관리자 모니터링 UI
```

---

## 6. 🌐 테스트 및 화면 접속 경로

서버 구동 후 브라우저에서 아래 URL로 접속하여 즉시 테스트하실 수 있습니다:

| 화면 / 기능 | URL 경로 | 설명 |
| :--- | :--- | :--- |
| **공통 본인인증 화면** | `http://localhost:8080/common/auth/nice-cert` | 통신사 미지정 SMS 인증, 기기 점유 MO 인증, 비여신 통합약관, 원클릭 시나리오 프리셋 |
| **9/30 CI 이관 관리** | `http://localhost:8080/common/auth/ci-migration` | 이기종 임시 데이터 현황, 정합성 검증, 마이그레이션 실행 및 감사 이력 |
| **소매금융 포털 메인** | `http://localhost:8080/` | 본인인증 및 CI 이관 관리, 게시판 바로가기 대시보드 |

### 🧪 검증된 JUnit 테스트 케이스 (전체 7건 통과 완료)
1. `testGetActiveTermsList`: 비여신금융거래 본인확인 필수 약관 4종 조회
2. `testRequestAndVerifyOtp_Normal`: 통신사 미지정 방식 인증번호 요청, TB_SMS_SEND_QUEUE 적재 및 88자 CI 발급 정상 완료
3. `testMinorCheck`: 만 19세 미만 미성년자(만 14세) 감지 및 법정대리인 동의 안내 반환
4. `testForeignerCheck`: 외국인 고객(`nationalInfo=2`) 감지 및 외국인등록증 실명확인 안내 반환
5. `testWatchlistBlockedCheck`: CI 기반 유의고객(Watchlist/FDS) 식별 및 본인인증 거래 즉시 차단
6. `testCiMigrationPipeline`: 9/30 이기종 CI 정합성 검증(88자, Base64, 중복제외) 및 운영 DB 이관 성공
7. `DemojspApplicationTests`: 스프링 부트 컨텍스트 로딩 및 통합 환경 검증

---

## 7. 🔌 실제 NICE 모듈 및 SMS 모듈 연동 아키텍처 및 운영 전환 가이드

### 7.1 실제 NICE 본인인증 모듈 연동 구조
* **연동 클래스**: [`NiceApiClientImpl.java`](file:///Users/macbook/coding/server/demojsp/src/main/java/com/example/demojsp/domain/auth/client/NiceApiClientImpl.java), [`NiceCryptoUtil.java`](file:///Users/macbook/coding/server/demojsp/src/main/java/com/example/demojsp/domain/auth/client/NiceCryptoUtil.java)
* **1단계 (통신사 미지정 OTP 요청)**:
  * URL: `POST https://svc.niceapi.co.kr/service/v1/auth/carrier-unspecified/request`
  * Header: `Authorization: Bearer {NICE_ACCESS_TOKEN}`, `client_id`, `product_id: 2101979001`
  * Body: `carrier=""` (통신사 미지정)로 설정하여 AES-128-CBC 암호화(`enc_data`) 및 HMAC-SHA256(`integrity_value`) 전송.
  * NICE 서버가 이통 3사 및 MVNO HLR 망과 통신하여 고객의 통신사를 자동 식별하고 인증 세션을 발급.
* **2단계 (인증번호 검증 및 CI/DI 복호화 수신)**:
  * URL: `POST https://svc.niceapi.co.kr/service/v1/auth/carrier-unspecified/verify`
  * Body: `request_no`, `res_seq`, `auth_token`, `otp_code` 전송.
  * NICE가 암호화하여 응답한 결과 전문을 복호화하여 **88자리 연계정보(CI)**, **64자리 DI**, **실명**, **생년월일**, **식별된 통신사(SKT/KT/LGT/MVNO)** 획득.
* **레거시 CPClient 바이너리(JAR) 지원**:
  * 구형 전용선 CPClient.jar 방식의 경우 `NiceCryptoUtil` 주석에 명시된 대로 `CPClient.fnEncode()`, `CPClient.fnDecode()`로 100% 치환 가능.

### 7.2 실제 금융 SMS 발송 모듈 연동 구조
* **연동 클래스**: [`SmsGatewayClientImpl.java`](file:///Users/macbook/coding/server/demojsp/src/main/java/com/example/demojsp/domain/auth/client/SmsGatewayClientImpl.java)
* **방식 ① DB 테이블 큐 방식 (`TB_SMS_SEND_QUEUE`)**:
  * 금융사 대표 발신번호(`1588-0000`), 수신번호, 6자리 OTP 인증번호 메시지 본문을 `TB_SMS_SEND_QUEUE`에 실시간 `INSERT`.
  * 사내 기업 메시징 에이전트(FEP/Daemon)가 Polling하여 이통사 SMSC로 초당 대량 발송.
* **방식 ② 중계사 REST API 방식**:
  * HTTP POST `https://api.sms-gateway.bank.co.kr/v1/send` 호출하여 실시간 발송.

### 7.3 상용 운영 전환 절차 (Going-Live Checklist)
1. **NICE 계약 및 자격증명 설정**:
   * NICE평가정보 계약 후 발급받은 `client-id`, `client-secret`, `product-id`를 `application.yaml`에 기입.
2. **Mock-Mode 비활성화**:
   ```yaml
   nice:
     api:
       base-url: https://svc.niceapi.co.kr
       client-id: {운영_발급_CLIENT_ID}
       client-secret: {운영_발급_CLIENT_SECRET}
       product-id: "2101979001"
       mock-mode: false # ★ 운영 실서버 통신 활성화

   sms:
     gateway:
       api-url: https://api.sms-gateway.bank.co.kr/v1/send
       api-key: {운영_SMS_KEY}
       sender-phone: "1588-0000"
       mock-mode: false # ★ 실제 문자 발송 활성화
   ```
3. **네트워크 방화벽 및 전용선 오픈**:
   * 대외계 NICE API IP(`svc.niceapi.co.kr:443`) 및 SMS 중계사 IP 아웃바운드 80/443 포트 오픈.
