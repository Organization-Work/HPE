jQuery(function ($) {
	var BUTTONS = false;
	$('.content').infonomy({
		'#paper': {
			'title': 'Theme Tracker',
			'html': '[DESCRIPTION...]',
			'button': BUTTONS,
			'buttonMargin': '100px auto 0',
			'width': '50%',
			'height': '200px',
			'popupMargin': '100px auto'
		}
	});
});
