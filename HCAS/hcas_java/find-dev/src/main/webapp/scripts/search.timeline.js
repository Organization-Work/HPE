jQuery(function($) {
	var tempID  =  this.versionObj;
	var tempID =  localStorage.getItem("storageId");
	
	// Resources and Constants
	var TIMELINE_URL = 'ajax/search/getTimelineTagValueDetails.json';
	var TIMELINE_DELTA_URL = 'ajax/search/getTimelineTagValueDeltaDetails.json';
	var TIMELINE_NETCHANGE_URL = 'ajax/search/getTimelineTagValueNetChangeDetails.json';
	var CLASS_FIELDS_URL = 'ajax/search/getTimelineFields.json';
	var TL_FILTERS_LIST_TEMPLATE = '../templates/search/timeline.fieldsList'+tempID+'.template';
	var TL_SETTING_TEMPLATE = '../templates/search/timelineviewer.settings'+tempID+'.template';
	var TL_LEGEND_TEMPLATE = '../templates/search/timelineviewer.legend'+tempID+'.template';
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

	
	// TODO: Constants -- Make these per view from Config
	var demo_fields=["PROV_NUM","CRISE_CAT","DIS_ENROL_CD","CAP_RATE_IND","RES_CITY","RES_STATE","RES_COUNTY"];
    var demo_view="834L";
	var context_title="Manage Care Population Trending";
    var context_yaxis_label="Recipients";
    var context_xaxis_label="Date";
    var focus_title_bar="Active Recipients by";
    var focus_title_comparison="Active Recipients by";
    var focus_title_delta="Daily Change by";
    var focus_yaxis_label="Recipients";
    var focus_xaxis_label="Date";

	var DEFAULT_DATE_PERIOD = 'day';
	var DEFAULT_SORT_TYPE = 'Value';
	var DEFAULT_SORT_ORDER = 'Descending';
	var DEFAULT_validStartDate= '1420070400'; //'1430452800'; // 9-1-2014
	// var DEFAULT_validEndDate='1437004799';//'1451624400';  // 1-2-2015
	var DEFAULT_validEndDate='1451624400';

	var DEFAULT_baselineDate='1430438400'; // '1430452800';	  // 9-1-2014
	var DEFAULT_asOfDate='1436918400'; // '1451624400';		  // 11-5-2014
	var selectedInterval = DEFAULT_DATE_PERIOD;
	var activeField='CAP_RATE_IND';
	// -----------------------------------------------------------------------------

	
	// Global Variables
    var focus_title=focus_title_bar;
	var date_field_selected={};
	var date_field_selected_label={};
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
	
	var showTotals=true;
    var time_svg;
	var stateChange=false
	var currentSearch, lastSearch, previousXHR, paraFieldsValue;
	var timeline, lastTotalResults;
	var extent; 
	var dataToRender = false;
	var numrect = 1;
	var time_svg=null;
	var y2_max=100;
	
    
    // Variables set from current UI Settings 
    var maxFields =$("#histogram-max-rows").val() ? $("#histogram-max-rows").val():20;

	
	// Add Elements for Structure of Page on UI Load
	// ---------------------------------------------------------------------------------------------
	var $showPanel = $('#showPanel');
	var $timeline = $('#timeline');
	
	
	// Settings Bar
	var $settingsEl = $('<div id="timeline-settings" class="btn-toolbar"></div>').appendTo($timeline);
	var settingsTemplate = $.resGet(TL_SETTING_TEMPLATE);	
	var $settings = $(settingsTemplate)
	.appendTo($settingsEl)
	.on('click','.timeline-refresh', function(evt) {
			evt.preventDefault();
			doSearch();
		})
	.on('click', '.timeline-settings', function(evt) {
		// prevent the popup from closing when you click on the input buttons
		evt.stopPropagation();
	});
	
	// Totals Display
	var $totalResults = $('<div id="timeline-totals" class="view-results-label"></div>').appendTo($settingsEl);
	
	// No Results Control
	var $noResultEl = $('<div id="no-results"></div>').appendTo($timeline);

	// Context Control
	var $contextEl = $('<div id="timeline-context"></div>').appendTo($timeline);
	var $tooltip =  $contextEl.append('<div class="tooltip"></div>');
	 	
	
	// Focus Control
	var $focusEl = $('<div id="timeline-focus"></div>').appendTo($timeline);
		
	
	// Loading Control
	var $loading = $('<div class="visualizer-loading"></div>').appendTo($timeline).hide();
	

    var $filtersEl = $('<div id="timeline-filters"></div>').appendTo($timeline).on('click', '.tfield-group-list li', function(){
        $('.tfield-group-list li.active').removeClass('active');
        var $el = $(this).addClass('active');
        $el.siblings('li').removeClass('active');
        activeField = $el.data('timelinefield');
        
        doSearch();
    });
    var fieldListTpl;
    var fieldList;
    
	// Legend Control
	var $legendAreaEl = $('<div id="timeline-legendarea"></div>').appendTo($timeline);
	
	// Legend Title Control
	var $legendTitleEl = $('<div id="timeline-legendtitlediv"><text id="timeline-legendtitle"></text></div>').appendTo($legendAreaEl);
	
	var $legendlegendBtnBarEl = $('<div id="timeline-btnbar" class="btn-toolbar"></div>').appendTo($legendAreaEl);
	var legendTemplate = $.resGet(TL_LEGEND_TEMPLATE);	
	var $legendsettings = $(legendTemplate)
	.appendTo($legendlegendBtnBarEl)
	.on('click','.clearLegendSelect', function(evt) {
			evt.preventDefault();
			
			// Unselect all items
			for (k in showValues[activeField]) {
				showValues[activeField][k]="false";
			}
			doSearch();
		})
	.on('click', '.sortTypeSelect', function(evt) {
			if (sortType[activeField]=='Value') {
				sortType[activeField]='Name';
			} else {
				sortType[activeField]='Value';				
			}
			doSearch();
	})
	.on('click', '.sortOrderSelect', function(evt) {
			if (sortOrder[activeField]=='Descending') {
				sortOrder[activeField]='Ascending';
			} else {
				sortOrder[activeField]='Descending';				
			}
			doSearch();
	})
	.on('click', '.resetLegendSelect', function(evt) {
		// Unselect all items
		initShowVals[activeField]=true;
		sortType[activeField]='Value';				
		sortOrder[activeField]='Descending';				
		doSearch();
	})
	
	
	// Legend Control
	var $legendEl = $('<div id="timeline-legend"></div>').appendTo($legendAreaEl);

	// Filter Values
	var $paraFilterForm = $('#parametricForm').data('filterForm');
	
   
	 var clearChart = function() {
	        timeline && timeline.redraw({
	            name: '',
	            size: 0,
	            data: []
	        }, false);
	        
	 };
	 // ----------------------------------------------------------------------------------------------------------
	 
	 
	 
	 // Helper Functions
	 // --------------------------------------------------------------
	 
	// Parsing functions for dates
	var parseDate = d3.time.format("%H:%M:%S %m/%d/%Y").parse;
	var parseEpoch = d3.time.format("%H:%M:%S %m/%d/%Y").parse;
	var countformat2 = d3.format("0,000");
    var dateformat3 = d3.time.format("%m-%d-%Y");
 
	// The following code was contained in the callback function.
	function dateAdd(date, interval, units) {
		  var ret = date; // new Date(date); //don't change original date
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
	 
    function onShown(){         
        rerender();
    }

	function doSearch() {
		var totalResultsLabel = '  Total ' + SearchSettings.getResultViewText()
				+ 's: '
				+ SearchEvents.getFormattedResult(lastTotalResults);

		lastSearch = currentSearch;
		$noResultEl.html("");
        
    	if (!SearchConfig.preloadParaValues) {
    		var currentParaFilters = $paraFilterForm.getAllParaFilters();  
    		if (_.indexOf(_.values(currentParaFilters), activeField) === -1) {
    			currentParaFilters[activeField] = activeField;
    		}
    		
    		submitData = _.extend({fieldNames: _.values(currentParaFilters)}, currentSearch);
    	}
        SearchEvents.toggleResultsLoading(true, $totalResults);
        var view = SearchEvents.getSelectedSearchView();
        var zSD=null;
        var zED=null;
        if (extent!=null) {
        	d1=extent[0];
        	d2=extent[1];
        	zSD=Math.floor(d1.getTime()/1000);
        	zED=Math.floor(d2.getTime()/1000);
        }
        
        // Set the valuesToRenderList from the showValues array
        renderList=""
        sep=""
        if ((showValues!=null) && (activeField in showValues)) {
	        for (r in showValues[activeField]) {
	        	if (showValues[activeField][r]=="true") {
	        		renderList+=sep+r;
	        		sep=","
	        	}
	        }
        }
		
        req_url=TIMELINE_URL;
		graphType=$('#timeline-charttype').attr("value");
		if (graphType=='Bar Chart') {
	        req_url=TIMELINE_URL;			
		} else if (graphType=='Line Chart') {
	        req_url=TIMELINE_URL;			
		} else if (graphType=='Daily Change') {
	        req_url=TIMELINE_DELTA_URL;						
		} else if (graphType=='Net Change') {
	        req_url=TIMELINE_NETCHANGE_URL;									
		}
		
        
        previousXHR = $
				.ajax({
					url : req_url + '?' + $.param({
						field:  activeField,
						maxResults: '',
						maxDisplayVals: '10',
						validStartDate: DEFAULT_validStartDate,//'1409544000', // 9-1-2014
						validEndDate: DEFAULT_validEndDate,//'1420174800',	  // 1-2-2015
						baselineDate: DEFAULT_baselineDate,//'1409544000',	  // 9-1-2014
						asOfDate: DEFAULT_asOfDate, //'1415163600',		  // 11-5-2014
						zoomStartDate: zSD,
						zoomEndDate: zED,
						valuesToRenderList: renderList
					}, true),
					type : 'POST',
					contentType : 'application/json',
					data : JSON.stringify(currentSearch),
					dataType : 'json',
					success : function(response) {
					}
				})
				.always(function() {
					//$loading.hide();
				//	SearchEvents.toggleResultsLoading(false, $totalResults);
				})
				.fail(
						function(response) {
							$totalResults.html(SearchEvents.getSearchErrorMessage("Error loading timeline data from server"));
							SearchEvents.toggleResultsLoading(false, $totalResults);
						}).done(function(json) {

					dataToRender = json;
					$totalResults.html(totalResultsLabel);
					
                    if (!dataToRender || !dataToRender.tdata) {
        				cnode = d3.select('#context_svg');
        				if (cnode) {
        					cnode.html('');
        					cnode.remove();
         				}
           				fnode = d3.select('#focus_svg');
        				if (fnode) {
        					fnode.html('');
        					fnode.remove();
         				}
          				lnode = d3.select('#legend_svg');
        				if (lnode) {
        					fnode.html('');
        					fnode.remove();
         				}
                   	$noResultEl.html('<span style="color: red;">No values found in this time period. Try a different search</span>');
        				SearchEvents.toggleResultsLoading(false, $totalResults);
             }
                    else {
                    	renderTotalsIfVisible();
                    	
                    	renderIfVisible();
                    }
				});

	
         paraFieldsValue = {};
        
        var submitData = currentSearch;
        
        
	    if (!SearchConfig.preloadParaValues) {
	    		var currentParaFilters = $paraFilterForm.getAllParaFilters();  
	    		if (_.indexOf(_.values(currentParaFilters), activeField) === -1) {
	    			currentParaFilters[activeField] = activeField;
	    		}
	    		
	    		submitData = _.extend({fieldNames: _.values(currentParaFilters)}, currentSearch);
	    		
	    }
	    $loading.hide()
	    SearchEvents.toggleResultsLoading(false, $totalResults);
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

	// ----------------------------------------------------------------

	   
	 // Rendering Functions
	 // --------------------------------------------------------------

	function rerender() {
 
        maxTimelineFields = $("#histogram-max-rows").val();
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

                // build data for multiselect and sunburst
                for (var i = 0, len = desired.fieldValues.length; i < len; ++i) {
                    var node = desired.fieldValues[i];
                    if (node != null) {
	                    var value = node.value;
	                    var labelVal = meta.parametric && meta.parametric.ranges ? getRangeLabel(value) : value;
	                    multiselectData.push({count: node.count, label: labelVal, value: value});
	
	                    if (i < maxTimelineFields) {
	                        fieldMap.push({
	                            filter: label,
	                            name: isNotSnomed ? labelVal : value.slice(0, value.lastIndexOf(' (')),
	                            rawName: value,
	                            size: node.count,
	                            data: [],
	                            totalNumResults: desired.totalValues,
	                            numValues: len
	                        });
	                    }
	                  }
                }
                
                toDraw =  {
                    data: fieldMap,
                    name: 'Total' + (lastTotalResults ? ': ' + lastTotalResults : ''),
                    totalCount : lastTotalResults
                };
            }
            // renderIfVisible();
            
        }

        $timeline.css('min-height', $filtersEl.position().top + $filtersEl.outerHeight(true));
        //$multiSelect.multiselect('dataprovider', multiselectData);
        
        if (lastTotalResults > 0) {
		      if (_.isEmpty(toDraw.data)) {
		    	  $noResultEl.html('<span style="color: red;">No data to render.</span>');
		      } else {
		    	  $noResultEl.html('');
		      }
        }

    /*    timeline.redraw(toDraw || {
            name: '',
            size: 0,
            data: []
        });
	  */    
    }
   
	function renderTotalsIfVisible() {
		if (dataToRender && $timeline.is(':visible')) {
	        SearchEvents.toggleResultsLoading(true, $totalResults);

   	
			// Context ------------------------------------------
	    	$contextEl.empty();

			var margin = {top : 60,right : 200,bottom : 50, left : 120};
			var width = $showPanel.width() - margin.left - margin.right;
			if (width<400)
				width=400
			var height = $showPanel.height() - margin.top - margin.bottom;
			height = Math.floor(height/4)
			if (height<300) {
				height=300
			}
			var margin2 = {top: 60, right: 10, bottom: 100, left: 120};
			width2=width-margin2.left-margin2.right;
			height2=height-margin2.top-margin2.bottom;

			context_div=d3.select('#timeline-context')
			
			
			context_svg = context_div.append('svg')
			.attr("id","context_svg")
		    .attr("width", width)
		    .attr("height", height);
			
			
	
			
			context = context_svg.append("g")
		    .attr("class", "context")
		    .attr("transform", "translate(" + margin.left + "," + margin.top + ")");	

						
			var x2 = d3.time.scale().range([ 0, width2 ]);
			var y2=  d3.scale.linear().range([0,height2 ]);
			var xAxis2 = d3.svg.axis().scale(x2).orient("bottom")
			var yAxis2 = d3.svg.axis().scale(y2).orient("left").ticks(5);

			var area2 = d3.svg.area()
		    .interpolate("step-after")
		    .x(function(d) { 
		    	return x2(d.date); 
		    })
		    .y0(height2)
		    .y1(function(d) { 
		    	return y2(d.count); 
		    });
		    function mousemove1() {
		        var x0 = x2.invert(d3.mouse(this)[0]),
		            i = bisectDate(data, x0, 1),
		            d0 = data[i - 1],
		            d1 = data[i],
		            d = x0 - d0.date > d1.date - x0 ? d1 : d0;
		            txt=f1.select("text");
		            rect=f1.select("rect");
		            dd=new Date(+d.date);
		  
		            txt.text(countformat(Math.floor(d.count)) + '   ('+dateformat2(dd)+')');
			        tbox=txt.node().getBBox();
		            dx=x2(d.date)
		            dy=y2(d.count)
		           
		            tx=0
		            if (dx+tbox.width>width2) {
		            	tx=-tbox.width
		            }
		            ty=-tbox.height
		            ty2=ty-10
		            	         
		          
	            		
		            f1.attr("transform", "translate(" + dx + "," + dy + ")");
		            rect.attr("transform", "translate(" + tx + "," + ty2 + ")")           	
            		.attr("width", tbox.width+20)
            		.attr("height", tbox.height+6)
            		.attr("fill", "rgba(255,255,255,0.90)")
			        txt.attr("transform", "translate(" + tx + "," + ty + ")");
		   }

			// End Context ------------------------------------------
   	
			//Context -----------------------------------------------------------
			recordcnt=dataToRender.tdata.totalsLayer;
			
			//End Context -------------------------------------------------------
 	
			// Context ------------------
			date2_min=new Date(parseInt(dataToRender.tdata.tRange.dateMin));

			min2_date_str=date2_min.toString();

			date2_max=new Date(parseInt(dataToRender.tdata.tRange.dateMax));		
			date2_max=dateAdd(date2_max,'day',1);
			max2_date_str=date2_max.toString();

			if (recordcnt.vals.length==0) {
				$totalResults.html(SearchEvents.getSearchErrorMessage("Error loading timeline data from server"));
				SearchEvents.toggleResultsLoading(false, $totalResults);
				return;
			}
			// Exnted last value
			var lastval=recordcnt.vals[recordcnt.vals.length-1]
			var newmaxdate= jsDateToEpoch(date2_max)
			var newlastval={count: lastval.count, date: newmaxdate}
			recordcnt.vals.push(newlastval)
			
			function brushed() {
				  if (brush.empty()) {
					  renderIfVisible()
					  return
				  }		
				  e1=brush.extent()
				  x.domain(e1)
		//		  focus.select(".x.axis").call(xAxis)

				  if (extent!=null) {
				  	barwidth=width/getSegments(extent[0],extent[1],selectedInterval);
				  } else {
					 barwidth=width/getSegments(date_min,date_max,selectedInterval);
				  }
				  valgroup.selectAll("rect")
		            .attr("y", function(d) { 
		            	return y(d.y0 + d.y);
		            })
		            .attr("height", function(d) { 
		                return y(d.y0) - y(d.y0 + d.y);
		            })
		            .attr("width",  barwidth)
		            .attr("x", function(d) {
		            	return x(parseDate(d.date)) 
		            })
		            //num_rect=rect[0].length
			}
			function brushend() {
				  if (d3.event.sourceEvent) { // not a programmatic event
					  extent=brush.extent()
					  doSearch();				 
				  }
			}
			brush = d3.svg.brush()//for slider bar at the bottom
			    .x(x2) 
			    .on("brush", brushed)
			    .on("brushend",brushend)


			y2_max=d3.max(recordcnt.vals, function(d) {
				 return d.count;});
			y2_min=d3.min(recordcnt.vals, function(d) {
				 return d.count;});	
			y2_min=0; // y2_min-10;
			y2_max=y2_max+10;
			var xaxis2=context.append("g").attr("class", "x axis");
			var yaxis2=context.append("g").attr("class", "y axis");
			x2.domain([date2_min,date2_max]);								
			y2.domain([y2_max,y2_min]);

			xaxis2.attr("transform",
					"translate(0," + height2 + ")").call(xAxis2).selectAll("text")  
		            .style("text-anchor", "end")
		            .attr("dx", "-.8em")
		            .attr("dy", ".15em")
		            .attr("transform", function(d) {
		                return "rotate(-65)" 
		            });
			var yaxis2_svg=context.append("g").attr("class", "y axis");
			yaxis2_svg.call(yAxis2);
			 


			// Context
			  zoombar=context.append("rect")
              .attr("class", "zoombar")
              .attr("x", 0)
              .attr("y", -20)
              .attr("width", width2)
              .attr("height",20)
              .style("fill","grey")
			  	.style("opacity",0.5);
			  
			  context.append("text")
			  .text("Zoom")
              .attr("x", 3)
              .attr("y", -2)
			  .style("font-color","black")
			  .style("opacity","0.7")
			  .style("font-size","10px");

			  zoomMin=new Date(parseInt(dataToRender.tdata.tRange.zoomMin));
			  zoomMax=new Date(parseInt(dataToRender.tdata.tRange.zoomMax));
			  extent=[zoomMin,zoomMax];
			  
/*			  zrange=[];
			  if (extent!=null) {
				  zrange=extent
			  }
*/
			  var br=context.append("g")
		     .attr("class", "x brush")
		     .call(brush.extent(extent))
		     .selectAll("rect")
		     .attr("y", -18)
		     .attr("height", height2+10);


			// ----------------------------------------------------
			
			data=recordcnt.vals
			data.sort(function(a, b) {return a.date - b.date;});
			
		    var tooltipDate = d3.time.format("%d/%m/%y");

		    var bisectDate = d3.bisector(function(d) {
		        return d.date;
		    }).left;
		    var countformat = d3.format("0,000");
		    var dateformat2 = d3.time.format("%m-%d-%Y");

	       var valueline = d3.svg.line()
            .interpolate("step-after")
            .x(function(d) {
                return x2(d.date);
            })
            .y(function(d) {
                return y2(d.count);
            });

			
			
	       	context.append("path")
	       	.datum(data)
	       	.attr("class", "area")
	       	.attr("d", area2);
			
            // Add the valueline path.
            context.append("path")
               .attr("class", "line")
                .attr("d", valueline(data));
			 			 
            // Tooltip
            f1 = context.append("g")
                .attr("class", "f1")
                .style("display", "none");

            f1.append("circle")
                .attr("r", 4.5);

            f1.append("rect")
            f1.append("text")
                .attr("x", 9)
                .attr("dy", ".35em");

            context.append("rect")
                .attr("class", "overlay")
                .attr("width", width2)
                .attr("height", height2)
                .on("mouseover", function() {
                    f1.style("display", null);
                })
                .on("mouseout", function() {
                    f1.style("display", "none");
                })
                .on("mousemove", mousemove1);

			 var xaxis_label_xoffset=xaxis2.node().getBoundingClientRect().width/2;
			 var xaxis_label_yoffset=xaxis2.node().getBoundingClientRect().height+6;
      
	         xTitle=context_xaxis_label
	         var xaxis_label=context.append("text")
				.attr("y", height2+xaxis_label_yoffset)
				.attr("x", xaxis_label_xoffset)
				.attr("font-size", "12px")
				.style({"text-anchor":"middle","font-weight": "bold"})
				.text(xTitle);

      
	         tTitle="Active Recipient Totals"
	         var xaxis_label=context.append("text")
				.attr("y", -25)
				.attr("x", xaxis_label_xoffset)
				.attr("font-size", "18px")
				.style({"text-anchor":"middle","font-weight": "bold"})
				.text(tTitle);
       
				
				var yaxis2_label_yoffset=-yaxis2_svg.node().getBoundingClientRect().width-20;
				yLabelText=context_yaxis_label;
				var ylabel=yaxis2_svg.append("text")
				.attr("transform", "rotate(-90)")
				.attr("y", yaxis2_label_yoffset)
				.attr("x", 0)
				.attr("font-size", "12px")
				.style({"text-anchor":"end","font-weight": "bold"})
				.text(yLabelText);
         
				 var xaxis2_label_xoffset=xaxis2.node().getBoundingClientRect().width/2;
				 var xaxis2_label_yoffset=xaxis2.node().getBoundingClientRect().height;

				d_min=getDateFromEpoch(parseInt(DEFAULT_validStartDate))
				d_max=getDateFromEpoch(parseInt(DEFAULT_validEndDate))
				
				min_date_str=getDateString(d_min);
				max_date_str=getDateString(d_max);
					
				 var min_date_label=context.append("text")
					.attr("y", height2 + xaxis2_label_yoffset+20)
					.attr("x", 0)
					.attr("dy", "1px")
					.style({"text-anchor":"start"})
					.html(min_date_str);
				 
				 				 
				 var max_date_label=context.append("text")
					.attr("y", height2 + xaxis2_label_yoffset+20)
					.attr("x", width2)
					.attr("dy", "1px")
					.style({"text-anchor":"end"})
					.html(max_date_str);
				
          //Get the Amount of alerts for tooltip

             // End Tooltip
			 // End Context ---------------------
  	
		}
        SearchEvents.toggleResultsLoading(false, $totalResults);
    	
    	
    }
 
	function mousemove2() {
        var m=d3.mouse(this);
        	d=d3.select(this).datum();
        	txt=f2.select("text");
            rect=f2.select("rect");
            dd=new Date(+d.date);
	        	dp=d3.select(this.parentNode).datum();
	        	dvar=dp.name;
            nlabel=dvar+': '+countformat2(Math.floor(d.count)) + '   ('+dateformat3(dd)+')'
            txt.text(nlabel);
            
            rbox=this.getBBox();
            tbox=txt.node().getBBox();
            
            dx=rbox.x+rbox.width/2
            dy=rbox.y
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


    function renderIfVisible() {
		// Render only if data is available
    	if (dataToRender && $timeline.is(':visible')) {
	        // Turn on animation
    		SearchEvents.toggleResultsLoading(false, $totalResults);


	        $focusEl.empty();
	        $legendEl.empty();     
			
			// Calcluate Margin
			var margin = {top : 50,right : 200,bottom : 250,left : 120}
			
			var width = $showPanel.width() - margin.left - margin.right
			if (width<400)
				width=400
			var offset = $contextEl.height()
			var height = $showPanel.height() - margin.top - margin.bottom-offset;
			if (height < 600) {
				height=600;
			}

			var margin2 = {top: 60, right: 10, bottom: 100, left: 120};
			width2=width-margin2.left-margin2.right;
			height2=height-margin2.top-margin2.bottom;
			
			focus_div=d3.select('#timeline-focus')
			
			
			focus_svg = focus_div.append('svg')
			.attr("id","focus_svg")
		    .attr("width", width)
		    .attr("height", height);

			
			// Setup Scales
			x = d3.time.scale().range([ 0, width2 ]);
			y = d3.scale.linear().range([0,height2 ]);
			z = d3.scale.ordinal().range(["darkblue", "blue", "lightblue"]);

			// Axis definition
			xAxis = d3.svg.axis().scale(x).orient("bottom");
			yAxis = d3.svg.axis().scale(y).orient("left");
	
			// Get tag value detail data
			layers=dataToRender.tdata.dataLayers;
 			
			// insert line breaks for long words
			var insertLinebreaks = function (d) {
			    var el = d3.select(this);
			    var words = d.split(' ');
			    el.text('');

			    for (var i = 0; i < words.length; i++) {
			        var tspan = el.append('tspan').text(words[i]);
			        if (i > 0)
			            tspan.attr('x', 0).attr('dy', '15');
			    }
			};
			
			
			// Calculate date span for detail view
			date_min=d3.min(layers, function(l) {
				return d3.min(l.vals,function(d) {
					return getDateFromEpochMS(parseInt(d.date));
				})
			});			
			if (date_min==null) {
				date_min=date2_min;
			}
			min_date_str=getDateString(date_min);
			

			date_max=d3.max(layers, function(l) {
				return d3.max(l.vals,function(d) {
					return dateAdd(getDateFromEpochMS(parseInt(d.date)),selectedInterval,1);
				})
			});	
			if (date_max==null) {
				date_max=date2_max;
			} 
			max_date_str=getDateString(date_max);

			barwidth=width2/getSegments(date_min,date_max,selectedInterval);

			
	
			
			// Calculate Stacked Graph Values
			var stack = d3.layout.stack()
			.offset("zero")
		    .values(function(d) { return d.vals; })
			.x(function(d) {return (d.date);})
			.y(function(d) {return d.count;})

			// Calculate Layers to draw
			var drawlayers=[];
			
			if (!(activeField in initShowVals) || (initShowVals[activeField])) {
				// First time, set ShowVals hash based on data returned
				for (j=0;j<layers.length;j++) {
					if (!(layers[j].varName in showValues)) { 
						showValues[layers[j].varName]={}
					}
					if ((layers[j].vals!=null) && (layers[j].vals.length>0)) {
						drawlayers.push(layers[j]);
						showValues[layers[j].varName][layers[j].name]="true";
					}
					else {
						showValues[layers[j].varName][layers[j].name]="false";
					}
					sortType[activeField]=DEFAULT_SORT_TYPE;
					sortOrder[activeField]=DEFAULT_SORT_ORDER;
				}
				initShowVals[activeField]=false;
			}
			else {
				// After that, use ShowVals hash to decide what data to draw 
				for (j=0;j<layers.length;j++) {
					if ((layers[j].vals!=null) && (layers[j].vals.length>0) && 
							(layers[j].varName in showValues) &&
							((layers[j].name in showValues[layers[j].varName]) &&  showValues[layers[j].varName][layers[j].name]=="true")) {
						drawlayers.push(layers[j]);
					}
				}	
			}
			
			
			var st=null;
    		 
 			graphType=$('#timeline-charttype').attr("value")
			
 			focus = focus_svg.append("g")
		    .attr("class", "focus")
		    .attr("transform", "translate(" + margin.left + "," + margin.top + ")");
					
			focus_chart = focus.append("g")
		    .attr("class", "focus_chart")
		    // .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

 			// Calculate Y range for Graph
 			y_max=y2_max;
            y_min=0
 			if (graphType=='Bar Chart') {
 				if (drawlayers.length!=0) {
 					st=stack(drawlayers);
 			
	 				y_max=d3.max(st[st.length-1].vals, function(d) {
						return d.y0+d.y;});
				    drawgroup = focus_chart.selectAll("g.drawgroup")
		            .data(drawlayers)
		            .enter().append("g")
		            .attr("class", "drawgroup")
		            .style("fill", function(d, i) { 
		            	return color(d.order); 
		            })
		            .style("stroke", function(d, i) { 
		            		if (graphType=='Bar Chart') {
		            			return (barwidth>4) ? d3.rgb(color(d.order)).darker() : color(d.order);
		            		} else if (graphType=='Line Chart') {
		            			return color(d.order);
		            		}
		            		return 'steel-blue';
		            })
			        .attr("data-label",function (d) {
			            	return d.varVal;
			         })
 				}
 				focus_title=focus_title_bar;
           } else if (graphType=='Line Chart') {
       			y_max=d3.max(drawlayers, 
       					function(l) {
       						return d3.max(l.vals, 
       								function(d) {
       									return d.count
       								})
       					});
			    drawgroup = focus_chart.selectAll("g.drawgroup")
		            .data(drawlayers)
		            .enter().append("g")
		            .attr("class", "drawgroup")
		            .style("fill", function(d, i) { 
		            	return color(d.order); 
		            })
		            .style("stroke", function(d, i) { 
		            		if (graphType=='Bar Chart') {
		            			return (barwidth>4) ? d3.rgb(color(d.order)).darker() : color(d.order);
		            		} else if (graphType=='Line Chart') {
		            			return color(d.order);
		            		}
		            		return 'steel-blue';
		            })
			        .attr("data-label",function (d) {
			            	return d.varVal;
			         })
					focus_title=focus_title_comparison;
           } else if (graphType=='Daily Change') {
        	   y_min=0;
        	   y_max=0;
        	   if (drawlayers.length!=0) {
 					addlayers=[]
 					remlayers=[]
 					for (var i = 0; i < drawlayers.length; i++)  {
 						if (drawlayers[i].type=='ADD') {
 								addlayers.push(drawlayers[i]);
 						}
 						else if (drawlayers[i].type=='REMOVE') {
 								remlayers.push(drawlayers[i]);								
 						}
 					}	
 					if (drawlayers.length!=0) {
 					  	st=stack(drawlayers);
 		 				y_max=d3.max(st[st.length-1].vals, function(d) {
 							return d.y0+d.y;});
 					}
 					if (addlayers.length!=0) {
 					  	addst=stack(addlayers);
		 					
					    drawgroupA = focus_chart.selectAll("g.drawgroupA")
	 		            .data(addlayers)
	 		            .enter().append("g")
	 		            .attr("class", "drawgroupA")
	 		            .style("fill", function(d, i) { 
	 		            	return colorAdd(d.order); 
	 		            })
	 		            .style("stroke", function(d, i) { 
	 		            	return (barwidth>4) ? d3.rgb(colorAdd(d.order)).darker() : colorAdd(d.order);
	 		            })
	 			        .attr("data-label",function (d) {
	 			            	return d.varVal;
	 			         })
		 	      		y_max=d3.max(addst, 
		 	      				function(l) {
		 	       					return d3.max(l.vals, 
		 	       							function(d) {
		 	       								return d.y+d.y0
		 	       					})
		 	       				});
					}
 					if (remlayers.length!=0) {
 						remst=stack(remlayers);
					    drawgroupR = focus_chart.selectAll("g.drawgroupR")
	 		            .data(remlayers)
	 		            .enter().append("g")
	 		            .attr("class", "drawgroupR")
	 		            .style("fill", function(d, i) { 
	 		            	return colorRemove(d.order); 
	 		            })
	 		            .style("stroke", function(d, i) { 
	 		            			return (barwidth>4) ? d3.rgb(colorRemove(d.order)).darker() : colorRemove(d.order);
	 		            })
	 			        .attr("data-label",function (d) {
	 			            	return d.varVal;
	 			         })
		 	      		y_min=-d3.max(remst, 
		 	      				function(l) {
		 	       					return d3.max(l.vals, 
		 	       							function(d) {
		 	       								return d.y+d.y0
		 	       					})
		 	       				});
					} 
 				}
 				focus_title=focus_title_delta;
             }
 			 
             y_max=y_max+y_max*0.10
 			 y_min=y_min+y_min*0.10
   			


  			
  			
 			x.domain([date_min,date_max]);
            y.domain([y_max,y_min]); 	           	


			var xaxis=focus_chart.append("g").attr("class", "x axis");
			
			xaxis.attr("transform",
					"translate(0," + height2 + ")").call(xAxis).selectAll("text")  
		            .style("text-anchor", "end")
		            .attr("dx", "-.8em")
		            .attr("dy", ".15em")
		            .attr("transform", function(d) {
		                return "rotate(-65)" 
		                });
			
	         var selectedSearchView = SearchEvents.getSelectedSearchView();
//	         var viewFilterFields = FilterFields[selectedSearchView];
//	         var vmeta = viewFilterFields[activeField];
//	         var tlabel = SearchSettings.getResultViewText()  // +'(s) by '+ vmeta.displayName;
/*	     	var context_title="Manage Care Population Trending"
	     	    var context_yaxis_label="Active Recipients"
	     	    var context_xaxis_label="Date"
	     	    var focus_title="Active Recipents by"
	     	    var focus_yaxis_label="Active Recipients"
*/
	         
	         var fieldTitle="";
	         if (selectedSearchView in FilterFields) {
		         if (activeField==null || !(activeField in FilterFields[selectedSearchView])) {
		        	 activeField=Object.keys(FilterFields[selectedSearchView])[0]
		         } 
	        	 fieldTitle=FilterFields[selectedSearchView][activeField].displayName;
	         }
	         var tlabel=focus_title+" "+fieldTitle;
	         
			 var title_label_xoffset=xaxis.node().getBoundingClientRect().width/2;
			 var title=focus_chart.append("text")
			.attr("y", "-25px")
			.attr("x", title_label_xoffset)
			.attr("font-size", "18px")
			.style({"text-anchor":"middle","font-weight": "bold"})
			.text(tlabel);
			 

			 var xaxis_label_xoffset=xaxis.node().getBoundingClientRect().width/2;
			 var xaxis_label_yoffset=xaxis.node().getBoundingClientRect().height;

			 var min_date_label=focus.append("text")
				.attr("y", height2 + xaxis_label_yoffset+20)
				.attr("x", 0)
				.attr("dy", "1px")
				.style({"text-anchor":"start"})
				.html(min_date_str);
			 
			 
			 min_date_label.selectAll('g text').each(insertLinebreaks);	
			 
			 var max_date_label=focus_chart.append("text")
				.attr("y", height2 + xaxis_label_yoffset+20)
				.attr("x", width2)
				.attr("dy", "1px")
				.style({"text-anchor":"end"})
				.html(max_date_str);
			
			max_date_label.selectAll('g text').each(insertLinebreaks);	

			
			
	         var view = SearchEvents.getSelectedSearchView();
	         
	         xTitle=focus_xaxis_label
			 var xaxis_label=focus_chart.append("text")
			.attr("y", height2+xaxis_label_yoffset)
			.attr("x", title_label_xoffset)
			.attr("dy", "16px")
			.style({"text-anchor":"middle","font-weight": "bold"})
			.text(xTitle);

			 
			 
			var yaxis_svg=focus_chart.append("g").attr("class", "y axis");
			yaxis_svg.call(yAxis);
			
			var yaxis_label_yoffset=-yaxis_svg.node().getBoundingClientRect().width-20;
			 
			yLabelText=focus_yaxis_label;
			var ylabel=yaxis_svg.append("text")
			.attr("transform", "rotate(-90)")
			.attr("y", yaxis_label_yoffset)
			.attr("x", 0)
			.attr("font-size", "12px")
			.style({"text-anchor":"end","font-weight": "bold"})
			.text(yLabelText);
   	    	

			
			// Draw Grid Lines
			if ($('#timeline-grids').is(':checked') ) {
				function make_x_axis(x, tx) {
					return d3.svg.axis().scale(x).orient("bottom").ticks(tx)
				}
	
				function make_y_axis(y, ty) {
					return d3.svg.axis().scale(y).orient("left").ticks(ty)
				}
				var txv = $('#timeline-grid-x').val();
				focus.append("g").attr("class", "grid").attr("transform",
						"translate(0," + height2 + ")").call(
						make_x_axis(x, txv).tickSize(-height2, 0, 0).tickFormat(""))
	
				var tyv = $('#timeline-grid-y').val();
				focus.append("g").attr("class", "grid").call(
						make_y_axis(y, tyv).tickSize(-width2, 0, 0).tickFormat(""))
			};
			
	/*		focus_svg.append("g")
		    .attr("class", "brush")
		    .call(d3.svg.brush().x(x).y(y)
		    .on("brushstart", brushstart)
		    .on("brush", brushmove)
		    .on("brushend", brushend));

			function brushstart() {
				focus_svg.classed("selecting", true);
			}
	
			function brushmove() {
			  var e = d3.event.target.extent();
			}
	
			function brushend() {
				focus_svg.classed("selecting", !d3.event.target.empty());
			}	
			
*/			
			if (layers.length!=0) {
			 
		        // Add a group for each row of the stack.
	            valgroup = focus_chart.selectAll("g.valgroup")
	            .data(layers)
	            .enter().append("g")
	            .attr("class", "valgroup")
	            .style("fill", function(d, i) { 
	            	return color(i); 
	            })
	            .style("stroke", function(d, i) { 
	            	return (barwidth>4) ? d3.rgb(color(i)).darker() : color(i) })
	            .attr("data-label",function (d) {
	            	return d.varVal;
	            })
	            .attr("order",function (d) {
	            	return i;
	            })
	         
	            
	            // sort by pos or by alpha
	        	slayers = layers.sort(function(a,b) { 
        			if (sortType[activeField]=='Value') {
        				if (sortOrder[activeField]=='Descending') {
    	        			return a.order-b.order        					
        				} else {
    	        			return b.order-a.order        						        					
        				}
        			} else {
           				var x = a.name.toLowerCase(), y = b.name.toLowerCase();
        				if (sortOrder[activeField]=='Descending') {
    	        			return x < y ? -1 : x > y ? 1 : 0;		
        				} else {
     	        			return y < x ? -1 : y > x ? 1 : 0;		
         				}	        				
         			}
	        		return 0;
	        	});
	           
	            var legendy=height+xaxis.node().getBoundingClientRect().height+xaxis_label.node().getBoundingClientRect().height+40+20;
	            var legendheight=legendy-height
	           
   	            
	            ldivarea=d3.select('#timeline-legendarea')

	            ldiv=d3.select('#timeline-legend')
	            
	            lsgv=ldiv.append('svg').attr('id','timeline-legendvallist');

	            
	            legenditems=lsgv.selectAll("g.legend-item")
	    	    .data(slayers)
	            .enter().append("g")
	            .attr("class","legend-item")
		            
 	            selectedTotal=0;
	            for(var k in showValues[activeField])
	            {
      	        	if (showValues[activeField][k]=="true") {
      	        		selectedTotal+=1;
      	        	}
	            }
				lTitle=fieldTitle+" Values ("+selectedTotal+" sel.)";
				 var ltitle_label=d3.select('#timeline-legendtitle')
				.text(lTitle);
				 
	        	text_height_max=20
	        	text_width_max=240
				 
				var $el = $('#sortTypeSpan');
	        	if (sortType[activeField]=='Value') {
	        		$el.html('Value');
				} else if (sortType[activeField]=='Name') {
	        		$el.html('Name');	      	    	
	      	    }
			
	    		var $el = $('#sortOrderSpan');
	        	if (sortOrder[activeField]=='Descending') {
				    $el.removeClass('up-caret');			 
	        	} else if (sortOrder[activeField]=='Descending') {
				    $el.addClass('up-caret');			 
	      	    }
    	
				textboxes=lsgv.selectAll("g.legend-item")
	    	        .append("text")
	    	        .attr("x",function(d,i) { 
	     	        	return (2*text_height_max);
	     	        })
	     	        .attr("y",function(d,i) { 
		        		if (d.type=='ADD') { 
		        			row=Math.floor(i/2)+1; 
		        		}
		        		else if (d.type=='REMOVE') {
		        			row=Math.floor(i/2)+1; 
		        		}
		        		else {
		        			row=i+1; 
		        		}
	     	        	return (row*text_height_max);
	     	        })
	    	        .text(function(d) {
	    	        	return d.name;
	    	        })
	    	        .style("fill",'black');

	      	    
	         	    
				 rects=lsgv.selectAll("g.legend-item")
	    	        .append("rect")
        	        .attr("x",function(d,i) { 
        	        		coffset=0;
         	        		if (d.type=='ADD') { 
         	        			coffset=0; 
        	        		}
        	        		else if (d.type=='REMOVE') {
        	        			coffset=text_height_max
        	        		}
         	        		
        	        		return (coffset);
        	        })
        	        .attr("y",function(d,i) { 
    	        		if (d.type=='ADD') { 
    	        			row=Math.floor(i/2)+1; 
    	        		}
    	        		else if (d.type=='REMOVE') {
    	        			row=Math.floor(i/2)+1; 
    	        		}
    	        		else {
    	        			row=i+1; 
    	        		} 	
        	        	return (row*text_height_max-text_height_max+4);
        	        })
        	        .attr("width",text_height_max-2)
        	        .attr("height",text_height_max-2)
        	        .style("fill",function(d,i) {
        	        	if ((d.varName in showValues) && (d.name in showValues[d.varName]) && (showValues[d.varName][d.name]=="true")) {
        	        		if (d.type=='ADD') { 
        	        			return colorAdd(Math.floor(d.order));
        	        		}
        	        		else if (d.type=='REMOVE') {
        	        			return colorRemove(Math.floor(d.order));
        	        		}
        	        		else {
        	        			return color(d.order);
        	        		}
        	        		
        	        	}
        	        	return 'white';
        	        }) 
        	        .style("stroke","black")
        	        .style("stroke-width","1")
        	        .on("click", function(d) {
        	        	if ((d.varName in showValues) && (d.name in showValues[d.varName])) {
        	        		showValues[d.varName][d.name]=(showValues[d.varName][d.name]=="true") ? "false" : "true";
	        	        	// console.log(showValues[d.varName][d.name]);
        	        	}
        	        	d3.event.stopPropagation();
  					  	doSearch();				 
        	        });    
           	
	          
				 	// Resize list svg to fit all items
				 	// Resize list svg to fit all items
				 	
				 	// Resize list svg to fit all items				 	
					maxlengendY=d3.max(textboxes[0], function(d){ 
				 		return d.clientHeight+5; 
				 	});
					if (maxlengendY==null || maxlengendY<10) {
						maxlengendY=10	
					}
					maxlengendX=d3.max(textboxes[0], function(d){ 
				 		return d.clientWidth+5; 
				 	});
				 	svgwidth=3*maxlengendY+maxlengendX
				 	if (svgwidth<240) {
				 		svgwidth=240
				 	}
				 	svgheigth=textboxes[0].length*maxlengendY+40
					
				 	lsgv.style('width',svgwidth+'px')
				 	lsgv.style('height',svgheigth+'px')

					
					
					var maketip = function (d) {	
	            	dd=new Date(+d.date);
	            	var tip = '<p class="tip3">' + d.name + '<p class="tip1">' + countformat2(d.count) + '</p> <p class="tip3">'+  dateformat3(dd)+'</p>';
	                return tip;
	            }

	            
	            
	            if (graphType=='Line Chart' ) {
	
	    		    var tooltipDate = d3.time.format("%d/%m/%y");

	    		    var bisectDate = d3.bisector(function(d) {
	    		        return d.date;
	    		    }).left;
	    		    var countformat = d3.format("0,000");
	    		    var dateformat2 = d3.time.format("%m-%d-%Y");

	    		    function mousemove3() {
	    		        var m=d3.mouse(this);
	    		        var x0 = x.invert(d3.mouse(this)[0]),
			            dl =d3.select(this).datum();
			            i = bisectDate(dl, x0, 1),
			            d0 = dl[i - 1],
			            d1 = dl[i],
			            d = x0 - d0.date > d1.date - x0 ? d1 : d0;
			            txt=f3.select("text");
			            rect=f3.select("rect");
			            dd=new Date(+d.date);

		   	        	dp=d3.select(this.parentNode).datum();
		   	        	dvar=dp.name;
    		            nlabel=dvar+': '+countformat2(Math.floor(d.count)) + '   ('+dateformat3(dd)+')'

			            txt.text(nlabel);
				        tbox=txt.node().getBBox();
			            dx=x(d.date)
			            dy=y(d.count)-9
			           
			            tx=0
			            if (dx+tbox.width>width2) {
			            	tx=-tbox.width
			            }
			            ty=-tbox.height
			            ty2=ty-5
			            f3.attr("transform", "translate(" + dx + "," + dy + ")");
			            rect.attr("transform", "translate(" + tx + "," + ty2 + ")")
			    		.attr("width", tbox.width+20)
			    		.attr("height", tbox.height+6)
			    		.attr("fill", "rgba(255,255,255,.90)")
			    		txt.attr("transform", "translate(" + tx + "," + ty + ")");
	    		   }
	    		   
	    		   for (var i in drawlayers) {
	    				// Exnted last value
	    				if (drawlayers[i].val!=null) {
		    				var lastval=drawlayers[i].vals[drawlayers[i].vals.length-1]
		    				var newmaxdate= jsDateToEpoch(date2_max)
		    				var newlastval={count: lastval.count, date: newmaxdate}
		    				drawlayers[i].vals.push(newlastval)
	    				}
	    			}	
	            	            	
	            	var line = d3.svg.line()
		            .interpolate("step-after")
		            .x(function(d) { return x(d.date); })
		            .y(function(d) { return y(d.count); })
		   	     
		            
		            drawgroup.append("path")
		            .datum(function(ls) {
		            	// Find relevant data in layers
		            	return ls.vals;
		             })  
		            .attr("class", "line")
		   	        .style("stroke-width","3px")
		   	        .style("stroke", function(d) { 
		   	        	dp=d3.select(this.parentNode).datum() 
		   	        	return color(dp.order);
		   	        })
		   	        .attr("d", line)
		   	        .on("mouseover", function (d) {                                  
			      		d3.select(this)                          //on mouseover of each line, give it a nice thick stroke
			        	.style("stroke-width",'10px');
		            	f3.style("display", null);     	
			    	})
		   	        .on("mouseout",	function(d) {        //undo everything on the mouseout
		   	        		d3.select(this)
		   	        		.style("stroke-width",'3px');
			            	f3.style("display", "none");
		   	        })
		            .on("mousemove", mousemove3);

		            // Tooltip
		            f3 = focus.append("g")
		                .attr("class", "f3")
		                .style("display", "none");

		            f3.append("circle")
		                .attr("r", 4.5);

		            f3.append("rect")
		            f3.append("text")
		                .attr("x", 9)
		                .attr("dy", ".35em");
            	
	            	
	 			}
	            else if (graphType=='Bar Chart') {
	            	

	            	
	            	
	            	            	
	            	
		            // Add a rect for each date.
		            var rect = drawgroup.selectAll("rect")
		            .data(function(ls) {
		            	return ls.vals;
		            })
		            .enter().append("svg:rect")
		            .attr("x", function(d) {
		            	return x(d.date); 
		            })
		            .attr("y", function(d) { 
		//            	return  height;
		            	return y(d.y0 + d.y);
		            })
		            .attr("height", function(d) { 
		//            	return  0; 
		                return y(d.y0) - y(d.y0 + d.y);
		            })
		            .attr("width", function(d,i) {
		            	w=barwidth;
		            	dp=d3.select(this.parentNode).datum();
		            	nd=dp.vals[i+1];
		            	if (nd!=null) {
		            		w=x(nd.date)-x(d.date)
		            	}
		            	// w=x(data[i])-x(d.date);
		            	return w;
		            
		            })
		            .on("mouseover", function() {
		            	f2.style("display", null);
		            })
		            .on("mouseout", function() {
		            	f2.style("display", "none");
		            })
		            .on("mousemove", mousemove2)
		            .on("click", function (d,i) {
		                var $el = $(this);
		                // {"AS_OF_DATE":[{"type":"I","op":"BETWEEN","val":"13/07/2015,14/07/2015"}
		                
		    		    var dateformat5 = d3.time.format("%d/%m/%Y");
	                
		                var edate = d.date;
		                var jdate1=new Date(+edate);
		            	var sdate1=dateformat5(jdate1);
		            	var jdate2=dateAdd(jdate1,'Day',1);
		            	var sdate2=dateformat5(jdate2)
		                var dvarname='AS_OF_DATE'
		                var dval=sdate1+','+sdate2
		                dp=d3.select(this.parentNode).datum();
		            	var varName =dp.varName;
		                var varVal=dp.name;

		                
		                var paraFilters = {};
		                paraFilters[varName] = [{type: INDEXED_FIELDTYPE, val: varVal}];
		                paraFilters[dvarname] = [{type: PARAMETRIC_FIELDTYPE, op: "BETWEEN",val: dval}];
		             

		                $('#parametricForm').data('filterForm').insertLoadingFilterSet()
		                    .loadFilters({
		                        boolOperator: 'AND',
		                        filterFields: paraFilters,
		                        childGroups: null,
		                        tag: ''
		                    }, null, null, true);
		                doSearch();
		            })
		        

		            // Tooltip
		            f2 = focus.append("g")
		                .attr("class", "f2")
		                .style("display", "none");

		            f2.append("circle")
		                .attr("r", 4.5);

		            f2.append("rect")
		            f2.append("text")
		                .attr("x", 9)
		                .attr("dy", ".35em");

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
	            else if (graphType=='Daily Change') {
		            // Add a rect for each date.
		            if (drawgroupA!=null) {
		            	var rect = drawgroupA.selectAll("rect")
			            .data(function(ls) {
			            	return ls.vals;
			            })
			            .enter().append("svg:rect")
			            .attr("x", function(d) {
			            	return x(d.date); 
			            })
			            .attr("y", function(d) { 
			//            	return  height;
			            	return y(d.y0 + d.y);
			            })
			            .attr("height", function(d) { 
			//            	return  0; 
			                return y(d.y0) - y(d.y0 + d.y);
			            })
			            .attr("width", function(d,i) {
			            	w=barwidth;
			            	//dp=d3.select(this.parentNode).datum();
			            	//nd=dp.vals[i+1];
			            	//	w=x(nd.date)-x(d.date)
			            	//}
			            	// w=x(data[i])-x(d.date);
			            	return w;
			            
			            })
			            .on("mouseover", function() {
			            	f2.style("display", null);
			            })
			            .on("mouseout", function() {
			            	f2.style("display", "none");
			            })
			            .on("mousemove", mousemove2)
			            .on("click", function (d,i) {
			                var $el = $(this);
			                // {"AS_OF_DATE":[{"type":"I","op":"BETWEEN","val":"13/07/2015,14/07/2015"}
			                
			    		    var dateformat5 = d3.time.format("%d/%m/%Y");
		                
			                var edate = d.date;
			                var jdate1=new Date(+edate);
			            	var sdate1=dateformat5(jdate1);
			            	var jdate2=dateAdd(jdate1,'Day',1);
			            	var sdate2=dateformat5(jdate2)
			                var dvarname='AS_OF_DATE'
			                var dval=sdate1+','+sdate2
			                dp=d3.select(this.parentNode).datum();
			            	var varName =dp.varName;
			                var varVal=dp.name;
	
			                
			                var paraFilters = {};
			                paraFilters[varName] = [{type: INDEXED_FIELDTYPE, val: varVal}];
			                paraFilters[dvarname] = [{type: PARAMETRIC_FIELDTYPE, op: "BETWEEN",val: dval}];
			             
	
			                $('#parametricForm').data('filterForm').insertLoadingFilterSet()
			                    .loadFilters({
			                        boolOperator: 'AND',
			                        filterFields: paraFilters,
			                        childGroups: null,
			                        tag: ''
			                    }, null, null, true);
			                doSearch();
			            })
		            }
		            if (drawgroupR!=null) {
		            	
		            
			            var rect = drawgroupR.selectAll("rect")
			            .data(function(ls) {
			            	return ls.vals;
			            })
			            .enter().append("svg:rect")
			            .attr("x", function(d) {
			            	return x(d.date); 
			            })
			            .attr("y", function(d) { 
			//            	return  height;
			            	return  2*y(0)-y(d.y0);
			            })
			            .attr("height", function(d) { 
			//            	return  0; 
			                return y(d.y0) - y(d.y0 + d.y);
			            })
			            .attr("width", function(d,i) {
			            	w=barwidth;
			            	//dp=d3.select(this.parentNode).datum();
			            	//nd=dp.vals[i+1];
			            	//	w=x(nd.date)-x(d.date)
			            	//}
			            	// w=x(data[i])-x(d.date);
			            	return w;
			            
			            })
			            .on("mouseover", function() {
			            	f2.style("display", null);
			            })
			            .on("mouseout", function() {
			            	f2.style("display", "none");
			            })
			            .on("mousemove", mousemove2)
			            .on("click", function (d,i) {
			                var $el = $(this);
			                // {"AS_OF_DATE":[{"type":"I","op":"BETWEEN","val":"13/07/2015,14/07/2015"}
			                
			    		    var dateformat5 = d3.time.format("%d/%m/%Y");
		                
			                var edate = d.date;
			                var jdate1=new Date(+edate);
			            	var sdate1=dateformat5(jdate1);
			            	var jdate2=dateAdd(jdate1,'Day',1);
			            	var sdate2=dateformat5(jdate2)
			                var dvarname='AS_OF_DATE'
			                var dval=sdate1+','+sdate2
			                dp=d3.select(this.parentNode).datum();
			            	var varName =dp.varName;
			                var varVal=dp.name;
	
			                
			                var paraFilters = {};
			                paraFilters[varName] = [{type: INDEXED_FIELDTYPE, val: varVal}];
			                paraFilters[dvarname] = [{type: PARAMETRIC_FIELDTYPE, op: "BETWEEN",val: dval}];
			             
	
			                $('#parametricForm').data('filterForm').insertLoadingFilterSet()
			                    .loadFilters({
			                        boolOperator: 'AND',
			                        filterFields: paraFilters,
			                        childGroups: null,
			                        tag: ''
			                    }, null, null, true);
			                doSearch();
			            })
		            
		            }
		            // Tooltip
		            f2 = focus.append("g")
		                .attr("class", "f2")
		                .style("display", "none")
	               		.style("background", "rgba(255,255,255,.90)");

		            f2.append("circle")
		                .attr("r", 4.5);

		            f2.append("rect")
		            f2.append("text")
		                .attr("x", 9)
		                .attr("dy", ".35em");

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
         
    		}
			SearchEvents.toggleResultsLoading(false, $totalResults);
			return;
			
		}
		var dragpoint;

		function timeline_click() {
			// Ignore the click event if it was suppressed
			if (d3.event.defaultPrevented)
				return;

			// Extract the click location\    
			var dragpoint = d3.mouse(this), p = {
				x : dragpoint[0],
				y : dragpoint[1]
			};

			// Append a new point
			focus.append("rectangle").attr("width", "10").attr("height",
					height).attr("transform",
					"translate(" + p.x + "," + margin.top + ")").attr("class",
					"box").style("cursor", "pointer").call(drag).style(
					"stroke", "#999999").style("fill", "#F6F6F6");

		}

		var drag = d3.behavior.drag().on("drag", dragmove);

		function dragmove(d) {
			var x = d3.event.x;
			var y = d3.event.y;
			d3.select(this).attr("transform", "translate(" + x + "," + y + ")");
		}

	}
	
	// ----------------------------------------------------------------

   
	 // Event Handlers
	 // --------------------------------------------------------------
	 
	
    $(".timeline-refresh").click(function(){
        doSearch();
    });

    
    $('.search-controls').on(SearchEvents.SEARCH_VIEW_CHANGED, function() {
    	activeField=null
        doSearch();
    });
    
    $('[href=#timeline]').on('shown', function() {
            doSearch();        
        return true;
    });
	
	SearchEvents.$.on(SearchEvents.SEARCH_REQUEST_SENT, function() {
    	if ($timeline.is(':visible')) {
			if (timeline) {
				timeline.remove();
			}
			dataToRender = timeline = undefined;
			lastSeach = currentSearch;
			currentSearch = null;
			previousXHR && previousXHR.abort();
			previousXHR = null;
	        extent=null;
    	}
    });

	
    
	
   
    
    SearchEvents.$.on(SearchEvents.RESULTS_PROCESSING, function(e, results, totalResults, data) {
	       lastTotalResults = _.reduce(totalResults, function(total, val){
	            return total + val;
	        }, 0);
	
	        currentSearch = data;
	
	        previousXHR && previousXHR.abort();
	        // extent=null;
	        
	        if ($timeline.is(':visible')) {
	            doSearch();
	        }
    });

/*    SearchEvents.$.on(SearchEvents.PARAMETRICS_LOADED, function(e, obj){
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
  */
    
    SearchEvents.$.on(SearchEvents.PARAMETRICS_LOADED, function(e, obj){
    	
    	
    	
	    	var data = obj.data;
				currentSearch = lastSearch = lastTotalResults = null;
				activeField='CAP_RATE_IND';
	      var selectedSearchView = SearchEvents.getSelectedSearchView();
	      var viewFilterFields = FilterFields[selectedSearchView];
	    	
	    	var paraFields = [];
	    	
	    	_.each(_.values(viewFilterFields), function(field) {
	    		if(field.popType) {
	    			paraFields.push(field.name);
	    		}
	    	}); 
	    	
	    	
	   // 	renderDateList();
	    	extent=null;
	   	
	    	var maxFieldListChars = 30;
	
	      var paraFieldItemTmpl = '<li <%- active %>><span data-toggle="tooltip" title="<%- label %>" class="filter-indicator"><%- displayLabel %><%= selectIndicator %></span></li>';
	      //var selectIndicatorTmpl = '<input data-toggle="tooltip" title="Field is used in the query" class="filter-indicator pull-right" type="radio" checked></input>';
	      
	    	fieldValuesData = _.map(paraFields, function(fieldname) {
	      	var fieldMeta = viewFilterFields[fieldname];
	      	var field = {name: fieldname};
	      	
	      	var label = fieldMeta.displayName;
	      	var defaultSelect = Boolean(viewFilterFields[field.name].popDefaultSelect);
	      	
	      	field.parentCategory = fieldMeta.parentCategory;
	      	field.parentGroup = fieldMeta.parentGroup;
	      	field.displayName = fieldMeta.displayName;
	      	field.displayLabel =  (label.length > maxFieldListChars) ? label.substring(0, maxFieldListChars) + "..." : label;
	      	
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
	      fieldListTpl = _.template($.resGet(TL_FILTERS_LIST_TEMPLATE));
	      fieldList = fieldListTpl({fields: mappedHeaderFields});
	      
	      $filtersEl.empty().append(fieldList);   
	      
	
	      $('input.filter-indicator').on('click', function() {
	          return false;
	      });

    });

   
});












