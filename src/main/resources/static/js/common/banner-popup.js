/**
 * =============================================================================
 *  [CF-BC-20 / 공통#39] 배너팝업 공통 컴포넌트  -  /js/common/banner-popup.js
 * -----------------------------------------------------------------------------
 *  게시일자가 겹치는 배너팝업이 2건 이상이면 ADMIN 표시순서대로 좌우 슬라이드로 넘겨 본다.
 *  (AS-IS: 표시순서를 지정해도 1건만 노출 → 일부 긴급공지 누락)
 *
 *  사용법 (APP 메인 화면):
 *    <script src="/js/common/banner-popup.js"></script>
 *    BannerPopup.open({ channel: 'APP' }).then(function (r) { ... });
 *
 *  옵션
 *    channel       노출 채널 (APP | PC_WEB | RECRUIT_WEB), 기본 APP
 *    items         배너 배열을 직접 넘기면 API 호출 생략 (ADMIN 미리보기 등)
 *    url           조회 API 변경 시 (기본 /api/common/banner-popups?channel=)
 *    container     팝업을 특정 영역 안에 띄울 때 (selector 또는 element). 없으면 화면 전체
 *    respectHidden "오늘 하루 보지 않기" 적용 여부 (기본 true)
 *    onLink(url)   [자세히 보기] 처리 함수 (기본: location.href 이동). APP 은 네이티브 브릿지 연결
 *
 *  결과 Promise → { shown: 노출 건수, viewed: 넘겨 본 배너 ID 목록, closedBy: 'close'|'hideToday'|'link'|'esc'|'none' }
 *
 *  - jQuery / Bootstrap 비의존, 화면 문자열은 textContent 로만 출력 (XSS 안전)
 *  - 스와이프(터치/마우스 드래그), 좌우 화살표 버튼, 키보드(←/→/Esc), 인디케이터("1 / 3" + 점) 지원
 *  - 자동 넘김 없음 (요구사항: 고객이 좌우로 넘겨 확인)
 * =============================================================================
 */
(function (global) {
    'use strict';

    var DEFAULT_URL = '/api/common/banner-popups?channel=';
    var HIDE_KEY = 'bannerPopup.hideToday.';
    var STYLE_ID = 'bnr-popup-style';
    var SWIPE_THRESHOLD = 0.18;   // 화면 너비의 18% 이상 끌면 다음/이전
    var CATEGORY_COLOR = {
        EMERGENCY_SYSTEM: '#c62828',
        EMERGENCY_GENERAL: '#ef6c00',
        NOTICE: '#1565c0',
        EVENT: '#2e7d32'
    };

    // ------------------------------------------------------------------ 스타일 (1회 주입)
    function injectStyle() {
        if (document.getElementById(STYLE_ID)) return;
        var css = [
            '.bnr-overlay{position:fixed;inset:0;z-index:2000;background:rgba(0,0,0,.55);display:flex;align-items:center;justify-content:center;padding:24px;}',
            '.bnr-overlay.bnr-in-container{position:absolute;z-index:50;}',
            '.bnr-dialog{background:#fff;border-radius:16px;width:100%;max-width:340px;overflow:hidden;box-shadow:0 12px 40px rgba(0,0,0,.3);outline:none;font-family:inherit;}',
            '.bnr-viewport{position:relative;overflow:hidden;touch-action:pan-y;user-select:none;-webkit-user-select:none;}',
            '.bnr-track{display:flex;transition:transform .3s ease;will-change:transform;}',
            '.bnr-track.bnr-dragging{transition:none;}',
            '.bnr-multi .bnr-slide{padding-left:34px;padding-right:34px;}',
            '.bnr-slide{flex:0 0 100%;min-width:0;padding:22px 20px 16px;box-sizing:border-box;min-height:220px;display:flex;flex-direction:column;}',
            '.bnr-badge{align-self:flex-start;font-size:11px;font-weight:700;color:#fff;border-radius:10px;padding:3px 9px;margin-bottom:10px;}',
            '.bnr-title{font-size:17px;font-weight:700;line-height:1.4;margin:0 0 10px;color:#111;word-break:keep-all;}',
            '.bnr-content{font-size:14px;line-height:1.6;color:#444;white-space:pre-line;margin:0;flex:1;word-break:keep-all;}',
            '.bnr-img{display:block;width:100%;border-radius:10px;margin-bottom:12px;}',
            '.bnr-link{margin-top:14px;align-self:stretch;border:1px solid #0d47a1;color:#0d47a1;background:#fff;border-radius:8px;padding:9px;font-size:14px;font-weight:700;cursor:pointer;}',
            '.bnr-link:active{background:#e3ecfa;}',
            '.bnr-arrow{position:absolute;top:50%;transform:translateY(-50%);width:26px;height:26px;border-radius:50%;border:none;background:rgba(0,0,0,.35);color:#fff;font-size:15px;line-height:26px;padding:0;cursor:pointer;z-index:2;}',
            '.bnr-arrow:disabled{opacity:.15;cursor:default;}',
            '.bnr-prev{left:4px;}.bnr-next{right:4px;}',
            '.bnr-indicator{display:flex;align-items:center;justify-content:center;gap:10px;padding:2px 0 12px;}',
            '.bnr-dots{display:flex;gap:6px;}',
            '.bnr-dot{width:7px;height:7px;border-radius:4px;border:none;padding:0;background:#c9ced6;cursor:pointer;transition:width .2s;}',
            '.bnr-dot[aria-current="true"]{width:18px;background:#0d47a1;}',
            '.bnr-count{font-size:12px;color:#666;font-variant-numeric:tabular-nums;}',
            '.bnr-count b{color:#0d47a1;}',
            '.bnr-footer{display:flex;border-top:1px solid #e5e7eb;}',
            '.bnr-footer button{flex:1;border:none;background:#fff;padding:14px 0;font-size:14px;cursor:pointer;}',
            '.bnr-footer .bnr-hide{color:#777;border-right:1px solid #e5e7eb;}',
            '.bnr-footer .bnr-close{color:#0d47a1;font-weight:700;}',
            '.bnr-sr{position:absolute;width:1px;height:1px;overflow:hidden;clip:rect(0 0 0 0);white-space:nowrap;}'
        ].join('\n');
        var style = document.createElement('style');
        style.id = STYLE_ID;
        style.textContent = css;
        document.head.appendChild(style);
    }

    // ------------------------------------------------------------------ 오늘 하루 보지 않기 (기기별 localStorage)
    function today() {
        var d = new Date();
        return d.getFullYear() + '-' + ('0' + (d.getMonth() + 1)).slice(-2) + '-' + ('0' + d.getDate()).slice(-2);
    }

    function readHidden(channel) {
        try {
            var v = JSON.parse(global.localStorage.getItem(HIDE_KEY + channel) || 'null');
            return v && v.date === today() && Array.isArray(v.ids) ? v.ids : [];
        } catch (e) {
            return [];   // 사생활 보호 모드 등 저장소 사용 불가 → 숨김 없음으로 동작
        }
    }

    function writeHidden(channel, ids) {
        try {
            var merged = readHidden(channel).concat(ids).filter(function (id, i, a) { return a.indexOf(id) === i; });
            global.localStorage.setItem(HIDE_KEY + channel, JSON.stringify({ date: today(), ids: merged }));
        } catch (e) { /* 저장 실패 시 무시 */ }
    }

    /** 서버 검증과 같은 기준: 같은 도메인 상대경로 또는 http/https */
    function safeUrl(url) {
        return typeof url === 'string' && (/^\/(?!\/)/.test(url) || /^https?:\/\//i.test(url));
    }

    // ------------------------------------------------------------------ DOM 유틸
    function el(tag, cls, text) {
        var e = document.createElement(tag);
        if (cls) e.className = cls;
        if (text != null) e.textContent = text;
        return e;
    }

    function resolveContainer(c) {
        if (!c) return null;
        return typeof c === 'string' ? document.querySelector(c) : c;
    }

    // ------------------------------------------------------------------ 팝업 렌더링
    function render(items, opts) {
        return new Promise(function (resolve) {
            injectStyle();
            var total = items.length;
            var index = 0;
            var viewed = [items[0].id];
            var container = resolveContainer(opts.container);
            var prevFocus = document.activeElement;

            var overlay = el('div', 'bnr-overlay' + (container ? ' bnr-in-container' : ''));
            var dialog = el('div', 'bnr-dialog' + (total > 1 ? ' bnr-multi' : ''));
            dialog.setAttribute('role', 'dialog');
            dialog.setAttribute('aria-modal', 'true');
            dialog.setAttribute('aria-roledescription', total > 1 ? '공지 슬라이드' : '공지');
            dialog.setAttribute('aria-label', '공지사항 ' + total + '건');
            dialog.tabIndex = -1;

            var viewport = el('div', 'bnr-viewport');
            var track = el('div', 'bnr-track');
            var live = el('div', 'bnr-sr');
            live.setAttribute('aria-live', 'polite');

            items.forEach(function (b, i) {
                var slide = el('div', 'bnr-slide');
                slide.setAttribute('role', 'group');
                slide.setAttribute('aria-roledescription', '슬라이드');
                slide.setAttribute('aria-label', (i + 1) + ' / ' + total);
                slide.setAttribute('data-banner-id', b.id);

                var badge = el('span', 'bnr-badge', b.categoryName || '공지');
                badge.style.background = CATEGORY_COLOR[b.category] || '#455a64';
                slide.appendChild(badge);

                if (b.imageUrl && safeUrl(b.imageUrl)) {
                    var img = el('img', 'bnr-img');
                    img.src = b.imageUrl;
                    img.alt = b.altText || '';
                    img.draggable = false;
                    slide.appendChild(img);
                }
                slide.appendChild(el('h2', 'bnr-title', b.title));
                slide.appendChild(el('p', 'bnr-content', b.content));

                if (b.linkUrl && safeUrl(b.linkUrl)) {
                    var link = el('button', 'bnr-link', '자세히 보기');
                    link.type = 'button';
                    link.addEventListener('click', function () {
                        if (moved) return;
                        finish('link');
                        if (typeof opts.onLink === 'function') opts.onLink(b.linkUrl, b);
                        else global.location.href = b.linkUrl;
                    });
                    slide.appendChild(link);
                }
                track.appendChild(slide);
            });
            viewport.appendChild(track);

            // 화살표 / 인디케이터는 2건 이상일 때만 (1건이면 기존과 같은 단건 팝업)
            var prev, next, dots = [], count;
            if (total > 1) {
                prev = el('button', 'bnr-arrow bnr-prev', '‹');
                prev.type = 'button';
                prev.setAttribute('aria-label', '이전 공지');
                next = el('button', 'bnr-arrow bnr-next', '›');
                next.type = 'button';
                next.setAttribute('aria-label', '다음 공지');
                prev.addEventListener('click', function () { go(index - 1); });
                next.addEventListener('click', function () { go(index + 1); });
                viewport.appendChild(prev);
                viewport.appendChild(next);
            }
            dialog.appendChild(viewport);

            if (total > 1) {
                var indicator = el('div', 'bnr-indicator');
                var dotWrap = el('div', 'bnr-dots');
                items.forEach(function (b, i) {
                    var d = el('button', 'bnr-dot');
                    d.type = 'button';
                    d.setAttribute('aria-label', (i + 1) + '번째 공지 보기');
                    d.addEventListener('click', function () { go(i); });
                    dots.push(d);
                    dotWrap.appendChild(d);
                });
                count = el('span', 'bnr-count');
                indicator.appendChild(dotWrap);
                indicator.appendChild(count);
                dialog.appendChild(indicator);
            }
            dialog.appendChild(live);

            var footer = el('div', 'bnr-footer');
            var hideBtn = el('button', 'bnr-hide', '오늘 하루 보지 않기');
            hideBtn.type = 'button';
            var closeBtn = el('button', 'bnr-close', '닫기');
            closeBtn.type = 'button';
            hideBtn.addEventListener('click', function () {
                writeHidden(opts.channel, items.map(function (b) { return b.id; }));
                finish('hideToday');
            });
            closeBtn.addEventListener('click', function () { finish('close'); });
            footer.appendChild(hideBtn);
            footer.appendChild(closeBtn);
            dialog.appendChild(footer);

            overlay.appendChild(dialog);
            if (container) {
                if (getComputedStyle(container).position === 'static') container.style.position = 'relative';
                container.appendChild(overlay);
            } else {
                document.body.appendChild(overlay);
            }

            // ---- 이동
            function go(i) {
                index = Math.max(0, Math.min(total - 1, i));
                track.style.transform = 'translateX(' + (-index * 100) + '%)';
                if (viewed.indexOf(items[index].id) < 0) viewed.push(items[index].id);
                if (total > 1) {
                    prev.disabled = index === 0;
                    next.disabled = index === total - 1;
                    dots.forEach(function (d, k) { d.setAttribute('aria-current', k === index ? 'true' : 'false'); });
                    count.textContent = '';
                    var cur = el('b', null, String(index + 1));
                    count.appendChild(cur);
                    count.appendChild(document.createTextNode(' / ' + total));
                    live.textContent = (index + 1) + ' / ' + total + ' ' + items[index].title;
                }
                Array.prototype.forEach.call(track.children, function (s, k) {
                    s.setAttribute('aria-hidden', k === index ? 'false' : 'true');
                });
            }

            // ---- 스와이프 (Pointer Events: 터치·마우스 공통)
            var startX = null, startY = 0, dx = 0, moved = false, pid = null;
            viewport.addEventListener('pointerdown', function (e) {
                if (total < 2 || e.target.closest('.bnr-arrow')) return;
                startX = e.clientX; startY = e.clientY; dx = 0; moved = false; pid = e.pointerId;
            });
            viewport.addEventListener('pointermove', function (e) {
                if (startX === null || e.pointerId !== pid) return;
                dx = e.clientX - startX;
                if (!moved && Math.abs(dx) > 6 && Math.abs(dx) > Math.abs(e.clientY - startY)) {
                    moved = true;
                    track.classList.add('bnr-dragging');
                    try { viewport.setPointerCapture(pid); } catch (ignore) { /* 일부 브라우저 미지원 */ }
                }
                if (moved) {
                    // 처음/마지막에서 바깥으로 끌면 저항감
                    var edge = (index === 0 && dx > 0) || (index === total - 1 && dx < 0);
                    var offset = edge ? dx / 3 : dx;
                    track.style.transform = 'translateX(calc(' + (-index * 100) + '% + ' + offset + 'px))';
                }
            });
            function endDrag(e) {
                if (startX === null || (e && e.pointerId !== pid)) return;
                track.classList.remove('bnr-dragging');
                if (moved) {
                    var w = viewport.clientWidth || 1;
                    if (dx <= -w * SWIPE_THRESHOLD) go(index + 1);
                    else if (dx >= w * SWIPE_THRESHOLD) go(index - 1);
                    else go(index);
                    // 드래그 직후 발생하는 click(자세히 보기) 무시
                    setTimeout(function () { moved = false; }, 0);
                }
                startX = null;
            }
            viewport.addEventListener('pointerup', endDrag);
            viewport.addEventListener('pointercancel', endDrag);

            // ---- 키보드 (←/→ 이동, Esc 닫기, Tab 은 팝업 안에서만 순환)
            function onKey(e) {
                if (e.key === 'ArrowLeft') { go(index - 1); e.preventDefault(); }
                else if (e.key === 'ArrowRight') { go(index + 1); e.preventDefault(); }
                else if (e.key === 'Escape') { finish('esc'); }
                else if (e.key === 'Tab') {
                    var f = Array.prototype.filter.call(dialog.querySelectorAll('button'), function (b) {
                        return !b.disabled && !b.closest('[aria-hidden="true"]');
                    });
                    if (!f.length) return;
                    var first = f[0], last = f[f.length - 1];
                    if (e.shiftKey && document.activeElement === first) { last.focus(); e.preventDefault(); }
                    else if (!e.shiftKey && document.activeElement === last) { first.focus(); e.preventDefault(); }
                }
            }
            dialog.addEventListener('keydown', onKey);

            var done = false;
            function finish(by) {
                if (done) return;
                done = true;
                overlay.parentNode && overlay.parentNode.removeChild(overlay);
                if (prevFocus && prevFocus.focus) { try { prevFocus.focus(); } catch (ignore) { /* noop */ } }
                var r = { shown: total, viewed: viewed.slice(), closedBy: by };
                if (typeof opts.onClose === 'function') opts.onClose(r);
                resolve(r);
            }

            go(0);
            dialog.focus();
        });
    }

    // ------------------------------------------------------------------ 공개 API
    var BannerPopup = {
        /**
         * 노출 대상 배너를 조회해 팝업을 띄운다. 노출할 배너가 없으면 즉시 { shown: 0, closedBy: 'none' }.
         */
        open: function (options) {
            var opts = Object.assign({ channel: 'APP', respectHidden: true }, options || {});
            var load = Array.isArray(opts.items)
                ? Promise.resolve(opts.items)
                : fetch((opts.url || DEFAULT_URL + encodeURIComponent(opts.channel)), {
                    credentials: 'same-origin',
                    headers: { 'X-Requested-With': 'XMLHttpRequest' }
                }).then(function (r) { return r.json(); }).then(function (res) {
                    if (!res || !res.success) throw res || new Error('배너팝업 조회 실패');
                    return res.data.items || [];
                });

            return load.then(function (items) {
                var hidden = opts.respectHidden ? readHidden(opts.channel) : [];
                var list = items.filter(function (b) { return hidden.indexOf(b.id) < 0; });
                if (!list.length) {
                    return { shown: 0, viewed: [], closedBy: 'none', hiddenCount: items.length - list.length };
                }
                return render(list, opts);
            });
        },

        /** "오늘 하루 보지 않기" 로 숨긴 배너 ID */
        hiddenIds: function (channel) {
            return readHidden(channel || 'APP');
        },

        /** "오늘 하루 보지 않기" 초기화 (테스트용) */
        resetHidden: function (channel) {
            try { global.localStorage.removeItem(HIDE_KEY + (channel || 'APP')); } catch (e) { /* noop */ }
        }
    };

    global.BannerPopup = BannerPopup;
})(window);
