/**
 * 제2금융권 소매금융 시스템 공통 JavaScript
 */

var Common = {

    /**
     * 금융 공통 AJAX Wrapper
     */
    ajax: function(options) {
        var defaults = {
            type: 'GET',
            dataType: 'json',
            showLoading: true,
            errorAlert: true
        };

        var settings = $.extend({}, defaults, options);
        var httpMethod = settings.type ? settings.type.toUpperCase() : 'GET';

        var ajaxOptions = {
            url: settings.url,
            type: httpMethod,
            dataType: settings.dataType
        };

        if (httpMethod === 'GET' || httpMethod === 'HEAD') {
            ajaxOptions.data = settings.data;
        } else {
            ajaxOptions.contentType = settings.contentType || 'application/json; charset=utf-8';
            if (typeof settings.data === 'object' && settings.data !== null) {
                ajaxOptions.data = JSON.stringify(settings.data);
            } else {
                ajaxOptions.data = settings.data;
            }
        }

        if (settings.showLoading) {
            Common.showLoading();
        }

        ajaxOptions.success = function(res) {
            if (settings.showLoading) Common.hideLoading();

            if (res && res.success) {
                if (typeof settings.success === 'function') {
                    settings.success(res.data, res);
                }
            } else {
                if (settings.errorAlert) {
                    Common.alert((res && res.message) ? res.message : '처리 중 오류가 발생했습니다.', { type: 'error' });
                }
                if (typeof settings.fail === 'function') {
                    settings.fail(res);
                }
            }
        };

        ajaxOptions.error = function(xhr, status, error) {
            if (settings.showLoading) Common.hideLoading();

            if (xhr.status === 401) {
                Common.alert('세션이 만료되었습니다. 다시 로그인해 주세요.', { type: 'warning' }).then(function () {
                    location.href = '/login';
                });
            } else if (xhr.status === 403) {
                Common.alert('해당 메뉴 또는 기능에 대한 접근 권한이 없습니다.', { type: 'warning' });
            } else {
                var errorRes = xhr.responseJSON;
                var msg = (errorRes && errorRes.message) ? errorRes.message : '시스템 오류가 발생했습니다. 관리자에게 문의하세요.';
                Common.alert(msg, { type: 'error' });
            }

            if (typeof settings.error === 'function') {
                settings.error(xhr, status, error);
            }
        };

        return $.ajax(ajaxOptions);
    },

    /**
     * [공통#42] 공통 알럿 - /js/common/ui-alert.js 가 포함된 화면이면 공통 Alert, 아니면 브라우저 기본 alert.
     * 항상 Promise 를 반환하므로 닫힌 뒤 처리는 .then() 으로 작성한다.
     */
    alert: function(message, opts) {
        if (window.UiAlert) return window.UiAlert.alert(message, opts);
        window.alert(message);
        return Promise.resolve();
    },

    /**
     * [공통#42] 공통 확인창 - Promise<boolean> (ui-alert.js 미포함 화면은 브라우저 기본 confirm)
     */
    confirm: function(message, opts) {
        if (window.UiAlert) return window.UiAlert.confirm(message, opts);
        return Promise.resolve(window.confirm(message));
    },

    /**
     * 로딩 바 제어
     */
    showLoading: function() {
        if ($('#global-loading-spinner').length === 0) {
            $('body').append('<div id="global-loading-spinner" style="position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,0.3);z-index:9999;display:flex;align-items:center;justify-content:center;"><div style="color:white;font-weight:bold;font-size:18px;">처리 중입니다...</div></div>');
        } else {
            $('#global-loading-spinner').show();
        }
    },

    hideLoading: function() {
        $('#global-loading-spinner').hide();
    },

    /**
     * 숫자에 3자리마다 콤마 추가 (금액 표시)
     */
    formatNumber: function(num) {
        if (!num && num !== 0) return '';
        return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
    },

    /**
     * 콤마 제거 후 숫자만 반환
     */
    unformatNumber: function(str) {
        if (!str) return 0;
        return parseInt(str.toString().replace(/,/g, ''), 10) || 0;
    }
};
