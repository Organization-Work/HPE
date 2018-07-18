/*
 * Fuel UX Tree
 * https://github.com/ExactTarget/fuelux
 *
 * Copyright (c) 2012 ExactTarget
 * Licensed under the MIT license.
 */

(function($){

	//var $ = require('jquery');


	// TREE CONSTRUCTOR AND PROTOTYPE

	var Tree = function (element, options) {
		this.$element = $(element);
		this.options = $.extend({}, $.fn.tree.defaults, options);

        
		this.$element.on('click', '.tree-item', $.proxy( function(ev) { 
		    this.selectItem(ev.currentTarget);  
		    
		    } ,this));
		
		this._getFolderContent().on('click', '.tree-item', $.proxy( function(ev) { 
		    var isEditing = $(ev.currentTarget).find('.tree-item-name').prop('contentEditable') === "true";
		    if (!isEditing) {
		        this.selectItem(ev.currentTarget);
		    }     		    
		    
		    } ,this));
		    
		
		this.$element.on('click', '.tree-folder-header', $.proxy( function(ev) { 
		    this.$element.trigger('foldertree-selected', $(ev.currentTarget).data());
		    this.selectFolder(ev.currentTarget); 
		    
		}, this));
		
		
        
		//this.render($entity);
	};

	Tree.prototype = {
		constructor: Tree,
		
		initFilterTree: function() {
		    this.$element.find('.folder-refreshed').removeClass('folder-refreshed');
		    
		    if(this.$element.find('.tree-folder').length === 1) {
    			var $folderContentLoader = this._getFolderContent().find('.content-loader');
    			$folderContentLoader.show();
		        
		    } else {
		        // reload the selected folder.
		        var $selectedFolder = this.$element.find('.tree-folder-selected');
		        if ($selectedFolder.length) {
		            /*
		            var $parent = $selectedFolder.parent();
    		        var $treeFolderContent = $parent.find('.tree-folder-content:eq(0)');
    		        $treeFolderContent.children().remove();
    		        */
    		        //$selectedFolder.parent().find('.icon-folder-open').eq(0).removeClass('icon-folder-close icon-folder-open').addClass('icon-folder-close');
    		        
    		        $selectedFolder.click();
    		    }
		        
		    }
		    
		},
		
		render: function () {
			this.populate(this.$element);
		},
		
		renameEntry: function($contentEntry, newData, isFolder) {
		    var $selectedFolder = this.$element.find('.tree-folder-selected');
		    $selectedFolder.removeClass('folder-refreshed');
		    
		    if (isFolder) {
		        var $folderHeader = $contentEntry.data('treeSource').find('> .tree-folder-header');
		        $folderHeader.removeData();
		        $folderHeader.data(newData);
		        $folderHeader.find('.tree-folder-name').text(newData.name);    
		    } else {
		        $contentEntry.removeData();
		        $contentEntry.data(newData);    
		    }
		},
		
		
		deleteEntry: function($contentEntry, isFolder) {
		    var $selectedFolder = this.$element.find('.tree-folder-selected');
		    $selectedFolder.removeClass('folder-refreshed');
		    if (isFolder) {
		        $contentEntry.data('treeSource').remove();
		    }
		    
		    $contentEntry.remove();
		    
		},
		
		loadRootFolder: function(folderData) {
		    var $folderContentLoader = this._getFolderContent().find('.content-loader');
		    $folderContentLoader.hide();
			
	            var $entity = this._createFolderEntity(folderData);
                    this.$element.append($entity);
                    $entity.find('.tree-folder-header').click();
		},
		
		createdTreeFolder: function(folderData) {
		    var $selectedFolder = this.$element.find('.tree-folder-selected');

	        var $newFolder = this._createFolderEntity(folderData);

	        $selectedFolder.parent().find('.tree-folder-content:eq(0)').append($newFolder);

		    var $folderContent = this._getFolderContent();
		    var $contentList = $folderContent.find('.content-list');
		    var $clone = $newFolder.clone().data('tree-source', $newFolder);
		    
	        $selectedFolder.data().children && $selectedFolder.data().children.push($clone);
		    $contentList.append($clone);
		    
		},
		
		getSelectedItem: function() {
		    // should be one only.
		    return this._getFolderContent().find('.tree-item.tree-selected');
		},
		
		getSelectedFolderId: function() {
		    var $selectedFolder = this.$element.find('.tree-folder-selected');
		    return $selectedFolder.data().id;
		},
		
		addNewFolder: function() {
		    var $selectedFolder = this.$element.find('.tree-folder-selected');
		    //var parentFolderId = _.isNumber($selectedFolder.data().id) ? $selectedFolder.data().id : $selectedFolder.data().name;
		    var $newFolder = this._createFolderEntity({name: 'New_Folder', type: 'FOLDER', parentFolderId: $selectedFolder.data().id});
		    
		    //$selectedFolder.parent().find('.tree-folder-content:eq(0)').append($newFolder);
		    //$selectedFolder.data().children && $selectedFolder.data().children.push($newFolder);
		    
		    // add the foldercontent list.
		    var $folderContent = this._getFolderContent();
		    var $contentList = $folderContent.find('.content-list');
		    //var $clone = $newFolder.clone().data('tree-source', $newFolder);
		    
		    $contentList.find('.last-added-folder').removeClass('last-added-folder');
            
            var $nameElement = $newFolder.find('.tree-folder-name');
		    $nameElement.addClass('last-added-folder new-folder');
            
		    $nameElement.attr('contentEditable', true);
		    $contentList.append($newFolder);
		    
		    $contentList.find('.last-added-folder').focus();
		    
		},
		
		_getFolderContent: function() {
		    var $folderContent = this.$element.closest('.filterDialogForm').find(this.options.folderContent);
		    
		    return $folderContent;
		},
		
		getMatchingTreeEntity: function(name, isFolder) {
		    
		    var selector = isFolder ? '.content-list .tree-folder-name' : '.content-list .tree-item-name';
		    
		    var $entities = this._getFolderContent().find(selector).not('.last-added-folder, .editing');
		    if ($entities) {
    		    var $matchedItems = $entities.filter(function() {
    		        return $(this).text() === name;
    		    });
    		    
    		    
    		    
    		    if ($matchedItems.length > 0) {
    		        return $matchedItems.first().parent();
    		    }
    		}
		    
		    return null;
		    
		},
				
		_createFolderEntity: function(value) {
			var $folderEntity = this.$element.find('.tree-folder:eq(0)').clone().show();
			$folderEntity.find('.tree-folder-name').html(value.name);
			$folderEntity.find('.tree-loader').html(this.options.loadingHTML);
			$folderEntity.find('.tree-folder-header').data(value);
			
			return $folderEntity;
		    
		},

		populate: function ($el) {
			var self = this;
			var $parent = $el.parent();
			var loader = $parent.find('.tree-loader:eq(0)');
			
			var $folderContent = this._getFolderContent();
			
			var $folderContentLoader = $folderContent.find('.tree-loader:eq(0)');
			

			loader.show();
			$folderContent.find('.content-list').children().detach();
			$folderContentLoader.show();
			
			this.options.dataSource.data($el.data(), function (items) {
			    
				loader.hide();
				$folderContentLoader.hide();

			    var folderContents = [];
			    var $contentList = $folderContent.find('.content-list');
			    
			    $el.addClass('folder-refreshed');
			    
			    if ($el.hasClass('tree-folder-header')) {
			        $parent.find('.tree-folder-content:eq(0)').children().remove();
			    }
			    
				$.each( items.data, function(index, value) {
					var $entity;

					if(value.type === "FOLDER") {
						$entity = self.$element.find('.tree-folder:eq(0)').clone().show();
						$entity.find('.tree-folder-name').html(value.name);
						$entity.find('.tree-loader').html(self.options.loadingHTML);
						$entity.find('.tree-folder-header').data(value);
					} else if (value.type === "ITEM") {
						$entity = self.$element.find('.tree-item:eq(0)').clone().show();
						$entity.find('.tree-item-name').html(value.name);
						$entity.data(value);
					}

					// Decorate $entity with data making the element
					// easily accessable with libraries like jQuery.
					//
					// Values are contained within the object returned
					// for folders and items as dataAttributes:
					//
					// {
					//     name: "An Item",
					//     type: 'item',
					//     dataAttributes = {
					//         'classes': 'required-item red-text',
					//         'data-parent': parentId,
					//         'guid': guid
					//     }
					// };

					var dataAttributes = value.dataAttributes || [];
					$.each(dataAttributes, function(key, value) {
						switch (key) {
						case 'class':
						case 'classes':
						case 'className':
							$entity.addClass(value);
							break;

						// id, style, data-*
						default:
							$entity.attr(key, value);
							break;
						}
					});
					
					
					
					if(value.type === "FOLDER") {
					    $el.hasClass('tree-folder-header') ? $parent.find('.tree-folder-content:eq(0)').append($entity) : $el.append($entity);
					    var $clone = $entity.clone().data('tree-source', $entity);
					    
					    folderContents.push($clone);
					} else {
					    folderContents.push($entity);
					}
					
					
				});
                
				$contentList.append(folderContents);
				
				$el.data().children = folderContents;

				// return newly populated folder
				self.$element.trigger('loaded', $parent);
			});
		},

		selectItem: function (el) {
			var $el = $(el);
			var $all = this._getFolderContent().find('.tree-selected');
			var data = [];

			if (this.options.multiSelect) {
				$.each($all, function(index, value) {
					var $val = $(value);
					if($val[0] !== $el[0]) {
						data.push( $(value).data() );
					}
				});
			} else if ($all[0] !== $el[0]) {
				$all.removeClass('tree-selected')
					.find('i').removeClass('icon-ok').addClass('tree-dot');
				data.push($el.data());
			}

			var eventType = 'selected';
			if($el.hasClass('tree-selected')) {
				eventType = 'unselected';
				$el.removeClass('tree-selected');
				$el.find('i').removeClass('icon-ok').addClass('tree-dot');
			} else {
				$el.addClass ('tree-selected');
				$el.find('i').removeClass('tree-dot').addClass('icon-ok');
				if (this.options.multiSelect) {
					data.push( $el.data() );
				}
			}

			if(data.length) {
				this.$element.trigger('selected', {info: data});
			}

			// Return new list of selected items, the item
			// clicked, and the type of event:
			$el.trigger('updated', {
				info: data,
				item: $el,
				eventType: eventType
			});
		},

		selectFolder: function (el) {
			var $el = $(el);
			var $parent = $el.parent();
			var $treeFolderContent = $parent.find('.tree-folder-content');
			var $treeFolderContentFirstChild = $treeFolderContent.eq(0);

			var eventType, classToTarget, classToAdd;
			if ($el.find('.icon-folder-close').length || !$el.hasClass('folder-refreshed')) {
				eventType = 'opened';
				classToTarget = '.icon-folder-close';
				classToAdd = 'icon-folder-open';

				$treeFolderContentFirstChild.show();
				if (!$treeFolderContent.children().length || !$el.hasClass('folder-refreshed')) {
					this.populate($el);
				} 
				
    			$parent.find(classToTarget).eq(0)
    				.removeClass('icon-folder-close icon-folder-open')
    				.addClass(classToAdd);
				
			} else {
			    var $folderContentList = this._getFolderContent().find('.content-list');
			    $folderContentList.children().detach();
			    
			    $folderContentList.append($el.data().children);
			    
			    // remove any previous selected item.
			    $folderContentList.find('.tree-item.tree-selected').click();
			    
			}
			
            this.$element.find('.tree-folder-selected').removeClass('tree-folder-selected');
            $el.addClass('tree-folder-selected');
            
				

			//this.$element.trigger(eventType, $el.data());
		},

		selectedItems: function () {
			var $sel = this.$element.find('.tree-selected');
			var data = [];

			$.each($sel, function (index, value) {
				data.push($(value).data());
			});
			return data;
		},

		// collapses open folders
		collapse: function () {
			var cacheItems = this.options.cacheItems;

			// find open folders
			this.$element.find('.icon-folder-open').each(function () {
				// update icon class
				var $this = $(this)
					.removeClass('icon-folder-close icon-folder-open')
					.addClass('icon-folder-close');

				// "close" or empty folder contents
				var $parent = $this.parent().parent();
				var $folder = $parent.children('.tree-folder-content');

				$folder.hide();
				if (!cacheItems) {
					$folder.empty();
				}
			});
		}
	};


	// TREE PLUGIN DEFINITION

	$.fn.tree = function (option, value) {
		var methodReturn;

		var $set = this.each(function () {
			var $this = $(this);
			var data = $this.data('tree');
			var options = typeof option === 'object' && option;

			if (!data) $this.data('tree', (data = new Tree(this, options)));
			if (typeof option === 'string') methodReturn = data[option](value);
		});

		return (methodReturn === undefined) ? $set : methodReturn;
	};

	$.fn.tree.defaults = {
		multiSelect: false,
		loadingHTML: '<div>Loading...</div>',
		cacheItems: true,
		showFolderOnly: false,
		folderContent: '.foldercontent'
	};

	$.fn.tree.Constructor = Tree;

})(jQuery);
