jQuery(function($){

	//var tempID  =  this.versionObj;
	var tempID =  localStorage.getItem("storageId");
	
    // TODO: add to settings
	var xhr = null;
    var maxSunburstFields = 500;
    var maxMultiSelectText = 60;
    
    var maxFieldListChars = 30;

    var currentSearch, lastSearch, previousXHR, paraFieldsValue;

    var sunburst, lastTotalResults, activeField;
     
    var FILTERS_LIST_TEMPLATE = '../templates/search/sunburst.fieldsList'+tempID+'.template';
   
    var maxFields =$("#histogram-max-rows").val() ? $("#histogram-max-rows").val():SearchConfig.sunburstMaxValues;
    var RESULT_FIELDS_URL = 'ajax/search/getFilteredResultFields.json?values=' + maxFields + '&singleQueryField=' + SearchConfig.sunburstSingleQueryFieldname + '&process=BARCHART';
    var $filterchart = $('#filterchart').empty();

    var $showPanel = $('#showPanel');
    
    var lastFetchError = false;

    var $listViewEl = $('<div id="sunburst-listview" class="btn-toolbar"></div>').appendTo($filterchart);
    var $noResultEl = $('<div id="no-results"></div>').appendTo($filterchart);
    var $chartEl = $('<div id="sunburst-chart"></div>').appendTo($filterchart);
    var resultCount = $("#histogram-max-rows").val()?$("#histogram-max-rows").val():20;
    var stateChange = false;
    var $filtersEl = $('<div id="sunburst-filters"></div>').appendTo($filterchart).on('click', '.field-group-list li', function(){
        $('.field-group-list li.active').removeClass('active');
        var $el = $(this).addClass('active');
        //$el.siblings('li').removeClass('active');
        activeField = $el.data('sunburstfield');
        
        if (!paraFieldsValue[activeField]) {
        	fetchParaValues(activeField);
        } else {
        	rerender();
        }
        if(lastFetchError) {
        	 SearchEvents.attemptSearch();
        }
    });
    
    var $paraFilterForm = $('#parametricForm').data('filterForm');

    var $btnGroupEl = $('<div class="btn-group"></div>').appendTo($listViewEl);
    var $listViewBtn = $('<button class="btn btn-mini btn-info addFilters" data-toggle="tooltip" title="Add selected filters"><i class="icon-white icon-arrow-left"></i> Add Filters</button>').appendTo($btnGroupEl);
    var $multiSelect = $('<select multiple="multiple" style="display: none;"> </select>').appendTo($listViewEl);
    var $settingTemplate =	$($.resGet('../templates/search/histogram.settings'+tempID+'.template')).appendTo($listViewEl).on('click', '.histogram-settings', function(evt){
        // prevent the popup from closing when you click on the input buttons
        evt.stopPropagation();
    });
    
    var $displaySQLTemplate =	$($.resGet('../templates/search/display.cohort'+tempID+'.template')).appendTo($listViewEl).on('click', '.cohort-settings', function(evt){
        // prevent the popup from closing when you click on the input buttons
        evt.stopPropagation();
    });
   


    var clearChart = function() {
        sunburst && sunburst.redraw({
            name: '',
            size: 0,
            data: []
        }, false);
        
    };
    
    var clearSelected = function(listView) {
        var data = [];
        $('option:selected', listView).each(function() {
            data.push($(this).val());
        });
        listView.multiselect('deselect', data);

        return data;
    };
    


    $listViewBtn.on('click', function(e){
        e.preventDefault();    
         
        var data = clearSelected($multiSelect);
        if (data.length > 0) {
            data = _.map(data, function(value) {
                return {
                    type: PARAMETRIC_FIELDTYPE,
                    val: value
                };
            });
            // update data
            var filterData = {};
            filterData[activeField] = data;
            $('#parametricForm').data('filterForm').insertLoadingFilterSet()
                .loadFilters({
                    boolOperator: 'OR',
                    filterFields: filterData,
                    childGroups: null,
                    tag: ''
                }, null, null);
            }
          	//  $(".filter-group .filter-contextmenu").hide();
            rerender();
            
            
    });

    $multiSelect.multiselect({
        buttonClass: 'btn btn-mini',
        buttonContainer: $btnGroupEl,
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
    $multiSelect.multiselect('dataprovider', []);

    var $totalResults = $('<div id="sunburst-totals" class="view-results-label"></div>').appendTo($filterchart);

    var color = d3.scale.ordinal().range(["#5ab7e0","#acdbef","#42ccbe","#a0e5de","#ea9047","#f6c498","#9bda6a","#cdecb4",
        "#7a9cfc",
        "#b8c7fe",
        "#ffd428",
        "#ffe993",
        "#fa726c",
        "#fcb8b5",
        "#84a2a5",
        "#c1d0d2",
        "#d392f4",
        "#e9c8f9",
        "#d9ac59",
        "#ecd5ac",
        "#a09bfc",
        "#cfcdfd",
        "#b7c11e",
        "#dbe08e",
        "#7a9cfc",
        "#b8c7fe",
        "#cc6a7d",
        "#e5b4be",
        "#47a1ea",
        "#a3d0f4",
        "#e0835a",
        "#efc1ac",
        "#a96ada",
        "#d4b4ec",
        "#a58784",
        "#d2c3c1",
        "#719cbc",
        "#b8cddd",
        "#81b273",
        "#c0d8b9"
    ]);

    $('[href=#filterchart]').on('shown', function() {
        if (!_.isEqual(lastSearch, currentSearch)) {
            doSearch();
        }
        SearchEvents.toggleResultsLoading(false, $totalResults); // do not show loading if we are done
        return false;
    });
    
    $("#histogram-max-rows").on("change",function(){
        stateChange =  true;
    });
    
    $(".histogram-refresh-order").click(function() {   	
    	if($(this).find('span').hasClass('icon-arrow-up')){
        	$(this).attr("data-order",'desc');
        	$(this).find('span').removeClass('icon-arrow-up').addClass('icon-arrow-down');
        } else {
        	$(this).attr("data-order",'asc');
        	$(this).find('span').removeClass('icon-arrow-down').addClass('icon-arrow-up');
        } 
        
    });
    
    $(".histogram-refresh").click(function(){
        $chartEl.empty();
        if(maxFields > 5000){ stateChange = true;}
        if(stateChange){
            SearchEvents.toggleResultsLoading(true, $totalResults);
            maxFields = $("#histogram-max-rows").val();
            sortOrder = $(".arrow-box-hover").attr('data-order');
            if(maxFields <= 5000) {
                RESULT_FIELDS_URL = 'ajax/search/getFilteredResultFields.json?values=' + maxFields + '&singleQueryField=' + SearchConfig.sunburstSingleQueryFieldname +'&process=BARCHART';
            	doSearch();
            } else {
               var dialog = $('#showMetaFieldsDialog');
                dialog.find('.meta-title').text("Fields alert");
                dialog.find('.modal-body').html("This exceeds the maximum allowable rows on this screen.  Please choose less than 5000 rows.");
                dialog.modal('show');
                SearchEvents.toggleResultsLoading(false, $totalResults);
                $("#histogram-max-rows").val('500');
            }
            stateChange =false;
        }
            onShown();


    })
    
    $(".copy-clipboard").click(function(){
    	var copyText = document.getElementById("cohort-query");
    	copyText.select();
    	document.execCommand("Copy");
    	//alert("Copied the text: " + copyText.value);
    });
    
    function onShown(){
        require(['Autn/bargraph'], function(BarGraph){

            $chartEl.css({
                width: '90%',
                height: $showPanel.height() - $listViewEl.height()
            });
            sunburst = new BarGraph($chartEl,{
            	dataOptions: function(d) {
                    if (d.filter) {
                        var filterData = {id: activeField, 
                                          value: d.rawName || d.name,
                                          fieldType: PARAMETRIC_FIELDTYPE};
                                       
                        $paraFilterForm.loadFilter(filterData);
                    }
            	},
            	onShortClick: function(d) {
                    if (d.filter) {
                        var filterData = {id: activeField,
                            value:d.rawName || d.name,
                            fieldType: PARAMETRIC_FIELDTYPE};

                        $paraFilterForm.loadFilter(filterData);
                    }
                },
                arcLabelFormatter: function(d){
                    var name = d.name;
                    var maxChars = 32;
                    if (name.length > maxChars) {
                        name = name.slice(0, maxChars - 1) + '\u2026';
                    }

                    return $('<div/>').html(name).text();
                },
                formatPercent : function(d,maxValue){
                     return Math.round((d/maxValue) * 100);
                },
                colorFn: function(d){
                    return color(d.name);
                },
                formatPercentage : $("#histogram-include-parent").prop("checked"),
                maximumRowsDisplayed : function(){
                    return maxSunburstFields;
                },
                maximumFieldsAllowed : $("#histogram-max-rows").val(),
                sortField :$("#histogram-sort option:selected").val()
            });
            
            
            rerender();
        });
    }

    function rerender() {
        if (!sunburst || _.isEmpty(paraFieldsValue)) {
            return;
        }

        maxSunburstFields = $("#histogram-max-rows").val();
        var toDraw;
        var multiselectData = [];

        if (activeField) {
            var desired = _.find(_.values(paraFieldsValue), function(data){
                return data.name === activeField;
            });
            
            var selectedSearchView = SearchEvents.getSelectedSearchView();
            var viewFilterFields = FilterFields[selectedSearchView];

            var meta = viewFilterFields[activeField];
            var label = meta.displayName;

            var SNOMED_FIELD = SearchConfig.searchViews[selectedSearchView].snomedTag;
            var SNOMED_PARENT_FIELD = SearchConfig.searchViews[selectedSearchView].snomedParentTag;

            if (desired) {
                var isNotSnomed = activeField !== SNOMED_FIELD && activeField !== SNOMED_PARENT_FIELD;

                var fieldMap = [];
                
                var dataType = meta.vertica && meta.vertica.dataType  ? meta.vertica.dataType : "TEXT";
                
                // check if this a numeric range field
                if(SearchConfig.groupFields[activeField] && SearchConfig.groupFields[activeField].groupType && SearchConfig.groupFields[activeField].groupType == "range") {
                	dataType = "PARARANGES"
                }

                // build data for multiselect and sunburst
                for (var i = 0, len = desired.fieldValues.length; i < len; ++i) {
                    var node = desired.fieldValues[i];
                    if (node != null) {
	                    var value = node.value;
	                    var countValue = SearchEvents.getFormattedResult(node.count);
	                    var labelVal = meta.parametric && meta.parametric.ranges ? getRangeLabel(value) : value;	                     
	                    multiselectData.push({count: countValue, label: labelVal, value: value});
	                    
	                    var minval = 0;
	                    if(dataType && dataType == "PARARANGES") {
	                    	var groups = SearchConfig.groupFields[activeField].groups;
	                    	for(var j = 0, len = groups.length; j < len; ++j) {
	                    		var group = groups[j];
	                    		if(group.groupLabel == value) {
	                    			minval = group.minVal ? group.minVal : 0;
	                    		}
	                    	}
	                    }
	                    
	                    if (i < maxSunburstFields) {
	                        fieldMap.push({
	                            filter: label,
	                            name: isNotSnomed ? labelVal : value.slice(0, value.lastIndexOf(' (')),
	                            rawName: value,
	                            size: node.count,
	                            data: [],
	                            totalNumResults: desired.totalValues,
	                            numValues: len,
	                            minVal:minval
	                            
	                        });
	                    }
	                  }
                }
            
                
                var toSortOrder = $('.arrow-box-hover').attr("data-order");
                
                toDraw =  {
                    data: fieldMap,
                    name: 'Total' + (lastTotalResults ? ': ' + lastTotalResults : ''),
                    totalCount: lastTotalResults,
                    resultLabel: SearchSettings.getResultViewText(),
                    dataType: dataType,
                    sortOrder : toSortOrder
                };
            }
        }

        $filterchart.css('min-height', $filtersEl.position().top + $filtersEl.outerHeight(true));
        $multiSelect.multiselect('dataprovider', multiselectData);
        
        if (lastTotalResults > 0) {
		      if (toDraw==null || _.isEmpty(toDraw.data)) {
		      	$("#no-results").html('<span style="color: red;">No data to render.</span>');
		      } else {
		      	$("#no-results").html('');
		      }
        }

        sunburst.redraw(toDraw || {
            name: '',
            size: 0,
            data: [],
            resultLabel: '',
            dataType: false
        });
	      
    }
    
    function getRangeLabel(value) {
    	var label = values;
    	
    	var values = value.split(',');
    	if (values.length > 0 && values[0] && values[0].length > 0) {
    		var numVal = Number(values[0]);
    		
    		label = _.isNaN(numVal) ? values[0] : getFormattedNumber(numVal);

    		label += '-';
    		
    		if (values.length > 1 && values[1] && values[1].length > 0) {
    			numVal =  Number(values[1]);
    			label += _.isNaN(numVal) ? values[1] : getFormattedNumber(numVal);
    		}
    	}
    	
    	return label;
    	
    }
    
    function getFormattedNumber(num) {
    	return  num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');;
    };
    
    
    function fetchParaValues(field) {
    	clearChart();
    	if(sunburst) {
    		sunburst.clearAll();
    	}
    	SearchEvents.toggleResultsLoading(true, $totalResults);
    	var submitData = _.extend({fieldNames: [field]}, currentSearch);
    	
    	if(xhr != null) {
    		xhr.abort();
    		xhr = null;
    		if(sunburst) {
        		sunburst.clearAll();
        	}
    	}
    	
    	xhr = $.ajax({
          url: RESULT_FIELDS_URL,
          type : 'POST',
          contentType : 'application/json',
          dataType : 'json',
          data: JSON.stringify(submitData),
          success: function(response){

              if (response.success) {
                	_.each(response.result, function(field) {
                		paraFieldsValue[field.name] = field;
                	});
                	// clear the chart again just to be sure before repainting
                	if(sunburst) {
                		sunburst.clearAll();
                	}
                	lastFetchError = false;
                	rerender();
                	
                  
              } else {
                  $totalResults.html(SearchEvents.getSearchErrorMessage(response.error, response.errorDetail));
            	  //$("#no-results").html('<span style="color: red;">No data to render.</span>');
            	  if(sunburst) {
              		sunburst.clearAll();
              	  }
            	  lastFetchError = true;
              }
          }/*,
          async: false*/
      }).always(function(){
          //$loading.hide()    	  
          SearchEvents.toggleResultsLoading(false, $totalResults);
      });    		
    	

    }
    
    function doSearch() {
    	// loading.show()
    	SearchEvents.toggleResultsLoading(true, $totalResults);
    	
        lastSearch = currentSearch;
        paraFieldsValue = {};
        
        var submitData = currentSearch;
        
        $("#no-results").html("");
        
	    	if (!SearchConfig.preloadParaValues) {
	    		var currentParaFilters = $paraFilterForm.getAllParaFilters();  
	    		if (_.indexOf(_.values(currentParaFilters), activeField) === -1) {
	    			currentParaFilters[activeField] = activeField;
	    		}
	    		var activeParaFilters = [activeField];
	    		// 2017-02-21 fix to search only the current active para field on the bar chart
	    		//submitData = _.extend({fieldNames: _.values(currentParaFilters)}, currentSearch);
	    		submitData = _.extend({fieldNames: activeParaFilters}, currentSearch);
	    	} 
        
        previousXHR = $.ajax({
            url: RESULT_FIELDS_URL,
            type : 'POST',
            contentType : 'application/json',
            dataType : 'json',
            data: JSON.stringify(submitData),
            success: function(response){
                
                if (response.success) {
                	lastFetchError = false;
                    var json = response.result;
                    
                    var totalResultsLabel = '  Total ' + SearchSettings.getResultViewText() + ': ' + SearchEvents.getFormattedResult(lastTotalResults);
    
                    $totalResults.text(totalResultsLabel);
                    
                    if (lastTotalResults === 0) {
                    	$("#no-results").html('<span style="color: red;">No results found. Try a different search</span>');
                    }

     								var currentParaFilters = $paraFilterForm.getAllParaFilters(); 
     								var indicatorInput = '<input data-toggle="tooltip" title="Field is used in the query" class="filter-indicator pull-right" type="radio" checked></input>';
     								
     								$('input.filter-indicator').toggleClass('hide', true);
     								
     								_.each(_.keys(currentParaFilters), function(field) {
     									var selector = 'div.field-group span[title="' + field + '"]';
     									$(selector).find('input.filter-indicator').toggleClass('hide', false);
     								});
     								
							      $('div.field-group').each(function() {
							          if ($(this).find('input.filter-indicator:visible').length > 0 || $(this).find('li.active').length > 0) {
							          	$(this).find('.collapse').collapse('show');
							          } else {
							          	$(this).find('.collapse.in').collapse('toggle');
							          }
							      });
							      
							      _.each(json, function(field) {
							      	paraFieldsValue[field.name] = field;	
							      })
                        
                    rerender();
                } else {
                    $totalResults.html(SearchEvents.getSearchErrorMessage(response.error, response.errorDetail));  
                    if(sunburst) {
                  		sunburst.clearAll();
                  	}
                    lastFetchError = true;
                }
            }
        }).always(function(){
            //$loading.hide()
            SearchEvents.toggleResultsLoading(false, $totalResults);
        });
        
    }
    
    SearchEvents.$.on(SearchEvents.PARAMETRICS_LOADED, function(e, obj){
    	var data = obj.data;
			currentSearch = lastSearch = lastTotalResults = activeField = null;
			
      var selectedSearchView = SearchEvents.getSelectedSearchView();
      var viewFilterFields = FilterFields[selectedSearchView];
    	
    	var paraFields = [];
    	
    	_.each(_.values(viewFilterFields), function(field) {
    		if(field.parametric) {
    			paraFields.push(field.name);
    		}
    	}); 
    	
      
      var paraFieldItemTmpl = '<li <%- active %>><span data-toggle="tooltip" title="<%- label %>" class="filter-indicator"><%- displayLabel %><%= selectIndicator %></span></li>';
      //var selectIndicatorTmpl = '<input data-toggle="tooltip" title="Field is used in the query" class="filter-indicator pull-right" type="radio" checked></input>';
      
      
      var fieldValuesData = _.map(paraFields, function(fieldname) {
      	var fieldMeta = viewFilterFields[fieldname];
      	var field = {name: fieldname};
      	
      	var label = fieldMeta.displayName;
      	var defaultSelect = Boolean(viewFilterFields[field.name].filterDefaultSelect);
      	
      	field.parentCategory = fieldMeta.parentCategory;
      	field.parentGroup = fieldMeta.parentGroup;
      	field.displayName = fieldMeta.displayName;
      	field.displayLabel =  (label.length > maxFieldListChars) ? label.substring(0, maxFieldListChars) + "..." : label;
    	if(fieldMeta.fieldOrdinal) {
    		field.fieldOrdinal = fieldMeta.fieldOrdinal;      		
    	}   
      	
      	if (field.name === activeField || (!activeField && defaultSelect)) {
      		field.activeClass = 'class=active';
      		activeField = field.name;
      	} else {
      		field.activeClass = '';
      	}
      	field.selectIndicator = true; //!_.isUndefined(currentParaFilters[label]);
      	
      	return field;
      }); 
      
      
      var mappedHeaderFields = Util.mapHeader(fieldValuesData, true); 
      if (!activeField && mappedHeaderFields.length) {
          mappedHeaderFields[0].values[0].activeClass = 'class=active'; 
          activeField = mappedHeaderFields[0].values[0].name;
      }
      var fieldListTpl = _.template($.resGet(FILTERS_LIST_TEMPLATE));
      var fieldList = fieldListTpl({fields: mappedHeaderFields});
      
      $filtersEl.empty().append(fieldList);   

      

      $('input.filter-indicator').on('click', function() {
          return false;
      });
    	   
		});
	
    SearchEvents.$.on(SearchEvents.SEARCH_REQUEST_SENT, function (e, data) {
        previousXHR && previousXHR.abort();
        previousXHR = null;
        
        if (currentSearch && currentSearch.searchView !== data.search.searchView) {
            activeField = null;
        }
        if(data.retrieveResultDocs) {
        	clearChart();
        }        
        
    });

    SearchEvents.$.on(SearchEvents.RESULTS_PROCESSING, function(e, results, totalResults, data) {
        lastTotalResults = _.reduce(totalResults, function(total, val){
            return total + val;
        }, 0);

        currentSearch = data;

        previousXHR && previousXHR.abort();

        if ($filterchart.is(':visible')) {        	
            doSearch();
        }

    });

    if ($filterchart.is(':visible')) {
        onShown();
    }
    
   
   /* var versionLoadingImg = $( "#buildVersionImg" ).val();
	$('head').append('<style>.view-results-label.loading:before{content: url(../img_logos/'+ versionLoadingImg + ')}</style>'); 
	$('head').append('<style>.loadinggif-importedValues{background: url(../img_logos/'+ versionLoadingImg + ')}</style>'); */
    
    
    
    
    });
