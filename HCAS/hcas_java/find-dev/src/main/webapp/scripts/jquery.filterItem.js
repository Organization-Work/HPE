tempID ;
var tempID  =  this.versionObj;
	
﻿var FILTER_FIELDS_TYPE = {
    DATE:  {template: '../templates/search/filters.date'+tempID+'.template', widgetName: "dateFilterItem"},
    DOCUMENTFOLDER: {template: '../templates/search/filters.documentfolder'+tempID+'.template', widgetName: "documentfolderFilterItem"},
    MATCH: {template: '../templates/search/filters.match'+tempID+'.template', widgetName: "matchFilterItem"},
    NUMERIC: {template: '../templates/search/filters.numeric'+tempID+'.template', widgetName: "numericFilterItem"},
    PARARANGES: {template: '../templates/search/filters.match'+tempID+'.template', widgetName: "matchFilterItem"},
    TEXT: {template: '../templates/search/filters.match'+tempID+'.template', widgetName: "matchFilterItem"},
    ASOFDATE: {template: '../templates/search/filters.date'+tempID+'.template', widgetName: "dateFilterItem"},
    MONTHPART: {template: '../templates/search/filters.monthpartition'+tempID+'.template', widgetName: "monthpartitionFilterItem"},
    HASFILTER: {template: '../templates/search/filters.match'+tempID+'.template', widgetName: "matchFilterItem"},
};


var FILTER_FIELD_SELECTOR = '.filter-field';
var FIELD_OPERATOR_SELECTOR = 'select.field-operator';
var FILTER_FIELD_DATA = 'filterfield';
var TEXT_AREA_SELECTOR = 'textarea.peepTextarea.tt-input';
var TOGGLE_BUTTON_SELECTOR = '.addMultifilter';

var $filtersContextMenu = $('#filtersContextMenu');
var $pasteMenuItems = $filtersContextMenu.find('.context-menu-item.paste-item');

;(function($, window, document, undefined) {   

    $.widget('parametric.filterItem', {
        options: {
            data: null,
            disableUngroup: true,
            disableGrouping: false
        },
        
        getValue: function(includeBlank) {            
            var filterFieldVal =  this.filterFieldWidget.getValue(includeBlank);
            
           var val = null;
               if (filterFieldVal || includeBlank) {
                val = {};
                val.type = $(this).data(FILTER_FIELD_DATA);
                val.op = this.element.find(FIELD_OPERATOR_SELECTOR).val();
              
                val.val = filterFieldVal;
                
                
                if( this.filterFieldWidget.element.find(TEXT_AREA_SELECTOR)
            			&& this.filterFieldWidget.element.find(TEXT_AREA_SELECTOR).css("display") 
            			&& this.filterFieldWidget.element.find(TEXT_AREA_SELECTOR).css("display") != 'none'
            			&& this.filterFieldWidget.element.find(TEXT_AREA_SELECTOR).css("display") != '') {
                	var return_val = this.filterFieldWidget.element.find(TEXT_AREA_SELECTOR).val().trim().split("\n");
                	val.val = null; // clear the single value from the request
                	val.listVal = return_val; // populate the multi value list
                	
            	} 
            }
            
            return val;
        },
        
        getName: function() {
            return this.id;
        },
        
        toString: function() {
        	return this.options.data.displayName + "=[" + this.filterFieldWidget.getValue(true) +"]";
        
        },
        
        toggleUngroup: function(enable) {
            if (enable) {
                this.element.find('.ungroup').removeClass('disabled');
            } else {
                this.element.find('.ungroup').addClass('disabled');
                
            }
        },
        
        toggleGrouping: function(enable) {
        	if (enable) {
        		this.element.find('a.grouping').prop('disabled', false).find('.icon').removeClass('disabled').prop('title', 'Group/Ungroup');
        	} else {
        		this.element.find('a.grouping').prop('disabled', true).find('.icon').addClass('disabled').prop('title', 'Grouping not allowed');
        	}
        },
        
        _createFilterField: function(filterFieldType) {
            var template = FILTER_FIELDS_TYPE[filterFieldType].template;

            return $.resGet(template);
            
        },
        
        _loadFilterField: function(filterFieldType, fieldOpVal, dataValue) {
            
            this.element.find(FILTER_FIELD_SELECTOR).remove();
            this.filterFieldWidget =  null;

            var $filterField = this._createFilterField(filterFieldType);
            var fieldWidgetName = FILTER_FIELDS_TYPE[filterFieldType].widgetName;
            
            // display the toggle button only for text fields
            if(filterFieldType == 'MATCH' || filterFieldType == 'TEXT' || filterFieldType == 'PARARANGES') {
            	this.element.find(TOGGLE_BUTTON_SELECTOR).show();
            } else {
            	this.element.find(TOGGLE_BUTTON_SELECTOR).hide();
            }
                        
            this.element.append($filterField);
            var $filterFieldContainer = this.element.find(FILTER_FIELD_SELECTOR);
            
            if (!fieldOpVal) {
               fieldOpVal = FilterTypeOperators[filterFieldType][0];
 				if (filterFieldType == "NUMERIC") {
					fieldOpVal="EQ";
				}
                this.element.find(FIELD_OPERATOR_SELECTOR).val(fieldOpVal);
            } 
            
            var fieldOp = FILTER_OPERATORS[fieldOpVal];
            
            this.element.find(FIELD_OPERATOR_SELECTOR).val(fieldOpVal);

            $(this).data(FILTER_FIELD_DATA, this.filterFieldsMap[filterFieldType]);

            var $filterFieldWidget = $filterFieldContainer[fieldWidgetName]({data: {parentId: this.id, fieldType: filterFieldType, fieldOp: fieldOp, filterValue: dataValue, fieldMeta: this._getFieldMeta()}});
            this.filterFieldWidget = $filterFieldWidget.data(fieldWidgetName);
            

            return this.filterFieldWidget;
            
        },
        
        _getFieldMeta: function() {
            var view = SearchEvents.getSelectedSearchView();
            var filter = FilterFields[view][this.id];
            
            var filterFieldType = $(this).data(FILTER_FIELD_DATA);
            var meta = {filterFieldType: filterFieldType};
            
            switch(filterFieldType) {
                case PARAMETRIC_FIELDTYPE:
                  return _.extend(meta, filter.parametric);
                case INDEXED_FIELDTYPE:
                    _.extend(meta, filter.indexed);
                case CUSTOM_FIELDTYPE:
                    _.extend(meta, filter.custom);
                default:
                    return meta;
            }

        },
        
        _create: function() {
            
            var widget = this;
            var data = this.options.data;
            
            this.id = data.name;
            this.whengrp = this.options.whengrp;
                        
            var dataFilterFieldType = null;
            
            // create filter field.
            var view = SearchEvents.getSelectedSearchView();
            var filter = FilterFields[view][data.name];
            
            this.filterFieldsMap = {};
            if (filter.parametric) {
                this.filterFieldsMap[filter.parametric.type] = PARAMETRIC_FIELDTYPE;
                if (data.fieldType === PARAMETRIC_FIELDTYPE) {
                    dataFilterFieldType =  filter.parametric.type;   
                }
            }
            if (filter.indexed) {
                this.filterFieldsMap[filter.indexed.type] = INDEXED_FIELDTYPE;
                if (data.fieldType === INDEXED_FIELDTYPE) {
                    dataFilterFieldType =  filter.indexed.type;   
                }
            }   
            if (filter.custom) {
                this.filterFieldsMap[filter.custom.type] = CUSTOM_FIELDTYPE;                
                if (data.fieldType === CUSTOM_FIELDTYPE) {
                    dataFilterFieldType =  filter.custom.type;   
                }
            }
            
            // Default to select parametric first, then indexed, then custom field.
            var filterField = filter.parametric || filter.indexed || filter.custom;
            var filterFieldType = dataFilterFieldType || filterField.type;
            
            this._loadFilterField(filterFieldType, data.fieldOp, data.filterValue);
            
            if (!SearchEvents.formEditDisabled()) {
            	this._registerControlEvents();
          	}
                       
        },
        
        _registerControlEvents: function() {
        		var widget = this;
        	if (!this.options.disableGrouping) {
            	this.toggleUngroup(!this.options.disableUngroup);
            } else {
            	this.toggleGrouping(false);	
            }
            this.element.on('contextmenu', '.filter-contextmenu', function(event) {
            	if ($(event.target).hasClass('filter-contextmenu')) {
            		FilterContextmenu.showMenu(widget, event.pageX, event.pageY - 45);
            	}
            	return false;   
			});
            
            this.element.on('click', '.add-subgroup', function(event) {
                var items = widget._getSelectedFilters(this);
                widget.element.trigger(FilterEvents.NEW_GROUP, items);
                
            });
            this.element.on('click', '.ungroup', function(event) {
                if ($(this).hasClass('disabled')) {
                    event.preventDefault(); 
                } else {
                    var items = widget._getSelectedFilters(this);
                    widget.element.trigger(FilterEvents.UNGROUP, items);
                }
           });
            this.element.on('click', '.filter-close', function(event) {
        		event.preventDefault();
            var items = widget._getSelectedFilters(this);
            widget.element.trigger(FilterEvents.DELETE, items);
            });
            
            this.element.on('click', '.filter-contextmenu', function(event) {            		
	            	if ($(event.target).hasClass('filter-contextmenu')) {
	            	event.preventDefault();
	                //only allow to select in one group at a time.
	                var $selectedGroupItems = widget.element.closest('fieldset.filter-group').find('> .filters-list > .filter-item.selected');
	                var $selectedItems = $('.filter-item.selected');
	                $selectedItems.not($selectedGroupItems).removeClass('selected');
                	widget.element.toggleClass('selected');
	            	}
            });
            this.element.on('change', FIELD_OPERATOR_SELECTOR, function(event) {
                var selOption = $(this).find('option:selected');
                var selFieldType = selOption.data('fieldtype');
                var widgetFieldType = widget.filterFieldWidget.getFieldType();
                                
                if($(this).val()=="IS_RANGE" || $(this).val()=="IS_NOT_RANGE"){
					 $(this).parent('div').next('a').show();
				 }else if($(this).val()=="IS" || $(this).val()=="IS_NOT"){
					 $(this).parent('div').next('a').show();
				 }else if($(this).val()=="CONTAINS" || $(this).val()=="NOT_CONTAINS"){
					 $(this).parent('div').next('a').show();
				 } else {
					 $(this).parent('div').next('a').hide();
				 }
                if (selFieldType === widgetFieldType) {
                    // still ok, call child to update operator change.
                    var selOperator = FILTER_OPERATORS[selOption.val()];
                    widget.filterFieldWidget.updateFieldOperator(selOperator);
                } else {
                    // create new fieldFilter.
                    var isLastEmpty = _.isEmpty(widget.getValue());
                    
                    widget._loadFilterField(selFieldType, selOption.val());
                    
                    if (!isLastEmpty) {
                        widget.element.trigger(FilterEvents.UPDATE, this);
                    }
                }
                return false;
            });
           
            this.element.on('click', '.removeMultiFilter', function() {
         	   $(this).addClass('addMultifilter').removeClass('removeMultiFilter');
         	   $(this).find('span').addClass('icon-chevron-down').removeClass('icon-chevron-up');
               $(this).next('div.filter-field').find('textarea').removeClass("inputTextareLarge");
               $(this).next('div.filter-field').find('textarea').scrollTop(0);
         	   	
            });
            
           this.element.on('click', '.addMultifilter', function() {
        	   $(this).addClass('removeMultiFilter').removeClass('addMultifilter');
        	   $(this).find('span').addClass('icon-chevron-up').removeClass('icon-chevron-down');
               $(this).next('div.filter-field').find('textarea').addClass("inputTextareLarge");
               $(this).next('div.filter-field').find('textarea').scrollTop(0);
        	 });
        },
        
        _getSelectedFilters: function(targetElement) {
            var $this = $(targetElement);
            var $selectedGroupItems = $this.closest('.filter-item').hasClass('selected') ?
                $this.closest('fieldset.filter-group').find('> .filters-list > .filter-item.selected') :
                this.element;
                
            return [$selectedGroupItems];
        },
        
        _getSelectedContextFilters: function(targetElement) {
            var $this = $(targetElement);
            var $thisRoot = $this.closest('.filter-item');
            var $selectedGroupItems = $thisRoot.hasClass('selected') ?
                $this.closest('fieldset.filter-group').find('> .filters-list > .filter-item.selected') :
                $thisRoot;
            
            return $selectedGroupItems;
        },
        

        _isDefined: function(str) {
            return str !== null && str !== undefined;
        },
        
        _eqaulIgnoreCase: function(str1, str2) {
            if (this._isDefined(str1)) {
                return this._isDefined(str2) ? str1.toLowerCase() === str2.toLowerCase() : false;
            }  
            
            return false;
        },
        
        
        destroy: function() {
        },
        
        handleCut: function() {
        	var $items = this._getSelectedFilters(this.element);
        	
        	this.element.trigger(FilterContextmenu.CUT, $items);
        	
        },
        
        handleCopy: function() {
        	var $items = this._getSelectedFilters(this.element);
        	
        	this.element.trigger(FilterContextmenu.COPY, $items);
        },
        
        handlePaste: function() {
        	this.element.trigger(FilterContextmenu.PASTE, this.element);
        	
        },
        
        handleGroupPaste: function() {
        	this.element.trigger(FilterContextmenu.PASTE_AS_GROUP, this.element);
        },
        
        setWhengrp: function(grp) {
        	this.whengrp = grp;
        },
        
        getWhengrp: function() {
        	return this.whengrp;
        }
        
    });

})(jQuery, window, document);
 
