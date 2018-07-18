;(function($, window, document, undefined) {
    
    var RESULT_TAG_URL = 'ajax/documentfolder/tagResults.json';
    
    var FOLDER_OPTIONS_TEMPLATE = '<% _.each(folders, function(folder) { %> <option class="doc-folders" title="<%= folder.fullPath %>" value="<%= folder.id %>"><%- folder.name %>(<%- folder.folderType %>)</option> <% }); %>';
    
    var isCancelled, loadingChild, totalResults;
    
    var GET_IMPORTTYPES_URL = 'ajax/documentfolder/getImportTypes.json';    
    var importTypesByView = {};
    var TYPE_OPTIONS = [];
    
    $.widget('search.docResultsTag', {
        options: {
        },
        
        _init: function() {
            isCancelled = loadingChild = false;
            
            $('#tagSearchView').val(SearchEvents.getSelectedSearchView());
            
            
            // show max docs if total docs exceed maxresults.
            totalResults = SearchEvents.getTotalResults();
            var maxResults = SearchConfig.docfolderMaxResults;
            var showMaxDocs = totalResults > maxResults;
            $('#tagMaxDocs').val(Math.min(maxResults, totalResults));
            $('#tagTotalResults').text(' of ' + SearchEvents.getFormattedResult(totalResults));

            $('#tagDocSlider').slider({
            	min: 0,
            	max: totalResults,
            	value: maxResults,
            	slide: function(evt, ui) {
            		$('#tagMaxDocs').val(ui.value);
            		
            		return true;
            	},
            	change: function(evt, ui) {
            		$('#tagMaxDocs').val(ui.value);
            		return true;
            	}
            });

            this.element.find('.maxdocs').toggleClass('hide', !showMaxDocs);
            
            this._loadImportFolderTypes();            
            
            this._reloadDocFolderSelect();            
            
            this.element.find('.control-group').removeClass('error').end();
            this._showModal();            
        },
        
        _create: function() {
            var $element = this.element;
            
            var $modal = $element,
            $form = $('#resultsTagForm'),
            $resultsTagFolderSelect = $('#resultsTagFolderSelect'),
            $resultTagFolderCreate = $("#resultTagFolderCreate"),
            $tagMaxDocs = $('#tagMaxDocs'),
            $maxDocSlider = $('#tagDocSlider');;
            
            $tagMaxDocs.on('change', function() {
                $(this).val($(this).val().trim());
                if ($widget._validateMaxDocs($(this).val())) {
                	$maxDocSlider.slider('value', $(this).val());
                } else {
                	$(this).val($maxDocSlider.slider('value'));
                }
            });

            $('input[type=text]').on('keypress', function(evt) {
            	if(evt.keyCode === 13) {
            		$(this).blur();
            		return false;
            	}
            });
            
            $resultsTagFolderSelect.on('change', function() {
                $(this).closest('.control-group').toggleClass('error', !$(this).val());
            });
            
            var $widget = this;
            $resultTagFolderCreate.on('click', function() {
                loadingChild = true;
                
                var $this = $widget;
                $this._hideModal();
                
                $this._loadImportFolderTypes(); 
                $('#addDocumentFolderDialog').find('#folderTypeSelect').empty().append(TYPE_OPTIONS);
                $('#addDocumentFolderDialog').docFolderCreate({
                    parentCallback: function(folder) {
                        if (folder) {
                            var newFolderOption = _.template(FOLDER_OPTIONS_TEMPLATE, [folder], {variable: 'folders'});
                            $resultsTagFolderSelect.append(newFolderOption);
                           
                            $resultsTagFolderSelect.val(folder.id);
                            
                            $resultsTagFolderSelect.closest('.control-group').toggleClass('error', false);
                            
                        }
                        $this._showModal();
                        
                    }
                });
                
                return false;
                
            });
            
            
            $modal.find('.confirm').click(function(){
                
                $(this).prop('disabled', true);
                $('#tagDocument-loader').show();

                $resultsTagFolderSelect.closest('.control-group').toggleClass('error', !$resultsTagFolderSelect.val());         
                $tagMaxDocs.closest('.control-group').toggleClass('error', !$widget._validateMaxDocs($tagMaxDocs.val()));                    

                if ($widget.element.find('.control-group').hasClass('error')) {
                    $(this).prop('disabled', false);
                    $('#tagDocument-loader').hide();
                    return false;
                }
                
                var submitData = {
                    exportDocFolderId: $resultsTagFolderSelect.val(),
                    searchView: SearchEvents.getSelectedSearchView(),
                    exportSearchData: JSON.stringify(SearchEvents.getSearchData()),
                    exportMaxDocs: $tagMaxDocs.val()
                };
                var $confirm = $(this);
                
                $.ajax(RESULT_TAG_URL, {
                    type: 'POST',
                    contentType: 'application/json',
                    data: JSON.stringify(submitData)
                }).done(function(folder){
                    $widget._hideModal();

                }).error(function(data){
                    $resultsTagFolderSelect.closest('.control-group').addClass('error');
                }).always(function() {
                    $confirm.prop('disabled', false);
                    $('#tagDocument-loader').hide();
                });
                
                
            });
            
            $modal.on('click', '.cancel', function() {
                isCancelled = true;
            });
            
            $modal.on('hidden', function() {
                !isCancelled && !loadingChild && SearchEvents.reloadFolders();   
                loadingChild = false;
            });
            
            
        },
        
        _loadImportFolderTypes: function () {
            var view = SearchEvents.getSelectedSearchView();

            if (!view) {
                return;
            }
            
            $.ajax(GET_IMPORTTYPES_URL, {
                type : 'POST',
                dataType : 'json',
                data : { searchView: view }
            }).done(function(typesMap){
                importTypesByView[view] = typesMap;
                TYPE_OPTIONS = [];
                
                $.each(typesMap, function(key, type) {
                    var option = _.template('<option value="<%= option.value %>"><%- option.name %></option>', {value: key, name: type.displayName}, {variable: "option"});
                    TYPE_OPTIONS.push(option);
                });
                
               
            });
        },
        
        _validateMaxDocs: function(val) {
            var intRegex = /^\d+$/;
            
            return intRegex.test(val) && (val > 0) && (val <= totalResults);
        },
        
        _showModal: function() {
            this.element.modal('show');
        },
        
        _hideModal: function() {
            this.element.modal('hide');
        },
        
        _reloadDocFolderSelect: function() {
            var folderOptions = _.template(FOLDER_OPTIONS_TEMPLATE, SearchEvents.getDocumentLists(), {variable: 'folders'});
            var $resultsTagFolderSelect = $('#resultsTagFolderSelect');
            $resultsTagFolderSelect.find('option.doc-folders').remove();
            $resultsTagFolderSelect.append(folderOptions);
            
            return $resultsTagFolderSelect;
        },
        
        
        destroy: function() {
        }
    });

})(jQuery, window, document);
 