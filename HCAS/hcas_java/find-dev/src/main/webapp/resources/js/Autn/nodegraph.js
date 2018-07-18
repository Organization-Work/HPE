if (typeof Autn === 'undefined') {
    Autn = {};
}

Autn.nodegraph = function(opts){
    var $ = jQuery;

    if (!opts) {
        opts = {};
    }

    var showNodeDocs = opts.showNodeDocs || function (node){
        Autn.Reader.query([node.name], {
            url: 'query.json',
            sentiment: sentiment,
            highlightColor: sentiment ? undefined : node.color,
            baseParams: {
                minDate: minDate,
                maxDate: maxDate
            }
        });
    };

    var searchCallback = opts.searchCallback || function (onSuccess, query, minDate) {
        return $.ajax('nodegraph.json', {
            success: onSuccess,
            data: {
                aqgMaxResults: aqgMaxResults,
                childLimit: childLimit,
                clusterDedupe: clusterDedupe,
                clusterSentiment: sentiment,
                linkMode: linkMode,
                minDate: minDate,
                maxDate: maxDate,
                query: query,
                termMaxResults: termMaxResults
            }
        });
    };

    $('#load-indicator').hide().ajaxStart(function(){
        $(this).show();
    }).ajaxStop(function(){
        $(this).hide();
    });

    var dom = $('#paper');
    var width = Math.max(1000, dom.width() - 350), height = dom.height();
    dom.width(width);
    dom.height(height);

    if (!document.implementation.hasFeature("http://www.w3.org/TR/SVG11/feature#BasicStructure", "1.1")) {
        $('<div></div>').css({position: 'absolute', 'font-size': 12}).text('Please use a browser with SVG support, e.g. IE9, Firefox or Chrome').appendTo(dom).position({
            at: 'center',
            my: 'center',
            of: dom
        });

        return;
    }

    var color = d3.scale.category20();

    var force = d3.layout.force()
            .charge(function(d) { return d.size * -5.5; })
            .linkDistance(function(link, idx) { return (link.source.size + link.target.size) * 2; })
            .linkStrength(0.5)
            .gravity(0.1)
//            .theta(0.8)
            .size([width, height]);

    var svg = d3.select(dom[0]).append('svg')
            .attr('width', width)
            .attr('height', height);

    var viewX = 0.5 * width, viewY = 0.5*height, viewW = width, viewH = height, viewScale = 1, viewScaleFactor = 1, dragMode;

    // Canvas drag+pan code
    dom.mousedown(function(evt){
        dragMode = true;
        var startViewX = viewX, startViewY = viewY, dragPos = [evt.pageX, evt.pageY];
        evt.preventDefault();

        $(document).bind('mouseup', onMouseUp).bind('mousemove', onMouseMove);

        function onMouseMove(evt){
            viewX = startViewX + (dragPos[0] - evt.pageX) * viewScaleFactor,
                    viewY = startViewY + (dragPos[1] - evt.pageY) * viewScaleFactor;
            svg.attr('viewBox', [viewX - .5*viewW,  viewY - .5*viewH, viewW, viewH].join(' '));
        }

        function onMouseUp() {
            dragMode = false;
            $(document).unbind('mouseup', onMouseUp).unbind('mousemove', onMouseMove);
        }
    });

    // Canvas scale code
    dom.mousewheel(function(evt, delta, deltaX, deltaY){
        viewScale += deltaY > 0 ? -1 : 1;
        viewScaleFactor = Math.pow(1.1, viewScale);
        viewW = width * viewScaleFactor;
        viewH = height * viewScaleFactor;
        svg.attr('viewBox', [viewX - .5*viewW,  viewY - .5*viewH, viewW, viewH].join(' '));

        return false;
    });

    var queryParam = /[?&]query=([^&]+)(&|$)/.exec(window.location.search),
        query = queryParam ? decodeURIComponent(queryParam[1]) : '',
        mintermParam = /[?&](?:minlinks|minterm)=(\d+)(&|$)/i.exec(window.location.search),
        sentiment = /[?&]sentiment=true(&|$)/i.test(window.location.search),
        linkModeParam = /[?&]linkMode=(querysummary|references|terms)(&|$)/i.exec(window.location.search),
        linkMode = linkModeParam ? linkModeParam[1].toLowerCase() : 'querysummary',
        minLinks = opts.minLinks != null ? opts.minLinks : mintermParam ? Number(mintermParam[1]) || 0 : 6,
        aqgMaxResultsParam = /[?&]aqgMaxResults=(\d+)(&|$)/i.exec(window.location.search),
        aqgMaxResults = aqgMaxResultsParam ? Number(aqgMaxResultsParam[1]) : (!query || /^\s*\*+\s*$/.test(query)) ? 10000 : 1000,
        childLimitParam = /[?&]childLimit=(\d+)(&|$)/i.exec(window.location.search),
        childLimit = childLimitParam ? Number(childLimitParam[1]) : 300,
        clusterDedupe = /[?&]clusterDedupe=true(&|$)/i.test(window.location.search),
        termMaxResultsParam = /[?&]termMaxResults=(\d+)(&|$)/i.exec(window.location.search),
        termMaxResults = termMaxResultsParam ? Number(termMaxResultsParam[1]) : 200,
        nodeMinRadiusParam = /[?&]nodeMinRadius=(\d+(\.\d*)?)(&|$)/i.exec(window.location.search),
        nodeMinRadius = nodeMinRadiusParam ? Number(nodeMinRadiusParam[1]) : 10,
        nodeMaxRadiusParam = /[?&]nodeMaxRadius=(\d+(\.\d*)?)(&|$)/i.exec(window.location.search),
        nodeMaxRadius = Math.max(nodeMaxRadiusParam ? Number(nodeMaxRadiusParam[1]) : 40, nodeMinRadius),
        minDate, maxDate;

    var delayParam = /[?&]delay=(\d+)(&|$)/i.exec(window.location.search);
    var delay = delayParam ? Number(delayParam[1]) : 300;

    var lastAjax = searchCallback(renderGraph, query, minDate);

    function doSearch() {
        lastAjax && lastAjax.abort();

        var query = $('input[name=query]', controls).val() || '';
        minDate = undefined;
        var mindateDays = $('select[name=mindate]').val();
        if (mindateDays) {
            var now = new Date();
            now.setDate(now.getDate() - mindateDays);
            minDate = Math.floor(now.setUTCHours(0,0,0,0) / 1000);
        }

        lastAjax = searchCallback(renderGraph, query, minDate);
        return false;
    }

    var controls = $('#controls').submit(doSearch);
    query && $('input[name=query]', controls).val(query);

    function resize(){
        this.size = Math.max(this.value.length, minQueryFieldLength);
    }

    var queryField = $('input[name=query]');
    var minQueryFieldLength = queryField.attr('size');
    queryField.on('keyup keydown blur update', resize).each(resize);

    var legend, noResults;

    function renderGraph(json) {
        legend && legend.remove();
        noResults && noResults.remove();
        legend = $('<div id="legend"><ul></ul></div>');
        svg.selectAll('text,circle.node,line.link').remove();

        if (!json.nodes || !json.nodes.length) {
            // nothing to draw
            noResults = $('<div>No results available, please try a different query</div>').css({position: 'absolute', opacity: 0}).appendTo(dom).position({
                at: 'center',
                my: 'center',
                of: dom
            }).animate({opacity: 1});

            return;
        }

        // link culling if the concepts share too few shared terms/references/docids
        json.links = json.links.filter(function(link){ return link.shared >= minLinks; });

        var clusterNodeMap = {};

        var sizes = json.nodes.map(function(a){return a.size;});
        var minSize = json.nodes[json.nodes.length - 1].size;
        var maxSize = json.nodes[0].size;
        var sizeScale = maxSize === minSize ? 0 : (nodeMaxRadius - nodeMinRadius) / (maxSize - minSize);
        json.nodes.forEach(function(node, idx){
            node.size = nodeMinRadius + (node.size - minSize) * sizeScale;

            node.label = String(1 + idx);

            node.color = json.sentiment ? Raphael.hsb(node.sentiment * 0.3333, 0.8, 0.9) : color(node.clusterId);
            node.textColor = d3.hsl(node.color).l < 0.6 ? '#fff' : '#000';

            $('<li></li>').text(node.label + ': ' + node.name).data('node', node).appendTo(legend.children('ul'))
                .prepend('<span class="legendpad" style="background-color: '+node.color+';">&nbsp;</span>')
                .append('<div class="metatext"></div>');

            var clusterNodes = clusterNodeMap[node.clusterId];
            if (clusterNodes) {
                clusterNodes.push(node);
            }
            else {
                clusterNodeMap[node.clusterId] = [node];
            }
        });

        legend.appendTo(document.body).on('mouseover', 'li', function(){
            hoveredNode = $(this).data('node');
            updateOpacity(false);
        }).on('mouseout', 'li', function(){
            scheduleResetOpacity();
        }).on('click', 'li', function(){
            showNodeDocs($(this).data('node'));
            return false;
        });

        var metaFn = opts.metaFn;

        var highlightOnHover = {};
        json.links.forEach(function(link){
            highlightOnHover[link.source + ',' + link.target] = highlightOnHover[link.target + ',' + link.source] = 1;
        });

        force.nodes(json.nodes)
             .links(json.links)
             .start();

        var link = svg.selectAll('line.link')
                .data(json.links)
                .enter().append('line')
                .attr('class', 'link');

        var node = svg.selectAll('circle.node')
                        .data(json.nodes)
                        .enter().append('circle')
                        .attr('class', 'node')
                        .attr('r', function(d) { return d.size; })
                        .style('fill', function(d) { return d.color; })
                        .call(force.drag)
                        .on('mouseover', function(d) {
                            hoveredNode = d;
                            updateOpacity(true);
                        })
                        .on('mouseout', function(d) {
                            scheduleResetOpacity();
                        })
                        .on('click', showNodeDocs);

        var labels = svg.selectAll('text')
                        .data(json.nodes)
                        .enter().append('text')
                        .attr('class', 'textlabel')
                        .text(function(d){ return d.label; });

        var hoveredNode;

        var scheduledResetOpacity;

        function scheduleResetOpacity() {
            scheduledResetOpacity && clearTimeout(scheduledResetOpacity);
            scheduledResetOpacity = setTimeout(function(){
                hoveredNode = null;
                updateOpacity(true);
            }, 500);
        }

        function updateOpacity(canChangeText) {
            scheduledResetOpacity && clearTimeout(scheduledResetOpacity);
            scheduledResetOpacity = null;

            // create a local reference for animation closure scope
            var baseHeight = 23;
            var expandedHeight = metaFn ? 40 : baseHeight;

            var hovered = hoveredNode;
            if (hovered) {
                // linksToNode
                node.transition(delay).attr('opacity', function(d){
                    return hovered === d ? 1 : highlightOnHover[hovered.index + ',' + d.index] ? 0.8
                        : hovered.clusterId === d.clusterId  ? 0.6 : 0.08;
                });

                link.transition(delay).attr('opacity', function(d){
                    return hovered === d.target || hovered === d.source ? 1 : 0.5;
                });

                var toFocus, hoveredOffset = 0;
                legend.find('li').each(function(idx, val){
                    var el = $(this);
                    var d = el.data('node');
                    var isCentered = hovered === d;
                    if (isCentered) {
                        toFocus = el;
                        el.css('background-color', d.color).stop().css('color', d.textColor);
                        metaFn && el.find('.metatext').text(metaFn(hovered));
                        return;
                    }
                    var linked = highlightOnHover[hovered.index + ',' + d.index];
                    var props = {'color': linked ? '#000' : '#C0C0C0'};
                    if (canChangeText) {
                        var height = props.height = linked ? expandedHeight : 0;
                        toFocus || (hoveredOffset += height);
                    }
                    el.css('background-color', '').stop().animate(props, delay);
                    metaFn && linked && el.find('.metatext').text(metaFn(hovered, d));
                });

                if (canChangeText) {
                    toFocus.css('height', expandedHeight);
                    legend.stop().animate({scrollTop: hoveredOffset}, delay);
                }
            }
            else {
                node.transition(delay).attr('opacity', 1);
                link.transition(delay).attr('opacity', 1);
                legend.find('li').css('background-color', '').css('color', '').stop().animate({ height: baseHeight }, delay);
            }
        }

        node.append('title')
            .text(function(d) { return d.name; });

        force.on('tick', function() {
            var nodes = json.nodes;
            var q = d3.geom.quadtree(nodes),
                    i = 0,
                    n = nodes.length;

            while (++i < n) {
                q.visit(collide(nodes[i]));
            }

            link.attr('x1', function(d) { return d.source.x; })
                .attr('y1', function(d) { return d.source.y; })
                .attr('x2', function(d) { return d.target.x; })
                .attr('y2', function(d) { return d.target.y; });

            node.attr('cx', function(d) { return d.x; })
                .attr('cy', function(d) { return d.y; });

            labels.attr('x', function(d) { return d.x; })
                // need to account for font-size
                  .attr('y', function(d) { return d.y + 4; });
        });
    }

    function collide(node) {
        var r = node.size * 2,
                nx1 = node.x - r,
                nx2 = node.x + r,
                ny1 = node.y - r,
                ny2 = node.y + r;
        return function(quad, x1, y1, x2, y2) {
            if (quad.point && (quad.point !== node)) {
                var x = node.x - quad.point.x,
                        y = node.y - quad.point.y,
                        lSq = x * x + y * y,
                        r = node.size + quad.point.size;
                if (lSq < r * r) {
                    var l = Math.sqrt(lSq);
                    l = (l - r) / l * .5;
                    node.x -= x *= l;
                    node.y -= y *= l;
                    quad.point.x += x;
                    quad.point.y += y;
                }
            }
            return x1 > nx2 || x2 < nx1 || y1 > ny2 || y2 < ny1;
        };
    }
};