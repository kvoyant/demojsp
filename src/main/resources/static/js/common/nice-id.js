/*
 * =============================================================================
 *  [CF-BC-03 / 공통#21] 휴대폰 본인인증 NICE ID 공통 컴포넌트 (DB 방식)  -  /js/common/nice-id.js
 * -----------------------------------------------------------------------------
 *  요구사항 (REQ-AP-601)
 *    - 통신사 미지정 방식: 성명 + 생년월일 + 휴대폰번호만 입력 (AS-IS 통신사 선택 필수 → TO-BE 선택 불필요)
 *    - MO 인증 결합: 휴대폰번호 입력 생략, 고객 휴대폰에서 문자를 보내 기기 소지까지 확인
 *    - 약관 추가(비여신금융거래 약관): 공통#22 약관동의(ConsentAgree)를 먼저 띄운다 - 약관 목록은 DB
 *    - 기존 디자인 컴포넌트 활용: 약관동의 레이어와 같은 모양 (ui-tokens 색상)
 *
 *  사용법 (업무 화면 JSP)
 *    <link rel="stylesheet" href="/css/common/ui-tokens.css">
 *    <link rel="stylesheet" href="/css/common/consent-agree.css">
 *    <link rel="stylesheet" href="/css/common/nice-id.css">
 *    <script src="/js/common/consent-agree.js"></script>
 *    <script src="/js/common/nice-id.js"></script>
 *
 *    NiceIdAuth.open({
 *        purposeCd: 'SIGN_UP',                 // 업무코드 (TB_CO_IDV_PURPOSE) - 약관·인증방식·나이·외국인 정책은 DB
 *        channel: 'APP',
 *        onComplete: function (r) {            // 인증 성공 (r.reqNo 를 다음 단계 요청에 보낸다)
 *            location.href = '/member/join/info?idvReqNo=' + r.reqNo;
 *        },
 *        onFail: function (r) { },             // 차단(미성년·외국인·유의고객) / 실패 / 만료 - 안내 화면을 본 뒤 호출
 *        onCancel: function () { }
 *    });
 *
 *  화면 흐름
 *    (약관동의 2페이지) → [휴대폰번호로 인증 | 번호 입력 없이(MO)] → 정보 입력 → 인증번호 / 문자 보내기 → 결과
 *
 *  - 서버 문자열은 textContent 로만 출력 (XSS 안전), CI 는 서버가 화면에 보내지 않는다
 *  - ES5 문법 유지 (구형 웹뷰)
 * =============================================================================
 */
(function (global) {
    'use strict';

    var API = '/api/common/niceid';
    var POLL_MS = 3000;

    // ------------------------------------------------------------------ 작은 도우미
    function el(tag, cls, text) {
        var e = document.createElement(tag);
        if (cls) e.className = cls;
        if (text != null) e.textContent = text;
        return e;
    }

    function call(method, url, body) {
        return fetch(url, {
            method: method,
            headers: body ? { 'Content-Type': 'application/json' } : {},
            body: body ? JSON.stringify(body) : undefined,
            credentials: 'same-origin'
        }).then(function (r) {
            return r.json().catch(function () {
                return { success: false, code: 'HTTP-' + r.status, message: '일시적인 오류가 발생했습니다.' };
            });
        });
    }

    function field(label, input, hint) {
        var w = el('label', 'nid-field');
        w.appendChild(el('span', 'nid-label', label));
        w.appendChild(input);
        if (hint) w.appendChild(el('span', 'nid-hint', hint));
        return w;
    }

    function input(type, placeholder, max) {
        var i = el('input', 'nid-input');
        i.type = type;
        i.placeholder = placeholder || '';
        if (max) i.maxLength = max;
        return i;
    }

    function digits(v) { return (v || '').replace(/[^0-9]/g, ''); }

    function mmss(sec) {
        sec = Math.max(0, sec);
        return Math.floor(sec / 60) + ':' + ('0' + (sec % 60)).slice(-2);
    }

    // ------------------------------------------------------------------ 컴포넌트
    function open(options) {
        var opts = options || {};
        if (!opts.purposeCd) throw new Error('NiceIdAuth.open: purposeCd 가 필요합니다.');
        var channel = opts.channel || 'APP';
        var state = { purpose: null, agreeNo: null, method: 'SMS', start: null, timer: null, poll: null };

        // ---- 레이어 뼈대 (약관동의와 같은 모양)
        var layer = el('div', 'nid-layer');
        var panel = el('div', 'nid-panel');
        panel.setAttribute('role', 'dialog');
        panel.setAttribute('aria-modal', 'true');
        panel.setAttribute('aria-labelledby', 'nidTitle');
        var head = el('div', 'nid-head');
        var back = el('button', 'nid-icon-btn', '‹');
        back.type = 'button';
        back.setAttribute('aria-label', '닫기');
        var title = el('h2', 'nid-head-title', '휴대폰 본인인증');
        title.id = 'nidTitle';
        title.tabIndex = -1;
        head.appendChild(back);
        head.appendChild(title);
        var body = el('div', 'nid-body');
        var foot = el('div', 'nid-foot');
        var main = el('button', 'nid-btn', '다음');
        main.type = 'button';
        foot.appendChild(main);
        panel.appendChild(head);
        panel.appendChild(body);
        panel.appendChild(foot);
        layer.appendChild(panel);

        function show() {
            if (!layer.parentNode) document.body.appendChild(layer);
            document.body.classList.add('nid-open');
        }

        function stopTimers() {
            if (state.timer) { clearInterval(state.timer); state.timer = null; }
            if (state.poll) { clearTimeout(state.poll); state.poll = null; }
        }

        function close() {
            stopTimers();
            document.body.classList.remove('nid-open');
            if (layer.parentNode) layer.parentNode.removeChild(layer);
        }

        function cancel() {
            close();
            if (typeof opts.onCancel === 'function') opts.onCancel();
        }
        back.addEventListener('click', cancel);

        function error(msg) {
            if (global.UiAlert) return global.UiAlert.alert(msg, { type: 'error' });
            global.alert(msg);
            return Promise.resolve();
        }

        /** 하단 버튼 1개 갱신 (매 화면마다 동작이 바뀐다) */
        function setMain(text, handler, disabled) {
            var b = main.cloneNode(false);
            b.textContent = text;
            b.disabled = !!disabled;
            b.addEventListener('click', handler);
            foot.replaceChild(b, main);
            main = b;
        }

        // ================================================================ ① 정책 조회 → 약관
        call('GET', API + '/purposes/' + encodeURIComponent(opts.purposeCd)).then(function (res) {
            if (!res.success) { return error(res.message).then(cancel); }
            state.purpose = res.data;
            state.method = res.data.smsYn === 'Y' ? 'SMS' : 'MO';
            if (res.data.consentBizCd && global.ConsentAgree) {
                // 비여신금융거래 약관 등 - 공통#22 약관동의 (약관 목록·필수여부는 DB)
                global.ConsentAgree.open({
                    bizCd: res.data.consentBizCd,
                    channel: channel,
                    onComplete: function (r) { state.agreeNo = r.agreeNo; renderForm(); },
                    onCancel: cancel
                });
            } else if (res.data.consentBizCd) {
                error('약관동의 컴포넌트(consent-agree.js)를 먼저 포함하세요.').then(cancel);
            } else {
                renderForm();
            }
        }).catch(function () { error('네트워크 오류가 발생했습니다.').then(cancel); });

        // ================================================================ ② 정보 입력 (SMS / MO)
        var f = { name: input('text', '홍길동', 50), birth: input('tel', '19900101', 8), phone: input('tel', '01012345678', 13) };
        f.birth.inputMode = 'numeric';
        f.phone.inputMode = 'numeric';

        function renderForm() {
            show();
            stopTimers();
            body.textContent = '';
            body.appendChild(el('p', 'nid-biz', state.purpose.purposeNm));
            body.appendChild(el('h3', 'nid-title', '본인 명의 휴대폰으로 인증해 주세요'));

            // 인증 방식 선택 (업무 정책에서 둘 다 허용할 때만 보인다)
            if (state.purpose.smsYn === 'Y' && state.purpose.moYn === 'Y') {
                var seg = el('div', 'nid-seg');
                seg.setAttribute('role', 'tablist');
                [['SMS', '휴대폰번호로 인증'], ['MO', '번호 입력 없이 인증']].forEach(function (m) {
                    var b = el('button', 'nid-seg-btn' + (state.method === m[0] ? ' on' : ''), m[1]);
                    b.type = 'button';
                    b.setAttribute('role', 'tab');
                    b.setAttribute('aria-selected', state.method === m[0] ? 'true' : 'false');
                    b.addEventListener('click', function () { state.method = m[0]; renderForm(); });
                    seg.appendChild(b);
                });
                body.appendChild(seg);
            }

            body.appendChild(field('성명', f.name));
            body.appendChild(field('생년월일 8자리', f.birth));
            if (state.method === 'SMS') {
                body.appendChild(field('휴대폰번호', f.phone, '통신사 선택 없이 인증합니다.'));   // ★ 통신사 선택 항목 없음
                setMain('인증번호 받기', requestSms);
            } else {
                body.appendChild(el('p', 'nid-note', '휴대폰번호를 입력하지 않습니다. 다음 화면의 문구를 이 휴대폰에서 문자로 보내면 번호와 기기 소지를 함께 확인합니다.'));
                setMain('문자 보내기로 인증', requestMo);
            }
            title.focus();
        }

        function baseBody() {
            return { purposeCd: opts.purposeCd, agreeNo: state.agreeNo, channel: channel, name: f.name.value.trim(), birth: digits(f.birth.value) };
        }

        // ================================================================ ③-SMS 인증번호
        function requestSms() {
            var b = baseBody();
            b.phone = digits(f.phone.value);
            main.disabled = true;
            call('POST', API + '/sms/request', b).then(function (res) {
                main.disabled = false;
                if (!res.success) return error(res.message);
                state.start = res.data;
                renderCode();
            });
        }

        function renderCode() {
            body.textContent = '';
            body.appendChild(el('h3', 'nid-title', '인증번호를 입력해 주세요'));
            body.appendChild(el('p', 'nid-note', state.start.phoneMask + ' 로 인증번호 6자리를 보냈습니다.'));
            var code = input('tel', '6자리', 6);
            code.inputMode = 'numeric';
            code.autocomplete = 'one-time-code';
            var timer = el('span', 'nid-timer');
            timer.setAttribute('aria-live', 'off');
            var w = field('인증번호', code);
            w.appendChild(timer);
            body.appendChild(w);
            if (state.start.testCode) {
                // local 시뮬레이터만 (운영 응답에는 testCode 가 없다)
                code.value = state.start.testCode;
                body.appendChild(el('p', 'nid-demo', '[local 시뮬레이터] 인증번호 ' + state.start.testCode + ' 를 자동 입력했습니다.'));
            }
            var again = el('button', 'nid-link', '인증번호 다시 받기');
            again.type = 'button';
            again.addEventListener('click', renderForm);
            body.appendChild(again);
            countdown(timer, function () { setMain('인증번호 다시 받기', renderForm); });
            setMain('확인', function () {
                main.disabled = true;
                call('POST', API + '/sms/verify', { reqNo: state.start.reqNo, code: digits(code.value) }).then(function (res) {
                    main.disabled = false;
                    if (res.success) return renderResult(res.data);
                    if (res.code === 'NID-008' || res.code === 'NID-006' || res.code === 'NID-004') {
                        return error(res.message).then(renderForm);            // 횟수 초과·만료 → 처음부터
                    }
                    return error(res.message);                                  // 오입력 (n/5)
                });
            });
            code.focus();
        }

        function countdown(target, onEnd) {
            var end = Date.now() + state.start.validSec * 1000;
            function tick() {
                var left = Math.round((end - Date.now()) / 1000);
                target.textContent = mmss(left);
                if (left <= 0) { stopTimers(); onEnd(); }
            }
            tick();
            state.timer = setInterval(tick, 1000);
        }

        // ================================================================ ③-MO 문자 보내기
        function requestMo() {
            main.disabled = true;
            call('POST', API + '/mo/request', baseBody()).then(function (res) {
                main.disabled = false;
                if (!res.success) return error(res.message);
                state.start = res.data;
                renderMo();
            });
        }

        function renderMo() {
            body.textContent = '';
            body.appendChild(el('h3', 'nid-title', '이 휴대폰에서 문자를 보내 주세요'));
            var box = el('div', 'nid-mo');
            box.appendChild(el('span', 'nid-mo-label', '받는 번호'));
            box.appendChild(el('strong', 'nid-mo-value', state.start.moNumber));
            box.appendChild(el('span', 'nid-mo-label', '보낼 내용'));
            box.appendChild(el('strong', 'nid-mo-value', state.start.moCode));
            body.appendChild(box);
            var openSms = el('a', 'nid-btn sub', '문자앱 열기');
            openSms.href = state.start.smsUri;                      // APP 웹뷰: 기본 문자앱이 번호·내용이 채워진 채 열린다
            body.appendChild(openSms);
            var timer = el('p', 'nid-note');
            body.appendChild(timer);
            var status = el('p', 'nid-note', '문자를 보내면 자동으로 확인합니다.');
            status.setAttribute('aria-live', 'polite');
            body.appendChild(status);

            if (opts.demo) {
                // [local 시뮬레이터] 실제 문자 대신 "이 번호에서 보냈다" 로 처리
                var demo = el('div', 'nid-demo');
                demo.appendChild(el('span', null, '[local 시뮬레이터] 문자 보낸 번호 '));
                var ph = input('tel', '01012345678', 13);
                ph.className = 'nid-input sm';
                var sent = el('button', 'nid-link', '보낸 것으로 처리');
                sent.type = 'button';
                sent.addEventListener('click', function () {
                    call('POST', API + '/mo/demo-send', { reqNo: state.start.reqNo, phone: digits(ph.value) }).then(function (res) {
                        if (!res.success) error(res.message); else status.textContent = '문자 수신 확인 중...';
                    });
                });
                demo.appendChild(ph);
                demo.appendChild(sent);
                body.appendChild(demo);
            }

            countdown(timer, function () { renderExpired(); });
            setMain('인증 확인', checkMo);
            poll();

            function poll() {
                state.poll = setTimeout(function () { checkMo(true); }, POLL_MS);
            }
            function checkMo(auto) {
                call('GET', API + '/mo/status?reqNo=' + encodeURIComponent(state.start.reqNo)).then(function (res) {
                    if (!res.success) { stopTimers(); return error(res.message).then(renderForm); }
                    if (res.data.status === 'REQUESTED') {
                        if (auto === true) poll(); else status.textContent = res.data.message;
                        return;
                    }
                    renderResult(res.data);
                });
            }
        }

        function renderExpired() {
            body.textContent = '';
            body.appendChild(el('h3', 'nid-title', '인증 시간이 지났습니다'));
            body.appendChild(el('p', 'nid-note', '처음부터 다시 진행해 주세요.'));
            setMain('다시 인증하기', renderForm);
        }

        // ================================================================ ④ 결과
        function renderResult(r) {
            stopTimers();
            if (r.status === 'VERIFIED') {
                close();
                if (typeof opts.onComplete === 'function') opts.onComplete(r);
                return;
            }
            body.textContent = '';
            body.appendChild(el('h3', 'nid-title', r.status === 'BLOCKED' ? '비대면 본인인증을 진행할 수 없습니다' : '본인인증을 완료하지 못했습니다'));
            body.appendChild(el('p', 'nid-result', r.message));
            setMain('확인', function () {
                close();
                if (typeof opts.onFail === 'function') opts.onFail(r);
            });
        }

        return { close: close };
    }

    global.NiceIdAuth = { open: open };
})(window);
