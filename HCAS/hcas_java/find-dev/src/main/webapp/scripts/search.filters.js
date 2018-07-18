var FILTER_OPERATORS = {
    EQ: {label: "=", value: "EQ"},
    NE: {label: "!=", value: "NE"},
    GT: {label: ">", value: "GT"},
    GE: {label: ">=", value: "GE"},
    LT: {label: "<", value: "LT"},
    LE: {label: "<=", value: "LE"},
    RANGE: {label: "between", value: "RANGE", dataType: "operand"},
    BEFORE: {label: "before", value: "BEFORE"},
    AFTER: {label: "after", value: "AFTER"},
    BETWEEN: {label: "between", value: "BETWEEN", dataType: "operand"},
    PERIOD: {label: "in", value: "PERIOD", dataType: "period"},
    IS: {label: "is", value: "IS"},
    IS_NOT: {label: "is not", value: "IS_NOT"},
    IS_RANGE: {label: "is", value: "IS_RANGE"},
    IS_NOT_RANGE: {label: "is not", value: "IS_NOT_RANGE"},
    CONTAINS: {label: "contains", value: "CONTAINS"},
    NOT_CONTAINS: {label: "not contains", value: "NOT_CONTAINS"},
    AS_OF_DATE: {label: "in", value: "AS_OF_DATE"},
    MONTH_PART: {label: "before", value: "MONTH_PART"},
};

var PARAMETRIC_FIELDTYPE = 'P';
var INDEXED_FIELDTYPE = 'I';
var CUSTOM_FIELDTYPE = 'C';


jQuery(function ($) {
	
	var tempID  =  this.versionObj;
	var tempID =  localStorage.getItem("storageId");
		    
    var searchViewsTpl = _.template($.resGet("../templates/filters.searchViews.template"));
    
    var sortedViews = _.sortBy(_.values(SearchConfig.searchViews), "displayName");
    var searchViewsControlHtml = searchViewsTpl({ searchViews: sortedViews });
    $('#searchControlForm .search-views-control').empty().append(searchViewsControlHtml); 
    
   
    
    $('.search-views-control').on('change', 'input[name=searchView]', function(event) {
        $(this).trigger(SearchEvents.SEARCH_VIEW_CHANGED);
        SearchEvents.updateResultViewOptions();
                 
    });
    
    var hashValue = $.parseJSON(decodeURI(window.location.hash.substring(1)) || '{}');
    
    var searchViewsInput = $('#searchControlForm input[name=searchView]');
    var checkedSearchView = hashValue.searchView || SearchConfig.defaultSearchView;
    
    _.some(searchViewsInput, function(viewInput) {
        var isChecked = $(viewInput).val() === checkedSearchView;
        if (isChecked) {
            $(viewInput).attr('checked', 'checked');
            SearchEvents.updateResultViewOptions();
        }
        return isChecked;
    });
   
    
    var $fieldlistControl = $('a.fieldlist-control');
    $fieldlistControl.fieldlistControl(); 

    var $parametricForm = $('#parametricForm').filterForm({fieldlistControl: $fieldlistControl.data('fieldlistControl')});
    
    var filtergroupTemplate = $.resGet('../templates/search/filters.filtergroup'+tempID+'.template');
    $parametricForm.append($(filtergroupTemplate));
    $('fieldset.filter-group').filtergroup({isRoot: true, fieldlistControl: $fieldlistControl.data('fieldlistControl')});
    
    $('.filter-tabs a[href="#advancedFilters"]').tab('show');

    // Update filters on browser refresh
    SearchEvents.$.on(SearchEvents.PARAMETRICS_LOADED, function(event, obj){
        if(!obj.isViewChange) {
            //var value = $.parseJSON(window.location.hash.substring(1) || '{}');
            if (hashValue.userSearchSettings && hashValue.userSearchSettings.combine) {
                $('#resultViewSelect').val(hashValue.userSearchSettings.combine);
            }
            if (hashValue && hashValue.filterGroup) {
            	SearchEvents.setQuery(hashValue.query);
                $('#parametricForm').find('.rootFilterGroup').data('filtergroup').loadFilters(hashValue.filterGroup);
            }
        }
    });
    
    var $fieldlistWidget = $('a.fieldlist-control').data('fieldlistControl');
    SearchEvents.getFieldValues = function(id) {
        return $fieldlistWidget.getFieldValues(id);
        
    };
});