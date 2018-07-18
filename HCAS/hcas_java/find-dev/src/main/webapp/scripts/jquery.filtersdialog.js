;(function($, window, document, undefined) {
    
    var FOLDER_CONTENT_URL = "ajax/parametric/getTreeFolderContent.json";
    var FOLDER_CREATE_URL = "ajax/parametric/createTreeFolder.json";
    var GET_ROOT_URL = "ajax/parametric/getRootFolder.json";
    var requestFolderContent;
    var treeDataSource = {
        data: function(params, callback) {
        	if(requestFolderContent && requestFolderContent.readyState != 4){
        		requestFolderContent.abort();
            }
            
            if (params.id !== undefined) {
            	$(".tree-loader-container").show();
                //call ajax for folder content of params.
            	requestFolderContent = $.ajax({
                    dataType : 'json',
                    url: FOLDER_CONTENT_URL,
                    data : {
                        folderId : params.id,
                        searchView: SearchEvents.getSelectedSearchView()
                    },
                    success: function(data){
                        //var folders = _.isArray(data) ? data : [data];
                        callback({data: data});
                        $(".tree-loader-container").hide();
                    },
                    error: function(){
                    	console.log('get folder content failure');
                    	$(".tree-loader-container").hide();
                    }
                }); 
            } else {
                callback({data: []});
            }
        }
    };
    
    var contextMenu = '<ul class="dropdown-menu">' +
                        '<li><a>delete</a></li>' +
                        '<li><a>rename</a></li>' +
                        '</ul>';
    
    function restrictionAlert(title, data){
    	$(".filter-dialog-ui").hide();
    	$('#confirmDialog').confirmDialog({
			title: title,
			message: "<strong>"+"Cannot "+ title +"</strong>"+ "<br>" + data.responseJSON.errorDetail,
			callbackObj: null,
			yesBtnClass: 'btn-primary',
			yesLabel: 'OK',
			yesCallback: function(obj) {
				$(".filter-dialog-ui").show();
            },
			noLabel: null
		});
    }
    
    $.widget('parametric.filtersDialog', {
        options: {
            isLoad: false,
            title: 'Save Filters',
            filtersData: ''
        },
        
        
        
        _init: function() {
            this.element.find('.title').text(this.options.title);
            
            // load the tree
            this.$tree.initFilterTree();

            var $modalUI = this.element.find('.filter-dialog-ui.modal');
            $modalUI.modal('show');
            $modalUI.focus();
            
            this.element.find('#savedFilterDescription').val('');
            this.enableDisable(); 
            this.element.find('.content-list').focus();
        },
        
        _create: function() {
            var $widget = this;
            
            // get the root folder
            this._getRootFolder();
            
            var $folderSelectControl = this.element.find('.folderselect');
            
            $folderSelectControl.on('foldertree-selected', function(event, data) {
                $(this).find('input').val(data.name);
            });
            
            var $tree = this.element.find('.tree.foldertree');
            $tree.tree({dataSource: treeDataSource, showFolderOnly: true});
            
            this.$tree = $tree.data('tree');
            
            $tree.on('click', function(event, data){
            	$widget.enableDisable();
            })
           
            this.element.on('dblclick', '.content-list .tree-folder-header', function(event, data) {
                var notEditing = $(this).find('.tree-folder-name').prop('contentEditable') !== "true";
                if (notEditing) {
                    var $treeSource = $(this).parent().data('tree-source');
                    if($treeSource) {
                        $treeSource.find('> .tree-folder-header').click();
                    }
                }
                $widget.enableDisable();
                return false;
            });
            
            
            var $ele = this.element;
            
            this.element.on('click', '.foldercontent', function(event, data) {
                $widget._getContextMenu().hide();
                
            });
            
            this.element.on('click', '.folder-menu-item', function(event, data) {
                var $this = $widget;
                
                var $contentTreeEntry = $widget._getContextMenu().data('tree-entry');
                var isFolder = $contentTreeEntry.hasClass('tree-folder');
                
                $widget._getContextMenu().hide();
                
                if (event.target.text === 'Rename') {
                    var selector = isFolder ? '.tree-folder-name' : '.tree-item-name';
                    var $nameElement = $contentTreeEntry.find(selector);
                    $nameElement.data('prevName', $nameElement.text());
                    $nameElement.addClass('editing');
                    $nameElement.attr('contentEditable', true).attr('spellcheck', false).focus();
                    
                }
                
                if (event.target.text === "Delete") {
                    if (isFolder) {
                        var folderName = $contentTreeEntry.find('.tree-folder-name').text(),
                        	fodlerId = $contentTreeEntry.data('treeSource').find('> .tree-folder-header').data().id,
                        	node = $("#docFolderTree").jstree().get_node(fodlerId);
                        
                        $widget.element.find('.filter-dialog-ui.modal').modal('hide');
                        
                        var nodeMetaData = node.original.nodeData,
                        	loginUser = $('.userUsername').text();
                        
                        if(node.children_d.length && !nodeMetaData.readOnly && (nodeMetaData.roles.length || nodeMetaData.owner==loginUser.trim())){
                        	$('#confirmDialog').confirmDialog({
                                title: 'Delete Folder',
                                message: 'Cannot Delete [' + folderName + '] from this tab, folder may contain other contents. Please use Folders to modify  this folder.',
                                callbackObj: $contentTreeEntry,
                                yesBtnClass: 'btn-primary',
                                yesLabel: 'OK',
                                yesCallback: function(obj) {$widget.element.find('.filter-dialog-ui.modal').modal('show');},
                                noBtnClass: null,
                                noLabel: null,
                                noCallback: null
                            });
                        }
                        else{
                        	$widget.element.find('.filter-dialog-ui.modal').modal('show');
                        	$widget._deleteFolder($contentTreeEntry);
                        }
                           
                    } else {
                        $widget._deleteTreeEntry($contentTreeEntry, false);
                    }
                }
                return false;
            });
            
            this.element.on('contextmenu', '.content-list .tree-folder-name, .content-list .tree-item-name', function(event, data) {
                if ($widget.options.isLoad) {
                    return false;
                }

                var isFolder = $(event.target).hasClass('tree-folder-name');
                var entry = isFolder ? $(event.target).parent().parent() : $(event.target).parent();
                
                if (isFolder) {
                    var folderData = entry.data('treeSource').find('> .tree-folder-header').data();
                    if (folderData && folderData.readOnly) {
                        return false;
                    }
                }
                
                var $menu = $widget._getContextMenu();
                var x = event.pageX - $('.filter-dialog-ui.modal').offset().left - $(event.target).closest('.foldercontent').get(0).offsetLeft;
                var y = event.pageY - $('.filter-dialog-ui.modal').offset().top - $(event.target).closest('.foldercontent').get(0).offsetTop + $('.folderselect').height();
                $menu.css({display: "block", left: x, top: y, zIndex: 1060});
                
                $menu.data('tree-entry', entry);
                
                
                return false;
            });
            
            this.element.on('contextmenu', '.foldercontent', function(event, data) {
                $widget._getContextMenu().hide();  
                return false;              
            });
            
            
            
            this.element.on('updated', '.content-list .tree-item', function(event, data) {
                var itemData = $(this).data();
                var name = (data.eventType === 'selected') ? itemData.name : '';
                var description = (data.eventType === 'selected') ? itemData.description : '';
                $widget.element.find('#savedFilterName').val(name);
                $widget.element.find('#savedFilterDescription').val(description);
                
            });
            
            
            this.element.on('keypress', '.tree-folder-name, .tree-item-name', function(event, data) {
                if(event.keyCode === 13) {
                    event.preventDefault();
                    $(this).blur();
                }
            });
            
                        
            $folderSelectControl.on('click', '.create-folder-control', function(event, data) {
                $widget.element.find('.foldercontent .content-list').focus();
                $widget.$tree.addNewFolder();
                return false;
                
            });
            
            this.element.on('blur', '.content-list .last-added-folder', function(event, data) {
                var newFolderName = $(this).text();
                var view = SearchEvents.getSelectedSearchView();
                
                // check dup folder name;
                /*var $dupFolder = $widget.$tree.getMatchingTreeEntity(newFolderName, true);
                if ($dupFolder) {
                    $widget.element.find('.filter-dialog-ui.modal').modal('hide');
                    
                    $('#confirmDialog').confirmDialog({
                        title: 'New Folder',
                        message: '<p>Folder [' + newFolderName + '] already exists.</p>',
                        callbackObj: $(this),
                        yesBtnClass: 'btn-primary',
                        yesLabel: 'OK',
                        yesCallback: function(obj) {
                            $widget.element.find('.filter-dialog-ui.modal').modal('show');
                            obj.closest('.tree-folder').remove();
                            //obj.attr('contentEditable', true).focus();
                            },
                        noBtnClass: null,
                        noLabel: null,
                        noCallback: null
                    });
                    
                    
                } else {*/
                    $(this).parent().data().name = newFolderName;
                    $(this).parent().data().searchView = view;
                    $widget._saveCreatedFolder($(this).parent());
                //}
            });

            this.element.on('blur', '.content-list .editing', function(event, data) {
                var isFolder = $(this).hasClass('tree-folder-name');
                var $entry = isFolder ? $(this).parent().parent() : $(this).parent();
                
                var newName = $(this).text();

                $(this).attr('contentEditable', false);  

                /*var duplicateName = $widget.$tree.getMatchingTreeEntity(newName, isFolder);
                
                $(this).removeClass('editing');
                
                if (duplicateName) {
                    $widget.element.find('.filter-dialog-ui.modal').modal('hide');
                    
                    $('#confirmDialog').confirmDialog({
                        title: 'Duplicate Name',
                        message: '<p>Name [' + newName + '] already exists.</p>',
                        callbackObj: $(this),
                        yesBtnClass: 'btn-primary',
                        yesLabel: 'OK',
                        yesCallback: function(obj) {
                            $widget.element.find('.filter-dialog-ui.modal').modal('show');
                            obj.text(obj.data('prevName'));
                            //obj.attr('contentEditable', true).focus();
                            },
                        noBtnClass: null,
                        noLabel: null,
                        noCallback: null
                    });
                    
                } else {*/
                    $widget._renameTreeEntry($entry, isFolder);
                //}
                
                return false;
                
             });
            
            this.element.on('click', '.save-control', function(event) {
                var saveName = $widget.element.find('#savedFilterName').val();
                if (_.isEmpty(saveName)) {
                    return false;
                }
                
                /*var $dupTreeItem = $widget.$tree.getMatchingTreeEntity(saveName, false);
                
                if ($dupTreeItem) {
                    var newDescription = $widget.element.find('#savedFilterDescription').val();
                    var newData = JSON.stringify($widget.options.filtersData);
                    
                    var updatedItemData = _.extend({}, $dupTreeItem.data(), {description: newDescription, data: newData});
                    
                    $widget.element.find('.filter-dialog-ui.modal').modal('hide');
                    
                    $('#confirmDialog').confirmDialog({
                        title: 'Save Confirm',
                        message: 'Filter [' + saveName + '] already exists.<br/>Do you want to replace it?',
                        callbackObj: updatedItemData,
                        //yesBtnClass: null,
                        yesLable: 'Yes',
                        yesCallback: function(obj) {$widget.saveFilter(obj, true);},
                        noBtnClass: 'btn-primary',
                        noLabel: 'No',
                        noCallback: function(obj) {
                            $widget.element.find('.filter-dialog-ui.modal').modal('show');
                        }
                    });   
                } else {*/
                
                    var filtersData = {
                        name: saveName,
                        description: $widget.element.find('#savedFilterDescription').val(),
                        data: JSON.stringify($widget.options.filtersData),
                        parentFolderId: $widget.$tree.getSelectedFolderId(),
                        type: 'ITEM' 
                    };
                    
                    $widget.saveFilter(filtersData);
                //}
                
                //SearchEvents.reloadFolders();
                return false;
                
            });
                                   
            
            this.element.on('click', 'a.load-control', function(event, data) {
                var $selectedItem = $widget.$tree.getSelectedItem();
                if ($selectedItem.length > 0) {
                    $widget._loadSavedFilter($selectedItem.data());                   
                    return false;
                }      
            });
            
            this.element.on('hidden', '.filter-dialog-ui.modal', function(event) {
                $widget._getContextMenu().hide();
            });          
            
        },
        
        _getRootFolder: function() {
            var $this = this;
            
            $.ajax({
                dataType : 'json',
                type: 'GET',
                url: GET_ROOT_URL,
                success: function(data){
                    $this.$tree.loadRootFolder(data);
                    
                },
                error: function(data) {
                }
            });
            
            
        },
        
        _saveCreatedFolder: function($folder) {
		    var $this = this
		    var $tmpFolder = $folder.parent();
		    $tmpFolder.find('.tree-loader').html('Updating...').show();
		    $(".tree-loader-container").show();
            $.ajax({
                dataType : 'json',
                type: 'POST',
                contentType: 'application/json',
                url: FOLDER_CREATE_URL,
                data : JSON.stringify($folder.data()),
                success: function(data){
                    $folder.parent().remove();
                    $this.$tree.createdTreeFolder(data);
                    SearchEvents.reloadFolders();
                    $(".tree-loader-container").hide();
                },
                error: function(data) {
                    //TODO: popup error
                    $folder.parent().remove();
                    restrictionAlert("Create Folder",data);
                    $(".tree-loader-container").hide();
                }
            });
           
        },
            
        
        _renameTreeEntry: function($contentTreeEntry, isFolder) {
        	$(".tree-loader-container").show();
            $contentTreeEntry.find('.tree-loader').html('Updating...').show(); 
            var entryData, url, label;
            if (isFolder) {
                entryData = $contentTreeEntry.data('treeSource').find('> .tree-folder-header').data();
                url = "ajax/parametric/renameFolder.json";
                label = "Rename Folder"
            } else {
                entryData = $contentTreeEntry.data();
                url = "ajax/parametric/renameFilter.json"
                label = "Rename Filter"
            }
            
            var $this = this;
            var selector = isFolder ? '.tree-folder-name' : '.tree-item-name';
            
            var newName = $contentTreeEntry.find(selector).text();
             
            $.ajax({
                dataType : 'json',
                url: url,
                data : {id: entryData.id, newName: newName},
                success: function(data){
                    $this.$tree.renameEntry($contentTreeEntry, data, isFolder);
                    SearchEvents.reloadFolders();
                },
                error: function(data) {
                    var selector = isFolder ? '.tree-folder-name' : '.tree-item-name';
                    var $nameElement = $contentTreeEntry.find(selector);
                    var oldName = $nameElement.data('prevName');
                    $nameElement.text(oldName);
                    restrictionAlert(label,data);
                },
                complete: function(data) {
                    $contentTreeEntry.find('.tree-loader').html('Updating...').hide(); 
                    $(".tree-loader-container").hide();
                }
            });
            
        },
        
        _deleteFolder: function($contentTreeEntry) {
            this._deleteTreeEntry($contentTreeEntry, true);
        },
        
        _deleteTreeEntry: function($contentTreeEntry, isFolder) {
            $contentTreeEntry.find('.tree-loader').html('Deleting...').show(); 
            $(".tree-loader-container").show();
            var entryData, url, title;
            if (isFolder) {
                entryData = $contentTreeEntry.data('treeSource').find('> .tree-folder-header').data();
                url = "ajax/parametric/deleteFolder.json"
                title = "Delete Folder"
            } else {
                entryData = $contentTreeEntry.data();
                url = "ajax/parametric/deleteFilter.json"
                title = "Delete Filter"
            }
            
            var $this = this;
            $.ajax(url + '?' + $.param({id: entryData.id}), {
                type: 'POST',
                success: function(data){
                	var node_id = (isFolder)?entryData.id:entryData.id+"filter";
                	var node = $("#docFolderTree").jstree().get_node(node_id);
                	$("#docFolderTree").jstree().delete_node(node);
                	$('#docFolderTree').jstree(true).redraw(true);
                    $this.$tree.deleteEntry($contentTreeEntry, isFolder);
                    //SearchEvents.reloadFolders();
                },
                error: function(data){
                	restrictionAlert(title,data);
                },
                complete: function(data) {
                	$(".tree-loader-container").hide();
                    $contentTreeEntry.find('.tree-loader').html('Updating...').hide(); 
                }
            });
            
        },
        
        _getContextMenu: function() {
            return this.element.find('.filterset-context-menu');    
        },
        
        _loadSavedFilter: function(filterItem) {
            var $this = this;
            
            var $insertedLoadingGroup = $('#parametricForm').data('filterForm').insertLoadingFilterSet();
            
            $.ajax({
                dataType : 'json',
                url: "ajax/parametric/getFilterData.json",
                data : {filterId: filterItem.id},
                success: function(data){
                    // load the filtergroup;
                    $insertedLoadingGroup.loadFilters(data, filterItem.name, filterItem.fullPath, true);
                    
                }
            });
            
            this.element.find('button.close').click();
            
            setTimeout(function() {
            	$('#searchGo').click();
            }, 1000);
            
        },
       
        saveFilter: function(data, isUpdate) {
            var $this = this;
            if(!isUpdate){
            	$('.save-control').attr('disabled','disabled');
            	$('#saveFilterLoader').show();
            }
            var url = isUpdate ? "ajax/parametric/updateFilter.json" : "ajax/parametric/saveFilter.json";
            
            // update data with the searchView
            data['searchView'] = SearchEvents.getSelectedSearchView();
            
            $.ajax({
                dataType : 'json',
                type: 'POST',
                contentType: 'application/json',
                url: url,
                data : JSON.stringify(data),
                success: function(data){
                    //var folders = _.isArray(data) ? data : [data];
                    $this.element.find('button.close').click();
                    $('#saveFilterLoader').hide();
                    SearchEvents.reloadFolders();
                },
                error: function(data){
                	$('#saveFilterLoader').hide();
                	restrictionAlert("Save Filter",data);
                }
            });
            
        },
        
        enableDisable: function(){
        	var rootFolder = this.element.find('#select-folder').val();
        	
        	if(rootFolder =="root" || rootFolder == "" || this.options.isLoad){
        		if (this.options.isLoad) {
                    this.element.find('#savedFilterName').val('');
                    this.element.find('a.load-control').show();
                    this.element.find('.create-folder-control').hide();
        		}
        		else{
                    this.element.find('.create-folder-control').show();
                	this.element.find('a.load-control').hide();
        		}
        		this.element.find('a.save-control').hide();
        		// set readonly for name/description.
                this.element.find('#savedFilterName').attr('readonly', true);
                this.element.find('#savedFilterDescription').attr('readonly', true);
        	}
        	else{
        		this.element.find('#savedFilterName').val(this.options.filtersData.tag);
                this.element.find('a.save-control').show();
                this.element.find('#savedFilterName').attr('readonly', false);
                this.element.find('#savedFilterDescription').attr('readonly', false);
                this.element.find('.create-folder-control').show();
            	this.element.find('a.load-control').hide();
        	}
        	
        },
        destroy: function() {
        }
    });

})(jQuery, window, document);
 