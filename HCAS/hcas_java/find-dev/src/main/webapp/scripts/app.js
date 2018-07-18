//  When the dom is ready
jQuery(function($) {

    var VIEWED_COOKIE_MESSAGE = 'viewed_cookie_message';
    var HIDDEN = 'hidden';
    
    var helpWin;

	/**
	 * Prevents an event from resulting in default behaviour.
	 */
	var preventEvent = function(e) {
		e.preventDefault();
		return false;
	};

	var $window = window,
		$scrollToTop = $('#scrollToTop'),
		$containerInfo = $('.content, .hasHelp'),
        $cookie_message = $('.cookieMessage');

	/**
	 * Label the link to the current page as active, and deactivate the link.
	 */
	(function() {
		// get the window's location (without any hashes or searches)
		var location = window.location.href.replace(/#.*$/g, '');

		$('.navLink').each(function() {
			if (this.href === location) {
				$(this).click(preventEvent).parent().addClass('active');
			}
		});
	})();

    $(document).ajaxError(function(evt, jqXHR, ajaxSettings, thrownError){
        var errorText = (jqXHR.responseJSON && jqXHR.responseJSON && jqXHR.responseJSON.errorDetail) || jqXHR.responseText;
        
        
        if (errorText && errorText.indexOf("securityinfo token has expired") !== -1) {
                        
            $('#confirmDialog').confirmDialog({
                title: 'Security Error',
                message: "Security token has expired. Login again to acquire an updated token",
                callbackObj: null,
                yesBtnClass: 'btn-primary',
                yesLabel: 'OK',
                yesCallback: function() {
                    window.location = '../loginpage.do';
                },
                noLabel: null
            });
        } else if (jqXHR.status === 403) {
        // the page has expired, redirect to login page
            window.location = '../loginpage.do';
        } 
        
    });

	/**
	 * Ensure the hash value is preserved between pages
	 */
	$('.navLink').click(function(e) {
		e.preventDefault();
		if ($(this).data('logout')) {
		    $('#loadingDialog').find('.title').text('Logging out...');
		    $('#loadingDialog').modal('show');
		    window.location = this.href;
		} else {
		    window.location = this.href + window.location.hash;
		}
	});

	/**
	 * Prevent non-forms from submitting
	 */
	$('.nonForm').submit(preventEvent);

	/**
	 * Clicking the scroll to top button will scroll all visible elements with
	 * the 'scrollableToTop' property
	 */
	$scrollToTop.click(function(e) {
		e.preventDefault();
		$('.scrollableToTop:visible').animate({
			scrollTop : 0,
			duration : 200
		});
	});
	
	/**
	 * TODO: Explanation
	 */
	$scrollToTop.focus(function() {
		$(this).blur();
	});
	
	/**
	 * Fade in and out the scroll to top button depending
	 * on how far down the scrollable box we are.
	 * 
	 * TODO: Fix the looping?
	 */
	$('.scrollableToTop:visible').scroll(function() {
		if ($(this).scrollTop() <= '100') {
			$('#scrollToTop').removeClass('shown');
		} else {
			$('#scrollToTop').addClass('shown');
		}
	});
	
	var infonomyState = false;
	
	var hcVersion = "1.0.0.0 b1087422";
	
	$('#helpAbout').click(function() {
	    $('#helpAboutDialog').modal('show');
	    
	    return false;
	});
	
	function getHelpFileName() {
		var helpFileName = "../docs/HC_UI_helpfile.htm";		
		if(HelpFile) {
			helpFileName = HelpFile;
		}
		return helpFileName;
	}
	
	
	$('#helpButton').click(function() {
	    if (!helpWin || helpWin.closed) {
	        helpWin = $window.open(getHelpFileName(), $(this).text(), "top=200,left=300,width=800,height=600");
	    } else {
	        helpWin.focus();
	    }
	});
	
	var shower = _.debounce(function () {
		if (infonomyState) {
			$containerInfo.infonomy('show');
		}
	}, 500);
	
	var tryHideShow = function () {
		if (infonomyState) {
			$containerInfo.infonomy('hide');
			shower();
		}
	};
	
	$('.nav.nav-tabs a').live('click', function () {
		setTimeout(tryHideShow, 100);
	});
	
	$(window).resize(tryHideShow);


    var COOKIE_MESSAGE_OPTIONS = {
        element: $cookie_message,
        cookieKey: VIEWED_COOKIE_MESSAGE,
        show: function ($elem) { $elem.removeClass(HIDDEN); },
        hide: function ($elem) { $elem.remove(); }
    };
    var LOGIN_SETTINGS_URL = '../getLoginSettings.json';
    CookieMessage.with_url(COOKIE_MESSAGE_OPTIONS)(LOGIN_SETTINGS_URL, 'POST');
    $('.cookieMessage .close').click(function () {
        Biscuits.add(VIEWED_COOKIE_MESSAGE, true);
        $cookie_message.hide();
    });
    
});
