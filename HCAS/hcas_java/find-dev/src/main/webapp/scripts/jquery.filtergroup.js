(function($, window, document, undefined) {
	
	var tempID  =  this.versionObj;
		
	var NON_PARAMETRIC_WIDGET_MAP = {
        date: 'dateFilterItem',
        monthpart: 'monthpartitionFilterItem',
        match: 'matchFilterItem',
        numeric: 'numericFilterItem',
        documentfolder: 'documentfolderFilterItem'
    };
    
    var DATA_COHORT = "cohortop";
    var COHORT_LINK = "a.group-cohort";
    var COHORT_CONTAINER = "div.group-cohort";
    var COHORT_ICON = ".icon-cohort";
    var COHORT_OPTIONS = ".cohort-options";
    var COHORT_CONFIGS = {
        PT_MATCH: {data: "PT_MATCH", img: "../img/cohort_14x16_plus.png"},
        PT_EXCLUDE: {data: "PT_EXCLUDE", img: "../img/cohort_14x16_minus.png"},
        PT_NONE: {data: "PT_NONE", img: "../img/cohort.png"}
    };
    
    var DEACTIVATED_CLASS = "deactivated";
    
    var FILTER_WIDGET_NAME = "filterItem";
    
    var WHEN_GROUP_OPERATOR = "WHEN";
    
    var TEXT_INPUT_SELECTOR = 'input.tt-input';
    var TEXT_INPUT_SELECTOR_HINT = 'input.tt-hint';
	var TEXT_AREA_SELECTOR = 'textarea.peepTextarea';
	var TEXT_AREA_TOGGLE = '.addMultifilter';

    $.widget('parametric.filtergroup', {
        options: {
            fieldlistControl: null,
            containers: {
                root: 'form.filters',
                group: 'fieldset.filter-group',
                item: '.filter-item',
                filters: '> .filters-list'
            },
            controls: {
                collapse: '> legend a.group-collapse',
                boolOperator: '> legend select',
                tag: '> legend a.grouptag',
                tagInput: '> legend input.grouptag',
                close: '> legend div.group-close',
                save: '> legend a.group-save',
                cohort: '> legend div.group-cohort'
            },
            filterTemplate: '../templates/filters.item.template',
            filterFieldTemplate: {
                date:  '../templates/search/filters.date'+tempID+'.template',
                monthpart:  '../templates/search/filters.monthpartition'+tempID+'.template',
                documentfolder: '../templates/search/filters.documentfolder'+tempID+'.template',
                match: '../templates/search/filters.match'+tempID+'.template',
                numeric: '../templates/search/filters.numeric'+tempID+'.template',
                text: '../templates/search/filters.match'+tempID+'.template'
                
            },
            groupTemplate: '../templates/filters.filtergroup.template',
            isRoot: false
        },

        /**
         * Public methods
         */
        addFilter: function(data, search) {
            if (!data.filterArray) {
                data.filterArray = [data.filterValue];
            }

            for (var i = 0; i < data.filterArray.length; i++) {
                data.filterValue = data.filterArray[i];

                var view = SearchEvents.getSelectedSearchView();
                var filterField = FilterFields[view][data.name];
                
                var $filter = this._createFilterItem(filterField);
                
                //var filterWidgetName = NON_PARAMETRIC_WIDGET_MAP[data.type] || 'filterItem';
                $filter[FILTER_WIDGET_NAME]({data: data, disableUngroup: this.options.isRoot, whengrp: this.getWhengrp()});
                
                $filter.data('widgetName', FILTER_WIDGET_NAME);

                this._getFiltersContainer().append($filter);
                if (!data.filterValue) {
                    $filter.find('input').focus();
                }
                this._clearClass($filter, 'lastAdded');
            }

            if (search) {
                this.element.trigger(FilterEvents.GROUP_UPDATE);
            }
                      
        }, 
        
        insertLoadingFilterSet: function() {
            var groupTpl = $.resGet(this.options.groupTemplate);
            var $addedGroup = $(groupTpl);
            this._getFiltersContainer().append($addedGroup);
            $addedGroup.filtergroup({fieldlistControl: this.options.fieldlistControl});
            $addedGroup.find('.group-loader').show();
            
            return $addedGroup.data('filtergroup');

        },
        
        loadFilters: function(data, filterName, filterPath, skipSearch) {
            if (!data) {
                return;
            }
            
            var $this = this;
            var $element = this.element;
            
            $element.find('.group-loader').hide();
            
            // set boolop
            var controls = this.options.controls;
            
            var disableGrouping = false;
            
            if (data.boolOperator) {
            	var $boolOp = $element.find(controls.boolOperator);
            	if ($boolOp) {
            		$boolOp.val(data.boolOperator);
            		if (data.boolOperator === WHEN_GROUP_OPERATOR) {            			
            			$boolOp.find('option').toggleClass('hidden', true);
            			$boolOp.find('option[value="WHEN"]').toggleClass('hidden', false);
            			$element.data('whenop', data.whenOp);
            			$element.data('whengrp', data.whenGroup);
            			
            			disableGrouping = true;            			           			
            		}
            	}
                
            }
            
            // set tag input
            var tagVal = data.tag || filterName;
            
            if (tagVal) {
                var $tag = $element.find(controls.tag);
                var $tagInput = $element.find(controls.tagInput);
                $tag.addClass('hidden');
                $tagInput.removeClass('hidden');
                $tagInput.val(tagVal);
                
                // add the tooltip
                if (filterPath) {
                    $tagInput.addClass('saved-filterset');
                    $tagInput.data('toggle', 'tooltip');
                    $tagInput.attr('title', filterPath);
                }
                $tagInput.blur();
            }
            
            // set cohortOp
            var $cohortControl = this.element.find(controls.cohort);
            if (data.cohortOp && $cohortControl.length > 0) {
                var selector = '.cohort-options[name="' + data.cohortOp + '"]';
                var $cohortElement = $cohortControl.find(selector);
                if ($cohortElement.length > 0) {
                    this._toggleCohortSelect($cohortElement[0]);
                }
            }
            

            // load filters
            var $fieldlistControl = this.options.fieldlistControl;

            var $childFilters = this._createChildFilters(data.filterFields);
            
            this._getFiltersContainer().append($childFilters);
            
            var fieldlistControl = this.options.fieldlistControl;
            
            // load childGroups
            if (data.childGroups) {
                _.each(data.childGroups, function(subGroupData) {
                    var groupTpl = $.resGet($this.options.groupTemplate);
                    var $subGroup = $(groupTpl);
                    $this._getFiltersContainer().append($subGroup);
                    $subGroup.filtergroup({fieldlistControl: fieldlistControl}).data('filtergroup').loadFilters(subGroupData);
                        
                });
            }
            
            // Check deactivate
            if (data.deactivated) {
                this._toggleDeactivated(false);
            } else if (!skipSearch) {
            
                // Do search?
                $element.trigger(FilterEvents.GROUP_UPDATE);
            }
            
        },
        
        getBoolOpValue: function() {
            return this.element.find(this.options.controls.boolOperator).val();
        },
        
        getFiltersData: function(includeBlank) {
            var data = {};
            
            if (this._isDeactivated() && !includeBlank) {
                return data;
            }
            
            var childGroupsData = this._getChildGroupsData(this._getChildGroups(), includeBlank);
            
            var fieldChildFiltersData = this._getChildFiltersData(this._getFieldChildFilters(), includeBlank);

            if (!_.isEmpty(fieldChildFiltersData)) {
                data.filterFields = fieldChildFiltersData;
            }
            
            if (!_.isEmpty(childGroupsData)) {
                data.childGroups = childGroupsData;
            }
                        
                        
            if (!_.isEmpty(data)) {
                data.boolOperator = this.getBoolOpValue();
                
                if (data.boolOperator === WHEN_GROUP_OPERATOR) {
                	data.whenOp = this.element.data('whenop');
                	data.whenGroup = this.element.data('whengrp');
                }
                
                data.tag = this.element.find(this.options.controls.tagInput).val();
                
                if ($(this).data(DATA_COHORT)) {
                    data.cohortOp = $(this).data(DATA_COHORT);
                }
                
                if (this._isDeactivated()){
                    data.deactivated = true;
                }
            }
            
            return data;
        },
        
        
        getFiltersDataString: function() {
        	var data = '';
        	
            if (this._isDeactivated()) {
                return data;
            }

        	var childrenSelector = '> ' + this.options.containers.item + ',' + this.options.containers.group;
        	var $children = this._getFiltersContainer().find(childrenSelector);
        	var $this = this;
        	
        	var operator = this.getBoolOpValue();
        	var match = operator.match(/^NOT_(AND|OR)/);
        	var boolOperator = match ? match[1] : operator;
        	var notOperator = match ? 'NOT' : null;
        	
        	
        	var tag = this.element.find(this.options.controls.tagInput).val();
        	
        	var data = '';
        	
        	$children.each(function(index, ele) {
        		if (index > 0) {
        			data = data + ' ' + boolOperator + ' ';
        		}

        		if ($(ele).hasClass('filter-group')) {
	                var $eleGroup = $(ele).data($this.widgetName);
	                var groupData = $eleGroup.getFiltersDataString();
	                if (groupData) {
	                	data += groupData;
	                    	
	                }
        			
        		} else {
	        		var itemWidget = $this._getItemWidget($(ele));
	        		data += itemWidget.toString();
        		}
        	});
        	
        	if (data) {
        		data = '(' + data + ')';
        	}
        	
        	if (notOperator) {
        		data = notOperator + ' ' + data;
        	}
        	
            if ($(this).data(DATA_COHORT)) {
                data = $(this).data(DATA_COHORT) + ' ' + data;
            }
        	
        	
        	return data;
        },
               
        getWhengrp: function() {
        	return this.element.data('whengrp');
        },
        
        handlePaste: function() {
        	this._pasteFilter(null, false, true);
        },
        
        handleGroupPaste: function() {
        	this._pasteFilter(null, true, true);
        },
        
        _destroy: function() {
        },
        
        
        /**
         * Private methods        
         */
        _create: function() {
            if (this.options.isRoot) {
                this.element.addClass('rootFilterGroup');
                this.element.find(this.options.controls.close).remove();
                this.element.find(this.options.controls.cohort).remove();
                //this.element.find(this.options.controls.load).show();
                
            } else if (!this._isParentRoot()) {
                this.element.find(this.options.controls.cohort).remove();
            } 
            
            if (!SearchEvents.formEditDisabled()) {
	            // register control events
	            this._registerControlEvents();
	          } else {
	          	// allow collapse toggling.
	          	this.element.on('click', this.options.controls.collapse, this, this._onToggleCollpase);
	          }
                    
        },

        _registerControlEvents: function() {
            var $widget = this;
            var groupControls = this.options.controls;
            var groupContainers = this.options.containers;

            if (!this.options.isRoot) {
                var closeOptions = groupControls.close + ' .grpclose-options';
                this.element.on('click', closeOptions, function(e) {
                    e.preventDefault();
                    $widget._onCloseSelect(this);
                });
                
                if (this.element.find(groupControls.cohort).length > 0) {
                    var cohortOptions = groupControls.cohort + ' .cohort-options';
                    this.element.on('click', cohortOptions, function(e) {
                        e.preventDefault();
                        
                        $widget._onCohortSelect(this);
                        
                    });
                }
            }
            
            // listen on group control related events
            this.element.on('click', groupControls.collapse, this, this._onToggleCollpase);
            this.element.on('click', groupControls.tag, this, this._onToggleTag);
            this.element.on("focus blur", groupControls.tagInput, this, this._onToggleTagInput);
            this.element.on('change', groupControls.boolOperator, this, this._onBoolOpChange);
            this.element.on('click', groupControls.save, this, this._onSave);
            
            // listen on child filters related events.
            var childFilter = this._getChildContainerSelector(groupContainers.item);
            var childGroup = this._getChildContainerSelector(groupContainers.group);
            
            this.element.on(FilterEvents.NEW_GROUP, childFilter, function(event, data) {
                $widget._createFilterSubgroup(data);
            });
            
            this.element.on(FilterEvents.UNGROUP, childFilter, function(event, data) {
                $widget._ungroupFilter(data);
                return false;
            });
            
            this.element.on(FilterEvents.GROUP_DELETE, childGroup, function(event, data) {
                $widget._removeGroup(data);
            });
            
            
            this.element.on(FilterEvents.DELETE, childFilter, function(event, data) {
                $widget._removeFilter(data);
            });

            this.element.on(FilterEvents.UPDATE, childFilter, function(event, data) {
                $widget._updateFilter(data);
            });

            this.element.on(FilterContextmenu.COPY, childFilter, function(event, data) {
                $widget._copyFilter(data);
            });

            this.element.on(FilterContextmenu.CUT, childFilter, function(event, data) {
                $widget._cutFilter(data);
            });

            this.element.on(FilterContextmenu.PASTE, childFilter, function(event, data) {
                $widget._pasteFilter(data, false);
            });

            this.element.on(FilterContextmenu.PASTE_AS_GROUP, childFilter, function(event, data) {
                $widget._pasteFilter(data, true);
            });
            
            if (this.options.isRoot) {
                this.element.on(FilterEvents.GROUP_UPDATE, function(e) {$widget._onGroupUpdate(false, e.target);});
                this.element.on(FilterEvents.GROUP_DEACTIVATE_UPDATE, function(e) {$widget._onGroupUpdate(true, e.target);});
            } 
            
            this.element.on('contextmenu', function(event) {
            	if ($(event.target).hasClass('filter-contextmenu')) {
            		FilterContextmenu.showMenu($widget, event.pageX, event.pageY - 45);
            	}
            	
							return false;   
            });           
           
        }, 
        
        _isDeactivated: function() {
            return this.element.hasClass(DEACTIVATED_CLASS);
            
        },
        
        _toggleDeactivated: function(updateSearch) {
            var closeOptions = this.options.controls.close + ' .grpclose-options';
            var $ele = this.element.find(closeOptions).filter('[name="GRP_DEACTIVATE"]');
            $ele.toggleClass(DEACTIVATED_CLASS);
            this.element.toggleClass(DEACTIVATED_CLASS);
            if (updateSearch) {
                this.element.trigger(FilterEvents.GROUP_DEACTIVATE_UPDATE);
            }
        },
         
        
        _isParentRoot: function() {
            if (this.element.hasClass('rootFilterGroup')) {
                return false;
            }
            
            var $parentGroup = this.element.parent().closest(this.options.containers.group);
            return $parentGroup.hasClass('rootFilterGroup');
        },
        
        _getItemWidget: function($ele) {
            var widgetName = $ele.data('widgetName');
            return $ele.data(widgetName);
        },      
 
        _removeFilter: function(filterItems) {
            var $filterItems = $(filterItems);
            var $this = this;
            
            var valueChange = _.some(filterItems, function(item) {
               
                var $itemWidget = $this._getItemWidget($(item));
                return !_.isEmpty($itemWidget.getValue());
            });
            
            $filterItems.remove();

            if (valueChange) {
                // trigger filter update
                this.element.trigger(FilterEvents.GROUP_UPDATE);
            }

            this._checkEmptiness();
        },
        
        _createFilterSubgroup: function(filterItems, $targetFilterItem, filterGroupContextAction) {
            
            var groupTpl = $.resGet(this.options.groupTemplate);
            var $addedGroup = $(groupTpl);
            
            var $filterItems = $(filterItems);
            
            if (!filterGroupContextAction) {
	            var $target = $targetFilterItem || $filterItems.first();
	            $addedGroup.insertAfter($target);
	          } else {
	          	$addedGroup.appendTo(this._getFiltersContainer());
	          }
            $addedGroup.filtergroup({fieldlistControl: this.options.fieldlistControl});
            
            $filterItems.appendTo(this._getFiltersContainer($addedGroup));
            
            var valueChange = false;
            var $this = this;
            var whengrp = this.getWhengrp();
            
            $filterItems.each(function(index, ele) {
                var itemWidget = $this._getItemWidget($(ele));
	              if (!$this._isWhenGroup()) {
	              	itemWidget.toggleUngroup(true);
	              } else {
	              	itemWidget.toggleGrouping(false);
	              }
                itemWidget.setWhengrp(whengrp);
                if (!_.isEmpty(itemWidget.getValue())) {
                    valueChange = true;
                }
                
            });
            
            this._clearClass($filterItems, 'selected');
            
            if (valueChange) {
                // trigger filter update event
                this.element.trigger(FilterEvents.GROUP_UPDATE);
            }
            
            setTimeout(function () {
                $addedGroup.click();
            }, 10);            
            
        },
        
        _ungroupFilter: function(filterItems) {
            var $currentGroup = this.element;
            var $parentGroup = $currentGroup.parent().closest(this.options.containers.group);
            if ($parentGroup.length === 0) {
                return false;
            }
            
            var $filterItems = $(filterItems);
            $filterItems.appendTo(this._getFiltersContainer($parentGroup));

            var valueChange = false;
            var isRoot = !$parentGroup.hasClass('rootFilterGroup');
            
            var $this = this;
            
            $filterItems.each(function(index, ele) {
                var itemWidget = $this._getItemWidget($(ele));
                itemWidget.toggleUngroup(isRoot);
                if (!_.isEmpty(itemWidget.getValue())) {
                    valueChange = true;
                }
                
            });

            this._clearClass($filterItems, 'selected');

            this._checkEmptiness();

            if (valueChange) {
                // trigger filter update event
                $parentGroup.trigger(FilterEvents.GROUP_UPDATE);
            }
        },

        _copyFilter: function(filterItems) {
      		var data = this._getChildFiltersData(filterItems, true);
      		
			  	FilterContextmenu.setClipboard(FilterContextmenu.COPY, data, this.getWhengrp());

        },

        _cutFilter: function(filterItems) {
      		var $items = $(filterItems).detach();
      		
			  	FilterContextmenu.setClipboard(FilterContextmenu.CUT, $items, this.getWhengrp());

      		var $this = this;
      		var valueChange = false;
          $items.each(function(index, ele) {
              var itemWidget = $this._getItemWidget($(ele));
              if (!_.isEmpty(itemWidget.getValue())) {
                  valueChange = true;
              }
              
          });
          
          this._checkEmptiness();

          if (valueChange) {
              // trigger filter update event
              this.element.trigger(FilterEvents.GROUP_UPDATE);
          }
            

        },
        
        _pasteFilter: function(targetFilterItem, pasteAsGroup, filterGroupContextAction) {
        	var $this = this;
        	
        	var clipboard = FilterContextmenu.getClipboard();
        	FilterContextmenu.clearClipboard();
        	
        	if (clipboard!=null) {
	        	var $items = clipboard.items;
	        	if (FilterContextmenu.CUT === clipboard.action) {
	        		$items = clipboard.items;
	        		
	        	} else if (FilterContextmenu.COPY === clipboard.action){
		          $items = this._createChildFilters($items);
		        		
	        	} 
        	}
        	if (!$items) {
        		return;
        	}
					
					$('.filter-item.selected').removeClass('selected');
					

					if (pasteAsGroup) {
						this._createFilterSubgroup($items, targetFilterItem, filterGroupContextAction);
					} else {
      			$items.toggleClass('selected', true);
      			if (!filterGroupContextAction) {
							$items.insertAfter(targetFilterItem);
						} else {
							$items.appendTo(this._getFiltersContainer());
						}

	      		this._clearClass($items, 'selected');
	
						var $this = this;
						var isRoot = !$(this).hasClass('rootFilterGroup');
	      		var valueChange = false;
	      		var whengrp = this.getWhengrp();
	      		
	      		
	          $items.each(function(index, ele) {
	              var itemWidget = $this._getItemWidget($(ele));
	              if (!$this._isWhenGroup()) {
	              	itemWidget.toggleUngroup(isRoot);
	              } else {
	              	itemWidget.toggleGrouping(false);
	              }
	              itemWidget.setWhengrp(whengrp);
	              if (!_.isEmpty(itemWidget.getValue())) {
	                  valueChange = true;
	              }
	              
	          });
	          
	          this._checkEmptiness();
	
	          if (valueChange) {
	              // trigger filter update event
	              this.element.trigger(FilterEvents.GROUP_UPDATE);
	          }

					}
      		

        },
        
        _updateFilter: function($filterItem) {
            // trigger filter update event;
            this.element.trigger(FilterEvents.GROUP_UPDATE);
        },
        
        _removeGroup: function($groupWidget) {
            var groupIsEmpty = $groupWidget._isEmpty();
            var groupIsDeactivated = $groupWidget._isDeactivated();
            $groupWidget.element.remove();
            if (!groupIsEmpty && !groupIsDeactivated) {
                this.element.trigger(FilterEvents.GROUP_UPDATE);
            }
            
            this._checkEmptiness();
            
        },
        
        _checkEmptiness: function() {
            if (!this.element.hasClass('rootFilterGroup') && this._isEmpty()) {
                this.element.trigger(FilterEvents.GROUP_DELETE, this);
                
                return true;
            }
            
            return false;
        },
         
        _clearClass: function($elements, className, delayTime) {
            var delay = (delayTime === undefined) ? 1000 : delayTime;
            
            setTimeout(function () {
                $elements.removeClass(className);
            }, delay);            
        },
        
        _getFilterTypeOperators: function(type) {
            return _.map(FilterTypeOperators[type], function(op) {
                return FILTER_OPERATORS[op];
            });
                        
        },
         
        _createFilterItem: function(filterField) {
            var typeOperators = {};
            if (filterField.indexed) {
                typeOperators[filterField.indexed.type] = this._getFilterTypeOperators(filterField.indexed.type);
            }

            if (filterField.parametric) {
                typeOperators[filterField.parametric.type] =  this._getFilterTypeOperators(filterField.parametric.type);
            }

            if (filterField.custom) {
                typeOperators[filterField.custom.type] =  this._getFilterTypeOperators(filterField.custom.type);
            }
                        
            //var template = this.options.filterTemplate[data.type];
            var template = this.options.filterTemplate;

            var filterItemTpl = _.template($.resGet(template));
            
            return $(filterItemTpl({displayName: filterField.displayName, typeOperators: typeOperators}));
        },
                 
        _getChildGroupsData: function($groups, includeBlank) {
            var data = [];
            var $widget = this;
            if ($groups) {
                $groups.each( function(index, ele) {
                    var $eleGroup = $(ele).data($widget.widgetName);
                    var groupVal = $eleGroup.getFiltersData(includeBlank);
                    if (_.size(groupVal) > 0) {
                        data.push(groupVal);
                    }
                });
            }
            
            return data;
        },
        
        _getChildFiltersData: function($items, includeBlank) {
            var data = {};
            var $this = this;
            
            if ($items) {
                $items.each(function(index, ele) {
                    var $eleItem = $this._getItemWidget($(ele));
                    var k = $eleItem.getName();
                    var v = $eleItem.getValue(includeBlank);
                    if (includeBlank || v) {
                        data[k] = (data[k] || []).concat([v]);
                    }
                });
                
            }
            return data;
        },
               
	_createChildFilters: function(data) {
          var $childFilters = $();
          
          var $this = this;

          _.each(data, function(valueArray, filterId) {
              var itemData;
              var fieldValue = '';
              var view = SearchEvents.getSelectedSearchView();
              var multiline_mode = false;
              
              itemData = _.extend({}, FilterFields[view][filterId]);
              
              if(itemData.parametric && itemData.parametric.type && (itemData.parametric.type === 'TEXT' || itemData.parametric.type === 'MATCH' || itemData.parametric.type === 'PARARANGES')) {
            	  multiline_mode = true;            	  
              }
          	
              // display a single line field or the multi line mode field based on the number of fields
              if(valueArray.length > 1 && multiline_mode) {            	  
            	  // multi line mode            	  
            	  
                  	itemData = _.extend({}, FilterFields[view][filterId]);
                  	itemData.fieldOp = valueArray[0].op;
                  	if(valueArray.length > 1 ){	
                  	for(var i = 0; i < valueArray.length; i++) {
                      var singleVal = valueArray[i].val;                       
                     //var fieldValue = fieldValue.concat(valueArray[i].val, "\n");
                      if(singleVal) {
                    	  fieldValue = fieldValue.concat(singleVal, "\n");
                      } else {
                      	var barMultiVal = '';
                      	fieldValue = '';
                      	var ArrayListVal = valueArray[i].listVal;
                      	if(ArrayListVal) {
							if(valueArray.length > 1 ){	
                      		for(var k = 0; k < ArrayListVal.length; k++) {
                      			barMultiVal = barMultiVal.concat(ArrayListVal[k], "\n");
                               }
                      		fieldValue = fieldValue.concat(barMultiVal, "\n");
							itemData.filterValue = fieldValue;
							itemData.fieldType = valueArray[0].type;
							var $filter = $this._createFilterItem(itemData);
							$filter.removeClass('lastAdded');
							$filter[FILTER_WIDGET_NAME]({data: itemData, disableUngroup: $this.options.isRoot, disableGrouping: $this._isWhenGroup(), whengrp: $this.getWhengrp()});
							$filter.data('widgetName', FILTER_WIDGET_NAME);
							// display the multiple filter values in the multi-line mode text area  
							$filter.find(TEXT_AREA_TOGGLE).trigger("click");
							if(fieldValue == ''){
								$filter.find(TEXT_AREA_SELECTOR).val(fieldValue);
								$childFilters = $childFilters.remove($filter);
							}else{
								$filter.find(TEXT_AREA_SELECTOR).val(fieldValue);
							    //$filter.find(TEXT_INPUT_SELECTOR).val('');	// clear the single value from the request
							    $childFilters = $childFilters.add($filter);
								  }
							$.each($childFilters, function (index, item) {
								 fieldValue = fieldValue.replace($(item).find("textarea.peepTextarea").val(), '')
								});
						  } 
						} 
                      }
            	  }
                  itemData.filterValue = fieldValue;
                  itemData.fieldType = valueArray[0].type;
                  var $filter = $this._createFilterItem(itemData);
                  $filter.removeClass('lastAdded');
                  
                  $filter[FILTER_WIDGET_NAME]({data: itemData, disableUngroup: $this.options.isRoot, disableGrouping: $this._isWhenGroup(), whengrp: $this.getWhengrp()});
                  $filter.data('widgetName', FILTER_WIDGET_NAME);
                  
                  // display the multiple filter values in the multi-line mode text area  
                  $filter.find(TEXT_AREA_TOGGLE).trigger("click");
				  if(fieldValue == ''){
					  $filter.find(TEXT_AREA_SELECTOR).val(fieldValue);
					  $childFilters = $childFilters.remove($filter);
					  }else{
					$filter.find(TEXT_AREA_SELECTOR).val(fieldValue);
				   //$filter.find(TEXT_INPUT_SELECTOR).val('');	// clear the single value from the request
                   $childFilters = $childFilters.add($filter);
						  
					  };
				} 
              } else {
            	  // single line mode
              	  for(var i = 0; i < valueArray.length; i++) {
                    	itemData = _.extend({}, FilterFields[view][filterId]);
                        itemData.fieldOp = valueArray[i].op;
                        var singleVal = valueArray[i].val;
                        
                        if(singleVal) {
                        	itemData.filterValue = singleVal;
                        } else {
                        	var multiVal = '';
                        	var valueArrayListVal = valueArray[i].listVal;
                        	if(valueArrayListVal) {
                        		 for(var j = 0; j < valueArrayListVal.length; j++) {
                            		 multiVal = multiVal.concat(valueArrayListVal[j], "\n");
                                 }
                        	}                        	
                        	itemData.filterValue = multiVal;
                        }
                        
                      //  itemData.filterValue = valueArray[i].listVal;
                        itemData.fieldType = valueArray[i].type;
                        var $filter = $this._createFilterItem(itemData);
                        $filter.removeClass('lastAdded');
                      
                        $filter[FILTER_WIDGET_NAME]({data: itemData, disableUngroup: $this.options.isRoot, disableGrouping: $this._isWhenGroup(), whengrp: $this.getWhengrp()});
                        $filter.data('widgetName', FILTER_WIDGET_NAME);
                      
                        $childFilters = $childFilters.add($filter);
                      
                    }    	  	
            }
             
            	
            });
            
            return $childFilters;
					
		},   
				
        _updateSearchWithFilters: function($widget) {
            var filterData = $widget.getFiltersData();
            SearchEvents.setFilters(filterData);
            SearchEvents.attemptSearch();
            
        },
        
        _getFiltersContainer: function($ele) {
            var $groupElement = $ele || this.element;
            
            return $groupElement.find(this.options.containers.filters);
            
        },
        
        _getChildContainerSelector: function(container) {
            return this.options.containers.filters + '> ' + container;
        },
        
        _getParaChildFilters: function() {
            var childFilters = '> ' + this.options.containers.item + '.para-item';
            
            return this._getFiltersContainer().find(childFilters);
            
        },
        
        _getFieldChildFilters: function() {
            var childFilters = '> ' + this.options.containers.item;
            
            return this._getFiltersContainer().find(childFilters);
            
        },
        
        _getChildGroups: function() {
            var childGroups = '> ' + this.options.containers.group;
            
            return this._getFiltersContainer().find(childGroups);
            
        },
        
        _isEmpty: function() {
            return this._getFiltersContainer().children().length === 0;
        },
        

        /**
         * Event handlers
        */
        
        _onSave: function(event, data) {
            var $widget = event.data;
            var $tagInput = $widget.element.find($widget.options.controls.tagInput);
            //var tagInputVal = $tagInput.hasClass('hidden') ? null : $tagInput.val();
            $widget.element.trigger(FilterEvents.GROUP_SAVE, $tagInput.val());
        },
        
        _onGroupUpdate: function(forceUpdate, targetEle) {
            if (!SearchSettings) {
                return;
            }

            if (!forceUpdate && $(targetEle).hasClass(DEACTIVATED_CLASS)) {
                return;
            }
            
            if (!SearchSettings.isManualSearch()) {
                _.debounce(this._updateSearchWithFilters, 250)(this);
            } else {
                SearchSettings.toggleFilterSyncError(true);
            }
        },
        
        _onRemove: function(event) {
            var $widget = event.data;
            $widget.element.trigger(FilterEvents.GROUP_DELETE, $widget);
            //$widget.element.remove();
            //$('form.filters fieldset.filter-group').trigger(FilterEvents.GROUP_UPDATE);
        },
        
        _onToggleCollpase: function(event) {
            var $widget = event.data;
            //var $fieldset = $('#' + $widget.elementId);
            var $groupContent = $widget.element.find($widget.options.containers.filters);
            var $icon = $(this).find('i[class^="icon"]');
            var iconImg = $icon.hasClass("icon-chevron-up") ? "icon icon-chevron-down" : "icon icon-chevron-up";
            $icon.removeClass();
            $groupContent.toggle();
            $icon.addClass(iconImg);
        },
        
        _onToggleTag: function(event) {
            var $widget = event.data;
            var $inputTag = $widget.element.find($widget.options.controls.tagInput);
            var $tag = $(this);
            if ($tag.hasClass('hidden')) {
                $inputTag.addClass('hidden');
                $tag.removeClass('hidden');
            } else {
                $tag.addClass('hidden');
                $inputTag.removeClass('hidden');
            }
            return true;
            
        },
        
        _onToggleTagInput: function(event) {
            var $widget = event.data;
            if (event.type !== "focusin") {
                if (_.isEmpty($(this).val())) {
                    var $tag = $widget.element.find($widget.options.controls.tag);
                    $(this).addClass('hidden');
                    $tag.removeClass('hidden');
                } else {
                    $(this).addClass('grouptag-label');  
                }
        
                return true;
                
            } else {
                $(this).removeClass('grouptag-label');  
                return true;
                
            }    
        },
        
        _onBoolOpChange: function(event) {
            var $widget = event.data;
            if (!$widget._isEmpty()) {
                //trigger update event;
                $widget.element.trigger(FilterEvents.GROUP_UPDATE);
                //console.log('boolean updated');
            }
            
        },
        
        _onCohortSelect: function(ele) {
            this._toggleCohortSelect(ele);
            if (!this._isEmpty()) {
                this.element.trigger(FilterEvents.GROUP_UPDATE);
            }
            
            return true;
        },
        
        _toggleCohortSelect: function(ele) {
            var selectedCohortData = ele.name;
            var selectedCohort = COHORT_CONFIGS[selectedCohortData];
            var $parent = $(ele).closest(COHORT_CONTAINER);
            var deselect = $(ele).hasClass('selected');
            $parent.find(COHORT_OPTIONS).removeClass('selected');

            if (deselect) {
                // deselect
                var noneImg = COHORT_CONFIGS['PT_NONE'].img;
                $parent.find(COHORT_ICON).attr('src', noneImg);
                selectedCohortData = null;
                
            } else {
                $parent.find(COHORT_ICON).attr('src', selectedCohort.img);
                $(ele).addClass('selected');
            }
            
            $(this).data(DATA_COHORT, selectedCohortData);
            
            this.element.toggleClass('cohortFilterGroup', selectedCohortData != null);
                        
        },
        
        _onCloseSelect: function(ele) {
            if (ele.name === 'GRP_REMOVE') {
                this.element.trigger(FilterEvents.GROUP_DELETE, this);
            } else {
                this._toggleDeactivated(true);
            }
        }, 
        
				_isWhenGroup: function() {
					return this.getBoolOpValue() === WHEN_GROUP_OPERATOR;
					
				}
				
    });

})(jQuery, window, document);
