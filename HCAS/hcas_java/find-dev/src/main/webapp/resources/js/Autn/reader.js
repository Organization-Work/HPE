(function($) {
    if (typeof Autn === 'undefined') {
        Autn = {};
    }

    Autn.Reader = {};

    var threadPopups = {};
    var colorMap = {
        '': 'black',
        Positive: 'green',
        Neutral: '#c1c100',
        Negative: 'red'
    };

    var templates = {
        twitter:    '<div class="tweet">' +
                        '<div class="image"><img src="<%-ctx.msg.data.PROFILE_IMAGE_URL%>" width="48" height="48" alt=""/></div>' +
                        '<div class="content">' +
                            '<div class="row"><span class="tweet-screen-name"><%-ctx.msg.data.SCREEN_NAME%></span></div>' +
                            // Twitter returns HTML entities escaped in the text and the IDX DRECONTENT is a literal copy,
                            // so we don't HTML-escape here. Beware CSRF attacks in future if twitter API ever changes.
                            '<div class="row"><div class="text"><%=ctx.tokenReplace(ctx.twitterLinks(ctx.linkify(ctx.msg.data.DRECONTENT)))%></div></div>' +
                            '<div class="row"><span class="timestamp"><%-ctx.msg.data.CREATED_AT%></span></div>' +
                        '</div><% if (ctx.opts.sentiment) {%><span style="position:absolute; top: 2px; right: 2px; color:<%=ctx.colorMap[ctx.msg.sentiment]%>">\u2022</span><%}%>' +
                    '</div>'
    };

    Autn.Reader.query = function(labels, opts) {
        opts = opts || {};
        var uniqueRef = encodeURIComponent(labels.join(',').toLowerCase());
        var params = $.extend({}, opts.baseParams, {
            engine: opts.engine || 0,
            query: labels,
            matchAllTerms: opts.matchAllTerms,
            pageSize: 12,
            pageNum: 0
        });

        fetchDocs(params, uniqueRef, opts);
    };

    function getTemplate(key) {
        var tpl = templates[key];
        switch (typeof (tpl)) {
            case 'undefined':
                return null;
            case 'function':
                return tpl;
            case 'string':
            default:
                return templates[key] = _.template(tpl, undefined, { variable: 'ctx' });
        }
    }

    function fetchMore(domEl) {
        var fetchMeta = domEl.data('fetchMeta');

        if (fetchMeta.fetching || !fetchMeta.canFetch) {
            return;
        }

        fetchMeta.fetching = true;
        var params = $.extend({}, fetchMeta.params, {pageNum: fetchMeta.params.pageNum + 1});

        $.ajax(fetchMeta.opts.url || 'query.json', {
            data: params,
            traditional: true,
            type: 'POST',
            complete: function() {
                fetchMeta.fetching = false;
            },
            success: function(data, textStatus, jqXHR) {
                var totalhits = data.totalhits;
                var messages = data.docs;
                var template = getTemplate(data.tpl);

                var toAppend = writeResultMarkup(messages, fetchMeta.opts, template);

                fetchMeta.numShown += messages.length;
                fetchMeta.params = params;
                fetchMeta.canFetch = fetchMeta.numShown < totalhits;

                domEl.find('div.autndoc-insertpt').before(toAppend);
                domEl.parent().find('div.autndoc-numresults').html(numResultsMarkup(totalhits, fetchMeta.numShown));
            }
        });
    }

    function fetchDocs(params, uniqueRef, opts) {
        var popup = threadPopups[uniqueRef];

        if (popup) {
            if (popup !== true) {
                popup.dialog('moveToTop');
            }

            return;
        }

        var title = opts.title || params.query.join('\u2192');

        // set a loading flag to prevent server being queried multiple times on rapid clicking
        threadPopups[uniqueRef] = true;

        $.ajax(opts.url || 'query.json', {
            data: params,
            traditional: true,
            type: 'POST',
            error: function(jqXHR, textStatus, errorThrown) {
                delete threadPopups[uniqueRef];
            },
            success: function(data, textStatus, jqXHR) {
                var totalhits = data.totalhits;
                var messages = data.docs;
                var fetchMeta = { numShown: messages.length, params: params, canFetch: messages.length < totalhits, opts: opts };
                var template = getTemplate(data.tpl);

                var markup = '<div class="autndoc-popup">' + writeResultMarkup(messages, opts, template) + '<div class="autndoc-insertpt"></div></div>';

                var dialog = threadPopups[uniqueRef] = $(markup).data('fetchMeta', fetchMeta).dialog(_.defaults({
                    maxHeight: 550,
                    title: _.escape(title) + '<div class="autndoc-numresults">' + numResultsMarkup(totalhits, fetchMeta.numShown) + '</div>',
                    width: opts.width || 900,
                    close: function() {
                        $(this).unbind('scroll', onScroll).remove();
                        delete threadPopups[uniqueRef];
                    }
                }, opts.dialogOpts)).scrollTop(0).bind('scroll', onScroll);

                if (dialog.prop('scrollHeight') <= dialog.prop('offsetHeight')) {
                    onScroll.call(dialog);
                }
            }
        });
    }

    function writeResultMarkup(messages, opts, template) {
        var markup = '';

        var style = opts.highlightColor ? ' style="color:' + opts.highlightColor + '"' : '';
        var startTag = '<span class="autn-highlight" '+style+'>';
        var endTag = '</span>';

        var ctx = {
            colorMap: colorMap,
            linkify: linkify,
            opts: opts,
            twitterLinks: twitterLinks,
            tokenReplace: function(text){ return tokenReplace(text, startTag, endTag); }
        };

        for (var ii = 0; ii < messages.length; ++ii) {
            var message = messages[ii];

            if (template) {
                ctx.msg = message;
                markup += template(ctx);
            }
            else {
                markup += '<div class="autndoc" x-autn-ref="'+_.escape(message.ref)+'"><div class="autndoc-header">'
                        + (opts.sentiment ? '<span style="color:'+colorMap[message.sentiment || '']+';margin-right:2px;float:left;">\u2022</span>' : '')
                        +'<div class="autndoc-title">'
                        + linkWrap(message.title, message.ref) + '</div></div><div class="autndoc-body">'
                        + tokenReplace(_.escape(message.summary), startTag, endTag)
                        + '</div></div>';
            }
        }

        return markup;
    }

    function numResultsMarkup(totalhits, numShown) {
        return '(' + numShown + ' of ' + plural(totalhits, 'result' ) + ')';
    }

    function onScroll() {
        var el = $(this);

        if (el.height() + el.scrollTop() > el.prop('scrollHeight') - 50) {
            fetchMore(el);
        }
    }

    function linkWrap(str, href) {
        if (typeof href === 'string' && href.match(/^http(s?):/)) {
            return '<a href="' + _.escape(href) + '" target="_blank">' + _.escape(str) + '</a>';
        }

        return _.escape(str);
    }

    function linkify(str){
            // Twitter XML-encodes its entites even in the JSON feed, and this is reflected in the IDX, so we can't
            // html-encode them or it'll be double escaped. Makes this method simpler.
            // e.g. "Gavin &amp; Stacey" in
            // http://search.twitter.com/search.json?callback=processTweets&rpp=25&result_type=recent&q=-RT%20%22gavin%20%26%20stacey%22&_nocache=1350551797051
//                $(document.body).html(_.map([
//                'http://t....',
//                'http://www.test.com/query?cat=%27%22&fish=mouse',
//                "http://t.co/8usdkaZn'",
//                'http://t.co/8usdkaZn"',
//                'http://t.co/8usdkaZn”',
//                "http://t.co/8usdkaZn'a",
//                'http://t.co/8usdkaZn"a',
//                'http://t.co/8usdkaZn”a'], function(a){
//                return linkify(a);
//                }).join('<br>'))
        return str && str.replace(/https?:\/\/[\w.]+?\/[\w.&#;?=%]+/g, function(link){
            return '<a href="' + link + '" target="_blank">' + link + '</a>';
        });
    }

    function twitterLinks(str) {
        return str && str.replace(/\B@(\w{3,})/g, function(text, twitterId){
            return '@<a href="http://twitter.com/#!/' + twitterId + '" target="_blank">' + twitterId + '</a>';
        });
    }

    function tokenReplace(str, startTag, endTag) {
        return str && str.replace(/&lt;(&#x2F;)?autn:highlight&gt;/g, function(str, end){
            return end ? endTag : startTag;
        });
    }

    function plural(count, singular, plural) {
        return count + ' ' + (count === 1 ? singular : (arguments[2] || (singular + 's')));
    }
})(jQuery);
