﻿;(function($, window, document, undefined) {

    $.widget('parametric.numericFilterItem', $.parametric.baseFilterItem, {
        getValue: function(includeBlank) {
            var fieldOp  = this.element.find('select.operator').val();
            var $input = this.element.find('input:text');
            var fieldValue;
            
            if (this.fieldOp.dataType === 'operand') {
                var num1 = $($input[0]).val();
                var num2 = $($input[1]).val();
                if (includeBlank) {
                    fieldValue =  num1 + "," +  num2;
                } else {
                    fieldValue = _.isEmpty(num1) || _.isEmpty(num2) ? null : num1 + "," +  num2;
                }
                
            } else {
                var num = $($input[0]).val();
                fieldValue = (!includeBlank && _.isEmpty(num)) ? null : num;
            }
            
            if (!includeBlank && _.isEmpty(fieldValue)) {
                return null;
            }
            
            
            return fieldValue;
        },
        
        _setValue: function(itemData) {
            var $input = this.element.find('input:text');
            
            if (this.fieldOp.dataType === 'operand') {
            	  var $operand = this.element.find('.operand');
            	  $operand.removeClass('hide');
            	  
            	  var fieldValues = itemData.filterValue.split(',');
            	  $($input[0]).val(fieldValues[0]);
            	  $($input[1]).val(fieldValues[1]);
            	
            } else {
            	  $($input[0]).val(itemData.filterValue);
            }
            
        },
        
        toString: function() {
        	var str = this.options.data.displayName + " " + this.element.find('select.operator').find(':selected').text().toUpperCase() +  " [";
        	var fieldValue;
        	
        	var $input = this.element.find('input:text');
            if (this.element.find('option:selected').data('type') === 'operand') {
                fieldValue = $($input[0]).val() + "," + $($input[1]).val();
            } else {
                fieldValue = $($input[0]).val();
            }
            
            str += fieldValue + "]";
            
            return str;
        },
                       
        updateFieldOperator: function(fieldOperator, ignoreSearchUpdate) {
            this.fieldOp = fieldOperator;
            
            var $div = this.element.find('.control-group');
            var $input = this.element.find('input:text');
            
            var $elem = this.element.find('.operand');
            if (fieldOperator.dataType === 'operand') {
                $elem.removeClass('hide');
                this.element.trigger(FilterEvents.UPDATE, this);
            } else {
                $elem.addClass('hide');
                
                $($input[1]).val('');
                this._clearError($($div[1]));
                
                if (this._isValid() && !ignoreSearchUpdate && !(this.lastValue == null && _.isEmpty($($input[1]).val()))) {
                    this.element.trigger(FilterEvents.UPDATE, this);
                }
                
                //return this._isValid();
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
            
            this.element.on('change', 'input', function(event, data) {
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
            });
            
        },

        _isValid: function() {
            var result;
            var $div = this.element.find('.control-group');
            var $div1 = $($div[0]);
            var $input = this.element.find('input:text');
            var value1 =  $($input[0]).val();

            this._clearError($div1);
            result =  !value1 || this._isNumeric($div1, value1);

            if (this.fieldOp.dataType === 'operand') {
                var $div2 = $($div[1]);
              //  var value2 =  $($input[1]).val();
                var value2 =  $($input[1]).val()
                this._clearError($div2);
                
                
                if (_.isEmpty(value1) && !_.isEmpty(value2)) {
                    this._setError($div1, 'Missing input');
                    result = false;
                    
                } else if (!_.isEmpty(value1) && _.isEmpty(value2)) {
                    this._setError($div2, 'Missing input');
                    result = false;
                    
                } else if (_.isEmpty(value1) && _.isEmpty(value2)) {
                    this._clearError($div1);
                    this._clearError($div2);
                    result = true;
                    
                } else {
                    result = this._isNumeric($div2, value2) && result;
                    if (result) {
                        if (Number(value2) > Number(value1)) {
                            this._clearError($div2);
                        } else {
                            this._setError($div1, 'First entry must be less than second entry');
                            this._setError($div2, 'Second entry must be greater than first entry');
                            result = false;
                        }
                    }
                    
                }
            } else {
                result = !_.isEmpty(value1) && !this._isNumeric($div1, value1) ? false : true;
            }
            
            return result;
        },

        _isNumeric: function($div, value) {
            if ($.isNumeric(value)) {
                this._clearError($div);
                return true;
            }
            this._setError($div, 'Entry must be numeric');
            return false;
        }
    });

})(jQuery, window, document);

