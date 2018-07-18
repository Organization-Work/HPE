/* Using the window url Hash for saving/loading search query */
jQuery(function($) {
	var $search = $('.simpleHash');
	var $searchText = $search.find('.simpleHashText');
	var hash = Hash.observe(function() {
		$searchText.val(hash.getCurrentValue().query || '*');
		$search.trigger('submit');
	});
	$search.submit(function() {
		hash.change({
			query : $searchText.val()
		});
	});
});