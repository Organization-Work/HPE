jQuery(function($){
    require(['Autn/i18n'], $.noop);

    var NETWORK_MAP_URL = 'ajax/search/getFilteredNetworkMap.json';

    var previousXHR;

    var $networkmap = $('#networkmap');
    var $showPanel = $('#showPanel');

    var width = 600;
    var height = 600;

    var renderLinkStrength, skip = 0, lastSearch, lastNodeCount, color;
    var onSliderChange;

    var $numDiscards = $('<div></div>').css({
        'marginBottom': 10
    }).appendTo($networkmap);

    var $linkSliderLabel = $('<div class="slider-text-label">Threshold</div>').hide().appendTo($networkmap);

    var $linkSlider = $('<div></div>').css({
        float: 'left',
        margin: '0px 1% 10px',
        width: '85%'
    }).appendTo($networkmap).slider({
        value: 0,
        min: 0,
        max: 1,
        step: 0.01,
        slide: function(evt, ui){
            var strength = Number(ui.value);
            if (renderLinkStrength !== strength) {
                renderLinkStrength = strength;
                updateDiscardsCount(lastNodeCount, skip, renderLinkStrength);
                onSliderChange && onSliderChange();
            }
        }
    }).hide();

    $('<div style="clear:both;"></div>').appendTo($networkmap);

    var $skipSliderLabel = $('<div class="slider-text-label">Discard</div>').hide().appendTo($networkmap);

    var $skipSlider = $('<div></div>').css({
        float: 'left',
        margin: '0px 1% 10px',
        width: '85%'
    }).appendTo($networkmap).slider({
        value: skip,
        min: 0,
        max: 30,
        step: 1,
        slide: function(evt, ui){
            updateDiscardsCount(lastNodeCount, Number(ui.value), renderLinkStrength);
        },
        stop: function(evt, ui){
            var toSkip = Number(ui.value);
            if (skip !== toSkip) {
                skip = toSkip;
                fetchNewResults();
            }
        }
    }).hide();

    $('<div style="clear:both;"></div>').hide().appendTo($networkmap);

    var svg = d3.select($networkmap[0]).append('svg')
            .attr('width', width)
            .attr('height', height);


    var $tooltip = $('<div class="topicmap-tooltip"></div>')
            .hide().appendTo($networkmap);

    var $totals = $('<div id="networkmap-totals"></div>').appendTo($networkmap);

    var $loading = $('<div class="visualizer-loading"></div>').appendTo($networkmap).hide();

    var background = svg.append('g'),
        foreground = svg.append('g');

    var force = d3.layout.force()
            .linkStrength(0.1)
            .gravity(0.1)
//            .theta(0.8)
            .size([width, height]);


    $('[href=#networkmap]').on('shown', function(){
        var $lastChild = $networkmap.children().not('svg').last();
        $networkmap.width('100%').height($showPanel.height() - $lastChild[0].offsetTop - $lastChild.outerHeight());
        width = $networkmap.width();
        height = $networkmap.height();
        svg.attr('width', width);
        svg.attr('height', height);
        var scaledMin = Math.min(width, height);
        // weights obtained arbitrarily from testing with a few graphs
        var chargeScale = -0.060 * scaledMin;
        var linkDistScale = 0.011 * scaledMin;

        force.size([width, height])
             .charge(function(d) { return d.size * chargeScale; })
             .linkDistance(function(link, idx) { return (link.source.size + link.target.size) * linkDistScale; })
            .stop().start()
    });
    
    function getSnomedField() {
        var selectedSearchView = SearchEvents.getSelectedSearchView();
        return SearchConfig.searchViews[selectedSearchView].snomedTag;
    }


    function updateDiscardsCount(count, discards, prob) {
        if (!lastNodeCount) {
            $numDiscards.text('');
            return;
        }

        var showing = ['Showing ' + (count > 1 ? count + ' categories' : 'category')];

        if (discards) {
            showing.push('discarding top ' + (discards > 1 ? discards + ' categories' : 'category') + ' in each supercategory');
        }

        if (prob) {
            // 'percentage' here is conditional probability; asked to word it this way to alleviate data mining fears
            // \u2264 is <= symbol
            showing.push('hiding links with percentage \u2264 ' + (100 * prob).toFixed(1) + '%');
        }

        $numDiscards.text(showing.join(', '));
    }

    function addTerms(terms){
        var changed;

        _.each(terms, function(term){
            if ($('#advancedFilters').parametricfilter('addFilter', getSnomedField(), term, true)) {
                changed = true;
            }
        });

        if (changed) {
            $('[href=#advancedFilters]').trigger('click');
        }
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


    SearchEvents.$.on(SearchEvents.RESULTS_PROCESSING, function(e, results, totalResults, data) {
        var totalOverAllDatabases = _.reduce(totalResults, function(sum, val){
            return sum + val;
        }, 0);

        $totals.text(totalOverAllDatabases ? 'Total Results: ' + totalOverAllDatabases : '');

        lastSearch = data;

        color = d3.scale.category20();

        fetchNewResults();
    });

    function fetchNewResults() {
        if (!lastSearch) {
            return;
        }

        previousXHR && previousXHR.abort();

        $loading.show();
        
        previousXHR = $.ajax({
            url: NETWORK_MAP_URL + '?' + $.param({
                fieldname: getSnomedField(),
                values: 30,
                skip: skip
            }),
            type : 'POST',
            contentType : 'application/json',
            dataType : 'json',
            data: JSON.stringify(lastSearch),
            success: function(json){
                var nodeMap = {};

                var baseProbabilities = json[''] || [];

                var scale = d3.scale.linear().range([5, 30])
                              .domain([d3.min(baseProbabilities, count), d3.max(baseProbabilities, count)]);

                var nodes = _.map(baseProbabilities, function(val, idx){
                    var text = val.value;

                    var lastIdx = text.lastIndexOf(' (');
                    var category = text.slice(lastIdx + 2, text.length - 1);

                    return nodeMap[text] = {
                        color: color(category),
                        name: text,
                        rawName: text,
                        size: scale(val.count),
                        count: val.count
                    };
                });

                lastNodeCount = nodes.length;

                $skipSliderLabel.show();
                $skipSlider.show();
                $linkSliderLabel.show();
                $linkSlider.show();

                function count(d) {
                    return d.count;
                }

                var links = [];

                _.each(json, function(arr, key){
                    if (key !== '') {
                        var sourceNode = nodeMap[key];

                        _.each(arr, function(val){
                            var text = val.value;
                            var targetNode = nodeMap[text];
                            links.push({
                                source: sourceNode,
                                target: targetNode,
                                strength: Math.min(1, val.count / sourceNode.count)
                            })
                        });
                    }
                });

                var linkScale = d3.scale.linear().range([0.5, 3]);

                function strength(d) {
                    return d.strength;
                }

                force.nodes(nodes)
                     .links(links)
                     .start();

                var link = background.selectAll('line.networkmap-link')
                        .data(links);

                link.exit().remove();

                link.enter().append('line').attr('class', 'networkmap-link');

                link.on('mouseover', function(){
                    var link = d3.select(this).classed('hover', true).datum();

                    labels.each(function(d){
                        if (d === link.source) {
                            d3.select(this).classed('source', true);
                        } else if (d === link.target) {
                            d3.select(this).classed('target', true);
                        }
                    });

                    var reverse = _.find(links, function(other){
                        return other.source === link.target && other.target === link.source;
                    });

                    var html = '<span class="source">X</span>\u2192<span class="target">Y</span>: ' + (link.strength * 100).toFixed(1) + '%';

                    if (reverse) {
                        html += '<br><span class="target">Y</span>\u2192<span class="source">X</span>: ' + (reverse.strength * 100).toFixed(1) + '%';
                    }

                    $tooltip.css('background', 'darkgray')
                            .html(html)
                            .show().position({ my: 'middle bottom', of: d3.event, offset: '0 -5', collision: 'fit', within: $networkmap});
                }).on('mouseout', function(){
                    var link = d3.select(this).classed('hover', false).datum();
                    labels.filter(function(d){
                        return d === link.source || d === link.target;
                    }).classed('source target', false);
                    $tooltip.hide();
                }).on('click', function(){
                    var linkMeta = d3.select(this).datum();
                    addTerms([linkMeta.source.rawName, linkMeta.target.rawName])
                });

                onSliderChange = updateLinkFiltering;
                updateLinkFiltering();

                function updateLinkFiltering() {
                    linkScale.domain([Math.max(renderLinkStrength, d3.min(links, strength)), d3.max(links, strength)]);

                    link.classed('hidden', function(d,i){
                        return d.strength <= renderLinkStrength;
                    }).attr('stroke-width', function(d){
                        return linkScale(d.strength);
                    })
                }

                var node = foreground.selectAll('circle.networkmap-node')
                              .data(nodes);

                node.exit().remove();

                node.enter().append('circle')
                            .attr('class', 'networkmap-node');

                node.attr('r', function(d) { return d.size; })
                    .style('fill', function(d) { return d.color; })
                    .call(force.drag)
                    .on('mousemove', function(d) {
                        require(['Autn/i18n'], function(i18n){
                            $tooltip.css('background', d.color)
                                    .html(_.escape(d.name) + '<br>' + i18n('idolview.center.label.numdocs', {DOCS: d.count}))
                                    .show().position({ my: 'left bottom', of: d3.event, offset: '0 -5', collision: 'fit', within: $networkmap});
                        });

                        link.filter(function(link,i){
                            return link.source === d;
                        }).classed('hover', true);
                    })
                    .on('mouseout', function(d) {
                        $tooltip.hide();
                        link.classed('hover', false);
                    })
                    .on('click', function(d){
                        addTerms([d.rawName]);
                    });

                var labels = background.selectAll('text.networkmap-label')
                                .data(nodes);

                labels.exit().remove();

                labels.enter().append('text')
                              .attr('class', 'networkmap-label');

                labels.text(function(d){ return d.name; });

                force.on('tick', function() {
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
                          .attr('y', function(d) { return d.y + d.size + 10;});
                });

                updateDiscardsCount(lastNodeCount, skip, renderLinkStrength);
            }
        }).always(function(){
            $loading.hide();
        });
    }
});