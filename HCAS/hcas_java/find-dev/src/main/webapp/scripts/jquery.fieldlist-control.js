;(function($, window, document, undefined) {
	var tempID  =  this.versionObj;
	$.widget('parametric.fieldlistControl', {
        options: {
            fieldListTemplate: '../templates/search/filters.fieldList'+tempID+'.template',
            fieldListSelector: '#field-list'
        },
        
        getFieldValues: function(id) {
            return _.extend({}, this.fieldValues[id]);
        },

        _create: function() {
            this.element.bind('fieldlistcontroladdfilter', function(event, data) {
                $('#parametricForm').data('filterForm').addFilter(data);
            });
        },

        _init: function() {
            if (!this.fieldValues) {
                this._reloadFieldValues(false);
            }
            
            //register add filter event handling.
            var $control = this;
            $(this.options.fieldListSelector).on('click', 'a.field-item', function(event) {
                var $elem = $(this);
                //var dataItem = dataItem = $control.fieldValues[$elem.data('id')];
                var dataItem = {
                    displayName: $elem.text(),
                    name: $elem.data('id')
                };
                $control._trigger('addfilter', event, dataItem);
            });
            
            $(this.options.fieldListSelector).on('click', 'li.fieldslist-group>a', function(event) {
            	
              var $elem = $(this).parent();
              var $items = $elem.find('a.field-item');
              var filterFields = {};
              _.each($items, function(item) {
              	var $this = $(item);
              	filterFields[$this.data('id')] = [{}];
              	
              });
          
              var whenop = $elem.data('whenop');    
              $('#parametricForm').data('filterForm').insertLoadingFilterSet()
              	.loadFilters({
              		boolOperator: whenop ? 'WHEN' : 'OR',
                  filterFields: filterFields,
                  childGroups: null,
                  tag: $elem.find('> a span').text(),
                  whenOp: whenop,
                  whenGroup: $elem.data('whengrp')
              	}, null, null, true);
              	
            });
            

            // register for inserting the custom filter set
            $(this.options.fieldListSelector).on('click', '#customFilterSet', function() {
                var $dialog = $('.filter-dialog');
                $dialog.filtersDialog({isLoad: true, title: 'Load Custom Filter Set'});
                return false;
            });


            $('.search-controls').on(SearchEvents.SEARCH_VIEW_CHANGED, function() {
                $control._reloadFieldValues(true);
                $('#tviewResetButton').click();

            });
        },

        _reloadFieldValues: function(isViewChange) {
            var $control = this;
            var couldNotLoadFieldValues = function(errorMsg) {
                var $fieldListMessage = $('.ignore', $control.options.fieldListSelector);
                var errorLoading = $control.options.errorLoading || 'Error loading filter fields!';
                $fieldListMessage.text(errorLoading);
                
                var message = errorMsg || 'Error loading filter fields!';
                $('#confirmDialog').confirmDialog({
                        title: 'Field loading',
                        message: message,
                        callbackObj: null,
                        yesBtnClass: 'btn-primary',
                        yesLabel: 'OK',
                        noLabel: null
                });                
            };
            
            var viewName = SearchEvents.getSelectedSearchView();
            if (viewName) {
                // loading mask
                $('#loadingDialog').find('.title').text('Loading fields...');
                $('#loadingDialog').modal('show');
                SearchEvents.getParametricsXhr().done(this._loadedFieldValues(couldNotLoadFieldValues, isViewChange)).fail(couldNotLoadFieldValues).always(function() {$('#loadingDialog').modal('hide');});
            }
            
        },
        
        _loadedFieldValues: function( failedLoadHandler, isViewChange ) {
            var $control = this;
            var fieldListTpl = _.template($.resGet(this.options.fieldListTemplate));
            
            return function(response) {
                $('#loadingDialog').modal('hide');

                if (response.success) {
                    $control.fieldValues = response.result;

                    var view = SearchEvents.getSelectedSearchView();
                    var html = fieldListTpl({
                        fields: Util.mapHeader(FilterFields[view])
                    });
                    $($control.options.fieldListSelector).empty().append(html);

                    setTimeout(function() {
					            SearchEvents.$.parametricLoaded = true;
					            SearchEvents.$.trigger(SearchEvents.PARAMETRICS_LOADED, {data: response.result, isViewChange: isViewChange} );
					            
					            var hashValue = $.parseJSON(decodeURI(window.location.hash.substring(1)) || '{}');
					            if (isViewChange || (!SearchSettings.isManualSearch() && (!hashValue || !hashValue.filterGroup))) {
					                $("#resetButton").click();
					            }
                    }, 500);
                    
                    
                }
                else {
                    failedLoadHandler(response.error);
                }
            };
        },

        _destroy: function() {
        }
        
    });
})(jQuery, window, document);

