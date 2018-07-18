;(function($, window, document, undefined) {

		var typeahead_limit = 20;
		var typeahead_minlength = 1;
		var typeahead_input = 'input.tt-input';

    $.widget('parametric.documentfolderFilterItem', $.parametric.baseFilterItem, {
        getValue: function(includeBlank) {
            var val = this.element.find('input[type=hidden]').val();
            if (!includeBlank && _.isEmpty(val)) {
                return null;
            }
            
            return val;
        },
        
        _setValue: function(itemData) {
            var $hiddenInput = this.element.find('input[type=hidden]');
            $hiddenInput.val(itemData.filterValue);
            
            var $visibleInput = this._getTypeaheadInput();
            
            var sourceFolders = itemData.restricted ? SearchEvents.getRestrictedLists() : SearchEvents.getNonRestrictedLists();
            
            var folder = _.find(sourceFolders, function(folder){
                return itemData.filterValue == folder.id;
            });
            
            if (folder) {
                $visibleInput.val(folder.name);
            }
        },
        
        toString: function() {
        	return this.options.data.displayName + " " + this.element.find('select.operator').val().toUpperCase() + " [" + this._getTypeaheadInput().val() +"]";
        
        },
        
        updateFieldOperator: function(fieldOperator, ignoreSearchUpdate) {
            this.fieldOp = fieldOperator;
            
            if (this._isValid() && !ignoreSearchUpdate && !(_.isEmpty(this.lastValue) && _.isEmpty(this.getValue()))) {
                this.element.trigger(FilterEvents.UPDATE, this);
            }
            
        },    

        _create: function() {
            var widget = this;
            var itemData = this.options.data;

            this.fieldType = itemData.fieldType;
            
            this.updateFieldOperator(itemData.fieldOp, true);

            if (itemData.filterValue) {
                this._setValue(itemData);
            }
            
            this._initTooltip();
            this.lastValue = this.getValue();
            
            this.isRestricted = itemData.restricted;

            //this.element.on('change', 'input', onWidgetValueChange);

            function onWidgetValueChange() {
                if (!widget._isValid()) {
                    return false;
                }

                var value = widget.getValue();
                if (value === widget.lastValue){
                    return false;
                }

                widget.lastValue = value;
                widget.element.trigger(FilterEvents.UPDATE, widget);
                
                widget._blurInput();
                
                return true;
            }
            
            var $hiddenInput = this.element.find('input[type=hidden]');
            var $visibleInput = this.element.find('input[type=text]');
            
            
            if (this._useTypeahead()) {

							var dfolders = new Bloodhound({
								limit: typeahead_limit,
							  datumTokenizer: function(d) {
							  	return d.value.split(/\W+|\s+/);
							  },
							  queryTokenizer: function(d) {
							  	return d.split(/\W+|\s+/);
							  },
							  local: _.map(getSourceFolders(), function(obj) {
	                	  	return {value: obj.name};
	              })
							});
						 
							// kicks off the loading/processing of `local` and `prefetch`
							dfolders.initialize();
	            
	            $visibleInput.typeahead(
		            {
							    hint: true,
							    highlight: true,
							    minLength: typeahead_minlength
		            },
		            {
		            		displayKey: 'value',
		                source: dfolders.ttAdapter()
		                
		            }).on('typeahead:selected typeahead:autocompleted', function() {
	                var folderLabel = this.value;
	
	                var folder = _.find(getSourceFolders(), function(folder){
	                    return folderLabel === folder.name;
	                });
	
	                if (folder) {
	                    $hiddenInput.val(folder.id);
	                    onWidgetValueChange();
	                }
	                else {
	                    $hiddenInput.val('');
	                    onWidgetValueChange();
	                }
	            }).on('typeahead:opened', function() {
	          		var dropdownMenu = $(this).parent().find('.tt-dropdown-menu');
	          		if ("none" === dropdownMenu.css('max-width')) {
		          		var maxWidth = Math.floor(0.9 * $(this).closest('fieldset').width());
		          		dropdownMenu.css('max-width', maxWidth + 'px');
		          	}
	          	});
						}

            SearchEvents.$.on(SearchEvents.FOLDERS_LOADED, updateVisibleField);

            updateVisibleField();

            function getSourceFolders() {
                return this.isRestricted ? SearchEvents.getRestrictedLists() : SearchEvents.getNonRestrictedLists();
            }

            function updateVisibleField() {
                var folderId = $hiddenInput.val();
                if (folderId) {
                    var folder = _.find(getSourceFolders(), function(folder){
                        // deliberately using '==' so they'll match if one is a string and one is a number
                        return folderId == folder.id;
                    });

                    if (folder) {
                        $visibleInput.val(folder.name);
                    }
                }
            }
            
        },
        
        _useTypeahead: function() {
        	return !($('#parametricForm').hasClass('edit-disabled'));
        },
        

        _isValid: function() {
            var $div = this.element.find('.control-group');
            var value = $div.find('input[type=hidden]').val();
            var displayText = this._getTypeaheadInput().val();

            if (displayText && value === '') {
                this._setError($div, 'Invalid entry');
                return false;
            }
            this._clearError($div);
            return true;
        },
        
        _getTypeaheadInput: function() {
        	return this._useTypeahead() ? this.element.find(typeahead_input) : this.element.find('input[type=text]');
        }
    });

})(jQuery, window, document);

