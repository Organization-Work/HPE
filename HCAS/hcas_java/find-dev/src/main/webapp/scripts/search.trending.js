jQuery(function($) {

	var tempID  =  this.versionObj;
	var tempID =  localStorage.getItem("storageId");
	
	// Resources and Constants
	var maxFieldListChars = 30;
	// TODO: Constants -- Make these per view from Config
	// var demo_fields=["PROV_NUM","CRISE_CAT","DIS_ENROL_CD","CAP_RATE_IND","RES_CITY","RES_STATE","RES_COUNTY"];
	// var demo_view="834L";
		
	var DEFAULT_TITLE = 'Trending'
	var DEFAULT_FIELD_TITLE_PREFIX = 'by'
		
	var DEFAULT_YAXIS_LABEL = 'Count'
	var DEFAULT_XAXIS_LABEL = 'Date'
		
	var DEFAULT_DATE_PERIOD = 'day';
	var DEFAULT_SORT_TYPE = 'Value';
	var DEFAULT_SORT_ORDER = 'Descending';
	
	var DEFAULT_VALID_START_DATE= '01/01/1900'; 
	var DEFAULT_VALID_END_DATE='12/31/2039';
	var DEFAULT_MINY=0;
	var DEFAULT_MAXY=1000000;
	
	//var DEFAULT_VALID_END_DATE='1437004799';//'1451624400';  // 1-2-2015
	
	var DEFAULT_baselineDate='1430438400'; // '1430452800';	  // 9-1-2014
	var DEFAULT_asOfDate='1436918400'; // '1451624400';		  // 11-5-2014
	var selectedInterval = DEFAULT_DATE_PERIOD;
	var tactiveField='MC/MC834/MC_PLAN';
	var startDate=DEFAULT_VALID_START_DATE;
	var endDate=DEFAULT_VALID_END_DATE;
	
	var spanAsofDateField='MC/MC834/AS_OF_DATE';
	var spanStartDateField='MC/MC834/HMO_START';
	var spanEndDateField='MC/MC834/HMO_END';	
	var spanAsofDateFormat='%m/%d/%Y';
	var selectedSortOrder=DEFAULT_SORT_ORDER;
	var selectedSortType=DEFAULT_SORT_TYPE;
	
	dataToRender={tdata:{dataLayers:{}}}; // Empty Data to render
	
	
	// Parsing functions for dates
	var parseDate = d3.time.format("%H:%M:%S %m/%d/%Y").parse;
	var parseEpoch = d3.time.format("%H:%M:%S %m/%d/%Y").parse;
    var countformat = d3.format("0,000");
	var countformat2 = d3.format("0,000");
    var dateformat = d3.time.format("%m-%d-%Y");

	// -----------------------------------------------------------------------------

	
	// Global Variables
	var focus_title=DEFAULT_TITLE;
	var field_prefix=DEFAULT_FIELD_TITLE_PREFIX;
	var focus_chart_type='';
	var date_field_selected={};
	var date_field_selected_label={};
	var graph_type='disabled';
	var layers;
	var lg;
	var context;
	var focus;
	var ldiv;
	var focus_chart;
	var valgroup;
	var drawgroup;
	var drawgroupA;
	var drawgroupR;
	var brush;
	var x;
	var y;
	var z;
	var f1;
	var xAxis;
	var yAxis;
	var showValues={};
	var initShowVals={};
	var sortType={};
	var sortOrder={};
	var zoomMode="Zoom Mode";
	var brushg;
		
	var showTotals=true;
	 var time_svg;
	var stateChange=false
	var currentSearch, lastSearch, TpreviousXHR, paraFieldsValue;
	var trending, lastTotalResults;
	var extent; 
	var dataToRender = false;
	var numrect = 1;
	var time_svg=null;
	var y2_max=100;
		
	
	var tcurrentSearch, tlastSearch, TpreviousXHR, tparaFieldsValue;
	var trending, tlastTotalResults, tactiveField;

	var TR_FILTERS_LIST_TEMPLATE = '../templates/search/trending.fieldsList'+tempID+'.template';
	// var TR_FILTERS_LIST_TEMPLATE = '../templates/search/trending.fieldsList.template';
	var TR_SETTING_TEMPLATE = '../templates/search/trending.settings'+tempID+'.template';
	var TR_LEGEND_TEMPLATE = '../templates/search/trending.legend'+tempID+'.template';
	

	var color = d3.scale.ordinal().range([
	                                      "#5ab7e0",   // acqua
	                                      "#acdbef",   // light acqua
	                                      "#42ccbe",   // orange
	                                      "#a0e5de",   // tan
	                                      "#ea9047",	// blue
	                                      "#f6c498",	// light blue
	                                      "#9bda6a",	// green
	                                      "#cdecb4",	// light green
	                                      "#7a9cfc",	// blue-purple
	                                      "#b8c7fe",	// light blue-purple
	                                      "#ffd428",	// yellow
	                                      "#ffe993",	// light-yellow
	                                      "#fa726c",	// red
	                                      "#fcb8b5",	// light-red
	                                      "#84a2a5",	// grey-blue
	                                      "#c1d0d2",	// light grey-blue
	                                      "#d392f4",	// purple
	                                      "#e9c8f9",	// light purple
	                                      "#d9ac59",	// golden
	                                      "#ecd5ac", 	// light golden
	                                      "#a09bfc",	// purple-blue
	                                      "#cfcdfd",	// light purple-blue
	                                      "#b7c11e",	// olive
	                                      "#dbe08e",	// light olive
	                                      "#7a9cfc",	// sky-blue
	                                      "#b8c7fe",	// light sky-blue
	                                      "#cc6a7d",	// mauve
	                                      "#e5b4be",	// light-mauve
	                                      "#47a1ea",	// bright-blue
	                                      "#a3d0f4",	// light bright-blue
	                                      "#e0835a",	// dark orange
	                                      "#efc1ac",	// light dark orange
	                                      "#a96ada",	// bright purple
	                                      "#d4b4ec",	// light bright purple
	                                      "#a58784",	// brown-grey
	                                      "#d2c3c1",	// light-brown grey
	                                      "#719cbc",	// dark blue grey
	                                      "#b8cddd",	// light dark blue grey
	                                      "#81b273",	// dark green
	                                      "#c0d8b9"		// light dark green
	                                  ]);

	var colorAdd = d3.scale.ordinal().range([
	                                      "#5ab7e0",   // acqua
	                                      "#acdbef",   // light acqua
	                                      "#42ccbe",   // blue
	                                      "#a0e5de",   // ight blue
	                                      "#9bda6a",	// green
	                                      "#cdecb4",	// light green
	                                      "#7a9cfc",	// blue-purple
	                                      "#b8c7fe",	// light blue-purple
	                                      "#84a2a5",	// grey-blue
	                                      "#c1d0d2",	// light grey-blue
	                                      "#a09bfc",	// purple-blue
	                                      "#cfcdfd",	// light purple-blue
	                                      "#7a9cfc",	// sky-blue
	                                      "#b8c7fe",	// light sky-blue
	                                      "#47a1ea",	// bright-blue
	                                      "#a3d0f4",	// light bright-blue
	                                      "#719cbc",	// dark blue grey
	                                      "#b8cddd",	// light dark blue grey
	                                      "#81b273",	// dark green
	                                      "#c0d8b9"		// light dark green
	                                  ]);
	var colorRemove = d3.scale.ordinal().range([
	  	                                  "#ea9047",	// orange
		                                  "#f6c498",	// tag
	                                      "#ffd428",	// yellow
	                                      "#ffe993",	// light-yellow
	                                      "#fa726c",	// red
	                                      "#fcb8b5",	// light-red
	                                      "#d392f4",	// purple
	                                      "#e9c8f9",	// light purple
	                                      "#d9ac59",	// golden
	                                      "#ecd5ac", 	// light golden
	                                      "#b7c11e",	// olive
	                                      "#dbe08e",	// light olive
	                                      "#cc6a7d",	// mauve
	                                      "#e5b4be",	// light-mauve
	                                      "#e0835a",	// dark orange
	                                      "#efc1ac",	// light dark orange
	                                      "#a96ada",	// bright purple
	                                      "#d4b4ec",	// light bright purple
	                                      "#a58784",	// brown-grey
	                                      "#d2c3c1",	// light-brown grey
	                                  ]);

	

	var maxVals =$("#trending-max-rows").val() ? $("#trending-max-rows").val():20;
	var TRENDING_TOTALS_URL = 'ajax/search/getTrendingTotals.json';
	var TRENDING_DETAILS_URL = 'ajax/search/getTrendingDetails.json';

	var maxFields =$("#trending-max-rows").val() ? $("#trending-max-rows").val():20;
	var RESULT_FIELDS_URL = 'ajax/search/getFilteredResultFields.json?maxResults=' + maxFields + '&field=' + SearchConfig.sunburstSingleQueryFieldname;
 
	var $trending = $('#trending').empty();
	var $showPanel = $('#showPanel');

  
	// Add Elements for Structure of Page on UI Load
	// ---------------------------------------------------------------------------------------------
	var $trending = $('#trending');
	//Settings Bar
	var $noResultEl = $('<div id="no-results"></div>').appendTo($trending);
	
	
	
	var $tsettingsEl = $('<div id="trending-settings" class="btn-toolbar"></div>').appendTo($trending);
	var settingsTemplate = $.resGet(TR_SETTING_TEMPLATE);	
	var $settings = $(settingsTemplate)
	.appendTo($tsettingsEl)
	.on('click','.trending-refresh', function(evt) {
			evt.preventDefault();
			doSearch();
		})
	.on('click', '.trending-settings', function(evt) {
		// prevent the popup from closing when you click on the input buttons
		evt.stopPropagation();
	});
	var $zoomDiv = $('<div id="zoom" class="btn-zoom"></div>').appendTo($trending);

	
	var $trendingchartEl = $('<div id="trending-chart"></div>').appendTo($trending);
	
	// Legend Control
	var $legendAreaEl = $('<div id="trending-legendarea"></div>').appendTo($trending);
	
	var graphType = $('#trending-charttype').attr("value")
	

	var resultCount = $("#trending-max-rows").val()?$("#trending-max-rows").val():20;
 
	// Variables set from current UI Settings 
	var maxFields =$("#histogram-max-rows").val() ? $("#histogram-max-rows").val():20;

	
	// Totals Display
	var $ttotalResults = $('<div id="trending-totals" class="view-results-label"></div>').appendTo($tsettingsEl);

	//Context Control
	var $contextEl = $('<div id="trending-context"></div>').appendTo($trendingchartEl);
	var $tooltip =  $contextEl.append('<div class="tooltip"></div>');

	// Focus Control
	var $focusEl = $('<div id="trending-focus"></div>').appendTo($trendingchartEl);
	
	
	// Loading Control
	var $loading = $('<div class="visualizer-loading"></div>').appendTo($trendingchartEl).hide();
 	

	// Filter Values
	var $paraFilterForm = $('#parametricForm').data('filterForm');

 
	var $tfiltersEl = $('<div id="trending-filters"></div>').appendTo($trendingchartEl).on('click', '.tfield-group-list li', function(){
		var stateChange = false;
		$('.tfield-group-list li.active').removeClass('active');
		var $el = $(this).addClass('active');
		//$el.siblings('li').removeClass('active');
		tactiveField = $el.data('trendingfield');
	 
		 if (!tparaFieldsValue[tactiveField]) {
		 	doSearch();
			 // fetchParaValues(tactiveField);
		 } else {
		 	rerender();
		 }
	});


 
		
	  
	
	var clearChart = function() {
		$focusEl.empty();
		$legendAreaEl.empty(); 
		
	};
 
	// Event Handlers
	// --------------------------------------------------------------
	
	 if ($trendingchartEl.is(':visible')) {
	     onShown();
	 }
	 
	
	// Trending Tab is displayed
	// -------------------------
	$('[href=#trending]').on('shown', function() {
		
		 doSearch();
		 
		 /*
		 if (tlastSearch==null || (!_.isEqual(lastSearch, tcurrentSearch))) {
	         doSearch();
	     }
	     */
	     return false;
	 });
	// -------------------------

	// SearchView is Changed
	// -------------------------
	$('.search-controls').on(SearchEvents.SEARCH_VIEW_CHANGED, function() {
	 
		
		// check if trending is enabled for this view, otherwise hide and return;
		
		// Set the tactiveField from the config
		
		
		
		
		tactiveField=null
		$focusEl.empty();
	 	$legendAreaEl.empty();   
		if ($trendingchartEl.is(':visible')) {
			doSearch();
		}
	});
	// -------------------------
 
 
	// Refresh button is clicked
	// -------------------------
	$(".trending-refresh").click(function(){
	   doSearch();
	});
	// -------------------------
	
	// Parametrics for view are loaded
	// -------------------------------
	SearchEvents.$.on(
		SearchEvents.PARAMETRICS_LOADED,
		function(e, obj) {
			var data = obj.data;
			tcurrentSearch = tlastSearch = tlastTotalResults = tactiveField = null;

			var selectedSearchView = SearchEvents
					.getSelectedSearchView();
			var viewFilterFields = FilterFields[selectedSearchView];

			var tparaFields = [];

			_.each(_.values(viewFilterFields), function(field) {
				if (field.parametric) {
					tparaFields.push(field.name);
				}
			});

			var paraFieldItemTmpl = '<li <%- active %>><span data-toggle="tooltip" title="<%- label %>" class="tfilter-indicator"><%- displayLabel %><%= selectIndicator %></span></li>';
			var selectIndicatorTmpl = '<input data-toggle="tooltip" title="Field is used in the query" class="tfilter-indicator pull-right" type="radio" checked></input>';
			
/*			if (!$trendingchartEl.is(':visible')) {
				return;
			}
*/
			var tfieldValuesData = _
					.map(
							tparaFields,
							function(tfieldname) {
								var tfieldMeta = viewFilterFields[tfieldname];
								var tfield = {
									name : tfieldname
								};

								var tlabel = tfieldMeta.displayName;
								var tdefaultSelect = Boolean(viewFilterFields[tfield.name].filterDefaultSelect);

								tfield.parentCategory = tfieldMeta.parentCategory;
								tfield.parentGroup = tfieldMeta.parentGroup;
								tfield.displayName = tfieldMeta.displayName;
								tfield.displayLabel = (tlabel.length > maxFieldListChars) ? tlabel
										.substring(0,
												maxFieldListChars)
										+ "..."
										: tlabel;

								if (tfield.name === tactiveField
										|| (!tactiveField && tdefaultSelect)) {
									tfield.activeClass = 'class=active';
									tactiveField = tfield.name;
								} else {
									tfield.activeClass = '';
								}
								tfield.selectIndicator = true; // !_.isUndefined(currentParaFilters[label]);

								return tfield;
							});

			var tmappedHeaderFields = Util.mapHeader(
					tfieldValuesData, true);
			if (!tactiveField && tmappedHeaderFields.length) {
				tmappedHeaderFields[0].values[0].activeClass = 'class=active';
				tactiveField = tmappedHeaderFields[0].values[0].name;
			}
			var tfieldListTpl = _.template($
					.resGet(TR_FILTERS_LIST_TEMPLATE));
			var tfieldList = tfieldListTpl({
				fields : tmappedHeaderFields
			});

			$tfiltersEl.empty().append(tfieldList);

			$('input.tfilter-indicator').on('click', function() {
				return false;
			});
	});
	

	// Search Request Sent, Waiting for results
	// ----------------------------------------
	SearchEvents.$.on(SearchEvents.SEARCH_REQUEST_SENT, function(e, data) {
		TpreviousXHR && TpreviousXHR.abort();
		TpreviousXHR = null;

		if (tcurrentSearch
				&& tcurrentSearch.searchView !== data.search.searchView) {
			tactiveField = null;
		}

		clearChart();
	});

	// Search Request Processing
	// -------------------------
	SearchEvents.$.on(SearchEvents.RESULTS_PROCESSING, function(e, results, ttotalResults, data) {
	     tlastTotalResults = _.reduce(ttotalResults, function(total, val){
	         return total + val;
	     }, 0);
	
	     tcurrentSearch = data;
	  
	     TpreviousXHR && TpreviousXHR.abort();
	
	     if ($trending!=null && $trending.hasClass("active")) {
	    	doSearch();
	     }
	 });
	
	
	// Mouse Move2
	// -------------------------
	
	function mousemove2() {
        var m=d3.mouse(this);
        	d=d3.select(this).datum();
        	txt=f2.select("text");
            rect=f2.select("rect");
            dd=new Date(+d.date);
	        	dp=d3.select(this.parentNode).datum();
	        	dvar=dp.varName;
            nlabel=dp.label+': '+countformat2(Math.floor(d.total_count)) + '   ('+d.date+')'
            txt.text(nlabel);
            
            rbox=this.getBBox();
            tbox=txt.node().getBBox();
            
            // dx=rbox.x+rbox.width/2
            // dy=rbox.y
            dx=m[0];
            dy=m[1];            
             
            
            tx=0
            if (dx+tbox.width>width2) {
            	tx=-tbox.width
            }
            ty=-tbox.height
            ty2=ty-10
            f2.attr("transform", "translate(" + dx + "," + dy + ")");
            txt.attr("transform", "translate(" + tx + "," + ty + ")");
            rect.attr("transform", "translate(" + tx + "," + ty2 + ")")           	
    		.attr("width", tbox.width+20)
    		.attr("height", tbox.height+6)
    		.attr("fill", "rgba(255,255,255,.5)")
	        txt.attr("transform", "translate(" + tx + "," + ty + ")");
           
                      	   
   }
	// Mouse Move4
	// -------------------------
	
	function mousemove4() {
        var m=d3.mouse(this);
        	d=d3.select(this).datum();
        	txt=f2.select("text");
            rect=f2.select("rect");
            dd=new Date(+d.date);
	        	dp=d3.select(this.parentNode).datum();
	        	dvar=dp.varName;
            nlabel=dp.label+': '+countformat2(Math.floor(d.add_count)) + '   ('+d.date+')'
            txt.text(nlabel);
            
            rbox=this.getBBox();
            tbox=txt.node().getBBox();
            
            dx=m[0]
            dy=m[1]
            // dx=rbox.x+rbox.width/2
            // dy=rbox.y
            tx=0
            if (dx+tbox.width>width2) {
            	tx=-tbox.width
            }
            ty=-tbox.height
            ty2=ty-10
            f2.attr("transform", "translate(" + dx + "," + dy + ")");
            txt.attr("transform", "translate(" + tx + "," + ty + ")");
            rect.attr("transform", "translate(" + tx + "," + ty2 + ")")           	
    		.attr("width", tbox.width+20)
    		.attr("height", tbox.height+6)
    		.attr("fill", "rgba(255,255,255,.5)")
	        txt.attr("transform", "translate(" + tx + "," + ty + ")");
           
                      	   
   }
	// Mouse Move2
	// -------------------------
	
	function mousemove5() {
        var m=d3.mouse(this);
        	d=d3.select(this).datum();
        	txt=f2.select("text");
            rect=f2.select("rect");
            dd=new Date(+d.date);
	        	dp=d3.select(this.parentNode).datum();
	        	dvar=dp.varName;
            nlabel=dp.label+': '+countformat2(Math.floor(d.remove_count)) + '   ('+d.date+')'
            txt.text(nlabel);
            
            rbox=this.getBBox();
            tbox=txt.node().getBBox();
            
            dx=m[0]
            dy=m[1]
            // dx=rbox.x+rbox.width/2
            // dy=rbox.y
            tx=0
            if (dx+tbox.width>width2) {
            	tx=-tbox.width
            }
            ty=-tbox.height
            ty2=ty-10
            f2.attr("transform", "translate(" + dx + "," + dy + ")");
            txt.attr("transform", "translate(" + tx + "," + ty + ")");
            rect.attr("transform", "translate(" + tx + "," + ty2 + ")")           	
    		.attr("width", tbox.width+20)
    		.attr("height", tbox.height+6)
    		.attr("fill", "rgba(255,255,255,.5)")
	        txt.attr("transform", "translate(" + tx + "," + ty + ")");
           
                      	   
   }


	// --------------------------------------------------------------
 
	
	// Helper Functions
	// --------------------------------------------------------------

	// fetchParaValues - Get the parametric values for the field and re-render
	// -----------------------------------------------------------------------
 
	// The following code was contained in the callback function.
	function dateAdd(date, interval, units) {
		  var ret = new Date(date); //don't change original date
		  switch(interval.toLowerCase()) {
		    case 'year'   :  ret.setFullYear(ret.getFullYear() + units);  break;
		    case 'quarter':  ret.setMonth(ret.getMonth() + 3*units);  break;
		    case 'month'  :  ret.setMonth(ret.getMonth() + units);  break;
		    case 'week'   :  ret.setDate(ret.getDate() + 7*units);  break;
		    case 'day'    :  ret.setDate(ret.getDate() + units);  break;
		    case 'hour'   :  ret.setTime(ret.getTime() + units*3600000);  break;
		    case 'minute' :  ret.setTime(ret.getTime() + units*60000);  break;
		    case 'second' :  ret.setTime(ret.getTime() + units*1000);  break;
		    default       :  ret = date;  break;
		  }
		  return ret;
	}		

	function jsDateToEpoch(d){
        // d = javascript date obj
        // returns epoch timestamp
        return (d.getTime()-d.getMilliseconds());
	}
	// Convert a date into a printable date
	function getDateString(d) {
		// padding function
	    var s = function(a,b){return(1e15+a+"").slice(-b)};
		
	    // default date parameter
	    if (typeof d == 'undefined'){
	        d = new Date();
	    };

/*	    return s(d.getHours(),2) + ':' +
        s(d.getMinutes(),2) + ':' +
        s(d.getSeconds(),2) + ' ' +
        s(d.getMonth()+1,2) + '-' +
        s(d.getDate(),2) + '-' +
        d.getFullYear(); 
*/
	    
	    return s(d.getMonth()+1,2) + '-' +
        s(d.getDate(),2) + '-' +
        d.getFullYear();	    
	}

	// Convert Epoch Seconds to Java Date
	function getDateFromEpoch(ed) {
	     d = new Date(ed*1000);
	     return d;
	}
	
	// Convert Epoch MilliSeconds to Java Date
	function getDateFromEpochMS(ed) {
	     d = new Date(ed);
	     return d;
	}

	function getSegments(startdate,enddate,interval) {
		 var diff=enddate.getTime() - startdate.getTime(); 
		 var bw=10;
		 switch(interval.toLowerCase()) {
		    case 'year'   :  bw=(diff/(365*24*60*60*1000));  break;
		    case 'quarter':  bw=(diff/(90*24*60*60*1000));  break;
		    case 'month'  :  bw=(diff/(31*24*60*60*1000));  break;
		    case 'week'   :  bw=(diff/(7*24*60*60*1000));  break;
		    case 'day'    :  bw=(diff/(24*60*60*1000));   break;
		    case 'hour'   :  bw=(diff/(60*60*1000)); break;
		    case 'minute' :  bw=(diff/(60*1000)); break;
		    case 'second' :  bw=(diff/(1000)); break;break;
		    default       :  bw = 10;  break;
		  }	
		 return bw;
	}
	
	function fetchParaValues(field) {
		clearChart();
		SearchEvents.toggleResultsLoading(true, $ttotalResults);
		var submitData = _.extend({
			fieldNames : [ field ]
		}, tcurrentSearch);

		$.ajax({
			url : RESULT_FIELDS_URL,
			type : 'POST',
			contentType : 'application/json',
			dataType : 'json',
			data : JSON.stringify(submitData),
			success : function(response) {

				if (response.success) {
					_.each(response.result, function(field) {
						tparaFieldsValue[field.name] = field;
					});

					rerender();

				} else {
					//$ttotalResults.html(SearchEvents.getSearchErrorMessage(response.error, response.errorDetail));
				}
			}
		}).always(function() {
			//$loading.hide()
			SearchEvents.toggleResultsLoading(false, $ttotalResults);
		});

	}

	// rerender - Redraw graph with current settings & data
	// -----------------------------------------------------
	function rerender() {
/*		if (_.isEmpty(tparaFieldsValue)) {
			SearchEvents.toggleResultsLoading(false, $ttotalResults);
			return;
		}
*/
		TtoDraw = {
			data : null,
			name : 'Total'
					+ (tlastTotalResults ? ': ' + tlastTotalResults : ''),
			totalCount : tlastTotalResults
		};

		// $trendingchartEl.css('min-height', $tfiltersEl.position().top
		//		+ $tfiltersEl.outerHeight(true));

		// Render Graph

		// renderTotals();
		
		renderIfVisible();

		/*
		if (tlastTotalResults > 0) {
		 
		 
		      if (_.isEmpty(toDraw.data)) {
		      	$("#no-results").html('<span style="color: red;">No data to render.</span>');
		      } else {
		      	$("#no-results").html('');
		      }
		}
		*/
		// SearchEvents.toggleResultsLoading(false, $ttotalResults);

	}

	// doSearch - Rerun search and re-draw graph with new data
	// -----------------------------------------------------
	function doSearch() {
		tlastSearch = tcurrentSearch;
		tparaFieldsValue = {};

		var submitData = tcurrentSearch;
		
		$("#no-results").html("");
		dataToRender=null;
		SearchEvents.toggleResultsLoading(true, $ttotalResults);
		
		TpreviousXHR = $
				.ajax(
						{
							url : TRENDING_TOTALS_URL + '?' + $.param({
								field:  tactiveField,
								graphType: graphType,
								dateField1: spanStartDateField,
								dateField2:	spanEndDateField,
								dateStart: startDate,
								dateEnd:   endDate,
								sortType: selectedSortType,
								sortOrder: selectedSortOrder,
							}, true),
							type : 'POST',
							contentType : 'application/json',
							dataType : 'json',
							field: tactiveField,
							graphType: graphType,
							dateField1: spanStartDateField,
							dateField2:	spanEndDateField,
							dateStart: startDate,
							dateEnd:   endDate,
							sortType: selectedSortType,
							sortOrder: selectedSortOrder,
							data : JSON.stringify(submitData),
							success : function(response) {

								if (response.tdata) {
									dataToRender = response;	
									rerender();
								} else {
									$ttotalResults.html(SearchEvents
											.getSearchErrorMessage(
													response.error,
													response.errorDetail));
								}
							}
						}).always(function() {
							// rerender();
							$loading.hide()
					SearchEvents.toggleResultsLoading(false, $ttotalResults);
				});

	}


	


	// renderIfVisible - render graph based on data
	// ---------------------------------------------
	function renderIfVisible() {
		// Render only if data is available
		if (dataToRender != null && dataToRender.tdata != null
				&& $trending.is(':visible')) {

			tdata=dataToRender.tdata;
			recordcnt=null;

			// Get Graph Type
			if (tdata.graphType!=null) {
				graph_type = tdata.graphType;			
			}
			
			
			// Set Title
			focus_title = DEFAULT_TITLE;			
			if (tdata.title!=null) {
				focus_title = tdata.title;			
			}

			// Set the Active Field
			if (tdata.field) {
				tactiveField=tdata.field;
			}

			// Set Y-Axis Label
			y_axis_label=DEFAULT_YAXIS_LABEL;			
			if (tdata.yaxisLabel) {
				y_axis_label=tdata.yaxisLabel;
			}
			
			// Set X-Axis Label
			x_axis_label=DEFAULT_XAXIS_LABEL;
			if (tdata.xaxisLabel) {
				x_axis_label=tdata.xaxisLabel;
			}
			
			
			// Set Valid Date Range 
			valid_start_date_str=DEFAULT_VALID_START_DATE;
			if (tdata.vaildStartDate!=null) {
				valid_start_date_str=tdata.vaildStartDate;
			}			
			valid_end_date_str=DEFAULT_VALID_END_DATE;
			if (tdata.vaildEndDate!=null) {
				valid_end_date_str=tdata.vaildEndDate;
			}
			
			// Set Actual Data Date Range
			start_date_str=valid_start_date_str;
			end_date_str=valid_end_date_str;
			if (tdata.startDate!=null) {
				start_date_str=tdata.startDate;
			}
			if (tdata.endDate!=null) {
				end_date_str=tdata.endDate;
			}
			date2_min=new Date(start_date_str);
			date2_max=new Date(end_date_str);		
			date2_max=dateAdd(date2_max,'day',1);
		
			// Set Min/Max Y Range
			y2_min=DEFAULT_MINY;
			y2_max=DEFAULT_MAXY;
			if (tdata.minY!=null) {
				y2_min=tdata.minY;
			}
			if (tdata.maxY!=null) {
				y2_max=tdata.maxY;
			}
			
			
			// Turn on animation
			SearchEvents.toggleResultsLoading(false, $ttotalResults);

			$focusEl.empty();
			$legendAreaEl.empty();     

			// Calcluate Margin
			var margin = {
				top : 150,
				right : 50,
				bottom : 250,
				left : 120
			}

			var width = $showPanel.width() - margin.left - margin.right
			if (width < 400)
				width = 400
			var offset = $contextEl.height()
			var height = $showPanel.height() - margin.top - margin.bottom
					- offset;
			if (height < 600) {
				height = 600;
			}

			var margin2 = {
				top : 150,
				right : 50,
				bottom : 100,
				left : 120
			};
			width2 = width - margin2.left - margin2.right;
			height2 = height - margin2.top - margin2.bottom;

			focus_div = d3.select('#trending-focus');			
		  
		    

			// Setup Scales
			x = d3.time.scale().range([ 0, width2 ]);
			x2= d3.time.scale().range([ 0, width2 ]);
			y = d3.scale.linear().range([ 0, height2 ]);
			y2= d3.time.scale().range([ 0, height2 ]);
			z = d3.scale.ordinal().range([ "darkblue", "blue", "lightblue" ]);

			// Axis definition
			xAxis = d3.svg.axis().scale(x).orient("bottom");
			yAxis = d3.svg.axis().scale(y).orient("left");


			focus_svg = focus_div.append('svg').attr("id", "focus_svg").attr(
					"width", width).attr("height", height)
					
			// insert line breaks for long words
			var insertLinebreaks = function(d) {
				var el = d3.select(this);
				var words = d.split(' ');
				el.text('');

				for (var i = 0; i < words.length; i++) {
					var tspan = el.append('tspan').text(words[i]);
					if (i > 0)
						tspan.attr('x', 0).attr('dy', '15');
				}
			};

			
			// Get tag value detail data
			fieldToRender = tdata.field;

			layers = dataToRender.tdata.dataLayers;

			// Calculate date span for detail view
			date_min = d3.min(layers, function(l) {
				return d3.min(l.vals, function(d) {
					return Date.parse(d.date);
				})
			});
			if (date_min == null) {
				date_min = date2_min;
			} else {
				date_min=getDateFromEpochMS(date_min)
			}
			min_date_str = getDateString(date_min);

			date_max = d3.max(layers, function(l) {
				return d3.max(l.vals, function(d) {
					return dateAdd(Date.parse(d.date),
							selectedInterval, 1);
				})
			});
			if (date_max == null) {
				date_max = date2_max;
			}
			max_date_str = getDateString(date_max);

			barwidth = width2
					/ getSegments(date_min, date_max, selectedInterval);

			// Calculate Stacked Graph Values
			var stack = d3.layout.stack().offset("zero").values(function(d) {
				return d.vals;
			}).x(function(d) {
				return (new Date(d.date));
			}).y(function(d) {
				return d.total_count;
			})

			// Calculate Stacked Graph Values
			var stack_add = d3.layout.stack().offset("zero").values(function(d) {
				return d.vals;
			}).x(function(d) {
				return (new Date(d.date));
			}).y(function(d) {
				return d.add_count;
			})
			// Calculate Stacked Graph Values
			var stack_rem = d3.layout.stack().offset("zero").values(function(d) {
				return d.vals;
			}).x(function(d) {
				return (new Date(d.date));
			}).y(function(d) {
				return d.remove_count;
			})

			// Calculate Layers to draw
			var drawlayers = [];

			if (!initShowVals || !(tactiveField in initShowVals) || (initShowVals[tactiveField])) {
				// First time, set ShowVals hash based on data returned
				for (j = 0; j < layers.length; j++) {
					if (!(layers[j].varName in showValues)) {
						showValues[layers[j].varName] = {}
					}
					if ((layers[j].vals != null) && (layers[j].vals.length > 0)) {
						drawlayers.push(layers[j]);
						showValues[layers[j].varName][layers[j].label] = "true";
					} else {
						showValues[layers[j].varName][layers[j].label] = "false";
					}
					sortType[tactiveField] = DEFAULT_SORT_TYPE;
					sortOrder[tactiveField] = DEFAULT_SORT_ORDER;
				}
				initShowVals[tactiveField] = true;
			} else {
				// After that, use ShowVals hash to decide what data to draw 
				for (j = 0; j < layers.length; j++) {
					if ((layers[j].vals != null)
							&& (layers[j].vals.length > 0)
							&& (layers[j].varName in showValues)
							&& ((layers[j].label in showValues[layers[j].varName]) && showValues[layers[j].varName][layers[j].label] == "true")) {
						drawlayers.push(layers[j]);
					}
				}
			}

			var st = null;

			graphType = $('#trending-charttype').attr("value")

			focus = focus_svg.append("g").attr("class", "focus").attr(
					"transform",
					"translate(" + margin.left + "," + margin.top + ")");
			
			
			focus_chart = focus.append("g").attr("class", "focus_chart")
				
			focus_chart.append("defs").append("clipPath")
			    .attr("id", "clip")
			    .append("rect")
			    .attr("id","zoom-region")
			    .attr("width", width2)
			    .attr("height", height2);
		
			
			
			
			
			
			// Calculate Y range for Graph
			y_max = y2_max;
			y_min = 0
			if (graphType == 'Bar Chart') {
				if (drawlayers.length != 0) {
					st = stack(drawlayers);

					y_max = d3.max(st[st.length - 1].vals, function(d) {
						return d.y0 + d.y;
					});
					drawgroup = focus_chart.selectAll("g.drawgroup").data(
							drawlayers).enter().append("g").attr("class",
							"drawgroup").style("fill", function(d, i) {
						return color(d.order);
					}).style(
							"stroke",
							function(d, i) {
								if (graphType == 'Bar Chart') {
									return (barwidth > 4) ? d3.rgb(
											color(d.order)).darker()
											: color(d.order);
								} else if (graphType == 'Line Chart') {
									return color(d.order);
								}
								return 'steel-blue';
							}).attr("data-label", function(d) {
						return d.label;
					})
				}
			} else if (graphType == 'Line Chart') {
				y_max = d3.max(drawlayers, function(l) {
					return d3.max(l.vals, function(d) {
						return d.total_count
					})
				});
				drawgroup = focus_chart.selectAll("g.drawgroup").data(
						drawlayers).enter().append("g").attr("class",
						"drawgroup").style("fill", function(d, i) {
					return color(d.order);
				}).style(
						"stroke",
						function(d, i) {
							if (graphType == 'Bar Chart') {
								return (barwidth > 4) ? d3.rgb(color(d.order))
										.darker() : color(d.order);
							} else if (graphType == 'Line Chart') {
								return color(d.order);
							}
							return 'steel-blue';
						}).attr("data-label", function(d) {
					return d.label;
				})
			} else if (graphType == 'Daily Change') {
				y_min = 0;
				y_max = 0;
				if (drawlayers.length != 0) {
				//	var addlayers = jQuery.extend(true, {}, drawlayers);
				//	var remlayers = jQuery.extend(true, {}, drawlayers);
					
					// var addlayers=clone(drawlayers);
					// var remlayers=clone(drawlayers);
					var addlayers = (JSON.parse(JSON.stringify(drawlayers)));
					var remlayers = (JSON.parse(JSON.stringify(drawlayers)));
					
					addst = stack_add(addlayers);

					drawgroupA = focus_chart.selectAll("g.drawgroupA")
							.data(addlayers).enter().append("g").attr(
									"class", "drawgroupA").style("fill",
									function(d, i) {
										return color(d.order);
									}).style(
									"stroke",
									function(d, i) {
										return (barwidth > 4) ? d3.rgb(
												color(d.order)).darker()
												: color(d.order);
									}).attr("data-label", function(d) {
										return d.label;
									}).attr("type","ADD")
					y_max = d3.max(addst, function(l) {
						return d3.max(l.vals, function(d) {
							return d.y + d.y0
						})
					});
					
					
					remst = stack_rem(remlayers);
					drawgroupR = focus_chart.selectAll("g.drawgroupR")
							.data(remlayers).enter().append("g").attr(
									"class", "drawgroupR").style("fill",
									function(d, i) {
										return color(d.order);
									}).style(
									"stroke",
									function(d, i) {
										return (barwidth > 4) ? d3.rgb(
												color(d.order))
												.darker()
												: color(d.order);
									}).attr("data-label", 
									function(d) {
										return d.label;
									}).attr("type","REMOVE")
					y_min = -d3.max(remst, function(l) {
						return d3.max(l.vals, function(d) {
							return d.y + d.y0
						})
					});
				}
			}

			y_max = y_max + y_max * 0.10
			y_min = y_min + y_min * 0.10

			x.domain([ date_min, date_max ]);
			y.domain([ y_max, y_min ]);

			var xaxisEl = focus_chart.append("g").attr("class", "x axis");

			xaxisEl.attr("transform", "translate(0," + height2 + ")").call(xAxis)
					.selectAll("text").style("text-anchor", "end").attr("dx",
							"-.8em").attr("dy", ".15em").attr("transform",
							function(d) {
								return "rotate(-65)"
							});
			
			var selectedSearchView = SearchEvents.getSelectedSearchView();

			var fieldTitle = "";
			if (selectedSearchView in FilterFields) {
				if (tactiveField == null
						|| !(tactiveField in FilterFields[selectedSearchView])) {
					tactiveField = Object.keys(FilterFields[selectedSearchView])[0]
				}
				fieldTitle = FilterFields[selectedSearchView][tactiveField].displayName;
			}
			// var tlabel = focus_title + " " + graphType + " " + field_prefix + " "+ fieldTitle;
			var tlabel = focus_title + " " + y_axis_label + " " + field_prefix + " " + fieldTitle + " " + graphType;
			
			var title_label_xoffset = xaxisEl.node().getBoundingClientRect().width / 2;
			var title = focus_chart.append("text").attr("y", "-25px").attr("x",
					title_label_xoffset).attr("font-size", "18px").style({
				"text-anchor" : "middle",
				"font-weight" : "bold"
			}).text(tlabel);

			var xaxis_label_xoffset = xaxisEl.node().getBoundingClientRect().width / 2;
			var xaxis_label_yoffset = xaxisEl.node().getBoundingClientRect().height;

/*			var min_date_label = focus.append("text").attr("y",
					height2 + xaxis_label_yoffset + 20).attr("x", 0).attr("dy",
					"1px").style({
				"text-anchor" : "start"
			}).html(min_date_str);

			min_date_label.selectAll('g text').each(insertLinebreaks);

			var max_date_label = focus_chart.append("text").attr("y",
					height2 + xaxis_label_yoffset + 20).attr("x", width2).attr(
					"dy", "1px").style({
				"text-anchor" : "end"
			}).html(max_date_str);

			max_date_label.selectAll('g text').each(insertLinebreaks);
*/
			var view = SearchEvents.getSelectedSearchView();

			xTitle = x_axis_label;

			var xaxis_label = focus_chart.append("text").attr("y",
					height2 + xaxis_label_yoffset).attr("x",
					title_label_xoffset).attr("dy", "16px").style({
				"text-anchor" : "middle",
				"font-weight" : "bold"
			}).text(xTitle);

			var yaxis_svg = focus_chart.append("g").attr("class", "y axis");
			yaxis_svg.call(yAxis);

			var yaxis_label_yoffset = -yaxis_svg.node().getBoundingClientRect().width - 20;

			yLabelText = y_axis_label;
			var ylabel = yaxis_svg.append("text").attr("transform",
					"rotate(-90)").attr("y", yaxis_label_yoffset).attr("x", 0)
					.attr("font-size", "12px").style({
						"text-anchor" : "end",
						"font-weight" : "bold"
					}).text(yLabelText);

		    xzoomoffset=width-250;
		    
		    zoom_button = focus_svg.append('text')
		      .attr("y", 50)
		      .attr("x", xzoomoffset)
		      .attr("class", "zoom-button")
		      .style("display","block")
		      .text(zoomMode).on(
						"click",
						function(d, i) {
						//	rerender();
						//	clear_button.style("display","none");
							if (zoomMode=="Filter Mode") {
								$(this).text("Zoom Mode");
								zoomMode="Zoom Mode";
								brushg = focus_chart.append("g")
							      .attr("class", "brush")
							      .call(brush);

							} else {
								$(this).text("Filter Mode");
								zoomMode="Filter Mode";
								if (brushg) {
									focus_chart.selectAll(".brush").remove();
								}
							}
							return;
						});
			
		
			// Draw Grid Lines
			if ($('#trending-grids').is(':checked')) {
				function make_x_axis(x, tx) {
					return d3.svg.axis().scale(x).orient("bottom").ticks(tx)
				}

				function make_y_axis(y, ty) {
					return d3.svg.axis().scale(y).orient("left").ticks(ty)
				}
				var txv = $('#trending-grid-x').val();
				focus_chart.append("g").attr("class", "grid").attr("transform",
						"translate(0," + height2 + ")").call(
						make_x_axis(x, txv).tickSize(-height2, 0, 0)
								.tickFormat(""))

				var tyv = $('#trending-grid-y').val();
				focus_chart.append("g").attr("class", "grid").call(
						make_y_axis(y, tyv).tickSize(-width2, 0, 0).tickFormat(
								""))
	/*
				focus_chart.append("g").attr("class", "grid").call(
						make_x_axis(x, txv).tickSize(-height2, 0, 0)
								.tickFormat(""))

				var tyv = $('#trending-grid-y').val();
				focus_chart.append("g").attr("class", "grid").call(
						make_y_axis(y, tyv).tickSize(-width2, 0, 0).tickFormat(
								""))
	 */
			}

// Legend Control
			

			if (layers.length != 0) {

				// Add a group for each row of the stack.
				valgroup = focus_chart.selectAll("g.valgroup").data(layers)
						.enter().append("g").attr("class", "valgroup").style(
								"fill", function(d, i) {
									return color(i);
								}).style(
								"stroke",
								function(d, i) {
									return (barwidth > 4) ? d3.rgb(color(i))
											.darker() : color(i)
								}).attr("data-label", function(d) {
							return d.label;
						}).attr("order", function(d) {
							return i;
						})

				// sort by pos or by alpha
				slayers = layers.sort(function(a, b) {
					if (sortType[tactiveField] == 'Value') {
						if (sortOrder[tactiveField] == 'Descending') {
							return a.order - b.order
						} else {
							return b.order - a.order
						}
					} else {
						var x = a.label.toLowerCase(), y = b.label.toLowerCase();
						if (sortOrder[tactiveField] == 'Descending') {
							return x < y ? -1 : x > y ? 1 : 0;
						} else {
							return y < x ? -1 : y > x ? 1 : 0;
						}
					}
					return 0;
				});

				var legendy = height
						+ xaxisEl.node().getBoundingClientRect().height
						+ xaxis_label.node().getBoundingClientRect().height
						+ 40 + 20;
				var legendheight = legendy - height

				ldivarea = d3.select('#trending-legendarea')
				// Legend Title Control
				$legendTitleEl = $('<div id="trending-legendtitlediv"><text id="trending-legendtitle"></text></div>').appendTo($legendAreaEl);



				$('<div id="trending-legend"></div>').appendTo($legendAreaEl);
			
				ldiv = d3.select('#trending-legend')

				lsgv = ldiv.append('svg').attr('id', 'trending-legendvallist');

				legenditems = lsgv.selectAll("g.legend-item").data(slayers)
						.enter().append("g").attr("class", "legend-item")

				selectedTotal = 0;
				for ( var k in showValues[tactiveField]) {
					if (showValues[tactiveField][k] == "true") {
						selectedTotal += 1;
					}
				}
				lTitle = fieldTitle + " Values (" + selectedTotal + " )";
				var ltitle_label = d3.select('#trending-legendtitle').text(
						lTitle);

				text_height_max = 20
				text_width_max = 400
				legendsvgwidth = width - margin.right
				if (legendsvgwidth < 500) {
					legendsvgwidth = 500
				}
				num_legend_col=Math.max(Math.floor(legendsvgwidth / text_width_max),0);
				row_offset=xaxisEl.node().getBoundingClientRect().left;
				row_offset=0;
				
				var $el = $('#sortTypeSpan');
				if (sortType[tactiveField] == 'Value') {
					$el.html('Value');
				} else if (sortType[tactiveField] == 'Name') {
					$el.html('Name');
				}

				var $el = $('#tsortOrderSpan');
				if (sortOrder[tactiveField] == 'Descending') {
					$el.removeClass('up-caret');
				} else if (sortOrder[tactiveField] == 'Descending') {
					$el.addClass('up-caret');
				}

				textboxes = lsgv.selectAll("g.legend-item").append("text").text(function(d) {
					return d.label;
				}).style("fill", 'black');
				
				maxlengendX = d3.max(textboxes[0], function(d) {
					//return d.clientWidth;
					if (d!=null) {
						return d.__data__.label.length*text_height_max/2;
					} 
					return 0;
				
				});
				
				text_width_max=maxlengendX + (4 * text_height_max);
				num_legend_col=Math.max(Math.floor(legendsvgwidth / text_width_max),0);
				
				textboxes.attr("x", function(d, i) {
							coffset= (i % num_legend_col) * text_width_max;
							return row_offset+ coffset + (2 * text_height_max);
						}).attr("y", function(d, i) {
							if (d.type == 'value') {
								row = Math.floor(i / 2) + 1;
								row = i + 1 
							} else if (d.type == 'REMOVE') {
								row = Math.floor(i / 2) + 1;
								row = i + 1 
							} else {
								row = i + 1;
							}
							row=Math.floor(i/num_legend_col)+1;
					
							return (row * text_height_max);
						})

				rects = lsgv
						.selectAll("g.legend-item")
						.append("rect")
						.attr("x", function(d, i) {
							coffset = 0;
							if (d.type == 'value') {
								coffset = 0;
							} else if (d.type == 'REMOVE') {
								coffset = text_height_max
								coffset = 0;
							}
							
							coffset= (i % num_legend_col) * text_width_max;

							return (row_offset+ coffset);
						})
						.attr(
								"y",
								function(d, i) {
									if (d.type == 'value') {
										row = Math.floor(i / 2) + 1;
										row = i + 1 
									} else if (d.type == 'REMOVE') {
										row = Math.floor(i / 2) + 1;
										row = i + 1; 
									} else {
										row = i + 1;
									}
									row=Math.floor(i/num_legend_col)+1;
									
									return (row * text_height_max
											- text_height_max + 4);
								})
						.attr("width", text_height_max - 2)
						.attr("height", text_height_max - 2)
						.style(
								"fill",
								function(d, i) {
									if ((d.varName in showValues)
											&& (d.label in showValues[d.varName])
											&& (showValues[d.varName][d.label] == "true")) {
											return color(d.order);
																				if (d.type == 'value') {
											return color(Math.floor(d.order));
										} else if (d.type == 'REMOVE') {
											return color(Math
													.floor(d.order));
										} else {
									}

									}
									return 'white';
								})
						.style("stroke", "black")
						.style("stroke-width", "1")
	
	
	/*					.on(
								"click",
								function(d) {
									if ((d.varName in showValues)
											&& (d.label in showValues[d.varName])) {
										showValues[d.varName][d.label] = (showValues[d.varName][d.label] == "true") ? "false"
												: "true";
										// console.log(showValues[d.varName][d.name]);
									}
									d3.event.stopPropagation();
									doSearch();
								});
	*/
				// Resize list svg to fit all items
				// Resize list svg to fit all items

				// Resize list svg to fit all items				 	
				maxlengendY = d3.max(textboxes[0], function(d) {
					return d.clientHeight + 5;
				});
				if (maxlengendY == null || maxlengendY < 10) {
					maxlengendY = 10
				}

				// maxlengendY + maxlengendX*num_legend_col
				legendsvgheight = Math.floor(textboxes[0].length /num_legend_col+1)* maxlengendY + 40

				lsgv.style('width', legendsvgwidth + 'px')
				.style('height', legendsvgheight + 'px')
				.attr("transform", "translate(" + margin.left + "," + 0 + ")");
				
				
				var maketip = function(d) {
					dd = new Date(d.date);
					var tip = '<p class="tip3">' + d.varName + '<p class="tip1">'
							+ countformat2(d.total_count) + '</p> <p class="tip3">'
							+ d.date + '</p>';
					return tip;
				}

				if (graphType == 'Line Chart') {

					var tooltipDate = d3.time.format("%d/%m/%y");

					var bisectDate = d3.bisector(function(d) {
						return new Date(d.date);
					}).left;
					var countformat = d3.format("0,000");
					var dateformat = d3.time.format("%m-%d-%Y");

					function mousemove3() {
						var m = d3.mouse(this);
						var x0 = x.invert(d3.mouse(this)[0]), dl = d3.select(
								this).datum();
						i = bisectDate(dl, x0, 1)
						if (i>=dl.length) {
							i=dl.length-1;
						}
						
						d0 = dl[i - 1]
						d1 = dl[i]
								d = x0 - new Date(d0.date) > new Date(d1.date) - x0 ? d1 : d0;
						txt = f3.select("text");
						rect = f3.select("rect");
						dd = new Date(d.date);

						dp = d3.select(this.parentNode).datum();
						dvar = dp.varName;
						nlabel = dp.label + ': '
								+ countformat2(Math.floor(d.total_count)) + '   ('
								+ d.date + ')'

						txt.text(nlabel);
						tbox = txt.node().getBBox();
						dx = x(new Date(d.date))
						
						dx=m[0]
						dy=m[1]
 
						//dyd=y(d.total_count) - 3;
						// dy = y(d.total_count) - 9

						tx = 0
						if (dx + tbox.width > width2) {
							tx = -tbox.width
						}
						ty = -tbox.height
						ty2 = ty - 10
						f3
								.attr("transform", "translate(" + dx + "," + dy
										+ ")");
						rect.attr("transform",
								"translate(" + tx + "," + ty2 + ")").attr(
								"width", tbox.width + 20).attr("height",
								tbox.height + 6).attr("fill",
								"rgba(255,255,255,.50)")
						txt.attr("transform", "translate(" + tx + "," + ty
								+ ")");
					}

					for ( var i in drawlayers) {
						// Exnted last value
						if (drawlayers[i].vals != null) {
							var lastval = drawlayers[i].vals[drawlayers[i].vals.length - 1]
							var newmaxdate = date2_max
							var newlastval = {
								total_count : lastval.total_count,
								add_count:0,
								remove_count:0,
								date : newmaxdate
							}
							// drawlayers[i].vals.push(newlastval)
						}
					}

					var line = d3.svg.line().interpolate("step-after").x(
							function(d) {
								// rd=new Date();
								return x(new Date(d.date));
							}).y(function(d) {
								return y(d.total_count);					
							})

					drawgroup.append("path").datum(function(ls) {
						// Find relevant data in layers
						return ls.vals;
					}).attr("class", "line").style("stroke-width", "3px")
							.style("stroke", function(d) {
								dp = d3.select(this.parentNode).datum()
								return color(dp.order);
							}).attr("d", line).on("mouseover", function(d) {
								d3.select(this) //on mouseover of each line, give it a nice thick stroke
								.style("stroke-width", '10px');
								f3.style("display", null);
							}).attr("clip-path", "url(#clip)")
							.on("mouseout", function(d) { //undo everything on the mouseout
								d3.select(this).style("stroke-width", '3px');
								f3.style("display", "none");
							}).on("mousemove", mousemove3).on(
									"click",
									function(d, i) {
										var $el = $(this);

										var dateformat5 = d3.time.format(spanAsofDateFormat);

										var m = d3.mouse(this);
										var x0 = x.invert(d3.mouse(this)[0]), dl = d3.select(
												this).datum();
										i = bisectDate(dl, x0, 1)
										if (i>=dl.length) {
											i=dl.length-1;
										}
										
										d0 = dl[i - 1]
										d1 = dl[i]
										d = x0 - new Date(d0.date) > new Date(d1.date) - x0 ? d1 : d0;
										txt = f3.select("text");
										rect = f3.select("rect");
										dd = new Date(d.date);

										dp = d3.select(this.parentNode).datum();
										dvar = dp.varName;
										nlabel = dp.label + ': '
												+ countformat2(Math.floor(d.total_count)) + '   ('
												+ d.date + ')'

										var edate = d.date;
										var jdate1 = new Date(edate);
										var dvarname = spanAsofDateField;
										var jdatestr=dateformat5(jdate1);
											
										dp = d3.select(this.parentNode).datum();
										var varName = dp.varName;
										var varVal = dp.label;

										var paraFilters = {};
										paraFilters[varName] = [ {
											type : INDEXED_FIELDTYPE,
											val : varVal
										} ];
										paraFilters[dvarname] = [ {
											type : INDEXED_FIELDTYPE,
											op : "AS_OF_DATE",
											val : jdatestr
										} ];

										$('#parametricForm').data('filterForm')
												.insertLoadingFilterSet().loadFilters({
													boolOperator : 'AND',
													filterFields : paraFilters,
													childGroups : null,
													tag : ''
												}, null, null);
									})

					// Tooltip
					f3 = focus.append("g").attr("class", "f3").style("display",
							"none").style("pointer-events","none")

					// f3.append("circle").attr("r", 4.5);

					f3.append("rect").style("background", "rgba(255,255,255,.50)");
					f3.append("text").attr("x", 9).attr("dy", ".35em");

				} else if (graphType == 'Bar Chart') {

					// Add a rect for each date.
					var rect = drawgroup.selectAll("rect").data(function(ls) {
						return ls.vals;
					}).enter().append("svg:rect").attr("x", function(d) {
						return x(new Date(d.date));
					}).attr("y", function(d) {
						//            	return  height;
						return y(d.y0 + d.y);
					}).attr("height", function(d) {
						//            	return  0; 
						h=Math.max(y(d.y0) - y(d.y0 + d.y),0);
						return h;
					}).attr("width", function(d, i) {
						w = barwidth;
						dp = d3.select(this.parentNode).datum();
						nd = dp.vals[i + 1];
						if (nd != null) {
							w = x(new Date(nd.date)) - x(new Date(d.date))
						}
						// w=x(data[i])-x(d.date);
						return w;

					}).attr("clip-path", "url(#clip)")
					.on("mouseover", function() {
						f2.style("display", null);
					}).on("mouseout", function() {
						f2.style("display", "none");
					}).on("mousemove", mousemove2).on(
							"click",
							function(d, i) {
								var $el = $(this);

								var dateformat5 = d3.time.format(spanAsofDateFormat);

								var edate = d.date;
								var jdate1 = new Date(edate);
								var dvarname = spanAsofDateField;
								var jdatestr=dateformat5(jdate1);
									
								dp = d3.select(this.parentNode).datum();
								var varName = dp.varName;
								var varVal = dp.label;

								var paraFilters = {};
								paraFilters[varName] = [ {
									type : INDEXED_FIELDTYPE,
									val : varVal
								} ];
								paraFilters[dvarname] = [ {
									type : INDEXED_FIELDTYPE,
									op : "AS_OF_DATE",
									val : jdatestr
								} ];

								$('#parametricForm').data('filterForm')
										.insertLoadingFilterSet().loadFilters({
											boolOperator : 'AND',
											filterFields : paraFilters,
											childGroups : null,
											tag : ''
										}, null, null);
							})

					// Tooltip
					f2 = focus.append("g").attr("class", "f2").style("display",
							"none");

					// f2.append("circle").attr("r", 4.5);

					f2.append("rect")
					f2.append("text").attr("x", 9).attr("dy", ".35em");

				} else if (graphType == 'Daily Change') {
					// Add a rect for each date.
					if (drawgroupA != null) {
						var rect = drawgroupA.selectAll("rect").data(
								function(ls) {
									return ls.vals;
								}).enter().append("svg:rect").attr("x",
								function(d) {
									return x(new Date(d.date));
								}).attr("y", function(d) {
							//            	return  height;
							return y(d.y0 + d.y);
						}).attr("height", function(d) {
							//            	return  0; 
							return y(d.y0) - y(d.y0 + d.y);
						}).attr("width", function(d, i) {
							w = barwidth;
							//dp=d3.select(this.parentNode).datum();
							//nd=dp.vals[i+1];
							//	w=x(nd.date)-x(d.date)
							//}
							// w=x(data[i])-x(d.date);
							return w;

						}).attr("clip-path", "url(#clip)")
						.on("mouseover", function() {
							f2.style("display", null);
						}).on("mouseout", function() {
							f2.style("display", "none");
						}).on("mousemove", mousemove4).on(
								"click",
								function(d, i) {
									var $el = $(this);

									var dateformat5 = d3.time.format(spanAsofDateFormat);

									var edate = d.date;
									var jdate1 = new Date(edate);
									var dvarname = spanStartDateField;
									var jdatestr=dateformat5(jdate1);
					            	var jdate2=dateAdd(jdate1,'Day',1);
					            	var jdate2str=dateformat5(jdate2)
					                var dval=jdatestr+','+jdate2str
										
									dp = d3.select(this.parentNode).datum();
									var varName = dp.varName;
									var varVal = dp.label;

									var paraFilters = {};
									paraFilters[varName] = [ {
										type : INDEXED_FIELDTYPE,
										val : varVal
									} ];
									paraFilters[dvarname] = [ {
										type : PARAMETRIC_FIELDTYPE,
										op : "BETWEEN",
										val : dval
									} ];

									$('#parametricForm').data('filterForm')
											.insertLoadingFilterSet().loadFilters({
												boolOperator : 'AND',
												filterFields : paraFilters,
												childGroups : null,
												tag : ''
											}, null, null);
								})
					}
					if (drawgroupR != null) {

						var rect = drawgroupR.selectAll("rect").data(
								function(ls) {
									return ls.vals;
								}).enter().append("svg:rect").attr("x",
								function(d) {
									return x(new Date(d.date));
								}).attr("y", function(d) {
							//            	return  height;
							return 2 * y(0) - y(d.y0);
						}).attr("height", function(d) {
							//            	return  0; 
							return y(d.y0) - y(d.y0 + d.y);
						}).attr("width", function(d, i) {
							w = barwidth;
							//dp=d3.select(this.parentNode).datum();
							//nd=dp.vals[i+1];
							//	w=x(nd.date)-x(d.date)
							//}
							// w=x(data[i])-x(d.date);
							return w;

						}).attr("clip-path", "url(#clip)")
						.on("mouseover", function() {
							f2.style("display", null);
						}).on("mouseout", function() {
							f2.style("display", "none");
						}).on("mousemove", mousemove5).on(
								"click",
								function(d, i) {
									var $el = $(this);

									var dateformat5 = d3.time.format(spanAsofDateFormat);

									var edate = d.date;
									var jdate1 = new Date(edate);
									var dvarname = spanEndDateField;
									var jdatestr=dateformat5(jdate1);
					            	var jdate2=dateAdd(jdate1,'Day',1);
					            	var jdate2str=dateformat5(jdate2)
					                var dval=jdatestr+','+jdate2str
										
									dp = d3.select(this.parentNode).datum();
									var varName = dp.varName;
									var varVal = dp.label;

									var paraFilters = {};
									paraFilters[varName] = [ {
										type : INDEXED_FIELDTYPE,
										val : varVal
									} ];
									paraFilters[dvarname] = [ {
										type : PARAMETRIC_FIELDTYPE,
										op : "BETWEEN",
										val : dval
									} ];

									$('#parametricForm').data('filterForm')
											.insertLoadingFilterSet().loadFilters({
												boolOperator : 'AND',
												filterFields : paraFilters,
												childGroups : null,
												tag : ''
											}, null, null);
								})

					}
					
					
					
					// Tooltip
					f2 = focus.append("g").attr("class", "f2").style("display",
							"none")
							.style("background", "rgba(255,255,255,.90)");

					// f2.append("circle").attr("r", 4.5);

					f2.append("rect")
					f2.append("text").attr("x", 9).attr("dy", ".35em");

					/*            num_rect=rect[0].length;
					
					           rect.transition()
					           .attr("duration",1000)
					           .delay(function(d, i) { return (i/num_rect) * 1000; })
					           .attr("y", function(d) { return y(d.y0 + d.y); })
					           .attr("height", function(d) { return y(d.y0) - y(d.y0 + d.y); });
					 */
					/*          rect.on('mouseover', function(evt){
					               var html = 'HELLO THERE';
					             
					              $tooltip.html(html.join('<BR>'))
					              .show().position({ my: 'left bottom', of: evt, offset: '0 -5', collision: 'fit', within: $topicmap});
					           })
					           .on('mouseout', $tooltip.hide());
					 */
									

				}
				var brush = d3.svg.brush()
				  .x(x)
				  .y(y)
				  .on("brushend", brushend);
				
				
				function resetBrush() {
					brush.remove();
				}				
				function brushend() {
				
				  extent = brush.extent();
				  if (brush.empty()) {
					  rerender();
					  return;
				  }
				  xe=[extent[0][0],extent[1][0]];
				  ye=[extent[1][1],extent[0][1]];
				  x.domain(xe);
				  y.domain(ye);
				  
				  
				  // Redraw graph
				  if (graphType == 'Bar Chart') {
			  

					// Add a rect for each date.
				  	barwidth = width2 / getSegments(date_min, date_max, selectedInterval);
					st = stack(drawlayers);
					var rect = drawgroup.selectAll("rect").attr("x", function(d) {
						return x(new Date(d.date));
					}).attr("y", function(d) {
						//            	return  height;
						return y(d.y0 + d.y);
					}).attr("height", function(d) {
						//            	return  0; 
						h=Math.max(y(d.y0) - y(d.y0 + d.y),0);
						return h;
					}).attr("width", function(d, i) {
						w = barwidth;
						dp = d3.select(this.parentNode).datum();
						nd = dp.vals[i + 1];
						if (nd != null) {
							w = x(new Date(nd.date)) - x(new Date(d.date))
						}
						// w=x(data[i])-x(d.date);
						return w;

					})
				    

				  } else if (graphType == 'Line Chart') {

					var line = d3.svg.line().interpolate("step-after").x(
							function(d) {
								// rd=new Date();
								return x(new Date(d.date));
							}).y(function(d) {
								return y(d.total_count);					
							})
					redrawlines=drawgroup.selectAll("path")
					redrawlines.attr("class", "line").style("stroke-width", "3px")
							.style("stroke", function(d) {
								dp = d3.select(this.parentNode).datum()
								return color(dp.order);
					}).attr("d", line)			  
					  
					  
					  
					  
					  
					  
					  
				  } else if (graphType == 'Daily Change') {
					  
						// Add a rect for each date.
					  	barwidth = width2 / getSegments(date_min, date_max, selectedInterval);
					  	addst = stack_add(addlayers);
						var rect = drawgroupA.selectAll("rect").attr("x", function(d) {
							return x(new Date(d.date));
						}).attr("y", function(d) {
							//            	return  height;
							return y(d.y0 + d.y);
						}).attr("height", function(d) {
							//            	return  0; 
							h=Math.max(y(d.y0) - y(d.y0 + d.y),0);
							return h;
						}).attr("width", function(d, i) {
							w = barwidth;
							dp = d3.select(this.parentNode).datum();
							nd = dp.vals[i + 1];
							if (nd != null) {
								w = x(new Date(nd.date)) - x(new Date(d.date))
							}
							// w=x(data[i])-x(d.date);
							return w;

						})
				  
					  	remst = stack_rem(remlayers);
						var rect = drawgroupR.selectAll("rect").attr("x",
								function(d) {
									return x(new Date(d.date));
								}).attr("y", function(d) {
							//            	return  height;
							return 2 * y(0) - y(d.y0);
						}).attr("height", function(d) {
							//            	return  0; 
							return y(d.y0) - y(d.y0 + d.y);
						}).attr("width", function(d, i) {
							w = barwidth;
							dp = d3.select(this.parentNode).datum();
							nd = dp.vals[i + 1];
							if (nd != null) {
								w = x(new Date(nd.date)) - x(new Date(d.date))
							}
							return w;

						}).attr("clip-path", "url(#clip)")
							
													
				/*				
						drawgroupR.selectAll("rect").attr("x", function(d) {
							return x(new Date(d.date));
						}).attr("y", function(d) {
							//            	return  height;
							return y(d.y0 + d.y);
						}).attr("height", function(d) {
							//            	return  0; 
							h=Math.max(y(d.y0) - y(d.y0 + d.y),0);
							return h;
						}).attr("width", function(d, i) {
							w = barwidth;
							dp = d3.select(this.parentNode).datum();
							nd = dp.vals[i + 1];
							if (nd != null) {
								w = x(new Date(nd.date)) - x(new Date(d.date))
							}
							// w=x(data[i])-x(d.date);
							return w;

						})
				  */
					  
				  }

				 focus_chart.select(".x.axis").call(xAxis);
				 focus_chart.select(".y.axis").call(yAxis);
				 brush.clear();
				 brushg.call(brush);
				 
				  get_button = d3.select(".clear-button");
				  xclearoffset=width2;
				  if(get_button.empty() === true) {
				    clear_button = focus_svg.append('text')
				      .attr("y", 50)
				      .attr("x", xclearoffset)
				      .attr("class", "clear-button")
				      .style("display","block")
				      .text("Clear Zoom").on(
								"click",
								function(d, i) {
									rerender();
									clear_button.style("display","none");
									return;
								});
				  }		
				
				}
				  
				  
		/*   Only draw brush if zoom mode is selected */
		if (zoomMode=="Zoom Mode") {
				brushg = focus_chart.append("g")
			      .attr("class", "brush")
			      .call(brush);
		}
				
			}
			SearchEvents.toggleResultsLoading(false, $ttotalResults);
			return;

		}
		var dragpoint;

		function trending_click() {
			// Ignore the click event if it was suppressed
			if (d3.event.defaultPrevented)
				return;

			// Extract the click location\    
			var dragpoint = d3.mouse(this), p = {
				x : dragpoint[0],
				y : dragpoint[1]
			};

			// Append a new point
			focus.append("rectangle").attr("width", "10")
					.attr("height", height).attr("transform",
							"translate(" + p.x + "," + margin.top + ")").attr(
							"class", "box").style("cursor", "pointer").call(
							drag).style("stroke", "#999999").style("fill",
							"#F6F6F6");

		}

		var drag = d3.behavior.drag().on("drag", dragmove);

		function dragmove(d) {
			var x = d3.event.x;
			var y = d3.event.y;
			d3.select(this).attr("transform", "translate(" + x + "," + y + ")");
		}

	}
	 return;
	 
});












