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
    var renderFn;

    $('#load-indicator').hide().ajaxStart(function(){
        $(this).show();
    }).ajaxStop(function(){
        $(this).hide();
    });

    var dom = $('#paper');
    var width, height;

    function resizePaper(){
        width = Math.max(500, dom.width());
        height = dom.height();
        if (canvas) {
            canvas.attr('width', width).attr('height', height);
            ctx.canvas.width = width;
            ctx.canvas.height = height;
            force.size([width, height]);
            setAxisAndRedraw();
            // Explicitly forcing rerender at the new size, otherwise the canvas appears to flicker
            // as you resize due to the brief delay between canvas clear+resize and the d3.animation.timer firing
            renderFn && renderFn();
        }
    }

    $(window).on('resize', _.throttle(resizePaper, 200));
    resizePaper();

    var excanvas = /[?&]excanvas=true(&|$)/i.test(window.location.search) && typeof G_vmlCanvasManager !== 'undefined';

    if (!isCanvasSupported()) {
        $('<div></div>').css({position: 'absolute', 'font-size': 12}).text('Please use a browser with canvas support, e.g. IE9, Firefox or Chrome').appendTo(dom).position({
            at: 'center',
            my: 'center',
            of: dom
        });

        return;
    }

    function isCanvasSupported(){
        if (excanvas) {
            return true;
        }
        var el = document.createElement('canvas');
        return !!(el.getContext && el.getContext('2d'));
    }

    var color = d3.scale.category20();

    var force = d3.layout.force()
            .charge(function(d) { return d.size * -5.5; })
            .linkDistance(function(link, idx) { return (link.source.size + link.target.size) * 2; })
            .linkStrength(0.5)
            .gravity(0.1)
//            .theta(0.8)
            .size([width, height]);

    var canvas = $('<canvas></canvas>').appendTo(dom)
        .attr('width', width)
        .attr('height', height);

    if (excanvas) {
        G_vmlCanvasManager.initElement(canvas[0]);
    }

    var ctx = canvas[0].getContext('2d');
    ctx.canvas.width = width;
    ctx.canvas.height = height;

    var viewX = 0, viewY = 0, viewScale = 1, viewScaleFactor = 1, invViewscaleFactor = 1, dragMode;

    // Canvas drag+pan code
    dom.mousedown(function(evt){
        dragMode = true;
        var startViewX = viewX, startViewY = viewY, dragPos = [evt.pageX, evt.pageY];
        evt.preventDefault();

        $(document).bind('mouseup', onMouseUp).bind('mousemove', onMouseMove);

        function onMouseMove(evt){
            viewX = startViewX + (dragPos[0] - evt.pageX) / viewScaleFactor;
            viewY = startViewY + (dragPos[1] - evt.pageY) / viewScaleFactor;
            setAxisAndRedraw();
        }

        function onMouseUp() {
            dragMode = false;
            $(document).unbind('mouseup', onMouseUp).unbind('mousemove', onMouseMove);
        }
    });

    // Canvas scale code
    dom.mousewheel(function(evt, delta, deltaX, deltaY){
        viewScale += deltaY > 0 ? -1 : 1;
        viewScaleFactor = Math.pow(1.1, -viewScale);
        invViewscaleFactor = 1/viewScaleFactor;
        setAxisAndRedraw();
        return false;
    });

    function setAxisAndRedraw() {
        ctx.setTransform(viewScaleFactor, 0, 0, viewScaleFactor, calcOffset(width, viewX), calcOffset(height, viewY));
        runAnimation();

        function calcOffset(length, offset){
            return -(offset + (viewScaleFactor - 1) * (offset + 0.5 * length));
        }
    }

    function findNodeForEvt(quad, ptX, ptY){
        var found = false;
        // Running on the assumption of non-overlapping nodes due to the collision detection code
        quad.visit(function(quad, x1, y1, x2, y2) {
            if (quad.point) {
                var x = ptX - quad.point.x,
                        y = ptY - quad.point.y,
                        lSq = x * x + y * y,
                        r = quad.point.size;
                if (lSq < r * r) {
                    found = quad.point;
                }
            }
            return found || (x1 > ptX || x2 < ptX || y1 > ptY || y2 < ptY);
        });

        return found;
    }

    function pageXToNodeX(x){
        return calcOffset(x - dom.offset().left, width, viewX);
    }

    function pageYToNodeY(y){
        return calcOffset(y - dom.offset().top, height, viewY);
    }

    function calcOffset(pt, length, offset){
        var halfL = 0.5 * length;
        return offset + halfL + (pt - halfL) * invViewscaleFactor;
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

    var image = new Image();
    image.src = 'resources/images/user.png';
    var imageLoaded = false;
    image.onload = function(){
        imageLoaded = true;
        image.onload = null;
    };

    doSearch();

    function renderGraph(json) {
        if (!imageLoaded) {
            setTimeout(function(){
                renderGraph(json);
            }, 200);
            return;
        }

        renderFn = null;
        isAnimating = false;
        legend && legend.remove();
        noResults && noResults.remove();
        legend = $('<div id="legend"><ul></ul></div>').hover(function(){
            draggedEl || (this.style.overflowY = 'auto');
        }, function(){
            this.style.overflowY = 'hidden';
        });

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
            force.stop();
            ctx.setTransform(1, 0, 0, 1, 0, 0);
            ctx.clearRect(0, 0, width, height);
            ctx.restore();

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
            link.opacity = link.opacityTo = 1;

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
            node.opacity = node.opacityTo = 1;
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
                nodes.forEach(function(d){
                    d.opacityTo = hovered === d ? 1 : highlightOnHover[hovered.index + ',' + d.index] ? 0.8
                        : hovered.clusterId === d.clusterId  ? 0.6 : 0.08;
                });

                links.forEach(function(d){
                    d.opacityTo = hovered === d.target || hovered === d.source ? 1 : 0.5;
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
                else {
                    runAnimation();
                }
            }
            else {
                nodes.forEach(function(d){
                    d.opacityTo = 1;
                });

                links.forEach(function(d){
                    d.opacityTo = 1;
                });
                legend.find('li').css('background-color', '').css('color', '').stop().animate({ height: baseHeight }, delay);
                header.stop().animate({height: '0px'}, {
                    duration: delay,
                    step: syncHeight
                });

                samplescontainer.stop().animate({opacity: 0});
                canChangeText || runAnimation();
            }
        }

        function syncHeight(num) {
            samplescontainer.css('top', num + 52);
        }

        var TWO_PI = 2 * Math.PI;

        force.on('tick', renderStep).on('start', function(){
            isAnimating = true;
        }).on('end', function() {
            isAnimating = false;
        });

        isAnimating = true;

        renderFn = renderStep;

        var draggedEl, dragX, dragY;

        function normalizeTouchMouse(evt) {
            if (evt.originalEvent && evt.originalEvent.changedTouches) {
                return evt.originalEvent.changedTouches[0];
            }
            return evt;
        }

        canvas.off('.usergraph').on('mousedown.usergraph touchstart.usergraph', function(evt){
            evt = normalizeTouchMouse(evt);
            var ptX = pageXToNodeX(evt.pageX),
                ptY = pageYToNodeY(evt.pageY),
                found = quad && findNodeForEvt(quad, ptX, ptY);

            if (found) {
                draggedEl = found;
                dragX = ptX;
                dragY = ptY;
                draggedEl.fixed |= 2;
                return false;
            }
        }).on('mousemove.usergraph touchmove.usergraph', function(evt){
            evt = normalizeTouchMouse(evt);
            if (dragMode) {
                return false;
            }

            if (draggedEl) {
                draggedEl.px = pageXToNodeX(evt.pageX);
                draggedEl.py = pageYToNodeY(evt.pageY);
                force.resume();
                return false;
            }

            var ptX = pageXToNodeX(evt.pageX),
                ptY = pageYToNodeY(evt.pageY),
                found = quad && findNodeForEvt(quad, ptX, ptY);

            if (found) {
                if (hoveredNode !== found) {
                    hoveredNode && (hoveredNode.fixed &= 1);
                    found.fixed |= 2;
                    hoveredNode = found;
                    updateOpacity(true);
                    runAnimation();
                }
            }
            else if (hoveredNode) {
                if(dragX == null) { hoveredNode.fixed &= 1; }
                hoveredNode = null;
                updateOpacity();
                runAnimation();
            }

            return false;
        });

        $(document).off('.usergraph').on('mouseup.usergraph touchend.usergraph', function(evt){
            evt = normalizeTouchMouse(evt);
            if (draggedEl) {
                draggedEl.fixed &= 1;

                // since we're stopping propagation to stop the drag+pan code from activating, we don't
                // get a click event, so we need to test for clicks here
                var dx = dragX - draggedEl.px, dy = dragY - draggedEl.py;
                if (dx * dx + dy * dy < 100) {
                    onShowNodeDocs(draggedEl, evt);
                }

                dragX = undefined;
                draggedEl = null;
                return false;
            }
            return false;
        });

        var quad;

        var opacityStep = 0.5;

        function renderStep() {
            var opacity, opacityTo, animating;
            ctx.save();
            ctx.setTransform(1, 0, 0, 1, 0, 0);
            ctx.clearRect(0, 0, width, height);
            ctx.restore();
            var ii = 0;
            quad = d3.geom.quadtree(nodes);

            while (++ii < nNodes) {
                quad.visit(collide(nodes[ii]));
            }

            ctx.strokeStyle = '#888';
            ctx.lineWidth = '1.5';
            var baseAlpha = 0.9;

            for (ii = 0; ii < nLinks; ++ii) {
                // can't optimize out the beginPath() since the lines have to be drawn at different opacities
                var link = links[ii];
                ctx.beginPath();

                opacity = link.opacity;
                opacityTo = link.opacityTo;
                if (opacity !== opacityTo) {
                    opacity = link.opacity = opacity < opacityTo ? Math.min(opacityTo, opacity + opacityStep)
                                                                 : Math.max(opacityTo, opacity - opacityStep);
                    animating = true;
                }

                ctx.globalAlpha = baseAlpha * opacity;
                ctx.moveTo(link.source.x, link.source.y);
                ctx.lineTo(link.target.x, link.target.y);
                ctx.stroke();
            }

            ctx.strokeStyle = '#fff';
            ctx.lineWidth = 1.5;

            for (ii = 0; ii < nNodes; ++ii) {
                var node = nodes[ii];
                ctx.beginPath();
                ctx.fillStyle = node.color;

                opacity = node.opacity;
                opacityTo = node.opacityTo;
                if (opacity !== opacityTo) {
                    opacity = node.opacity = opacity < opacityTo ? Math.min(opacityTo, opacity + opacityStep)
                                                                 : Math.max(opacityTo, opacity - opacityStep);
                    animating = true;
                }

                ctx.globalAlpha = node.opacity;
                ctx.arc(node.x, node.y, node.size, 0, TWO_PI, false);
                ctx.fill();
                ctx.stroke();
                ctx.globalAlpha = 1;
                ctx.drawImage(image, node.x - 8, node.y - 8);
            }

            return animating;
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

    var isAnimating, hasTimer;

    function runAnimation() {
        if (!isAnimating && renderFn && !hasTimer) {
            hasTimer = true;
            d3.timer(function(){
                // return true to stop
                var stop = isAnimating || !renderFn || !renderFn();
                if (stop) {
                    hasTimer = false;
                }
                return stop;
            });
        }
    }
};
