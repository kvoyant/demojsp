/**
 * =============================================================================
 *  [CF-BC-04] 약관동의 공통 컴포넌트  -  /js/common/terms-agree.js   (REQ-AP-602)
 * -----------------------------------------------------------------------------
 *  각 화면에 개별 코딩되어 있던 약관동의를 이 컴포넌트 하나로 통일한다.
 *  화면은 업무코드(bizCode)만 넘긴다. 약관 코드/문구/필수여부는 서버(ADMIN 등록 약관)에서 내려온다.
 *
 *  동작 (단일 페이지 2개)
 *    1단계  필수 약관  : [필수 전체동의] + 항목별 체크/상세보기 → 필수 모두 동의해야 [다음] 활성화
 *    2단계  선택 약관  : [선택 전체동의] + 항목별 체크/상세보기 → [완료] (미동의 가능)
 *                       선택 약관이 없는 업무는 2단계를 생략하고 1단계에서 바로 [동의 완료]
 *    완료   POST /api/common/terms/{bizCode}/agree → 서버가 필수 재검증 + 버전/해시 증적 저장 + 세션 반영
 *
 *  사용 예)
 *    TermsAgree.open({ bizCode: 'SIGN_UP', channel: 'PC_WEB' })      // 기본: 전체화면 레이어
 *      .then(function (r) {
 *          if (!r.success) return;                 // 사용자가 닫음 (code: USER_CANCELED)
 *          // r.data = { agreementId, agreedCodes, declinedCodes, versions, agreedAt }
 *      })
 *      .catch(function (err) { alert(err.message); });   // 약관 조회 실패 등 { success:false, code, message }
 *
 *    TermsAgree.open({ bizCode: 'LOAN_APPLY', mode: 'inline', container: '#termsArea' })   // 화면 안에 삽입
 *
 *  보조 함수
 *    TermsAgree.load(bizCode)               약관 목록 (필수/선택 분리)
 *    TermsAgree.detail(bizCode, termsCode)  약관 전문
 *    TermsAgree.status(bizCode)             현재 세션 동의 결과 (없으면 null)
 *
 *  jQuery / Bootstrap 비의존 (스타일은 .ta- 접두어로 자체 주입) → APP WebView / PC / 모집인WEB 공용.
 *  모든 서버 문자열은 textContent 로만 출력 (XSS 안전).
 * =============================================================================
 */
(function (window, document) {
    'use strict';

    var API_BASE = '/api/common/terms';
    var STYLE_ID = 'ta-style';

    // ------------------------------------------------------------------ 서버 호출

    function unwrap(res) {
        return res.json().catch(function () {
            return { success: false, code: 'HTTP_' + res.status, message: '서버 응답을 처리할 수 없습니다.' };
        }).then(function (body) {
            if (!res.ok || !body || !body.success) throw body || { success: false, code: 'HTTP_' + res.status };
            return body.data;
        });
    }

    function getJson(url) {
        return fetch(url, { credentials: 'same-origin', headers: { 'X-Requested-With': 'XMLHttpRequest' } }).then(unwrap);
    }

    function load(bizCode) {
        return getJson(API_BASE + '/' + encodeURIComponent(bizCode));
    }

    function detail(bizCode, termsCode) {
        return getJson(API_BASE + '/' + encodeURIComponent(bizCode) + '/items/' + encodeURIComponent(termsCode));
    }

    function status(bizCode) {
        return getJson(API_BASE + '/' + encodeURIComponent(bizCode) + '/status');
    }

    function submitAgree(bizCode, body) {
        return fetch(API_BASE + '/' + encodeURIComponent(bizCode) + '/agree', {
            method: 'POST',
            credentials: 'same-origin',
            headers: { 'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest' },
            // Spring Security CSRF 도입 시 헤더 추가: 'X-CSRF-TOKEN': token
            body: JSON.stringify(body)
        }).then(unwrap);
    }

    // ------------------------------------------------------------------ 스타일

    var CSS = [
        '.ta-overlay{position:fixed;inset:0;z-index:2000;background:rgba(15,23,42,.55);display:flex;align-items:center;justify-content:center}',
        '.ta-sheet{position:relative;display:flex;flex-direction:column;background:#fff;color:#1f2937;width:100%;max-width:480px;height:100%;max-height:760px;overflow:hidden;font-size:15px;line-height:1.5;box-sizing:border-box}',
        '@media (min-width:600px){.ta-overlay .ta-sheet{height:92%;border-radius:16px;box-shadow:0 20px 50px rgba(0,0,0,.25)}}',
        '.ta-inline .ta-sheet{max-width:none;height:auto;max-height:none;border:1px solid #dbe1ea;border-radius:12px}',
        '.ta-sheet *{box-sizing:border-box}',
        '.ta-head{display:flex;align-items:center;gap:8px;padding:14px 16px;border-bottom:1px solid #eef1f5;min-height:56px}',
        '.ta-head-title{flex:1;font-weight:700;font-size:16px;margin:0}',
        '.ta-step{font-size:12px;color:#0d47a1;background:#e8f0fe;border-radius:999px;padding:2px 10px;font-weight:700}',
        '.ta-icon-btn{border:0;background:none;font-size:22px;line-height:1;color:#475569;cursor:pointer;padding:4px 6px;border-radius:6px}',
        '.ta-icon-btn:focus-visible,.ta-row-main:focus-visible,.ta-view:focus-visible,.ta-all:focus-visible,.ta-btn:focus-visible{outline:3px solid #90caf9;outline-offset:2px}',
        '.ta-body{flex:1;overflow-y:auto;padding:20px 16px}',
        '.ta-lead{font-size:20px;font-weight:700;margin:0 0 6px;white-space:pre-line;color:#0f172a}',
        '.ta-sub{font-size:13px;color:#64748b;margin:0 0 18px}',
        '.ta-all{display:flex;align-items:center;gap:10px;width:100%;padding:14px;border:1px solid #cfd8e3;border-radius:12px;background:#f8fafc;font-weight:700;font-size:16px;cursor:pointer;text-align:left;color:inherit}',
        '.ta-all[aria-checked=true]{border-color:#0d47a1;background:#eef4ff}',
        '.ta-list{list-style:none;margin:14px 0 0;padding:0}',
        '.ta-row{display:flex;align-items:center;border-bottom:1px solid #f1f5f9}',
        '.ta-row-main{flex:1;display:flex;align-items:center;gap:10px;padding:12px 4px;border:0;background:none;text-align:left;cursor:pointer;color:inherit;font-size:14px}',
        '.ta-tag{font-size:11px;font-weight:700;border-radius:4px;padding:1px 6px;margin-right:6px;white-space:nowrap}',
        '.ta-tag-req{color:#c62828;background:#fdecea}',
        '.ta-tag-opt{color:#475569;background:#eef1f5}',
        '.ta-view{border:0;background:none;color:#64748b;font-size:13px;padding:10px 6px;cursor:pointer;white-space:nowrap}',
        '.ta-view:hover{color:#0d47a1;text-decoration:underline}',
        '.ta-check{flex:none;width:22px;height:22px;border-radius:50%;border:2px solid #cbd5e1;display:inline-flex;align-items:center;justify-content:center;color:#fff;font-size:13px;font-weight:700}',
        '[aria-checked=true] > .ta-check{background:#0d47a1;border-color:#0d47a1}',
        '.ta-foot{padding:12px 16px 16px;border-top:1px solid #eef1f5;background:#fff}',
        '.ta-hint{font-size:12px;color:#c62828;margin:0 0 8px;min-height:18px}',
        '.ta-hint.ta-ok{color:#2e7d32}',
        '.ta-btns{display:flex;gap:8px}',
        '.ta-btn{flex:1;border:0;border-radius:10px;padding:14px;font-size:16px;font-weight:700;cursor:pointer;background:#0d47a1;color:#fff}',
        '.ta-btn[disabled]{background:#cbd5e1;cursor:not-allowed}',
        '.ta-btn-sub{flex:0 0 32%;background:#eef1f5;color:#334155}',
        '.ta-detail{position:absolute;inset:0;background:#fff;display:flex;flex-direction:column;z-index:2}',
        '.ta-meta{font-size:12px;color:#64748b;margin:0 0 12px}',
        '.ta-content{white-space:pre-wrap;word-break:keep-all;font-size:14px;line-height:1.7;color:#334155;margin:0;font-family:inherit}',
        '.ta-pdf{display:inline-block;margin:0 0 12px;font-size:13px;color:#0d47a1}',
        '.ta-empty{color:#64748b;font-size:14px;padding:12px 0}',
        '.ta-loading{padding:40px 0;text-align:center;color:#64748b}'
    ].join('\n');

    function injectStyle() {
        if (document.getElementById(STYLE_ID)) return;
        var style = document.createElement('style');
        style.id = STYLE_ID;
        style.textContent = CSS;
        document.head.appendChild(style);
    }

    // ------------------------------------------------------------------ DOM 헬퍼

    function el(tag, className, text) {
        var node = document.createElement(tag);
        if (className) node.className = className;
        if (text != null) node.textContent = text;
        return node;
    }

    function button(className, text, onClick) {
        var b = el('button', className, text);
        b.type = 'button';
        if (onClick) b.addEventListener('click', onClick);
        return b;
    }

    function checkMark() {
        var c = el('span', 'ta-check', '\u2713');
        c.setAttribute('aria-hidden', 'true');
        return c;
    }

    /** PDF/원문 링크는 같은 도메인 상대경로 또는 https 만 허용 */
    function safeUrl(url) {
        if (!url) return null;
        if (url.charAt(0) === '/' && url.charAt(1) !== '/') return url;
        if (url.indexOf('https://') === 0) return url;
        return null;
    }

    // ------------------------------------------------------------------ 컴포넌트

    function open(options) {
        options = options || {};
        if (!options.bizCode) {
            return Promise.reject({ success: false, code: 'TA_INVALID', message: 'bizCode 는 필수입니다.' });
        }
        var inline = options.mode === 'inline';
        var container = null;
        if (inline) {
            container = typeof options.container === 'string' ? document.querySelector(options.container) : options.container;
            if (!container) {
                return Promise.reject({ success: false, code: 'TA_INVALID', message: 'inline 모드는 container 가 필요합니다.' });
            }
        }
        injectStyle();

        return new Promise(function (resolve, reject) {
            var state = { set: null, checked: {}, step: 1, busy: false, done: false };
            var prevOverflow = null;
            var lastFocus = document.activeElement;

            // 골격
            var root = el('div', inline ? 'ta-inline' : 'ta-overlay');
            var sheet = el('div', 'ta-sheet');
            sheet.setAttribute('role', inline ? 'region' : 'dialog');
            if (!inline) sheet.setAttribute('aria-modal', 'true');
            var head = el('div', 'ta-head');
            var backBtn = button('ta-icon-btn', '\u2039', function () { goStep(1); });
            backBtn.setAttribute('aria-label', '이전 단계');
            var titleEl = el('h2', 'ta-head-title', options.title || '약관 동의');
            titleEl.id = 'ta-title-' + Math.random().toString(36).slice(2, 8);
            sheet.setAttribute('aria-labelledby', titleEl.id);
            var stepEl = el('span', 'ta-step');
            head.appendChild(backBtn);
            head.appendChild(titleEl);
            head.appendChild(stepEl);
            if (!inline) {
                var closeBtn = button('ta-icon-btn', '\u00d7', function () { cancel(); });
                closeBtn.setAttribute('aria-label', '닫기');
                head.appendChild(closeBtn);
            }
            var body = el('div', 'ta-body');
            body.appendChild(el('div', 'ta-loading', '약관을 불러오는 중입니다…'));
            var foot = el('div', 'ta-foot');
            var hint = el('p', 'ta-hint');
            hint.setAttribute('aria-live', 'polite');
            var btns = el('div', 'ta-btns');
            foot.appendChild(hint);
            foot.appendChild(btns);
            sheet.appendChild(head);
            sheet.appendChild(body);
            sheet.appendChild(foot);
            root.appendChild(sheet);

            if (inline) {
                container.innerHTML = '';
                container.appendChild(root);
            } else {
                document.body.appendChild(root);
                prevOverflow = document.body.style.overflow;
                document.body.style.overflow = 'hidden';
                document.addEventListener('keydown', onKeydown);
            }
            backBtn.style.visibility = 'hidden';

            function onKeydown(e) {
                if (e.key !== 'Escape') return;
                var d = sheet.querySelector('.ta-detail');
                if (d) closeDetail(); else cancel();
            }

            function teardown() {
                if (inline) return;
                document.removeEventListener('keydown', onKeydown);
                document.body.style.overflow = prevOverflow || '';
                if (root.parentNode) root.parentNode.removeChild(root);
                if (lastFocus && lastFocus.focus) lastFocus.focus();
            }

            function cancel() {
                if (state.done) return;
                state.done = true;
                teardown();
                resolve({ success: false, code: 'USER_CANCELED', message: '약관 동의를 취소했습니다.' });
            }

            function fail(err) {
                state.done = true;
                teardown();
                if (inline) {
                    body.innerHTML = '';
                    body.appendChild(el('p', 'ta-empty', (err && err.message) || '약관을 불러올 수 없습니다.'));
                    btns.innerHTML = '';
                }
                reject(err && err.code ? err : { success: false, code: 'TA_ERROR', message: '약관을 불러올 수 없습니다.' });
            }

            // ---- 데이터

            function list(step) {
                return step === 1 ? state.set.required : state.set.optional;
            }

            function hasOptional() {
                return state.set.optional && state.set.optional.length > 0;
            }

            function allChecked(items) {
                for (var i = 0; i < items.length; i++) if (!state.checked[items[i].termsCode]) return false;
                return true;
            }

            function setAll(items, value) {
                items.forEach(function (t) { state.checked[t.termsCode] = value; });
            }

            function reload(message) {
                state.step = 1;
                state.checked = {};
                return load(options.bizCode).then(function (set) {
                    state.set = set;
                    if (!options.title) titleEl.textContent = (set.bizName || '약관') + ' 약관 동의';
                    render();
                    if (message) setHint(message, false);
                });
            }

            // ---- 렌더링

            function setHint(text, ok) {
                hint.textContent = text || '';
                hint.className = 'ta-hint' + (ok ? ' ta-ok' : '');
            }

            function goStep(step) {
                if (state.busy) return;
                state.step = step;
                render();
                body.scrollTop = 0;
            }

            function render() {
                var step = state.step;
                var items = list(step);
                var twoSteps = hasOptional();
                stepEl.textContent = twoSteps ? (step + ' / 2') : '1 / 1';
                backBtn.style.visibility = step === 2 ? 'visible' : 'hidden';

                body.innerHTML = '';
                body.appendChild(el('p', 'ta-lead', step === 1
                    ? '서비스 이용을 위해\n필수 약관에 동의해 주세요'
                    : '더 나은 혜택을 위한\n선택 약관이에요'));
                body.appendChild(el('p', 'ta-sub', step === 1
                    ? '항목을 누르면 약관 전문을 확인할 수 있습니다.'
                    : '동의하지 않아도 서비스 이용에는 제한이 없습니다.'));

                if (!items.length) {
                    body.appendChild(el('p', 'ta-empty', '해당 약관이 없습니다.'));
                } else {
                    // 전체 동의
                    var all = button('ta-all', null, function () {
                        setAll(items, !allChecked(items));
                        render();
                        sheet.querySelector('.ta-all').focus();
                    });
                    all.setAttribute('role', 'checkbox');
                    all.setAttribute('aria-checked', String(allChecked(items)));
                    all.appendChild(checkMark());
                    all.appendChild(document.createTextNode(step === 1 ? '필수 약관 전체 동의' : '선택 약관 전체 동의'));
                    body.appendChild(all);

                    // 항목
                    var ul = el('ul', 'ta-list');
                    items.forEach(function (t) {
                        var li = el('li', 'ta-row');
                        var main = button('ta-row-main', null, function () {
                            state.checked[t.termsCode] = !state.checked[t.termsCode];
                            render();
                            var again = sheet.querySelector('[data-code="' + cssEscape(t.termsCode) + '"]');
                            if (again) again.focus();
                        });
                        main.setAttribute('role', 'checkbox');
                        main.setAttribute('aria-checked', String(!!state.checked[t.termsCode]));
                        main.setAttribute('data-code', t.termsCode);
                        main.appendChild(checkMark());
                        var label = el('span');
                        label.appendChild(el('span', 'ta-tag ' + (t.required ? 'ta-tag-req' : 'ta-tag-opt'), t.required ? '필수' : '선택'));
                        label.appendChild(document.createTextNode(t.title));
                        main.appendChild(label);
                        var view = button('ta-view', '보기 \u203a', function () { openDetail(t); });
                        view.setAttribute('aria-label', t.title + ' 전문 보기');
                        li.appendChild(main);
                        li.appendChild(view);
                        ul.appendChild(li);
                    });
                    body.appendChild(ul);
                }

                // 하단 버튼
                btns.innerHTML = '';
                if (step === 1) {
                    var ok = allChecked(items);
                    var next = button('ta-btn', twoSteps ? '다음' : '동의 완료', function () {
                        if (!allChecked(list(1))) return;          // 필수 미동의 → 진행 불가
                        if (twoSteps) goStep(2); else complete();
                    });
                    next.disabled = !ok;
                    btns.appendChild(next);
                    setHint(ok ? '' : '필수 약관에 모두 동의해야 다음으로 진행할 수 있습니다.', false);
                } else {
                    btns.appendChild(button('ta-btn ta-btn-sub', '이전', function () { goStep(1); }));
                    btns.appendChild(button('ta-btn', '완료', function () { complete(); }));
                    setHint('', false);
                }
            }

            // ---- 상세보기

            function openDetail(t) {
                var layer = el('div', 'ta-detail');
                layer.setAttribute('role', 'document');
                var dHead = el('div', 'ta-head');
                var back = button('ta-icon-btn', '\u2039', closeDetail);
                back.setAttribute('aria-label', '목록으로');
                dHead.appendChild(back);
                dHead.appendChild(el('h3', 'ta-head-title', t.title));
                var dBody = el('div', 'ta-body');
                dBody.appendChild(el('div', 'ta-loading', '약관 전문을 불러오는 중입니다…'));
                var dFoot = el('div', 'ta-foot');
                var dBtns = el('div', 'ta-btns');
                dBtns.appendChild(button('ta-btn', '확인하고 동의', function () {
                    state.checked[t.termsCode] = true;
                    closeDetail();
                }));
                dFoot.appendChild(dBtns);
                layer.appendChild(dHead);
                layer.appendChild(dBody);
                layer.appendChild(dFoot);
                sheet.appendChild(layer);
                back.focus();

                detail(options.bizCode, t.termsCode).then(function (d) {
                    dBody.innerHTML = '';
                    dBody.appendChild(el('p', 'ta-meta', (d.required ? '필수' : '선택') + ' · 버전 ' + d.version + ' · 시행 ' + (d.registeredAt || '-')));
                    var pdf = safeUrl(d.pdfUrl);
                    if (pdf) {
                        // PDF/약관 Viewer 는 CF-NA-03 공통모듈 담당 → 여기서는 링크만 제공
                        var a = el('a', 'ta-pdf', 'PDF 원문 보기');
                        a.href = pdf; a.target = '_blank'; a.rel = 'noopener';
                        dBody.appendChild(a);
                    }
                    dBody.appendChild(el('pre', 'ta-content', d.content));
                }).catch(function (err) {
                    dBody.innerHTML = '';
                    dBody.appendChild(el('p', 'ta-empty', (err && err.message) || '약관 전문을 불러올 수 없습니다.'));
                });
            }

            function closeDetail() {
                var d = sheet.querySelector('.ta-detail');
                if (d) sheet.removeChild(d);
                render();
            }

            // ---- 완료

            function complete() {
                if (state.busy) return;
                if (!allChecked(list(1))) { goStep(1); return; }
                state.busy = true;
                var items = list(1).concat(state.set.optional || []).map(function (t) {
                    return { termsCode: t.termsCode, version: t.version, agreed: !!state.checked[t.termsCode] };
                });
                setHint('처리 중입니다…', true);
                Array.prototype.forEach.call(btns.querySelectorAll('button'), function (b) { b.disabled = true; });

                submitAgree(options.bizCode, { channel: options.channel || 'PC_WEB', items: items })
                    .then(function (result) {
                        state.busy = false;
                        state.done = true;
                        if (inline) {
                            body.innerHTML = '';
                            body.appendChild(el('p', 'ta-lead', '약관 동의가 완료되었습니다.'));
                            body.appendChild(el('p', 'ta-sub', '동의번호 ' + result.agreementId + ' · ' + result.agreedAt));
                            btns.innerHTML = '';
                            setHint('', true);
                            backBtn.style.visibility = 'hidden';
                        }
                        teardown();
                        resolve({ success: true, data: result });
                    })
                    .catch(function (err) {
                        state.busy = false;
                        if (err && err.code === 'TRM-103') {
                            // 화면을 띄워둔 사이 약관이 개정됨 → 새 약관으로 다시 동의
                            reload('약관이 개정되었습니다. 변경된 약관을 확인 후 다시 동의해 주세요.').catch(fail);
                            return;
                        }
                        render();
                        setHint((err && err.message) || '약관 동의 처리 중 오류가 발생했습니다.', false);
                    });
            }

            function cssEscape(s) {
                return String(s).replace(/["\\]/g, '\\$&');
            }

            reload().then(function () {
                if (!state.set.required.length && !hasOptional()) {
                    fail({ success: false, code: 'TRM-001', message: '등록된 약관이 없는 업무입니다.' });
                    return;
                }
                if (!state.set.required.length) goStep(2);  // 선택 약관만 있는 업무
                if (!inline) {
                    var first = sheet.querySelector('.ta-all') || sheet.querySelector('.ta-btn');
                    if (first) first.focus();
                }
            }).catch(fail);
        });
    }

    window.TermsAgree = { open: open, load: load, detail: detail, status: status };
})(window, document);
