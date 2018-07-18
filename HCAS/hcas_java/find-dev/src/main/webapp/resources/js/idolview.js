// raphael.js doesn't import correctly using require.js, see https://github.com/DmitryBaranovskiy/raphael/issues/524
require(['Autn/sunburst', 'Autn/tooltip', 'Autn/graph', 'underscore', 'Autn/viewer', 'Autn/i18n', 'Autn/longpresshandler', 'Autn/filterpicker', 'jquery', 'jqueryui', 'jquerytouchpunch', 'jqueryhashchange', 'json2', 'd3'], function(Sunburst, Tooltip, Graph, _, Viewer, i18n, LongPressHandler, FilterSelector){
    hideLoadIndicator();
    var chartEl = $('#chart');

    var debug = window.location.search.match(/[?&]debug=true(&|$)/i);
    var useIframe = window.location.search.match(/[?&]iframe=true(&|$)/i);
    var isMobileDevice = window.location.search.match(/[?&]mobile=true(&|$)/i) || /iPhone|iPad|iPod|Android|BlackBerry|IEMobile/i.test(navigator.userAgent);

    var maxValues = /[?&]maxValues=(-?\d+)(&|$)/i.exec(window.location.search);
    maxValues = maxValues ? Number(maxValues[1]) : 26;

    var animationTimeParam = window.location.search.match(/[?&]animationTime=(\d+)(&|$)/i);
    var animationTime = Number(animationTimeParam && animationTimeParam[1]) || 1000;

    var sunburst = new Sunburst(chartEl, {
        arcLabelFormatter: debug && function(d){ return d.name; },
        animationTime: animationTime,
        onShortClick: onShortClick,
        onLongClick: onLongClick,
        onHover: onHover
    });

    var graphEl = $('#timegraph');
    var graph = new Graph(graphEl), baseDates;

    var lastAjax, lastSearchFilterParams;

    if (debug) {
        $('.debugonly').removeClass('debugonly');

        $('#test-aqg').click(function(){
            $.ajax('aqg.json', {
                contentType: 'application/json; charset=UTF-8',
                data: JSON.stringify(appendDateParams({
                    filters: {},
                    hierarchyCountKeys: [],
                    maxValues: 200
                })),
                type: 'POST',
                success: function(json) {
                    console.log(json);
                }
            });
        });
    }

    $(function(){
        var expectedHashStr, lastCustomHashObj = {d: undefined, f: undefined};

        var filterSelector = new FilterSelector({
            onFilterChange: doSearch,
            onHashChange: function(encoded) {
                lastCustomHashObj.f = encoded;
                location.hash = expectedHashStr = objectAsHash(lastCustomHashObj);
            }
        });

        var objectAsHash = function (obj) {
            //  Preserves the query value
            var previous = Hash.getCurrentValue();
            return JSON.stringify($.extend({}, previous ? {query: previous.query} : {}, obj));
        };

        startRequest($.ajax('filters.json', {
            complete: hideLoadIndicator,
            success: function(json) {
                $(function(){
                    filterSelector.initializeFilters(json);

                    $(window).hashchange();
                });
            }
        }));

        graphEl.on('autn.graph.datechange', function(dateFilter, isUserInitiated) {
            if (isUserInitiated) {
                lastCustomHashObj.d = graph.getDateFilter() || undefined;
                location.hash = expectedHashStr = JSON.stringify(lastCustomHashObj);
            }

            doSearch();
        });

        chartEl.sortable({
            // disable dragging the svg canvas by restricting draggable items to a nonexistent set
            items: 'li.nonexistent',
            accept: 'ul.draggable-filter-inactive li, ul.draggable-filter-active li',
            receive: function(evt, ui){
                // if you drag a filter in, it doesn't make sense to expand that section anymore with the new UI
                // so we'll create a parameter filter for the dropped element instead
                $(ui.sender).sortable('cancel');
                var oldDisp = ui.item.css('display');

                ui.item.css('display', 'none');
                var dropEl = document.elementFromPoint(evt.clientX, evt.clientY);
                ui.item.css('display', oldDisp);

                var data = dropEl.__data__;

                if (dropEl.tagName !== 'path' || !data || !data.parent) {
                    return;
                }

                var selectedKey = ui.item.data('filterType');
                var isExistingFilter = ui.item.is('li.draggable-filter-filter');

                if (isExistingFilter) {
                     filterSelector.updateFilter(ui.item, data.name, data.filter);
                }
                else if (selectedKey === 'Parameter') {
                    var filters = {};
                    filters[data.filter] = data.name;
                    filterSelector.addFilters(filters);
                }
                else {
                    filterSelector.addQueryText(data.name);
                }
            }
        });

        function doSearch() {
            sunburst.resetFocus();

            var filterParams = filterSelector.getSearchParams();
            var hierarchyCountKeys = filterParams.hierarchyCountKeys;

            startRequest($.ajax('filter.json', {
                complete: hideLoadIndicator,
                contentType: 'application/json; charset=UTF-8',
                data: JSON.stringify(appendDateParams($.extend(true, {
                    fetchDates: true,
                    maxValues: maxValues
                }, filterParams))),
                type: 'POST',
                success: function(json) {
                    lastSearchFilterParams = filterParams;
                    filterSelector.setLastSuccessfulParams(filterParams, json.size);
                    assignFilterLabels(json, hierarchyCountKeys);
                    sunburst.redraw(json, false);

                    baseDates = json.dates;
                    graph.render(baseDates);
                }
            }));

            return false;
        }

        $(window).hashchange(function(){
            var hashStr = decodeURIComponent(window.location.href.replace(/^[^#]+#?/,''));

            if (expectedHashStr !== hashStr) {
                expectedHashStr = hashStr;
                var json;
                try {
                    // window.location.hash can be inconsistent about how it's encoded, so use location.href instead
                    json = JSON.parse(hashStr || '{}');
                    graph.setDateFilter(json.d);
                    json.f && filterSelector.setFiltersFromHash(json.f);
                    lastCustomHashObj = json;
                }
                catch (e) {
                    // invalid hash
                    debugger;
                }
            }
        });
    });

    function assignFilterLabels(json, filters) {
        labelNodes(json, filters, 0);

        function labelNodes(json, filters, depth) {
            var children = json.children;
            var label = filters[depth];

            if (children) {
                for (var ii = 0, max = children.length; ii < max; ++ii) {
                    children[ii].filter = label;
                    labelNodes(children[ii], filters, depth + 1);
                }
            }
        }
    }

    function onShortClick(d){
        cancelLastRequest();

        var filters = $.extend(true, {}, lastSearchFilterParams.filters);

        for (var current = d; current && current.filter; current = current.parent) {
            var oldParamFilters = filters[current.filter];
            if (oldParamFilters) {
                oldParamFilters.push(current.name);
            }
            else {
                filters[current.filter] = [current.name];
            }
        }

        if (d.parent) {
            startRequest($.ajax('dates.json', {
                complete: hideLoadIndicator,
                contentType: 'application/json; charset=UTF-8',
                data: JSON.stringify(appendDateParams({
                    filters: filters,
                    text: lastSearchFilterParams.text
                })),
                type: 'POST',
                success: function(dates) {
                    graph.render(dates);
                }
            }));
        }
        else if (baseDates) {
            graph.render(baseDates);
        }
    }

    function onLongClick(d) {
        showDocs(d);
    }

    var viewers = {};

    function showDocs(d) {
        // show documents in the node
        var oldFilters = lastSearchFilterParams.filters;
        var filters = $.extend(true, {}, oldFilters), title = [], dedupedFilterTitles = {};

        for (var current = d; current && current.filter; current = current.parent) {
            var dedupeKey = current.filter + '\u2192' + current.name;
            if (!dedupedFilterTitles[dedupeKey]) {
                title.push(current.name);
                dedupedFilterTitles[dedupeKey] = 1;
            }

            var oldParamFilters = filters[current.filter];
            if (oldParamFilters) {
                oldParamFilters.push(current.name);
            }
            else {
                filters[current.filter] = [current.name];
            }
        }

        for (var fieldName in oldFilters) {
            (oldFilters[fieldName] || []).reverse().forEach(function(fieldValue){
                var dedupeKey = fieldName + '\u2192' + fieldValue;
                if (!dedupedFilterTitles[dedupeKey]) {
                    title.push(fieldValue);
                    dedupedFilterTitles[dedupeKey] = 1;
                }
            });
        }

        lastSearchFilterParams.text && title.push(lastSearchFilterParams.text);

        var baseParams = appendDateParams({ filters: filters, text: lastSearchFilterParams.text });
        var key = JSON.stringify(baseParams);
        var viewer = viewers[key];

        if (viewer) {
            viewer.toFront();
            return;
        }

        viewer = viewers[key] = new Viewer({
            autoLoad: !debug,
            iframe: useIframe,
            preventLinkTraversal: 2000,
            pageSize: useIframe ? 10 : 20,
            closeOnTitleClick: isMobileDevice,
            height: Math.max(200, chartEl.height() - 20),
            dialogOpts: {
                title: _.escape(title.reverse().join('\u2192')) + '<div class="viewer-doccount"></div>',
                position: {
                    my: 'center top',
                    at: 'center top',
                    of: chartEl
                }
            },
            onClose: function() {
                delete viewers[key];
            },
            onFetch: function(page, pageSize, onSuccess, onFailure) {
                var params = $.extend({}, baseParams, {page: page, pageSize: pageSize});

                return $.ajax('filteredDocs.json', {
                    contentType: 'application/json; charset=UTF-8',
                    data: JSON.stringify(params),
                    type: 'POST',
                    success: onSuccess,
                    failure: onFailure
                });
            },
            onNewMessages: function(dialog, count) {
                dialog.find('div.viewer-doccount').text(i18n('idolview.viewer.resultcount', {RESULTS: count}));
            }
        });
    }

    function onHover(d){
        if (d) {
            $('li.draggable-filter-filter').each(function(idx, el){
                $(el).toggleClass('sector-hovered', $(this).data('filterField') === d.filter);
            });
            return;
        }

        $('li.draggable-filter-filter').removeClass('sector-hovered');
    }

    function appendDateParams(params) {
        var dateRange = graph.getDateFilter();

        if (dateRange) {
            params.start = dateRange[0];
            params.end = dateRange[1];
        }

        return params;
    }

    function startRequest(ajax) {
        $('#load-indicator').show();
        cancelLastRequest();
        lastAjax = ajax;
    }

    function hideLoadIndicator() {
        $('#load-indicator').hide();
    }

    function cancelLastRequest() {
        if (lastAjax) {
            lastAjax.abort();
            lastAjax = null;
        }
    }
});
