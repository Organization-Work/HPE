(function($){
    var threadPopups = {};

    if (typeof Autn === 'undefined') {
        Autn = {};
    }

    Autn.viewDoc = function(ref, title, opts){
        var popup = threadPopups[ref];
        if (popup) {
            if (popup !== true) {
                popup.dialog('moveToTop');
            }

            return;
        }

        threadPopups[ref] = true;

        $.ajax(opts && opts.url || 'content.json', {
            contentType: 'application/json',
            data: {reference: ref},
            traditional: true,
            error: function(jqXHR, textStatus, errorThrown) {
                delete threadPopups[ref];
            },
            success: function(data, textStatus, jqXHR) {
                var sections = data.sections;

                var markup = '<div class="autndoc-popup">' + _.escape(sections.join('')).replace(/^\n+|\n+$/g,'').replace(/\n/g,'<br>') + '</div>';

                var dialog = threadPopups[ref] = $(markup).dialog({
                    title: linkWrap(title, ref),
                    width: 800,
                    close: function() {
                        delete threadPopups[ref];
                    }
                });
            }
        });

    };

    function linkWrap(str, href) {
        if (typeof href === 'string' && href.match(/^http(s?):/)) {
            return '<a href="' + _.escape(href) + '" target="_blank">' + _.escape(str) + '</a>';
        }

        return _.escape(str);
    }

})(jQuery);