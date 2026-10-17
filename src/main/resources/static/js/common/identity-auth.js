/**
 * =============================================================================
 *  [CF-BC-03] 본인인증 공통 JS  -  /js/common/identity-auth.js
 * -----------------------------------------------------------------------------
 *  업무 화면은 IdentityAuth.open() 한 줄만 호출하면 된다.
 *  인증기관이 NICE 든 사설인증이든 화면 코드는 바뀌지 않는다.
 *  (jQuery / common.js 에 의존하지 않는 순수 JS - 어느 화면에서나 단독 포함 가능)
 *
 *  ※ 기존 NICE 화면(/common/auth/nice-cert, common.js)과는 독립적이다.
 *
 *  사용 예)
 *    IdentityAuth.open({
 *        method : 'NICE_MOBILE',
 *        channel: 'PC_WEB',               // APP | PC_WEB | RECRUIT_WEB
 *        purpose: 'SIGN_UP',
 *        agreedTerms: ['NICE_PRIVACY', ...],
 *        returnUrl: '/member/join/step2'   // APP 채널만 사용
 *    }).then(function (result) {           // 팝업 채널: 인증창이 닫히면 resolve
 *        // result = { success, purpose, code, message }
 *    }).catch(function (err) {             // 시작 단계 오류 (약관 미동의 등)
 *        // err = { success:false, code, message }  ← 서버 ApiResponse 형식
 *    });
 *
 *  동작 순서
 *    1) (팝업 채널) 클릭 즉시 빈 팝업을 먼저 연다  ← 비동기 호출 뒤에 열면 브라우저 팝업차단에 걸림
 *    2) POST /api/common/identity/start → ApiResponse.data = { actionUrl, httpMethod, formParams, popup }
 *    3) form 을 동적으로 만들어 팝업(또는 현재창)으로 submit
 *    4) 인증 완료 → callback.jsp 가 opener 로 postMessage → 여기서 Promise resolve
 * =============================================================================
 */
(function (window, document) {
    'use strict';

    var API_BASE = '/api/common/identity';
    var POPUP_NAME = 'identityAuthPopup';
    var POPUP_FEATURES = 'width=500,height=650,top=100,left=100,scrollbars=yes,resizable=yes';
    var MESSAGE_TYPE = 'COMMON_IDENTITY_RESULT';   // callback.jsp 와 동일해야 함

    /** 현재 진행 중인 인증의 resolve 함수 (동시에 1건만 진행) */
    var pendingResolve = null;

    /** 팝업(callback.jsp)에서 보낸 결과 수신 */
    window.addEventListener('message', function (event) {
        // 같은 출처(우리 도메인)에서 온 메시지만 신뢰 - 다른 사이트가 보낸 가짜 결과 차단
        if (event.origin !== window.location.origin) return;
        var data = event.data || {};
        if (data.type !== MESSAGE_TYPE || !pendingResolve) return;
        var resolve = pendingResolve;
        pendingResolve = null;
        resolve(data);
    });

    /** 서버 응답(ApiResponse)을 풀어서 data 만 반환, 실패면 reject */
    function unwrap(res) {
        return res.json().catch(function () {
            return { success: false, code: 'HTTP_' + res.status, message: '서버 응답을 처리할 수 없습니다.' };
        }).then(function (body) {
            if (!res.ok || !body || !body.success) throw body;   // { success:false, code, message }
            return body.data;
        });
    }

    /** hidden form 을 만들어 target 창으로 submit */
    function submitForm(actionUrl, httpMethod, params, target) {
        var form = document.createElement('form');
        form.method = httpMethod;
        form.action = actionUrl;
        form.target = target;
        form.style.display = 'none';
        Object.keys(params).forEach(function (name) {
            var input = document.createElement('input');
            input.type = 'hidden';
            input.name = name;
            input.value = params[name];
            form.appendChild(input);
        });
        document.body.appendChild(form);
        form.submit();
        document.body.removeChild(form);
    }

    function open(options) {
        var isPopupChannel = options.channel !== 'APP';

        // 1) 사용자 클릭 이벤트 안에서 동기적으로 팝업을 먼저 연다 (팝업 차단 회피)
        var popup = null;
        if (isPopupChannel) {
            popup = window.open('about:blank', POPUP_NAME, POPUP_FEATURES);
            if (!popup) {
                return Promise.reject({ success: false, code: 'POPUP_BLOCKED', message: '팝업 차단을 해제해 주세요.' });
            }
        }

        // 2) 인증 시작 API 호출
        return fetch(API_BASE + '/start', {
            method: 'POST',
            credentials: 'same-origin',
            headers: { 'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest' },
            // Spring Security CSRF 도입 시 헤더 추가: 'X-CSRF-TOKEN': token
            body: JSON.stringify({
                method: options.method,
                channel: options.channel,
                purpose: options.purpose,
                returnUrl: options.returnUrl || null,
                agreedTerms: options.agreedTerms || [],
                options: options.options || {}
            })
        }).then(unwrap).then(function (start) {
            // 3) 인증창으로 이동
            if (start.popup) {
                submitForm(start.actionUrl, start.httpMethod, start.formParams, POPUP_NAME);
                // 4) 결과 대기 (사용자가 팝업을 그냥 닫는 경우도 감지)
                return new Promise(function (resolve) {
                    pendingResolve = resolve;
                    var timer = setInterval(function () {
                        if (popup.closed) {
                            clearInterval(timer);
                            // postMessage 가 먼저 도착했다면 pendingResolve 는 이미 null
                            if (pendingResolve === resolve) {
                                pendingResolve = null;
                                resolve({ success: false, code: 'USER_CLOSED', message: '본인인증이 취소되었습니다.' });
                            }
                        }
                    }, 500);
                });
            }
            // APP(WebView): 현재 창에서 이동. 완료 후 callback.jsp 가 returnUrl 로 보내준다.
            submitForm(start.actionUrl, start.httpMethod, start.formParams, '_self');
            return new Promise(function () {}); // 페이지가 떠나므로 resolve 되지 않음
        }).catch(function (err) {
            if (popup && !popup.closed) popup.close();
            throw err;
        });
    }

    /** 약관 목록 조회 (약관 동의/상세보기 화면 구성용) */
    function getTerms(method) {
        return fetch(API_BASE + '/terms?method=' + encodeURIComponent(method), { credentials: 'same-origin' })
            .then(unwrap);
    }

    /** 인증 결과(마스킹) 조회. 미인증이면 null */
    function getResult(purpose) {
        return fetch(API_BASE + '/result?purpose=' + encodeURIComponent(purpose), { credentials: 'same-origin' })
            .then(unwrap);
    }

    window.IdentityAuth = { open: open, getTerms: getTerms, getResult: getResult };
})(window, document);
