jQuery(function ($) {

    /*  # Constants/Options  */

    var urls = {
        'login': 'login.json',
        'login_success': 'p/search.do',
        'register': 'register.json',
        'register_success': 'p/search.do',
        'sso_login': 'login.do',
        'getSettings': 'getLoginSettings.json'
    };

    var DEBUG_MODE = false;

    var VIEWED_COOKIE_MESSAGE = 'viewed_cookie_message';
    var POST = 'POST';
    var VISIBLE = 'visible';
    var HIDDEN = 'hidden';
    var ENTER_KEY = 13;
    var LOAD_ERROR_HTML = 'Error loading. <br> Refresh to try again.';
    var LOGIN_SETTINGS_KEY = 'loginSettings';

    var UNKNOWN_ERROR = 'An unknown error occurred.';
    var FAILED_REQUEST = 'Could not connect with server.';
    var ERROR_MESSAGES = {
        1: 'A user with that name already exists.',
        2: 'Registration failed: invalid username.',
        4: 'Username or password incorrect.',
        8: 'Could not read settings.',
        16: 'Login is not allowed.',
        32: 'Registration is not allowed.'
    };

    //  :: ParentElement -> Selector -> Element
    var context_ui = function (parent) {
        return function (sel) {
            return $(sel, parent);
        };
    };

    var id = function (v) {
        return v;
    };
    var debug_only = function (f) {
        return DEBUG_MODE ? f : id;
    };

    var log = debug_only(function () {
        return console.log.apply(undefined, arguments);
    });


    /*  # UI Elements  */

    var $ui_parent = $('.login-parent');
    var ui = context_ui($ui_parent);

    var $login_form = ui('.loginform');
    var $userpass_area = ui('.userpassArea');
    var $username = ui('.username');
    var $password = ui('.password');
    var $fields = $().add($username).add($password);
    var $reset = ui('.reset');
    var $login = ui('.login');
    var $register = ui('.register');
    var $sso_login = ui('.ssoLogin');
    var $activity_indicator = ui('.activityIndicator');
    var $working = $('.working', $activity_indicator);
    var $login_error = ui('.loginError');
    var $cookie_message = $('.cookieMessage');


    /*  # State  */

    var ajax_request = null;
    var indicator_timeout = null;


    /*  # Functionality  */

    /*  Display the working/activity indicator  */
    var show_indicator = function () {
        clearTimeout(indicator_timeout);
        $activity_indicator.addClass(VISIBLE);
    };

    /*  Fade out the working/activity indicator  */
    var hide_indicator = function () {
        indicator_timeout = setTimeout(function () {
            $activity_indicator.removeClass(VISIBLE);
            indicator_timeout = null;
        }, 200);
    };

    /*  Display an error alert, given an optional error number  */
    var show_error = function (id,code) {
        $login_error.text(code);
        $login_error.show();
    };
    /*  Display an error alert, given some error text to show  */
    var show_error_with_text = function (text) {
        $login_error.text(text || UNKNOWN_ERROR);
        $login_error.show();
    };

    /*  Hide the error alert  */
    var hide_error = function () {
        $login_error.hide();
    };

    /*  Give focus to some element, and select it's contents (textbox)  */
    var focus_field = function ($field) {
        return $field.focus().select();
    };

    /*  Reset the ui, clear input fields, hide errors, etc...  */
    var reset_ui = function () {
        $fields.val('');
        hide_indicator();
        hide_error();
        focus_field($username);
    };

    /*  Abort any ajax active request and clear the request handle for future requests  */
    var reset_ajax = function () {
        if (ajax_request) {
            ajax_request.abort();
        }
        ajax_request = null;
        hide_indicator();
    };

    /*  Given a callback, returns a function that when called
        only executes the callback if the ajax request handle
        is free.  */
    var if_can_ajax_then = function (callback) {
        return function () {
            if (ajax_request !== null) {
                log('Request currently in process.');
                return null;
            }
            return callback.apply(undefined, arguments);
        };
    };

    /*  Gathers the user's input data into an object.  */
    var get_input_data = function () {
        return({
            'username': $username.val(),
            'password': $password.val()
        });
    };

    /*  Redirects the browser to a url  */
    var redirect_to = function (url) {
        window.location.href = url;
    };

    /*  If login is successful  */
    var login_successful = function (res) {
        redirect_to(res.result || urls.login_success);
    };

    /*  If registration is successful  */
    var register_successful = function () {
        redirect_to(urls.register_success);
    };

    /*  Template for an ajax response callback.  */
    var response_function = function (success) {
        return function (response) {
            log("Response:", JSON.stringify(response));
            //  Successful response
            if (response.success) {
                success(response);
            }
            //  Unsuccessful response
            else {
                show_error(response.error_no,response.error_code);
                focus_field($username);
            }
        };
    };

    var login_response = response_function(login_successful);
    var register_response = response_function(register_successful);

    /*  On a failed ajax, show the failed request error alert  */
    var failed_ajax = function () {
        show_error_with_text(FAILED_REQUEST);
    };

    /*  Defines a function for performing ajax requests to a url, given an success callback.  */
    var ajax_action = function (url, callback) {
        return if_can_ajax_then(function () {
            show_indicator();
            hide_error();
            ajax_request = $.ajax({
                type: POST,
                url: url,
                data: get_input_data()
            });

            ajax_request.done(callback);
            ajax_request.fail(failed_ajax);
            ajax_request.always(reset_ajax);
        });
    };

    var attempt_login = ajax_action(urls.login, login_response);
    var attempt_register = ajax_action(urls.register, register_response);

    var login_with_sso = if_can_ajax_then(function () {
        window.location.href = urls.sso_login;
    });

    var error_loading_settings = function () {
        $working.html(LOAD_ERROR_HTML);
    };

    var configure_controls = function (settings) {
        if (settings.sso) {
            $sso_login.show();
        }
        else {
            $sso_login.remove();
        }
        if (settings.userpass) {
            $userpass_area.show();
            if (settings.canRegister) {
                $register.show();
            }
            else {
                $register.remove();
            }
        }
        else {
            $userpass_area.remove();
        }
    };

    /*  Setup the cookie message depending on the loaded settings  */
    var configure_cookie_message = CookieMessage.with_config({
        element: $cookie_message,
        cookieKey: VIEWED_COOKIE_MESSAGE,
        show: function ($elem) { $elem.removeClass(HIDDEN); },
        hide: function ($elem) { $elem.remove(); }
    });
    $cookie_message.find('.close').click(function () {
        Biscuits.add(VIEWED_COOKIE_MESSAGE, true);
        $cookie_message.remove();
    });

    /*  Configures controls, cookie message, form, indicator.  */
    var setup_ui = function (response) {
        if (!response.success || !response.result) {
            error_loading_settings();
        }
        else {
            var settings = response.result;
            configure_cookie_message(settings);
            configure_controls(settings);
            $login_form.addClass(VISIBLE);
            hide_indicator();
            reset_ui();
        }
    };

    /*  Attempts to retrieve the login settings.  */
    var getSettings = function () {
        if (window[LOGIN_SETTINGS_KEY]) {
            setup_ui(window[LOGIN_SETTINGS_KEY]);
        } else {
            ajax_action(urls.getSettings, setup_ui)();
        }
    };


    /*  # Init  */

    /*  ## UI Event Hooks  */

    $login.click(attempt_login);
    $register.click(attempt_register);
    $sso_login.click(login_with_sso);
    $reset.click(reset_ui);

    var on_enter_key = function (callback) {
        return function (e) {
            return (e.keyCode === ENTER_KEY) ? callback(e) : undefined;
        };
    };

    $username.keyup(on_enter_key(function () {
        focus_field($password);
        return false;
    }));

    $password.keyup(on_enter_key(function () {
        $login.focus();
        attempt_login();
        return false;
    }));


    getSettings();
});