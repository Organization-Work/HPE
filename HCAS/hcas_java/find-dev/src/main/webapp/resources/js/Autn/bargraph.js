// raphael.js doesn't import correctly using require.js, see https://github.com/DmitryBaranovskiy/raphael/issues/524
define(['underscore', 'Autn/i18n', 'Autn/longpresshandler', 'jquery', 'jqueryui', 'jquerytouchpunch', 'jqueryhashchange', 'json2', 'd3'], function (_, i18n, LongPressHandler) {

    return function (chartEl, opts) {
        // Chrome is picky and thinks it doesn't have full SVG support, so testing for
        //    document.implementation.hasFeature("http://www.w3.org/TR/SVG11/feature#Shape", "1.0")
        // doesn't work.
        // http://stackoverflow.com/questions/654112/how-do-you-detect-support-for-vml-or-svg-in-a-browser
        if (!document.implementation.hasFeature("http://www.w3.org/TR/SVG11/feature#BasicStructure", "1.1")) {
            chartEl.text(i18n('idolview.svg.browser.required'));
            return;
        }

        opts = opts || {};

        var arcLabelFormatter = opts.arcLabelFormatter;
        var formatPercentage = opts.formatPercentage;
        var formatPercent = opts.formatPercent;
        var maximumFieldsAllowed = opts.maximumFieldsAllowed;
        var margin = {top: 10, right: 20, bottom: 20, left: 20};
        var $filterElement = $("#sunburst-filters");
        var w = window,
            d = document,
            e = d.documentElement;
        var windowx = w.innerWidth || e.clientWidth || d.clientWidth;
        var windowy = w.innerHeight || e.clientHeight || d.clientHeight;
        //console.log(chartEl.width());
        var width = windowx * 0.72 - 200 //windowx * 0.72 - 200 ,
            // magic number 40 corresponds to 25 bar height and 15 px space between rectangles
            height = (40 * maximumFieldsAllowed) + 100 ,
            barHeight = (40 * maximumFieldsAllowed) + margin.top + margin.bottom;


        // Lock the width and height after first render
        chartEl.width(width);
        chartEl.height(height);
        var sortField = opts.sortField;
        chartEl.append('<div class="tooltip"></div>');
        var sortedAlphabeticalData, sortedCountData;


        var resultsRange = d3.select(chartEl[0]).append('svg')
            .attr('width', width)
            .attr('height', 130).attr("class", "results-range");
        var minX = 100;
        resultsRange.attr("viewBox", minX +" 0 "+width+" "+110);
        resultsRange.attr("preserveAspectRatio","xMidYMid meet");
        var vis = d3.select(chartEl[0]).append('svg')
            .attr('width', width)
            .attr('height', height).attr("class", "histogram");
        vis.attr("viewBox","0 0 "+width+" "+height);
        vis.attr("preserveAspectRatio","xMidYMid meet");
      //  var scaleGroup = vis.append("g").attr("transform", "translate(300,0)");

        var textValueGrp = vis.append("g").attr("transform", "translate(0,40)");
        var filterTextGrp = vis.append("g").attr("transform", "translate(0,0)");
        var rectGroup = vis.append("g").attr("transform", "translate(300,40)");
        var animationTime = 1000;
        var textAppearanceTime = 1500;

        var onShortClick = opts.onShortClick;
        var onLongClick = opts.onLongClick;


        var colorFn = opts.colorFn || function (d, i) {
            return color(d[i]);
        };

        var longPressHandler = new LongPressHandler();
        var arcLabelTimeout;
        var prevClicked, prevHovered;
        var svgLayer = resultsRange.append("g").attr("transform", "translate(300,0)");

        this.resetFocus = function () {
            prevClicked = prevHovered = null;

        };

        this.redraw = redraw;


        // Legend is for total text and count ideally we would display a rectangle showing the particular color but here it is Total ${count} as per mockup
        var legend = resultsRange.append("g")
            .attr("class", "legend")
            .attr("x", width/2)
            .attr("y", 25)
            .attr("height", 100)
            .attr("width", 100);
        // This function is for updating with width and height when we resize
        function updateWindow() {
           windowx = w.innerWidth || e.clientWidth || d.clientWidth;
           windowy = w.innerHeight || e.clientHeight || d.clientHeight;
            // Magic number 0.72 pertains to 3/4 of the view port width.
            width = windowx * 0.72 - 200;
           // barHeight = windowy;
            vis.attr("width", width);
            resultsRange.attr("width", width);
            vis.attr("viewBox","0 0 "+width+" "+height);
            resultsRange.attr("viewBox","0 0 "+width+" "+110);
        }
        var numberFormat=function(num) {
            if(num) {
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
            }
        };

        // Need to uncomment this part out after upgrading to bootstrap 3.
            window.onresize = updateWindow;
        
         this.clearAll =  function() {
	        	 if (arcLabelTimeout) {
	                 clearTimeout(arcLabelTimeout);
	             }
                vis.selectAll("text").remove();
                resultsRange.selectAll(".layer").remove();
                resultsRange.selectAll("rect").remove();
                resultsRange.selectAll("text").remove();
                // resultsRange.selectAll("text").remove();
                rectGroup.selectAll("rect").remove();
                rectGroup.selectAll("text").remove();
                d3.selectAll("svg > text").remove();
                textValueGrp.selectAll("text").remove();
                filterTextGrp.selectAll("text").remove();
            };
            
        function redraw(json, retainZoom) {
            // Specifies the colors selected to specific to the array index value
            var color = d3.scale.ordinal().domain(d3.range(0,json.data.length)).range(["#42ccbe", "#a0e5de", "#ea9047", "#f6c498", "#5ab7e0", "#acdbef", "#9bda6a", "#cdecb4", "#7a9cfc", "#b8c7fe", "#ffd428", "#ffe993", "#fa726c", "#fcb8b5", "#84a2a5", "#c1d0d2", "#d392f4", "#e9c8f9", "#d9ac59", "#ecd5ac", "#a09bfc", "#cfcdfd", "#b7c11e", "#dbe08e", "#7a9cfc", "#b8c7fe",
                "#cc6a7d", "#e5b4be", "#47a1ea", "#a3d0f4", "#e0835a", "#efc1ac", "#a96ada", "#d4b4ec", "#a58784", "#d2c3c1", "#719cbc", "#b8cddd", "#81b273", "#c0d8b9"
            ]);
            longPressHandler.cancel();
            // maximumRecord returns the maximum value in given array
            var maximumRecord = d3.max(json.data, function (d) {
                return d.size;
            });
           // width = windowx * 0.72 - ($filterElement.is(":visible")?($filterElement.width() - 30) : 140);
            // Total encounter variable
            var totalEncounter = json.totalCount;
            barHeight = json.data.length * 40;
            vis.select("svg").attr("height", barHeight);
            vis.selectAll("text").remove();
            resultsRange.selectAll(".layer").remove();
            resultsRange.selectAll("rect").remove();
            resultsRange.selectAll("text").remove();
            // resultsRange.selectAll("text").remove();
            rectGroup.selectAll("rect").remove();
            rectGroup.selectAll("text").remove();
            d3.selectAll("svg > text").remove();
            textValueGrp.selectAll("text").remove();
            filterTextGrp.selectAll("text").remove();
            if (json.data.length > 0) {


                sortedAlphabeticalData = _.sortBy(json.data, function (o) {
                    return o.name;
                });
              //  sortedAlphabeticalData.reverse();

                sortedCountData = _.sortBy(json.data, function (o) {
                    return o.size;
                }).reverse();
                if(sortField== "Alphabetical" && json.dataType){
                	if(json.dataType.toUpperCase() == "DATE") {
                		if(json.sortOrder== "asc" && json.dataType){
	                    	sortedCountData.sort(function(a,b) { 
	                    		var aDate=new Date(a.name); var bDate=new Date(b.name);
	                    	    return aDate - bDate;
	                    	});
	                    	sortedAlphabeticalData.sort(function(a,b) { 
	                    		var aDate=new Date(a.name); var bDate=new Date(b.name);
	                    	    return aDate - bDate;
	                    	});
	                    } else {
	                    	sortedCountData.sort(function(a,b) { 
	                    		var aDate=new Date(a.name); var bDate=new Date(b.name);
	                    	    return bDate - aDate;
	                    	});
	                    	sortedAlphabeticalData.sort(function(a,b) { 
	                    		var aDate=new Date(a.name); var bDate=new Date(b.name);
	                    	    return bDate - aDate;
	                    	})
	                    }
                	}              	
	                if(json.dataType.toUpperCase() == "TEXT") {
	                	if(json.sortOrder== "asc" && json.dataType){
	                    	sortedAlphabeticalData.sort(function(a,b) { return a.name - b.name });
	                    	sortedCountData.sort(function(a,b) { return a.name - b.name });
	                    	sortedCountData = _.sortBy(json.data, function (o) {
		                        return o.name;
		                    });
	                    	sortedAlphabeticalData = _.sortBy(json.data, function (o) {
		                        return o.name;
		                    });
	                    } else {
	                    	sortedAlphabeticalData.sort(function(a,b) { return b.name - a.name });
	                    	sortedCountData.sort(function(a,b) { return b.name - a.name });
	                    	sortedAlphabeticalData = _.sortBy(json.data, function (o) {
		                        return o.name;
		                    }).reverse();
	                    	sortedCountData = _.sortBy(json.data, function (o) {
		                        return o.name;
		                    }).reverse();
	                    }
	            	}
	                if(json.dataType.toUpperCase() == "NUMERIC") {
	                	if(json.sortOrder== "asc" && json.dataType){
	                    	sortedAlphabeticalData.sort(function(a,b) { return parseInt(a.name) - parseInt(b.name) });
	                    	sortedCountData.sort(function(a,b) { return parseInt(a.name) - parseInt(b.name) });
	                    	sortedCountData = _.sortBy(json.data, function (o) {
		                        return parseInt(o.name);
		                    });
	                    	sortedAlphabeticalData = _.sortBy(json.data, function (o) {
		                        return parseInt(o.name);
		                    });
	                    } else {
	                    	sortedAlphabeticalData.sort(function(a,b) { return parseInt(b.name) - parseInt(a.name) });
	                    	sortedCountData.sort(function(a,b) { return parseInt(b.name) - parseInt(a.name) });
	                    	sortedAlphabeticalData = _.sortBy(json.data, function (o) {
		                        return parseInt(o.name);
		                    }).reverse();
	                    	sortedCountData = _.sortBy(json.data, function (o) {
		                        return parseInt(o.name);
		                    }).reverse();
	                    }
	            	}
	                if(json.dataType.toUpperCase() == "PARARANGES") {
	                	if(json.sortOrder== "asc" && json.dataType){
	                    	sortedAlphabeticalData.sort(function(a,b) { return a.minVal - b.minVal });
	                    	sortedCountData.sort(function(a,b) { return a.minVal - b.minVal });
	                    	sortedCountData = _.sortBy(json.data, function (o) {
		                        return o.minVal;
		                    });
	                    	sortedAlphabeticalData = _.sortBy(json.data, function (o) {
		                        return o.minVal;
		                    });
	                    } else {
	                    	sortedAlphabeticalData.sort(function(a,b) { return b.minVal - a.minVal });
	                    	sortedCountData.sort(function(a,b) { return b.minVal - a.minVal });
	                    	sortedAlphabeticalData = _.sortBy(json.data, function (o) {
		                        return o.minVal;
		                    }).reverse();
	                    	sortedCountData = _.sortBy(json.data, function (o) {
		                        return o.minVal;
		                    }).reverse();
	                    }
	            	}
	                
                } 
                if(sortField ==='DocumentCount') {
                	//if(json.dataType.toUpperCase() == "TEXT" || json.dataType.toUpperCase() == "DATE" || json.dataType.toUpperCase() == "NUMERIC") {
	                	if(json.sortOrder== "asc") {
	                    	sortedAlphabeticalData.sort(function(a,b) { return a.size - b.size });
	                    	sortedCountData.sort(function(a,b) { return a.size - b.size });
	                    	sortedCountData = _.sortBy(json.data, function (o) {
		                        return o.size;
		                    });
	                    	sortedAlphabeticalData = _.sortBy(json.data, function (o) {
		                        return o.size;
		                    });
	                    } else {
	                    	sortedAlphabeticalData.sort(function(a,b) { return b.size - a.size });
	                    	sortedCountData.sort(function(a,b) { return b.size - a.size });
	                    	sortedAlphabeticalData = _.sortBy(json.data, function (o) {
		                        return o.size;
		                    }).reverse();
	                    	sortedCountData = _.sortBy(json.data, function (o) {
		                        return o.size;
		                    }).reverse();
	                    }
	            	//} 
                }
                var scaleData = sortField == "Alphabetical" ? _.clone(sortedAlphabeticalData) : _.clone(sortedCountData);
                var initialSum = 0;
                var totalTaggedResults = d3.sum(sortedCountData, function (d) {
                    return d.size;
                });
                totalTaggedResults=totalTaggedResults;
                var underTaggedObject = null;
                if (totalEncounter > totalTaggedResults) {
                    underTaggedObject = {};
                    underTaggedObject.name = "Records not matching any of the below tags";
                    underTaggedObject.size = totalEncounter - totalTaggedResults;
                    scaleData.push(underTaggedObject);
                }

                var overTaggedObj = {};

                overTaggedObj.name = "Total number of tags";
                overTaggedObj.size = totalTaggedResults;

                var indicatorIndex = 0;
                for (var val = 0; val < scaleData.length; val++) {
                    if (initialSum < totalEncounter) {
                        initialSum = initialSum + scaleData[val].size;
                    } else {

                        // scaleData.splice(val,0,overTaggedObj);
                        indicatorIndex = val;
                        break;
                    }
                }
                /* Stack layout for results range */
                var stack = d3.layout.stack();
                var layers = stack(d3.range(scaleData.length).map(function (d) {
                    var a = [];
                    a[0] = {x: 0, y: scaleData[d].size, name: scaleData[d].name, size: scaleData[d].size};

                    return a;
                }))
                //the largest stack
                var yStackMax = d3.max(layers, function (layer) {
                    return d3.max(layer, function (d) {
                        return d.y0 + d.y;
                    });
                });

                resultsRange.append("g").append("text").attr("text-anchor", "right").attr("class", "results-range-text").text("Results Range").attr("y", "60").attr("x", 180);
                var xscale = d3.scale.linear()
                    .domain([0, yStackMax])
                    .range([0, width- 400]);
                stack(layers);
                var sum = 0, indicatorPosition;
                svgLayer = resultsRange.selectAll(".layer").data(layers).enter().append("g").attr("transform", "translate(300,0)").attr("class", "layer").style("fill", function (d, i) {
                    if (i == (scaleData.length - 1) && underTaggedObject != null) {
                        return "#b9b8bb"
                    } else {
                        return color(i)
                    }
                });
                svgLayer.selectAll("rect").data(function (d) {
                    return d;
                }).enter().append("rect").attr("y", 40)
                    .attr("x", function (d) {
                        return xscale(d.y0);
                    })
                    .attr("height", 25)
                    .attr("width", function (d) {
                        return xscale(d.y);
                    }).transition()
                    .duration(animationTime);
                var formattedTotalEncounter = numberFormat(totalEncounter);
                var formattedTotalTagged = numberFormat(totalTaggedResults);
                resultsRange.append("g").attr("transform", "translate(300,0)").append("rect").data([overTaggedObj]).attr("class", "total-tagged").attr("x", xscale(totalTaggedResults)).attr("y", 40).style("fill", "#767676").attr("width", 6).attr("height", 25);
                resultsRange.append("g").attr("transform", "translate(300,0)").append("text").attr("x", xscale(totalTaggedResults)).attr("y", 102).text(formattedTotalTagged).attr("class", "light-grey results-range-text").style("fill", "#767676");
                resultsRange.append("g").attr("transform", "translate(300,0)").append("text").attr("x", xscale(totalTaggedResults)).attr("y", 85).text("Tagged  " + json.resultLabel).style("class", "light-grey results-range-text").style("fill", "#767676");

                var val = 0;

                for (val = 0; val < layers.length; val++) {
                    if (layers[val][0].y0 > totalEncounter) {
                        indicatorPosition = layers[val][0].y0;
                        break;
                    }
                }
                resultsRange.append("g").attr("transform", "translate(300,0)").append("rect").data([
                    {"name": "Total number of results", "size": totalEncounter}
                ]).attr("class", "total-tagged").attr("y", 40).attr("x", xscale(totalEncounter)).attr("height", 25).attr("width", 5).style("fill", "#3F3F3F");
                var transf = "translate(295,0)"
                legend.append("g").attr("transform", transf).append("text")
                    .attr("x", xscale(totalEncounter))
                    .attr("y", 25)
                    .attr("class","results-range-text")
                    .text(formattedTotalEncounter);
                legend.append("g").attr("transform", transf).append("text")
                    .attr("x", xscale(totalEncounter))
                    .attr("y", 10).
                    attr("class", "results-range-text")
                    .text("Total " + json.resultLabel);



                filterTextGrp.append("text").attr("class", "results-range-text").text(json.data[0].filter).attr("y", "20").attr("x", "0");

                    if (json.data.length < json.data[0].totalNumResults) {
                        filterTextGrp.append("g").attr("transform", "translate(0,0)").append("text")
                            .attr("x", function(d){
                                return width - 270;
                            })
                            .attr("y", 20).
                            attr("class", "results-range-text")
                            .text("Top " + Math.min(maximumFieldsAllowed, json.data[0].numValues)  + " tags of " + json.data[0].totalNumResults + " are shown");
                    }

                /** Start of histogram results **/
                var x = d3.scale.linear()
                    .domain([maximumRecord,0])
                    .range([width- 400,0]);
                var y = d3.scale.linear().domain([0, json.data.length]).range([barHeight, 0]);
                //      sortedCountData = sortedCountData.reverse();
                visEl = rectGroup.selectAll("rect").data(sortField == "Alphabetical" ? sortedAlphabeticalData : sortedCountData).enter().append("rect")
                    .attr("y", function (d, i) {
                        return barHeight - y(i);
                    }).transition()
                    .duration(animationTime)
                    .attr('x', 0)
                    .attr("width", function (d, i) {
                    	var dataSize = d.size;
                    	if(dataSize < 0) {
                    		dataSize = 0;
                    	}
                        return x(dataSize);
                    }).attr("height", 25)
                    .style('fill', function (d, i) {
                        return  color(i)
                    }).call(callLabel);


                textValueGrp.selectAll("text").data(sortField == "Alphabetical" ? sortedAlphabeticalData : sortedCountData).enter().append("text").text(function (d, i) {
                    return arcLabelFormatter(d);
                }).attr("y", function (d, i) {
                    return barHeight - y(i) + 15;
                }).attr("x", 0);
                var updateEls = visEl;
                // updateEls.exit().remove();
                /*
                 var animate = updateEls.length < 200, path;
                 // on the existing elements
                 var delay = function (d, i) {
                 return i * 50;
                 };
                 */
                $(".results-range").css("border-bottom: 2px solid #e1e1e8");
                
                /* The below sets up the count on histogram bars */
                function callLabel() {
                    if (arcLabelTimeout) {
                        clearTimeout(arcLabelTimeout);
                    }

                    arcLabelTimeout = setTimeout(function () {


                        rectGroup.selectAll("text").data(sortField == "Alphabetical" ? sortedAlphabeticalData : sortedCountData).enter().append("text").attr("y", function (d, i) {
                            return ( barHeight - y(i) + 16 );
                        })
                            .attr("x", function (d, i) {
                            	var dataSize = d.size;
                            	if(dataSize < 0) {
                            		dataSize = 0;
                            	}
                                    return (x(dataSize) + 10)

                            }).text(function (d, i) {
                                if (formatPercentage) {
                                    return formatPercent(d.size, totalEncounter) + "%";
                                } else {
                                    return numberFormat(d.size);
                                }
                            });

                    }, 1000);

                }
            }

            

            var lastTouchEnd;

            // Click events on histogram
            rectGroup.selectAll("rect").on('mousedown', mousedown);
            rectGroup.selectAll("rect").on('mouseup', longPressHandler.mouseup);
            rectGroup.selectAll("rect").on('mouseover', hover);
            rectGroup.selectAll("rect").on('mouseout', mouseout);
            // Hover events for results range
            svgLayer.selectAll("rect").on('mouseover', hover);
            svgLayer.selectAll("rect").on('mouseout', mouseout);
            resultsRange.selectAll(".total-tagged").on('mouseover', hover);
            resultsRange.selectAll(".total-tagged").on('mouseout', mouseout);
            rectGroup.selectAll("rect").on('touchstart', function (d) {
                if (d3.event.touches.length > 1) {
                    return;
                }
                mousedown(d);
            });
            vis.selectAll("rect").on('touchmove', function (d) {
                var evt = d3.event;
                if (evt.touches.length > 1) {
                    return;
                }
                // event.target on touch events refers to the original dom element which the touch started on,
                // so we have to look up the object from the mouse event
                var dragEl = document.elementFromPoint(evt.touches[0].clientX, evt.touches[0].clientY);

                if (!dragEl || dragEl.tagName !== 'path') {
                    return;
                }

                var hoveredData = dragEl.__data__;

                if (!hoveredData) {
                    return;
                }

                // If unzoomed, you can only scroll on the 'Total' count
                // If zoomed, you can only scroll on the zoomed segment or its parent
                if (prevClicked ? hoveredData !== prevClicked && hoveredData !== prevClicked.parent : hoveredData.parent != null) {
                    evt.preventDefault();
                }

                longPressHandler.cancel();
                hover(hoveredData);
            });
            vis.selectAll("rect").on('touchcancel', longPressHandler.cancel);

            vis.selectAll("rect").on('touchend', function (d) {
                if (d3.event.touches.length > 1) {
                    return;
                }

                lastTouchEnd = Date.now();

                d3.event.preventDefault();
                longPressHandler.mouseup();
            });


            function mousedown(d) {
                if (lastTouchEnd && (Date.now() - lastTouchEnd < 100 )) {
                    // iPhone tends to fire a mousedown after the touchend event, which we don't want
                    return;
                }


                longPressHandler.mousedown(function () {


                    if (d === prevClicked) {
                        return;
                    }

                    prevClicked = d;

                    onShortClick && onShortClick(d);


                }, function () {

                    onLongClick && onLongClick(d);
                });

                if (d3.event.ctrlKey) {
                    longPressHandler.longPress();
                }
            }

            //   hideCenterLabel();
            // Hover tooltip shown on results range bar.
            function hover(d) {
                if (prevHovered === d) {
                    return;
                }
                prevHovered = d;


                $('.tooltip').html(d.name + " (" + numberFormat(d.size) + " )");

                $('.tooltip').css('left', (d3.event.pageX - $("#filterchart").offset().left) + 'px')
                $('.tooltip').css('top', d3.event.pageY - $("#filterchart").offset().top - 60 + 'px');
                $('.tooltip').css('background-color', '#1E1E1E');
                $('.tooltip').show();
                $('.tooltip').css('opacity', '.8');
                $('.tooltip').css('border-radius', '3px');
                $('.tooltip').css('color', '#FFFFFF')

            }

            function mouseout() {
                prevHovered = null;
                longPressHandler.cancel();
                $('.tooltip').css('display', 'none');
                $('.tooltip').css('opacity', '0');

            }
        }


    };

});