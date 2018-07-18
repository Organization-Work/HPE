/**
 * search.searching.js
 */

jQuery(function($) {
	
	/* -- Constants -- */
	var tempID =  localStorage.getItem("storageId");
	
	var SUGGESTION_LIST_HIGHLIGHT = 'alert alert-info';
	var SUGGESTION_LABEL_HIGHLIGHT = 'label-warning';
	
	var sortVal = "";
	var sortOrder = "";
	var SUGGESTIONS_URL = "ajax/search/getSuggestions.json";
	var RESULTS_URL = "ajax/search/getFilteredResults.json?process=RESULTS&sort="+sortVal+ "&sortOrder="+sortOrder;
	var LINKS_URL = "ajax/search/getLinks.json";
    var PARAMETRIC_VALUES_URL = "ajax/parametric/getParaFieldValues.json";
    var PARAMETRIC_NAMES_URL = "ajax/parametric/getParaFieldNames.json";

	//  Templates
	var SUGGESTION_TEMPLATE_URL = '../templates/search/suggestions'+tempID+'.template';
	var RESULT_TEMPLATE_URL = '../templates/search/results'+tempID+'.template';
    var META_TEMPLATE_URL = '../templates/search/meta'+tempID+'.template';
    var LINKS_TEMPLATE_URL = '../templates/search/links'+tempID+'.template';

    // This would be nice to replace with the require text plugin
	var SUGGESTION_TEMPLATE = $.resGet(SUGGESTION_TEMPLATE_URL);
	var RESULT_TEMPLATE = $.resGet(RESULT_TEMPLATE_URL);
	var META_TEMPLATE = _.template($.resGet(META_TEMPLATE_URL), undefined, {variable: 'ctx'});
	var LINKS_TEMPLATE = _.template($.resGet(LINKS_TEMPLATE_URL), undefined, {variable: 'ctx'});

	var $searchArea = $("#searchArea");
	var $searchGo = $('#searchGo');
	var $resultsContainer = $("#searchResults");
	var $resultsBox = $resultsContainer.find('.resultsList');
	var $resultsIntro = $resultsContainer.find('.resultsIntro');
	var $totalResults = $resultsContainer.find('.totalResults');
	
	var $filterchart = $('#filterchart');
	var $totalResults_barchart = $filterchart.find('.view-results-label');
    
    var $resultActions = $('#resultActions');
	var $resultExport = $('#resultsExportAction');
	var $resultTag = $('#resultsTagAction');
	
	
	var $resultViewSelect = $('#resultViewSelect');
	

	var $suggestionsBox = $("#suggestions");
	// the default display on the search
	var $initialContent = $(".initial");
	var $initialSidebarContent = $(".sidebar .initial");
	// page - hide when searching, display when
	var $resultsDivs = $(".results");
	var $resultsSidebarDivs = $(".sidebar .results");
	// reset
	var $scrollToTop = $('#scrollToTop');
	
	var $newsTab = $('[href=#initialContent]');
	var $searchTab = $('[href=#searchResults]');
    var $filterchartTab = $('[href=#filterchart]');
    var $topicmapTab = $('[href=#topicmap]');
    var $tableviewerTab = $('[href=#tableviewer]');
    var $trendingviewerTab = $('[href=#trending]');
    var $networkMapTab = $('[href=#networkmap]');
	var $agentFromSearch = $('#agentFromSearch');
    var $searchTypeTabs = $filterchartTab.add($topicmapTab).add($trendingviewerTab).add($networkMapTab).add($searchTab).add($tableviewerTab);

    var $infContext = $('#show');
    var infAccuracy = 100;
    var $docviewContainer = $('#cboxContent');

    var filters = {};
    var currentSearchData = {};
    var lastSearchData = {};
    var lastSettings = {};
    var lastTotalResults = {};
    var $searchModeSelect = $('.search-mode select');
    var $filterSyncError = $('#filterSyncError');
    var $resultsSort = $("#results-sort");
    
    var $cohortContent = $("#cohort-query");
    var $cohortSection = $("#cohort-section");

    function updateTotalResults() {
        var total = _.reduce(lastTotalResults, function(acc, val){
            return acc + (val || 0);
        }, 0);

        //$totalResults.text($totalResults.text().replace(/\s*\d*$/, ' ' + total));
        var totalResultLabel = '  Total ' + SearchSettings.getResultViewText() + ': ' + SearchEvents.getFormattedResult(total);
        $totalResults.text(totalResultLabel);
        
        $('.result-actions li > a').toggleClass('disabled', !total);
        
    }
    
    function activateDocFolderTab() {
        var $filtersTab = $('#docfoldersTab');
        if ($filtersTab[0].id !== $('.filter-tabs li.active')[0].id) {
            $filtersTab.find('a').click();
        } 
        
    }
    
    $searchTab.on('shown', function(){
    	if (!_.isEqual(lastSearchData, currentSearchData)) {
    		attemptSearch();
    	}
         return true;
    });
    
    $resultViewSelect.on('change', function() {
        attemptSearch();
        return true;
        
    });
    
    $resultExport.on('click', function() {
        if (!$(this).hasClass('disabled')) {
            $('#documentExportDialog').docResultsExport({folderId: null, resultsExport: true});
        }
        return false;        
    });

    $resultTag.on('click', function() {
        if (!$(this).hasClass('disabled')) {
            activateDocFolderTab();
            $('#resultsTagDialog').docResultsTag();
        }
        return false;        
    });
    
    $('.filter-tabs').on('click', '.options-tab', function(e) {
        var navTabs =$(this).closest('.filter-tabs');
        
        var addControl = $(this).data('addcontrol');
        
        navTabs.find('.toolbar').toggleClass('hidden', true);
        navTabs.find(addControl).removeClass('hidden');
        
        return true;
    });
    
    $('.search-mode select').change(function() {
        var emptyResult = $resultsContainer.find('.resultsIntro').length > 0;

        if (!SearchSettings.isManualSearch() && ($filterSyncError.is(':visible') || emptyResult)) {
            updateFilterData();
            attemptSearch();
        }
        
        return true;
    });

/*    
    var toggleSearchMode = function(isManualMode) {
        $('#show').toggleClass('manual', isManualMode);
        
    };
    
*/
    SearchSettings.$.on(SearchSettings.DATABASECHANGE, updateTotalResults);
    

	/* -- Methods -- */
	var attemptSearch = (function() {
		return function(fromHashChange) {
			_.defer(function() {
				var text = $.trim($searchArea.val());

                if ((text || !_.isEmpty(filters)))  {
					var loadedSuggestions, loadedResults;

					/*
					 * Request the search, if it is still valid, and the
					 * search was a success then render the results.
					 */
                    SearchEvents.$.trigger(SearchEvents.SEARCH, {text:text});
					requestSearch(text, {
						results : function(json) {
						    SearchEvents.toggleResultsLoading(false, $totalResults);
						    
                            lastTotalResults = json.totalResults;
                            var results = json.docs;
							startDisplayingResults();
							_.extend(currentSearchData, {stateMatchId: json.resultStateMatchId, stateDontMatchId: json.resultStateDontMatchId});
							currentSearchData.retrieveResultDocs = false; // reset
							SearchEvents.$.trigger(SearchEvents.RESULTS_PROCESSING, [ results, lastTotalResults, $.extend(true, {}, currentSearchData) ]);
                            updateTotalResults();
                           // if(results-sort=="Patient ID"){
							$resultsBox.html(renderResults(results));							
                           // }
							loadedResults = true;
							
							// display cohort query to users
							$("#cohort-query").val(json.sqlQuery);
							// show or hide the cohort section now
							// var selectedSearchView = SearchEvents.getSelectedSearchView();
							// var displayCohort = SearchConfig.searchViews[selectedSearchView].displayCohort;
							 if(json.sqlQuery) {
								 $("#cohort-section").show();
							 } else {
								 $("#cohort-section").hide();
							 }
							
							
							// "Can't execute freed code" error only happens in IE, so ignore, all other browsers ignore automatically anyways
							try {
								SearchEvents.$.trigger(SearchEvents.RESULTS_LOADED);
							} catch(ignore) {
								
							}
							
							applySummaryProcessing(loadedSuggestions, loadedResults);
							 if($resultsContainer.is(':visible')) {
								 lastSearchData = currentSearchData;
							 }
						},
/*						suggestions : function(results) {
							SearchEvents.$.trigger(SearchEvents.SUGGESTIONS_PROCESSING, [ results ]);
							$suggestionsBox.html(renderSuggestionResults(results));
							loadedSuggestions = results;
							SearchEvents.$.trigger(SearchEvents.SUGGESTIONS_LOADED);
							applySummaryProcessing(loadedSuggestions, loadedResults);
						}
*/
					}, fromHashChange);
				} else {
					resetSearch();
				}
			});
		};
	})();
	
	
    var updateFilterData = function() {
	    SearchSettings.toggleFilterSyncError(false);
	    var $paraFilterForm = $('#parametricForm').data('filterForm');
	    if ($paraFilterForm) {
            SearchEvents.setFilters($paraFilterForm.getFiltersData());
        }
        
    };

	var renderSuggestionResults = function (results) {
		return _.map(results.clusters, renderSuggestions).join('');
	};

    var sortArrays = function (obj) {
        _.each(obj, function (v, k) {
            obj[k] = _.sortBy(v, _.identity);
        });
        return obj || {};
    };

    function getSearchSettings() {
        //var userSettings = _.extend({}, SearchEvents.getUserSearchSettings());
        //delete userSettings['id'];
        //delete userSettings['owner'];
        //var combineSetting = $resultViewSelect.val();
        
        return {
            userSearchSettings: {combine: $resultViewSelect.val()},
            displayChars: SearchSettings.getDisplayChars(),
            minScore: SearchSettings.getMinScore()
        };
    }

    SearchSettings.$.on(SearchSettings.DISPLAYCHARSCHANGE + ' ' + SearchSettings.MINSCORECHANGE, _.debounce(function() {
        if (!_.isEqual(getSearchSettings(), lastSettings)) {
            SearchEvents.attemptSearch();
        }
    }, 500));

    SearchSettings.getCurrentSearchFilters = function() {
        return $.extend(true, {}, currentSearchData);
    };
    
    SearchSettings.setDocviewHlField = function(hlfield) {
        $docviewContainer.data('hlfield', hlfield);
    }
    
    SearchSettings.getDocviewHlField = function() {
        return $docviewContainer.data('hlfield');
    }
    
    SearchSettings.setActiveViewTab = function(tabId) {
        $docviewContainer.data('activeTab', tabId);
    }
    
    SearchSettings.getActiveViewTab = function() {
        return $docviewContainer.data('activeTab');
    }
    
    SearchSettings.getResultViewText = function() {
        return $resultViewSelect.find('option:selected').text();
    }
    
    SearchSettings.getSearchMode = function() {
        return $searchModeSelect.val();
    }
    
    SearchSettings.isManualSearch = function() {
        return 'MANUAL' === $searchModeSelect.val();
    }
    
    SearchSettings.toggleFilterSyncError = function(isError) {
        $filterSyncError.toggleClass('hidden', !isError);
        SearchEvents.toggleResultsSyncError(isError);
    }
    
    SearchEvents.toggleResultsSyncError = function(isError, $resultElement) {
        var $resultsLabel = $resultElement || $('.view-results-label');
        $resultsLabel.toggleClass('error', isError);
        $infContext.toggleClass('results-outdated', isError);
    }

    SearchEvents.toggleResultsLoading = function(isLoading, $resultElement) {
        var $resultsLabel = $resultElement || $('.view-results-label');
        $resultsLabel.toggleClass('loading', isLoading);
    }
    
   
    
    // toggleSearchMode(SearchSettings.isManualSearch());


	var requestSearch = (function() {
		var previousXHR, previousXHRSuggest;

		return _.debounce(function(text, succeeded, fromHashChange) {
			var xhr, xhrSuggest;

			if (previousXHR) {
				previousXHR.abort();
			}
			if (previousXHR) {
				previousXHRSuggest.abort();
			}

            cancelNextPage();

            currentSearchData = $.extend(true, {}, currentSearchData);

            if (!fromHashChange) {
                //  Apply all the registered filters
                //var dataFilters = _.foldl(filters || [], function (v, f) { return f(v); }, {});
                //  Build the search request data
                //currentSearchData.filters = sortArrays(dataFilters);
                
                currentSearchData.filterGroup = _.isEmpty(filters) ? null : filters;
            }
            
            currentSearchData.gotAll = undefined;
            currentSearchData.page = 0;
            currentSearchData.query = text;
            currentSearchData.searchView = SearchEvents.getSelectedSearchView();
            if($resultsContainer.is(':visible')) {
            	currentSearchData.retrieveResultDocs = true;
            }

            lastSettings = getSearchSettings();

            _.extend(currentSearchData, lastSettings);

            if (!_.isEqual(hash.getCurrentValue(), currentSearchData)) {
                hash.changeAfter(currentSearchData, 200);
                //SearchHistory.saveSearch(currentSearchData.query, true);
            }
            
            $filterchart = $('#filterchart');
        	$totalResults_barchart = $filterchart.find('.view-results-label');
            
            SearchEvents.toggleResultsLoading(true, $totalResults);
            SearchEvents.toggleResultsLoading(true, $totalResults_barchart);
            if($('.goActive').length>0){
            	delete currentSearchData.filterGroup.filterFields;
            	$('.goActive').removeClass('.goActive');
            }
            // if($resultsContainer.is(':visible')) {
			xhr = previousXHR = $.ajax({
				url : RESULTS_URL,
                type : 'POST',
                contentType : 'application/json',
				dataType : 'json',
				data : JSON.stringify(currentSearchData),
				success : function(response) {
    				    if (response.success) {
        				    var result = response.result;
        					// If the request is no-longer needed, ignore it
        					if (xhr !== previousXHR) {
        						return;
        					}
        					previousXHR = undefined;
        					Util.log("Last ajax request succeeded.", result);
        					succeeded.results(result);
    				    } else {
    				        
        					if (xhr !== previousXHR) {
        						return;
        					}
        					previousXHR = undefined;
        					Util.log("Error with last ajax request.", result);
        					
        					 	//vis.selectAll("text").remove();
        		                $(".results-range").find(".layer").remove();
        		                $(".results-range").find("rect").remove();
        		                $(".results-range").find("text").remove();
        		                // resultsRange.selectAll("text").remove();
        		                $("svg").find("g").find("rect").remove();
        		                $("svg").find("g").find("text").remove();
        		                d3.selectAll("svg > text").remove();
        		                //textValueGrp.selectAll("text").remove();
        		                //filterTextGrp.selectAll("text").remove();     	                
        					$('#sunburst-totals').html(SearchEvents.getSearchErrorMessage(response.error, response.errorDetail));
        					SearchEvents.toggleResultsLoading(false, $totalResults);
        					$totalResults.html(SearchEvents.getSearchErrorMessage(response.error, response.errorDetail));
        					var errorMsg = '<span class="search-error">' + response.error + '<span class="error-detail">' + response.errorDetail + '</span></span>';
        					$('#confirmDialog').confirmDialog({
        						title: 'Search Error',
        						message: errorMsg,
        						callbackObj: null,
        						yesBtnClass: 'btn-primary',
        						yesLabel: 'OK',
        						noLabel: null
        					});                
        					
    				    }
				},
				error : function(result) {
					if (xhr !== previousXHR) {
						return;
					}
					previousXHR = undefined;
					Util.log("Error with last ajax request.", result);
				}
			});
            //}
            SearchEvents.$.trigger(SearchEvents.SEARCH_REQUEST_SENT, { search: $.extend(true, {}, currentSearchData) });
		}, 200);
	})();

	/**
	 * Recursively renders suggestions as html
	 */
	var renderSuggestions = function recur(data) {
		if (!data) {
			return "";
		}

		return _.template(SUGGESTION_TEMPLATE, {
			item : {
				name : Util.cleanHtml(data.name),
				database : data.database,
				classes : data.classes,
				children : _.map(data.children, recur)
			}
		});
	};

	var flattenSuggestions = function(suggestions) {
		if (!suggestions) {
			return [];
		}
		return _.chain(suggestions).map(function(item) {
			return [ {name: item.name, database: item.database, important: item.important}, flattenSuggestions(item.children) ];
		}).flatten().value();
	};

	var idAsCode = function(id) {
		return '{#' + id + '#}';
	};

	var suggestionAsCode = function(s) {
		return '<span class="suggestdb-' + s.database + (s.important ? ' important ' : ' label-info ') + ' suggestionLabel" href="#' + s.name.replace(/[^\w\s]+/g, '') + '">' + s.name + '</span>';
	};

	var largestToSmallest = function(a) {
		return (-a.length);
	};

	var listSuggestions = function(ss) {
		return _.chain(flattenSuggestions(ss)).uniq()
				.sortBy(largestToSmallest).value();
	};

	var processSummariesWithSuggestions = function(summary, suggestions) {
		return processSummary(summary, _.map(suggestions.mainClusters, function (cluster) {
			cluster.important = true;
			return cluster;
		}).concat(_.map(suggestions.clusters, function (cluster) {
			cluster.important = false;
			return cluster;
		})));
	};

    var applySummaryProcessing = function (loadedSuggestions, loadedResults) {
        if (loadedSuggestions && loadedResults) {

            $resultsBox.find("p").not(".processed").addClass('processed').each(function() {
                var $this = $(this);
                $this.html(processSummariesWithSuggestions($this.html(), loadedSuggestions));
            });

            $resultsBox.find(".suggestionLabel:not(.important)").not(".processed").addClass('processed').toggler({
                "this" : {
                    active : 'label label-info setSearch',
                    inactive : ''
                }
            });
            $resultsBox.find(".suggestionLabel.important").not(".processed").addClass('processed').toggler({
                "this" : {
                    active : 'label setSearch',
                    inactive : ''
                }
            });

            SearchEvents.$.trigger(SearchEvents.LOADED_BOTH);
        }
    };

	var processSummary = function(summary, suggestions) {
		var id = 0;
		var idMap = {};
		_.each(listSuggestions(suggestions), function(suggestion) {
			id += 1;
			var idCode = idAsCode(id);
			idMap[idCode] = suggestion;
			summary = summary.replace(new RegExp("\\b" + suggestion.name + "\\b", 'gi'), idCode);
		});
		_.each(idMap, function(suggestion, idCode) {
			summary = summary.replace(new RegExp(idCode, 'g'), suggestionAsCode(suggestion));
		});
		return summary;
	};

	/**
	 * Renders search results as html
	 */
	var flag = 1;
	var renderResults = function(data) {
		if (!data) {
			return "";
		}
		
		 // populate the sort options
		 //$('#results-sort option').remove();
		 var first = _.first(data);
		 if(first && flag) {
			 if (first.displayFields) {
				 _.each(first.displayFields, function(value, key) {
					 if (value && value.length) {					
						 $('#results-sort').append(new Option(key, key));
					 }
				 });
			 }
			 flag=0;
		 }
		 
		 
		 
		return _.map(data, function(item) {
			item.title = Util.cleanHtml(item.title);
			item.summary = Util.cleanHtml(item.summary);
			item.linksStr = (item.links && !_.isEmpty(item.links)) ? '&links=' + item.links[0] : '';
			
			return _.template(RESULT_TEMPLATE, {
				item : item,
				view : SearchEvents.getSelectedSearchView()
			});
		}).join("");
	};

	/**
	 * Hides the hot/breaking news and shows the search results container
	 */
	var startDisplayingResults = function() {
		$scrollToTop.click();
		setTimeout(function() {
            if (!$searchTypeTabs.parent().hasClass('active')) {
                $filterchartTab.tab('show');
            }
			$initialSidebarContent.hide();
			$resultsSidebarDivs.show();
			$resultsIntro.detach();
			$agentFromSearch.show();
		}, 200);
	};


    $infContext.on('scroll', function () {
        //  If we're on the right tab
        if ($searchTab.parent().hasClass('active') && currentSearchData && currentSearchData.query) {
            loadWhileNeedNextPage();
        }
    });

    var nextPageXhr = undefined;

    var cancelNextPage = function () {
        if (nextPageXhr) {
            nextPageXhr.abort();
            nextPageXhr = null;
        }
    };

    var getNextPage = function () {
        if (nextPageXhr) { return false; }
        currentSearchData.page += 1;
        if($resultsContainer.is(':visible')) {
        	currentSearchData.retrieveResultDocs = true;
        }
        return nextPageXhr = $.ajax({
            url: RESULTS_URL,
            type: 'POST',
            contentType: 'application/json',
            dataType: 'json',
            data: JSON.stringify(currentSearchData)
        }).always(function () { nextPageXhr = null; })
            .fail(function () { currentSearchData.page -= 1; });
    };

    var loadWhileNeedNextPage = function () {
        if (!currentSearchData.gotAll && shouldGetNextPage($infContext, infAccuracy)) {
            var request = getNextPage();
            if (request) {
                request.done(processNextPage);
            }
        }
    };

    var processNextPage = function (response) {
        var results = response.result.docs;
        if (results.length > 0) {
            SearchEvents.$.trigger(SearchEvents.RESULTS_PROCESSING, [ results, lastTotalResults, $.extend(true, {}, currentSearchData) ]);
            $resultsBox.append(renderResults(results));
            SearchEvents.$.trigger(SearchEvents.RESULTS_LOADED);
            applySummaryProcessing(true, true);
            loadWhileNeedNextPage();
        } else {
            currentSearchData.gotAll = true;
        }
    };

    var shouldGetNextPage = function (element, accuracy) {
        return element.scrollTop() >= element.prop('scrollHeight') - element.height() - accuracy;
    };



    /**
	 * Clears the search and results/suggestions containers. Displays the
	 * hot/breaking news
	 */
    var resetSearch = function () {
        Util.log("reset");
        $scrollToTop.click();
        currentSearchData = {};
        filters = {};
        cancelNextPage();
        setTimeout(function () {
            $initialSidebarContent.show();
            $resultsSidebarDivs.hide();
            if ($searchTypeTabs.parent().hasClass('active')) {
                $newsTab.tab('show');
            }
            $resultsBox.html("");
            $resultsContainer.prepend($resultsIntro);
            $suggestionsBox.html("");
            $agentFromSearch.hide();

            // set default * search on reset
            var defaultText = '*';
            $searchArea.val(defaultText);
            $searchArea.trigger("change");
            _.delay(function () {
                Hash.change({query: defaultText, searchView: SearchEvents.getSelectedSearchView()}, null);
            });
        }, 200);

        SearchEvents.$.trigger(SearchEvents.RESET);
    };

	/**
	 * Call for a reset on hashempty or when the reset button is clicked
	 */
	$("#resetButton").on('click', resetSearch);

	/**
	 * Clicking a suggestion should update the search
	 */
	$(".content").delegate(".setSearch", "click", function(e) {
		e.preventDefault();
		var text = (e.shiftKey ? $.trim($searchArea.val()) + ' ' : '') + $(this).text();
		$searchArea.val($.trim(text));
		$searchArea.trigger("change");
		_.delay(function() {
			Hash.change({query:text}, null);
		});
	});

	$agentFromSearch.on('click', function () {
        var query = currentSearchData.query;

        AgentEdit.create({
            databases: SearchSettings.getDatabases(),
            name: query,
            concepts: query.split(' AND '),
            filters: _.clone(currentSearchData.filters)
        }, {
			success: function (data) {
				Agents.reloadAndOpen(data);
			}
		});
	});

    $('.content').on('click', '.meta-data button', function() {
        var btn = $(this);
        var values = btn.data('meta-values');
        var key = btn.data('meta-key');
        var dialog = $('#showMetaFieldsDialog');
        dialog.find('.meta-title').text(key);
        dialog.find('.modal-body').html(META_TEMPLATE({ values: values }));
        dialog.modal('show');
    }).on('click', '.getLinks', function(evt) {
        evt.preventDefault();
        $.ajax({
            dataType : 'json',
            url: LINKS_URL,
            data : {
                reference : $(this).closest('.btn-group').data('reference')
            },
            success: function(data){
                var dialog = $('#showMetaFieldsDialog');
                dialog.find('.meta-title').text('Citations');
                dialog.find('.modal-body').html(LINKS_TEMPLATE(data));
                dialog.modal('show');
            }
        });
    });

	/**
	 * When entering a search term -> Search
	 */
	var hash = Hash.observe(function (value) {
	    if (!SearchEvents.$.parametricLoaded) {
	        return;
	    }
	    
        var noQuery = !value.query || $.trim(value.query) === '';
        if (noQuery && _.isEmpty(value.filters)) {
			$searchArea.val('');
			resetSearch();
		} else {
			$searchArea.val(value.query);
            currentSearchData = value;
			attemptSearch(true);
		}
	});
    
    
	$searchArea.change(function() {
	    var $this = $(this);
        updateFilterData();
	    attemptSearch();
	    setTimeout(function() {
	        $this.blur();
	    }, 10);
	    
	});
	
	$searchGo.click(function() {
		var dataOrder = $('.arrow-box').attr("data-order");
		RESULTS_URL = "ajax/search/getFilteredResults.json?process=RESULTS&sort="+$resultsSort.val() + "&sortOrder="+dataOrder;
	    updateFilterData();
	    attemptSearch();
	    
	    return false;
	});
	
	$('.arrow-box').click(function(){
		console.log('Test');
		if($(this).find('span').hasClass('icon-arrow-up')){			
			$(this).find('span').removeClass('icon-arrow-up').addClass('icon-arrow-down arrow-box-hover');
			$(this).attr("data-order",'desc');
		} else {		
			$(this).find('span').removeClass('icon-arrow-down arrow-box-hover').addClass('icon-arrow-up');
			$(this).attr("data-order",'asc');
		}
		var dataOrder = $(this).attr("data-order");
		RESULTS_URL = "ajax/search/getFilteredResults.json?process=RESULTS&sort="+$resultsSort.val()+"&sortOrder="+dataOrder;
		updateFilterData();
	    attemptSearch();
	    
	    return false;
		
	})
	
	$resultsSort.change(function () {
		var dataOrder = $('.arrow-box').attr("data-order");
		RESULTS_URL = "ajax/search/getFilteredResults.json?process=RESULTS&sort="+$resultsSort.val()+ "&sortOrder="+dataOrder;;
	    updateFilterData();
	    attemptSearch();
	    
	    return false;
	 });
	
	SearchEvents.setQuery = function(query) {
		$searchArea.val(query || '*');	
	};
	
    SearchEvents.setFilters = function (fs) {
        filters = fs;
        delete currentSearchData.stateMatchId;
        delete currentSearchData.stateDontMatchId;
    };
    SearchEvents.addFilters = function (fs) {
        filters = filters.concat(fs);
    };

    SearchEvents.setCategory = function (category) {
        currentSearchData.category = category;
    };

    SearchEvents.removeCategory = function () {
        delete currentSearchData.category;
    };

    SearchEvents.getCategory = function () {
        return currentSearchData.category;
    };
    
    SearchEvents.getSearchData = function () {
        return _.extend({}, currentSearchData);
    };

    SearchEvents.attemptSearch = function () {
        attemptSearch();
    };
    
    
    

    $resultsBox.on('click', '.squishable a[rel=search-result]', function() {
        $(this).closest('.squishable').addClass('lastClicked')
               .siblings('.lastClicked').removeClass('lastClicked');
    });

    SearchEvents.getDocuments = function() {
        var $visibleDocs = $resultsBox.find('.squishable').filter(':visible');
        var $lastClicked = $visibleDocs.filter('.lastClicked');

        var total = _.reduce(lastTotalResults, function(acc, val){
            return acc + (val || 0);
        }, 0);

        return {
            idx: $visibleDocs.index($lastClicked),
            total: total,
            urls: $visibleDocs.map(function(i, e){
                return $('a[rel=search-result]', e).attr('href')
            })
        };
    };
    
    SearchEvents.getTotalResults = function() {
        return _.reduce(lastTotalResults, function(acc, val){
            return acc + (val || 0);
        }, 0);
    };

    SearchEvents.scrollToDoc = function (idx) {
        var $visibleDocs = $resultsBox.find('.squishable').filter(':visible');
        var $el = $visibleDocs.eq(idx);

        if ($el.length) {
            $infContext.animate({
                scrollTop: $el[0].offsetTop - 0.5 * $infContext.height()
            });

            $visibleDocs.filter('.lastClicked').removeClass('lastClicked');
            $el.addClass('lastClicked');
        }
    };

    var parametricsXhr = {};

    SearchEvents.getParametricsXhr = function() {
        var viewName = SearchEvents.getSelectedSearchView();
        if (!parametricsXhr[viewName]) {
            parametricsXhr[viewName] = $.ajax({
							url : SearchConfig.preloadParaValues ? PARAMETRIC_VALUES_URL : PARAMETRIC_NAMES_URL,
							dataType : 'json',             
              data: SearchConfig.preloadParaValues ? {searchView: viewName, singleQueryField: SearchConfig.parametricSingleQueryFieldname} : {searchView: viewName}
            });
        }

        return parametricsXhr[viewName];
    };

	var getSimilarSearchSetters = function (el, parent) {
		return $('.setSearch[href="'+$(el).attr('href')+'"]', parent || document);
	};
	$('#suggestions, #searchResults')
	.delegate('.setSearch', 'mouseenter', function () {
		getSimilarSearchSetters(this, '#suggestions').addClass(SUGGESTION_LIST_HIGHLIGHT);
		var results = getSimilarSearchSetters(this, '#searchResults');
		results.not('.important').removeClass('label-info');
		results.addClass(SUGGESTION_LABEL_HIGHLIGHT);
		$(this).popover('show');
	})
	.delegate('.setSearch', 'mouseleave', function () {
		getSimilarSearchSetters(this, '#suggestions').removeClass(SUGGESTION_LIST_HIGHLIGHT);
		var results = getSimilarSearchSetters(this, '#searchResults');
		results.not('.important').addClass('label-info');
		results.removeClass(SUGGESTION_LABEL_HIGHLIGHT);
		$(this).popover('hide');
	});
});
