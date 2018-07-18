var Hash = {};
jQuery(function($) {
	var api = Hash;
	var observers = [];
	var previousHash = {};
	var timeoutId = null;

	var HashObserver = function HashObserver(callback) {
		this.callback = callback;
	};

	HashObserver.prototype = {
		onchange : function(value) {
			return this.callback(value);
		},
		change : function(value) {
			return api.change(value, this);
		},
		changeAfter : function(value, delay) {
			return api.changeAfter(value, this, delay);
		},
		cancel : function() {
			return api.remove(this);
		},
		getCurrentValue : function() {
			return api.getCurrentValue();
		}
	};

	api.observe = function(callback) {
		var observer = new HashObserver(callback);
		observers.push(observer);
		return observer;
	};

	api.remove = function(observer) {
		// Locate the observer
		var index = observers.indexOf(observer);
		if (index > -1) {
			// Remove it from the list
			observers.splice(index, 1);
		}
		return api;
	};

	var cleanHash = function(obj) {
		for ( var key in obj) {
			if (obj.hasOwnProperty(key) && obj[key] === undefined) {
				delete obj[key];
			}
		}
	};

	api.change = function(value, sender) {
		if (_.isEqual(value, previousHash)) {
			return;
		}
		// Cancel any scheduled updates
		if (timeoutId) {
			clearTimeout(timeoutId);
			timeoutId = null;
		}
		// Update the hash object
		
		if (_.isEqual(value, {})) {
			previousHash = {};
		} else {
            previousHash = _.clone(value);
			cleanHash(previousHash);
		}
		// Update the window hash
		if (sender !== window) {
			window.location.hash = $.toJSON(previousHash);
        }
		$(observers).each(function(index, observer) {
			if (observer && observer !== sender) {
				observer.onchange(_.clone(previousHash));
			}
		});
		return api;
	};

	api.changeAfter = function(value, sender, delay) {
		if (_.isEqual(value, previousHash)) {
			return;
		}
		// Cancel any previous updates
		if (timeoutId) {
			clearTimeout(timeoutId);
			timeoutId = null;
		}
		// Schedule a new update
		timeoutId = setTimeout(function() {
			api.change(value, sender);
		}, delay);
		return api;
	};

	api.getCurrentValue = function() {
		return $.extend({}, previousHash);
	};

	var getWindowHash = function() {
		return $.parseJSON(decodeURI(window.location.hash.substring(1)) || '{}');
	};

	var windowHashChange = function() {
		var value = getWindowHash();
		if (_.isEqual(value, previousHash)) {
			return;
		}
		api.change(value, window);
	};

	$(window).on('hashchange', windowHashChange);
	_.delay(function () {
        var value = getWindowHash();
        window.hack = getWindowHash;

        if (_.size(value)) {
            $(window).trigger('hashchange');
        }
    }, 1);
});