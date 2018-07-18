;(function($, window, document, undefined) {

    $.widget('parametric.dateFilterItem', $.parametric.baseFilterItem, {
        getValue: function(includeBlank) {
            var fieldOp  = this.element.find('select').val();
            var fieldValue;
            
            if (this.fieldOp.dataType === 'period') {
                fieldValue = this.element.find('.date-period select').val();
                
            } else if (this.fieldOp.dataType === 'operand') {
                var dateVal1 = this.datepicker1.$element.find('input').val();
                var dateVal2 = this.datepicker2.$element.find('input').val();
                if (includeBlank) {
                    fieldValue = dateVal1 + "," + dateVal2;
                } else {
                    fieldValue = _.isEmpty(dateVal1) || _.isEmpty(dateVal2) ? null : dateVal1 + "," + dateVal2;
                }
            } else {
                var dateVal = this.datepicker1.$element.find('input').val();
                fieldValue = (!includeBlank && _.isEmpty(dateVal)) ? null : dateVal;
            }
            
            if (!includeBlank && _.isEmpty(fieldValue)) {
                return null;
            }
            
            return fieldValue;
        },
        
        _setValue: function(itemData) {            
            var $input = this.element.find('input');

            var $dateInputElem = this.element.find('.date-input');
            var $periodElem = this.element.find('.date-period');
            
            if (this.fieldOp.dataType === 'period') {                
                $dateInputElem.addClass('hide');
                $periodElem.removeClass('hide');
                
                this.element.find('.date-period select').val(itemData.filterValue);
                
            } else {
                $dateInputElem.removeClass('hide');
                $periodElem.addClass('hide');
                
                if (this.fieldOp.dataType === 'operand') {
                	  var $operand = this.element.find('.operand');
                	  $operand.removeClass('hide');
                	  
                	  var fieldValues = itemData.filterValue.split(',');
                	  if (!_.isEmpty(fieldValues[0])) {
                	    this.datepicker1.setValue(fieldValues[0]);
                	  }
                	  if (!_.isEmpty(fieldValues[1])) {
                	    this.datepicker2.setValue(fieldValues[1]);
                	  }
            	
                } else {
                	  if (!_.isEmpty(itemData.filterValue)) {
                	    this.datepicker1.setValue(itemData.filterValue);
                	  } 
                }
            }
            
        },
        

        toString: function() {
        	var str = this.options.data.displayName + " " + this.element.find('select.operator').find(':selected').text().toUpperCase() +  " [";
        	var fieldValue;
        	
        	if (this.element.find('option:selected').data('type') === 'period') {
        	    fieldValue = this.element.find('.date-period option:selected').text();
        	} else if (this.element.find('option:selected').data('type') === 'operand') {
                fieldValue = this.datepicker1.$element.find('input').val() + "," + this.datepicker2.$element.find('input').val();
            } else {
                fieldValue =  this.datepicker1.$element.find('input').val();
            }
            
            str += fieldValue + "]";
            
            return str;
        },

        updateFieldOperator: function(fieldOperator, ignoreSearchUpdate) {
            this.fieldOp = fieldOperator;

            var $dateInputElem = this.element.find('.date-input');
            var $periodElem = this.element.find('.date-period');
            
            
            if (this.fieldOp.dataType === 'period') {
                $dateInputElem.addClass('hide');
                $periodElem.removeClass('hide');
                
                this.datepicker1.setDate(new Date());
                this.datepicker1.setDate(null);
                this.datepicker2.setDate(null);
                
                this._clearError(this.datepicker1.$element);
                this._clearError(this.datepicker2.$element);
                
                if (this._isValid() && !ignoreSearchUpdate) {
                    this.lastValue = this.getValue();
                    this.element.trigger(FilterEvents.UPDATE, this);
                }
                
                
            } else {
                $dateInputElem.removeClass('hide');
                $periodElem.addClass('hide');
                
                var $elem = this.element.find('.operand');
                if (this.fieldOp.dataType === 'operand') {
                    $elem.removeClass('hide');
                    this.datepicker2.setDate(this._setStartDate());
                    this.datepicker2.setDate(null);
                    this._setEndDate();
                    
                    this.element.trigger(FilterEvents.UPDATE, this);

                } else {
                    $elem.addClass('hide');
                    this.datepicker1.setEndDate(null);
                    this.datepicker2.setDate(null);
                    this._clearError(this.datepicker2.$element);
                    
                    var dateVal = this.datepicker1.$element.find('input').val();
                    
                    if (this._isValid() && !ignoreSearchUpdate && !(_.isEmpty(this.lastValue) && _.isEmpty(dateVal))) {
                        this.lastValue = this.getValue();
                        this.element.trigger(FilterEvents.UPDATE, this);
                    }
                }
            }
            
        },
        
        _create: function() {
            var widget = this;
            var itemData = this.options.data;

            this.fieldType = itemData.fieldType;
            
            // initialize datetimepicker
            var fieldMeta = itemData.fieldMeta;
            var dateFormat = fieldMeta && fieldMeta.format ? fieldMeta.format : 'dd/MM/yyyy';
            this.datepicker1 = this.element.find('.datetimepicker1').datetimepicker({
                format: dateFormat,
                language: 'en'
           }).data('datetimepicker');

            this.datepicker2 = this.element.find('.datetimepicker2').datetimepicker({
                format: dateFormat,
                language: 'en'
            }).data('datetimepicker');
            

            this.updateFieldOperator(itemData.fieldOp, true);

            if (itemData.filterValue) {
                this._setValue(itemData);
            }
            
            this._initTooltip();
            this.lastValue = this.getValue();
            
            this.datepicker1.$element.on('changeDate', $.proxy(this._setStartDate, this));

            this.datepicker2.$element.on('changeDate', $.proxy(this._setEndDate, this));

            this.element.on('changeDate', function() {
                if (!widget._isValid()) {
                    return false;
                }

                var value = widget.getValue();
                if (value === widget.lastValue){
                    return false;
                }

                widget.lastValue = value;
                widget.element.trigger(FilterEvents.UPDATE, widget);
                return true;
            });
            
            this.element.find('.date-period select').on('change', function() {
                if (widget._isValid()) {
                    widget.lastValue = widget.getValue();
                    
                    widget.element.trigger(FilterEvents.UPDATE, widget);
                }
                
            });
        },
        
        _setStartDate: function() {
            var date = this.datepicker1.getDate();
            var date2 = null;
            if (date !== null) {
                date2 = new Date(date.valueOf() + 86400000);
                this.datepicker2.setStartDate(date2);
            }
            return date2;
            
        },

        _setEndDate: function() {
            var date = this.datepicker2.getDate();
            var endDate = (date !== null) ? new Date(date.valueOf() - 86400000) : null;
            this.datepicker1.setEndDate(endDate);
        },

        _isValid: function() {
            this._clearError(this.datepicker1.$element);
            
            if (this.fieldOp.dataType === 'operand') {
                var dateVal1 = this.datepicker1.getDate() && this.datepicker1.getDate().valueOf();
                var dateVal2 = this.datepicker2.getDate() && this.datepicker2.getDate().valueOf();
                
                if (dateVal1 && !dateVal2) {
                    this._setError(this.datepicker2.$element, 'Second date is missing');
                    return false;
                } if (!dateVal1 && dateVal2) {
                    this._setError(this.datepicker1.$element, 'First date is missing');
                    return false;
                } else if (dateVal1 && dateVal2 && dateVal2 <= dateVal1) {
                    this._setError(this.datepicker1.$element, 'First date has to be less than second date');
                    this._setError(this.datepicker2.$element, 'Second date has to be greater than first date');
                    return false;
                } else {
                    this._clearError(this.datepicker2.$element);
                }
            }
            
            
            return true;
        },

        _isNotEmpty: function(datepicker) {
            var result = true;
            var value;

            if (datepicker.getDate()) {
                value = datepicker.getDate().valueOf();
            }

            if (value && value !== 0) {
                this._clearError(datepicker.$element);
            } else {
                this._setError(datepicker.$element, 'Date cannot be empty');
                result = false;
            }

            return result;
        }
    });

})(jQuery, window, document);