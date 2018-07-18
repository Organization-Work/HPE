jQuery(function($){
    
	var tempID  =  this.versionObj;
	var tempID =  localStorage.getItem("storageId");
	
    var totalOverAllDatabases, prevFormat;
    
    var FIELD_VALUES_HUE = 0.32;
    var TOTAL_TAGGED_HUE = 0.08;
    //var ROW_PCT_HUE = 0.19;
    var ROW_PCT_HUE = COL_PCT_HUE = 0.49;

    var TABLE_VIEWER_URL = 'ajax/search/getTableViewer.json';

    var $tableviewer = $('#tableviewer');
    var $visualPageContainer = $('#show');

    var settingsTemplate = $.resGet('../templates/search/tableviewer.settings'+tempID+'.template');
    var tableMarkup = _.template($.resGet('../templates/search/tableviewer.table'+tempID+'.template'), undefined, {variable: 'ctx'});
    var fieldTemplate = _.template($.resGet('../templates/search/tableviewer.fieldpicker'+tempID+'.template'), undefined, {variable: 'ctx'});

    var $totalResults = $('<div id="tableviewer-totals" class="view-results-label"></div>').appendTo($tableviewer);
    var $loading = $('<div class="visualizer-loading"></div>').appendTo($tableviewer).hide();
    var $settings = $(settingsTemplate).appendTo($tableviewer).on('click', '.tableviewer-refresh', function(evt){
        evt.preventDefault();
        doQuery();
    }).on('click', '.tableviewer-settings', function(evt){
        // prevent the popup from closing when you click on the input buttons
        evt.stopPropagation();
    });
    
    var $formatOptions = $('.table-format-options').on('click', function(e) {
        e.preventDefault();
        
        $('.table-format-options').removeClass('selected');
        $(this).addClass('selected');
        
        toggleOutputFormat();
        
        
    });
    
    var xFieldSelectLabel = 'Select X-Axis Field';
    var $xFieldPicker = $('<div class="control-group fieldpicker"></div>').html(fieldTemplate({
        label: xFieldSelectLabel
    })).appendTo($tableviewer).on('click', 'a', function(){
        var $el = $(this);
        var name = $el.data('fieldname');
        var field = _.find(parametrics, function(field){ return field.name === name; });
        
        if (field) {
            if (field === yField) {
                return false;
            }

            xField = field;
            xFilterValue = $(this).find('input').val() || undefined;
            setFieldPickerLabel($el.closest('.fieldpicker'), xField, xFilterValue);
            doQuery();
        }
    });
    

    var yFieldSelectLabel = 'Select Y-Axis Field';
    var $yFieldPicker = $('<div class="control-group fieldpicker"></div>').html(fieldTemplate({
        label: yFieldSelectLabel
    })).addClass('pull-left').appendTo($tableviewer).on('click', 'a', function(){
        var $el = $(this);
        var name = $el.data('fieldname');
        var field = _.find(parametrics, function(field){ return field.name === name; });

        if (field) {
            if (field === xField) {
                return false;
            }

            yField = field;
            yFilterValue = $(this).find('input').val() || undefined;
            setFieldPickerLabel($el.closest('.fieldpicker'), yField, yFilterValue);
            doQuery();
        }
    });

    $xFieldPicker.add($yFieldPicker).on('click', '.tableviewer-fieldpicker-snomed input', function(evt){
        return false;
    }).on('click', '.dropdown-toggle', function(){
        var $input = $(this).closest('.fieldpicker').find('input');
        if ($input.data('typeahead')) {
            return;
        }

        $input.typeahead({
            source: function(query, process){
                var values = $('a.fieldlist-control').data('fieldlistControl').fieldValues;

                var snomed = _.find(values, function(a){
                    return a.displayName === 'Concept+';
                });

                return snomed && snomed.values || [];
            }
        }).on('keypress', function(e){
            if (e.keyCode === 13) {
                $(this).closest('a').trigger('click');
            }
        })
    });
    
    function resetTable() {
    	$table && $table.remove();
    	$(this).toggleClass('hide', true);

      $xFieldPicker.css('margin-left', '');
      $xFieldPicker.find('.fieldlabel').text(xFieldSelectLabel);

      $yFieldPicker.css('margin-top', '');
      $yFieldPicker.find('.fieldlabel').text(yFieldSelectLabel);
      
      xField = yField = null;
          	
    }
    
    var $resetBtn = $('#tviewResetButton');
    $resetBtn.click(function() {
    	resetTable();
    });
    
   
    

    function buildFieldPickerLabel(field, filterValue) {
        return field.displayName + (filterValue ? ' [' + filterValue + ']' : '');
    }

    function setFieldPickerLabel($fieldpicker, field, filterValue) {
        $fieldpicker.find('.fieldlabel').text(buildFieldPickerLabel(field, filterValue));
    }

    $('[href=#tableviewer]').on('shown', function(){
        if (!_.isEqual(lastSearch, currentSearch)) {
            doQuery();
        }
        
        return false;        
    });

    var $table;
    var currentSearch, lastSearch, previousXHR, xField, yField, xFilterValue, yFilterValue, parametrics = [];

    SearchEvents.$.on(SearchEvents.RESULTS_PROCESSING, function(e, results, totalResults, data) {
        totalOverAllDatabases = _.reduce(totalResults, function(sum, val){
            return sum + val;
        }, 0);
        
        SearchEvents.toggleResultsLoading(false, $totalResults);

        currentSearch = data;

        var parametricsFields = _.filter(FilterFields[SearchEvents.getSelectedSearchView()], function(field){
            var isSnomed = /^Concept\+?$/.test(field.displayName);
            return field.parametric && !field.parametric.ranges && !isSnomed;
        });
        
        parametrics = _.sortBy(parametricsFields, "displayName");

        $xFieldPicker.html(fieldTemplate({
            label: xField && buildFieldPickerLabel(xField, xFilterValue) || 'Select X-Axis Field',
            snomedFilter: xFilterValue,
            fields: parametrics
        }));

        $yFieldPicker.html(fieldTemplate({
            label: yField && buildFieldPickerLabel(yField, yFilterValue) || 'Select Y-Axis Field',
            snomedFilter: yFilterValue,
            fields: parametrics
        }));

        
        if ($tableviewer.is(':visible')) {
            doQuery();
        }
    });


    function doQuery(){
        previousXHR && previousXHR.abort();
        
        lastSearch = currentSearch;

        var totalResultsLabel = '  Total ' + SearchSettings.getResultViewText() + ': ' +  SearchEvents.getFormattedResult(totalOverAllDatabases);
        $totalResults.text(totalResultsLabel);


        if (xField) {
            // try to reselect the correct field when switching between views
            xField = _.find(parametrics, function(a){return a.displayName === xField.displayName});
        }

        if (yField) {
            yField = _.find(parametrics, function(a){return a.displayName === yField.displayName});
        }

        if (!xField || !yField) {
            return;
        }

        var lastXField = xField, lastYField = yField,
            lastXFilterValue = xFilterValue, lastYFilterValue = yFilterValue;

        if ($table) {
            $table.remove();
        }

        //$loading.show();
        SearchEvents.toggleResultsLoading(true, $totalResults);

        previousXHR = $.ajax(TABLE_VIEWER_URL + '?' + $.param({
            xField: lastXField.name,
            yField: lastYField.name,
            xFilterValue: xFilterValue,
            yFilterValue: yFilterValue,
            nX: $('#tableviewer-max-cols').val(),
            nY: $('#tableviewer-max-rows').val(),
            sort: $('#tableviewer-sort').val(),
            includeParent: $('#tableviewer-include-parent').is(':checked')
        }), {
            data: JSON.stringify(currentSearch),
            type : 'POST',
            contentType : 'application/json',
            dataType : 'json'
        }).always(function(){
            //$loading.hide();
            SearchEvents.toggleResultsLoading(false, $totalResults);
        }).fail(function(response) {
            $totalResults.html(SearchEvents.getSearchErrorMessage("Error loading tableview data from server"));
        }).done(function(json){

            if ($table) {
                $table.remove();
            }
            
            $resetBtn.toggleClass('hide', false);

            // var xvalues = formatTableValues(json.xvalues);            
            // var yvalues = formatTableValues(json.yvalues);
            var xvalues=json.xvalues;
            var yvalues=json.yvalues;
            var table = json.table;

            var xmisc = _.map(json.xvalues, function(field, xIdx){
                return field.count - _.reduce(table, function(sum, xRow){
                    return sum + xRow[xIdx];
                }, 0)
            });

            var ymisc = _.map(json.yvalues, function(field, yIdx){
                return field.count - _.reduce(table[yIdx], function(sum, val){
                    return sum + val;
                }, 0)
            });

            $table = $(tableMarkup({
                table: table,
                xvalues: xvalues,
                yvalues: yvalues,
                xmisc: xmisc,
                ymisc: ymisc,
                xField: lastXField,
                yField: lastYField,
                totalResults: totalOverAllDatabases
            })).appendTo($tableviewer).on('click', '.x-misc', function(){
                $tableviewer.toggleClass('hide-x-misc');
                return false;
            }).on('click', '.y-misc', function(){
                $tableviewer.toggleClass('hide-y-misc');
                return false;
            }).on('click', 'td.fieldvalue-xy', function(){
                var $el = $(this);
                // we subtract one from the indexes to remove the column/row headings
                var xValue = xvalues[$el.index() - 1].value;
                var yValue = yvalues[$el.closest('tr').index() - 1].value;

                var paraFilters = {};
                paraFilters[lastXField.name] = [{type: PARAMETRIC_FIELDTYPE, val: xValue}];
                paraFilters[lastYField.name] = [{type: PARAMETRIC_FIELDTYPE, val: yValue}];

                $('#parametricForm').data('filterForm').insertLoadingFilterSet()
                    .loadFilters({
                        boolOperator: 'AND',
                        filterFields: paraFilters,
                        childGroups: null,
                        tag: ''
                    }, null, null, true);

                return false;
            }).on('click', 'th.fieldvalue-x', function(){
                // we subtract one from the indexes to remove the column/row headings
                var xValue = xvalues[$(this).index() - 1].value;
                addParametricFilter(lastXField, xValue);
            }).on('click', 'th.fieldvalue-y', function(){
                // we subtract one from the indexes to remove the column/row headings
                var yValue = yvalues[$(this).closest('tr').index() - 1].value;
                addParametricFilter(lastYField, yValue);
            });

            function addParametricFilter(field, value) {
                $('#parametricForm').data('filterForm').loadFilter({id: field.name, 
                                                                    value: value,
                                                                    fieldType: PARAMETRIC_FIELDTYPE});
            }
            $table.css('max-height', $visualPageContainer.height() * 0.85);
            $xFieldPicker.css('margin-left', $table[0].offsetLeft + 0.5 * Math.min($table.width(), $table.find('table').width()));
            setFieldPickerLabel($xFieldPicker, lastXField, lastXFilterValue);

            $yFieldPicker.css('margin-top', 0.5 * $table.height());
            setFieldPickerLabel($yFieldPicker, lastYField, lastYFilterValue);
            
            prevFormat = null;
            toggleOutputFormat();
        });
    }
    
    
    function toggleOutputFormat() {
        var $selectedOtpion = $formatOptions.filter('.selected');
        var selectedFormat = $selectedOtpion.attr('name');
        if (selectedFormat === prevFormat) {
            return false;
        }
        
        $('#formatSelector').text($selectedOtpion.text());
        
        var renderHeatMap;
        
        if ('pct_row' === prevFormat || 'pct_col' === prevFormat) {
            $('.row-pct').removeClass('row-pct');
            $('.col-pct').removeClass('col-pct');
        }
        
        switch (selectedFormat) {
            case 'count':
                renderHeatMap = 'pct_total' !== prevFormat;
                showTotalBasedValues(false, renderHeatMap);
                break;
            case 'pct_total':
                renderHeatMap = 'count' !== prevFormat;
                showTotalBasedValues(true, renderHeatMap);
                break;
            case 'pct_row':
                showRowPct();
                break;
            case 'pct_col':
                showColumnPct();
                break;
            default:
                showTotalBasedValues(false, false, true);
                break;
                
        }
        
        prevFormat = selectedFormat;
        
            
    }
    
    function showRowPct() {
        var styleClass = 'row-pct';
        
        $('tr.fieldvalue-xy').each(function() {
            var rowTotal = $(this).find('td.total-y').data('count');
            var $rowCells = $(this).find('td.fieldvalue-xy,td.y-misc');
            var maxCell = _.max($rowCells, function(ele) {
                return $(ele).data('count');              
            	// return parseInt($(ele).data('count').replace(/,/g, ''));
            });
            processRowColPct($rowCells, rowTotal, $(maxCell).data('count'), ROW_PCT_HUE, styleClass);
        });


        $('tr.x-misc').each(function() {
            var rowTotal = $(this).find('td.misc-total').data('count');
            // var $rowCells = $(this).find('td.misc.x-misc').data('count');
            var $rowCells = $(this).find('td.misc.x-misc')
            var maxCell = _.max($rowCells, function(ele) {
                return $(ele).data('count');              
                // return parseInt($(ele).data('count').data('count').replace(/,/g, ''));
            });
            processRowColPct($rowCells, rowTotal, $(maxCell).data('count'), ROW_PCT_HUE, styleClass);
        });
       
        $('td.misc-misc').addClass(styleClass);
       
        var $taggedCellsY = $('td.total-y,td.misc-total.x-misc');
        var maxTaggedValY = processTotalBasedCells($taggedCellsY, 'count');
        renderTotalHeatMap($taggedCellsY, 'count', maxTaggedValY, TOTAL_TAGGED_HUE);
        $taggedCellsY.addClass(styleClass);
        
        $('.tableviewer-container th').removeClass('selected');
        $('th.fieldvalue-y').toggleClass('selected');

        $('tr.total-x').hide();
        $('th.total-x,td.total-y,td.misc-total.x-misc').show();
    }
    
    function processRowColPct($eles, total, maxVal, hue, styleClass) {
        
        $eles.each(function() {
            var val = $(this).data('count');
            var factor = maxVal ? val / maxVal : 0;
            var pct = val ? (val / total * 100).toFixed(2) : 0;
        	fpct=numberFormat(pct);
        	$(this).text(fpct + '%');
            setBackgroundColor($(this), hue, factor);
            $(this).addClass(styleClass);
        });
    };
    
    
    function showColumnPct() {
        var styleClass = 'col-pct';
        
        $('tr.total-x').find('td.total-x,td.misc-total.y-misc').each(function(index) {
            var colTotal = $(this).data('count');
            var colIdx = $(this).data('col');
            var $colCells = $('td.c-' + colIdx);
            var maxCell = _.max($colCells, function(ele) {
                return $(ele).data('count');
            	// return parseInt($(ele).data('count').replace(/,/g, ''));
            });
            
            processRowColPct($colCells, colTotal, $(maxCell).data('count'), COL_PCT_HUE, styleClass);
        });
        
        $('td.misc-misc').addClass(styleClass);
        
        var $taggedCellsX = $('td.total-x,td.misc-total.y-misc');
        var maxTaggedValX = processTotalBasedCells($taggedCellsX, 'count');
        renderTotalHeatMap($taggedCellsX, 'count', maxTaggedValX, TOTAL_TAGGED_HUE);
        $taggedCellsX.addClass(styleClass);
        
        $('.tableviewer-container th').removeClass('selected');
        $('th.fieldvalue-x').toggleClass('selected');
       
        $('th.total-x,td.total-y,td.misc-total.x-misc').hide();
        $('tr.total-x').show();
        
    }
    
    
    function showTotalBasedValues(isPct, renderHeatMap, useDefaultStyle) {
        var key = isPct ? 'pct' : 'count';
        var suffix = isPct ? '%' : '';
        
        var $fieldCells = $('td.fieldvalue-xy,td.misc.x-misc,td.misc.y-misc');
        var maxFieldVal = processTotalBasedCells($fieldCells, key, suffix);
        
        var $taggedCellsX = $('td.total-x,td.misc-total.y-misc');
        var maxTaggedValX = processTotalBasedCells($taggedCellsX, key, suffix);

        var $taggedCellsY = $('td.total-y,td.misc-total.x-misc');
        var maxTaggedValY = processTotalBasedCells($taggedCellsY, key, suffix);

        $('.tableviewer-container th').removeClass('selected');
        $('th.total-x,td.total-y,td.misc-total.x-misc').show();
        $('tr.total-x').show();
        
        if (renderHeatMap) {
            renderTotalHeatMap($fieldCells, key, maxFieldVal, FIELD_VALUES_HUE);
            renderTotalHeatMap($taggedCellsX, key, maxTaggedValX, TOTAL_TAGGED_HUE);
            renderTotalHeatMap($taggedCellsY, key, maxTaggedValY, TOTAL_TAGGED_HUE);
            
        } else if (useDefaultStyle) {
            renderDefaultStyle($fieldCells);
            renderDefaultStyle($taggedCellsX);
            renderDefaultStyle($taggedCellsY);
        }
    }
    
    function processTotalBasedCells($cells, key, suffix) {
        var maxVal = 0;
        
        suffix = suffix || '';
        
        $cells.each(function() {
            var val = $(this).data(key);
            if (val!=null) {
            	// val2=parseInt(val.replace(/,/g, ''))
	            if (!_.isUndefined(val)) {
	            	fval=numberFormat(val);
	                $(this).text(fval + suffix);
	                maxVal = Math.max(maxVal, val);
	            };
            }
        });
        
        return maxVal;
    }
    
    function renderTotalHeatMap($fieldCells, key, max, hue) {

        $fieldCells.each(function() {
            var factor = max ? $(this).data(key) / max : 0;
            setBackgroundColor($(this), hue, factor);
        });
    }
    
    function renderDefaultStyle($fieldCells) {
        $fieldCells.each(function() {
            $(this).css('background-color', '');
        });
    }
    
    function setBackgroundColor($ele, hue, factor) {
        var sat = 0.1 + (0.9 * factor);
        var brightness = 1.0; //0.8 + (0.2 * factor);
        var color = Raphael.hsb(hue, sat, brightness) ;
        $ele.css('background-color', color);
        
    }
    
    function formatTableValues(values) {
    	_.each(values, function(obj) {    		
			if(obj) {
				if(obj.count) {
					var replace = SearchEvents.getFormattedResult(obj.count)
					obj.count = replace;
				}
			}
    	});
    	return values;    	
    }
    
    var numberFormat=function(num) {
    	
       
        	var formattedNum = num.toString();	
        	formattedNum = parseFloat(Math.round(formattedNum + 'e2') + 'e-2');
        	formattedNum = formattedNum.toString();
        	
        	var res = formattedNum.split(".");	 
        	if(formattedNum.indexOf('.') != -1) {	 
        	  if(res[1].length < 2) {	  	
        	  	formattedNum = Number(formattedNum).toFixed(2);
        	  }
        	}
        	formattedNum = formattedNum.toString();
            var result = formattedNum.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
            return result;
        
    };
});
