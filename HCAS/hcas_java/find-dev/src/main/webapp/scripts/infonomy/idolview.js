jQuery(function ($) {
	var BUTTONS = false;
	$('.content').infonomy({
		'.idolview': {
			'title': 'Sunburst',
			'html': '[DESCRIPTION...]',
			'button': BUTTONS,
			'buttonMargin': '100px auto 0',
			'width': '50%',
			'height': '50%',
			'popupMargin': 'auto'
		}
	});
});
