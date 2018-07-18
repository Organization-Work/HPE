define(['jquery', 'jqueryui', 'underscore'], function($){
    function Tooltip(cfg) {
        var pos = cfg.pos, parent = cfg.parent || document.body;

        var el, timeout;


        this.hide = function() {
            if (timeout) {
                clearTimeout(timeout);
                timeout = null;
            }

            el && el.remove();
        };

        this.show = function(html) {
            this.hide();

            el = $('<div class="idolview-tooltip">'+html+'</div>').appendTo(parent).position(pos);
            return el;
        };

        this.schedule = function(html, time) {
            this.hide();

            timeout = setTimeout(_.bind(this.show, this, html), time || 1000);
        };
    }

    return Tooltip;
});
