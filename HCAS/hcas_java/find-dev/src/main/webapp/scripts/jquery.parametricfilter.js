(function($){
	var tempID  =  this.versionObj;
	var namespace = 'parametricfilter';

    if (jQuery.fn[namespace]) {
        // only load plugin once
        return;
    }

    var EVENTS = {
        // keeping full names for easier usage searching
        update: 'parametricfilterupdate',
        search: 'parametricfiltersearch'
    };

    var URLS = {
        fieldList: '../templates/search/filters.fieldList'+tempID+'.template',
        fieldInstance: '../templates/search/filters.instance'+tempID+'.template'
    };

    var createTemplate = _.memoize(function(key){
        return _.template($.resGet(URLS[key]));
    });

    function ParametricFilter($parent, cfg) {
        var fieldListTpl = cfg.fieldListTpl || createTemplate('fieldList');
        var fieldInstanceTpl = cfg.fieldInstanceTpl || createTemplate('fieldInstance');
        var errorLoading = cfg.errorLoading || 'Error loading filter fields!';
        var ID_PREFIX = cfg.fieldIdPrefix || (_.uniqueId(namespace) + '-');

        /*  UI Components  */
        var ui = function (c, p) { return $(c, p || $parent); };

        var $filterList = ui('.filter-list');
        var $fieldList = ui('.field-list');
        var $fieldListMessage = ui('.ignore', $fieldList);

        /*  State  */

        var fieldValues = {};
        var filterFunctions = {};
        var updating = false;


        /*  Functionality  */

        var setterFilter = function (k, v) {
            return function (data) {
                data[k] = v ? [v] : [];
                return data;
            };
        };

        var appenderFilter = function (k, v) {
            return function (data) {
                if (v) {
                    data[k] = (data[k] || []).concat([v]);
                }
                return data;
            };
        };

        var couldNotLoadFieldValues = this.couldNotLoadFieldValues = function () {
            $fieldListMessage.text(errorLoading);
        };

        var storeFieldValues = this.storeFieldValues = function (values) {
            fieldValues = values;
            renderFieldValues();
        };

        var renderFieldValues = function () {
            var html = fieldListTpl({ fields: fieldValues });
            $fieldList.empty().append(html);
        };

        var clearLastAdded = function ($elements) {
            setTimeout(function () {
                $elements.removeClass('lastAdded');
            }, 1000);
        };

        var removeAllFilters = function () {
            $filterList.find('.filter').remove();
            filterFunctions = {};
            // we need to wrap the array, otherwise jQuery.trigger will treat a 4-element array as 4 separate arguments
            $parent.trigger(EVENTS.update, [[]]);
        };

        var filterValuesLookup = function (type) {
            return function () {
                return fieldValues[type].values;
            };
        };

        var spawnFilter = function (type, value) {
            var id = _.uniqueId(ID_PREFIX);
            var templateData = { item: { id: id, field: fieldValues[type] } };
            var $elements = $($.parseHTML(fieldInstanceTpl(templateData)));
            $elements.find('input').typeahead({
                source: filterValuesLookup(type)
            }).val(value);
            $filterList.prepend($elements);
            clearLastAdded($elements);
            return { id: id, fieldName: type, elements: $elements };
        };

        var renderFilter = function (type) {
            spawnFilter(type, '');
            //  This doesn't work
            //$elements.find('input').focus();
        };

        var setSearchWithFilters = function () {
            // we need to wrap the array, otherwise jQuery.trigger will treat a 4-element array as 4 separate arguments
            $parent.trigger(EVENTS.update, [_.values(filterFunctions)]);
        };

        var updateSearchWithFilters = function () {
            updating = true;
            setSearchWithFilters();
            $parent.trigger(EVENTS.search);
        };

        var updateSearchWithFilters_debounced = _.debounce(updateSearchWithFilters, 500);

        var extractFilterDataFromElement = function ($element) {
            return {
                id: $element.data('id'),
                fieldName: $element.data('field-name'),
                value: $element.find('input').val()
            };
        };

        var loadFiltersFromHashObject = function (filterMap) {
            removeAllFilters();
            _.each(filterMap, function (values, key) {
                _.each(values, function (value) {
                    var filterData = spawnFilter(key, value);
                    filterFunctions[filterData.id] = appenderFilter(key, value);
                });
            });
            setSearchWithFilters();
        };


        /*  Event Hooks  */

        $filterList.on('click', '.close', function () {
            var $filter = $(this).closest('.filter');
            var filterData = extractFilterDataFromElement($filter);
            delete filterFunctions[filterData.id];
            $filter.remove();
            updateSearchWithFilters_debounced();
        });

        $fieldList.on('click', 'a.field-item', function () {
            renderFilter($(this).data('id'));
        });

        $filterList.on('change', 'input', function (evt) {
            if (this.value === this.lastValue) {
                // Workaround for a bug where the query happens twice if you type in something in the parametric filter,
                // press enter (1st query from typeahead programatically triggering an event with isTrigger=true) then
                // you click elsewhere and the textfield loses focus (2nd query due to DOM change event)
                return false;
            }
            this.lastValue = this.value;

            var $filter = $(this).closest('.filter');
            var filterData = extractFilterDataFromElement($filter);

            filterFunctions[filterData.id] = appenderFilter(filterData.fieldName, filterData.value);

            updateSearchWithFilters_debounced();
        });

        this.addFilter = function(type, value, search){
            var isDuplicate = _.some($filterList.find('.filter'), function (dom) {
                var filterData = extractFilterDataFromElement($(dom));
                return filterData.fieldName === type && filterData.value === value;
            });

            if (isDuplicate) {
                return false;
            }

            var filterData = spawnFilter(type, value);
            filterFunctions[filterData.id] = appenderFilter(type, value);
            search && updateSearchWithFilters_debounced();
            return true;
        };

        this.loadFilters = function (filters){
            if (!updating && !_.isEmpty(fieldValues)) {
                loadFiltersFromHashObject(filters);
            }
            updating = false;
        };

        this.loadFieldsAndFiltersFromDeferred = function(deferred, onSuccess) {
            deferred.done(function (response) {
                if (response.success) {
                    storeFieldValues(response.result);
                    onSuccess && onSuccess.call($parent);
                }
                else {
                    couldNotLoadFieldValues();
                }
            }).fail(couldNotLoadFieldValues)
        }
    }

    var methods = {
        init: function(cfg) {
            return this.each(function(i, el){
                var $el = $(el);

                if ($el.data(namespace)) {
                    // class already instantiated before
                    return;
                }

                $el.data(namespace, new ParametricFilter($el, $.isPlainObject(cfg) ? cfg : {}))
            });
        }
    };

    _.each(['couldNotLoadFieldValues', 'loadFilters', 'storeFieldValues', 'loadFieldsAndFiltersFromDeferred', 'addFilter'], function(key){
        methods[key] = function(){
            var args = arguments;
            return this.each(function(i, el){
                var data = $(el).data(namespace);
                if (data) {
                    data[key].apply(data, args);
                } else {
                    $.error(namespace + ' has not been instantiated yet');
                }
            });
        }
    });

    jQuery.fn[namespace] = function(method){
        if ( methods[method] ) {
            return methods[method].apply( this, Array.prototype.slice.call( arguments, 1 ));
        } else if ( typeof method === 'object' || ! method ) {
            return methods.init.apply( this, arguments );
        } else {
            $.error('Method ' +  method + ' does not exist on jQuery.' + namespace );
        }
    }
})(jQuery);
