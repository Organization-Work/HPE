(function($) {
    if (typeof Autn === 'undefined') {
        Autn = {};
    }

    var threadPopups = {}, init;

    Autn.MsgReader = {
        query: function(from, to) {
            if (!init) {
                init = true;
                $(document).on('click', 'div.msg-body', null, function() {
                    $(this).toggleClass('msg-collapsed');
                }).on('click', 'div.msg-fetchmore', null, function() {
                    fetchMore($(this).closest('div.thread-popup'));
                });
            }

            fetchMail({
                from: from,
                to: to,
                pageSize: 6,
                pageNum: 0
            }, from + '\u2192' + to);
        }
    };

    function plural(count, singular, plural) {
        return count + ' ' + (count === 1 ? singular : (arguments[2] || (singular + 's')));
    }

    function onScroll() {
        var el = $(this);

        if (el.height() + el.scrollTop() > el.prop('scrollHeight') - 50) {
            fetchMore(el);
        }
    }

    function fetchMore(domEl) {
        var fetchMeta = domEl.data('fetchMeta');

        if (fetchMeta.fetching || !fetchMeta.canFetch) {
            return;
        }

        fetchMeta.fetching = true;
        var params = $.extend({}, fetchMeta.params, {pageNum: fetchMeta.params.pageNum + 1});

        $.ajax('messages.json', {
            contentType: 'application/json',
            data: params,
            complete: function() {
                fetchMeta.fetching = false;
            },
            success: function(data, textStatus, jqXHR) {
                var totalhits = data.totalhits;
                var messages = data.messages;

                var toAppend = writeResultMarkup(messages);

                fetchMeta.numShown += messages.length;
                fetchMeta.params = params;
                fetchMeta.canFetch = fetchMeta.numShown < totalhits;

                domEl.find('div.numresults').before(toAppend).html(numResultsMarkup(totalhits, fetchMeta.numShown));
            }
        });
    }

    function writeResultMarkup(messages) {
        var markup = '';

        for (var ii = 0; ii < messages.length; ++ii) {
            var message = messages[ii];
            markup += '<div class="msg" x-autn-ref="'+_.escape(message.reference)+'"><div class="msg-meta"><div class="msg-title">'
                    + _.escape(message.title) + '</div><div><span class="msg-meta-header">Date:&nbsp;</span>'
                    + _.escape(new Date(message.msgDate)) + '</div><div><span class="msg-meta-header">From:&nbsp;</span>'
                    + _.escape(message.from) + '</div>';
            appendMarkup('to', 'To');
            appendMarkup('cc', 'CC');
            appendMarkup('bcc', 'BCC');
            markup += '</div><div class="msg-body msg-collapsed">';
            var style = '';
            var padding = 0;
            var alpha = 4;
            for (var jj = 0; jj < message.emailBody.length; ++jj) {
                if (jj > 0) {
                    padding += 10;
                    if (alpha < 8) {
                        alpha += 2;
                    }
                    var faded = alpha.toString(16);
                    style = 'padding-left: '+padding+'px;color:#' + faded + faded + faded;
                }
                // For some reason, the email bodies have doublequotes prefixed with a backslash
                markup += '<div style="'+style+'">' + _.escape(message.emailBody[jj].replace(/\\"/g, '"')) + '</div>';
            }

            markup += '</div></div>';
        }

        return markup;

        function appendMarkup(prop, label) {
            var value = message[prop];
            if (value) {
                markup += '<div><span class="msg-meta-header">'+label+':&nbsp;</span>'+_.escape(value)+'</div>';
            }
        }
    }

    function numResultsMarkup(totalhits, numShown) {
        var toReturn = 'Showing ' + numShown + ' of ' + plural(totalhits, 'message');

        if (numShown < totalhits) {
            toReturn += '<div class="msg-fetchmore">Fetch more...</div>';
        }

        return toReturn;
    }

    function fetchMail(params, title) {
        var uniqueRef = params.from + '\u2192' + params.to;
        var popup = threadPopups[uniqueRef];

        if (popup) {
            if (popup !== true) {
                popup.dialog('moveToTop');
            }

            return;
        }

        // set a loading flag to prevent server being queried multiple times on rapid clicking
        threadPopups[uniqueRef] = true;

        $.ajax('messages.json', {
            contentType: 'application/json',
            data: params,
            error: function(jqXHR, textStatus, errorThrown) {
                delete threadPopups[uniqueRef];
            },
            success: function(data, textStatus, jqXHR) {
                var totalhits = data.totalhits;
                var messages = data.messages;
                var fetchMeta = { numShown: messages.length, params: params, canFetch: messages.length < totalhits };

                var markup = '<div class="thread-popup">' + writeResultMarkup(messages)
                        + '<div class="numresults">' + numResultsMarkup(totalhits, fetchMeta.numShown)
                        + '</div></div>';

                threadPopups[uniqueRef] = $(markup).data('fetchMeta', fetchMeta).dialog({
                    title: _.escape(title),
                    width: 800,
                    close: function() {
                        $(this).unbind('scroll', onScroll).remove();
                        delete threadPopups[uniqueRef];
                    }
                }).scrollTop(0).bind('scroll', onScroll);
            }
        });
    }

})(jQuery);
