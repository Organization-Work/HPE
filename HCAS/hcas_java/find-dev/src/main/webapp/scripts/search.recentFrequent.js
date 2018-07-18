/**
 * Recent and Frequent searches
 */
var SearchHistory = {};
SearchHistory.SAVE_SEARCH_DELAY = 5000;
SearchHistory.SAVE_RELOAD_DELAY = 5000;
SearchHistory.SAVE_SEARCH_URL = "ajax/searchhistory/saveToHistory.json";

jQuery(function($) {
	// Selectors for <div> elements to render results into - they must contain a <ul> element, or whatever is set in displayElement.
	var RECENT_SEARCHES = $('#recentSearches');
	var FREQUENT_SEARCHES = $('#frequentSearches');
	var TOP_CONCEPTS = $('#topConcepts');
	var DISPLAY_ELEMENT = "ul";
	
	// Templates for rendering results
	var TEMPLATE_STRINGS = "<% _.each(results, function (result) { %><li><a class='setSearch' href='#<%= result %>'><%= result %></a><% }) %>";
	var TEMPLATE_VALUES = "<% _.each(results, function (result) { %><li><a class='setSearch' href='#<%= result.value %>'><%= result.value %></a><% }) %>";
	
	var INDICATOR = '<div class="indicator progress progress-striped active"><div class="bar"></div></div>';

	// Saved search config

	var configureForLoading = function (target) {
		target.find(DISPLAY_ELEMENT).html('');
		target.append(INDICATOR);
		_.delay(function () {
			target.find('.indicator .bar').css('width', '5%');
		});
	};
	
	SearchHistory.reloadNow = function () {
		configureForLoading(RECENT_SEARCHES);
		configureForLoading(TOP_CONCEPTS);
		
		$.ajax({
			url : "ajax/searchhistory/getAllSearchHistory.json",
			dataType : 'json',
			success : function(result) {
				renderList(result.recent, 		RECENT_SEARCHES, 	TEMPLATE_STRINGS);
				//renderList(result.frequent, 	FREQUENT_SEARCHES, 	TEMPLATE_VALUES);
				renderList(result.topConcepts, 	TOP_CONCEPTS, 		TEMPLATE_VALUES);
			},
			error : function(result) {
				renderError(RECENT_SEARCHES);
				renderError(TOP_CONCEPTS);
				Util.log("Error loading search history.", result);
			}
		});
	};
	SearchHistory.reloadSoon = _.debounce(SearchHistory.reloadNow, SearchHistory.SAVE_RELOAD_DELAY);

	SearchHistory.saveSearch = _.debounce(function(searchParam, shouldReload) {
		
		//	Prevent empty strings
		if ($.trim(''+searchParam) === "") { return; }
		
		$.ajax({
			url : SearchHistory.SAVE_SEARCH_URL,
			dataType : 'json',
			data : { search : searchParam },
			success : function(result) {
				if (shouldReload) { SearchHistory.reloadSoon(); }
			},
			error : function(result) {
				Util.log("Error logging search", result);
			}
		});
		
	}, SearchHistory.SAVE_SEARCH_DELAY);

	/**
	 * Renders a collection of values using a template and
	 * sets the results as children of some target DOM node.
	 */
	function renderList(results, target, template) {
		target.find('.indicator .bar').css('width', '100%');
		_.delay(function () {
			target
			.removeClass('nopacity')
			.addClass('fullOpacity')
			.find(DISPLAY_ELEMENT).html(_.template(template, { results : results })).end()
			.find('.indicator').remove();
		}, 600);
	};
	
	function renderError(target) {
		target.find('.indicator').remove();
		target.find(DISPLAY_ELEMENT).append('<li class="muted">Error loading content.');
	}
	
	//  Execute a reload on launch
	SearchHistory.reloadNow();
});