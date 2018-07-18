(function($){
    var methods = {
        init: function(options) {
            return $(this).each(function() {
                var dom = $(this), pluginMeta = dom.data('visualizer');

                if (pluginMeta) {
                    // plugin has been set before
                    return;
                }

                pluginMeta = {};
                dom.data('visualizer', pluginMeta);

                setupPlugin(dom, options || {}, pluginMeta);
            });
        },
        resize: function(width, height) {
            return $(this).each(function(){
                var dom = $(this), pluginMeta = dom.data('visualizer');
                if (pluginMeta) {
                    pluginMeta.resize(width, height);
                }
            });
        },
        reset: function() {
            return $(this).each(function(){
                var dom = $(this), pluginMeta = dom.data('visualizer');
                if (pluginMeta) {
                    pluginMeta.reset();
                }
            });
        },
        search: function(query, numresults, loaderPath, loaderW, loaderH) {
            return $(this).each(function(){
                var dom = $(this), pluginMeta = dom.data('visualizer');
                if (pluginMeta) {
                    pluginMeta.search(query, numresults, loaderPath, loaderW, loaderH);
                }
            });
        }
    };

    $.fn.visualizer = function(method) {
        if ( methods[method] ) {
            return methods[method].apply( this, Array.prototype.slice.call( arguments, 1 ));
        } else if ( typeof method === 'object' || ! method ) {
            return methods.init.apply( this, arguments );
        } else {
            $.error( 'Method ' +  method + ' does not exist on jQuery.visualizer' );
        }
    };

        function setupPlugin(dom, options, pluginMeta) {
            var tooltipEl = options.tooltip;
            var navTreeEl = options.navTree;
            var handleResize = options.handleResize;
            var onRefClick = options.onRefClick;
            var doAjaxFetch = options.doAjaxFetch || (function(node, numresults, excludeRefs, onSuccess, onError) {
                // node: node we're expanding, will have depth===0 if it's the root node
                // numresults is the numresults specified in the search() call
                // excludeRefs is an array of node.refs which have previously been returned
                // onSuccess should be called if your call succeeds (expects one argument, a JSON object with children array)
                // onError should be called if your call returns
                // Your callback should return an XHR object, e.g. the return value of jQuery.ajax
                var url, params;

                if (node.depth === 0) {
                    // either initial fetch or expansion of root node
                    url = 'query.json';
                    params = {
                        pageSize: numresults,
                        query: node.title,
                        summary: false,
                        totalResults: false,
                        exclude: excludeRefs
                    };
                }
                else {
                    // expansion of non-root node
                    url = 'suggest.json';
                    params = {
                        pageSize: numresults,
                        reference: node.refs ? node.refs.map(function(a){return a.ref;}) : node.ref,
                        summary: false,
                        totalResults: false,
                        exclude: excludeRefs
                    };
                }

                return $.ajax(url, {
                    contentType: 'application/x-www-form-urlencoded; charset=UTF-8',
                    dataType: 'json',
                    type: 'POST',
                    traditional: true,
                    data: params,
                    success: onSuccess,
                    error: onError
                });
            });

            dom.bind('contextmenu', returnFalse);
            var width = dom.width(), height = dom.height();

            navTreeEl.parent('div').bind('contextmenu', returnFalse);

            var paper = Raphael(dom[0], width, height);

            function returnFalse(){ return false; }

            pluginMeta.reset = resetView;

            pluginMeta.resize = function(availWidth, availHeight) {
                if (availWidth > 0 && availHeight > 0) {
                    width = availWidth;
                    height = availHeight;
                    paper.setSize(availWidth, availHeight);
                    dom.width(availWidth);
                    dom.height(availHeight);
                    viewW = width * viewScaleFactor;
                    viewH = height * viewScaleFactor;
                    paper.setViewBox(viewX - .5*viewW,  viewY - .5*viewH, viewW, viewH);
                }
            };

            var excludeRefs = [];
            var linkMap = {};
            var firstMarker;
            var highlightedLinks;

            var linkColor = '#ccc', linkHighlightColor = 'orange', suggestionMarkerFill = 'r#834cff-white', isRendering;

            function resetView(){
                lastAjax && lastAjax.abort();
                if (lastNodes) {
                    lastNodes.forEach(function(node) {
                        if (node.loading) {
                            node.loading.abort();
                            node.loading = null;
                        }
                    });
                    lastNodes = null;
                }
                paper.clear();
                viewScaleFactor = 1;
                viewScale = viewX = viewY = 0;
                viewW = width;
                viewH = height;
                paper.setViewBox(-.5*width, -.5*height, width, height);
                navTreeEl.empty().siblings('div').remove();
                handleResize();
            }

            var lastAjax, lastNodes, lastNumResults;

            // Canvas drag code
            var dragMode = false;
            var viewX = 0, viewY = 0, viewW = width, viewH = height, viewScale = 1, viewScaleFactor = 1;
            handleResize();

            dom.mousedown(function(evt){
                dragMode = true;
                var startViewX = viewX, startViewY = viewY, dragPos = [evt.pageX, evt.pageY];

                $(document).bind('mouseup', onMouseUp).bind('mousemove', onMouseMove);

                function onMouseMove(evt){
                    viewX = startViewX + dragPos[0] - evt.pageX,
                    viewY = startViewY + dragPos[1] - evt.pageY;
                    paper.setViewBox(viewX - .5*viewW,  viewY - .5*viewH, viewW, viewH);
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
                paper.setViewBox(viewX - .5*viewW,  viewY - .5*viewH, viewW, viewH);
                return false;
            });

            // Navigator click code
            $(document).on('click', '.node-nav', function(){
                var node = $(this).data('node');
                if (node && node.depth) {
                    onRefClick && onRefClick(node);
                }
            });

            // Navigator mouseover
            $(document).on('mouseover', '.node-nav', function() {
                var node = $(this).data('node');
                if (node) {
                    highlightPathToNode(node, false, $(this).data('mark'));
                }
            });

            // Navigator mouseup
            $(document).on('mouseup', '.node-nav', function(evt) {
                // right click
                if (evt.ctrlKey || (evt.which != null ? evt.which === 3 : evt.button === 2)) {
                    var mark = $(this).data('mark') || $(this).data('node');
                    if (mark.mark) {
                        unmarkNode(mark, false);
                    }
                    return false;
                }
            });

            pluginMeta.search = function(queryText, numresults, loaderPath, loaderW, loaderH) {
                resetView();
                navTreeEl.html('&nbsp;');
                var loader = paper.image(loaderPath, -0.5*loaderW, -0.5*loaderH, loaderW, loaderH);
                excludeRefs.length = 0;
                linkMap = {};
                firstMarker = highlightedLinks = null;
                var rootNode = {name: queryText, title: queryText, ref: 'root', depth: 0};
                lastAjax = doAjaxFetch(rootNode, numresults, excludeRefs, function(json){
                    paper.clear();
                    isRendering = false;
                    lastNumResults = numresults;
                    renderData(docsToTree(json, rootNode));
                }, function(){
                    paper.clear();
                    paper.text(0, 0, 'No results available, please try a different query').attr({
                        dy: '.35em', 'text-anchor': 'middle',
                        fill: 'black',
                        'font-family': 'Verdana',
                        'font-weight': 'bold',
                        'font-size': 12,
                        opacity: 0.1
                    }).animate({opacity: 1}, 500);
                    loader.remove();
                });

                if (!lastAjax) {
                    throw new Error('doAjaxFetch callback should return an XHR object from jQuery.ajax');
                }
            };

            function docsToTree(json, root) {
                var newNodes = json.docs.map(function(child) {
                    excludeRefs.push(child.ref);
                    var fixedTitle = htmlFix(child.title);
                    return {name: fixedTitle, ref: child.ref, children: [], title: fixedTitle, sentiment: child.sentiment};
                });

                root.children = root.children ? root.children.concat(newNodes) : newNodes;
                return root;
            }

            function htmlFix(str) {
                // the wikipedia data seems to contain escaped titles
                return str && str.replace(/&quot;/g, '\'').replace(/&amp;/g, '&');
            }

            function findOverlap(distanceSq, paperX, paperY, json, exclude) {
                if (json !== exclude && json.abspos) {
                    var x = json.abspos[0];
                    var y = json.abspos[1];
                    var dx = paperX - x;
                    var dy = paperY - y;
                    if (dx * dx + dy * dy < distanceSq) {
                        return json;
                    }
                }

                var children = json.children;
                if (children) {
                    for (var ii = children.length - 1; ii >=0; --ii) {
                        var intersect = findOverlap(distanceSq, paperX, paperY, children[ii], exclude);
                        if (intersect) {
                            return intersect;
                        }
                    }
                }

                return null;
            }

            function strokeNodeColor(node) {
                return node.depth === 0 ? '#07ccd7'
                    : node.mark ? node.mark.color
                    : 'steelblue';
            }

            function fillNodeColor(node) {
                return node.refs && node.refs.length > 1 ? suggestionMarkerFill
                    : node.sentiment == null ? 'white'
                    : node.sentiment === 'Positive' ? '#00ff3c'
                    : node.sentiment === 'Negative' ? '#ff1a1a'
                    : '#ffeb0c';
            }

            function unmarkNode(node, isDeletion) {
                var markMeta = node.mark;
                if (!node.depth || !markMeta) {
                    return;
                }
                // clear highlighting
                delete node.mark;
                isDeletion || node.marker.attr({
                    fill: fillNodeColor(node),
                    stroke: strokeNodeColor(node)
                });
                markMeta.el && markMeta.el.remove();
                highlightPathToNode(node, false);
                updateNodeNavEl(node, false);
                handleResize();
            }

            function markNode(node, isDeletion, fillColor) {
                if (!node.depth || node.mark) {
                    return;
                }

                var color = fillColor || Raphael.getColor();
                node.mark = {color: color};
                node.marker.attr({stroke: color});
                updateNodeNavEl(node, false);
                highlightPathToNode(node, true);
            }

            function highlightPathToNode(node, updatePath, mark) {
                var markDetails = mark ? mark.mark : node.mark;
                var desiredColor = markDetails ? markDetails.color : linkHighlightColor;

                if (updatePath) {
                    var rootpathlabel = navTreeEl.empty();
                    var pathContainer = rootpathlabel.parent('div');
                    var pathHeight = pathContainer.height();

                    if (markDetails) {
                        var markEl = markDetails.el;
                        if (!markEl) {
                            markDetails.el = markEl = $('<div style="color:'+desiredColor+'"></div>').appendTo(pathContainer);
                        }
                        else {
                            markEl.empty();
                        }
                    }
                }

                if (highlightedLinks) {
                    highlightedLinks.attr('stroke', linkColor);
                }
                highlightedLinks = paper.set();

                for (var current = node; current; current = current.parent) {
                    if (current.parent) {
                        var link = linkMap[current.ref];
                        if (link) {
                            highlightedLinks.push(link);
                        }
                    }
                    if (updatePath) {
                        if (current !== node) {
                            rootpathlabel.prepend('\u2192');
                            markDetails && markEl.prepend('\u2192');
                        }

                        var css = 'node-nav', style='';
                        if(current.depth === 0) {
                            css += ' node-nav-query';
                        }
                        else if (current.mark) {
                            style = ' style="background-color:'+current.mark.color+'"';
                        }
                        else if (current.refs && current.refs.length > 1) {
                            css += ' node-nav-multiple';
                        }
                        else if (current.sentiment != null) {
                            css += ' node-nav-' + current.sentiment.slice(0,3).toLowerCase();
                        }

                        var markup = '<span class="'+css+'"'+style+'>'+_.escape(current.title)+'</span>';
                        $(markup).data('node', current).prependTo(rootpathlabel);
                        markDetails && $(markup).data('node', current).data('mark', node).prependTo(markEl);
                    }
                }

                highlightedLinks.attr('stroke', desiredColor);
                if (firstMarker) {
                    highlightedLinks.insertBefore(firstMarker);
                }

                node.marker.animate({'25%': {r: 7}, '50%': {r: 4.5}, '75%': {r: 5.8}, '100%': {r: 4.5}}, 500);

                if(updatePath && pathHeight !== pathContainer.height()) {
                    handleResize();
                }
            }

            function updateNodeNavEl(node, isSuggestion) {
                var navEls = $('span.node-nav').filter(function(idx, el){return $(el).data('node') === node;});
                if (isSuggestion) {
                    if (node.refs && node.refs.length > 1) {
                        navEls.addClass('node-nav-multiple');
                    }
                }

                navEls.css('background-color', node.mark ? node.mark.color : '');
            }

            function binarySearchX(a, fromIndex, toIndex, key) {
                var low = fromIndex, high = toIndex - 1;

                while (low <= high) {
                    var mid = (low + high) >>> 1, midVal = a[mid].x;

                    if (midVal < key)
                        low = mid + 1;
                    else if (midVal > key)
                        high = mid - 1;
                    else
                        return mid; // actual index found
                }
                return -(low + 1);  // insertion index.
            }

            function renderData(json) {
                isRendering = false;
                var halfW = 0.5 * width;
                var halfH = 0.5 * height;
                var r = Math.min(halfW, halfH);
                var edgePad = 0.25 * r;
                var treeDepth = r - edgePad;

                var tree = d3.layout.tree()
                    .size([360, treeDepth])
                    .separation(function(a, b) { return (a.parent == b.parent ? 1 : 2) / a.depth; });

                var diagonal = d3.svg.diagonal.radial()
                    .projection(function(d) { return [d.y, d.x / 180 * Math.PI]; });

                var nodes = tree.nodes(json);

                if (nodes.length <= 2) {
                    nodes.forEach(function(node){
                        if (isNaN(node.x)) {
                            node.x = 0;
                        }
                    });
                }

                lastNodes = nodes;

                var links = tree.links(nodes);

                var animTime = 1000;

                var animateOpacity = [];
                var animObj;
                var animEl;

                var dragEl, dragTextEl, colorPicker, isRightClick;

                var maxDepth = 0;

                var sortedNodes = nodes.slice().sort(function(a,b){
                    return a.x - b.x;
                });

                links.forEach(function(link) {
                    var path = diagonal(link);

                    if (Raphael.vml) {
                        // VML doesn't cope with scientific notation e.g. 1.1021457184401395e-14
                        path = path.replace(/\d(\.\d+)?e-\d+/g, 0);
                    }

                    if (link.target.depth > maxDepth) {
                        maxDepth = link.target.depth;
                    }

                    var finalPoint = /([^, ]+),([^, ]+)$/.exec(path);
                    link.target.abspos = [Number(finalPoint[1]), Number(finalPoint[2])];

                    var pairKey = link.target.ref;
                    var pair = linkMap[pairKey];
                    if (pair) {
                        animateTogether(pair, {path: path}, animTime);
                        return;
                    }

                    animateOpacity.push(
                        pair = linkMap[pairKey] = paper.path(path).attr({stroke: linkColor, 'stroke-width': 1.5, opacity: 0, transform: Raphael.vml ? 't0,0' : undefined})
                    );

                    if (firstMarker) {
                        pair.insertBefore(firstMarker);
                    }
                });

                nodes.forEach(function(node){
                    // Have to use a relative rotation, absolute rotations seem cause problems with setViewBox in VML;
                    // possibly the viewbox is handled as another transform but without updating the rotation origin
                    // for the absolute rotation.
                    var transform = 'r' + (node.x + 180) + ',0,0t0,' + node.y;

                    if (node.marker) {
                        animateTogether(node.marker, {transform: transform}, animTime);
                    }
                    else {
                        node.marker = paper.circle(0,0, 4.5).attr({
                            transform: transform,
                            fill: fillNodeColor(node),
                            cursor: 'pointer',
                            stroke: strokeNodeColor(node),
                            'stroke-width': 1.5,
                            opacity: 0
                        }).data('node', node).drag(function(dx,dy, x,y,evt){
                            evt.stopPropagation();
                            evt.preventDefault();
                            if (node.depth === 0) {
                                return;
                            }
                            var offset = dom.offset();
                            var cx = this.dragX = viewX + (x - offset.left - width * 0.5) * viewScaleFactor,
                                cy = this.dragY = viewY + (y - offset.top  - height * 0.5) * viewScaleFactor;

                            // evt.button and evt.which aren't set correctly in Firefox during dragMove
                            if (isRightClick) {
                                if (node.mark) {
                                    return;
                                }
                                // right drag
                                var paperDY = cy - node.abspos[1];
                                var paperDX = cx - node.abspos[0];
                                var radius = Math.max(12, Math.min(25, Math.sqrt(paperDX * paperDX + paperDY * paperDY)));
                                var brightness = radius / 25;
                                var color = 'hsb('+(Math.atan2(paperDY, paperDX) / Math.PI * 0.5 + 0.5)%1 + ',.9,'+brightness+')';

                                if (colorPicker) {
                                    colorPicker.attr({cx: node.abspos[0], cy: node.abspos[1], r: radius, stroke: color, fill: color});
                                }
                                else {
                                    colorPicker = paper.circle(node.abspos[0], node.abspos[1], radius).attr({stroke: color, fill: color, 'fill-opacity': 0.3});
                                    colorPicker.toBack();
                                }
                                return;
                            }

                            if (dragEl) {
                                dragEl.attr({cx: cx, cy: cy});
                                dragTextEl.attr({x: cx + 8, y: cy});
                                return;
                            }

                            dragEl = paper.circle(cx, cy, 4.5).attr({
                                fill: 'white',
                                stroke: 'steelblue',
                                'stroke-width': 1.5,
                                opacity: 0.8
                            });
                            dragTextEl = paper.text(cx + 8, cy, node.title).attr({
                                fill: 'black',
                                'text-anchor': 'start',
                                'font-size': 14
                            });
                            node.marker.attr({'stroke-opacity': 0.2});
                            node.textEl.attr({'opacity': 0.2});
                        }, function(x,y,evt){
                            this.dragX = this.dragY = undefined;
                            evt.stopPropagation();
                            evt.preventDefault();
                            isRightClick = evt.ctrlKey || (evt.which != null ? evt.which === 3 : evt.button === 2);
                        }, function(evt){
                            evt.stopPropagation();
                            evt.preventDefault();
                            if (isRightClick) {
                                if (node.depth === 0) {
                                    return;
                                }

                                if (node.mark) {
                                    unmarkNode(node, false);
                                }
                                else if (colorPicker) {
                                    var fillColor = colorPicker.attr('stroke');
                                    markNode(node, false, Raphael.color(fillColor).hex);
                                    colorPicker.remove();
                                    colorPicker = null;
                                }
                                else {
                                    // we've right-clicked. evt.which doesn't exist in IE, so use evt.button there
                                    // need setTimeout to avoid breaking Raphael's internal drag code in IE
                                    setTimeout(function(){
                                        if (node.depth !== 0) {
                                            removeNodes(node, true);
                                            renderData(json);
                                        }
                                    }, 1);
                                }
                                return;
                            }

                            if (dragEl) {
                                dragEl.remove();
                                dragTextEl.remove();
                                dragEl = dragTextEl = null;
                                // we're dragging around a node
                                // getElementsByPoint doesn't work with transformed nodes
                                // paper.getElementByPoint(evt.clientX, evt.clientY) works, but doesn't include
                                // the size of the dragged object
                                var target = findOverlap(81, this.dragX, this.dragY, json, node);
                                if (target) {
                                    var refs = target.refs, hasDupe = false;
                                    if (!refs) {
                                        refs = target.refs = [target];
                                    }

                                    for (var kk = refs.length - 1; kk >= 0; --kk) {
                                        if (refs[kk].ref === node.ref) {
                                            hasDupe = true;
                                            break;
                                        }
                                    }

                                    if (!hasDupe) {
                                        target.marker.attr({fill: suggestionMarkerFill});
                                        refs.push({ref: node.ref, title: node.title});
                                        target.name = target.name.replace(/^\u2022?/,'\u2022') + '\n\u2022' + node.title;

                                        if (target.children) {
                                            removeNodes(target, false);
                                        }

                                        updateNodeNavEl(target, true);
                                        suggestForNode(target);
                                    }
                                }
                                node.marker.attr({'stroke-opacity': 1});
                                node.textEl.attr({'opacity': 1});
                                return;
                            }

                            suggestForNode(node);
                        }).hover(function(evt){
                            if (Raphael.vml && colorPicker) {
                                return;
                            }

                            if (evt.pageX == null) {
                                evt.pageX = evt.clientX + document.body.scrollLeft + document.documentElement.scrollLeft;
                                evt.pageY = evt.clientY + document.body.scrollTop + document.documentElement.scrollTop;
                            }
                            tooltipEl.offset({
                                left: evt.pageX + 15,
                                top: evt.pageY + 5
                            }).html(_.escape(node.name).replace(/\n/g, '<br>')).css('visibility', 'visible');

                            highlightPathToNode(node, true);
                        }, function(){
                            tooltipEl.css('visibility', 'hidden');
                        });

                        animateOpacity.push(node.marker);

                        if (firstMarker) {
                            node.marker.insertAfter(firstMarker);
                        } else {
                            firstMarker = node.marker;
                        }
                    }

                    var rootNode = node.depth === 0;
                    var less180 = rootNode || node.x < 180;
                    var hideLabel = less180 !== node.less180;
                    node.less180 = less180;
                    var desiredX = less180 ? 8 :-8;
                    var textAnchor = less180 ? 'start' : 'end';
    //                    var textTransform = rootNode ? '' : transform + (less180 ? 'r90,0,0' : 'r270,0,0') + baseTransform;
                    var textTransform = rootNode ? 't0,0' : 't' + node.abspos[0] + ',' + node.abspos[1];
                    var textHeight = 60;
                    var wrapped;
                    var fontFamily = 'sans-serif';

                    var textWidth;
                    if (node.depth === maxDepth) {
                        textWidth = treeDepth + edgePad - node.y - 12;
                    }
                    else {
                        var halfHeight = 0.5 * textHeight;
                        var lIdx = binarySearchX(sortedNodes, 0, sortedNodes.length, node.x - halfHeight);
                        if (lIdx < 0) {
                            lIdx = -lIdx - 1;
                        }
                        var rIdx = binarySearchX(sortedNodes, lIdx, sortedNodes.length, node.x + halfHeight);
                        if (rIdx < 0) {
                            rIdx = -rIdx - 1;
                        }

                        var nearestLowerDepth = maxDepth + 1, nearestY;
                        for (var idx = lIdx; idx <= rIdx && idx < sortedNodes.length; ++idx) {
                            var otherNode = sortedNodes[idx];
                            if (otherNode !== node && otherNode.depth > node.depth && otherNode.depth < nearestLowerDepth) {
                                nearestLowerDepth = otherNode.depth;
                                nearestY = otherNode.y;
                            }
                        }

                        if (nearestLowerDepth === maxDepth + 1) {
                            // no nodes below this, extend all the way
                            textWidth = Math.max(10, treeDepth + edgePad - node.y - 20);
                        }
                        else {
                            textWidth = Math.max(10, nearestY - node.y - 13);
                        }
                    }

    //                    // 0' is the vertical
    //                    if (node.x < 45|| node.x > 135 && node.x < 225 || node.x > 315) {
    //                        var tmpSize = textWidth;
    //                        textWidth = textHeight;
    //                        textHeight = tmpSize;
    //                    }

                    if (node.textEl) {
                        var existingText = node.textEl.attr('text');
                        wrapped = Autn.wordWrap(paper, fontFamily, textWidth, node.name, 0, 12, 6, textHeight);
                        if (existingText !== wrapped.text) {
                            hideLabel = true;
                        }
                        if (hideLabel) {
                            node.textEl.attr({
                                text: wrapped.text,
                               'font-size': wrapped.fontSize,
                                x: desiredX,
                                'text-anchor': textAnchor,
                                transform: textTransform,
                                opacity: 0
                            });
                            colorTextNode(node);
                            animateOpacity.push(node.textEl);
                        }
                        else {
                            animateTogether(node.textEl, {
                                'font-size': wrapped.fontSize,
                                x: desiredX,
                                'text-anchor': textAnchor,
                                transform: textTransform
                            }, animTime);
                        }
                    }
                    else {
                        wrapped = Autn.wordWrap(paper, fontFamily, textWidth, node.name, 0, 12, 6, textHeight);
                        node.textEl = paper.text(desiredX, 0, wrapped.text).attr({
                            cursor: 'pointer',
                            dy: '.31em',
                            'font-family': fontFamily,
                            'font-size': wrapped.fontSize,
                            'text-anchor': textAnchor,
                            transform: textTransform,
                            opacity: 0
                        }).click(function(evt){
                            if (node.depth !== 0) {
                                var refs = node.refs;
                                if (Raphael.svg && refs && refs.length > 1) {
                                    if (evt.pageY == null) {
                                        evt.pageY = evt.clientY + document.body.scrollTop + document.documentElement.scrollTop;
                                    }
                                    var refIdx = refs.length - 1;
                                    var tspans = $('tspan', node.textEl.node);
                                    for (var jj = tspans.length - 1; jj >= 0; --jj) {
                                        var tspan = tspans[jj];
                                        if (tspan === evt.target && refIdx >= 0) {
                                            var selNode = refs[refIdx];
                                            onRefClick && onRefClick(selNode);
                                            return;
                                        }
                                        if ($(tspan).text().indexOf('\u2022') !== -1) {
                                            refIdx--;
                                        }
                                    }
                                }

                                onRefClick && onRefClick(node);
                            }
                        });

                        colorTextNode(node);

                        animateOpacity.push(node.textEl);
                    }
                });

                if (animateOpacity.length) {
                    setTimeout(function() {
                        paper.set(animateOpacity).animate({opacity: 1}, animTime);
                    }, animTime);
                }

                isRendering = false;

                function colorTextNode(node) {
                    if (Raphael.svg && node.refs && node.refs.length) {
                        var colors = ['black', '#404040'];
                        var colorIdx = 0;
                        var curColor;
                        $('tspan', node.textEl.node).each(function(idx, tspan){
                            var tSpanEl = $(tspan);
                            if (tSpanEl.text().indexOf('\u2022') !== -1) {
                                curColor = colors[colorIdx];
                                colorIdx = (colorIdx + 1) % colors.length;
                            }
                            tSpanEl.css('fill', curColor);
                        });
                    }
                }

                function suggestForNode(node) {
                    if (node.loading || isRendering) {
                        return;
                    }

                    node.loading = doAjaxFetch(node, lastNumResults, excludeRefs,
                        function(childJson){
                            docsToTree(childJson, node);
                            renderData(json);
                            node.loading = false;
                        },
                        function() {
                            node.loading = false;
                        });
                }

                function animateTogether(el, opts, time) {
                    if (animObj) {
                        el.animateWith(animEl, animObj, opts, time);
                    }
                    else {
                        animEl = el;
                        animObj = el.animate(opts, time);
                    }
                }

                function removeNodes(node, removeSelf) {
                    if (isRendering) {
                        return;
                    }

                    var toRemove = [];

                    if (removeSelf) {
                        removeInternal(node);
                        var parentChildren = node.parent.children;
                        parentChildren.splice(parentChildren.indexOf(node), 1);
                    }
                    else {
                        node.children.forEach(removeInternal);
                        delete node.children;
                    }

                    if (toRemove.length) {
                        paper.set(toRemove).animate({opacity: 0}, animTime, function(){
                            this.remove();
                        });
                    }

                    function removeInternal(child) {
                        if (child.loading) {
                            child.loading.abort();
                            child.loading = null;
                        }

                        if (child.mark) {
                            unmarkNode(child, true);
                        }

                        if (child.marker) {
                            toRemove.push(child.marker);
                        }
                        if (child.textEl) {
                            toRemove.push(child.textEl);
                        }

                        var idx = excludeRefs.indexOf(child.ref);
                        if (idx >= 0) {
                            excludeRefs.splice(idx, 1);
                        }

                        var pairKey = child.ref;
                        var pair = linkMap[pairKey];
                        if (pair) {
                            toRemove.push(pair);
                            delete linkMap[pairKey];
                        }

                        if (child.children) {
                            child.children.forEach(removeInternal);
                        }
                    }
                }
            }
        }
})(jQuery);
