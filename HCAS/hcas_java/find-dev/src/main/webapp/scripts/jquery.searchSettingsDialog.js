;(function($, window, document, undefined) {
    var GET_SETTINGS_URL = "ajax/search/getSearchSettings.json";
    var SET_SETTINGS_URL = "ajax/search/setSearchSettings.json";

    var userSettings;
    
    function loadSettings() {
        $.ajax(GET_SETTINGS_URL).done(function(settings){
            userSettings = settings;
        });
    };
    
    loadSettings();
    
    $("#searchSettingsControl").click(function() {
        $("#searchSettingsDialog").searchSettingsDialog();
        return false;
    });
    
    SearchEvents.getUserSearchSettings = function() {
        return userSettings;
    }
    
      
    $.widget('parametric.searchSettingsDialog', {
        options: {
            
        },
        
        _init: function() {
            var $ele = this.element,
            $combine = $ele.find('input[name=combine]').val(userSettings.combine),
            $summary = $ele.find('input[name=summary]').val(userSettings.summary);
            $ele.find('.error-message').hide();
            
            this._showModal();
            
            
        },
        
        _create: function() {
            var $this = this;
            var $modal = this.element,
            $combine = $modal.find('input[name=combine]'),
            $summary = $modal.find('input[name=summary]');
            
            $modal.find('.confirm').click(function(){
                var combine = $.trim($combine.val());
                var summary = $.trim($summary.val());
    
                $combine.closest('.control-group').toggleClass('error', !combine);
                $summary.closest('.control-group').toggleClass('error', !summary);
                
                if (combine || summary) {
                    // submit the settings.
                    $.ajax(SET_SETTINGS_URL, {
                        type : 'POST',
                        contentType: 'application/json; charset=UTF-8',
                        dataType : 'json',
                        data : JSON.stringify({combine: combine, summary: summary})
                    }).done(function(settings){
                        userSettings = settings;
                        setTimeout(function() {
                            SearchEvents.attemptSearch();
                        }, 10);
                        
                        $this._hideModal();
                       
                    }).error(function() {
                        $modal.find('.error-message').show();
                    });
                }
                
                return false;
            });
            
            $modal.on('hidden', function() {
                
            });
            
        },
        
        _showModal: function() {
            this.element.modal('show');
        },
        
        _hideModal: function() {
            this.element.modal('hide');
        },
        
        
        destroy: function() {
        }
    });

})(jQuery, window, document);
 