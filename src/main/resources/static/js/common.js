/**
 * 제2금융권 소매금융 시스템 공통 JavaScript
 */

var Common = {

    /**
     * 금융 공통 AJAX Wrapper
     */
    ajax: function(options) {
        var defaults = {
            type: 'POST',
            contentType: 'application/json; charset=utf-8',
            dataType: 'json',
            showLoading: true,
            errorAlert: true
        };

        var settings = $.extend({}, defaults, options);

        if (settings.showLoading) {
            Common.showLoading();
        }

        return $.ajax({
            url: settings.url,
            type: settings.type,
            contentType: settings.contentType,
            data: typeof settings.data === 'object' ? JSON.stringify(settings.data) : settings.data,
            dataType: settings.dataType,
            success: function(res) {
                if (settings.showLoading) Common.hideLoading();

                if (res.success) {
                    if (typeof settings.success === 'function') {
                        settings.success(res.data, res);
                    }
                } else {
                    if (settings.errorAlert) {
                        alert(res.message || '처리 중 오류가 발생했습니다.');
                    }
                    if (typeof settings.fail === 'function') {
                        settings.fail(res);
                    }
                }
            },
            error: function(xhr, status, error) {
                if (settings.showLoading) Common.hideLoading();

                if (xhr.status === 401) {
                    alert('세션이 만료되었습니다. 다시 로그인해 주세요.');
                    location.href = '/login';
                } else if (xhr.status === 403) {
                    alert('해당 메뉴 또는 기능에 대한 접근 권한이 없습니다.');
                } else {
                    var errorRes = xhr.responseJSON;
                    var msg = (errorRes && errorRes.message) ? errorRes.message : '시스템 오류가 발생했습니다. 관리자에게 문의하세요.';
                    alert(msg);
                }

                if (typeof settings.error === 'function') {
                    settings.error(xhr, status, error);
                }
            }
        });
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
