var iFrameViewer = {};

//  When the dom is ready
jQuery(function($) {

    //  Prevent use on ipad, ipod, and iphone
    var useColorbox = !navigator.userAgent.match(/ipad|ipod|iphone/i);

    var RESOURCE_PREFIX = 'resource:';
    var RESOURCE_VIEWER_URL = 'demo';
    var frameSize = '90%';
    var iFrameId = _.uniqueId('iframeViewer');

    var preventLinkTraversal = 500;
    var disableLinkDuration = 0;
    var unloadAttempted = false;
    var preventRewriteInterval = null;
    var isOpen = false;

    //  Check support for availability of sandbox property (Chrome)
    //  :: Boolean
    var sandbox = 'sandbox' in document.createElement('object');

    //  Check support for availability of security property (IE)
    //  :: Boolean
    var supportsRestrictedSecurity = jQuery.browser.msie;

    //  :: String
    var iframeTemplate = (function () {
        //  Build appropriate properties based on available functionality
        var iFrameProperties
            = sandbox? ' sandbox="" '
            : supportsRestrictedSecurity? ' security="restricted" '
            : '';
        return '<object class="iframeViewer" id="'+iFrameId+'" '+iFrameProperties+'></object>';
    })();

    //  Properties for a colorbox call
    var colorboxProperties = {
        html: iframeTemplate,
        open: true,
        width: frameSize,
        height: frameSize,
        //title: "Loading, Please wait", 
        onCleanup: function(){
            // Unlike Firefox and Chrome, IE doesn't fire the 'beforeunload' event when the colorbox is closed
            //   (but does fire it when you're switching pages within the colorbox iframe).
            // Workaround is to change the page to some other page briefly.
            $.browser.msie && $('#'+iFrameId).attr('data', 'about:blank');
        },        
        onOpen: function() {        	 
        	 $("#cboxContent").css({"background-image" : "url('../resources/images/ajax-loader.gif')",
					"background-repeat" : "no-repeat",
					"background-position" : "center center"});
        	
        }       
    };

    $(document).bind('cbox_open', function () {
        isOpen = true;
        SearchSettings.setDocviewHlField(null);
        SearchSettings.setActiveViewTab(null);
    });
    
    $(document).bind('cbox_closed', function () {
        isOpen = false;        
        setTimeout(function() {
            SearchEvents.reloadFolders();
        }, 10);
        
    });

    //  Neither sandbox, nor security properties
    if (!sandbox && !supportsRestrictedSecurity) {
        //  This is a hack to prevent certain websites (like the New York Times) which refuse to be embedded from
        //  redirecting to a different website. Unnecessary if iframe sandboxing is supported, e.g. Chrome/Safari
        //  [Copied from Sunburst]
        $(window.top).on('beforeunload', function(){
            if (Date.now() < disableLinkDuration) {
                unloadAttempted = true;
            }
        });
    }
    var selectedNodes = {};
    SearchEvents.saveSelectedNodes = function(nodes) {
    	selectedNodes = nodes;	
    }
    /* Shifts to a url relative to the open one. */
    var shiftBy = function (difference) {
        var docs = SearchEvents.getDocuments();
        var idx = docs.idx;
        var urls = docs.urls;
        var shiftDocIdx = idx + difference;
        var shiftDoc = urls[shiftDocIdx];
        
        SearchEvents.scrollToDoc(shiftDocIdx);
        var linksHref = shiftDoc + gethighLightQueryParams(shiftDoc);
        linksHref = linksHref + '&selectedNodes=' + selectedNodes;
        openUrl(linksHref);
        
        prev.prop('disabled', shiftDocIdx === 0);
        next.prop('disabled', shiftDocIdx === docs.total - 1);
        
    };
    
    var gethighLightQueryParams = function(docRef) {
        var params = '';
        var hlfield = SearchSettings.getDocviewHlField();

        if (hlfield && (hlfield.field.type === 'hllink' || isSnomedField(hlfield.field.name))) {
            params = hlfield.data  || '';
        }
        
        return params;
    }
    
    var isSnomedField = function(fieldname) {
        return /.*(NOTEEVENT\/)?SMP?$/.test(fieldname);
    }
    

    var popoverShowing = function () {
        return ($('.popover.in').length === 1);
    };

    var openPrevious = function () {
        shiftBy(-1);
    };
    var openNext     = function () {
    	$(".peepCheck:checkbox");
		sessionStorage.setItem("PreviousChecked" , $(".peepCheck:checkbox"));
        shiftBy(+1);
    };
    var elemList     = [];
    var urlIndex     = 0;
    var box, prev, next, both, navButtons;

    box = $('#colorbox');
    prev = $('<button class="prevBtn btn btn-large btn-warning"><i class="icon-chevron-left"></i></button>');
    next = $('<button class="nextBtn btn btn-large btn-warning"><i class="icon-chevron-right"></i></button>');
    navButtons = $().add(prev).add(next);
    box.append(next, prev);
    prev.click(openPrevious);
    next.click(openNext);

    if (window.navigator.msPointerEnabled) {

        //  Disabled touch actions on the elements -- we're handling them instead
        $().add(navButtons).add(box).css({'-ms-touch-action': 'none'});

        prev.hammer().on('swipe', function (event) {
            if (event.direction === 'right') {
                openPrevious();
            }
        });

        next.hammer().on('swipe', function (event) {
            if (event.direction === 'left') {
                openNext();
            }
        });

        box.hammer().bind('swipe', function (event) {
            ( event.direction === 'left'?  openNext()
                : event.direction === 'right'? openPrevious()
                : undefined );
        });

    }


    /* Builds a function for retrieving the content of a loaded document */
    var fnGetContent = function (difference) {
        return function () {
            if ((urlIndex === 0 && difference < 0)
                || (urlIndex >= elemList.length - 1 && difference > 0)) {
                return 'No more documents.';
            }
            return elemList[urlIndex + (+difference)].innerHTML;
        };
    };



    /* Opens a group of links for navigating and viewing */
    var openGroup = function (from, group) {
        elemList = $('[rel="'+group+'"]');
        urlIndex = elemList.index(from);
        $(from).trigger('opened');
        open(from.href, true);
    };

    /* Opens a document */
    var open = function (url, showNavigation) {
        if (showNavigation) {
            navButtons.show();
        } else {
            navButtons.hide();
        }
        
        var docs = SearchEvents.getDocuments();

        openUrl(url);

        prev.prop('disabled', docs.idx === 0);
        next.prop('disabled', docs.idx === docs.total - 1);
        
    };

    var openUrl = function (url) {
        if (useColorbox) {
            $(window).colorbox({html:''}).colorbox(colorboxProperties);
            setIframeLocation(url);
        }
    };


    //  Elements with a lightbox class should open a viewer on click
    $('body').delegate('.lightbox', 'click', function (e) {
    	sessionStorage.setItem("PreviousChecked", null);
        //return;
    	if (useColorbox) {
            e.preventDefault();
            
            
            var rel = $(this).prop('rel');
            if (rel) {
                //openGroup(this, rel);
                open(this.href, true);
            } else {
                openUrl(this.href);
            }
            
            return false;
        } else {
            $(this).attr('target', '_blank');
        }
    });

    var navigateViewer = function (viewer, url) {
        viewer.attr('data', url);
        var newViewer = viewer.clone();
        viewer.after(newViewer);
        viewer.remove();
        return newViewer;
    };

    //  [Taken from Sunburst and Modified]
    function setIframeLocation(url) {
        var iframeEl = $('#'+iFrameId);

        if (!sandbox && !supportsRestrictedSecurity) {
            // need to reset the page to a known safe page before clearing unloadAttempted so any delayed hijacking
            // attempts from a previous page are lost before we visit the new page
            iframeEl = navigateViewer(iframeEl, 'viewer.do');

            unloadAttempted = false;
            if (preventLinkTraversal) {
                var nextTime = Date.now() + preventLinkTraversal;

                if (preventRewriteInterval) {
                    clearInterval(preventRewriteInterval);
                }

                if (nextTime > disableLinkDuration) {
                    disableLinkDuration = nextTime;
                }

                preventRewriteInterval = setInterval(preventLocationChange, 1)
            }
        }

        iframeEl = navigateViewer(iframeEl, url);

        function preventLocationChange() {
            if (Date.now() < disableLinkDuration) {
                if (unloadAttempted) {
                    window.top.location = '204.do';
                    el.find('object').attr('data', 'viewer.do?url='+encodeURIComponent(url, 'UTF-8'));
                }
            }
            else if(preventRewriteInterval) {
                clearInterval(preventRewriteInterval);
                preventRewriteInterval = null;
                unloadAttempted = false;
            }
        }
    }

    //  Reveal api methods
    iFrameViewer.openUrl = openUrl;
    iFrameViewer.openGroup = openGroup;
});
