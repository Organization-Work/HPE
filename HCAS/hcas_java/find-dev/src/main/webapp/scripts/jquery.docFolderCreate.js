;(function($, window, document, undefined) {
    
    var ADD_FOLDER_URL = 'ajax/documentfolder/addFolder.json';
    var GET_ROOT_URL = "ajax/parametric/getRootFolder.json";
    var GET_PARENT_URL = "ajax/parametric/getParentFolder.json"
    
    var DEFAULT_OPTIONS = {
        parentCallback: null
    };
    
    var roles = [], parentFolder;
    var callbackData;
    
    $.widget('search.docFolderCreate',  {
        options: {
        
        },
        
        _init: function() {
            
            this.options = _.extend({}, DEFAULT_OPTIONS, this.options);
            
            callbackData = null;
            
            var $element = this.element;
            $element.find('.control-group').removeClass('error').end();
            $element.find('input[name=label]').val('');
            $element.find('input[name=tooltip]').val('');
            $element.find('input[type=checkbox]').prop('checked', false);
            
            if(this.options.button == "addDocFolderList"){
            	$element.find('h3').html('Add New Document List');
            	$element.find('.add-text').html('Add Document List');
            }
            else{
            	$element.find('h3').html('Add New Folder');
            	$element.find('.add-text').html('Add Folder');
            }
            
            this._showModal();    
            
            //roles = this.getListedRoles();
            
            if(this.options.button == "documentfoldersadd" ||  this.options.button == "addDocFolderChild"){
            	$("#folderType").css("display", "none");
            }
            else{
            	$("#folderType").css("display", "block");
            }
        },
        
        _create: function() {
            var $modal = this.element;
            var $widget = this;
            var parentId;
            
            /*$modal.find("#rolesList").on('change', function(event){
            	roles = $widget.getListedRoles()
            });*/
       
            $modal.find('.confirm').click(function(){  
            $("#addFolderloader").show();
            $(this).prop('disabled',true); 
            if($widget.options.node_id==null){		
                		$.ajax({
                			dataType : 'json',
                			type: 'GET',
                			url: GET_ROOT_URL,
                			success: function(data){
                				parentId = data.id;
                				$widget._confirmCreate($modal,$widget,parentId);
                			}
                		});
            	}
            	else{
            		parentId =$widget.options.node_id
            		$widget._confirmCreate($modal,$widget,parentId);
            	}
            	return false;
            });
            
            $modal.on('hidden', function() {
                $widget.options.parentCallback && $widget.options.parentCallback(callbackData);        
            }).find('form').on('submit', function(){
                $modal.find('.confirm').click();
                return false;
            }); 
        },
        
        _showModal: function() {
            this.element.modal('show');
        },
        
        _hideModal: function() {
            this.element.modal('hide');
        },
        
        destroy: function() {
        },
        
        _confirmCreate: function($modal,$widget,parentId){
        		
            	var $label = $modal.find('input[name=label]'),                	
                $tooltip = $modal.find('input[name=tooltip]'),
                $isReadOnly = $modal.find('input[type=checkbox]'),
                label = $.trim($label.val()),
                folderType = (this.options.button == "documentfoldersadd" ||  this.options.button == "addDocFolderChild")?null:$.trim($modal.find('#folderTypeSelect').val());
            

            	$label.closest('.control-group').toggleClass('error', !label);
            	var $this = $widget;
            	
            	roles = this.getListedRoles();
    		
            	if (label){
            		$.ajax(ADD_FOLDER_URL, {
            			type: 'POST',
            			data: {
            				label: label,
            				searchView: SearchEvents.getSelectedSearchView(),
            				tooltip: $.trim($tooltip.val()),
            				isReadOnly: $isReadOnly.prop('checked'),
            				folderType: folderType,
            				parentId: parentId,
            				folderRoles: JSON.stringify(roles)
            			}
            		}).done(function(folder){
            			$this.createDocFolder(parentId, folder);
            			$this._hideModal();
            		}).error(function(data){
            			$('#alertNewFolder').text(data.responseJSON.errorDetail);
            			$label.closest('.control-group').addClass('error');
            		}).always(function(){
            			$("#addFolderloader").hide();
            			$modal.find('button.confirm').prop('disabled', false);
            		})
            	}
        },
        createDocFolder: function(parentId,folder){
    		var ref = $('#docFolderTree').jstree(true),folderType;
    		var checkBoxClass;
    		
    		if(folder.folderType!=null){
    			folderType = (roles===null)?"documentList":"documentListShared";
    			checkBoxClass = "docview-checkbox";
    		}
    		else{
    			folderType = (roles===null)?"default":"folderClosedShared";
    			checkBoxClass = "no_checkbox";
    		}
    		
    		if(folder.parent.name == "root") { 
    				$("#docFolderTree").jstree("create_node", null, {"id":folder.id, "type":folderType,"parent":folder.parent.id,"a_attr": {"class":checkBoxClass},"text":folder.name,"nodeData":folder}, "last", function (node) {
    				this.edit(node);
    				ref.redraw(true);
    			});
    			return false; 
    		}

    		var sel = ref.create_node(parentId, {"id":folder.id,"type":folderType,"parent":folder.parent.id,"a_attr": {"class":checkBoxClass},"text":folder.name,"nodeData":folder});
    		if(sel) {
    			ref.edit(sel);
    			ref.redraw(true);
    		}
        },
        getListedRoles: function() {
            return $("#rolesList").chosen().val()
        }
    });

})(jQuery, window, document);
 