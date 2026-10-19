/*
 * =============================================================================
 *  [CF-BC-04 / 공통#22] 약관동의 공통 컴포넌트 (DB 방식)  -  /js/common/consent-agree.js
 * -----------------------------------------------------------------------------
 *  업무 화면은 업무코드만 넘긴다. 약관 이름·문구·필수여부·순서는 전부 DB(디지털어드민 등록값)에서 온다.
 *
 *    <link rel="stylesheet" href="/css/common/consent-agree.css">
 *    <script src="/js/common/consent-agree.js"></script>
 *
 *    ConsentAgree.open({
 *        bizCd: 'SIGN_UP',                 // 업무코드 (TB_CO_TERMS_BIZ)
 *        channel: 'APP',                   // APP | PC_WEB | AGENT_WEB
 *        onComplete: function (result) {   // result.agreeNo 를 다음 단계 요청에 함께 보낸다
 *            location.href = '/member/join/info?agreeNo=' + result.agreeNo;
 *        },
 *        onCancel: function () { }         // 사용자가 닫았을 때 (선택)
 *    });
 *
 *  화면 흐름 (REQ-AP-602)
 *    1페이지  필수 약관  : [필수 전체동의] → 모두 체크해야 [다음] 활성
 *    2페이지  선택 약관  : [선택 전체동의] → 체크하지 않아도 [확인] 가능
 *    약관 이름을 누르면 전문(상세)을 보고 [동의] 할 수 있다.
 *    선택 약관이 없는 업무는 1페이지에서 바로 저장, 필수 약관이 없는 업무는 2페이지부터 시작.
 *    저장 시 서버가 DB 기준으로 필수 동의를 다시 검사하고, 개정됐으면(CNS-102) 새 약관으로 다시 보여준다.
 *
 *  서버 문자열은 textContent 로만 넣는다 (XSS 방지).
 * =============================================================================
 */
(function (global) {
    'use strict';

    var API = '/api/common/consent';

    // ------------------------------------------------------------------ 작은 도우미
    function el(tag, className, text) {
        var e = document.createElement(tag);
        if (className) e.className = className;
        if (text != null) e.textContent = text;
        return e;
    }

    function request(method, url, body) {
        return fetch(url, {
            method: method,
            headers: body ? { 'Content-Type': 'application/json' } : {},
            body: body ? JSON.stringify(body) : undefined,
            credentials: 'same-origin'
        }).then(function (res) {
            return res.json().catch(function () {
                return { success: false, code: 'HTTP-' + res.status, message: '일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.' };
            });
        });
    }

    function showMessage(message) {
        if (global.UiAlert) return global.UiAlert.alert(message);
        global.alert(message);
        return Promise.resolve();
    }

    function safeUrl(url) {   // PDF 링크는 같은 사이트 경로 또는 https 만
        return typeof url === 'string' && (/^https:\/\//.test(url) || (/^\//.test(url) && !/^\/\//.test(url)));
    }

    // ------------------------------------------------------------------ 컴포넌트
    function open(options) {
        if (!options || !options.bizCd) throw new Error('ConsentAgree.open: bizCd 가 필요합니다.');

        var state = {
            list: null,          // { bizNm, required:[], optional:[] }
            page: null,          // 'REQ' | 'OPT'
            checked: {},         // termsCd → true
            viewed: {}           // termsCd → true (상세 열람)
        };

        // ---- 레이어 뼈대
        var layer = el('div', 'cns-layer');
        var panel = el('div', 'cns-panel');
        panel.setAttribute('role', 'dialog');
        panel.setAttribute('aria-modal', 'true');
        panel.setAttribute('aria-labelledby', 'cnsTitle');

        var head = el('div', 'cns-head');
        var btnBack = el('button', 'cns-icon-btn', '‹');
        btnBack.type = 'button';
        btnBack.setAttribute('aria-label', '뒤로');
        var title = el('h2', 'cns-head-title', '약관동의');
        title.id = 'cnsTitle';
        title.tabIndex = -1;
        var step = el('span', 'cns-step');
        head.appendChild(btnBack);
        head.appendChild(title);
        head.appendChild(step);

        var body = el('div', 'cns-body');
        var foot = el('div', 'cns-foot');
        var btnNext = el('button', 'cns-btn', '다음');
        btnNext.type = 'button';
        foot.appendChild(btnNext);

        panel.appendChild(head);
        panel.appendChild(body);
        panel.appendChild(foot);
        layer.appendChild(panel);
        document.body.appendChild(layer);
        document.body.classList.add('cns-open');

        // ---- 상세(전문) 시트
        var detail = el('div', 'cns-detail');
        detail.hidden = true;
        panel.appendChild(detail);

        function close() {
            document.removeEventListener('keydown', onKey);
            document.body.classList.remove('cns-open');
            if (layer.parentNode) layer.parentNode.removeChild(layer);
        }

        function cancel() {
            close();
            if (typeof options.onCancel === 'function') options.onCancel();
        }

        function onKey(e) {
            if (e.key !== 'Escape') return;
            if (!detail.hidden) { hideDetail(); } else { goBack(); }
        }
        document.addEventListener('keydown', onKey);

        // ---- 데이터 불러오기
        function load() {
            body.textContent = '';
            body.appendChild(el('p', 'cns-loading', '약관을 불러오는 중입니다...'));
            btnNext.disabled = true;
            return request('GET', API + '/' + encodeURIComponent(options.bizCd)).then(function (res) {
                if (!res.success) {
                    return showMessage(res.message).then(cancel);
                }
                state.list = res.data;
                state.checked = {};
                state.viewed = {};
                render(state.list.required.length ? 'REQ' : 'OPT');
            }).catch(function () {
                showMessage('네트워크 오류로 약관을 불러오지 못했습니다.').then(cancel);
            });
        }

        // ---- 페이지 그리기 (REQ 필수 / OPT 선택)
        function currentTerms() {
            return state.page === 'REQ' ? state.list.required : state.list.optional;
        }

        function render(page) {
            state.page = page;
            var terms = currentTerms();
            var twoPages = state.list.required.length > 0 && state.list.optional.length > 0;
            step.textContent = twoPages ? (page === 'REQ' ? '1/2' : '2/2') : '';
            body.textContent = '';
            body.scrollTop = 0;

            var lead = el('div', 'cns-lead');
            lead.appendChild(el('p', 'cns-biz', state.list.bizNm));
            lead.appendChild(el('h3', 'cns-page-title', page === 'REQ' ? '필수 약관에 동의해 주세요' : '선택 약관에 동의해 주세요'));
            lead.appendChild(el('p', 'cns-page-desc', page === 'REQ'
                ? '서비스 이용을 위해 꼭 필요한 약관입니다.'
                : '동의하지 않아도 서비스를 이용할 수 있습니다.'));
            body.appendChild(lead);

            // 전체동의
            var all = el('label', 'cns-all');
            var allBox = el('input');
            allBox.type = 'checkbox';
            all.appendChild(allBox);
            all.appendChild(el('span', 'cns-check', ''));
            all.appendChild(el('span', 'cns-all-text', page === 'REQ' ? '필수 약관 전체동의' : '선택 약관 전체동의'));
            body.appendChild(all);

            // 항목
            var ul = el('ul', 'cns-list');
            var boxes = [];
            terms.forEach(function (t) {
                var li = el('li', 'cns-item');
                var label = el('label', 'cns-item-check');
                var box = el('input');
                box.type = 'checkbox';
                box.checked = !!state.checked[t.termsCd];
                box.setAttribute('aria-label', t.termsNm + (page === 'REQ' ? ' (필수)' : ' (선택)') + ' 동의');
                box.addEventListener('change', function () {
                    state.checked[t.termsCd] = box.checked;
                    refresh();
                });
                boxes.push(box);
                label.appendChild(box);
                label.appendChild(el('span', 'cns-check', ''));
                li.appendChild(label);

                var open = el('button', 'cns-item-name');
                open.type = 'button';
                open.appendChild(el('span', page === 'REQ' ? 'cns-badge req' : 'cns-badge', page === 'REQ' ? '필수' : '선택'));
                open.appendChild(el('span', 'cns-item-text', t.termsNm));
                open.appendChild(el('span', 'cns-arrow', '›'));
                open.setAttribute('aria-label', t.termsNm + ' 내용 보기');
                open.addEventListener('click', function () { showDetail(t); });
                li.appendChild(open);
                ul.appendChild(li);
            });
            body.appendChild(ul);

            allBox.addEventListener('change', function () {
                terms.forEach(function (t, i) {
                    state.checked[t.termsCd] = allBox.checked;
                    boxes[i].checked = allBox.checked;
                });
                refresh();
            });

            function refresh() {
                var count = terms.filter(function (t) { return state.checked[t.termsCd]; }).length;
                allBox.checked = count === terms.length;
                // 필수 페이지: 모두 체크해야 다음 / 선택 페이지: 항상 가능
                btnNext.disabled = page === 'REQ' && count !== terms.length;
                var last = page === 'OPT' || state.list.optional.length === 0;
                btnNext.textContent = last ? '확인' : '다음';
            }
            refresh();
            title.focus();
        }

        // ---- 상세(전문) 보기
        function showDetail(t) {
            detail.textContent = '';
            var dHead = el('div', 'cns-head');
            var dBack = el('button', 'cns-icon-btn', '‹');
            dBack.type = 'button';
            dBack.setAttribute('aria-label', '목록으로');
            dBack.addEventListener('click', hideDetail);
            var dTitle = el('h2', 'cns-head-title', t.termsNm);
            dTitle.tabIndex = -1;
            dHead.appendChild(dBack);
            dHead.appendChild(dTitle);
            detail.appendChild(dHead);

            var dBody = el('div', 'cns-body');
            dBody.appendChild(el('p', 'cns-meta', 'v' + t.termsVer + ' · 시행일 ' + t.effectDt));
            var content = el('div', 'cns-content', '불러오는 중...');
            dBody.appendChild(content);
            detail.appendChild(dBody);

            var dFoot = el('div', 'cns-foot');
            var dAgree = el('button', 'cns-btn', '동의');
            dAgree.type = 'button';
            dAgree.addEventListener('click', function () {
                state.checked[t.termsCd] = true;
                hideDetail();
                render(state.page);
            });
            dFoot.appendChild(dAgree);
            detail.appendChild(dFoot);
            detail.hidden = false;
            dTitle.focus();

            // 목록에서 받은 버전 그대로 전문 조회 → 동의할 때도 같은 버전으로 보낸다
            request('GET', API + '/terms/' + encodeURIComponent(t.termsCd) + '/' + t.termsVer).then(function (res) {
                if (!res.success) { content.textContent = res.message; return; }
                content.textContent = res.data.termsCn;
                state.viewed[t.termsCd] = true;
                if (safeUrl(res.data.pdfUrl)) {
                    var a = el('a', 'cns-pdf', 'PDF 원문 보기');
                    a.href = res.data.pdfUrl;
                    a.target = '_blank';
                    a.rel = 'noopener';
                    dBody.appendChild(a);
                }
            }).catch(function () { content.textContent = '네트워크 오류로 약관을 불러오지 못했습니다.'; });
        }

        function hideDetail() {
            detail.hidden = true;
            detail.textContent = '';
            title.focus();
        }

        // ---- 버튼
        function goBack() {
            if (state.page === 'OPT' && state.list && state.list.required.length) {
                render('REQ');
            } else {
                cancel();
            }
        }
        btnBack.addEventListener('click', goBack);

        btnNext.addEventListener('click', function () {
            if (state.page === 'REQ' && state.list.optional.length) {
                render('OPT');
            } else {
                submit();
            }
        });

        // ---- 저장
        function submit() {
            var items = state.list.required.concat(state.list.optional).map(function (t) {
                return {
                    termsCd: t.termsCd,
                    termsVer: t.termsVer,
                    agreeYn: state.checked[t.termsCd] ? 'Y' : 'N',
                    viewYn: state.viewed[t.termsCd] ? 'Y' : 'N'
                };
            });
            btnNext.disabled = true;
            request('POST', API + '/' + encodeURIComponent(options.bizCd) + '/agree', {
                channel: options.channel || 'APP',
                items: items
            }).then(function (res) {
                if (res.success) {
                    close();
                    if (typeof options.onComplete === 'function') options.onComplete(res.data);
                    return;
                }
                if (res.code === 'CNS-102') {           // 개정됨 → 새 약관으로 처음부터
                    return showMessage(res.message).then(load);
                }
                btnNext.disabled = false;
                return showMessage(res.message);
            }).catch(function () {
                btnNext.disabled = false;
                showMessage('네트워크 오류로 저장하지 못했습니다. 다시 시도해 주세요.');
            });
        }

        load();
        return { close: close };
    }

    global.ConsentAgree = { open: open };
})(window);
