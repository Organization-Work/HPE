jQuery(function($){	
	var tempID  =  this.versionObj;
	var tempID =  localStorage.getItem("storageId");
	
    // preload the libraries
    require(['Autn/i18n'], $.noop);

    var currentSearch, lastSearch, previousXHR;

    var RESULT_FIELDS_URL = 'ajax/search/getFilteredResultFields.json';
    var SINGLE_FIELD_VALUES_URL = 'ajax/parametric/getSingleParaFieldValues.json';
    
    var fieldsTemplate = _.template($.resGet('../templates/search/topicmapFields'+tempID+'.template'));

    var $topicmap = $('#topicmap');
    var $showPanel = $('#showPanel');

    // query-wise, it's more efficient to fetch the max number of terms then discard as needed than fetch a current query
    var maxToFetch = 300;
    var maxMultiSelectText = 30;

    var topDiscards = 0;
    var numToDisplay = 50;

    var dataToRender, $paper;

    var $tooltip;

    var savedParams, savedResults, savedTotalCount, totalOverAllDatabases, lastSavedParams, categoryMap;

    var discardedSupercategories = {};

    var fieldList, $topicList;

    _.each(SearchConfig.culledSuperCategories, function(val){
        discardedSupercategories[val] = true;
    });

    var $totalResults = $('<div id="topicmap-totals" class="view-results-label"></div>').appendTo($topicmap);

    //var $loading = $('<div class="visualizer-loading"></div>').appendTo($topicmap).hide();

    $tooltip = $('<div class="topicmap-tooltip"></div>').hide().appendTo($topicmap);

    var $toolbar =  $('<div class="btn-toolbar" style="margin: 0;"></div>').appendTo($topicmap);

    var $topicListViewEl = $('<div class="btn-group"/>').appendTo($toolbar);
    var $tmlvAdd = $('<button class="btn btn-mini btn-info" data-toggle="tooltip" title="Add selected filters"><i class="icon-white icon-arrow-left"></i> Add Filters</button>').appendTo($topicListViewEl);

    var $tmlvFieldSelect = $('<select style="display: none;"></select>').appendTo($toolbar);
    var $tmlvParameter = $('<select multiple="multiple" style="display: none;"></select>').appendTo($toolbar);

    var $saveGroup = $('<div class="btn-group"></div>').appendTo(($toolbar));
    var $compareBtn = $('<button class="btn btn-mini btn-info"><i class="icon-white icon-wrench"></i> Compare </button>').appendTo($saveGroup);
    var $saveBtn = $('<button class="btn btn-mini btn-info"><i class="icon-white icon-plus"></i> Select for Comparison</button>').appendTo($saveGroup);

    $compareBtn.tooltip({
        html: true,
        placement: 'bottom',
        title: '<div>Press Select for Comparison</div>'
    });

    $saveBtn.tooltip({
        html: true,
        placement: 'bottom',
        title: '<div>Select results for comparison</div>'
    });


    var $update = $('<div class="btn-group"></div>').appendTo($toolbar);

    var $numDiscards = $('<div></div>').css({marginBottom: 10}).appendTo($toolbar);
    updateDiscardsCount(topDiscards, numToDisplay);

    $('<div class="slider-text-label">Categories</div>').appendTo($toolbar);

    $('<div></div>').css({
        float: 'left',
        margin: '0px 1% 10px',
        width: '85%'
    }).appendTo($toolbar).slider({
        value: numToDisplay,
        min: 50,
        max: maxToFetch,
        step: 10,
        slide: function(evt, ui) {
            updateDiscardsCount(topDiscards, Number(ui.value));
        },
        stop: function(evt, ui) {
            var currentValue = Number(ui.value);
            if (numToDisplay !== currentValue) {
                numToDisplay = Number(ui.value);
                rerender();
            }
        }
    });

    $('<div class="slider-text-label">Discard</div>').appendTo($toolbar);

    $('<div></div>').css({
        float: 'left',
        margin: '0px 1% 10px',
        width: '85%'
    }).appendTo($topicmap).slider({
        value: topDiscards,
        min: 0,
        max: 30,
        step: 1,
        slide: function(evt, ui) {
            updateDiscardsCount(Number(ui.value), numToDisplay);
        },
        stop: function(evt, ui) {
            var currentValue = Number(ui.value);
            if (topDiscards !== currentValue) {
                topDiscards = Number(ui.value);
                rerender();
            }
        }
    });
    
    function getSnomedField() {
        var selectedSearchView = SearchEvents.getSelectedSearchView();
        return SearchConfig.searchViews[selectedSearchView].snomedTag;
    }

    function getSnomedParentField() {
        var selectedSearchView = SearchEvents.getSelectedSearchView();
        return SearchConfig.searchViews[selectedSearchView].snomedParentTag;
    }
    
    function getParametricFields() {
        var selectedSearchView = SearchEvents.getSelectedSearchView();
        return FilterFields[selectedSearchView];
        
    }
    

    $tmlvFieldSelect.multiselect({
        buttonClass: 'btn btn-mini topicmap-btn-left topicmap-btn-right',
        buttonContainer: $topicListViewEl,
        maxHeight: 500,
        onChange: function(element, checked) {
            if (checked) {            	            
				updateParametricList(categoryMap[$(element)[0].value]);            	  
            }
        },
        buttonText: function(options) {
            var selectedArray = [];
            options.each(function() {
                var label = ($(this).attr('label') !== undefined) ? $(this).attr('label') : $(this).html();
                selectedArray.push(label);
            });

            var selected = selectedArray.join(', ');
            
            if (selected.length > maxMultiSelectText) {            	
                selected =  selected.substring(0, maxMultiSelectText) + "...";
            }
            return selected  + ' <b class="caret"></b>';
        }
    });

    $tmlvFieldSelect.multiselect('dataprovider', []);

    $tmlvParameter.multiselect({
        buttonClass: 'btn btn-mini topicmap-btn-left',
        enableCaseInsensitiveFiltering: true,
        enableFiltering: true,
        maxHeight: 500,
        buttonText: function(options) {
            if (options.length == 0) {
                return this.nonSelectedText + ' <b class="caret"></b>';
            }
            else if (options.length > 3) {
                return options.length + ' ' + this.nSelectedText + ' <b class="caret"></b>';
            }
            else {
                var selectedArray = [];
                options.each(function() {
                    var label = ($(this).attr('label') !== undefined) ? $(this).attr('label') : $(this).html();
                    selectedArray.push(label);
                });

                var selected = selectedArray.join(', ');
                if (selected.length > maxMultiSelectText) {
                    selected =  options.length + ' ' + this.nSelectedText;
                }
                return selected  + ' <b class="caret"></b>';
            }
        }
    });

    $tmlvParameter.multiselect('dataprovider', []);	

    $tmlvAdd.on('click', function(e){
        e.preventDefault();

        var entry = $tmlvFieldSelect.multiselect('getSelected');
        if (entry && entry.val()) {
            var data = [], label;
            var category = categoryMap[entry.val()];

            $('option:selected', $tmlvParameter).each(function() {
                label = $(this).val();
                if (category.isSNOMED) {
                    label +=' (' + category.name + ')'
                }
                data.push(label);
            });
            if (data.length > 0) {
                
                data = _.map(data, function(value) {
                    return {
                        type: PARAMETRIC_FIELDTYPE,
                        val: value
                    };
                });
                
                var filterData = {};
                var filterId = category.isSNOMED ? getSnomedField() : category.name;
                filterData[filterId] = data;
                $('#parametricForm').data('filterForm').insertLoadingFilterSet()
                    .loadFilters({
                        boolOperator: 'OR',
                        filterFields: filterData,
                        childGroups: null,
                        tag: ''
                    }, null, null);
                }
        }
    });

    $('[href=#topicmap]').on('shown', function(){
        if (!_.isEqual(lastSearch, currentSearch)) {
            doSearch();
        }
        
        return false;
        
        //renderIfVisible();
    });	

    $compareBtn.on('touchstart click', function(event) {
        event.stopPropagation();
        if (!savedResults) {
            return;
        }

        var toRender = reduceData(retrieveDiffs(totalOverAllDatabases));
        dataToRender = {
            name: '',
            sentiment: true,
            size: 1,
            children: _.map(toRender, function(categoryMeta){
                
                var absPosMax = absNegMax = 0;
                _.each(categoryMeta.children, function(b) {
                    if (b.rawDiff > 0) {
                        absPosMax = Math.max(absPosMax, b.size);
                    } else {
                        absNegMax = Math.max(absNegMax, b.size);
                    }
                    
                });
                
                
                if (absPosMax == 0 && absNegMax == 0) {
                    categoryMeta.rawCount = undefined;
                    categoryMeta.sentiment = 0;
                } else {
                    var total = 0;

                    _.each(categoryMeta.children, function(val) {
                        // we're using sentiment as a proxy for current/old. The higher the sentiment, the higher
                        // the relative proportion of the category in the current dataset as opposed to the old one
                        var absMax = val.rawDiff >= 0 ? absPosMax : absNegMax;
                        var sentimentDiff = absMax == 0 ? 0 : 0.5 * (val.rawDiff / absMax);
                        var sentiment = val.sentiment = 0.5 + sentimentDiff;
                        val.rawCount = undefined;
                        total += sentiment;
                    });

                    categoryMeta.rawCount = undefined;
                    categoryMeta.sentiment = total / categoryMeta.children.length;
                }

                return categoryMeta;
            })
        };

        renderIfVisible();
    });

    $saveBtn.on('touchstart click', function(event) {
        event.stopPropagation();
        if (!categoryMap) {
            return;
        }

        savedResults = categoryMap;
        savedParams = lastSavedParams;
        savedTotalCount = totalOverAllDatabases;
        setSavedTooltip();
        $saveBtn.trigger('mouseover');
    });

    function updateDiscardsCount(discards, toFetch) {
        var showing = 'Showing ' + toFetch + ' categories';
        if (discards) {
            showing += ', discarding top ' + (discards > 1 ? discards + ' categories' : 'category') + ' in each supercategory';
        }
        $numDiscards.text(showing);
    }

    function renderIfVisible() {
        if (dataToRender && $topicmap.is(':visible')) {
            if (!$paper) {
                var lastChild = $topicmap.children().last();
                $paper = $('<div></div>').css({
                    height: $showPanel.height() - lastChild[0].offsetTop - lastChild.outerHeight(true) - 40,
                    margin: 'auto',
                    width: '98%'
                }).appendTo($topicmap);

                $paper.topicmap({
                    maxFont: 24,
                    minFont: 12,
                    maxLeafFont: 12,
                    enforceLabelBounds: true,
                    skipAnimation: true,
                    onLayoutCreation: function(layout){
                        layout.sort(function (a,b) {
                            return a.size - b.size;
                        });
                    },
                    onLeafClick: function(d){
                        var label = d.name;
                        var parent = d.parent;
                        var field = parent.field;

                        if (parent.isSNOMED) {
                            field = getSnomedField();
                            label +=' (' + parent.name + ')';
                        }

                        var filterData = {id: field, 
                                          value: label,
                                          fieldType: PARAMETRIC_FIELDTYPE};
                        $('#parametricForm').data('filterForm').loadFilter(filterData);
                    },
                    onNodeRender: function(node){
                        node.path.mousemove(function(evt){
                            require(['Autn/i18n', 'messageformat'], function(i18n){
                                var html = [_.escape(node.name)];
                                
                                if (node.parent && node.parent.field) {
                                	html.push('[' + node.parent.name + ']');
                                }

                                if (_.isNumber(node.current)) {
                                    html.push('Current: ' + (node.current*100).toFixed(1) + '%');
                                }

                                if (_.isNumber(node.old)) {
                                    html.push('Previous: ' + (node.old*100).toFixed(1) + '%');
                                }

                                if (node.rawCount) {
                                    var percentage = node.field ? '' : ' (' + ((node.rawCount/totalOverAllDatabases)*100).toFixed(1) + '%)';
                                    html.push(i18n('idolview.center.label.numdocs', {DOCS: node.rawCount}) + percentage);
                                }

                                $tooltip.css('background', node.color2)
                                        .html(html.join('<BR>'))
                                        .show().position({ my: 'left bottom', of: evt, offset: '0 -5', collision: 'fit', within: $topicmap});
                            });
                        }).mouseout(function(){
                            $tooltip.hide();
                        });
                    }
                });
            }

            $paper.topicmap('renderData', dataToRender, dataToRender.sentiment);
            dataToRender = undefined;
        }
    }

    function parseCategories(entries) {
        var fieldMap = {};
        var name, category;
        for (var i = 0, len = entries.length; i < len; ++i) {
            name = entries[i];
            category = name.slice(name.lastIndexOf(' (') + 2, name.length - 1);
            if (!(discardedSupercategories[category] || category in fieldMap) ) {
                fieldMap[category] = true;
            }
        }
        return fieldMap;
    }

    function parseFieldNames(entries, ignoreFields) {
        var key;
        var fieldNames = [];
        
        var viewParametricFields = getParametricFields();
        
        var keys = _.keys(entries);
        for (var i = 0, len = keys.length; i < len; ++i) {
            key = keys[i];
            if (!(key in ignoreFields)) {
                fieldNames.push({
                    displayName: entries[key].displayName,
                    fieldName: key,
                    defaultSelect: viewParametricFields[key] ? viewParametricFields[key].topicmapDefaultSelect : false
                });
            }
        }
        return fieldNames;
    }
    
    function doSearch() {
    	//$loading.show()
        SearchEvents.toggleResultsLoading(true, $totalResults);
        var totalResultsLabel = '  Total ' + SearchSettings.getResultViewText() + ': ' +  SearchEvents.getFormattedResult(totalOverAllDatabases);
        var submitData = _.extend({fieldNames: retrieveFieldNames()}, currentSearch);

        lastSearch = currentSearch;

        previousXHR = $.ajax({
            url: RESULT_FIELDS_URL + '?' + $.param({
                // for diffs to work accurately, we'd need to fetch all the results
                values: SearchConfig.topicmapMaxValues,
                singleQueryField: SearchConfig.parametricSingleQueryFieldname,
                process: 'TOPICMAP'
                
            }, true),
            type : 'POST',
            contentType : 'application/json',
            dataType : 'json',
            data: JSON.stringify(submitData),
            success: function(response){

                if (response.success) {
                    var fields = response.result;
                    
                    if (!fields.length) {
                        return;
                    }
    
                    // Workaround for lack of container: support in Bootstrap 2.0.4
                    $('.topicmap-tooltip-label').closest('.tooltip').remove();
    
                    categoryMap = processFieldData(fields);
                    lastSavedParams = currentSearch;
                    updateCategoryList();
                    rerender();
                } else {
                    $totalResults.html(SearchEvents.getSearchErrorMessage(response.error, response.errorDetail));
                }
            }
        }).always(function(){
            //$loading.hide()
            SearchEvents.toggleResultsLoading(false, $totalResults);
            $totalResults.text(totalResultsLabel);
            
        });
        
    }


    SearchEvents.$.on(SearchEvents.PARAMETRICS_LOADED, function(e, obj){
    	var data = obj.data;
    	if (SearchConfig.preloadParaValues) {
    		initParaData(obj.data);
    	} else {
				$.ajax({
        	url: SINGLE_FIELD_VALUES_URL + '?' + $.param({
                // for diffs to work accurately, we'd need to fetch all the results
                searchView: SearchEvents.getSelectedSearchView(),
                field: getSnomedField()
            }, true),
            type : 'POST',
            contentType : 'application/json',
            dataType : 'json',
            success: function(response){

                if (response.success) {
                    initParaData(response.result);
                } else {
                    //$totalResults.html(SearchEvents.getSearchErrorMessage(response.error, response.errorDetail));
                }
            }
        });    		
    	}
    	
    	
    });
	
    function initParaData(data) {		
        var snomed_field = getSnomedField();
        var snomed_parent_field = getSnomedParentField();

        var categories = data[snomed_field] ? _.keys(parseCategories(data[snomed_field].values)).sort() : null;
        
        var ignoreFields = {};
        ignoreFields[snomed_field] = true;
        ignoreFields[snomed_parent_field] = true;
        
        var searchView = SearchEvents.getSelectedSearchView();
        var paraFields = {};
         _.each(FilterFields[searchView], function(field) {
        	if (field.parametric) {
        		paraFields[field.name] = field;
        	} 	
        });
        
        var fields = parseFieldNames(paraFields, ignoreFields).sort( function(a, b) {
            var nameA = a.displayName.toLowerCase(), nameB= b.displayName.toLowerCase();
            if (nameA < nameB) {
                return -1;
            } else if (nameA > nameB) {
                return 1;
            }
            return 0;
        });

        fieldList = fieldsTemplate({categories: categories, fieldNames: fields});
        $topicList = $(fieldList).appendTo($update.empty());
        
        var isSnomed = false;
        if('SNOMED' == SearchConfig.defaultTopicBranch) {
        	isSnomed = true;
        }
         //'SNOMED' == SearchConfig.defaultTopicBranch;
        var $selectedTopicBranch = isSnomed ? $('#snomedTopicBranch') : $('#fieldsTopicBranch');
        if (isSnomed) {
        	$selectedTopicBranch.find('input').prop('checked', true);
        } else {
				var $fieldList = $('#fieldList', $topicList);
				var $defaultSelectedFields = $fieldList.find('.default-selected');
				  if ($defaultSelectedFields.length == $fieldList.find('.topicmap-input').length) {
					$selectedTopicBranch.find('input').prop('checked', true);
				  } else {
					$defaultSelectedFields.prop('checked', true);
				  }
        }
        
        $('#updatebtn', $topicList).on('touchstart click', function(event) {
            event.preventDefault();
            if (retrieveFieldNames().length > 0) {
                SearchEvents.attemptSearch();
            }
        });				

        $('li a', $topicList).on('touchstart click', function(event) {			
            event.stopPropagation();
            $(event.target).blur();
        });

        $('li.nav-header button', $topicList).on('touchstart click', function(event) {
            event.stopPropagation();
            if (event.target.nodeName == 'INPUT') {												
                return;
            }
            var $elem = $(this);
            $elem.parent().find('ul').slideToggle('fast');
            $elem.find('i').toggleClass("icon-minus");
        });

        $('.topicmap-header-input', $topicList).on('change',  $.proxy(function(event) {
            event.stopPropagation();
            var $target = $(event.target);
            var checked = $target.prop('checked') || false;
            $target.parent().nextUntil('li.nav-header').find('input').prop('checked', checked);	
        }, this));

        $('.topicmap-input', $topicList).on('change', $.proxy(function(event) {
            event.stopPropagation();
            var $target = $(event.target);
            var $header = $target.closest('li.nav-header').find('input.topicmap-header-input');
            var headerChecked = $header.prop('checked') || false;
            var checked = $target.prop('checked') || false;
            if (headerChecked && !checked) {
                $header.prop('checked', false);
            } else if (!headerChecked && checked){
                var $inputs = $target.closest('ul').find('input');
                var $checked = $inputs.filter(':checked');
                /*if ($inputs.length == $checked.length) {
                    $header.prop('checked', true);
                }*/
				if ($checked.length > getMaxTopicMapFieldsAllowed()) {
					alert("Only " + getMaxTopicMapFieldsAllowed() + " selections allowed for this view ");
					$target.closest('li a').find('input').prop('checked',false);
                }
            }
        }, this));
		$('#clearAll', $topicList).on('touchstart click', function(event) {
			event.stopPropagation();
			$("input[type='checkbox'], .topicmap").prop('checked',false);
		});
    }
    
    function getMaxTopicMapFieldsAllowed() {
        var selectedSearchView = SearchEvents.getSelectedSearchView();
        return SearchConfig.searchViews[selectedSearchView].topicMapMaxCount;
    }
    
    SearchEvents.$.on(SearchEvents.SEARCH_REQUEST_SENT, function() {
        if ($paper) {
            $paper.remove();
        }
        dataToRender = $paper = undefined;

        previousXHR && previousXHR.abort();
        previousXHR = null;
    });

    SearchEvents.$.on(SearchEvents.RESULTS_PROCESSING, function(e, results, totalResults, data) {
        totalOverAllDatabases = _.reduce(totalResults, function(sum, val){
            return sum + val;
        }, 0);
        
        
        if ($paper) {
            $paper.remove();
        }

        dataToRender = $paper = undefined;

        previousXHR && previousXHR.abort();

        //$loading.show();
        currentSearch = data;
        
        if ($topicmap.is(':visible')) {
            doSearch();
        }
        
    });
    
    var $paraFilterForm = $('#parametricForm').data('filterForm');

    function setSavedTooltip() {
        if (!savedResults) {
            return;
        }
	    var html = '';
	    
        if (savedParams.minScore) {
            html += '<div><span class="topicmap-tooltip-label">Min Score:</span> ' + _.escape(savedParams.minScore) + '</div>';
        }

        html += '<div><span class="topicmap-tooltip-label">Results:</span> ' + savedTotalCount + '</div>';

        html += '<div><span class="topicmap-tooltip-label">Query:</span> ' + _.escape(savedParams.query)+'</div>';

        var filters = savedParams.filterGroup;
        if (!_.isEmpty(filters)) {
	    	var filterString = $paraFilterForm.getFiltersDataString();
            html += '<div><span class="topicmap-tooltip-label">Filters: ' + _.escape(filterString)+'</span></div>';
        }


        $compareBtn.tooltip('destroy').tooltip({
            html: true,
            placement: 'bottom',
            title: '<div>Compare against stored search:</div>' + html
        });

        $saveBtn.tooltip('destroy').tooltip({
            html: true,
            placement: 'bottom',
            title: '<div>Replace stored search:</div>' + html
        });
    }

    function reduceData(dataMap) {
        var categories = [];
        var category, entry, i, val;

        var results = retrieveFieldList(dataMap, categories).sort( function(a, b) {
            return b.count - a.count;
        });

        var maxResults = results.length;
        maxResults = maxResults > numToDisplay ? numToDisplay : maxResults;

        for (i = 0; i < maxResults; ++i) {
            entry = results[i];
            val = dataMap[entry.fieldId].fieldValues[entry.index];
            category = categories[entry.catId];
            category.children.push({
                name: entry.name ? entry.name : val.value,
                rawCount: val.count,
                size: val.count,
                old: val.old,
                current: val.current,
                rawDiff: val.rawDiff
            });

            category.size += entry.count;
        }

        return _.compact(_.map(categories, function(rawValues){
            if (rawValues.children.length) {
                rawValues.rawCount = rawValues.size;
                return rawValues;
            }
            return false;
        }));
    }

    function retrieveFieldList(dataMap, categories) {
        var results = [];
        var values, catId;

        var start, valueLength, maxVal;
        var viewParametricFields = getParametricFields();
        
        _.each(dataMap, function(fieldData, fieldId) {
            values = fieldData.fieldValues;
            start = topDiscards;

            categories.push({
                discarded: 0,
                isSNOMED: fieldData.isSNOMED,
                field: fieldData.name,
                name: fieldData.isSNOMED ? fieldData.name : viewParametricFields[fieldData.name].displayName,
                size: 0,
                children: []
            });

            valueLength = values.length;
            maxVal = start + numToDisplay;
            if (maxVal < valueLength ) {
                valueLength = maxVal;
            }

            catId = categories.length - 1;
            for (; start < valueLength; ++start) {
                results.push({
                    catId: catId,
                    count: values[start].count,
                    fieldId: fieldId,
                    index: start
                });
            }
        });

        return results;
    }

    function retrieveFieldNames() {
        var fieldNames = [];
        if ($('#snomedList', $topicList).find('input:checked').length > 0) {
            fieldNames.push(getSnomedField());
        }
        $('#fieldList', $topicList).find('input:checked').each(function(index, input){
            fieldNames.push(input.value);
        });

        return fieldNames;
    }

    function retrieveDiffs(totalOverAllDatabases) {
        var results = [];
        var fieldData = getFieldData(categoryMap, savedResults);
        _.each(fieldData, function(item, name) {
            var i, length, entry;
            var reduced = {};

            // change old results to hashmap based on name as key  (iterate through all results)
            if (item.oldValues) {
                var fieldValues = item.oldValues.fieldValues;
                for (i = 0, length = fieldValues.length; i < length; ++i ) {
                    entry = fieldValues[i];
                    reduced[entry.value] = { old: entry.count };
                }
            }

            // update hashmap with current value from current search results (iterate through all results)
            var value, existing;
            fieldValues = item.currentValues.fieldValues;
            for (i = 0, length = fieldValues.length; i < length; ++i ) {
                entry = fieldValues[i];
                value = entry.value;
                existing = reduced[value];
                if (existing) {
                    existing.current = entry.count;
                }
                else {
                    reduced[value] = { current: entry.count };
                }
            }

            var diffs = _.map(reduced, function(obj, key){
                var currentPerc = (obj.current / totalOverAllDatabases) || 0;
                var oldPerc = (obj.old / savedTotalCount) || 0;
                var rawDiff = currentPerc - oldPerc;
                return {
                    // normalize by document count
                    count: Math.abs(rawDiff),
                    rawDiff: rawDiff,
                    current: currentPerc,
                    old: oldPerc,
                    value: key
                };
            }).sort(function(a,b){
                return b.count - a.count;
            });

            results.push({
                name: name,
                isSNOMED: item.currentValues.isSNOMED,
                fieldValues: diffs
            });
        });

        return results;
    }

    function getFieldData(data, oldData) {
        var fields = {};
        _.each(data, function(entry){
            fields[entry.name] = {
                currentValues: entry
            }
        });

        _.each(oldData, function(entry){
            var field = fields[entry.name];
            if (field) {
                field.oldValues = entry;
            }
        });

        return fields;
    }

    function updateCategoryList() {
        var i, length, input, value, category;
        var listView = [], tempView = [];

        var $snomed = $('#snomedList', $topicList).find('input:checked');
        if ($snomed.length > 0) {
            for (i = 0, length = $snomed.length; i < length; ++i) {
                value = $snomed[i].value;
                if (categoryMap[value]) {
                    tempView.push({ label: value, value: value });
                    if (!category) {
                        category = value;
                    }
                }
            }

            if (tempView.length > 0 ) {
                listView.push({ startGroup: true, label: 'Concept Branches' });
                listView = listView.concat(tempView);
                listView.push({ endGroup: true });
            }
        }

        var $fields = $('#fieldList', $topicList).find('input:checked');
        var viewParametricFields = getParametricFields();
        if ($fields.length > 0) {
            tempView = [];
            for (i = 0, length = $fields.length; i < length; ++i) {
                value = $fields[i].value;
                if (categoryMap[value]) {
                    tempView.push({ label: viewParametricFields[value].displayName, value: value });
                    if (!category) {
                        category = value;
                    }
                }
            }

            if (tempView.length > 0 ) {
                listView.push({ startGroup: true, label: 'Fields' });
                listView = listView.concat(tempView);
                listView.push({ endGroup: true });
            }
        }

        $tmlvFieldSelect.multiselect('dataprovider', listView);

        if (category) {
            updateParametricList(categoryMap[category]);
        }
    }


    function processFieldData(fields) {
        var categoryMap = {};
        var i, length, entry;

        for (i = 0, length = fields.length; i < length; ++i) {
            entry = fields[i];
            if (entry.name == getSnomedField()) {
                processSNOMEDResults(entry.fieldValues, categoryMap);
            } else {
                categoryMap[entry.name] = entry;
            }
        }

        return categoryMap;
    }

    function processSNOMEDResults(rawData, categoryMap) {
        var i, length, val, name, lastIdx, category, label, entry;

        var fieldNames = {};
        $('#snomedList', $topicList).find('input:checked').each(function(index, input){
            fieldNames[input.value] = true;
        });

        for (i = 0, length = rawData.length; i < length; ++i) {
            val = rawData[i];

            // assumes the values are of the form e.g. 'INFILTRATION (MORPHOLOGIC ABNOMALITY)'
            name = val.value;
            lastIdx = name.lastIndexOf(' (');
            category = name.slice(lastIdx + 2, name.length - 1);
            label = name.slice(0, lastIdx);

            if (fieldNames[category]) {
                entry = categoryMap[category];
                if (!entry) {
                    categoryMap[category] = {
                        name: category,
                        isSNOMED: true,
                        numValues: 0,
                        totalValues: 0,
                        fieldValues: []
                    };

                    entry = categoryMap[category];
                }

                entry.fieldValues.push({
                    count: val.count,
                    value: label
                });

                ++entry.numValues;
                ++entry.totalValues;
            }
        }

        return categoryMap;
    }

    function updateParametricList(category) {
        var listView = [];
        var viewParametricFields = getParametricFields();
        var meta = viewParametricFields[category.isSNOMED ? getSnomedField() : category.name];
        var node, value;
        var length = category.fieldValues.length;
        if (length > maxToFetch) {
            length = maxToFetch;
        }
        for (var i = 0; i < length; ++i ) {
            node = category.fieldValues[i];
            value = meta.type == 'range' ? node.value.replace(',', '\u2192') : node.value;
            listView.push({
                count: node.count,
                label: value,
                value: value
            });
        }

        $tmlvParameter.multiselect('dataprovider', listView);
    }


    function rerender() {
        dataToRender = {
            name: '',
            size: 1,
            children: reduceData(categoryMap)
        };

        renderIfVisible();
    }
});

