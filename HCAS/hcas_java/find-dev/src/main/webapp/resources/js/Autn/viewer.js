define(['jquery', 'underscore', 'text!tpl/viewer-doc.htm', 'Autn/i18n', 'jqueryui', 'd3'], function($, _, docTplStr, i18n) {

    var docTpl = _.template(docTplStr, undefined, {variable: 'data'});

    var disableLinkDuration = 0;
    var preventRewriteInterval;
    var unloadAttempted = true;

    var sandbox = 'sandbox' in document.createElement('iframe'), supportsRestrictedSecurity = jQuery.browser.msie;

    if (!sandbox && !supportsRestrictedSecurity) {
        // This is a hack to prevent certain websites (like the New York Times) which refuse to be embedded from
        // redirecting to a different website. Unnecessary if iframe sandboxing is supported, e.g. Chrome/Safari
        $(window).on('beforeunload', function(){
            if (Date.now() < disableLinkDuration) {
                unloadAttempted = true;
            }
        });
    }

    // IE has a security="restricted" flag which works against browser hijacking; links open in new windows
    var security = !sandbox && supportsRestrictedSecurity ? ' security="restricted"' : '';
    var iframeMarkup = '<iframe sandbox="" class="viewer-iframe" src="viewer.do" '+security+'></iframe>';

    var dateFmt = d3.time.format('%A, %d %b %Y');

    var tplScope = {
        dateFmt: function(t){ return dateFmt(new Date(t)); }
    };

    function formatDoc(doc) {
        tplScope.doc = doc;
        return docTpl(tplScope);
    }

    function Viewer (cfg) {
        var onFetch = cfg.onFetch, onClose = cfg.onClose, el, scrollEl, me = this, page = 0,
            pageSize = cfg.pageSize || 10, moreResults, useIframe = cfg.iframe, onNewMessages = cfg.onNewMessages,
            numResults = 0, closeOnTitleClick = cfg.closeOnTitleClick;

        var knownRefs = {};

        var lastAjax = onFetch.call(this, page, pageSize, function(json) {
            numResults = json.docs.length;

            if (!numResults) {
                close();
                return;
            }

            ++page;
            lastAjax = null;
            moreResults = numResults === pageSize;

            // handle initial rendering and setup
            var innerHTML = json.docs.map(formatDoc).join('');

            json.docs.forEach(function(doc){
                knownRefs[doc.ref] = true;
            });

            var dialogHTML = ['<div class="viewer">'];

            if (useIframe) {
                dialogHTML.push(iframeMarkup);
            }

            dialogHTML.push('<div class="viewer-results">', innerHTML, '</div></div>');

            el = $(dialogHTML.join('')).dialog(_.defaults({
                title: cfg.title,
                width: cfg.width || 940,
                height: cfg.height || 724,
                close: close
            }, cfg.dialogOpts));

            if (closeOnTitleClick) {
                el.dialog('widget').find('div.ui-dialog-titlebar').click(function(){
                    el.dialog('close');
                });
            }

            onNewMessages && onNewMessages.call(me, el.dialog('widget'), numResults);

            scrollEl = el.find('div.viewer-results').scrollTop(0).bind('scroll', onScroll);

            if (useIframe) {
                scrollEl.css('padding-left', 20);

                el.on('click', 'a', null, function() {
                    setIframeLocation($(this).attr('href'));
                    return false;
                })
            }

            if (scrollEl.prop('scrollHeight') <= scrollEl.prop('offsetHeight')) {
                onScroll();
            }

            if (useIframe && cfg.autoLoad) {
                setIframeLocation(json.docs[0].ref);
            }
        }, close);

        this.toFront = function() {
            el && el.dialog('moveToTop');
        };

        function setIframeLocation(url) {
            var iframeEl = el.find('.viewer-iframe');

            if (/\.pdf$/i.test(url)) {
                iframeEl.replaceWith('<object class="viewer-iframe" data="'+ _.escape(url)+'"></object>');
                return;
            }

            if (!iframeEl.is('iframe')) {
                iframeEl.replaceWith(iframeMarkup);
                iframeEl = el.find('.viewer-iframe');
            }

            if (!sandbox && !supportsRestrictedSecurity) {
                // need to reset the page to a known safe page before clearing unloadAttempted so any delayed hijacking
                // attempts from a previous page are lost before we visit the new page
                iframeEl.attr('src', 'viewer.do');
                unloadAttempted = false;
                if (cfg.preventLinkTraversal) {
                    var nextTime = Date.now() + cfg.preventLinkTraversal;

                    if (preventRewriteInterval) {
                        clearInterval(preventRewriteInterval);
                    }

                    if (nextTime > disableLinkDuration) {
                        disableLinkDuration = nextTime;
                    }

                    preventRewriteInterval = setInterval(preventLocationChange, 1)
                }
            }

            iframeEl.attr('src', url);

            function preventLocationChange() {
                if (Date.now() < disableLinkDuration) {
                    if (unloadAttempted) {
                        window.location = '204.do';
                        el.find('iframe').attr('src', 'viewer.do?url='+encodeURIComponent(url));
                    }
                }
                else if(preventRewriteInterval) {
                    clearInterval(preventRewriteInterval);
                    preventRewriteInterval = null;
                    unloadAttempted = false;
                }
            }
        }

        function onScroll() {
            if (scrollEl.height() + scrollEl.scrollTop() > scrollEl.prop('scrollHeight') - 50) {
                fetchMore();
            }
        }

        function fetchMore() {
            if (lastAjax || !moreResults) {
                return;
            }

            lastAjax = onFetch.call(this, page, pageSize, function(json) {
                ++page;
                lastAjax = null;
                moreResults = json.docs.length === pageSize;

                var toAdd = '';

                json.docs.forEach(function(doc){
                    if (!knownRefs[doc.ref]) {
                        knownRefs[doc.ref] = true;
                        toAdd += formatDoc(doc);
                        ++numResults;
                    }
                });

                if (toAdd) {
                    $(toAdd).appendTo(el.find('div.viewer-results'));

                    onNewMessages && onNewMessages.call(me, el.dialog('widget'), numResults);
                }

            }, function() {
                lastAjax = null;
            });
        }

        function close() {
            if (lastAjax) {
                lastAjax.abort();
                lastAjax = null;
            }

            onClose.call(me);
        }
    }

    return Viewer;
});
