// raphael.js doesn't import correctly using require.js, see https://github.com/DmitryBaranovskiy/raphael/issues/524
define(['Autn/tooltip', 'underscore', 'Autn/i18n', 'Autn/longpresshandler', 'jquery', 'jqueryui', 'jquerytouchpunch', 'jqueryhashchange', 'json2', 'd3'], function(Tooltip, _, i18n, LongPressHandler){

return function(chartEl, opts){
    // Chrome is picky and thinks it doesn't have full SVG support, so testing for
    //    document.implementation.hasFeature("http://www.w3.org/TR/SVG11/feature#Shape", "1.0")
    // doesn't work.
    // http://stackoverflow.com/questions/654112/how-do-you-detect-support-for-vml-or-svg-in-a-browser
    if (!document.implementation.hasFeature("http://www.w3.org/TR/SVG11/feature#BasicStructure", "1.1")) {
        chartEl.text(i18n('idolview.svg.browser.required'));
        return;
    }

    opts = opts || {};

    var arcLabelFormatter = opts.arcLabelFormatter;

    var width = chartEl.width(),
            height = chartEl.height(),
            radius = Math.min(width, height) / 2,
            minRadius = 70;

    // Lock the width and height after first render
    chartEl.width(width);
    chartEl.height(height);

    var x = d3.scale.linear()
            .range([0, 2 * Math.PI]);

    var y = opts.radiusScaleFn && opts.radiusScaleFn(radius)
            || d3.scale.sqrt().range([0, radius]);

    var color = d3.scale.category20c();

    var vis = d3.select(chartEl[0]).append('svg')
            .attr('width', width)
            .attr('height', height)
            .append('g')
            .attr('transform', 'translate(' + width / 2 + ',' + height / 2 + ')');

    var animationTime = opts.animationTime || 1000;
    var textAppearanceTime = animationTime + 500;

    var onShortClick = opts.onShortClick;
    var onLongClick = opts.onLongClick;
    var onHover = opts.onHover;

    var colorFn = opts.colorFn || function (d) { return color((d.children ? d : d.parent).name); };

    var longPressHandler = new LongPressHandler();

    var infoTooltip = new Tooltip({
        parent: chartEl,
        pos: { my: 'top', at: 'center', of: chartEl, offset: '0 ' + (minRadius + 20) }
    });

    var partition = d3.layout.partition()
            .value(function(d) { return d.size; })
            .innerValue(function(d) {
                var innerSize = d.size;
                var children = d.children;
                if (children) {
                    for (var ii = 0, max = children.length; ii < max; ++ii) {
                        innerSize -= children[ii].size || 0;
                    }
                }

                // need the negative check to prevent overflow due to single documents having multiple fields
                // e.g. if there's 5 PERSON fields in a doc with one PLACE field, expanding PLACE before PERSON means you'd have
                // more docs on the outer-level than the inner-level.
                // this also means the inner section is too large though?
                return innerSize < 0 ? 0 : innerSize;
            });

    var arc = d3.svg.arc()
            .startAngle(function(d) { return Math.max(0, Math.min(2 * Math.PI, x(d.x))); })
            .endAngle(function(d) { return Math.max(0, Math.min(2 * Math.PI, x(d.x + d.dx))); })
            .innerRadius(function(d) { return Math.max(0, y(d.y)); })
            .outerRadius(function(d) { return Math.max(0, y(d.y + d.dy)); });

    var arcLabelTimeout;
    var prevClicked, prevHovered;

    this.resetFocus = function(){
        prevClicked = prevHovered = null;

    };

    this.redraw = redraw;

    function redraw(json, retainZoom) {
        infoTooltip.hide();
        longPressHandler.cancel();

        var visEl = vis.data([json]);
        var updateEls = visEl.selectAll('path')
                .data(partition.nodes, arcKey);
        updateEls.exit().remove();

        if (!retainZoom) {
            x.domain([0,1]);
            y.domain([0,1]).range([0, radius]);
        } else if (prevClicked) {
            // should zoom onto the current el
            x.domain([prevClicked.x, prevClicked.x + prevClicked.dx]);
            y.domain([prevClicked.y, 1]).range([prevClicked.y ? minRadius : 0, radius]);
        }

        var animate = updateEls.length < 200, path;

        // on the existing elements

        if (animate) {
            updateEls.transition().duration(animationTime).attr('d', function(d,i){
                return arc(d);
            }).style('fill', colorFn);

            // animating opacity is too slow to be practical
            path = updateEls
                    // on new elements
                    .enter().insert('path', 'path').classed('circle-node', true).attr('d', function(d,i){
                return arc(d);
            }).style('fill', 'white').transition().duration(animationTime).style('fill', colorFn);

        }
        else {
            updateEls.attr('d', function(d,i){
                return arc(d);
            }).style('fill', colorFn);

            path = updateEls
                    // on new elements
                    .enter().insert('path', 'path').classed('circle-node', true).attr('d', function(d,i){
                return arc(d);
            }).style('fill', colorFn);
        }

        hideArcLabels();
        scheduleArcLabels(1, textAppearanceTime);

        var lastTouchEnd;

        // on all elements
        updateEls.on('mousedown', mousedown);
        updateEls.on('mouseup', longPressHandler.mouseup);
        updateEls.on('mouseover', hover);
        updateEls.on('mouseout', mouseout);
        updateEls.on('touchstart', function(d){
            if (d3.event.touches.length > 1) {
                return;
            }

            hover(d);
            mousedown(d);
        });

        updateEls.on('touchmove', function(d){
            var evt = d3.event;
            if (evt.touches.length > 1) {
                return;
            }

            // event.target on touch events refers to the original dom element which the touch started on,
            // so we have to look up the object from the mouse event
            var dragEl = document.elementFromPoint(evt.touches[0].clientX, evt.touches[0].clientY);

            if (!dragEl || dragEl.tagName !== 'path') {
                return;
            }

            var hoveredData = dragEl.__data__;

            if (!hoveredData) {
                return;
            }

            // If unzoomed, you can only scroll on the 'Total' count
            // If zoomed, you can only scroll on the zoomed segment or its parent
            if (prevClicked ? hoveredData !== prevClicked && hoveredData !== prevClicked.parent : hoveredData.parent != null) {
                evt.preventDefault();
            }

            longPressHandler.cancel();
            hover(hoveredData);
        });
        updateEls.on('touchcancel', longPressHandler.cancel);

        updateEls.on('touchend', function(d){
            if (d3.event.touches.length > 1) {
                return;
            }

            lastTouchEnd = Date.now();

            d3.event.preventDefault();
            longPressHandler.mouseup();
        });

        function hideArcLabels() {
            if (arcLabelTimeout) {
                clearTimeout(arcLabelTimeout);
                arcLabelTimeout = null;
            }

            var arcLabels = visEl.selectAll('text.labels').data([]);
            arcLabels.exit().remove();
        }

        function scheduleArcLabels(minDepth, delay) {
            if (arcLabelTimeout) {
                clearTimeout(arcLabelTimeout);
            }

            arcLabelTimeout = setTimeout(function(){
                drawArcLabels(minDepth);
            }, delay);
        }

        function drawArcLabels(minDepth) {
            if (!arcLabelFormatter) {
                return;
            }

            if (arcLabelTimeout) {
                clearTimeout(arcLabelTimeout);
                arcLabelTimeout = null;
            }

            var TWO_PI = 2 * Math.PI;
            var arcLabels = visEl.selectAll('text.labels').data(function(node){
                return partition.nodes(node).filter(function(d){
                    if (d.depth < minDepth) {
                        return false;
                    }

                    var angle1 = x(d.x);
                    var angle2 = x(d.x + d.dx);
                    return angle1 >= 0 && angle1 <= TWO_PI && angle2 >= 0 && angle2 <= TWO_PI && (angle2 - angle1) > 0.1;
                });
            }, arcKey);
            arcLabels.enter().append('svg:text').classed('labels', true);
            arcLabels.exit().remove();
            arcLabels.attr('transform', function(d) {
                var rawAngle = x(d.x + 0.5 * d.dx);
                var radAngle = rawAngle < Math.PI ? rawAngle - 0.5 * Math.PI : rawAngle + 0.5 * Math.PI;
                var angle = radAngle / Math.PI * 180;
                return 'translate(' + arc.centroid(d) + ') rotate('+angle+')';
            }).text(function(d){
                return arcLabelFormatter(d);
            });
        }

        function mousedown(d) {
            if (lastTouchEnd && (Date.now() - lastTouchEnd < 100)) {
                // iPhone tends to fire a mousedown after the touchend event, which we don't want
                return;
            }

            longPressHandler.mousedown(function(){
                infoTooltip.hide();

                if (d === prevClicked) {
                    return;
                }

                prevClicked = d;

                onShortClick && onShortClick(d);
                
                /* Only has one level, so no need to drill down.
                hideArcLabels();

                updateEls.transition().duration(animationTime).attrTween('d', arcTween(d));

                scheduleArcLabels(d.depth + 1, textAppearanceTime);

                if (!d.children || !d.children.length) {
                    var html = i18n('idolview.filter.leaf.options.html');
                    infoTooltip.schedule(html, textAppearanceTime + 1000);
                }
                */
            }, function() {
                infoTooltip.hide();
                onLongClick && onLongClick(d);
            });

            if (d3.event.ctrlKey) {
                longPressHandler.longPress();
            }
        }

        hideCenterLabel();

        function hover(d) {
            if (prevHovered === d) {
                return;
            }

            prevHovered = d;

            // todo: better rendering respecting bounds of circles
            showCenterLabel(d);

            onHover && onHover(d);
        }

        function mouseout() {
            prevHovered = null;
            longPressHandler.cancel();
            onHover && onHover(null);
        }
    }

    var centerLabel;

    function showCenterLabel(d) {
        var innerHTML = (d.filter && ('<div class="idolview-tooltip-filter">[' + _.escape(i18n(d.filter).toUpperCase()) + ']</div>') || '')
            + '<div class="idolview-tooltip-name">'+_.escape(d.name)+'</div>';

        if (d.size) {
            innerHTML += '<div class="idolview-tooltip-size">'+i18n('idolview.center.label.numdocs', {DOCS: d.size})+'</div>';
        }

        if (!centerLabel) {
            centerLabel = $('<div class="idolview-tooltip">'+innerHTML+'</div>').appendTo(chartEl);
        }
        else {
            centerLabel.html(innerHTML);
        }

        for (var root = d; root.parent; root = root.parent) {}

        var maxWidth = 2 * y(root.y + root.dy);

        centerLabel.css('maxWidth', maxWidth).position({ my: 'center', at: 'center', of: chartEl });
    }

    function hideCenterLabel() {
        if (centerLabel) {
            centerLabel.remove();
            centerLabel = null;
        }
    }

    function arcKey(d, i) {
        var key = '';
        for (var current = d; current && current.filter; current = current.parent) {
            key += '\u2192' + current.filter + ':' + current.name;
        }

        return key;
    }

    function arcTween(d) {
        var xd = d3.interpolate(x.domain(), [d.x, d.x + d.dx]),
                yd = d3.interpolate(y.domain(), [d.y, 1]),
                yr = d3.interpolate(y.range(), [d.y ? minRadius : 0, radius]);
        var oldT = 0;
        // The problem with the original sunburst code is it assumes that the 0-th element would always run first and set the domain and
        // range of the interpolation functions; which saves having to set them before computing the arcs of the other arcs.
        // However, there's no guarantee that the tween functions execute in order, so if the other arcs execute first, their paths are calculated
        // using the domain and range of the old 't' value. This is visibly noticable when zooming in onto a small leaf, e.g. one two levels deep
        // and taking < 1 degree. When fully expanded computed using the old less-than-one 't' value, it doesn't fill the entire circle.
        // Solution is to remember the old 't' value and check if it's changed on every computation, slightly slower than the original, but
        // still much faster than recalculating the domains on every arc.
        return function(d, i) {
            return function(t) {
                if (oldT !== t) {
                    oldT = t;
                    x.domain(xd(t)); y.domain(yd(t)).range(yr(t));
                }
                return arc(d);
            }
        };
    }
};
});
