define(['jquery', 'Autn/i18n', 'jqueryui', 'json2'], function($, i18n){
    var filterCache = {};

    function autoCompleteSource(field) {
        return function(request, response) {
            var term = request.term.toUpperCase();
            var existing = filterCache[field], maxOptions = 10;

            if (existing instanceof Array) {
                response(substrFilter(existing));
                return;
            }

            if (existing) {
                existing.done(function() { response(substrFilter(filterCache[field])); });
            }

            $.ajax('filter.json', {
                contentType: 'application/json; charset=UTF-8',
                data: JSON.stringify({
                    filters: {},
                    hierarchyCountKeys: [field],
                    fetchDates: true,
                    maxValues: 1000
                }),
                type: 'POST',
                success: function(json) {
                    var terms = json.children.map(function(opt){return opt.name;});
                    filterCache[field] = terms;
                    response(substrFilter(terms));
                }
            });

            function substrFilter(options) {
                return term.length === 0 ? options.slice(0, maxOptions) : options.filter(function(a){ return a.indexOf(term) === 0; }).slice(0, maxOptions);
            }
        }
    }

    return function FilterPicker(cfg){
        var onFilterChange = cfg.onFilterChange;
        var onHashChange = cfg.onHashChange;
        var filters = [];

        var typeMap = {};

        var cursorAt = (function(){
            var tmp;
            try {
                // IE9 is aware of the pointer-events: none property, but doesn't seem to work
                if (!$.browser.msie && getComputedStyle) {
                    tmp = document.createElement('x');
                    tmp.style.pointerEvents = 'none';
                    tmp.style.pointerEvents = 'invalidvalue';
                    document.body.appendChild(tmp);
                    if (getComputedStyle(tmp, null).pointerEvents === 'none') {
                        return undefined;
                    }
                }
            }
            catch(e){
                // do nothing
                tmp && document.body.removeChild(tmp);
            }

            // if a browser doesn't support pointer-events, use it.
            return { top: -1 };
        })();

        $(['Parameter', 'Query'].map(function(type){
            typeMap[type.charAt(0)] = type;
            return $('<li class="draggable-filter-type" unselectable="on"></li>').text(i18n('idolview.' + type)).data('filterType', type);
        })).appendTo(
            $('ul.draggable-filter-inactive').sortable({
                items: 'li.draggable-filter-type',
                cursorAt: cursorAt,
                connectWith: 'ul.draggable-filter,#chart',
                containment: 'document',
                receive: function(evt, ui) {
                    if ($(ui.item).hasClass('draggable-filter-filter')) {
                        $(ui.sender).sortable('cancel');
                        $(ui.item).remove();
                    }
                },
                change: function(evt, ui) {
                    cursorAt || $(ui.item).css('pointer-events', 'none');
                },
                stop: function(evt, ui) {
                    cursorAt || $(ui.item).css('pointer-events', '');
                }
            })
        );

        var lastSortComplete;

        var endMarker, showingEndMarker = true;

        var activeFilterList = $('ul.draggable-filter-active').append(
                endMarker = $('<li class="draggable-filter-endmarker"></li>').text(i18n('idolview.filter.droptarget'))
            ).sortable({
            items: 'li.draggable-filter-filter,li.draggable',
            cursorAt: cursorAt,
            cancel: 'li.draggable',
            connectWith: 'ul.draggable-filter,#chart',
            containment: 'document',
            receive: function(evt, ui){
                lastSortComplete = null;

                // note, this does rely on always having a li at the start
                var appendAfter = $(ui.item).prevAll('li:not(li.draggable-filter-endmarker)').first();
                var text = ui.item.text();
                var filterType = ui.item.data('filterType');
                $(ui.sender).sortable('cancel');
                var newFilter = $('<li class="draggable-filter-filter" unselectable="on"></li>').text(text)
                    .data('filterType', filterType)
                    .toggleClass('isQueryTextType', filterType === 'Query')
                    .insertAfter(appendAfter);
                padSpacers();
                expandBox(newFilter);
            },
            change: function(evt, ui) {
                cursorAt || $(ui.item).css('pointer-events', 'none');
                if (oldBox) {
                    oldBox.remove();
                    oldBox = null;
                }
                // we can't change the element order, since $.sortable relies on it staying the same,
                // so we just hide the elements which shouldn't be shown
                padSpacers(true)
            },
            stop: function(evt, ui) {
                cursorAt || $(ui.item).css('pointer-events', '');
                lastSortComplete = Date.now();
                padSpacers();
            }
        }).bind('sortupdate', function(evt, ui) {
            tryNewQuery();
        }).on('click', 'li.draggable-filter-filter', null, function(){
            (!lastSortComplete || (Date.now() - lastSortComplete > 50)) && expandBox($(this));
        });

        var lastSuccessfulParams, oldBox;

        function expandBox(el) {
            var width = el.outerWidth();

            if (oldBox) {
                var dismiss = el.is(oldBox.data('originEl'));
                oldBox.submit();
                if (dismiss) {
                    oldBox.remove();
                    oldBox = null;
                    return;
                }
                oldBox.remove();
            }

            var isParametric = el.data('filterType') === 'Parameter';

            var textFilter = el.data('filterValue') || '';

            oldBox = $('<form class="draggable-filter-popup"><input value="'+_.escape(textFilter)+'"></form>')
                .insertAfter($('ul.draggable-filter-active'))
                .position({my: 'left top', at: 'left top', of: el})
                .animate({left: '+=' + (width+1)}, function(){
                    var inputEl = $(this).find('input').focus();

                    if (isParametric) {
                        var filterField = el.data('filterField');
                        var filterEls = filters.map(function(filter){
                            var key = filter.key;
                            var thisKey = filterField === key;
                            return $('<div class="filter-option">'+_.escape(i18n(key))+'</div>')
                                .data('filterField', key)
                                .toggleClass('isFiltering', thisKey)
                                .toggleClass('isViewing', !textFilter);
                        });
                        $(filterEls).appendTo($(this));

                        filterField && inputEl.autocomplete({minLength: 0, source: autoCompleteSource(filterField)});
                    }
                })
                .data('originEl', el).on('click', '.filter-option', null, function(){
                    // we activate the filter?
                    el.data('filterField', $(this).data('filterField'))
                      .text($(this).text())
                      .addClass('isFiltering');
                    oldBox.submit();
                })
                .on('submit', function() {
                    var inputEl = oldBox.find('input');

                    var inputText = inputEl.val();

                    var isReady = Boolean(isParametric ? el.data('filterField') : inputText);
                    el.data('filterValue', inputText || '').toggleClass('isFiltering', isReady)
                      .toggleClass('isViewing', isParametric && !inputText && isReady);

                    if (isReady) {
                        if (!isParametric) {
                            el.text(inputText);
                        }
                        tryNewQuery();
                        oldBox.remove();
                    }
                    else {
                        inputEl.focus();
                    }

                    return false;
                });
        }

        this.getSearchParams = getSearchParams;

        function getSearchParams() {
            var filters = {};
            var searchText = [];
            var hierarchyMap = {};

            activeFilterList.find('li.draggable-filter-filter.isFiltering').each(function(idx, el){
                var $el = $(el);
                var field = $el.data('filterField');
                var text = $el.data('filterValue');

                if (field) {
                    hierarchyMap[field] = 1;
                    if (text) {
                        var curFilters = filters[field];
                        if (curFilters) {
                            curFilters.push(text);
                        }
                        else {
                            curFilters = filters[field] = [text];
                        }
                    }
                }
                else if (text) {
                    searchText.push(text);
                }
            });

            var hierarchyCountKeys = [];
            for (var key in hierarchyMap) {
                hierarchyCountKeys.push(key);
            }

            if (searchText.length > 1) {
                searchText = searchText.map(function(a){return '(' + a + ')';});
            }

            return {
                filters: filters,
                hierarchyCountKeys: hierarchyCountKeys,
                text: searchText.join(' AND ') || undefined
            };
        }

        function encodeHash(){
            // need to encode the current filters into a hash string
            var filters = [];

            activeFilterList.find('li.draggable-filter-filter').each(function(idx, el){
                var type = $(el).data('filterType');
                var value = $(el).data('filterValue');
                var field = $(el).data('filterField');

                filters.push({t: type.charAt(0), v: value || undefined, f: field || undefined});
            });

            return filters;
        }

        function createFilter(obj) {
            var type = typeMap[obj.t];
            var value = obj.v;
            var field = obj.f;

            var isParametric = type === 'Parameter';
            var isQuery = type === 'Query';

            if (!isParametric && !isQuery) {
                throw new Error('Invalid type');
            }

            var el = $('<li class="draggable-filter-filter"></li>')
                .data({'filterType': type, 'filterValue': value, 'filterField': field});

            if (isParametric) {
                if (field) {
                    el.addClass('isFiltering');
                    !value && el.addClass('isViewing');
                    el.text(i18n(field));
                }
                else {
                    el.text(i18n('idolview.' + type));
                }
            }
            else {
                el.text(value || i18n('idolview.' + type));
                el.addClass('isQueryTextType');
                value && el.addClass('isFiltering');
            }

            return el;
        }

        function decodeHash(json){
            json = json || [];

            try {
                if (!(json instanceof Array)) {
                    return;
                }

                activeFilterList.find('li.draggable-filter-filter').remove();

                if (json.length) {
                    $(json.map(createFilter)).insertAfter(activeFilterList.children('li:not(li.draggable-filter-endmarker)').last());
                    tryNewQuery();
                }

                padSpacers();

                if(oldBox) {
                    oldBox.remove();
                    oldBox = null;
                }
            }
            catch (e) {
                // do nothing, it's not a valid hash
            }
        }

        function tryNewQuery() {
            onHashChange && onHashChange(encodeHash());

            var showEndMarker = activeFilterList.children('.draggable-filter-filter').length === 0;
            if (showingEndMarker !== showEndMarker) {
                showingEndMarker = showEndMarker;
                endMarker.animate(showEndMarker ? {opacity: 1, 'margin-top' : 2} : {opacity: 0.2, 'margin-top' : 8});
            }

            var newParams = getSearchParams();
            if (lastSuccessfulParams && JSON.stringify(newParams) === lastSuccessfulParams) {
                // params haven't changed, not a new search
                return;
            }

            onFilterChange && onFilterChange();
        }

        function padSpacers(hide) {
            if (!hide) {
                activeFilterList.children('.draggable-filter-filter').each(function(idx, el){
                    var $el = $(el);
                    if ($el.prev().is('li.draggable-filter-filter')) {
                        $('<div class="draggable-filter-separator"></div>').insertBefore($el);
                    }
                });

                activeFilterList.children('div.draggable-filter-separator').each(function(idx, el){
                    var $el = $(el);
                    if (!($el.next().is('.draggable-filter-filter'))) {
                        $el.remove();
                    }
                });
            }

            activeFilterList.children('div.draggable-filter-separator').css('display', function(){
                var $this = $(this);
                return $this.next().is('.draggable-filter-filter:not(.ui-sortable-helper)')
                    && $this.prev().is('.draggable-filter-filter:not(.ui-sortable-helper)') ? '' : 'none';
            });
        }

        this.setLastSuccessfulParams = function(params, blur) {
            lastSuccessfulParams = JSON.stringify(params);
        };

        this.initializeFilters = function(newFilters) {
            filters = newFilters;
        };

        this.updateFilter = function(filter, text, field){
            filter.data('filterValue', text);
            filter.addClass('isFiltering');

            if (filter.data('filterType') === 'Parameter') {
                filter.data('filterField', field);
                filter.text(i18n(field));
                filter.removeClass('isViewing');
            }
            else {
                filter.text(text);
            }

            tryNewQuery();
        };

        this.addFilters = function(filterMap) {
            var els = [];
            for (var field in filterMap) {
                els.push(createFilter({t: 'P', f: field,  v: filterMap[field]}));
            }

            els.length && $(els).insertAfter(activeFilterList.children('li:not(li.draggable-filter-endmarker)').last());
            padSpacers();
            tryNewQuery();
        };

        this.addQueryText = function(queryText) {
            createFilter({t: 'Q', v: queryText}).insertAfter(activeFilterList.children('li:not(li.draggable-filter-endmarker)').last());
            padSpacers();
            tryNewQuery();
        };

        this.setFiltersFromHash = decodeHash;
    };
});
