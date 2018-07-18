/**
 * Biscuits.
 * User: liam.goodacre
 * Date: 16/01/13
 * Time: 17:24
 */
(function (window, undefined) {

    var PLUGIN_NAME = 'Biscuits';

    var ITERATOR_REG = /;\s?([^=]+)=([^;]*)/g;
    var daysFromNow = function (days) {
        var d = new Date;
        d.setTime(d.getTime() + (days * 24 * 60 * 60 * 1000));
        return d;
    };


    var api = {};

    api.add = function (key, value, days) {
        document.cookie = [
            key, '=', encodeURIComponent(JSON.stringify(value ? value : null)),
            (days ? '; expires=' + daysFromNow(days).toGMTString() : ''),
            '; path=/'].join('');
        return api;
    };

    api.addAll = function (obj) {
        for (var key in obj) {
            if (obj.hasOwnProperty(key)) {
                api.add(key, obj[key]);
            }
        }
        return api;
    };

    api.addAllDetail = function (obj) {
        for (var key in obj) {
            if (obj.hasOwnProperty(key)) {
                var val = obj[key];
                api.add(key, val.value, val.days);
            }
        }
        return api;
    };

    api.getAll = function () {
        var result = {};
        (';' + document.cookie + ';').replace(ITERATOR_REG, function (match, k, v) {
            result[k] = JSON.parse(decodeURIComponent(v));
        });
        return result;
    };

    api.get = function (key) {
        var all = api.getAll();
        return key in all ? all[key] : undefined;
    };

    api.del = function (key) {
        return api.add(key, '', -1);
    };

    api.delAll = function () {
        var all = api.getAll();
        for (var key in all) {
            if (all.hasOwnProperty(key)) {
                api.del(key);
            }
        }
        return api;
    };

    api.has = function (key) {
        return(';' + document.cookie).match(new RegExp(';\\s?' + key + '=')) !== null;
    };


    window[PLUGIN_NAME] = api;
    return api;

})(window);