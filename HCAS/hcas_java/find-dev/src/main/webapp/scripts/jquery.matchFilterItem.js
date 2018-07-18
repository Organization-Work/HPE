;(function($, window, document, undefined) {
	
		//typeahead related variables:
		var typeahead_limit = 20;
		var typeahead_minlength = 1;
		var typeahead_querysort = 'documentcount';
		var typeahead_input = 'textarea.peepTextarea.tt-input';
		var typeahead_baseurl = 'ajax/search/getAutocompletValues.json?values=' + typeahead_limit + '&sort=' + typeahead_querysort + '&query=%QUERY';
		var $ttHint = $(".twitter-typeahead").find('textarea');
		
    $.widget('parametric.matchFilterItem', $.parametric.baseFilterItem, {
        getValue: function(includeBlank) {
            var val = this._getTypeaheadVal();
            
            if (!includeBlank && _.isEmpty(val)) {
                return null;
            }
            
            return val;
        },
        
        _setValue: function(itemData) {   
        	this._setTypeaheadVal(itemData.filterValue);        
                        
        },
        
        _isContainOp: function(op) {
            return FILTER_OPERATORS.CONTAINS.value === op || FILTER_OPERATORS.NOT_CONTAINS.value === op;
        },
        
        _isISOp: function(op) {
            return FILTER_OPERATORS.IS.value === op ||
                   FILTER_OPERATORS.IS_NOT.value === op ||
                   FILTER_OPERATORS.IS_RANGE.value ===  op ||
                   FILTER_OPERATORS.IS_NOT_RANGE.value ===  op;
        },
        
        toString: function() {
        	return this.options.data.displayName + " " + this.element.find('select.operator').val() +  " [" + this._getTypeaheadVal() +"]";
        
        },
        
        updateFieldOperator: function(fieldOperator, ignoreSearchUpdate) {
            var oldOp = this.fieldOp && this.fieldOp.value;
            var newOp = fieldOperator.value;
            
            var isLastEmpty = _.isEmpty(this.lastValue);
            
            /*if (this._isISOp(oldOp)) {
                if (this._isContainOp(newOp)) {
                    this._setTypeaheadVal('');
                    this.lastValue = '';
                    
                }
            } else {
                if (this._isISOp(newOp)) {
                    this._setTypeaheadVal('');
                    this.lastValue = '';
                    
                }
            } */
            this.fieldOp = fieldOperator;
            if (this._isValid() && !ignoreSearchUpdate && !(isLastEmpty && _.isEmpty(this._getTypeaheadVal()))) {
                this.element.trigger(FilterEvents.UPDATE, this);
            }
            
        },

        _create: function() {
            var widget = this;
            var itemData = this.options.data;
            
            this.fieldType = itemData.fieldType;            

            //this.isParametric = itemData.fieldMeta && itemData.fieldMeta.filterFieldType === PARAMETRIC_FIELDTYPE;


            this.updateFieldOperator(itemData.fieldOp, true);
           
            if (itemData.filterValue) {            	
                this._setValue(itemData);               
            }                       
			this.isParametric = itemData.fieldMeta && itemData.fieldMeta.filterFieldType === PARAMETRIC_FIELDTYPE;
			
			/*this.element.on('click', 'input[type=button]', function(event, data) {          	            	
				if (widget._useTypeahead()) {
					widget._initTypeahead(itemData.parentId);				        	         	
				}           
			});*/
			
			if (this._useTypeahead()) {
				this._initTypeahead(itemData.parentId);				        	         	
			}
			
			
            this.element.on('change', 'input', function(event, data) {          	            	
            	return widget.isParametric && widget._isISOp(widget.fieldOp.value) ? true : widget._validateText(this);           
           });
            
			//this._useTypeahead();
			//this._initTypeahead();
            this._initTooltip();
            this.lastValue = this.getValue();
        },              
        
        _useTypeahead: function() {
        	var isDisbledEdit = $('#parametricForm').hasClass('edit-disabled');        
        	return this.isParametric && !isDisbledEdit;
        },
        
        _initTypeahead: function(fieldname) {
        	var widget = this;
        	
          //Typeahead
			    this.typeAheadSource = SearchConfig.preloadParaValues ? this._getLocalSource(fieldname, widget) : this._getRemoteSource(fieldname, widget);
						
				this.typeAheadSource.initialize();
				
				this.element.find('textarea').typeahead(
	            {
						    hint: true,
						    highlight: true,
						    minLength: typeahead_minlength
	            },	            
	            {
	            	displayKey: 'value',
	            	source: widget.typeAheadSource.ttAdapter()
	             }
	           ).on('typeahead:focus typeahead:autocompleted', function() {
	           	var $input = widget.element.find(typeahead_input);
	            return widget._validateText($input);
	           	
	          }).on('typeahead:selected typeahead:autocompleted', function() {
	           	var $input = widget.element.find(typeahead_input);
	            return widget._validateText($input);
	           	
	          }).on('typeahead:opened', function() {
	          	var dropdownMenu = $(this).parent().find('.tt-dropdown-menu');
	          	if ("none" === dropdownMenu.css('max-width')) {
		          	var maxWidth = Math.floor(0.9 * $(this).closest('fieldset').width());
		          	dropdownMenu.css('max-width', maxWidth + 'px');	          		          	
		          }
	          });
        	/*typeahead_baseurl = 'ajax/search/getAutocompletValues.json';
        	
        		widget.element.find('input[type=text]').autocomplete({
          	      source: function( request, response ) {
          	        $.ajax(typeahead_baseurl + '?' +  $.param({
          	        	query: request.term,
              			sort: typeahead_querysort,
              			value: typeahead_limit,
              			field:  encodeURI(fieldname)
          	        }),     	        		
          	        {        	          
          	        	data: JSON.stringify(SearchSettings.getCurrentSearchFilters()),
          	        	type: 'POST',
                        contentType: "application/json",
        				  dataType: "json",
        				 
          	          success: function( data ) {
          	        	  
          	        	  widget.element.find('input[type=text]').css({"background-image" : "",
    						"background-repeat" : "",
    						"background-position" : "",
    						"background-attachment": "",
    						"background-color" : "",
    						"background-origin" : ""});
          	        	var values = data.result && data.result[0] && data.result[0].fieldValues;
          	            response(values || {value: ""});
          	          },
          	          error: function(error) {
          	        	 widget.element.find('input[type=text]').css({"background-image" : "",
     						"background-repeat" : "",
     						"background-position" : "",
     						"background-attachment": "",
     						"background-color" : "",
     						"background-origin" : ""});
          	          }
          	        });
          	      },
          	      minLength: 1000,
          	      select: function( event, ui ) {
          	        console.log( ui.item ?
          	          "Selected: " + ui.item.label :
          	          "Nothing selected, input was " + this.value);
          	    	var $input = widget.element.find(typeahead_input);
      	            return widget._validateText($input);
          	      },
          	      open: function() {
          	        $( this ).removeClass( "ui-corner-all" ).addClass( "ui-corner-top" );
          	      widget.element.find('input[type=text]').css({"background-image" : "",
						"background-repeat" : "",
						"background-position" : "",
						"background-attachment": "",
						"background-color" : "",
						"background-origin" : ""});
          	      },
          	      close: function() {
          	        $( this ).removeClass( "ui-corner-top" ).addClass( "ui-corner-all" );
          	        $( this ).autocomplete('option', 'minLength', 1000);
          	        widget.element.find('input[type=text]').css({"background-image" : "",
						"background-repeat" : "",
						"background-position" : "",
						"background-attachment": "",
						"background-color" : "",
						"background-origin" : ""});
          	      },
          	      change: function(event, ui) {
          	    	  //event.preventDefault();
          	      }
          	    });
        	
        		widget.element.find('input[type=button]').click(function() {
        			widget.element.find('input[type=text]').autocomplete('option', 'minLength', 0);
        			widget.element.find('input[type=text]').autocomplete('search', widget.element.find('input[type=text]').val());
        			widget.element.find('input[type=text]').css({"background-image" : "url('../resources/images/ajax-loader.gif')",
						"background-repeat" : "no-repeat",
						"background-position" : "right center",
						"background-attachment": "scroll",
						"background-color" : "transparent",
						"background-origin" : "content-box"});
        		});
        		
        		widget.element.find('input[type=text]').keyup(function(){
        			widget.element.find('input[type=text]').autocomplete('option', 'minLength', 1000);
        			 var valThis = $(this).val();
        			 $('.ui-autocomplete>li').each(function(){
        				 var text = $(this).text().toLowerCase();
        				 if(text.indexOf(valThis) > -1) {
        					 $(this).show();
        				 } else {
        					 $(this).hide();   
        				 }        					     
        			 });
        		});*/

        	

        	
        },
        
        _getRemoteSource: function(fieldname, widget) {
        	          
        	var $ttHint = widget.element.find(typeahead_input);        	
			    var remoteSource = new Bloodhound({
					  limit: typeahead_limit,           	
					  datumTokenizer: function(d) {
					  	return d.value.split(/\W+|\s+/);
					  },
			    	queryTokenizer: function(d) {
			    		return d.split(/\W+|\s+/);
			    	},
			    	remote: {
				    	url: typeahead_baseurl + '&field=' + encodeURI(fieldname),
					    replace: function(_url, query) {
					    	if (!$ttHint.length) {
					    		$ttHint = widget.element.find(typeahead_input);
					    		//$ttHint = $(".twitter-typeahead").find('input');
			            	}					    	
					    	$ttHint.css({"background-image" : "url('../resources/images/ajax-loader.gif')",
					    								"background-repeat" : "no-repeat",
					    								"background-position" : "center center",
					    								"background-attachment": "scroll",
					    								"background-color" : "transparent",
					    								"background-origin" : "content-box"});
					    	
					    	/*setTimeout(function() {
					    		//$ttHint.addClass("typeahead-loading");
					    		$('input.tt-input').css('background', 'transparent url("../resources/images/ajax-loader.gif") no-repeat scroll right center content-box !important');
					    	}, 1000);*/
					    	this.ajax.data = JSON.stringify(SearchSettings.getCurrentSearchFilters());
					    	if(query.split('\n').pop() || query.split('\n').pop()==""){
					    		var qryVal = query.split('\n').pop();
					    		var $textarea = $('textarea.peepTextarea.tt-input');
					    		$textarea.scrollTop($textarea[0].scrollHeight);
					    	     
					    	} else {
					    		var qryVal = query.split('\n')[0];
					    	}
					    	return _url.replace('%QUERY', qryVal);
					    },
					    filter: function(data) {
					    	if (!$ttHint.length) {
					    		$ttHint = widget.element.find(typeahead_input);
					    		//$ttHint = $(".twitter-typeahead").find('input');
			            	}	
					    	//$ttHint.removeClass("typeahead-loading");
					    	$ttHint.css({"background-image" : "",
								"background-repeat" : "",
								"background-position" : "",
								"background-attachment": "",
								"background-color" : "",
								"background-origin" : ""});
					    	
					    	var values = data.result && data.result[0] && data.result[0].fieldValues;
					    	return values || {value: ""};
					    		
					    },
				        ajax: {
				            type : 'POST',
				            contentType : 'application/json',
				            dataType : 'json',
				            data: {}				            
				        },
				        rateLimitWait: 1000
			    	}
				});					
				return remoteSource;
        },
        
        _getLocalSource: function(fieldname) {
					var localSource = new Bloodhound({
					  limit: typeahead_limit,           	
					  datumTokenizer: function(d) {
					  	return d.value.split(/\W+|\s+/);					  	
					  },
					  queryTokenizer: function(d) {
					  	return d.split(/\W+|\s+/);
					  },
					  local: _.map(SearchEvents.getFieldValues(fieldname).values, function(val) {
              	  	return {value: val};
            })
					});
        	
        	return localSource;
        },
        
        _getTypeaheadVal: function() {  
        	var inputSelector = this._useTypeahead() ? typeahead_input : 'textarea';      	
        	
        	// if multivalue box is visible use value from that box instead of the single line input box
        /*	if( this.element.find(textarea_input)
        			&& this.element.find(textarea_input).css("display") 
        			&& this.element.find(textarea_input).css("display") != 'none'
        			&& this.element.find(textarea_input).css("display") != '') {
        		inputSelector = textarea_input;
        	} */  
        	var return_val = this.element.find(inputSelector).val();
        	return return_val;
        },
        
        _setTypeaheadVal: function(newVal) {
        	var inputSelector = this._useTypeahead() ? typeahead_input : 'textarea';
        	this.element.find(inputSelector).val(newVal);
        },
        
        _validateText: function(inputEle) {
            if (!this._isValid()) {
                return false;
            }

            var value = $(inputEle).val().trim();
            if (value === this.lastValue){
                return false;
            }

            this.lastValue = value;
            
            this.element.trigger(FilterEvents.UPDATE, this);
            this._blurInput();
            
            return true;
            
        },
        
        _validateMatch: function(inputEle) {
            var inputVal = $(inputEle).val().trim();
            
            // Made sure it's a selected value from typeahead
            if (inputVal && (this.selectedTypeahead !== inputVal || inputVal === this.lastValue)) {
                if (this.selectedTypeahead !== inputVal) {
                    inputEle.value =  this.lastValue;
                }
                return false;
            }
            
            this.lastValue = inputVal;
                                         
            this.element.trigger(FilterEvents.UPDATE, this);
            
            this._blurInput();
            
            return true;
        },

        _isValid: function() {
            // nothing to validate for this field
            return true;
        }
    });

})(jQuery, window, document);