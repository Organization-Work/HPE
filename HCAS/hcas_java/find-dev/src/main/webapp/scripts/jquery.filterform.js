;(function($, window, document, undefined) {
	var tempID  =  this.versionObj;
		
    $.widget('parametric.filterForm', {
        options: {
            fieldlistControl: null
        },
        
        addFilter: function(data, search) {
            this._activateFiltersTab();
            var $activeGroup = this._getActiveFilterGroup();
            $activeGroup.addFilter(data, search);

        },
        
        getFiltersData: function() {
            //var $activeGroup = this._getActiveFilterGroup();
            var $rootGroup = this.element.find('> fieldset.filter-group').data('filtergroup');
            
            return $rootGroup.getFiltersData();
            
        },
        
        getFiltersDataString: function() {
            var $rootGroup = this.element.find('> fieldset.filter-group').data('filtergroup');
            
            return $rootGroup.getFiltersDataString();
            
        },
        
        
        handleFilterSetSave: function($group) {
            var filtersData = $group.getFiltersData(true);
            
            var $dialog = $('.filter-dialog');
            $dialog.filtersDialog({filtersData: filtersData, isLoad: false, title: 'Save Custom Filter Set'});
            
        },
        
        loadFilter: function(data) {
        	var allfiltersData = this.getFiltersData();
        	var add = true;
        	
            var fieldData = this.options.fieldlistControl.getFieldValues(data.id);
            var filterData = _.extend(fieldData, {
                filterArray: $.isArray(data.value) ? data.value : [data.value],
                fieldType: data.fieldType
            });
           /* _.map(allfiltersData.filterFields, function (field) {
            	if(field[data.id]) {
            		_.each(field[data.id], function (fieldDetails) {
            			if(fieldDetails.val === data.value) {
            				add = false;
            			}
            		});
            	}
            });*/
            if(add) {
            	this.addFilter(filterData, true);
            }         
            
        },
        
        getAllParaFilters: function() {
            var paraFilters = {};
            $('.filter-item').each(function() {
                var widgetName = $(this).data('widgetName');
                var $itemWidget = $(this).data(widgetName);
                if ($($itemWidget).data('filterfield') == PARAMETRIC_FIELDTYPE) {
                    if ($itemWidget.getValue()) {
                        var name = $(this).find('span.filter-label').text();
                        paraFilters[name] = $itemWidget.getName();
                    }
                }
                
            });
            
            return paraFilters;
        },
        
        
        _create: function() {
            var $widget = this;
            this.element.on(FilterEvents.GROUP_SAVE, function(event, data) {
                var $group = $(event.target).data('filtergroup');
                $widget.handleFilterSetSave($group);
                
            });
            
            $('.search-controls').on(SearchEvents.RESET, function(event, data) {
                
                var filtergroupTemplate = $.resGet('../templates/search/filters.filtergroup'+tempID+'.template');
                $widget.element.find('fieldset.filter-group').eq(0).remove();
                $widget.element.append($(filtergroupTemplate));
                var $rootGroup = $('fieldset.filter-group');
                $rootGroup.filtergroup({isRoot: true, fieldlistControl: $('a.fieldlist-control').data('fieldlistControl')});
                
                $rootGroup.trigger(FilterEvents.GROUP_RESET_DONE);
                
                return false;
                
            });
            
            
            if (!this.element.hasClass('edit-disabled')) {

            	FilterContextmenu.initMenu();
            }
            
            this.element.on('click', 'fieldset.filter-group', this, this._onHighlightGroup);
            
        },
        
        insertLoadingFilterSet: function() {
            this._activateFiltersTab();
            var activeGroup = this._getActiveFilterGroup();
            return activeGroup.insertLoadingFilterSet();
        },
        
        _getActiveFilterGroup: function() {
            var $activeGroup = $('.active-filtergroup');
            if ($activeGroup.length === 0) {
                $activeGroup = this.element.find('> fieldset.filter-group');
            }

            return $activeGroup.data('filtergroup');            
        },
        
        
        _onHighlightGroup: function(event) {
            var $clickedFieldset = $(event.target).closest('fieldset.filter-group');
            if ($clickedFieldset.find('> legend select').val() === 'WHEN') {
            	return;
            }
            
            if ($clickedFieldset && !$clickedFieldset.hasClass('active-filtergroup')) {
                $('.active-filtergroup').removeClass('active-filtergroup group-highlight');
                $('.filter-item.selected').removeClass('selected');
                $clickedFieldset.addClass('active-filtergroup');
                if (!$clickedFieldset.hasClass('rootFilterGroup')) {
                    $clickedFieldset.addClass('group-highlight');
                }
            } 
            
        },
        
        _activateFiltersTab: function() {
            var $filtersTab = $('#filtersTab');
            if ($filtersTab[0].id !== $('.filter-tabs li.active')[0].id) {
                $filtersTab.find('a').click();
            } 
            
        },

        
        destroy: function() {
        }
    });

})(jQuery, window, document);
 