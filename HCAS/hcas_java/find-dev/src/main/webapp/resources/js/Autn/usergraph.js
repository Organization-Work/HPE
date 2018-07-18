if (typeof Autn === 'undefined') {
    Autn = {};
}

Autn.usergraph = function(opts){
    var $ = jQuery;

    if (!opts) {
        opts = {};
    }

    var showNodeDocs = opts.showNodeDocs;
    var showTermText = opts.showTermText;
    var termWeights = opts.termWeights;
    var searchCallback = opts.searchCallback;
    var fetchProfiles = opts.fetchProfiles;
    var fetchQuerySummary = opts.fetchQuerySummary;
    var fetchSuggestions = opts.fetchSuggestions;

    $('#load-indicator').hide().ajaxStart(function(){
        $(this).show();
    }).ajaxStop(function(){
        $(this).hide();
    });

    var dom = $('#paper');
    var width, height;

    function resizePaper(){
        var oldW = width, oldH = height;
        width = Math.max(500, dom.width());
        height = dom.height();
        if (svg) {
            force.size([width, height]);
            // viewX and viewY need to be adjusted to work
            viewX += 0.5 * (width - oldW), viewY += 0.5 * (height - oldH);
            svg.attr('width', width).attr('height', height);
            setAxisAndRedraw();
        }
    }

    $(window).on('resize', _.throttle(resizePaper, 200));
    resizePaper();

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
        setAxisAndRedraw();
        return false;
    });

    function setAxisAndRedraw() {
        viewScaleFactor = Math.pow(1.1, viewScale);
        viewW = width * viewScaleFactor;
        viewH = height * viewScaleFactor;
        svg.attr('viewBox', [viewX - .5*viewW,  viewY - .5*viewH, viewW, viewH].join(' '));
    }

    var cull = !/[?&]cull=false(&|$)/i.test(window.location.search),
        maxUnlinked = opts.maxUnlinked >= 0 ? opts.maxUnlinked : 5,
        adapt= !/[?&]adapt=false(&|$)/i.test(window.location.search),
        queryParam = /[?&]query=([^&]+)(&|$)/.exec(window.location.search),
        query = queryParam ? decodeURIComponent(queryParam[1]) : '',
        minLinks = opts.minLinks,
        nodeMinRadiusParam = /[?&]nodeMinRadius=(\d+(\.\d*)?)(&|$)/i.exec(window.location.search),
        nodeMinRadius = nodeMinRadiusParam ? Number(nodeMinRadiusParam[1]) : 10,
        nodeMaxRadiusParam = /[?&]nodeMaxRadius=(\d+(\.\d*)?)(&|$)/i.exec(window.location.search),
        nodeMaxRadius = Math.max(nodeMaxRadiusParam ? Number(nodeMaxRadiusParam[1]) : 40, nodeMinRadius),
        minDate;

    var delayParam = /[?&]delay=(\d+)(&|$)/i.exec(window.location.search);
    var delay = delayParam ? Number(delayParam[1]) : 300;

    var lastAjax;

    function doSearch() {
        lastAjax && lastAjax.abort();

        minLinks = desiredSliderValue;
        minTermsSlider.slider('value', desiredSliderValue);

        var fieldVal = queryField.val();
        var query = emptyText !== fieldVal ? fieldVal || '' : '';
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

    var minTermsTooltip;

    var minSliderValue = 1,
        desiredSliderValue = minLinks;

    var minTermsSlider = $('#mintermslider').slider({
        value: desiredSliderValue,
        min: minSliderValue,
        max: 11,
        step: 1,
        slide: function(evt, ui) {
            var val = Number(ui.value);

            if (!minTermsTooltip) {
                minTermsTooltip = $('<div>'+val+'</div>').css({position: 'absolute', textAlign: 'center', display:'none'}).appendTo(minTermsSlider);
            }
            else {
                minTermsTooltip.text(val);
            }

            // Delay is to ensure it happens after the slide event (after the handle has been repositioned)
            setTimeout(function(){
                minTermsTooltip && minTermsTooltip.show().position({my: 'center top', at: 'center bottom', of: ui.handle});
            }, 1);
        },
        stop: function(evt, ui) {
            minTermsTooltip && minTermsTooltip.remove();
            minTermsTooltip = null;
            $(ui.handle).blur();
            desiredSliderValue = ui.value;
            doSearch();
        }
    });

    var controls = $('#controls').submit(doSearch);

    function resize(evtOrCmp){
        var el = $(this);
        var val = el.val();
        if (evtOrCmp.type === 'blur' && !val) {
            val = emptyText;
            el.val(emptyText);
        }

        el.css('color', val === emptyText ? '' : 'black');
        this.size = Math.max(this.value.length, minQueryFieldLength);
    }

    var queryField = $('input[name=query]');
    query && queryField.val(query);
    var minQueryFieldLength = queryField.attr('size');
    var emptyText = queryField.data('emptytext');

    queryField.on('keyup keydown blur update', resize).each(resize);
    queryField.on('focus', function(){
        if (this.value === emptyText) {
            this.value = '';
        }
    });

    var legend, header, noResults;

    var aqgcontainer = $('#aqgcontainer'),
        samplescontainer = $('#samplescontainer').css('opacity', 0).show(),
        samplesEl = $('#samples');


    doSearch();

    function renderGraph(json) {
        legend && legend.remove();
        noResults && noResults.remove();
        legend = $('<div id="legend"><ul></ul></div>').hover(function(){
            this.style.overflowY = 'auto';
        }, function(){
            this.style.overflowY = 'hidden';
        });
        svg.selectAll('image,text,circle.node,line.link').remove();

        header = aqgcontainer.html('<div></div>');
        var headerBody = header.children('div');

        var nodes = json.nodes;
        var nOrigNodes = nodes.length;
        var links = json.links;
        var linkMeta = json.linkMeta;

        // link culling if the concepts share too few shared terms/references/docids
        links = links.filter(function(link){ return link.shared >= minLinks; });
        var nLinks = links.length;

        links.forEach(function(link){
            var src = nodes[link.source];
            var target = nodes[link.target];
            src.used = target.used = true;
            link.source = src;
            link.target = target;
        });

        if (cull) {
            if (minLinks <= minSliderValue) {
                var allowedUnlinked = maxUnlinked;
                nodes = nodes.filter(function(node){
                    var used = node.used;
                    if (used) {
                        return true;
                    }

                    --allowedUnlinked;
                    return allowedUnlinked >= 0;
                });
            }
            else {
                nodes = nodes.filter(function(node){
                    return node.used;
                });
            }
        }

        var nNodes = nodes.length;

        if (!nNodes) {
            // nothing to draw

            if (adapt && nOrigNodes > nNodes && minLinks > minSliderValue) {
                minLinks--;
                minTermsSlider.slider('value', minLinks);
                renderGraph(json);
                return;
            }

            noResults = $('<div>No results available, please try a different query</div>').css({position: 'absolute', opacity: 0}).appendTo(dom).position({
                at: 'center',
                my: 'center',
                of: dom
            }).animate({opacity: 1});

            return;
        }

        var nodeToNodeGroup = _.range(nNodes),
            highlightOnHover = {},
            linksAvailable = nLinks && links[0].links;

        for (var ii = 0; ii < nNodes; ++ii) {
            nodes[ii].index = ii;
        }

        links.forEach(function(link){
            var sourceIdx = link.source.index;
            var targetIdx = link.target.index;
            highlightOnHover[sourceIdx + ',' + targetIdx] = highlightOnHover[targetIdx + ',' + sourceIdx] = link;
            link.opacity = 1;

            // Find distinct node groups; the nodes are linked by any chain of links (quick-find algorithm)
            var sId = nodeToNodeGroup[sourceIdx], tId = nodeToNodeGroup[targetIdx];
            for (var ii = 0; ii < nNodes; ++ii) {
                if (nodeToNodeGroup[ii] === sId) {
                    nodeToNodeGroup[ii] = tId;
                }
            }
        });

        // Build a list of terms for each distinct node group
        var nodeGroupToDocIds = {};
        var nodeGroupToTerms = {};
        var nodeGroupToQuerySummary = {};
        var nodeGroupToSuggestions = {};

        if (linkMeta) {
            nodes.forEach(function(node){
                // there's some terms. assign them to the node group
                var docIdsForCluster = linkMeta[node.clusterId];
                var nodeGroupId = nodeToNodeGroup[node.index];
                var docIdsForNodeGroup = nodeGroupToDocIds[nodeGroupId];
                if (!docIdsForNodeGroup) {
                    docIdsForNodeGroup = nodeGroupToDocIds[nodeGroupId] = {};
                }

                docIdsForCluster.forEach(function(docId){
                    docIdsForNodeGroup[docId] = true;
                });
            });

            _.each(nodeGroupToDocIds, function(val, key) {
                var docIds = nodeGroupToDocIds[key] = _.keys(val).sort();
                fetchProfiles(docIds).done(function(json){
                    var terms = json.terms;

                    var termText = nodeGroupToTerms[key] = terms.map(termWeights ?
                            function(t){ return t.term + '~['+t.weight+']' }
                          : function(t){ return t.term + '~'; });

                    fetchQuerySummary(termText).done(function(json){
                        var qs = json.querysummary;

                        // pick the longest term
                        var clusterTerms = {};

                        qs.forEach(function(element){
                            var cluster = element.cluster;
                            if (cluster >= 0) {
                                var phrase = clusterTerms[cluster];
                                var word = element.value;
                                if (phrase) {
                                    if (word.length > phrase.word.length) {
                                        phrase.word = word;
                                    }
                                    phrase.pdocs += element.pdocs;
                                } else {
                                    phrase = clusterTerms[cluster] = {word: word, pdocs: element.pdocs};
                                }
                            }
                        });

                        nodeGroupToQuerySummary[key] = {
                            markup: '<div class="ideascloud">&nbsp;</div>' + _.shuffle(_.values(clusterTerms)).map(function(phrase){
                                var pd = phrase.pdocs,
                                    num = pd > 45 ? 5
                                        : pd > 25 ? 4
                                        : pd > 8 ? 3
                                        : pd > 5 ? 2
                                        : 1;
                                return '<span class="cloud'+num+'">'+_.escape(phrase.word)+'</span>';
                            }).join(' ')
                            + (showTermText ? '<div class="queryterms">'+terms.map(function(a){return '<span>'+_.escape(a.text||a.term)+'</span> ';}).join('')+'</div>' : '')
                        };
                    });


                    fetchSuggestions(termText).done(function(json){
                        var markup = '';
                        var startTag = '<span class="autn-highlight">';
                        var endTag = '</span>';

                        _.each(json.docs, function(message){
                            markup += '<div class="sampledoc"><div class="sampledoc-title">'
                                    + linkWrap(message.title, message.ref) + '</div><div class="sampledoc-body">'
                                    + tokenReplace(_.escape(message.summary), startTag, endTag)
                                    + '</div></div>';
                        });

                        function tokenReplace(str, startTag, endTag) {
                            return str && str.replace(/&lt;(&#x2F;)?autn:highlight&gt;/g, function(str, end){
                                return end ? endTag : startTag;
                            });
                        }

                        function linkWrap(str, href) {
                            if (typeof href === 'string' && href.match(/^http(s?):/)) {
                                return '<a href="' + _.escape(href) + '" target="_blank">' + _.escape(str) + '</a>';
                            }

                            return _.escape(str);
                        }

                        nodeGroupToSuggestions[key] = {
                            markup: markup
                        };
                    });
                });
            });
        }

        var clusterNodeMap = {};

        var minSize = nodes[nNodes - 1].size;
        var maxSize = nodes[0].size;
        var sizeScale = maxSize === minSize ? 0 : (nodeMaxRadius - nodeMinRadius) / (maxSize - minSize);
        nodes.forEach(function(node, idx){
            node.size = nodeMinRadius + (node.size - minSize) * sizeScale;

            node.label = String(1 + idx);
            node.clusterId = nodeToNodeGroup[node.index];
            node.color = json.sentiment ? Raphael.hsb(node.sentiment * 0.3333, 0.8, 0.9) : color(node.clusterId);
            node.textColor = d3.hsl(node.color).l < 0.6 ? '#fff' : '#000';

            $('<li></li>').text(node.name).data('node', node).appendTo(legend.children('ul'))
                .prepend('<span class="legendpad" style="background-color: '+node.color+';">&nbsp;</span>')
                .append('<div class="sharedterms"></div>');

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
        }).on('click', 'li', function(evt){
            onShowNodeDocs($(this).data('node'), evt);
            return false;
        });

        force.nodes(nodes)
             .links(links)
             .start();

        var link = svg.selectAll('line.link')
                .data(links)
                .enter().append('line')
                .attr('class', 'link');

        var node = svg.selectAll('circle.node')
                        .data(nodes)
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
                        .on('click', function(node){
                            onShowNodeDocs(node, d3.event)
                        });

        var labels = svg.selectAll('image')
                        .data(nodes)
                        .enter().append('image')
                        .attr('xlink:href', 'resources/images/user.png')
                        .attr('width', '16px')
                        .attr('height', '16px')
                        .classed('textlabel', true);

        var hoveredNode;

        function onShowNodeDocs(node, evt) {
            var nodegroupId = nodeToNodeGroup[node.index],
                terms = nodeGroupToTerms[nodegroupId];
            showNodeDocs(node, nodegroupId, evt || d3.event, terms);
        }

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
            var expandedHeight = baseHeight;

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
                        return;
                    }
                    var linked = highlightOnHover[hovered.index + ',' + d.index];
                    var props = {'color': linked ? '#000' : '#C0C0C0'};
                    if (canChangeText) {
                        var height = props.height = linked ? expandedHeight : 0;
                        toFocus || (hoveredOffset += height);
                    }
                    el.css('background-color', '').stop().animate(props, delay);
                });


                var nodeGroup = nodeToNodeGroup[hovered.index];
                var qs = nodeGroupToQuerySummary[nodeGroup];
                if (qs) {
                    headerBody.html(qs.markup);
                    header.stop().animate({height: headerBody.height()}, {
                        duration: delay,
                        step: syncHeight
                    });
                }

                var suggested = nodeGroupToSuggestions[nodeGroup];
                if (suggested) {
                    samplesEl.stop().html(suggested.markup);
                    var height = samplesEl[0].scrollHeight;
                    samplesEl[0].scrollTop = 0;
                    samplesEl.delay(2000).animate({scrollTop: height}, height * 100, 'linear');
                    samplescontainer.stop().animate({opacity: 1});
                }
                else {
                    samplescontainer.stop().animate({opacity: 0});
                }

                if (canChangeText) {
                    toFocus && toFocus.css('height', expandedHeight);
                    legend.stop().animate({scrollTop: hoveredOffset}, delay);
                }
            }
            else {
                node.transition(delay).attr('opacity', 1);
                link.transition(delay).attr('opacity', 1);
                legend.find('li').css('background-color', '').css('color', '').stop().animate({ height: baseHeight }, delay);
                header.stop().animate({height: '0px'}, {
                    duration: delay,
                    step: syncHeight
                });
                samplescontainer.stop().animate({opacity: 0});
            }
        }

        function syncHeight(num) {
            samplescontainer.css('top', num + 52);
        }

        node.append('title')
            .text(function(d) { return d.name; });

        force.on('tick', function() {
            var q = d3.geom.quadtree(nodes),
                    i = 0;

            while (++i < nNodes) {
                q.visit(collide(nodes[i]));
            }

            link.attr('x1', function(d) { return d.source.x; })
                .attr('y1', function(d) { return d.source.y; })
                .attr('x2', function(d) { return d.target.x; })
                .attr('y2', function(d) { return d.target.y; });

            node.attr('cx', function(d) { return d.x; })
                .attr('cy', function(d) { return d.y; });

            labels.attr('x', function(d) { return d.x - 8; })
                // need to account for font-size
                  .attr('y', function(d) { return d.y - 8; });
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
