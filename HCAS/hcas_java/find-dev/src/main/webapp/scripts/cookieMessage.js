jQuery(function ($) {

    var API_NAME = 'CookieMessage';
    var api = {};


    //  Options Object -> Config Object -> Cookie Magic
    api.with_config = function (options) {
        return function (config) {
            var $elem = options.element;
            if (config.cookieMessageDisplayed && !Biscuits.has(options.cookieKey)) {
                $elem.find('.text').text(config.cookieMessage);
                options.show($elem);
            }
            else {
                options.hide($elem);
            }
        };
    };

    //  Options Object -> (Url String, Type String) -> Cookie Magic
    api.with_url = function (options) {
        return function (url, type) {
            var request = $.ajax({ url: url, type: type });
            request.done(function (response) {
                api.with_config(options)(response.result);
            });
            request.fail(function () {
                api.with_config(options)({});
            });
        };
    };


    window[API_NAME] = api;

});