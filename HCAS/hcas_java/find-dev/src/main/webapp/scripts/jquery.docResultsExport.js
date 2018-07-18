;(function($, window, document, undefined) {
    
    var DOC_EXPORT_URL = 'ajax/documentfolder/exportDocFolder.do';
    var RESULT_EXPORT_URL = 'ajax/documentfolder/exportResults.do';
    
    var FOLDER_OPTIONS_TEMPLATE = '<% _.each(folders, function(folder) { %> <option class="doc-folders" value="<%= folder.id %>"><%- folder.label %></option> <% }); %>';
    
    var exportableFields, totalResults;
    
    $.widget('search.docResultsExport', {
        options: {
            folderId: null,
            resultsExport: false
        },
        
        _init: function() {
            
            $('#exportDocFolderId').val(this.options.folderId || '');
            $('#hiddenExportTargetFields').val('');
            $('#hiddenExportSourceFields').val('');
            $('#exportName').val('');
            $('#exportSearchView').val(SearchEvents.getSelectedSearchView());
            
            var title = this.options.resultsExport ? 'Results Export' : 'Document Folder Export';
            $('#docExportTitle').text(title);
            
            
            var isResultsExport = this.options.resultsExport;
            
            this.element.toggleClass('result-export', isResultsExport);
            this.element.find('form').toggleClass('result-export', isResultsExport);
            this.element.find('.results-export-control').toggleClass('hide', !isResultsExport);
            
            if (isResultsExport) {
                // show max docs if total docs exceed maxresults.
                totalResults = SearchEvents.getTotalResults();
                var maxResults = SearchConfig.docfolderMaxResults;
                //var showMaxDocs = totalResults > maxResults;
                var showMaxDocs = true;
                // var displayMaxDocs=Math.min(maxResults, totalResults);
                var displayMaxDocs = maxResults;
                $('#exportMaxDocs').val(displayMaxDocs);
                $('#exportTotalResults').text(' from ' + SearchEvents.getFormattedResult(totalResults) + ' records ');
																
		            $('#exportDocSlider').slider({
		            	min: 0,
		            	max: displayMaxDocs,
		            	value: displayMaxDocs,
		            	slide: function(evt, ui) {
		            		if (ui.val>displayMaxDocs) {
		            			return false;
		            		}
		            		$('#exportMaxDocs').val(ui.value);
		            		return true;
		            	},
		            	change: function(evt, ui) {
		            		if (ui.val>displayMaxDocs) {
		            			return false;
		            		}
		            		$('#exportMaxDocs').val(ui.value);
		            		return true;
		            	}
		            });
		            
                this.element.find('.maxdocs').toggleClass('hide', !showMaxDocs);
                
                
            }
            
            
            this.element.find('.control-group').removeClass('error').end();
            this._toggleClearFields();
            
            this._showModal();

            
        },
        
        _reloadFields: function() {
            exportableFields = this._getExportFields();
            
            var sortedFields = _.values(exportableFields).sort(function(a, b) {
                if (b.weight != a.weight) {
                    return b.weight - a.weight;
                } else {
                    return (b.name === a.name) ? 0 : ((b.name > a.name) ? -1 : 1);
                }
            });
            
            var $exportFieldsSelect = $('#exportFieldsSelect');
            /*var selOptions = _.template('<% _.each(fields, function(field) { %> <option value="<%= field.id %>"><%- field.name %></option> <% }); %>',sortedFields, {variable: 'fields'});
            $exportFieldsSelect.append(selOptions);*/
            
            $exportFieldsSelect.multiselect('dataprovider', []);
             var selOptions = [];
             var preSelectedField;
            _.each(sortedFields, function(field) {
            	selOptions.push({
            		'label' : field.name,
            		'value' : field.id
            	});
            	if(field.reference) {
            		preSelectedField = field;
            	}
            });            
            $exportFieldsSelect.multiselect('dataprovider', selOptions); 
            if(preSelectedField) {
            	$exportFieldsSelect.multiselect('select', preSelectedField.id);
            }
            
            
        },
        
        _create: function() {            
            var $element = this.element;
            var $widget = this;
            
            var $modal = $element,
            $exportFieldsSelect = $('#exportFieldsSelect'),
            $hiddenTargetFields = $('#hiddenExportTargetFields'),
            $hiddenSourceFields = $('#hiddenExportSourceFields'),
            $exportTable = $('#fieldsexporttable'),
            $formatSelect = $('#exportFormat'),
            $exportName = $('#exportName'),
            $form = $('#fieldsExportForm'),
            $exportMaxDocs = $('#exportMaxDocs'),
            $clearExportFields = $('#clearExportFields'),
            $exportFieldsSelect = $('#exportFieldsSelect'),
            $maxDocSlider = $('#exportDocSlider');
            
            
            
            var exportRowTemplate = '<tr tabindex="-1" data-field="<%= field.name %>">' +
                                        '<td class="exportfieldsource drag-handle" title="drag to move"><%- field.displayName %></td>' + 
                                        '<td class="exportfieldtarget"><div tabindex="-1" class="exportfieldvalue" contenteditable><%- field.displayName %></div></td>' + 
                                        '<td class="span1 exportfielddelete"><a title="Delete" class="exportfielddelete"><i class="icon-trash"></i></a></td>' + 
                                     '</tr>';
            $exportFieldsSelect.multiselect({
                buttonText: function(options, select) {
                    return options.length + ' selected  <b class="caret"></b>';
                },
                onChange: function(element, checked) {
                    $exportFieldsSelect.closest('.control-group').toggleClass('error', !$exportFieldsSelect.val());
                    
                    if (checked) {
                        var rowTemplate = _.template(exportRowTemplate, {name: element.val(), displayName: element.text()}, {variable: 'field'});
                        var $rowTemplate = $(rowTemplate);
                        $exportTable.find('tbody').append($rowTemplate);
                        $rowTemplate.find('.exportfieldvalue').focus();
                        
                    } else {
                        $exportTable.find('tbody tr').filter(function() {
                            return $(this).data('field') === element.val();
                        }).remove();
                    }
                    
                    $widget._toggleClearFields();
                    
                }
            });
            this._reloadFields();
            
            $exportTable.on('click', 'a.exportfielddelete', function() {
                var $row = $(this).closest('tr');
                var field = $row.data('field');
                $exportFieldsSelect.multiselect('deselect', [field]);
                $row.remove();
                
            });
            
            $exportTable.find('tbody').sortable({
                cursor: 'move',
                handle: '.drag-handle',
                opacity: 0.7,
                helper: function(e, ui) {
                        ui.children().each(function() {
                            $(this).width($(this).width());
                        });
                        
                        return ui;
                    }
            });
            
            $('input[type=text]').on('keypress', function(evt) {
            	if(evt.keyCode === 13) {
            		$(this).blur();
            		return false;
            	}
            });
            
            
            $exportTable.on('click', 'td.drag-handle', function() {
                var $row = $(this).closest('tr');
                $row.focus();
                
            });
            
            $exportName.on('change', function() {
               $(this).val($(this).val().trim());
                
                $(this).closest('.control-group').toggleClass('error', !$(this).val());
                
                return !!$(this).val();
            });
            
            
            $exportMaxDocs.on('change', function(evt) {            		
                $(this).val($(this).val().trim());
				var maxResults = SearchConfig.docfolderMaxResults;
                if ($widget._validateMaxDocs($(this).val(),maxResults)) {
                	$maxDocSlider.slider('value', $(this).val());
                } else {
                	$(this).val($maxDocSlider.slider('value'));
                }
                
                return true;
            });
            
         
            
            $clearExportFields.on('click', function() {
                $exportFieldsSelect.multiselect('deselect', _.keys(exportableFields));
            });
            
            
            $modal.find('.confirm').click(function(){

                $(this).prop('disabled', true);
                
                var $exportedRows = $exportTable.find('tbody tr');
                $exportFieldsSelect.closest('.control-group').toggleClass('error', !$exportFieldsSelect.val());            
                $exportName.closest('.control-group').toggleClass('error', !$exportName.val());
				var maxResults = SearchConfig.docfolderMaxResults;
                if ($widget.options.resultsExport) {
                     $exportMaxDocs.closest('.control-group').toggleClass('error', !$widget._validateMaxDocs($exportMaxDocs.val(),maxResults));                    
                }
                           
                if ($widget.element.find('.control-group').hasClass('error')) {
                    $(this).prop('disabled', false);
                    return false;
                }
                
                var srcFields = [];
                var targetFields = [];
                
                var fieldsMap = {};
                $exportedRows.each(function(index) {
                    srcFields.push($(this).data('field'));
                    targetFields.push($(this).find('div.exportfieldvalue').text());
                    
                });
                
                $hiddenTargetFields.val(srcFields.join());
                $hiddenSourceFields.val(targetFields.join());
                
                $exportFieldsSelect.prop('disabled', true);
                
                var actionUrl = $widget.options.resultsExport ? RESULT_EXPORT_URL : DOC_EXPORT_URL;
                $form.attr('action', actionUrl);
                
                if ($widget.options.resultsExport) {
                    $('#exportSearchData').val(JSON.stringify(SearchEvents.getSearchData()));
                }
                
                $form.submit();
    
                $exportFieldsSelect.prop('disabled', false);
                $(this).prop('disabled', false);
                
                $widget._hideModal();
                
            });
            
            $('.search-controls').on(SearchEvents.SEARCH_VIEW_CHANGED, function() {
                $('#exportFieldsSelect').multiselect('deselect', _.keys(exportableFields));
                $widget._reloadFields();
            });
            
            
            
        },
        
        _toggleClearFields: function() {
            var isEmpty = $('#fieldsexporttable').find('.exportfielddelete').length === 0;
            $('#clearExportFields').prop('disabled', isEmpty);
            
        },
        
        _validateMaxDocs: function(val,max) {
            var intRegex = /^\d+$/;
            
            return intRegex.test(val) && (val > 0) && (val <= max);
        },
        
        _showModal: function() {
            this.element.modal('show');
        },
        
        _hideModal: function() {
            this.element.modal('hide');
        },
        
        _reloadDocFolderSelect: function() {
            var folderOptions = _.template(FOLDER_OPTIONS_TEMPLATE, SearchEvents.getDocumentLists(), {variable: 'folders'});
            var $exportDocFolderSelect = $('#exportDocFolderSelect');
            $exportDocFolderSelect.find('option.doc-folders').remove();
            $exportDocFolderSelect.append(folderOptions);
            
            return $exportDocFolderSelect;
        },
        
        _getExportFields: function() {
            var view = SearchEvents.getSelectedSearchView();
            var fields = {};
    
            _.each(FilterFields[view], function(detail, key) {
                if(!fields[key] && detail.printable) {
                    fields[key] = {id: key, name: detail.displayName, weight: detail.weight, reference: detail.referenceType};
                }
            });
            
            return fields;
            
        },
        
        
        destroy: function() {
        }
    });

})(jQuery, window, document);
 