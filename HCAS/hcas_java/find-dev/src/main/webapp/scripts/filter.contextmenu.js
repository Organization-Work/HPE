var $filtersContextMenu = $('#filtersContextMenu');
var $pasteMenuItem = $('#filtersPaste');
var $pasteGrpMenuItem = $('#filtersGroupPaste');

var FilterContextmenu = {
	COPY: "copy",
	CUT: "cut",
	PASTE: "paste",
	PASTE_AS_GROUP: "pasteAsGroup",
	
	initMenu: function() {
		
		$filtersContextMenu.on('click', '.context-menu-item', function(evt) {
			evt.preventDefault();

			var $contextSource = $filtersContextMenu.data('contextSource');
			
			if ($(this).hasClass('disabled') || !$contextSource) {
				return false;
			}
						
			var action = $(this).data('action');
			switch (action) {
				case 'copy':
					$contextSource.handleCopy();
					break;
				case 'cut':
					$contextSource.handleCut();
					break;
				case 'paste':
					$contextSource.handlePaste();
					break;
				case 'grouppaste':
					$contextSource.handleGroupPaste();
					break;
			}
			
			return true;
		});
		
    $('body').click(function() {
    	$filtersContextMenu.hide();
    	$filtersContextMenu.data('contextSource', null);
      $('.in-filter-contextmenu').removeClass('in-filter-contextmenu');
    });
            
		
	},
	
	showMenu: function($contextSource, posX, posY) {
		$filtersContextMenu.data('contextSource', $contextSource).css({
  		display: "block",
  		left: posX,
  		top: posY,
  		zIndex: 1060
		});
		
		var isFilterGroup = $contextSource.element.hasClass('filter-group');

		$('.in-filter-contextmenu').removeClass('in-filter-contextmenu');
		if (!isFilterGroup) {
			$contextSource.element.addClass('in-filter-contextmenu');
		} 
		
		$filtersContextMenu.find('.copy-cut').toggleClass('hide', isFilterGroup);
		
		var clipboard = this.getClipboard();
		var isCopy = clipboard && clipboard.action === this.COPY;

		var disablePaste = !clipboard || ($contextSource.getWhengrp() && (isCopy || clipboard.whengrp !== $contextSource.getWhengrp()));
		var disablePasteGroup = disablePaste || $contextSource.getWhengrp();
		
		$pasteMenuItem.toggleClass('disabled', Boolean(disablePaste));
		$pasteGrpMenuItem.toggleClass('disabled', Boolean(disablePasteGroup));
				
	},

		
	getClipboard: function() {
		return $filtersContextMenu.data('clipboard');
	},
	
	setClipboard: function(action, items, whengrp) {
  	var clipboard = this._createClipboard(action, items, whengrp);
  	$filtersContextMenu.data('clipboard', clipboard);
		
	},
	
	clearClipboard: function() {
		$filtersContextMenu.data('clipboard', null);
	},
	
	_createClipboard: function(action, $items, whengrp) {
		return {
			action: action,
			items: $items,
			whengrp: whengrp
		};
	}
		
};

