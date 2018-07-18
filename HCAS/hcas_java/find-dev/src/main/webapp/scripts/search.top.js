jQuery(function ($) {
	var tempID  =  this.versionObj;
	var tempID =  localStorage.getItem("storageId");
	
    /*  Constants  */

    var GET_TOP_TITLES_URL = 'ajax/search/getTopTitlesSummaries.json';
    var TEMPLATE = _.template($.resGet('../templates/search/topTitles'+tempID+'.template'));
    var $parent = $('.sidebar');
    var $container = $parent.find('#suggestions');

    /*  State  */
    var allowPopover = false;
    var currentQuery = null;
    var request = null;
    var cache = {};


    /*  Functionality  */

    var clearCache = function () {
        cache = {};
    };

    var popoverResponse = function (response, search) {
        if (allowPopover) {
            $('.popover').remove();
            $parent.data('popover', null).popover({
                title: 'Top Search Documents',
                placement: 'right',
                content: TEMPLATE({ items: response, search: search }),
                html: true,
                trigger: 'manual'
            }).popover('show');
        }
    };

    var requestTitlesForSearch = function () {

        allowPopover = true;

        var queryText = $(this).text();

        var dat = $.extend({}, currentQuery, {
            page: 0,
            query: queryText
        });

        if (queryText in cache) {
            return popoverResponse(cache[queryText], dat);
        }

        request = $.ajax({
            type: 'post',
            url: GET_TOP_TITLES_URL,
            contentType: 'application/json',
            data: $.toJSON(dat)
        });

        request.done(function (response) {
            cache[queryText] = response;
            popoverResponse(response, dat);
        });
    };


    var hideTitles = function () {
        if (request) {
            request.abort();
            request = null;
        }
        allowPopover = false;
        $parent.popover('hide');
    };


    /*  Event Hooks  */

    $container
        .on('mouseenter', '.setSearch', requestTitlesForSearch)
        .on('mouseleave', hideTitles);

    SearchEvents.$.on(SearchEvents.SEARCH_REQUEST_SENT, function (e, data) {
        clearCache();
        currentQuery = $.extend(true, {}, data.search);
    });

});