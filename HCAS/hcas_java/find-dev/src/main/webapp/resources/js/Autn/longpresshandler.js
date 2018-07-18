define([], function() {
    function LongPressHandler(cfg) {
        var duration = (cfg || {}).duration || 500, timeout, fired;

        var clickFn, longPressFn;

        this.mousedown = function(newClickFn, newLongPressFn) {
            clickFn = newClickFn;
            longPressFn = newLongPressFn;
            timeout && clearTimeout(timeout);
            fired = false;
            timeout = setTimeout(longPress, duration);
        };

        this.mouseup = function() {
            if (!fired) {
                fired = true;
                timeout && clearTimeout(timeout);
                clickFn();
                clickFn = longPressFn = null;
            }
        };

        this.cancel = function() {
            fired = true;
            timeout && clearTimeout(timeout);
            timeout = clickFn = longPressFn = null;
        };

        this.longPress = longPress;

        function longPress() {
            fired = true;
            timeout && clearTimeout(timeout);
            longPressFn();
            timeout = clickFn = longPressFn = null;
        }
    }

    return LongPressHandler;
});