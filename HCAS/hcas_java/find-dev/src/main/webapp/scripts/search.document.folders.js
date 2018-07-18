jQuery(function($){
	var tempID  =  this.versionObj;
	var tempID =  localStorage.getItem("storageId");
	
    var GET_FOLDERS_URL = 'ajax/documentfolder/getFolders.json';
    var DELETE_FOLDER_URL = 'ajax/documentfolder/deleteFolder.json';
    var EDIT_FOLDER_URL = 'ajax/documentfolder/editFolder.json';
    var SET_SELECTED_FOLDER_URL = 'ajax/documentfolder/setSelected.json';
    var GET_IMPORTTYPES_URL = 'ajax/documentfolder/getImportTypes.json';
    var DOC_IMPORT_URL = 'ajax/documentfolder/docImportData.json';
    var GET_IMPORTED_VALUES_URL = 'ajax/documentfolder/getImportedValues.json';
    var GET_FOLDER_DATA_URL = 'ajax/documentfolder/getFoldersdata.json';
    var GET_CUSTOM_FILTER_DATA_URL = 'ajax/documentfolder/getCustomFilter.json';
    var CCP_DOC_URL = 'ajax/documentfolder/ccpDocFolder.json';
    var GET_FILTER_ITEM = 'ajax/parametric/getFilterItem.json';
    var GET_USER_ROLES = 'ajax/documentfolder/getUserRoles.json';
    var EDIT_CUSTOMFILTER_URL = 'ajax/documentfolder/editCustomFilter.json'
    
    var SELECTED_FILE_KEY = "selectedFile";

    var documentListByView = {};
    
    var importTypesByView = {};
    var TYPE_OPTIONS = [];
    var USER_ROLES = [];
    var importedData;
    
    var docfolderfilteradded;
    
    // don't really approve of resGet, since it's synchronous, but it'll have to do until we use require.js
    var documentgroupTemplate = _.template($.resGet('../templates/search/documentfolder'+tempID+'.template'));
    
    var treeData = {
		    "core" : {
		        "animation" : true,
		        "check_callback" : true,
		        "data" : [],
		        "themes" : { 
		          "stripes" : true
		        }
		      },
		      "types" : {
		    	    "default" : {
		    	    	"valid_children" : ["default","folderClosedShared","folderClosedSharedReadOnly","documentList","documentListOthers","documentListUser","documentListShared","documentListSharedReadOnly","customFilter","customFilterOthers","customFilterUser","customFilterShared","customFilterSharedReadOnly"],
		    	    	"icon": "jstree-folderClosed"
		            },
		           	"folderClosedShared" : {
		           		"valid_children" : ["default","folderClosedShared","folderClosedSharedReadOnly","documentList","documentListOthers","documentListUser","documentListShared","documentListSharedReadOnly","customFilter","customFilterOthers","customFilterUser","customFilterShared","customFilterSharedReadOnly"],
		           		"icon" : "jstree-folderClosedShared"
		           	},
		           	"folderClosedSharedReadOnly" : {
		           		"valid_children" : ["default","folderClosedShared","folderClosedSharedReadOnly","documentList","documentListOthers","documentListUser","documentListShared","documentListSharedReadOnly","customFilter","customFilterOthers","customFilterUser","customFilterShared","customFilterSharedReadOnly"],
		           		"icon" : "jstree-folderClosedSharedReadOnly"
		           	},
		      	    "documentList" : {
		      	    	"icon" : "jstree-documentList"
		      	    },
		      	    "documentListShared" : {
		      	    	"icon" : "jstree-documentListShared"
		      	    },
		      	    "documentListSharedReadOnly" : {
		      	    	"icon" : "jstree-documentListSharedReadOnly"
		      	    },
		      	    "customFilter" : {
		      	    	"icon" : "jstree-customFilter"
		      	    },
		      	    "customFilterUser" : {
		      	    	"icon" : "jstree-customFilterUser"
		      	    },
		      	    "customFilterOthers" : {
		      	    	"icon" : "jstree-customFilterOthers"
		      	    },
		      	    "customFilterShared" : {
		      	    	"icon" : "jstree-customFilterShared"
		      	    },
		      	    "customFilterSharedReadOnly" : {
		      	    	"icon" : "jstree-customFilterSharedReadOnly"
		      	    }
		  	  }, 
		      "checkbox" : {
		        "keep_selected_style" : false
		      },
		      "plugins" : ["checkbox","sort","search","types","state"]
		    };
    
    loadImportTypes();
    loadFolders();
    loadUserRoles();
     
    function loadImportTypes() {
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
    }
    
    function loadUserRoles(){
    	$.ajax(GET_USER_ROLES,{
    		type : 'POST',
    		dataType : 'json',
    	}).done(function(roles){
    		USER_ROLES = [];
    		
    		$.each(roles, function(key, role){
    			var option = _.template('<option value="<%= option.name %>"><%= option.name %></option>', {name: role}, {variable: "option"});
    			USER_ROLES.push(option);
    		});
    	})
    }
    
    function bindJstreeEvents(){
    	var DOCUMENTFOLDER_DROPDOWN_TEMPLATE_URL = _.template($.resGet('../templates/search/documentfolderDropdown'+tempID+'.template'));
    	var DOCUMENTFOLDERLIST_DROPDOWN_TEMPLATE_URL = _.template($.resGet('../templates/search/documentfolderListDropdown'+tempID+'.template'));
    	var CUSTOMFILTER_DROPDOWN_TEMPLATE_URL = _.template($.resGet('../templates/search/customFilterDropdown'+tempID+'.template'));
    	
    	$('#docFolderTree').bind('redraw.jstree', function(event, data) {
   		  var $tree = $(this);
  		  $($tree.jstree().get_json($tree, {
  		      flat: true
  		    }))
  		    .each(function(index, value) {
  		      var node = $("#docFolderTree").jstree().get_node(this.id);
  		      var isFolder = node.original.nodeData.folderType;
  		      if(isFolder === undefined){
  		    	$("#docFolderTree li#"+node.id+">a").after(CUSTOMFILTER_DROPDOWN_TEMPLATE_URL);
  		      }
  		      else if(isFolder===null){
  		    	$("#docFolderTree li#"+node.id+">a").after(DOCUMENTFOLDER_DROPDOWN_TEMPLATE_URL);
  		      }
  		      else{
  		    	var docCount = node.original.nodeData.docCount;
  		    	$("#docFolderTree li#"+node.id+">a").after(DOCUMENTFOLDERLIST_DROPDOWN_TEMPLATE_URL);
  		    	$("#docFolderTree li#"+node.id+" #docCountBadge").html(docCount);
  		    	$("#docFolderTree li#"+node.id+" .jstree-checkbox").attr('data-original-title','Show In Document View');
  		      }
  		    })
  		    
  		    //display the searchbox and label on the basis of no of node present
  		    var searchBox = $("#documentfolders>.jstree-searchbox"),
  		    	textInfo = $("#documentfolders>.text-info");
  		  	if(data.nodes.length>1){
  		  		searchBox.show()
  		  	}
  		  	else if(data.nodes.length==0){
  		  		searchBox.hide()
  		  	}
  		  	else if(data.nodes.length==1){
		  		var node = $("#docFolderTree").jstree().get_node(data.nodes[0]);
		  		(node.children_d.length)?searchBox.show():searchBox.hide();
  		  	}
  		  	
  		  	(data.nodes.length>=1) ? textInfo.hide() : textInfo.show();
  		  	
  		}).bind('create_node.jstree', function(event,data){
  			if(data.node.original.nodeData.folderType!=null){
  				documentListByView[SearchEvents.getSelectedSearchView()].push(data.node.original.nodeData);
  			}
      }).bind('delete_node.jstree', function(data, event){
          var deletedNodes = event.node.children_d;
          var deletedDocumentLists = [];
          var view = SearchEvents.getSelectedSearchView();
          for(var nodeIndex=0; nodeIndex<deletedNodes.length; nodeIndex++){
              var node = $("#docFolderTree").jstree().get_node(deletedNodes[nodeIndex]);
              if(node.original.nodeData.folderType!=undefined || node.original.nodeData.folderType!=null){
                deletedDocumentLists.push(node.original.nodeData.id);
              }
          }
          var newDocumentList = [];         
          for(var listIndex=0; listIndex < documentListByView[view].length; listIndex++){
        	var documentListNode_id = documentListByView[view][listIndex].id;
            if(!_.contains(deletedDocumentLists, documentListNode_id)) {            	
            	newDocumentList.push(documentListByView[view][listIndex]);
            }
          }
          documentListByView[view] = newDocumentList;
      }).bind('close_node.jstree', function(event, data){
  			var closeIcon;
  			if(data.node.icon == "jstree-FolderOpened")
  				closeIcon = "jstree-folderClosed";
  			else if(data.node.icon == "jstree-folderOpenedShared")
  				closeIcon = "jstree-folderClosedShared";
  			else
  				closeIcon = "jstree-folderClosedSharedReadOnly";
  			
  			//on closing the folder changing the icon to docFolder-close
  			data.instance.set_icon(data.node, closeIcon);
  		}).bind('open_node.jstree', function(event, data){
  			//on opening the folder changing the icon to docFolder-open
  			var openIcon;
  			if(data.node.icon == "jstree-folderClosed")
  				openIcon = "jstree-FolderOpened";
  			else if(data.node.icon == "jstree-folderClosedShared")
  				openIcon = "jstree-folderOpenedShared";
  			else
  				openIcon = "jstree-folderOpenedSharedReadOnly";
  			
  			data.instance.set_icon(data.node, openIcon);
  			var childrenElements = data.node.children_d;
  			childrenElements.forEach(function(id){
  				var node = $("#docFolderTree").jstree().get_node(id);
  				var isFolder = node.original.nodeData.folderType;
  				if(isFolder === undefined){
  	  		    	$("#docFolderTree li#"+node.id+">a").after(CUSTOMFILTER_DROPDOWN_TEMPLATE_URL);
  	  		    }
  				else if(isFolder===null){
  					$("#docFolderTree li#"+node.id+">a").after(DOCUMENTFOLDER_DROPDOWN_TEMPLATE_URL);
  				}
  				else{
  					var docCount = node.original.nodeData.docCount;
  					$("#docFolderTree li#"+node.id+">a").after(DOCUMENTFOLDERLIST_DROPDOWN_TEMPLATE_URL);
  					$("#docFolderTree li#"+node.id+" #docCountBadge").html(docCount);
  					$("#docFolderTree li#"+node.id+" .jstree-checkbox").attr('data-original-title','Show In Document View');
  				}
  			})
  		}).on('hover_node.jstree',function(e,data){
  			$("#"+data.node.id+" .jstree-checkbox").tooltip({placement: 'bottom'});
  		})
    	 var to = false;
    	    $('#plugins4_q').keyup(function () {
    	      if(to) { clearTimeout(to); }
    	      to = setTimeout(function () {
    	        var v = $('#plugins4_q').val();
    	        $('#docFolderTree').jstree(true).search(v);
    	      }, 250);
    	    });
    }
    
    function loadFolders() {
    	
    	var $documentfolders = $('#documentfolders');
    	$documentfolders.empty();
    	$documentfolders.append(documentgroupTemplate);
    	$('#folder-loader').show();
    	$documentfolders.append('<div class="row jstree-searchbox"><input placeholder="Search Folders/Filters/Lists" type="text" id="plugins4_q" value="" class="input pull-left"></div>');
        $documentfolders.append('<div id="docFolderTree"></div>');
        loginUser = $('.userUsername').text();
        
    	var view = SearchEvents.getSelectedSearchView();
    	
    	if (!view) {
            return;
        }
    	
    	$.ajax(GET_FOLDER_DATA_URL, {
            type: 'POST',
            dataType : 'json',
            data : { searchView: view }
    	}).done(function(data){ 
    		var count=0;
    		if(data.folders) {
	    		for(var i=0; i<data.folders.length; i++) { 
	    			if(data.folders[i].parent==null){
	    				continue;
	    			}
	    			else {
	    				var folderType;
	    				if(data.folders[i].roles.length && data.folders[i].owner!=loginUser.trim() && data.folders[i].readOnly){
	    					//ReadOnly folder
	    					folderType = 'folderClosedSharedReadOnly';
	    				}
	    				else if(data.folders[i].roles.length){
	    					//Shared folder
	    					folderType = 'folderClosedShared';
	    				}
	    				else if(!data.folders[i].roles.length && data.folders[i].owner!=loginUser.trim()){
	    					//restricted folder
	    					folderType = 'folderClosedSharedReadOnly';
	    				}
	    				else{
	    					folderType = 'default';
	    				}
	    				var parent_id = (data.folders[i].parent.parent==null)?"#":data.folders[i].parent.id.toString();
	    				treeData.core.data[count]={"id":data.folders[i].id.toString(),"type": folderType,"parent":parent_id,"a_attr": {"class":"no_checkbox"},"text":data.folders[i].name,"nodeData":data.folders[i]}
	    				count++;
	    			}
	           }
    		}
    		if(data.customFilters){
    			for(var i=0; i<data.customFilters.length; i++){
    				var customFilterType;
    				if(data.customFilters[i].roles.length && data.customFilters[i].owner!=loginUser.trim() && data.customFilters[i].readOnly){
    					//ReadOnly custom filter
    					customFilterType = 'customFilterSharedReadOnly';
    				}
    				else if(data.customFilters[i].roles.length){
    					//Shared custom filter
    					customFilterType = 'customFilterShared';
    				}
    				else{
    					customFilterType = 'customFilter';
    				}
    				treeData.core.data[count]={"id":(data.customFilters[i].id).toString()+"filter","type": customFilterType,"parent":data.customFilters[i].parent.id,"a_attr": {"class":"no_checkbox"},"text":data.customFilters[i].name,"nodeData":data.customFilters[i]}
    				count++
    			}
    		}
    		if(data.lists){
    			for(var i=0; i<data.lists.length; i++){
    				var documentListType;
    				if(data.lists[i].roles.length && data.lists[i].owner!=loginUser.trim() && data.lists[i].readOnly){
    					//ReadOnly document list
    					documentListType = 'documentListSharedReadOnly';
    				}
    				else if(data.lists[i].roles.length){
    					//Shared document list
    					documentListType = 'documentListShared';
    				}
    				else{
    					documentListType = 'documentList';
    				}
    				treeData.core.data[count]={"id":data.lists[i].id.toString(),"type": documentListType,"parent":data.lists[i].parent.id.toString(),"a_attr": {"class":"docView_checkbox"},"text":data.lists[i].name,"nodeData":data.lists[i]}
    				count++;
    			}
    		}
    		
    	   $('#docFolderTree').jstree(treeData)
           bindJstreeEvents();
           documentListByView[view] = data.lists;
           SearchEvents.$.trigger(SearchEvents.FOLDERS_LOADED);
           $('#folder-loader').hide();
       }).error(function(){
       	 	console.log("Error to get the tree data"); 
       });
    }
    
    function appendRoleType(modal_id, folderMeta){
    	$roleTypeSelect = $('#'+modal_id).find('#rolesList').empty().append(USER_ROLES);
    	
    	if(folderMeta!=null){
    		var folderRoles = folderMeta.roles
    		$roleTypeSelect.find('option').each(function(i, e){
        		var role = _.find(folderRoles, function(folderRole){
        			if(folderRole==e.value){
        				$roleTypeSelect.find("option[value="+folderRole+"]").prop("selected", "selected");
        			}
        		})
        	});
    		$('#'+modal_id).find('input[type=checkbox]').prop('checked', folderMeta.readOnly).attr("disabled",false);
    	}
    	
    	if(folderMeta==null || (folderMeta.roles && folderMeta.roles.length==0)){
    		$('#'+modal_id).find('input[type=checkbox]').attr("disabled", true);
    	}
    	
    	$roleTypeSelect.chosen({width: "60%"});
		$roleTypeSelect.trigger("chosen:updated");
		
		$('#'+modal_id).find('#rolesList').on('change', function(event) {
			var selectedRoles = $('#'+modal_id).find('#rolesList').chosen().val();
			if(selectedRoles==null){
				$('#'+modal_id).find('input[type=checkbox]').prop('checked', false).attr("disabled", true);
			}
			else{
				$('#'+modal_id).find('input[type=checkbox]').attr("disabled", false);
			}
        });
    }
    
    var $documentfolders = $('#documentfolders').on('click', '.documentfolderdelete,.documentfolderListdelete,.customFilterdelete', function(evt){
    	var node_id = $(this).closest('.jstree-node').attr('id'),
		node = $("#docFolderTree").jstree().get_node(node_id),
		folderMeta = node.original.nodeData;
    	var modal_title, delete_Message;
    	
    	if(this.className == "documentfolderdelete"){
    		modal_title ="Folder"
    		delete_Message = " folder and all its contents"
    	}
    	else if(this.className == "documentfolderListdelete"){
    		modal_title = "Document List"
    		delete_Message = " document list and all its references"
    	}
    	else if(this.className == "customFilterdelete"){
    		modal_title = "Custom Filter"
    		delete_Message = " custom filter"
    	}
    	var allowDelete = true;    	
    	var allFiltersData = $('#parametricForm').data('filterForm').getFiltersData();
    	
    	// prevent delete if the folder is added as an active filter
    	_.each(allFiltersData.filterFields, function(value, fieldName) {    			
   			var field = _.find(FilterFields[folderMeta.searchView], function(field){
   				return field.custom && field.custom.type === 'DOCUMENTFOLDER';
    		});
    			
    		if(field.name === fieldName) { // show message, prevent delete    			
    			allowDelete = false;    			
    			var   $modal = $('#preventDeleteDocumentFolderDialog').find('[name=label]').text('').end().modal().show();    			
    		}
    	});     		
    	
    	if(allowDelete) {
	        //var	node = $("#docFolderTree").jstree().get_node(folderMeta.id);
	        var listIds = [];
	        listIds = node.children_d;
	        listIds.push(node.id);
	        var   $modal = $('#deleteDocumentFolderDialog').find('strong').text(folderMeta.name).end().modal().show();
	        $modal.find('h3>span').html(modal_title);
	        $modal.find('.modal-body>[name=label]').html(delete_Message);
	        var view = SearchEvents.getSelectedSearchView();
	        $modal.find('.confirm').click(function(){
	        	$("#delete-loader").show();
	        	var $submitEle = $(this);
	        	$submitEle.prop('disabled',true);
	            $.ajax(DELETE_FOLDER_URL, {
	                type: 'POST',
	                data: {ids: listIds,searchView: view}
	            }).done(function(){
	            	$("#docFolderTree").jstree().delete_node(node);
	            	$('#docFolderTree').jstree(true).redraw(true);
	                $modal.modal('hide');
	            }).error(function(data){
	            	$modal.modal('hide');
	            	$('#confirmDialog').confirmDialog({
	    				title: 'Delete Alert',
	    				message: "<strong>"+"Cannot Delete "+"</strong>"+ "<br>" + data.responseJSON.errorDetail,
	    				callbackObj: null,
	    				yesBtnClass: 'btn-primary',
	    				yesLabel: 'OK',
	    				noLabel: null
	    			});
	            }).always(function(){
	            	$("#delete-loader").hide();
	            	$submitEle.prop('disabled',false);
	            });
	        });
	        
	        $modal.one('hidden', function() {
	            $modal.find('.confirm').off();
	        });       
    	}
        return false;
    }).on('click', '.documentfolderListedit', function(){
    	$("#importedValues").html('');
    	$('#loadinggif-importedValuesId').show();
    	importedData = false;
    	var node_id = $(this).closest('.jstree-node').attr('id'),
			node = $("#docFolderTree").jstree().get_node(node_id),
			folderMeta = node.original.nodeData,
            $modal = $('#editDocumentFolderListDialog').find('.control-group').removeClass('error').end().modal().show(),
            $label = $modal.find('input[name=label]').val(folderMeta.name),
            $tooltip = $modal.find('input[name=tooltip]').val(folderMeta.tooltip || ''),
            $owner = $modal.find('input[name=owner]').val(folderMeta.owner),
            $isReadOnly = $modal.find('input[type=checkbox]').prop('checked', folderMeta.readOnly),
            loginUser = $('.userUsername').text();
            $importTypeSelect = $modal.find('#importTypeSelect').empty().append(TYPE_OPTIONS);
            appendRoleType("editDocumentFolderListDialog", folderMeta);
                      
        var view = SearchEvents.getSelectedSearchView();
        var originalImportedData = [];
        var typeDetails = importTypesByView[view][$importTypeSelect.val()];
            $.ajax(GET_IMPORTED_VALUES_URL,{
            	type: 'POST',
            	data: {
            		id : folderMeta.id,
            		searchView: view,
            		owner: folderMeta.owner
            	}
            }).done(function(importedValuesData) {
            	for(var i=0; i<importedValuesData.length; i++){
            		originalImportedData.push(importedValuesData[i]);
            	}
            	searchImportedValues(importedValuesData,$modal);
            }).error(function(data) {
            	$label.closest('.control-group').addClass('error');
            }).always(function(){
            	$('#loadinggif-importedValuesId').hide();
            })

        $modal.find('form').off('submit').on('submit', function(){
            $modal.find('.confirm').click();
            return false;
        });
            
        $('#alertEditList').text('');    
        $modal.find('.confirm').click(function(){
            var label = $.trim($label.val());
            var view = SearchEvents.getSelectedSearchView();
            var roles = $modal.find('#rolesList').chosen().val();
            roles = (roles==null)?[]:roles;
            $label.closest('.control-group').toggleClass('error', !label);
            var $submitEle = $(this);
            $submitEle.prop('disabled', true);
            $("#edit-loader").show();
            
            var importedValuesFromUL = []
            $('#importedValues').find('li').each(function(){            
                    importedValuesFromUL.push(this.textContent);
            });
            
            var editedData = {
                    searchView: view,
                    //importData: $importedValues.val().split(",").join("\n"),
                    importData: importedValuesFromUL.join("\n"),
                    ignoreFirstRow: false,
                    importType: importTypesByView[view][folderMeta.folderType], 
                    folderId: folderMeta.id
            };
            var differenceExist = false;
            var leftDifference = _.difference(originalImportedData, importedValuesFromUL);
            var RightDifference = _.difference(importedValuesFromUL, originalImportedData);
            
            if(leftDifference.length || RightDifference.length)
            	differenceExist = true;
            
            if (label) {
                $.ajax(EDIT_FOLDER_URL, {
                    type: 'POST',
                    data: {
                        id: folderMeta.id,
                        label: label,
                        searchView: view,
                        tooltip: $.trim($tooltip.val()),
                        readOnly : $isReadOnly.prop('checked'),
                        roles : JSON.stringify(roles),
                        importedValues: importedValuesFromUL.join("\n"),
                        childNodeIds : JSON.stringify([]),
                        dataDifference : differenceExist
                    }
                }).done(function(){	
                	if(differenceExist){
                		importData(editedData, $submitEle, $modal);
                	}
                	else{
                		importedData=true;
                		$modal.modal('hide');
                		$("#edit-loader").hide();
                	}
                }).error(function(editObj){        	
                	$('#alertEditList').text(editObj.responseJSON.errorDetail);
                    $label.closest('.control-group').addClass('error');
                    $("#edit-loader").hide();
                    $submitEle.prop('disabled', false);
                }).always(function(){
                	
                })
            }
            return false;
        });

        $modal.one('hidden', function() {
            $modal.find('.confirm').off();
            $("#results").text('');
        	$('#searchField').val('');
        	$('#resultsCount').html("");
            if (importedData) {
                SearchEvents.reloadFolders();
            }
        });

        return false;
    }).on('click', '.customFilterEdit', function(){
    	var node_id = $(this).closest('.jstree-node').attr('id'),
		node = $("#docFolderTree").jstree().get_node(node_id),
		folderMeta = node.original.nodeData;
    	$modal = $('#editDocumentFolderDialog').find('.control-group').removeClass('error').end().modal().show(),
    	$name = $modal.find('input[name=label]').val(folderMeta.name),
    	$description = $modal.find('input[name=tooltip]').val(folderMeta.description || ''),
    	$owner = $modal.find('input[name=owner]').val(folderMeta.owner),
    	$isReadOnly = $modal.find('input[type=checkbox]').prop('checked', folderMeta.readOnly),
    	loginUser = $('.userUsername').text();
    	appendRoleType("editDocumentFolderDialog", folderMeta);
    	
    	$modal.find('h3').html("Edit Custom Filter");
    	
    	$modal.find('form').off('submit').on('submit', function(){
            $modal.find('.confirm').click();
            return false;
        });
    	var $importTypeSelect = $modal.find('#importTypeSelect').empty().append(TYPE_OPTIONS);
    	$('#alertEdit').text('');
        $modal.find('.confirm').click(function(){
        	$submitEle = $(this);
        	$submitEle.prop('disabled', true);
        	$("#editFolder-loader").show();
            var name = $.trim($name.val());
            var view = SearchEvents.getSelectedSearchView();
            var roles = $modal.find('#rolesList').chosen().val();
            roles = (roles==null)?[]:roles;
            $name.closest('.control-group').toggleClass('error', !name);
            
            if (name) {
                $.ajax(EDIT_CUSTOMFILTER_URL, {
                    type: 'POST',
                    data: {
                        id: folderMeta.id,
                        name: name,
                        description: $.trim($description.val()),
                        readOnly: $isReadOnly.prop('checked'),
                        roles : JSON.stringify(roles)
                    }
                }).done(function(){
                    $modal.modal('hide');
                    loadFolders();
                }).error(function(editObj){
                	$('#alertEdit').text(editObj.responseJSON.errorDetail);
                    $name.closest('.control-group').addClass('error');
                }).always(function(){
                	$submitEle.prop('disabled', false);
                	$("#editFolder-loader").hide();
                })
            }
            return false;
        });
        
        $modal.one('hidden', function() {
            $modal.find('.confirm').off();
        });
        return false;
    }).on('click', '.documentfolderListaddfilter', function(){
    	var node_id = $(this).closest('.jstree-node').attr('id'),
		node = $("#docFolderTree").jstree().get_node(node_id),
		folderMeta = node.original.nodeData;
        var field = _.find(FilterFields[folderMeta.searchView], function(field){
            return field.custom && field.custom.type === 'DOCUMENTFOLDER';
        });
        
        if (field) {
        		 var filter = $.extend({
                     fieldOp: FILTER_OPERATORS.IS.value,
                     fieldType: PARAMETRIC_FIELDTYPE,
                     filterValue: folderMeta.id
                 }, field);
                 
                 $('#parametricForm').data('filterForm').addFilter(filter, true);
        }

    }).on('click', '.docfolderListimport', function() {
    	var node_id = $(this).closest('.jstree-node').attr('id'),
    		node = $("#docFolderTree").jstree().get_node(node_id),
    		folderMeta = node.original.nodeData;
    	
        //var folderMeta = $(this).closest('.documentfolder').data('documentfolder'),
        var $docFolder = $(this).closest('.documentfolder'),
            $modal = $('#documentImportDialog').find('.control-group').removeClass('error').end().modal().show(),           
            $filename = $modal.find('#importListFilename').removeData().val(''),
            $ignoreHeaderCheck = $modal.find('#ignoreHeaderCheck').prop('checked', true),
            $importFileControl = $modal.find('#importFileControl'),
            $hiddenFileSelector = $modal.find('#hiddenImportSelector'),
            $importTypeSelect = $modal.find('#importTypeSelect').empty().append(TYPE_OPTIONS);
        
        var view = SearchEvents.getSelectedSearchView();
        var importTypeDefault = importTypesByView[view][folderMeta.folderType]
        $modal.find('select option:contains('+importTypeDefault.displayName+')').prop('selected',true);
        
        importedData = false;
        	
        $importFileControl.click(function(event) {
            $hiddenFileSelector.click();
            event.preventDefault();
            
        });
        
        $hiddenFileSelector.click(function(event) {
            this.value = null;
        });
        
        $hiddenFileSelector.change(function() {
            $filename.closest('.control-group').toggleClass('error', false);
            $('.importFile').toggleClass('err-msg', false);
            $(".importFile").hide();
            var selectedFile = this.files[0];
            $filename.val(selectedFile.name);
            $filename.data(SELECTED_FILE_KEY, selectedFile);
            
            return true;
        })
        
        $(".importFile").hide();
        $modal.find('.confirm').click(function(){

            $filename.closest('.control-group').toggleClass('error', !$filename.val());
            if(!$filename.val()){
            	$('.importFile').toggleClass('err-msg');
            	$('.importFile').show();
            }else{
            	$('.importFile').hide();
            }
            var selectedFile = $filename.data(SELECTED_FILE_KEY); 
            var fileReader = new FileReader();
           
            var $submitEle = $(this);
            
            fileReader.onload = function(event) {
                var view = SearchEvents.getSelectedSearchView();
                var typeDetails = importTypesByView[view][$importTypeSelect.val()];
                var data = {
                    searchView: view,
                    importData: event.target.result,
                    ignoreFirstRow: $ignoreHeaderCheck.is(':checked'),
                    importType: typeDetails,
                    folderId: folderMeta.id
                };
                $submitEle.prop('disabled', true);
                $("#import-loader").show();
                importData(data, $submitEle,$modal);
            };
            
            // handle error...
            fileReader.onerror = function(event) {
                $filename.closest('.control-group').toggleClass('error', true);
            };
            
            fileReader.readAsText(selectedFile, "UTF-8");

        });
        
        $modal.one('hidden', function() {
            $modal.find('.confirm').off();
            $hiddenFileSelector.off();
            $importFileControl.off();
            
            if (importedData) {
            	loadFolders();
            }
        });
        return false;
        
    }).on('click', '.docfolderListexport', function() {
    	var node_id = $(this).closest('.jstree-node').attr('id'),
		node = $("#docFolderTree").jstree().get_node(node_id),
		folderMeta = node.original.nodeData;
        //var folderMeta = $(this).closest('.documentfolder').data('documentfolder');
        $('#documentExportDialog').docResultsExport({folderId: folderMeta.id, resultsExport: false});
        return false;
        
    }).on('change', '.documentfolder input[type=checkbox]', function(){
        $(this).closest('.documentfolder').data('documentfolder').selected = this.checked;

        $.ajax(SET_SELECTED_FOLDER_URL + '?' + $.param({searchView: SearchEvents.getSelectedSearchView()}), {
            type: 'POST',
            contentType: 'application/json; charset=UTF-8',
            data: JSON.stringify(_.pluck(getCheckedFolders(), 'id'))
        });
    }).on('click','.addDocFolderChild,.addDocFolderList', function(){
    	$('#alertNewFolder').text('');
    	var nodeId = this.closest("li.jstree-node").id,
    	node = $("#docFolderTree").jstree().get_node(nodeId),
    	folderMeta = node.original.nodeData;
		$('#addDocumentFolderDialog').docFolderCreate({ button: this.className,node_id : nodeId });
		$importTypeSelect = $('#addDocumentFolderDialog').find('#folderTypeSelect').empty().append(TYPE_OPTIONS);
		appendRoleType("addDocumentFolderDialog", folderMeta);
		return false;
    }).on('click','.documentfolderedit', function(){
    	var node_id = $(this).closest('.jstree-node').attr('id'),
    	node = $("#docFolderTree").jstree().get_node(node_id),
    	folderMeta = node.original.nodeData,
    	$modal = $('#editDocumentFolderDialog').find('.control-group').removeClass('error').end().modal().show(),
    	$label = $modal.find('input[name=label]').val(folderMeta.name),
    	$tooltip = $modal.find('input[name=tooltip]').val(folderMeta.tooltip || ''),
    	$owner = $modal.find('input[name=owner]').val(folderMeta.owner),
    	$isReadOnly = $modal.find('input[type=checkbox]').prop('checked', folderMeta.readOnly),
    	loginUser = $('.userUsername').text();
    	appendRoleType("editDocumentFolderDialog", folderMeta);
    	$modal.find('h3').html("Edit Folder");
    	$modal.find('form').off('submit').on('submit', function(){
            $modal.find('.confirm').click();
            return false;
        });
    	
    	var $importTypeSelect = $modal.find('#importTypeSelect').empty().append(TYPE_OPTIONS),
    		prevRoles = folderMeta.roles;
    	
    	$('#alertEdit').text('');
    	
        $modal.find('.confirm').click(function(){
        	$("#editFolder-loader").show();
        	var $submitEle = $(this);
        	$submitEle.prop('disabled', true);
        	var currentRoles = $modal.find('#rolesList').chosen().val(),
        		currentRoles = (currentRoles==null)?[]:currentRoles,
        		leftDifference = _.difference(prevRoles,currentRoles),
        		rightDifferece = _.difference(currentRoles,prevRoles),
        		differenceExist = false,
        		loginUser = $('.userUsername').text();
        	
        	if(leftDifference.length || rightDifferece.length)
        		differenceExist = true;
        		
        	if(node.children_d.length && (differenceExist || folderMeta.readOnly != $isReadOnly.prop('checked')) 
        			&& (!folderMeta.readOnly || folderMeta.owner==loginUser.trim())){
        		$modal.modal('hide');
            	$('#confirmDialog').confirmDialog({
            		title: 'Edit Confirm',
            		message: 'Do you want to assign all the children of this folder to the same roles and permission as this folder?',
            		yesLable: 'Yes',
            		yesCallback: function() { 
            			var children_nodes = node.children_d;
            			editFolder($modal, folderMeta, children_nodes)
            		},
            		noBtnClass: 'btn-primary',
            		noLabel: 'No',
            		noCallback: function() { 
            			var children_nodes = [];
            			editFolder($modal, folderMeta, children_nodes)
            		}
            	});
        	}
        	else{
        		editFolder($modal, folderMeta, []);
        	}
        });
        
        $modal.one('hidden', function() {
            $modal.find('.confirm').off();
        });

        return false;
    }).on('click','.documentfolderCut',function(event, data){
    	var ref = $('#docFolderTree').jstree(true)
    	var node_id = $(this).closest('.jstree-node').attr('id'),
    	node = $("#docFolderTree").jstree().get_node(node_id);
    	sourceNodeId = node_id;
    	ccp_mode = "cut_node";
    	$('a.documentfolderPaste').removeClass('disable-paste');
    	$('li.docFolderCopy').removeClass('docFolderCopy');
    	$('li.docFolderCut').removeClass('docFolderCut');
    	$('li#'+node_id).addClass('docFolderCut');
    	ref.cut(node);
    }).on('click','.documentfolderCopy',function(event, data){
    	var ref = $('#docFolderTree').jstree(true)
    	var node_id = $(this).closest('.jstree-node').attr('id'),
    	node = $("#docFolderTree").jstree().get_node(node_id);
    	sourceNodeId = node_id;
    	ccp_mode = "copy_node";
    	$('a.documentfolderPaste').removeClass('disable-paste')
    	$('li.docFolderCopy').removeClass('docFolderCopy');
    	$('li.docFolderCut').removeClass('docFolderCut');
    	$('li#'+node_id).addClass('docFolderCopy');
    	ref.copy(node);
    }).on('click','.documentfolderPaste',function(event, data){
    	var ref = $('#docFolderTree').jstree(true);
    	var dest_nodeId = $(this).closest('.jstree-node').attr('id'),
    	dest_node = $("#docFolderTree").jstree().get_node(dest_nodeId),
    	source_node = $("#docFolderTree").jstree().get_node(sourceNodeId),
    	source_folder_type = source_node.original.nodeData.folderType,
    	dest_folder_type = dest_node.original.nodeData.folderType;
    	$('#folder-loader').show();
    	$('#docFolderTree').hide();
    	$('.jstree-searchbox').hide();
    		$.ajax(CCP_DOC_URL, {
    			type: 'POST',
    			dataType : 'json',
    			data: {
    				sourceId : sourceNodeId,
    				destId : dest_nodeId,
    				mode : ccp_mode,
    				childNodeIds : JSON.stringify(source_node.children_d)
    			}
    		}).done(function(data){
    			$('#folder-loader').hide();
    			loadFolders();
    		}).error(function(data){
    			
    			$('#docFolderTree').show();
    	    	$('.jstree-searchbox').show();
    	    	$('#folder-loader').hide();
    			$('#confirmDialog').confirmDialog({
    				title: 'Paste Alert',
    				message: "<strong>"+"Cannot Paste "+"</strong>"+ "<br>" + data.responseJSON.errorDetail,
    				callbackObj: null,
    				yesBtnClass: 'btn-primary',
    				yesLabel: 'OK',
    				noLabel: null
    			});
    			$('li.docFolderCopy').removeClass('docFolderCopy');
    			$('li.docFolderCut').removeClass('docFolderCut');
    		}).always(function(){
    			
    			ccp_mode = "";
    			$('a.documentfolderPaste').addClass('disable-paste');
    		});
    }).on('click','.loadCustomFilter', function(event, data){
    	var view = SearchEvents.getSelectedSearchView(),
    	node_id = $(this).closest('.jstree-node').attr('id'),
    	node = $("#docFolderTree").jstree().get_node(node_id),
    	folderMeta = node.original.nodeData;
    	$.ajax(GET_FILTER_ITEM, {
            type: 'POST',
            data: {
            	filterId: folderMeta.id,
            	searchView: view
            }
        }).done(function(filterItem){
        	loadSavedFilter(filterItem)
        }).error(function(data){
        	console.log("Error");
        })
    });
    
    $('a[href=#documentfolders]').on('shown', function() {
        $addButton.show();
    }).closest('li').siblings('li').find('a[data-toggle="tab"]').on('shown', function(){
        $addButton.hide();
    });
    
    function loadSavedFilter(filterItem){
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
        
        //this.element.find('button.close').click();
        
        setTimeout(function() {
        	$('#searchGo').click();
        }, 1000);
    }
    
    var $addButton = $('#documentfoldersadd').click(function(){
    		$('#alertNewFolder').text('');
    		$('#addDocumentFolderDialog').docFolderCreate({ button: this.id,node_id: null });
    		appendRoleType("addDocumentFolderDialog", null);
    		return false;
    	});

    $('.search-controls').on(SearchEvents.SEARCH_VIEW_CHANGED, function() {
        loadImportTypes();
        loadFolders();
    });

    SearchEvents.getCheckedFolders = getCheckedFolders;
    
    SearchEvents.getDocumentLists = function(){
    	return documentListByView[SearchEvents.getSelectedSearchView()];
    }
    SearchEvents.reloadFolders = function() {
        loadFolders();
    };
    
    SearchEvents.getRestrictedLists = function() {
    	var lists = documentListByView[SearchEvents.getSelectedSearchView()];
    	return _.filter(lists, function(list){
    		return list.restricted;
    	})
    };
    
    SearchEvents.getNonRestrictedLists = function(){
    	var lists = documentListByView[SearchEvents.getSelectedSearchView()];
    	return _.filter(lists, function(list){
    		return !list.restricted;
    	})
    };
    
    function getCheckedFolders() {
        return _.map($documentfolders.find('li[aria-selected="true"] .docView_checkbox'), function(e){
        	var node = $("#docFolderTree").jstree().get_node(e.parentElement.id); 
            return node.original.nodeData;
        });
    }
    
    function editFolder($modal, folderMeta, childrens){
    	  var $label = $modal.find('input[name=label]'),
    	  $tooltip = $modal.find('input[name=tooltip]'),
    	  $readOnly = $modal.find('input[type=checkbox]'),
		  label = $.trim($label.val()),
		  view = SearchEvents.getSelectedSearchView(),
		  roles = $modal.find('#rolesList').chosen().val(),
		  roles = (roles==null)?[]:roles;
		  
		  $label.closest('.control-group').toggleClass('error', !label);
		  
		  if (label) {
			  $.ajax(EDIT_FOLDER_URL, {
				  type: 'POST',
				  data: {
					  id: folderMeta.id,
					  label: label,
					  searchView: view,
					  tooltip: $.trim($tooltip.val()),
					  readOnly : $readOnly.prop('checked'),
					  roles : JSON.stringify(roles),
					  importedValues: "",
					  childNodeIds : JSON.stringify(childrens),
					  dataDifference : false
				  }
			  }).done(function(){
				  if(!childrens.length){
					  $modal.modal('hide');  
				  }
				  loadFolders();
			  }).error(function(editObj){
				  $('#alertEdit').text(editObj.responseJSON.errorDetail);
				  $label.closest('.control-group').addClass('error');
				  $modal.modal('show');
			  }).always(function(){
				  $("#editFolder-loader").hide();
				  $modal.find('button.confirm').prop('disabled', false);
			  })
		  }
    }

    function searchImportedValues(textareaData, $modal){
    	var list = "";
    	$('#alertSearch').show();
    	$('#alertSearch').html('<p>Each value should be in a new line in the Items List box below.</p>');
    	showHideAlertSearch();
        for(i=0; i<textareaData.length; i++){
        	var className='found'+textareaData[i];
        	className=className.replace(/ +/g, "");
        	className=className.replace(/[^a-z0-9]|\s+|\r?\n|\r/gmi, "");
        	list +="<li style='height:17px' id="+className+"><div contenteditable='true' class='pull-left' style='width:90%'>"+textareaData[i]+"</div><a style='width:10%' title='Remove From List' class='pull-right' data-original-title='Delete' id='deleteDocValue'><i class='icon-remove'></i></a></li>";
        }
        $("#importedValues").html(list);
        
        $modal.find(".clearItems").click(function(){
			$("#clearItemloader").show();
			setTimeout(function(){
				$("ul#importedValues li").remove();	
				textareaData = [];
			},1000);
		});
		
		$("ul#importedValues li:last-child").on('remove', function(){
			$("#clearItemloader").hide();
		});

    	$modal.find('#add').click(function(){
    		    $("#importedValues").html('');
    		    $('#alertSearch').html('');
    		    var mlist='';
    			var addValuesList =[]
    		    var searchTextBoxValue=$('#name').val();
    		    addValuesList=searchTextBoxValue.split("\n");//split on the basis of newline in the textarea
    		    for(i=0; i<addValuesList.length; i++){
    		    	if(!(addValuesList[i]==""))
    		    	textareaData.push(addValuesList[i]);
    		    } 
    		
    		    textareaData.sort(function(a,b) {
    		    	  if (isNaN(a) || isNaN(b)) {
    		    	    return a > b ? 1 : -1;
    		    	  }
    		    	  return a - b;
    		    });
    		    
    			for(i=0; i<textareaData.length; i++){
    				var className='found'+textareaData[i];
    				className=className.replace(/ +/g, "");
    				className=className.replace(/[^a-z0-9]|\s+|\r?\n|\r/gmi, "");
    		        mlist +="<li style='height:17px' id="+className+"><div contenteditable='true' class='pull-left' style='width:90%'>"+textareaData[i]+"</div><a style='width:10%' title='Remove From List' class='pull-right' data-original-title='Delete' id='deleteDocValue'><i class='icon-remove'></i></a></li>";
    		    }
    		    $("#importedValues").html(mlist);
    		    $('#name').val('');
    		    return false;
    	});
    	var array;
        $modal.find('#check').click(function(){
        	var name = $('#name').val();
        	$('#alertSearch').html('');
            $('#importedValues li').removeClass('found');
            
            
            var search = name.toUpperCase();
            array = jQuery.grep(textareaData, function(value) {
                return value.toUpperCase().indexOf(search) >= 0;
            });
            
            for(var i=0; i<array.length; i++){
            	classNamePrefix=array[i].replace(/ +/g, "");
            	classNamePrefix=classNamePrefix.replace(/[^a-z0-9]|\s+|\r?\n|\r/gmi, "");
            	$('#found'+classNamePrefix).addClass('found');
            }
            if(array.length>0){
            classNamePrefix=array[0].replace(/ +/g, "");
        	classNamePrefix=classNamePrefix.replace(/[^a-z0-9]|\s+|\r?\n|\r/gmi, "");
        	$('#found'+classNamePrefix).removeClass('found');
        	$('#found'+classNamePrefix).addClass('found-next');
        	var elmnt = document.getElementById('found'+classNamePrefix);
			elmnt.scrollIntoView();
			$('#check').hide();
			$('#nextValue').show();
            }
            else{
            	$('#alertSearch').html('<p>No matches found for '+name+'. Please click on <b>+</b> to add.</p>');
            	showHideAlertSearch();
            }
        });
        
        var nextIndex = 1;
        var prevIndex = 0;
        $modal.find('#nextValue').click(function(){
        	if(array.length>1){
        	classNamePrefix=array[nextIndex].replace(/ +/g, "");
        	classNamePrefix=classNamePrefix.replace(/[^a-z0-9]|\s+|\r?\n|\r/gmi, "");
        	
        	classNameprefixBefore = array[prevIndex].replace(/ +/g, "");
        	classNameprefixBefore=classNameprefixBefore.replace(/[^a-z0-9]|\s+|\r?\n|\r/gmi, "");
        	
        	$('#found'+classNamePrefix).addClass('found-next');
        	$('#found'+classNamePrefix).removeClass('found');
        	
        	$('#found'+classNameprefixBefore).removeClass('found-next');
        	$('#found'+classNameprefixBefore).addClass('found');
        	
        	var elmnt = document.getElementById('found'+classNamePrefix);
			elmnt.scrollIntoView();
			if(nextIndex==array.length-1){
				nextIndex=0;
				prevIndex=array.length-1;
				}
			else{
				prevIndex=nextIndex;
				nextIndex++;
				}
        	}
        	else{
        		$('#alertSearch').html('<p>No more values like '+ array[0] +'</p>');
        		showHideAlertSearch();
        	}
        });
        
        $('#name').bind('keyup', function(){
        	var searchValue = this.value;
        	$('ul#importedValues li').each(function(index){
        		var item = $(this);
        		item.removeClass('found');
        		if($(this).find('div').html().indexOf(searchValue) != -1 && searchValue.length){
        			item.addClass('found');
        		}
        	});
        	var firstSearch = $('ul#importedValues li.found')[0];
        	if(firstSearch!==undefined)
        	firstSearch.scrollIntoView();
        	
        	nextIndex=1;
        	prevIndex=0;
        	$('#nextValue').hide();
        	$('#check').show();
        	$('ul#importedValues>li.found-next').removeClass('found-next');
        })
        
        $modal.one('hidden', function() {
            $modal.find('.confirm').off();
            $modal.find('.cancel').off();
            $modal.find('#check').off();
            $modal.find('#add').off();
            $modal.find('#nextValue').off();
            
            $('#name').val('');
            $('#alertSearch').html('');
            $('#check').show();
			$('#nextValue').hide();
        });
        
        $("ul#importedValues").on("click", "a#deleteDocValue", function(e) {
            e.preventDefault();
            var value = this.parentNode.textContent;
            var index = textareaData.indexOf(value);
            if (index > -1) {
            	textareaData.splice(index, 1);
            }
            $(this).parent().remove();
        });
    }
    
    function showHideAlertSearch(){
    	$('#alertSearch').fadeIn('slow').delay(4000).fadeOut('slow');
    }
    
    function importData(data, $submitEle, $modal) {
        $.ajax(DOC_IMPORT_URL, {
            type : 'POST',
            contentType: 'application/json; charset=UTF-8',
            dataType : 'json',
            data : JSON.stringify(data)
        }).done(function(docFolderObj){
            importedData = true; 
            if(docFolderObj.documentCount>=0){
            	var docMessage=docFolderObj.documentMessage.replace(/\n/g, "<br/>");
            	$(".modal-body > div > div i").removeClass("big-icon-exclamation-sign");
            	$(".modal-body > div > div i").removeClass("error-icon");
            	$(".modal-body > div > div i").addClass("success-icon");
            	$('#confirmDialog').confirmDialog({
    				title: 'Data Import',
    				message: docMessage,
    				callbackObj: null,
    				yesBtnClass: 'btn-primary',
    				yesLabel: 'OK',
    				noLabel: null
    			});
            }
            else if(docFolderObj.documentCount==-1){
            	$(".modal-body > div > div  i").removeClass("big-icon-exclamation-sign");
            	$(".modal-body > div > div i").removeClass("success-icon");
            	$(".modal-body > div > div i").addClass("error-icon");
            	$('#confirmDialog').confirmDialog({
					title: 'Import Error',
					message: "<strong>"+"Error in importing"+"</strong>"+"<br>"+docFolderObj.documentMessage,
					callbackObj: null,
					yesBtnClass: 'btn-primary',
					yesLabel: 'OK',
					noLabel: null
    			});    
            }
        }).fail(function() {
        		$(".modal-body > div > div  i").removeClass("big-icon-exclamation-sign");
        		$(".modal-body > div > div i").removeClass("success-icon");
        		$(".modal-body > div > div i").addClass("error-icon");
        		
    			$('#confirmDialog').confirmDialog({
					title: 'Import Error',
					message: "<strong>"+"Error in importing"+"</strong>"+"<br>"+docFolderObj.documentMessage+"<br>"+" Please check the log file for details.",
					callbackObj: null,
					yesBtnClass: 'btn-primary',
					yesLabel: 'OK',
					noLabel: null
    			});                
                
        }).always(function() {
        	if($modal.selector=="#documentImportDialog"){
        		$submitEle.prop('disabled', false);
        		$("#Import-loader").hide();
        	}
        	else if($modal.selector=="#editDocumentFolderListDialog"){
        		$("#edit-loader").hide();
        	}
            $modal.modal('hide');
        });
    }
});