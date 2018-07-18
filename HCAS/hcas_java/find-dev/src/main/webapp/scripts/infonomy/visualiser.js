jQuery(function ($) {
	var BUTTONS = false;
	$('.content').infonomy({
		'.visualiser': {
			'title': 'Visualiser',
			'html': '[DESCRIPTION...]',
			'button': BUTTONS,
			'buttonMargin': '100px auto 0',
			'height': '200px',
			'popupMargin': '10px 10px'
		}
	});
});
