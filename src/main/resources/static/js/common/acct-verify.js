/*
 * =============================================================================
 *  [CF-BC-06 / 공통#25] 1원 계좌인증 공통 컴포넌트 (DB 방식)  -  /js/common/acct-verify.js
 * -----------------------------------------------------------------------------
 *  요구사항 (REQ-AP-605)
 *    - 하나의 1원 인증 서비스를 쓰되, 업무 Context 에 따라 화면을 분리한다
 *        LOAN   : 대출 입출금계좌 등록용 - 대출금 입금/출금계좌를 불러와 자동완성
 *        COMMON : 인증수단 등록 · 환불계좌 등 타 업무 공통 - 은행 선택 + 계좌번호 입력
 *    - 자동완성 계좌가 아닌 다른 계좌를 쓰려면 [수정] → 은행·계좌번호 직접 입력
 *    - 화면 종류 · 자동완성 · 유효시간 · 입력 횟수는 DB (TB_CO_ACCT_BIZ) → 디지털어드민에서 변경
 *
 *  사용법 (업무 화면 JSP)
 *    <link rel="stylesheet" href="/css/common/ui-tokens.css">
 *    <link rel="stylesheet" href="/css/common/acct-verify.css">
 *    <script src="/js/common/acct-verify.js"></script>
 *
 *    AcctVerify.open({
 *        bizCd: 'LOAN_DISBURSE',               // 업무 Context 코드 (TB_CO_ACCT_BIZ)
 *        channel: 'APP',                       // APP / PC_WEB / AGENT_WEB
 *        params: { idvReqNo: '...', idvPurposeCd: 'SIGN_UP' },   // 비로그인 업무만: 공통#21 본인인증 결과로 고객 실명 확인
 *        onComplete: function (r) {            // r.reqNo 를 다음 단계 요청에 보낸다 → 서버 checkVerified
 *            location.href = '/loan/exec/confirm?avReqNo=' + r.reqNo;
 *        },
 *        onFail: function (r) { },             // 입력 횟수 초과 · 시간 초과 등 - 안내 화면을 본 뒤 호출
 *        onCancel: function () { }
 *    });
 *
 *  - 전체 계좌번호는 화면에 내려오지 않는다 (자동완성 후보도 마스킹, 서버가 원장에서 다시 읽는다)
 *  - 서버 문자열은 textContent 로만 출력 (XSS 안전), ES5 문법 유지 (구형 웹뷰)
 * =============================================================================
 */
(function (global) {
    'use strict';

    var API = '/api/common/acct-verify';

    // ------------------------------------------------------------------ 작은 도우미
    function el(tag, cls, text) {
        var e = document.createElement(tag);
        if (cls) e.className = cls;
        if (text != null) e.textContent = text;
        return e;
    }

    function qs(params) {
        var out = [];
        for (var k in params) {
            if (params.hasOwnProperty(k) && params[k] != null && params[k] !== '') {
                out.push(encodeURIComponent(k) + '=' + encodeURIComponent(params[k]));
            }
        }
        return out.length ? '?' + out.join('&') : '';
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

    function digits(v) { return (v || '').replace(/[^0-9]/g, ''); }

    function mmss(sec) {
        sec = Math.max(0, sec);
        return Math.floor(sec / 60) + ':' + ('0' + (sec % 60)).slice(-2);
    }

    // ------------------------------------------------------------------ 컴포넌트
    function open(options) {
        var opts = options || {};
        if (!opts.bizCd) throw new Error('AcctVerify.open: bizCd 가 필요합니다.');
        var channel = opts.channel || 'APP';
        var params = opts.params || {};
        var q = qs(params);
        // state.mode: PREFILL(자동완성 선택) / INPUT(직접 입력)
        var state = { ctx: null, mode: 'INPUT', prefillId: null, modified: false, start: null, timer: null };

        // ---- 레이어 뼈대 (약관동의 · 본인인증과 같은 모양)
        var layer = el('div', 'avf-layer');
        var panel = el('div', 'avf-panel');
        panel.setAttribute('role', 'dialog');
        panel.setAttribute('aria-modal', 'true');
        panel.setAttribute('aria-labelledby', 'avfTitle');
        var head = el('div', 'avf-head');
        var back = el('button', 'avf-icon-btn', '‹');
        back.type = 'button';
        back.setAttribute('aria-label', '닫기');
        var title = el('h2', 'avf-head-title', '계좌인증');
        title.id = 'avfTitle';
        title.tabIndex = -1;
        head.appendChild(back);
        head.appendChild(title);
        var body = el('div', 'avf-body');
        var foot = el('div', 'avf-foot');
        var main = el('button', 'avf-btn', '1원 보내기');
        main.type = 'button';
        foot.appendChild(main);
        panel.appendChild(head);
        panel.appendChild(body);
        panel.appendChild(foot);
        layer.appendChild(panel);

        function show() {
            if (!layer.parentNode) document.body.appendChild(layer);
            document.body.classList.add('avf-open');
        }

        function close() {
            clearInterval(state.timer);
            if (layer.parentNode) layer.parentNode.removeChild(layer);
            document.body.classList.remove('avf-open');
        }

        function clear() {
            while (body.firstChild) body.removeChild(body.firstChild);
        }

        function error(box, msg) {
            box.textContent = msg || '';
            box.hidden = !msg;
        }

        back.onclick = function () {
            close();
            if (opts.onCancel) opts.onCancel();
        };

        // ================================================= ① 계좌 선택 / 입력
        function renderAccount() {
            var ctx = state.ctx;
            clear();
            clearInterval(state.timer);
            title.textContent = ctx.screenType === 'LOAN' ? '대출 입출금계좌 인증' : '계좌인증';
            body.appendChild(el('p', 'avf-biz', ctx.bizNm));
            body.appendChild(el('h3', 'avf-title', ctx.screenType === 'LOAN'
                ? '대출금을 받고 갚을\n본인 명의 계좌를 확인할게요'
                : '본인 명의 계좌를\n확인할게요'));
            body.appendChild(el('p', 'avf-note', '입력한 계좌로 1원을 보내 드려요. 입금자명에 있는 숫자 4자리를 입력하면 인증이 끝나요.'));

            var err = el('p', 'avf-error');
            err.setAttribute('role', 'alert');
            err.hidden = true;

            var hasPrefill = ctx.screenType === 'LOAN' && ctx.prefills && ctx.prefills.length > 0;
            if (hasPrefill && !state.modified) {
                // ---- LOAN: 대출금 입금/출금계좌 자동완성
                state.mode = 'PREFILL';
                if (!state.prefillId) state.prefillId = ctx.prefills[0].prefillId;
                body.appendChild(el('p', 'avf-label', '대출 입출금계좌'));
                var list = el('div', 'avf-prefills');
                list.setAttribute('role', 'radiogroup');
                ctx.prefills.forEach(function (p) {
                    var b = el('button', 'avf-prefill' + (p.prefillId === state.prefillId ? ' on' : ''));
                    b.type = 'button';
                    b.setAttribute('role', 'radio');
                    b.setAttribute('aria-checked', p.prefillId === state.prefillId ? 'true' : 'false');
                    b.appendChild(el('span', 'avf-prefill-bank', p.bankNm + ' ' + p.acctMask));
                    b.appendChild(el('span', 'avf-prefill-sub', p.productNm + ' · ' + p.usage));
                    b.onclick = function () {
                        state.prefillId = p.prefillId;
                        renderAccount();
                    };
                    list.appendChild(b);
                });
                body.appendChild(list);
                var edit = el('button', 'avf-link', '다른 계좌로 수정');
                edit.type = 'button';
                edit.onclick = function () {
                    state.modified = true;          // 요구사항: 자동완성 계좌 [수정] → 직접 입력
                    renderAccount();
                };
                body.appendChild(edit);
                body.appendChild(err);
                main.textContent = '1원 보내기';
                main.disabled = false;
                main.onclick = function () { send(err, { prefillId: state.prefillId }); };
                return;
            }

            // ---- COMMON (또는 LOAN 에서 [수정]): 은행 선택 + 계좌번호
            state.mode = 'INPUT';
            var bank = el('select', 'avf-input');
            bank.appendChild(new Option('은행을 선택하세요', ''));
            ctx.banks.forEach(function (b) { bank.appendChild(new Option(b.bankNm, b.bankCd)); });
            var acct = el('input', 'avf-input');
            acct.type = 'text';
            acct.inputMode = 'numeric';
            acct.placeholder = '계좌번호 (- 없이 숫자만)';
            acct.maxLength = 20;
            acct.autocomplete = 'off';
            body.appendChild(field('은행', bank));
            body.appendChild(field('계좌번호', acct, '본인 명의 계좌만 등록할 수 있어요.'));
            if (hasPrefill) {
                var undo = el('button', 'avf-link', '대출 입출금계좌 불러오기');
                undo.type = 'button';
                undo.onclick = function () {
                    state.modified = false;
                    renderAccount();
                };
                body.appendChild(undo);
            }
            body.appendChild(err);
            function check() { main.disabled = !bank.value || digits(acct.value).length < 10; }
            bank.onchange = check;
            acct.oninput = function () {
                acct.value = digits(acct.value);
                check();
            };
            main.textContent = '1원 보내기';
            check();
            main.onclick = function () {
                send(err, { bankCd: bank.value, acctNo: digits(acct.value), modifiedYn: state.modified ? 'Y' : 'N' });
            };
            setTimeout(function () { bank.focus(); }, 0);
        }

        function field(label, input, hint) {
            var w = el('label', 'avf-field');
            w.appendChild(el('span', 'avf-label', label));
            w.appendChild(input);
            if (hint) w.appendChild(el('span', 'avf-hint', hint));
            return w;
        }

        function send(err, acct) {
            main.disabled = true;
            var req = { bizCd: opts.bizCd, channel: channel, idvReqNo: params.idvReqNo, idvPurposeCd: params.idvPurposeCd };
            for (var k in acct) if (acct.hasOwnProperty(k)) req[k] = acct[k];
            call('POST', API + '/request' + q, req).then(function (res) {
                main.disabled = false;
                if (!res.success) return error(err, res.message);
                state.start = res.data;
                renderCode();
            });
        }

        // ================================================= ② 입금자명 숫자 4자리
        function renderCode() {
            var s = state.start;
            clear();
            body.appendChild(el('p', 'avf-biz', state.ctx.bizNm));
            body.appendChild(el('h3', 'avf-title', '1원을 보냈어요\n입금자명을 확인해 주세요'));
            var card = el('div', 'avf-sent');
            card.appendChild(el('span', 'avf-sent-label', '받는 계좌'));
            card.appendChild(el('span', 'avf-sent-value', s.bankNm + ' ' + s.acctMask));
            card.appendChild(el('span', 'avf-sent-label', '예금주'));
            card.appendChild(el('span', 'avf-sent-value', s.holderNm));
            card.appendChild(el('span', 'avf-sent-label', '입금자명'));
            var memo = el('span', 'avf-sent-value avf-memo');
            memo.appendChild(document.createTextNode(s.memoPrefix));
            memo.appendChild(el('span', 'avf-memo-code', '0000'));
            card.appendChild(memo);
            body.appendChild(card);
            body.appendChild(el('p', 'avf-note', '은행 앱이나 문자에서 1원 입금 내역의 입금자명 "' + s.memoPrefix + '" 뒤 숫자 4자리를 입력하세요.'));

            var code = el('input', 'avf-input avf-code');
            code.type = 'text';
            code.inputMode = 'numeric';
            code.maxLength = 4;
            code.autocomplete = 'one-time-code';
            code.placeholder = '숫자 4자리';
            var f = field('입금자명 숫자', code);
            var timer = el('span', 'avf-timer');
            timer.setAttribute('aria-live', 'off');
            f.appendChild(timer);
            body.appendChild(f);
            if (s.testCode) body.appendChild(el('p', 'avf-demo', '[local 시뮬레이터] 입금자명: ' + s.memoPrefix + s.testCode));
            var err = el('p', 'avf-error');
            err.setAttribute('role', 'alert');
            err.hidden = true;
            body.appendChild(err);
            var again = el('button', 'avf-link', '계좌 다시 입력');
            again.type = 'button';
            again.onclick = renderAccount;
            body.appendChild(again);

            var left = s.validSec;
            timer.textContent = mmss(left);
            state.timer = setInterval(function () {
                left -= 1;
                timer.textContent = mmss(left);
                if (left <= 0) {
                    clearInterval(state.timer);
                    main.disabled = true;
                    error(err, '입력 시간이 지났어요. [계좌 다시 입력] 으로 1원을 다시 받으세요.');
                }
            }, 1000);

            code.oninput = function () {
                code.value = digits(code.value).slice(0, 4);
                main.disabled = code.value.length !== 4 || left <= 0;
            };
            main.textContent = '인증하기';
            main.disabled = true;
            main.onclick = function () {
                main.disabled = true;
                call('POST', API + '/verify' + q, { reqNo: s.reqNo, code: code.value }).then(function (res) {
                    if (res.success) return renderResult(true, res.data, res.message);
                    // 횟수 초과 · 시간 초과 → 결과 화면, 단순 불일치 → 그 자리에서 다시
                    if (res.code === 'AVF-008' || res.code === 'AVF-006' || res.code === 'AVF-004') {
                        return renderResult(false, { reqNo: s.reqNo, resultCd: res.code }, res.message);
                    }
                    error(err, res.message);
                    code.value = '';
                    code.focus();
                });
            };
            setTimeout(function () { code.focus(); }, 0);
        }

        // ================================================= ③ 결과
        function renderResult(ok, data, message) {
            clearInterval(state.timer);
            clear();
            body.appendChild(el('div', 'avf-icon ' + (ok ? 'ok' : 'fail'), ok ? '✓' : '!'));
            body.appendChild(el('h3', 'avf-title center', ok ? '계좌인증이 완료되었어요' : '계좌인증을 완료하지 못했어요'));
            var p = el('p', 'avf-result', ok ? (data.bankNm + ' ' + data.acctMask) : message);
            body.appendChild(p);
            main.textContent = ok ? '다음' : '확인';
            main.disabled = false;
            main.onclick = function () {
                close();
                if (ok && opts.onComplete) opts.onComplete(data);
                if (!ok && opts.onFail) opts.onFail(data);
            };
            if (!ok) {
                var retry = el('button', 'avf-btn sub', '처음부터 다시');
                retry.type = 'button';
                retry.onclick = renderAccount;
                body.appendChild(retry);
            }
            title.focus();
        }

        // ================================================= 시작: 업무 Context 조회
        show();
        body.appendChild(el('p', 'avf-note', '불러오는 중…'));
        main.disabled = true;
        call('GET', API + '/context/' + encodeURIComponent(opts.bizCd) + q).then(function (res) {
            if (!res.success) {
                clear();
                body.appendChild(el('h3', 'avf-title', '계좌인증을 시작할 수 없어요'));
                body.appendChild(el('p', 'avf-result', res.message));
                main.textContent = '닫기';
                main.disabled = false;
                main.onclick = function () {
                    close();
                    if (opts.onFail) opts.onFail({ resultCd: res.code });
                };
                return;
            }
            state.ctx = res.data;
            renderAccount();
            title.focus();
        });

        return { close: close };
    }

    global.AcctVerify = { open: open };
})(window);
