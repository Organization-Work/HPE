/* ============================================================
 * jQuery.toggler.js v0.0.1
 * ============================================================
 * @Author: Liam Goodacre
 * ============================================================ */
(function($) {

	var DATA = "toggler", ACTIVE = "active", INACTIVE = "inactive", TOGGLER_ACTIVE = "toggler-active", TOGGLER_ACTIVE_CLASS = "."
			+ TOGGLER_ACTIVE;

	var setup = function(elems, options) {
		$(elems).each(function() {
			var $this = $(this);
			$this.data(DATA, options);
			execute($this, $this.is(TOGGLER_ACTIVE_CLASS) ? "on" : "off");
		});
	};

	var commands = {
		toggle : function($elem) {
			return $elem.each(function() {
				var $this = $(this);
				var opts = $this.data(DATA);
				commands[$this.is(TOGGLER_ACTIVE_CLASS) ? "off" : "on"]($this);
			});
		},
		on : function($elems) {
			return $elems.each(function() {
				var $elem = $(this);
				process($elem, INACTIVE, ACTIVE);
				$elem.addClass(TOGGLER_ACTIVE);
			});
		},
		off : function($elems) {
			return $elems.each(function() {
				var $elem = $(this);
				process($elem, ACTIVE, INACTIVE);
				$elem.removeClass(TOGGLER_ACTIVE);
			});
		},
		status : function($elem) {
			return $elem.hasClass(TOGGLER_ACTIVE);
		},
		statuses : function($elems) {
			var result = {};
			$elems.each(function(index, elem) {
				result[index] = commands.status($(elem));
			});
			return result;
		}
	};

	var apply = function($elem, rules, disable, enable) {
		$elem.removeClass(rules[disable] || "").addClass(rules[enable] || "");
	};

	var process = function($elem, disable, enable) {
		$elem.each(function() {
			var $this = $(this);
			$.each($this.data(DATA) || {}, function(sel, rules) {
				var subelem = undefined;
				if (sel === 'this') {
					subelem = $this;
				} else if (sel.substring(0, 4) === 'this') {
					subelem = $this.filter(sel.substring(4));
				} else if (sel.substring(0, 1) === '>' || sel.substring(0, 1) === '&') {
					subelem = $this.find(sel.substring(1));
				} else {
					var parentMatch = /^\^+/.exec(sel);
					var len;
					if (parentMatch) {
						len = parentMatch[0].length;
						subelem = $($this.parents().get(len - 1)).find(sel.substring(len));
					}
				}
				apply(subelem || $(sel), rules, disable, enable);
			});
		});
	};

	var execute = function($elems, command) {
		return commands[command]($elems);
	};

	$.fn.toggler = function(_args, onClick) {
		if ($.isPlainObject(_args)) {
			setup(this, _args);
			if (onClick) {
				this.click(function() {
					$(this).toggler();
				});
			}
			return this;
		}
		return execute(this, _args || "toggle");
	};

})(window.jQuery);
