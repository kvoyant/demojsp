/**
 * =============================================================================
 *  [CF-BC-20 / 공통#44] 홈 상단슬라이드배너  -  /js/common/top-slide-banner.js
 * -----------------------------------------------------------------------------
 *  [방향] 상품소개 배너 롤링 노출     [목적] 기존 메인 "슬라이드 배너팝업" 대체
 *  [정책] 제공대상 계좌 미보유 사용자 / 배너노출기간(ADMIN, 공통#45)
 *  [기능] 1. 최대 5건   2. 자동 슬라이딩 2초 (2~4건이면 빈 슬롯 숨김)
 *         3. 구조: 상품소개 메인텍스트 + 서브텍스트 + 배너 수
 *         4. 하단 Indicator 자동 변경 · 일시정지/재생 버튼 · 배너 수 "01/05"
 *
 *  사용법
 *    <link rel="stylesheet" href="/css/common/ui-tokens.css">
 *    <link rel="stylesheet" href="/css/common/top-slide-banner.css">
 *    <script src="/js/common/top-slide-banner.js"></script>
 *
 *    <div id="topSlide"></div>                         ← 홈 상단
 *    TopSlideBanner.mount('#topSlide', {
 *        channel: 'APP',
 *        onIneligible: function (data) { ... 계좌 보유 회원 → 같은 영역에 계좌정보 표시 ... }
 *    });
 *
 *  옵션
 *    channel        APP (기본) | PC_WEB | RECRUIT_WEB
 *    params         API 추가 파라미터 (local 샘플: { demoCustomer: 'customer02' })
 *    autoplay       자동 슬라이딩 (기본 true, 간격은 서버 응답 intervalMs = 2000)
 *    onLink(url, item)   배너 터치 시 (기본: location.assign) - APP 네이티브 화면전환 브릿지 연결
 *    onIneligible(data)  제공대상 아님 (계좌 보유) → 배너 영역 숨김 후 호출
 *    onLoad(data) / onEmpty(data) / onError(err) / onChange(index, item)
 *
 *  접근성 (KWCAG 2.2 / WCAG 2.2.2 움직이는 콘텐츠 정지 수단)
 *    - 일시정지/재생 버튼 제공, 키보드 포커스·마우스 올림·터치 중에는 자동 넘김 멈춤, 화면이 숨겨지면 멈춤
 *    - carousel/slide 역할·"3 / 5" 라벨, 사용자가 직접 넘길 때만 스크린리더 안내(aria-live)
 *    - 움직임 줄이기 설정 시 슬라이드 애니메이션 없이 전환
 *  - jQuery / Bootstrap 비의존, 문구는 textContent 로만 출력 (XSS 안전), 색은 서버 검증(#RRGGBB) 값만 사용
 *  - JSP 에서 사용하므로 템플릿 문자열(백틱 + 달러 중괄호)을 쓰지 않는다 (EL 충돌)
 * =============================================================================
 */
(function (global) {
    'use strict';

    var API = '/api/common/top-slide-banners';
    var HEX = /^#[0-9A-Fa-f]{6}$/;
    var SWIPE_PX = 40;
    var seq = 0;
    var instances = typeof WeakMap === 'function' ? new WeakMap() : null;

    function toEl(t) {
        if (!t) return null;
        if (typeof t === 'string') return document.querySelector(t);
        return t.nodeType === 1 ? t : null;
    }
    function el(tag, cls, text) {
        var e = document.createElement(tag);
        if (cls) e.className = cls;
        if (text != null) e.textContent = text;
        return e;
    }
    function pad2(n) { return n < 10 ? '0' + n : String(n); }
    function query(params) {
        var q = [];
        Object.keys(params || {}).forEach(function (k) {
            if (params[k] != null && params[k] !== '') q.push(encodeURIComponent(k) + '=' + encodeURIComponent(params[k]));
        });
        return q.join('&');
    }
    function reducedMotion() {
        return !!(global.matchMedia && global.matchMedia('(prefers-reduced-motion: reduce)').matches);
    }

    // ------------------------------------------------------------------ Carousel
    function Carousel(host, data, opts) {
        this.host = host;
        this.data = data;
        this.opts = opts;
        this.items = data.items || [];
        this.index = 0;
        this.timer = null;
        this.userPaused = false;     // 사용자가 [일시정지] 를 누름 (재생 누를 때까지 유지)
        this.holds = {};             // 일시 멈춤 사유 (hover / focus / touch / hidden)
        this.interval = Math.max(1000, data.intervalMs || 2000);
        this.build();
    }

    Carousel.prototype.build = function () {
        var self = this;
        var n = this.items.length;
        var id = 'ui-tsb-' + (++seq);
        var root = el('section', 'ui-tsb' + (n > 1 ? ' is-multi' : ''));
        root.setAttribute('aria-roledescription', 'carousel');
        root.setAttribute('aria-label', '상품 안내 배너');
        root.id = id;

        var viewport = el('div', 'ui-tsb__viewport');
        var track = el('ul', 'ui-tsb__track');
        track.id = id + '-track';
        track.setAttribute('aria-live', 'off');
        // 기능2 "2·3·4개 셋팅 시 빈 슬롯은 숨김": 받은 건수만큼만 슬라이드를 만든다 (고정 5칸 아님)
        this.slides = this.items.map(function (item, i) { return self.slide(item, i, n); });
        this.slides.forEach(function (s) { track.appendChild(s); });
        viewport.appendChild(track);
        root.appendChild(viewport);
        this.track = track;

        // 기능4 하단 Indicator: [일시정지/재생] + 배너 수 "01/05" (2건 이상일 때만)
        if (n > 1) {
            var ind = el('div', 'ui-tsb__indicator');
            var toggle = el('button', 'ui-tsb__toggle');
            toggle.type = 'button';
            toggle.setAttribute('aria-controls', track.id);
            toggle.appendChild(el('span', 'ui-tsb__toggle-icon'));
            toggle.addEventListener('click', function () { self.setUserPaused(!self.userPaused); });
            var count = el('span', 'ui-tsb__count');
            count.setAttribute('aria-hidden', 'true');      // 슬라이드 라벨("3 / 5")로 이미 안내
            var cur = el('b', 'ui-tsb__current', '01');
            count.appendChild(cur);
            count.appendChild(document.createTextNode('/' + pad2(n)));
            ind.appendChild(toggle);
            ind.appendChild(count);
            root.appendChild(ind);
            this.toggle = toggle;
            this.current = cur;

            this.bindGestures(viewport);
            root.addEventListener('keydown', function (e) {
                if (e.key === 'ArrowRight') { self.go(self.index + 1, true); e.preventDefault(); }
                else if (e.key === 'ArrowLeft') { self.go(self.index - 1, true); e.preventDefault(); }
            });
            // 마우스 올림 · 키보드 포커스 동안 멈춤 (읽는 중 넘어가지 않게)
            root.addEventListener('mouseenter', function () { self.hold('hover', true); });
            root.addEventListener('mouseleave', function () { self.hold('hover', false); });
            root.addEventListener('focusin', function () { self.hold('focus', true); });
            root.addEventListener('focusout', function (e) { if (!root.contains(e.relatedTarget)) self.hold('focus', false); });
            this.onVisibility = function () { self.hold('hidden', document.hidden); };
            document.addEventListener('visibilitychange', this.onVisibility);
        }

        this.root = root;
        this.host.textContent = '';
        this.host.appendChild(root);
        this.host.hidden = false;
        this.go(0, false);
        if (n > 1 && this.opts.autoplay !== false) this.schedule();
        this.syncToggle();
    };

    Carousel.prototype.slide = function (item, i, n) {
        var self = this;
        var li = el('li', 'ui-tsb__slide');
        li.setAttribute('role', 'group');
        li.setAttribute('aria-roledescription', 'slide');
        li.setAttribute('aria-label', (i + 1) + ' / ' + n);
        if (HEX.test(item.backgroundColor || '')) li.style.backgroundColor = item.backgroundColor;
        if (HEX.test(item.textColor || '')) li.style.color = item.textColor;
        li.setAttribute('data-tone', (item.textColor || '').toUpperCase() === '#FFFFFF' ? 'light' : 'dark');

        var box;
        if (item.linkUrl) {
            box = el('a', 'ui-tsb__link');
            box.href = item.linkUrl;
            box.addEventListener('click', function (e) {
                if (self.dragged) { e.preventDefault(); return; }       // 스와이프 끝의 클릭은 무시
                if (typeof self.opts.onLink === 'function') { e.preventDefault(); self.opts.onLink(item.linkUrl, item); }
            });
        } else {
            box = el('div', 'ui-tsb__link');
        }
        var text = el('span', 'ui-tsb__text');
        text.appendChild(el('strong', 'ui-tsb__main', item.mainText));
        if (item.subText) text.appendChild(el('span', 'ui-tsb__sub', item.subText));
        box.appendChild(text);
        if (item.imageUrl) {
            var img = el('img', 'ui-tsb__img');
            img.src = item.imageUrl;
            img.alt = item.altText || '';
            img.decoding = 'async';
            img.loading = i === 0 ? 'eager' : 'lazy';
            img.addEventListener('error', function () { img.remove(); }, { once: true });
            box.appendChild(img);
        }
        li.appendChild(box);
        return li;
    };

    /** i 번째로 이동 (끝 → 처음 순환). byUser=true 면 스크린리더 안내 + 자동 넘김 타이머 재시작 */
    Carousel.prototype.go = function (i, byUser) {
        var n = this.slides.length;
        if (!n) return;
        this.index = ((i % n) + n) % n;
        this.track.style.transform = 'translateX(' + (-100 * this.index) + '%)';
        var self = this;
        this.slides.forEach(function (s, k) {
            var on = k === self.index;
            s.setAttribute('aria-hidden', on ? 'false' : 'true');
            s.classList.toggle('is-active', on);
            // 보이지 않는 슬라이드의 링크로 Tab 이동 방지
            var link = s.querySelector('a');
            if (link) { if (on) link.removeAttribute('tabindex'); else link.setAttribute('tabindex', '-1'); }
        });
        if (this.current) this.current.textContent = pad2(this.index + 1);      // "01" 자동 변경
        this.track.setAttribute('aria-live', byUser ? 'polite' : 'off');
        if (byUser) this.schedule();
        if (typeof this.opts.onChange === 'function') this.opts.onChange(this.index, this.items[this.index]);
    };

    Carousel.prototype.playing = function () {
        if (this.slides.length < 2 || this.opts.autoplay === false || this.userPaused) return false;
        var h = this.holds;
        return !Object.keys(h).some(function (k) { return h[k]; });
    };

    Carousel.prototype.schedule = function () {
        var self = this;
        clearTimeout(this.timer);
        this.timer = null;
        if (!this.playing()) return;
        this.timer = setTimeout(function () { self.go(self.index + 1, false); self.schedule(); }, this.interval);
    };

    Carousel.prototype.hold = function (reason, on) {
        this.holds[reason] = !!on;
        this.schedule();
        this.syncToggle();
    };

    Carousel.prototype.setUserPaused = function (paused) {
        this.userPaused = !!paused;
        this.schedule();
        this.syncToggle();
    };

    Carousel.prototype.syncToggle = function () {
        if (!this.toggle) return;
        var paused = this.userPaused || this.opts.autoplay === false;
        this.toggle.classList.toggle('is-paused', paused);
        this.toggle.setAttribute('aria-label', paused ? '자동 넘김 재생' : '자동 넘김 일시정지');
        this.root.classList.toggle('is-playing', this.playing());
    };

    Carousel.prototype.bindGestures = function (viewport) {
        var self = this;
        var startX = null, startY = null, dx = 0, pid = null;
        viewport.addEventListener('pointerdown', function (e) {
            if (e.pointerType === 'mouse' && e.button !== 0) return;
            startX = e.clientX; startY = e.clientY; dx = 0; pid = e.pointerId;
            self.dragged = false;
            self.hold('touch', true);
            try { viewport.setPointerCapture(e.pointerId); } catch (ignore) { /* 미지원 브라우저 */ }
        });
        // 링크·이미지의 브라우저 기본 끌기(drag)가 시작되면 pointercancel 로 스와이프가 끊기므로 막는다
        viewport.addEventListener('dragstart', function (e) { e.preventDefault(); });
        viewport.addEventListener('pointermove', function (e) {
            if (startX === null || e.pointerId !== pid) return;
            dx = e.clientX - startX;
            if (!self.dragged && Math.abs(dx) > 8 && Math.abs(dx) > Math.abs(e.clientY - startY)) {
                self.dragged = true;
                self.root.classList.add('is-dragging');
            }
            if (self.dragged) {
                var w = viewport.clientWidth || 1;
                if (Math.abs(dx) > w) dx = dx > 0 ? w : -w;
                self.track.style.transform = 'translateX(calc(' + (-100 * self.index) + '% + ' + dx + 'px))';
            }
        });
        function end() {
            if (startX === null) return;
            self.root.classList.remove('is-dragging');
            if (self.dragged && Math.abs(dx) >= SWIPE_PX) self.go(self.index + (dx < 0 ? 1 : -1), true);
            else self.go(self.index, false);
            startX = null; pid = null;
            self.hold('touch', false);
            // click 이벤트가 pointerup 뒤에 오므로 잠시 뒤 해제
            setTimeout(function () { self.dragged = false; }, 0);
        }
        viewport.addEventListener('pointerup', end);
        viewport.addEventListener('pointercancel', end);
        viewport.addEventListener('pointerleave', function (e) { if (e.pointerType === 'mouse') end(); });
    };

    Carousel.prototype.destroy = function () {
        clearTimeout(this.timer);
        if (this.onVisibility) document.removeEventListener('visibilitychange', this.onVisibility);
        this.host.textContent = '';
    };

    // ------------------------------------------------------------------ public
    function render(target, data, opts) {
        var host = toEl(target);
        if (!host) return null;
        opts = opts || {};
        var prev = instances ? instances.get(host) : host.__tsb;
        if (prev) prev.destroy();
        if (!data || !data.eligible || !data.items || !data.items.length) {
            host.textContent = '';
            host.hidden = true;
            return null;
        }
        if (reducedMotion()) host.classList.add('ui-tsb-reduced');
        var c = new Carousel(host, data, opts);
        if (instances) instances.set(host, c); else host.__tsb = c;
        return c;
    }

    function mount(target, opts) {
        var host = toEl(target);
        if (!host) return Promise.reject(new Error('TopSlideBanner: 대상 요소 없음 ' + target));
        opts = Object.assign({ channel: 'APP' }, opts || {});
        host.hidden = true;
        var url = API + '?' + query(Object.assign({ channel: opts.channel }, opts.params || {}));
        return fetch(url, { credentials: 'same-origin', headers: { 'X-Requested-With': 'XMLHttpRequest', 'Accept': 'application/json' } })
            .then(function (r) { return r.json(); })
            .then(function (res) {
                if (!res || !res.success) throw Object.assign(new Error((res && res.message) || '배너 조회 실패'), { response: res });
                var data = res.data;
                render(host, data, opts);
                if (typeof opts.onLoad === 'function') opts.onLoad(data);
                if (!data.eligible) { if (typeof opts.onIneligible === 'function') opts.onIneligible(data); }
                else if (!data.total && typeof opts.onEmpty === 'function') opts.onEmpty(data);
                return data;
            })
            .catch(function (e) {
                render(host, null, opts);           // 홈 화면은 막지 않는다
                if (typeof opts.onError === 'function') opts.onError(e);
                return null;
            });
    }

    /** 현재 상태 (테스트·디버깅용) */
    function state(target) {
        var host = toEl(target);
        var c = host && (instances ? instances.get(host) : host.__tsb);
        if (!c) return null;
        return { index: c.index, total: c.slides.length, playing: c.playing(), userPaused: c.userPaused, interval: c.interval };
    }

    global.TopSlideBanner = {
        mount: mount,
        render: render,
        state: state,
        pause: function (t) { var h = toEl(t), c = h && (instances ? instances.get(h) : h.__tsb); if (c) c.setUserPaused(true); },
        play: function (t) { var h = toEl(t), c = h && (instances ? instances.get(h) : h.__tsb); if (c) c.setUserPaused(false); }
    };
})(window);
