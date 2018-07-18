;(function($, window, document, undefined) {
    
    var _default = {
            title: 'Confirmation',
            message: 'Are you sure?',
            callbackObj: null,
            yesBtnClass: null,
            yesLabel: 'Yes',
            yesCallback: null,
            noBtnClass: null,
            noLabel: 'No',
            noCallback: null
        
    };  
      
    $.widget('parametric.confirmDialog', {
        options: {
            
        },
        
        _init: function() {
            var $ele = this.element;

            var $yesControl = $ele.find('.yes-control').removeClass('btn-primary btn-info btn-success btn-warning btn-danger btn-inverse btn-link');
            var $noControl = $ele.find('.no-control').removeClass('btn-primary btn-info btn-success btn-warning btn-danger btn-inverse btn-link');
            
            var options = _.extend({}, _default, this.options);
            
            $ele.find('.title').text(options.title);
            $ele.find('.message').html(options.message);
            
            if (_.isEmpty(options.yesLabel)) {
                $yesControl.hide();
            } else {
                $yesControl.text(options.yesLabel);
                $yesControl.show();
            }
            
            if (options.yesBtnClass) {
                $yesControl.addClass(options.yesBtnClass);
            }
            
            if (_.isEmpty(options.noLabel)) {
                $noControl.hide();
            } else {
                $noControl.text(options.noLabel);
                $noControl.show();
            }
            
            if (options.noBtnClass) {
                $noControl.addClass(options.noBtnClass);
            }
            
            $ele.modal('show');
            
            
        },
        
        _create: function() {
            var $this = this;
            
            this.element.on('click', '.yes-control', function(event) {
                $this._onYes();
                return false;
            });

            this.element.on('click', '.no-control', function(event) {
                $this._onNo();
                return false;
            });
            
            this.element.on('hidden', function() {
                $this.options = {};
            });
            
        },
        
        _onYes: function() {
            this.options.yesCallback && this.options.yesCallback(this.options.callbackObj);
            this._close();
            
        },
        
        _onNo: function() {
            this.options.noCallback && this.options.noCallback(this.options.callbackObj);
            this._close();
            
        },
        
        _close: function() {
            this.element.find('button.close').click();
        },
       
        destroy: function() {
            //this.element.data('modal', null);
            //console.log('destroy called');
            this.options = {};
        }
    });

})(jQuery, window, document);
 