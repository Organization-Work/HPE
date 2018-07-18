var SearchEvents = {
    SEARCH: "search",
    RESET : "reset",
    SEARCH_REQUEST_SENT: "searchRequestSent",
    RESULTS_PROCESSING : "resultsProcessing",
    SUGGESTIONS_PROCESSING : "suggestionsProcessing",
    RESULTS_LOADED : "resultsLoaded",
    SUGGESTIONS_LOADED : "suggestionsLoaded",
    LOADED_BOTH : "loadedBoth",
    PARAMETRICS_LOADED: "parametricsLoaded",
    SEARCH_VIEW_CHANGED: "searchViewChanged",
    FOLDERS_LOADED: "foldersLoaded"
};

SearchEvents.getSelectedSearchView = function() {
    return $('input[name=searchView]:checked', '#searchControlForm').val();
};

SearchEvents.getSearchErrorMessage = function(errMsg, errDetail) {
    var cause = errDetail || errMsg;
   // return "";
    return _.template("<span class='search-error' data-toggle='tooltip' title='<%- error.cause %>'><%- error.message %></span>", {message: errMsg, cause: cause}, {variable: 'error'});
}

SearchEvents.updateResultViewOptions = function() {
    var view = SearchEvents.getSelectedSearchView();
   
  //  $('#resultViewSelect').find('option[value=simple]').text(SearchConfig.searchViews[view].resultView.simple);
   // $('#resultViewSelect').find('option[value=fieldcheck]').text(SearchConfig.searchViews[view].resultView.fieldCheck);
    
    $('#resultViewSelect option').remove();
    
    if(SearchConfig.searchViews[view].resultView.simple) {
    	if($('#resultViewSelect').find('option[value=simple]').length > 0) {
    		$('#resultViewSelect').find('option[value=simple]').text(SearchConfig.searchViews[view].resultView.simple);
    	} else {
    		$('#resultViewSelect').append(new Option(SearchConfig.searchViews[view].resultView.simple, "simple"));
    	}    	
    }
    
    if(SearchConfig.searchViews[view].resultView.fieldCheck) {
    	if($('#resultViewSelect').find('option[value=fieldcheck]').length > 0) {
    		$('#resultViewSelect').find('option[value=fieldcheck]').text(SearchConfig.searchViews[view].resultView.fieldCheck);
    	} else {
    		$('#resultViewSelect').append(new Option(SearchConfig.searchViews[view].resultView.fieldCheck, "fieldcheck"));
    	}
    	
    }
   
    if(SearchConfig.searchViews[view].resultView.measurement3) {
    	if($('#resultViewSelect').find('option[value=measurement3]').length > 0) {
    		$('#resultViewSelect').find('option[value=measurement3]').text(SearchConfig.searchViews[view].resultView.measurement3);
    	} else {
    		$('#resultViewSelect').append(new Option(SearchConfig.searchViews[view].resultView.measurement3, "measurement3"));
    	}
    	
    }
    if(SearchConfig.searchViews[view].resultView.measurement4) {
	    if($('#resultViewSelect').find('option[value=measurement4]').length > 0) {
    		$('#resultViewSelect').find('option[value=measurement4]').text(SearchConfig.searchViews[view].resultView.measurement4);
    	} else {
    		$('#resultViewSelect').append(new Option(SearchConfig.searchViews[view].resultView.measurement4, "measurement4"));
    	}    	
    }
    if(SearchConfig.searchViews[view].trending) {
	    trendingCFG=SearchConfig.searchViews[view].trending;
    	if (trendingCFG) {
    		if (trendingCFG.enabled==true) {
    			$('#trending-tab-select').show();
    		} else {
    			$('#trending-tab-select').hide();  		
    			if ($('#trending-tab-select').hasClass('active')) {
    				$('#trending-tab-select').removeClass('active');
    				// $('#filterchart').addClass('active');
    				$("#filterchart-tab-anchor" ).trigger( "click" );
    			}
    		}
    	}
    	
 
    }
}

SearchEvents.getFormattedResult = function(total) {
	var formattedNum = total.toString();	
	formattedNum = parseFloat(Math.round(formattedNum + 'e2') + 'e-2');
	formattedNum = formattedNum.toString();
	
	var res = formattedNum.split(".");	 
	if(formattedNum.indexOf('.') != -1) {	 
	  if(res[1].length < 2) {	  	
	  	formattedNum = Number(formattedNum).toFixed(2);
	  }
	}
	formattedNum = formattedNum.toString();
	
	var rgx = /(\d+)(\d{3})/;
	while (rgx.test(formattedNum)) {
		formattedNum = formattedNum.replace(rgx, '$1' + ',' + '$2');
	}
	
	return formattedNum;
	
}

SearchEvents.formEditDisabled = function() {
	return $('#parametricForm').hasClass('edit-disabled');
}



SearchEvents.$ = jQuery(SearchEvents);
SearchEvents.$.parametricLoaded = false;