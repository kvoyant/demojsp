-- =============================================================================
-- [CF-BC-04 / 공통#22] 약관동의 샘플 데이터 (= 디지털어드민에 등록된 약관이라고 가정)
--   TB_CO_TERMS_BIZ 가 비어 있을 때 1회만 적재된다 (common.store.seed-if-empty=true).
--   운영은 ADMIN 화면 / DBA 가 직접 등록 - 이 파일은 사용하지 않는다.
--   CN_HASH 는 비워 둔다 → 첫 동의 때 서버가 전문으로 계산해 채운다.
--   주의: 약관 문구 안에 세미콜론을 쓰지 않는다 (SQL 문장 구분자).
-- =============================================================================

-- ---------------------------------------------------------------- 업무코드
INSERT INTO TB_CO_TERMS_BIZ (BIZ_CD, BIZ_NM, BIZ_DESC, USE_YN, REG_AT, REG_BY, UPD_AT, UPD_BY) VALUES
 ('SIGN_UP',       '회원가입',                 'APP/PC WEB 회원가입 첫 단계',             'Y', CURRENT_TIMESTAMP, 'SEED', CURRENT_TIMESTAMP, 'SEED'),
 ('LOAN_APPLY',    '대출신청',                 '신용·오토·모기지 대출신청 약관동의 단계', 'Y', CURRENT_TIMESTAMP, 'SEED', CURRENT_TIMESTAMP, 'SEED'),
 ('DEPOSIT_OPEN',  '계좌개설(수신)',           '입출금·예금·적금 신규 가입',              'Y', CURRENT_TIMESTAMP, 'SEED', CURRENT_TIMESTAMP, 'SEED'),
 ('IDENTITY_NICE', '휴대폰 본인인증',          'NICE ID 휴대폰 본인인증 전 (선택약관 없음)', 'Y', CURRENT_TIMESTAMP, 'SEED', CURRENT_TIMESTAMP, 'SEED'),
 ('ID_CARD',       '신분증 진위확인·안면인식', '신분증 촬영 전 / 얼굴 촬영 전',           'Y', CURRENT_TIMESTAMP, 'SEED', CURRENT_TIMESTAMP, 'SEED');

-- ---------------------------------------------------------------- 약관 (버전별)
INSERT INTO TB_CO_TERMS (TERMS_CD, TERMS_VER, TERMS_NM, TERMS_CN, PDF_URL, CN_HASH, EFFECT_DT, USE_YN, REG_AT, REG_BY) VALUES
 ('SVC_USE', 1, '전자금융거래 기본약관',
'제1조 (목적)
이 약관은 페퍼저축은행(이하 "은행")과 이용자 사이의 전자금융거래에 관한 기본적인 사항을 정함을 목적으로 합니다.

제2조 (적용범위)
이 약관은 은행이 제공하는 모바일뱅킹, 인터넷뱅킹 등 전자금융거래 서비스에 적용됩니다.

제3조 (접근매체의 관리)
이용자는 접근매체를 제3자에게 대여하거나 양도해서는 안 됩니다.

※ 샘플 문구입니다. 실제 문구는 디지털어드민 약관관리에서 등록합니다.', NULL, NULL, '2026-01-01', 'Y', CURRENT_TIMESTAMP, 'SEED'),

 ('SVC_USE', 2, '전자금융거래 기본약관',
'제1조 (목적)
이 약관은 페퍼저축은행(이하 "은행")과 이용자 사이의 전자금융거래에 관한 기본적인 사항을 정함을 목적으로 합니다.

제2조 (적용범위)
이 약관은 은행이 제공하는 모바일뱅킹, 인터넷뱅킹, 오픈뱅킹 등 전자금융거래 서비스에 적용됩니다.

제3조 (접근매체의 관리)
이용자는 접근매체를 제3자에게 대여하거나 양도해서는 안 되며, 분실 시 즉시 은행에 신고해야 합니다.

[개정 예정본] 2027-01-01 시행 - ADMIN 미리보기 기준일을 바꾸면 확인할 수 있습니다.
※ 샘플 문구입니다.', NULL, NULL, '2027-01-01', 'Y', CURRENT_TIMESTAMP, 'SEED'),

 ('PRIVACY_COLLECT', 1, '개인(신용)정보 수집·이용 동의',
'1. 수집·이용 목적: 회원관리, 금융거래 제공
2. 수집 항목: 성명, 생년월일, 휴대폰번호, 연계정보(CI)
3. 보유·이용 기간: 거래 종료 후 5년

※ 이전 버전 (2026-07-01 개정으로 대체됨)', NULL, NULL, '2025-01-01', 'Y', CURRENT_TIMESTAMP, 'SEED'),

 ('PRIVACY_COLLECT', 2, '개인(신용)정보 수집·이용 동의',
'1. 수집·이용 목적: 회원관리, 금융거래의 설정·유지·이행, 법령상 의무 이행
2. 수집 항목: 성명, 생년월일, 성별, 휴대폰번호, 연계정보(CI), 이메일
3. 보유·이용 기간: 금융거래 종료일로부터 5년
4. 동의를 거부할 권리가 있으나, 거부 시 서비스 이용이 제한됩니다.

※ 샘플 문구입니다.', NULL, NULL, '2026-07-01', 'Y', CURRENT_TIMESTAMP, 'SEED'),

 ('PRIVACY_PROVIDE', 1, '개인(신용)정보 제공 동의',
'1. 제공받는 자: 저축은행중앙회, 신용정보집중기관
2. 제공 목적: 대출 심사 및 사후관리
3. 제공 항목: 성명, 대출 신청 정보, 신용거래 정보
4. 보유 기간: 제공 목적 달성 시까지

※ 샘플 문구입니다.', NULL, NULL, '2026-01-01', 'Y', CURRENT_TIMESTAMP, 'SEED'),

 ('UNIQUE_ID', 1, '고유식별정보 처리 동의',
'1. 처리 목적: 실명확인 및 금융거래 법령상 의무 이행
2. 처리 항목: 주민등록번호, 외국인등록번호
3. 보유 기간: 금융거래 종료일로부터 5년

※ 샘플 문구입니다.', NULL, NULL, '2026-01-01', 'Y', CURRENT_TIMESTAMP, 'SEED'),

 ('NON_CREDIT', 1, '비여신금융거래 약관',
'비여신금융거래(조회, 증명서 발급, 회원정보 관리 등)를 위한 본인확인에 관한 약관입니다.
통신사 선택 없이 성명, 생년월일, 휴대폰번호로 본인확인을 진행합니다.

※ 샘플 문구입니다.', NULL, NULL, '2026-01-01', 'Y', CURRENT_TIMESTAMP, 'SEED'),

 ('NICE_SERVICE', 1, '본인확인서비스 이용약관',
'NICE평가정보(주)가 제공하는 휴대폰 본인확인서비스 이용에 관한 약관입니다.
이동통신 3사 및 알뜰폰 사업자 공통으로 적용됩니다.

※ 샘플 문구입니다.', NULL, NULL, '2026-01-01', 'Y', CURRENT_TIMESTAMP, 'SEED'),

 ('LOAN_BASIC', 1, '여신거래기본약관',
'제1조 (적용범위)
이 약관은 은행과 채무자 사이의 여신거래에 관한 기본적인 사항을 정합니다.

제2조 (이자 등)
이자, 할인료, 보증료, 수수료 등은 법령이 허용하는 한도 내에서 은행이 정한 바에 따릅니다.

※ 샘플 문구입니다.', NULL, NULL, '2026-01-01', 'Y', CURRENT_TIMESTAMP, 'SEED'),

 ('CREDIT_INQUIRY', 1, '개인(신용)정보 조회 동의',
'1. 조회 대상 기관: 신용정보집중기관, 신용평가회사
2. 조회 목적: 대출 한도 및 금리 산정
3. 조회 항목: 신용거래 정보, 신용도 판단 정보
조회 기록은 신용평점에 영향을 주지 않습니다.

※ 샘플 문구입니다.', NULL, NULL, '2026-01-01', 'Y', CURRENT_TIMESTAMP, 'SEED'),

 ('DEPOSIT_BASIC', 1, '예금거래기본약관',
'제1조 (적용범위)
이 약관은 은행과 예금주 사이의 예금거래에 관한 기본적인 사항을 정합니다.

제2조 (실명거래)
예금주는 실명으로 거래해야 합니다.

※ 샘플 문구입니다.', NULL, NULL, '2026-01-01', 'Y', CURRENT_TIMESTAMP, 'SEED'),

 ('MARKETING_USE', 1, '마케팅 목적 개인정보 수집·이용 동의',
'1. 목적: 신상품 안내, 이벤트·혜택 제공
2. 항목: 성명, 휴대폰번호, 이메일
3. 보유 기간: 동의 철회 시까지
동의하지 않아도 서비스 이용에는 제한이 없습니다.

※ 샘플 문구입니다.', NULL, NULL, '2026-01-01', 'Y', CURRENT_TIMESTAMP, 'SEED'),

 ('AD_RECEIVE', 1, '광고성 정보 수신 동의 (문자·푸시·이메일)',
'은행의 상품·이벤트 등 광고성 정보를 문자, 앱 푸시, 이메일로 받습니다.
수신 동의는 언제든지 앱 설정에서 철회할 수 있습니다.

※ 샘플 문구입니다.', NULL, NULL, '2026-01-01', 'Y', CURRENT_TIMESTAMP, 'SEED'),

 ('ID_CARD_COLLECT', 1, '신분증 사본 수집·이용 동의',
'비대면 실명확인을 위해 신분증 사본(이미지)을 수집하고 발급기관에 진위확인을 요청합니다.
수집한 사본은 관련 법령에 따라 보관 후 파기합니다.

※ 샘플 문구입니다.', NULL, NULL, '2026-01-01', 'Y', CURRENT_TIMESTAMP, 'SEED'),

 ('FACE_BIO', 1, '생체(얼굴)정보 수집·이용 동의',
'안면인증을 위해 촬영한 얼굴 이미지와 신분증 사진을 비교합니다.
얼굴 이미지는 인증 완료 즉시 파기합니다.

※ 샘플 문구입니다.', NULL, NULL, '2026-01-01', 'Y', CURRENT_TIMESTAMP, 'SEED');

-- ---------------------------------------------------------------- 업무별 약관 (REQ_YN Y=필수 / N=선택)
INSERT INTO TB_CO_BIZ_TERMS (BIZ_CD, TERMS_CD, REQ_YN, SORT_ORD, USE_YN, UPD_AT, UPD_BY) VALUES
 ('SIGN_UP', 'SVC_USE',         'Y', 1, 'Y', CURRENT_TIMESTAMP, 'SEED'),
 ('SIGN_UP', 'PRIVACY_COLLECT', 'Y', 2, 'Y', CURRENT_TIMESTAMP, 'SEED'),
 ('SIGN_UP', 'UNIQUE_ID',       'Y', 3, 'Y', CURRENT_TIMESTAMP, 'SEED'),
 ('SIGN_UP', 'MARKETING_USE',   'N', 1, 'Y', CURRENT_TIMESTAMP, 'SEED'),
 ('SIGN_UP', 'AD_RECEIVE',      'N', 2, 'Y', CURRENT_TIMESTAMP, 'SEED'),

 ('LOAN_APPLY', 'LOAN_BASIC',      'Y', 1, 'Y', CURRENT_TIMESTAMP, 'SEED'),
 ('LOAN_APPLY', 'PRIVACY_COLLECT', 'Y', 2, 'Y', CURRENT_TIMESTAMP, 'SEED'),
 ('LOAN_APPLY', 'CREDIT_INQUIRY',  'Y', 3, 'Y', CURRENT_TIMESTAMP, 'SEED'),
 ('LOAN_APPLY', 'PRIVACY_PROVIDE', 'Y', 4, 'Y', CURRENT_TIMESTAMP, 'SEED'),
 ('LOAN_APPLY', 'MARKETING_USE',   'N', 1, 'Y', CURRENT_TIMESTAMP, 'SEED'),

 ('DEPOSIT_OPEN', 'DEPOSIT_BASIC',   'Y', 1, 'Y', CURRENT_TIMESTAMP, 'SEED'),
 ('DEPOSIT_OPEN', 'PRIVACY_COLLECT', 'Y', 2, 'Y', CURRENT_TIMESTAMP, 'SEED'),
 ('DEPOSIT_OPEN', 'UNIQUE_ID',       'Y', 3, 'Y', CURRENT_TIMESTAMP, 'SEED'),
 ('DEPOSIT_OPEN', 'AD_RECEIVE',      'N', 1, 'Y', CURRENT_TIMESTAMP, 'SEED'),

 ('IDENTITY_NICE', 'NICE_SERVICE', 'Y', 1, 'Y', CURRENT_TIMESTAMP, 'SEED'),
 ('IDENTITY_NICE', 'UNIQUE_ID',    'Y', 2, 'Y', CURRENT_TIMESTAMP, 'SEED'),
 ('IDENTITY_NICE', 'NON_CREDIT',   'Y', 3, 'Y', CURRENT_TIMESTAMP, 'SEED'),

 ('ID_CARD', 'ID_CARD_COLLECT', 'Y', 1, 'Y', CURRENT_TIMESTAMP, 'SEED'),
 ('ID_CARD', 'FACE_BIO',        'Y', 2, 'Y', CURRENT_TIMESTAMP, 'SEED');
