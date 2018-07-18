// Include this file in a page to load popular and breaking clusters on document load.

// TODO:
// -  Better handling for reference - currently treated as a URL.
//    For other datasets this should be a link to a View page or something. 

jQuery(function($) {
	var tempID  =  this.versionObj;
	var tempID =  localStorage.getItem("storageId");
	
	// Constants
	var MAX_ITEMS_PER_CLUSTER = 3;

	var HEADLINE_CATEGORY = "Headline";
	var BREAKING_CATEGORY = "Breaking";
	var POPULAR_CATEGORY = 'Popular';

	var $clusterContainer = $('#initialContent .initialContentList');

	var popularNewsUrl = 'ajax/clusters/getPopularNews.json';
	var breakingNewsUrl = 'ajax/clusters/getBreakingNews.json';


	// Templates
	var HEADLINE_TEMPLATE = '../templates/search/headline'+tempID+'.template';
	var CLUSTER_TEMPLATE = '../templates/search/cluster'+tempID+'.template';
	var CLUSTER_RESULTS_TEMPLATE = '../templates/search/clusterResults'+tempID+'.template';

	var headlineTemplate = $.resGet(HEADLINE_TEMPLATE);
	var clusterTemplate = $.resGet(CLUSTER_TEMPLATE);
	var clusterResultsTemplate = $.resGet(CLUSTER_RESULTS_TEMPLATE);


	var getClusters = function (url, onComplete) {
		$.ajax({
			url : url,
			dataType : 'json',
			success : function(result) {
				onComplete(result);
			}
		});
	};

	/**
	 * Encodes uris and converts to a viewer ref if needed
	 */
	var processReference = function (reference) {
		var result = encodeURI(reference);
		/* -- TODO: DO MAGIC TO CONVERT TO VIEW URL IF REQUIRED -- */
		return result;
	};

	/**
	 * Cleans the html from a result object
	 */
	var processResult = function (data) {
		if (data) {
			// Strip any html in the title & summary
			data.title = Util.cleanHtml(data.title);
			data.summary = Util.cleanHtml(data.summary);
			// Process the reference - currently assumes a URL
			data.reference = processReference(data.reference);
			// Process any children the element may have
			data.children = _.map(data.children, processResult);
		}
		return data;
	};

	/**
	 * Calculates headline news and orders: [headline . breaking . popular]
	 */
	var processNews = function(popular, breaking) {
		var result, news, popularItems, breakingItems;

		news = {
			headline : [],
			popular : [],
			breaking : []
		};

		popularItems = processNewsCategory(popular);
		breakingItems = processNewsCategory(breaking);

		//  Process the headline and the popular news
		_.each(popularItems, function(item, title) {
			//  Headline news items are both popular and breaking
			if (title in breakingItems) {
				item.category = HEADLINE_CATEGORY;
				news.headline.push(item);
			} else {
				//  Non-breaking popular are popular
				item.category = POPULAR_CATEGORY;
				news.popular.push(item);
			}
		});

		//  Process the breaking news
		_.each(breakingItems, function(item, title) {
			//  Non-popular breaking are breaking
			if (!(title in popularItems)) {
				item.category = BREAKING_CATEGORY;
				news.breaking.push(item);
			}
		});

		//  Merge all news
		var result = [];

		_.each([news.headline, news.breaking, news.popular], function (cluster) {
			result = result.concat(cluster.slice(0, MAX_ITEMS_PER_CLUSTER));
		});

		//  Process each news document
		_.each(result, function (item) {
			item.documents = _.map(item.documents, processResult);
		});
		return result;
	};

	/**
	 * Builds a hask map of items.
	 * Identify each item by their title.
	 */
	var processNewsCategory = function(items) {
		var result = {};
		_.each(items, function(item) {
			result[item.title] = item;
		});
		return result;
	};


	//  On load
	(function() {
		var popularNews = null, breakingNews = null;

		//  Retrieve the news
		getClusters(popularNewsUrl, function(popularNewsItems) {
			popularNews = popularNewsItems;
			composeAndRenderNews();
		});
		getClusters(breakingNewsUrl, function(breakingNewsItems) {
			breakingNews = breakingNewsItems;
			composeAndRenderNews();
		});

		/**
		 * Processes the loaded news and renders it to the page
		 */
		var composeAndRenderNews = function() {
			//  If both popular and breaking have been loaded
			if (popularNews && breakingNews) {
				//  Process all the news into a list of news items
				//  Sorted based on type and score, where
				//    type :: Heading > Breaking > Popular
				var news = processNews(popularNews, breakingNews);
//				var news = processNews(popularNews, breakingNews.concat(popularNews.slice(0, 2)));

				//  Render the news items
				$clusterContainer.html(_.template(clusterResultsTemplate, { items: news }));

				//  Set up any display configuration
				display();
			}
		};

		var display = function() {
			$('.summary-expand').toggler({
				'^ .hiddenSummary' : {
					active : 'showHiddenSummary'
				}
			});

			$('#show .nopacity').not('.noResults')
				.addClass('fullOpacity')
				.removeClass('nopacity');
		};

		$(window).on('resize', function () {
			var $rect = $('#show');
			$('.halfFull, .middle').toggler($rect.width() < 800 ? 'on' : 'off');
		});

		$('.summary-expand').live('click', function() {
			$(this).toggler('toggle');
		});
	})();
});
