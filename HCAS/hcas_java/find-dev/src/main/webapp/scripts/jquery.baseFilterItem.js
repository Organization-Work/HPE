;(function($, window, document, undefined) {

    $.widget('parametric.baseFilterItem', {
        getFieldType: function() {
            return this.fieldType;
        },
                
        _blurInput: function(input) {
            var $input = input ? $(input) : this.element.find('input');
            setTimeout(function() {
                $input.blur();
            }, 10);
        },

        _initTooltip: function() {
            this.element.find('input').each(function(){
                $(this).tooltip({
                    html: true,
                    placement: 'right'
                });
            });
        },

        _clearError: function($div, inputElem) {
            var selector = inputElem ? inputElem : 'input';
            $div.removeClass('error');
            $div.find(selector).tooltip('hide')
                .attr('data-original-title', '')
                .tooltip('fixTitle')
                .tooltip('show');
        },

        _setError: function($div, text, inputElem) {
            var selector = inputElem ? inputElem : 'input';
            $div.addClass('error');
            $div.find(selector).tooltip('hide')
                .attr('data-original-title', text)
                .tooltip('fixTitle')
                .tooltip('show');
        },

        _isValid: function() {            
            return true;
        }
    });

})(jQuery, window, document);

