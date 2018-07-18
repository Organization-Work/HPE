;(function($, window, document, undefined) {

    $.widget('parametric.monthpartitionFilterItem', $.parametric.baseFilterItem, {
        getValue: function(includeBlank) {
             var fieldValue;
            
            var dateVal1 = this.datepicker1.$element.find('input').val();
            var drange = this.element.find('.partition-period select').val();
                
            fieldValue = dateVal1 + "," + drange;
          
            
            if (!includeBlank && _.isEmpty(fieldValue)) {
                return null;
            }
            return fieldValue;
        },
        
        _setValue: function(itemData) {            
            var $input = this.element.find('input');

            var $dateInputElem = this.element.find('.date-input');
            var $periodElem = this.element.find('.partition-period');
            
         	var fieldValues = itemData.filterValue.split(',');
         	
         	
      	  if (!_.isEmpty(fieldValues[0])) {
         	    this.datepicker1.setValue(fieldValues[0]);
      	  }
      	  if (!_.isEmpty(fieldValues[1])) {
        	this.element.find('.partition-period select').val(fieldValues[1]);
      	  }
            
        },
        

        toString: function() {
        	var str = this.options.data.displayName + " " + this.element.find('select.operator').find(':selected').text().toUpperCase() +  " [";
        	var fieldValue;
        	
            var dateVal1 = this.datepicker1.$element.find('input').val();
            var drange = this.element.find('.partition-period select').val();    
            fieldValue = dateVal1 + "," + drange;            
            str += fieldValue + "]";
            
            return str;
        },

        updateFieldOperator: function(fieldOperator, ignoreSearchUpdate) {
        },
        
        _create: function() {
            var widget = this;
            var itemData = this.options.data;

            this.fieldType = itemData.fieldType;
            
            // initialize datetimepicker
            var fieldMeta = itemData.fieldMeta;
            var dateFormat = fieldMeta && fieldMeta.format ? fieldMeta.format : 'MM/yyyy';
            this.datepicker1 = this.element.find('.datetimepicker1').datetimepicker({
                format: dateFormat,
                language: 'en'
           }).data('datetimepicker');

   
            if (itemData.filterValue) {
                this._setValue(itemData);
            }
            
            this._initTooltip();
            this.lastValue = this.getValue();
            
            this.datepicker1.$element.on('changeDate', $.proxy(this._setStartDate, this));


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
            
            this.element.find('.partition-period select').on('change', function() {
                if (widget._isValid()) {
                    widget.lastValue = widget.getValue();
                    
                    widget.element.trigger(FilterEvents.UPDATE, widget);
                }
                
            });
        },
        
        _setStartDate: function() {
            var date = this.datepicker1.getDate();
            return date;
            
        },

        _setEndDate: function() {
            var date = this.datepicker1.getDate();
            var endDate = (date !== null) ? new Date(date.valueOf() - 86400000) : null;
            this.datepicker1.setEndDate(endDate);
        },

        _isValid: function() {
            this._clearError(this.datepicker1.$element);
            
            var dateVal1 = this.datepicker1.getDate() && this.datepicker1.getDate().valueOf();              
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