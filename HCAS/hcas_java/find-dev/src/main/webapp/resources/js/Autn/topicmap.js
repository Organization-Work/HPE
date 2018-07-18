(function($){
    var methods = {
        init: function(options) {
            return $(this).each(function() {
                var dom = $(this), pluginMeta = dom.data('topicmap');

                if (pluginMeta) {
                    // plugin has been set before
                    return;
                }

                pluginMeta = {};
                dom.data('topicmap', pluginMeta);

                setupPlugin(dom, options || {}, pluginMeta);
            });
        },
        clear: function() {
            return $(this).each(function(){
                var dom = $(this), pluginMeta = dom.data('topicmap');
                if (pluginMeta) {
                    pluginMeta.clear();
                }
            });
        },
        showLoader: function(path, loaderW, loaderH) {
            return $(this).each(function(){
                var dom = $(this), pluginMeta = dom.data('topicmap');
                if (pluginMeta) {
                    pluginMeta.clear();
                    pluginMeta.showLoader(path, loaderW, loaderH);
                }
            });
        },
        renderData: function(json, clusterSentiment) {
            return $(this).each(function(){
                var dom = $(this), pluginMeta = dom.data('topicmap');
                if (pluginMeta) {
                    pluginMeta.renderData(json, clusterSentiment);
                }
            });
        },
        animate: function(animate, singleStep) {
            return $(this).each(function(){
                var dom = $(this), pluginMeta = dom.data('topicmap');
                if (pluginMeta) {
                    pluginMeta.animate(animate, singleStep);
                }
            });
        }
    };

    $.fn.topicmap = function(method) {
        if ( methods[method] ) {
            return methods[method].apply( this, Array.prototype.slice.call( arguments, 1 ));
        } else if ( typeof method === 'object' || ! method ) {
            return methods.init.apply( this, arguments );
        } else {
            $.error( 'Method ' +  method + ' does not exist on jQuery.topicmap' );
        }
    };

    function setupPlugin(dom, options, pluginMeta) {
        var hideLegend = options.hideLegend;

        var debug = options.debug;
        var skipAnimation = options.skipAnimation;
        var baseOpacity = skipAnimation ? 1 : 0.1;

        dom.bind('contextmenu.topicmap', function(){ return false; });
        var width = dom.width(),
                height = dom.height(), area = width * height;

        var paper = Raphael(dom[0], width, height), mesh;

        var onLeafClick = options.onLeafClick;
        var onMarkerClick = options.onMarkerClick;
        var onLayoutCreation = options.onLayoutCreation;

        var isDragging = false;

        var animateTimeout, continuousAnimate = !options.singleStep;

        var onNodeRender = options.onNodeRender;
        var threshold = options.threshold || 0.5;
        var minFontOpt = options.minFont || 6;
        var maxFontOpt = options.maxFont || 50;
        var maxLeafFont = options.maxLeafFont || 14;
        var minAreaSize = options.minAreaSize || 10;
        var enforceLabelBounds = options.enforceLabelBounds;

        pluginMeta.renderData = renderData;
        pluginMeta.animate = function(animate, singleStep){
            if (!mesh) {
                return;
            }
            if (!animate) {
                if (animateTimeout) {
                    animateTimeout = clearTimeout(animateTimeout);
                }
                return;
            }

            continuousAnimate = !singleStep;
            if (animateTimeout) {
                animateTimeout = clearTimeout(animateTimeout);
            }
            animateLoop();
        };
        pluginMeta.clear = function() {
            if (animateTimeout) {
                animateTimeout = clearTimeout(animateTimeout);
            }
            paper.clear();
            mesh = null;
        };
        pluginMeta.showLoader = function(path, loaderW, loaderH) {
            paper.image(path, (width - loaderW) * .5, (height - loaderH) * .5, loaderW, loaderH);
        };

        function PolyMesh(json, clusterSentiment, width, height) {
            var vertexMap = {};
            var vertexId = 0;
            var depthPolyMeta = [];
            var steps = 0;
            var stable = false;
            var renderedStable = false;
            var maxDepth = 0;

            var treemap = d3.layout.treemap()
                    .padding(0)
                    .size([width, height])
                    .value(function(d) { return d.size; });

            onLayoutCreation && onLayoutCreation(treemap);

            var treenodes = treemap(json);
            var root = treenodes[0].data || treenodes[0];
            treenodes.forEach(function(treenode){
                if (treenode.data) {
                    treenode.data.posData = treenode;
                    treenode.data.depth = treenode.depth;
                    if (treenode.parent) {
                        treenode.data.parent = treenode.parent.data;
                    }
                }
                else {
                    treenode.posData = treenode;
                }
                if (treenode.depth > maxDepth) {
                    maxDepth = treenode.depth;
                }
            });

            var shiftTL = [];
            var shiftBR = [];
            processTreeRoot(root, {toShiftTL:shiftTL, toShiftBR: shiftBR});

            var step = 5;
            shiftTL.forEach(function(vtx) {
                vtx.x -= step;
                vtx.y -= step;
            });
            shiftBR.forEach(function(vtx) {
                vtx.x += step;
                vtx.y += step;
            });

            for (var vtxId in vertexMap) {
                var vertex = vertexMap[vtxId];
                if (vertex.depth > 0) {
                    depthPolyMeta[vertex.depth].vertices.push(vertex);
                }
            }

            for (var depth = 0; depth <= maxDepth; ++depth) {
                depthPolyMeta[depth].polygons.forEach(function(node){
                    node.vertices.forEach(function(vert) {
                        var vertPolygons = vert.usage;
                        if (vertPolygons) {
                            vertPolygons.users.push(node);
                        }
                        else {
                            vertPolygons = vert.usage = {owner: node, users: [node]};
                        }
                    });
                });
            }

            function processTreeRoot(node, extra) {
                var posData = node.posData;
                var depth = node.depth;
                node.pressureInertia = 1;

                if (!posData.dx || !posData.dy) {
                    return;
                }

                var depthMeta = depthPolyMeta[depth];
                if (!depthMeta) {
                    depthMeta = depthPolyMeta[depth] = {polygons: [node], vertices: []};
                }
                else {
                    depthMeta.polygons.push(node);
                }

                if (!node.children) {
                    return;
                }

                // Essentially, given a treemap, want to reduce it to a hierarchical set of bubbles
                // where each vertex is shared by two bubbles if on a parent edge or three bubbles
                // otherwise
                // This means if you have 4 treemap squares meeting at a point, need to split that point
                // into 2 points, with different squares owning different points
                // the tree node indexes start from bottom right, moving left, then moving up
                var left = posData.x;
                var top = posData.y;
                var bot = top + posData.dy;
                var right = left + posData.dx;

                var vertices = node.vertices;
                var vertexUse = {};

                if (!vertices) {
                    vertices = node.vertices = [];
                    [[left, top], [left, bot], [right, bot], [right, top]].forEach(function(pt){
                        var vertex = new Vertex(pt[0], pt[1], depth);
                        vertexUse[vertexKey(pt[0], pt[1])] = vertex;
                        vertices.push(vertex);
                    });
                }
                else {
                    node.vertices.forEach(function(vtx){
                        vertexUse[vertexKey(vtx.x, vtx.y)] = vtx;
                    });
                }

                var childVertexReuse = {};
                var hueRange = node.hueRange || 1;
                var hue = node.hue || 0;
                node.children.forEach(function(child, childIdx){
                    var cPosData = child.posData;
                    if (!cPosData.dx || !cPosData.dy) {
                        // ignore this child, it has no width or height
                        return;
                    }

                    var sat = child.children ? 0.9 : 0.75;
                    var brightness = child.children ? 0.9 : 0.7 + 0.2 * childIdx/node.children.length;
                    var bright2factor = 0.6;
                    


                    if (clusterSentiment && child.sentiment != null) {
                        // [0, 0.3333]
                        child.hue = child.sentiment * 0.3333;
                    }
                    else {
                        child.hueRange = 0.8 * hueRange / node.children.length;
                        child.hue = hue + hueRange * ((childIdx/node.children.length + 0.5) % 1);
                    }

                    child.color = Raphael.hsb(child.hue, sat, brightness) ;
                    child.color2 = Raphael.hsb(child.hue, sat, brightness * bright2factor) ;

                    var cLeft = cPosData.x;
                    var cTop = cPosData.y;
                    var cBot = cTop + cPosData.dy;
                    var cRight = cLeft + cPosData.dx;
                    child.vertices = [];

                    var usedParentKeys = {};

                    [[cLeft, cTop], [cLeft, cBot], [cRight, cBot], [cRight, cTop]].forEach(function(pt){
                        var key = vertexKey(pt[0], pt[1]);
                        var parentVertex = vertexUse[key];
                        if (parentVertex) {
                            child.vertices.push(parentVertex);
                            usedParentKeys[key] = true;
                        }
                        else {
                            var tmpVertex = childVertexReuse[key];
                            if (tmpVertex) {
                                tmpVertex.users.push(child);
                            } else {
                                tmpVertex = childVertexReuse[key] = {users: [child], pt: pt};
                            }
                            child.vertices.push(tmpVertex);
                        }
                    });

                    // children must have the of node of any parent node
                    node.vertices.forEach(function(vtx){
                        if ((vtx.x === cLeft || vtx.x === cRight || vtx.y === cTop || vtx.y === cBot)
                            && vtx.x <= cRight && vtx.x >= cLeft && vtx.y <= cBot && vtx.y >= cTop
                            && !usedParentKeys[vertexKey(vtx.x, vtx.y)]) {
                            child.vertices.push(vtx);
                            child.requiresSort = true;
                        }
                    });
                });

                function findConstrainingEdge(vertex, matchProp, withinProp) {
                    for (var ii = vertices.length - 1; ii >= 0; --ii) {
                        var testVtx = vertices[ii];
                        var nextVtx = vertices[(ii + 1) % vertices.length];
                        var withinNum = vertex[withinProp];
                        var withinLimit1 = testVtx[withinProp];
                        var withinLimit2 = nextVtx[withinProp];
                        if (testVtx[matchProp] === vertex[matchProp]
                            && (withinNum <= withinLimit1 && withinNum >= withinLimit2
                                || withinNum >= withinLimit1 && withinNum <= withinLimit2)) {
                            vertex.constrainToEdge(testVtx, nextVtx);
                            return;
                        }
                    }
                    throwError('cannot find constraining edge');
                }

                for (var key in childVertexReuse) {
                    var vertexMeta = childVertexReuse[key], users = vertexMeta.users, pt = vertexMeta.pt, vertex;
                    switch(users.length) {
                        case 3:
                            // default case, an internal node, convert to a normal vertex
                            vertex = new Vertex(pt[0], pt[1], depth + 1);
                            replaceAll(users, vertexMeta, vertex);
                            break;
                        case 2:
                            // a point on the edge. mark as being limited to moving along parent edge
                            vertex = new Vertex(pt[0], pt[1], depth + 1);
                            if (pt[0] === left || pt[0] === right) {
                                findConstrainingEdge(vertex, 'x', 'y');
                            }
                            else if(pt[1] === bot || pt[1] === top) {
                                findConstrainingEdge(vertex, 'y', 'x');
                            }
                            else {
                                // This isn't a vertex on the edge of the parent, should be joined with a sibling node
                                for (var siblings = users[0].parent.children, ii = siblings.length - 1; ii >=0; --ii) {
                                    var sibling = siblings[ii];
                                    if (users.indexOf(sibling) === -1) {
                                        var sibPos = sibling.posData;
                                        var sibLeft = sibPos.x;
                                        var sibTop = sibPos.y;
                                        var sibRight = sibLeft + sibPos.dx;
                                        var sibBot = sibTop + sibPos.dy;
                                        if (pt[0] >= sibLeft && pt[0] <= sibRight && pt[1] >= sibTop && pt[1] <= sibBot) {
                                            sibling.requiresSort = true;
                                            sibling.vertices.push(vertex);
                                            break;
                                        }
                                    }
                                }
                            }
                            replaceAll(users, vertexMeta, vertex);
                            break;
                        case 4:
                            // shared point, needs to be split
                            vertex = new Vertex(pt[0], pt[1], depth + 1, -1);
                            var vertexBR = new Vertex(pt[0], pt[1], depth + 1, +1);
                            extra.toShiftTL.push(vertex);
                            extra.toShiftBR.push(vertexBR);

                            if (pt[0] === left) {
                                findConstrainingEdge(vertex, 'x', 'y');
                            }
                            else if(pt[0] === right) {
                                findConstrainingEdge(vertexBR, 'x', 'y');
                            }
                            if (pt[1] === top) {
                                findConstrainingEdge(vertex, 'y', 'x');
                            }
                            else if (pt[1] === bot) {
                                findConstrainingEdge(vertexBR, 'y', 'x');
                            }

                            users.forEach(function(node){
                                var idx = node.vertices.indexOf(vertexMeta);
                                if (idx < 0) {
                                    throwError();
                                }

                                var userPosData = node.posData;
                                var userCx = userPosData.x + userPosData.dx * 0.5;
                                var userCy = userPosData.y + userPosData.dy * 0.5;

                                var isLeft = userCx < pt[0];
                                var isAbove = userCy < pt[1];
                                if (isLeft) {
                                    if (isAbove) {
                                        node.vertices.splice(idx, 1, vertex);
                                    }
                                    else {
                                        node.vertices.splice(idx, 1, vertexBR, vertex);
                                    }
                                }
                                else if (isAbove) {
                                    node.vertices.splice(idx, 1, vertex, vertexBR);
                                }
                                else {
                                    node.vertices.splice(idx, 1, vertexBR);
                                }
                            });
                            break;
                        default:
                            throwError();
                    }
                }

                node.children.forEach(function(child) {
                    if (child.requiresSort) {
                        var midX = child.posData.x + 0.5 * child.posData.dx;
                        var midY = child.posData.y + 0.5 * child.posData.dy;
                        child.vertices.forEach(function(vertex){
                            vertex.tmpAngle = Math.atan2(vertex.y - midY, vertex.x - midX);
                        });
                        child.vertices.sort(function(a,b){
                            var diff = b.tmpAngle - a.tmpAngle;
                            return diff ||
                               (Math.atan2(b.y + b.shift - midY, b.x + b.shift - midX)
                                   - Math.atan2(a.y + a.shift - midY, a.x + a.shift - midX));
                        });
                        delete child.requiresSort;
                    }
                    processTreeRoot(child, extra);
                });

                function replaceAll(users, toReplace, replaceWith) {
                    users.forEach(function(node) {
                        var idx = node.vertices.indexOf(toReplace);
                        if (idx < 0) {
                            throwError();
                        }

                        node.vertices.splice(idx, 1, replaceWith);
                    });
                }
            }

            function throwError(err) {
               throw new Error(arguments[0] || 'should not happen');
            }

            this.step = function() {
                if (stable) {
                    return true;
                }

                var dampening = Math.pow(0.5, steps / 20);
//                    var dampening = 1;
                steps++;
                var largestStep = 0;

                // step at increasing depth to simulate positions
                for (var depth = 0; depth <= maxDepth; ++depth) {
                    var polyMeta = depthPolyMeta[depth];

                    polyMeta.polygons.forEach(function (polygon) {
                        // apply forces on vertices
                        // 1) pressure
                        // 2) surface tension
                        // 3) angle force

                        var polyInfo = d3.geom.polygon(polygon.vertices.map(function(vertex){
                            return [vertex.x, vertex.y];
                        }));

                        polygon.area = polyInfo.area();
                        polygon.centroid = polyInfo.centroid();
                        var pScale = 3;
                        var pressureRatio = (Math.abs(polygon.area) / area / polygon.size);
                        polygon.pressureInertia = Math.min(2, Math.max(polygon.pressureInertia * pressureRatio, 0.5));
                        var pressure = pScale - pScale * polygon.pressureInertia;

                        polygon.vertices.forEach(function(vertex, idx, vertices){
                            if (vertex.depth === depth) {
                                var next = vertices[(idx+1)%vertices.length];
                                if (next.depth === depth) {
                                    // apply surface tension force pulling nodes together
                                    var dx = next.x - vertex.x;
                                    var dy = next.y - vertex.y;
                                    var distSq = dx * dx + dy * dy;
                                    var dist = Math.sqrt(distSq);
                                    if (dist === 0) {
                                        return;
                                    }
                                    var dxFrac = dx / dist;
                                    var dyFrac = dy / dist;

                                    var pressureDx = pressure * dyFrac;
                                    var pressureDy = pressure * dxFrac;
                                    vertex.dx -= pressureDx;
                                    vertex.dy += pressureDy;
                                    next.dx -= pressureDx;
                                    next.dy += pressureDy;

                                    var tensionScale = .5;
                                    var tensionDx = tensionScale * dxFrac;
                                    vertex.dx += tensionDx;
                                    var tensionDy = tensionScale * dyFrac;
                                    vertex.dy += tensionDy;
                                    next.dx -= tensionDx;
                                    next.dy -= tensionDy;
                                }
                            }
                        });
                    });

                    polyMeta.vertices.forEach(function(vertex){
                        var forceX = dampening * vertex.dx;
                        vertex.x += forceX;
                        var forceY = dampening * vertex.dy;
                        vertex.y += forceY;
                        largestStep = Math.max(largestStep, Math.abs(forceX), Math.abs(forceY));
                        vertex.dx = vertex.dy = 0;
                        // compute new vertex position
                        // possibly reshape polygons if they've become convex? is this required?
                        if (vertex.vtxContraints) {
                            // todo: do we want to keep this?
                            var out = vertex.applyConstraints();
                            /*
                            var t = out[0];
                            if (t < 0 || t > 1) {
                                var currentEnd, oldConstraint;
                                if (t < 0) {
                                    currentEnd = vertex.vtxContraints[0];
                                    oldConstraint = vertex.vtxContraints[1];
                                }
                                else {
                                    currentEnd = vertex.vtxContraints[1];
                                    oldConstraint = vertex.vtxContraints[0];
                                }
                                // vertex wants to move to next polygon edge
                                // need to find next vertex, update constraint, then update
                                // winding order on all polygons using the vertex
                                var ownerVtxs = currentEnd.usage.owner.vertices;
                                var idxInOwner = ownerVtxs.indexOf(currentEnd);
                                if (idxInOwner < 0) {
                                    throwError('index should be in parent');
                                }

                                var previous = ownerVtxs[(idxInOwner + 1)%ownerVtxs.length];
                                var next = ownerVtxs[(idxInOwner + ownerVtxs.length - 1)%ownerVtxs.length];
                                if (previous !== oldConstraint) {
                                    if (next !== oldConstraint) {
                                        throwError('old constraint should be in parent');
                                    }
                                    var tmp = next;
                                    next = previous;
                                    previous = tmp;
                                }

                                if (t > 1) {
                                    vertex.vtxContraints[0] = currentEnd;
                                    vertex.vtxContraints[1] = next;
                                }
                                else {
                                    vertex.vtxContraints[0] = next;
                                    vertex.vtxContraints[1] = currentEnd;
                                }

                                vertex.usage.users.forEach(function(node, tmpIdx) {
                                    var vtxIdx = node.vertices.indexOf(vertex);
                                    var otherIdx = node.vertices.indexOf(currentEnd);
                                    if (vtxIdx < 0) {
                                        throwError('old constraint should be in user');
                                    }
                                    if (otherIdx < 0) {
                                        // wasn't using the constraint point, should add it
                                        // does this always cope with cw/ccw winding correctly?
                                        node.vertices.splice(t > 1 ? vtxIdx : vtxIdx + 1, 0, currentEnd);
                                        currentEnd.usage.users.push(node);
                                    }
                                    else {
                                        // was using the constraint point, should remove it
                                        node.vertices.splice(otherIdx, 1);
                                        currentEnd.usage.users.splice(currentEnd.usage.users.indexOf(node), 1);
                                    }
                                });
                            }
                            //*/
                        }
                    });

                }

                if (largestStep < threshold) {
                    stable = true;
                }
            };

            var me = this;
            var suppressClick, delayedReappear;
            this.redraw = function() {
                if (renderedStable) {
                    return;
                }

                for (var depth = maxDepth; depth >= 1; --depth) {
                    var polyMeta = depthPolyMeta[depth];
                    var polygons = polyMeta.polygons;

                    polygons.forEach(function (node) {
                        if (!node.vertices) {
                            typeof console !== 'undefined' && console.log('not implemented yet');
                            return;
                        }

                        var poly = node.poly = node.vertices.map(function(a){return [a.x, a.y];});
                        var newPath = 'M' + poly.join('L') + 'Z';
                        if (node.path) {
                            node.path.attr('path', newPath);
                        }
                        else {
                            node.path = paper.path(newPath)
                                    .attr({fill: '330-'+node.color+'-'+node.color2, 'fill-opacity': node.children ? 0.9 : 1, stroke: 'white', 'stroke-width': node.children ? 8/(node.depth + 1) : 4, 'stroke-opacity': node.children ? 0.2 : 0.7});
                        }
                    });

                    polyMeta.lastPath = polygons[polygons.length-1].path;
                }

                if (stable && !renderedStable) {
                    renderedStable = true;
                    for (depth = maxDepth; depth >= 1; --depth) {
                        polyMeta = depthPolyMeta[depth];
                        polyMeta.polygons.forEach(function (node) {
                            var minFont = minFontOpt;
                            var maxFont = node.children ? maxFontOpt : maxLeafFont;
                            var centroidX = node.centroid[0];
                            var centroidY = node.centroid[1];
                            var textEl = paper.text(centroidX, centroidY, node.name).attr({
                                dy: '.35em', 'text-anchor': 'middle',
                                fill: 'white',
                                'font-family': 'Verdana',
                                'font-weight': 'bold',
                                'font-size': maxFont,
                                opacity: depth <=2 ? baseOpacity : 1
                            });

                            $(textEl.node).css('pointer-events', 'none');

                            var poly = d3.geom.polygon(node.poly);
                            var horz = poly.clip([[0, centroidY], [width, centroidY]]);
                            if (!horz.length) {
                                // The polygon clip algorithm can fail when given duplicate points, e.g. when two different
                                // control points are pushed so they overlap together
                                // d3.geom.polygon([[382.1332976780137,249.8231858864144],[546.0929301802026,249.82635195736358],
                                // [552.0577121178245,178.83555722361845],[541.9655448961505,170.00360404938775],
                                // [348.6641270339407,162.08838050657752],[348.6641270339407,162.08838050657752]])
                                // .clip([[0, 196.58283779445776], [960, 206.58283779445776], [0, 306.58283779445776]]) gives []
                                var deduped = [];
                                var dedupedKeys = {};
                                node.poly.forEach(function(vtx) {
                                    var key = vtx[0] + ':' + vtx[1];
                                    if (!dedupedKeys[key]) {
                                        dedupedKeys[key] = true;
                                        deduped.push(vtx);
                                    }
                                });

                                if (deduped.length >= 3 && deduped.length < node.poly.length) {
                                    poly = d3.geom.polygon(deduped); 
                                    horz = poly.clip([[0, centroidY], [width, centroidY]]);
                                }
                            }

                            var sized = false;
                            if (horz.length) {
                                var vert = poly.clip([[centroidX, 0], [centroidX, height]]);
                                if (vert.length) {
                                    // todo: why is horz sometimes empty?
                                    Raphael.svg && textEl.attr('x', 0.5 * (horz[0][0] + horz[1][0]));
                                    var wrapAttrs = Autn.wordWrap(paper, 'Verdana', horz[0][0] - horz[1][0], node.name, 0.5, maxFont, minFont, vert[0][1] - vert[1][1], textEl);
                                    sized = wrapAttrs.fit;
                                }
                            }

                            if (sized || !enforceLabelBounds) {
                                textEl.insertAfter(polyMeta.lastPath);
                                node.textEl = textEl;
                            }
                            else {
                                textEl.remove();
                            }

                            // Need to handle right-clicks with mouseup since .click() doesn't catch right-click events
                            node.path.mouseup(function(evt){
                                // IE uses evt.button with different buttons, and doesn't support ctrlKey
                                if (evt.ctrlKey || (evt.which != null ? evt.which === 3 : evt.button === 2)) {
                                    evt.stopPropagation();
                                    onRightClick(node);
                                }

                                if (delayedReappear) {
                                    clearTimeout(delayedReappear);
                                    delayedReappear = null;
                                }
                            }).mousedown(function(evt){
                                suppressClick = false;

                                if (delayedReappear) {
                                    clearTimeout(delayedReappear);
                                    delayedReappear = null;
                                }

                                delayedReappear = node.parent && node.parent.path ? setTimeout(function(){
                                    onRightClick(node);
                                    suppressClick = true;
                                }, 500) : null;
                            }).click(function (evt, x, y){
                                if (node.animating || evt.ctrlKey || suppressClick) {
                                    return;
                                }

                                if (node.children) {
                                    var anim = Raphael.animation({opacity: 0}, 500, undefined, function(){
                                        node.path.hide();
                                        node.textEl && node.textEl.hide();
                                        node.animating = false;
                                    });
                                    node.path.animate(anim);
                                    node.textEl && node.textEl.animateWith(node.path, anim, anim);
                                    node.animating = true;
                                    return;
                                }
                                var names = [];
                                for (var current = node; current != null; current = current.parent) { names.push((current.data || current).name); }
                                onLeafClick && onLeafClick(node, names, clusterSentiment, evt);
                            });

                            onNodeRender && onNodeRender(node);

                            function onRightClick(node) {
                                if (node.animating) {return;}
                                if (node.parent && node.parent.path) {
                                    var anim = Raphael.animation({opacity: 0.9}, 500, undefined, function(){
                                        node.parent.animating = false;
                                    });
                                    node.parent.path.animate(anim);
                                    node.parent.textEl && node.parent.textEl.animateWith(node.path, anim, anim);
                                    node.parent.animating = true;
                                    node.parent.path.show();
                                    node.parent.textEl && node.parent.textEl.show();
                                }
                            }
                        });
                    }

                    if (!skipAnimation) {
                        // Animations have to be done as a separate step to make it smoother, since the CPU load from
                        // the text position computation means it takes a long time to finish.
                        setTimeout(function(){
                            for (depth = Math.min(2, maxDepth); depth >= 1; --depth) {
                                var polygons = depthPolyMeta[depth].polygons;
                                var baseDelay = Math.min(300, 3000 / polygons.length);
                                depthPolyMeta[depth].polygons.forEach(function (node, nodeIdx) {
                                    node.animating = true;

                                    if (node.textEl) {
                                        node.textEl.animate(Raphael.animation({opacity: 1}, 1000, undefined, function(){
                                            node.animating = false;
                                        }).delay(baseDelay * nodeIdx));
                                    }
                                    else {
                                        node.animating = false;
                                    }
                                });
                            }
                        }, 10);
                    }
                }

                if (debug) {
                    for (depth = maxDepth; depth >= 0; --depth) {
                        depthPolyMeta[depth].vertices.forEach(function(vtx){
                            if (vtx.marker) {
                                if (stable) {
                                    vtx.marker.remove();
                                    return;
                                }

                                vtx.marker.attr({cx: vtx.x, cy: vtx.y});
                                return;
                            }
                            vtx.marker = paper.circle(vtx.x, vtx.y, 3).attr({fill: vtx.vtxContraints ? 'blue' : 'red'});

                            vtx.marker.hover(function(){
                                vtx.vtxContraints && vtx.vtxContraints.forEach(function(a){a.marker.attr({fill: 'orange'});});
                                onMarkerClick && onMarkerClick(vtx); 
                            }, function(){
                                vtx.vtxContraints && vtx.vtxContraints.forEach(function(a){a.marker.attr({fill: a.vtxContraints ? 'blue' : 'red'});});
                            });

                            vtx.marker.drag(function(dx,dy, x,y,evt){
                                this.dragX = x; this.dragY = y;
                                this.attr({cx:x + this.offsetX, cy: y + this.offsetY});
                            }, function(x,y,evt){
                                isDragging = true;
                                this.dragX = this.dragY = undefined;
                                this.startX = x;
                                this.startY = y;
                                this.offsetX = this.attr('cx') - x;
                                this.offsetY = this.attr('cy') - y;
                            }, function(evt){
                                isDragging = false;
                                if (this.dragX === undefined) {
                                }
                                else {
                                    vtx.x = this.dragX + this.offsetX;
                                    vtx.y = this.dragY + this.offsetY;
                                }

                                if (vtx.vtxContraints) {
                                    vtx.applyConstraints();
                                }

                                applyConstraintsForVertex(vtx);

                                function applyConstraintsForVertex(vtx) {
                                    for (var depth = 0; depth <= maxDepth; ++depth) {
                                        depthPolyMeta[depth].vertices.forEach(function(otherVtx) {
                                            if (otherVtx.vtxContraints && otherVtx.vtxContraints.indexOf(vtx) !== -1) {
                                                otherVtx.applyConstraints();
                                                applyConstraintsForVertex(otherVtx);
                                            }
                                        });
                                    }
                                }

                                me.redraw();
                            });
                        });
                    }
                }
            };

            function vertexKey(x,y) {
                return x.toFixed(2) + ':' + y.toFixed(2);
            }

            function Vertex(x,y,depth,shift) {
                // x, y, depth
                this.x = x; this.y = y; this.depth = depth; this.dx = this.dy = 0; this.shift = arguments[3] || 0;
                vertexMap[this.vertexId = ++vertexId] = this;

                this.constrainToEdge = function(vtx1, vtx2) {
                    this.vtxContraints = [vtx1, vtx2];
                };

                // finds projection of this point along the two vertexes and clips to the polygon edges
                this.applyConstraints = function() {
                    var a = this.vtxContraints[0];
                    var b = this.vtxContraints[1];
                    var cx = this.x, cy = this.y;
                    var ABx = b.x - a.x, ABy = b.y - a.y,
                        modABSq = ABx * ABx + ABy * ABy;

                    if (modABSq === 0) {
                        this.x = a.x;
                        this.y = a.y;
                        return [t, cx, cy];
                    }

                    var ACx = cx - a.x, ACy = cy - a.y,
                        t = (ACx * ABx + ACy * ABy) / modABSq;

                    if (t < 0) {
                        this.x = a.x;
                        this.y = a.y;
                        return [t, cx, cy];
                    }
                    else if (t > 1) {
                        this.x = b.x;
                        this.y = b.y;
                        return [t, cx, cy];
                    }
                    else {
                        this.x = a.x + ABx * t;
                        this.y = a.y + ABy * t;
                        return [t, cx, cy];
                    }
                };
            }
        }

        // size normalization, specifically tied to the
        function normalizeSize(json) {
            var baseSize = json.size;
            if (json.children) {
                if (!json.children.length) {
                    delete json.children;
                    return;
                }

                var childCount = json.children.reduce(function(a,b){
                    return a + b.size;
                }, 0);

                json.children.forEach(function(child){
                    child.size *= baseSize / childCount;
                    normalizeSize(child);
                });
            }
        }

        function cullTinyPoints(json, area, minPixels) {
            // remove all points which would render at less than minPixels size, since they'd break the layout
            var scaleToRenderArea = area / json.size;

            clean(json);

            function clean(json) {
                if (json.children) {
                    json.children = _.filter(json.children, function(child){
                        if (child.size * scaleToRenderArea >= minPixels) {
                            clean(child);
                            return true;
                        }
                        return false;
                    });
                }
            }
        }

        function renderData(json, clusterSentiment) {
            if (animateTimeout) {
                animateTimeout = clearTimeout(animateTimeout);
            }

            if (json) {
                // defensive copy, since we'll be potentially removing small elements
                json = $.extend(true, {}, json);
                normalizeSize(json);
                cullTinyPoints(json, width * height, minAreaSize);
//                renormalize the sizes in case we've culled some nodes
                normalizeSize(json);
            }

            paper.clear();
            width = dom.width();
            height = dom.height();

            if (!json || !json.children || !json.children.length) {
                paper.text(width * 0.5, height * 0.5, 'No results available, please try a different query').attr({
                    dy: '.35em', 'text-anchor': 'middle',
                    fill: 'black',
                    'font-family': 'Verdana',
                    'font-weight': 'bold',
                    'font-size': 12,
                    opacity: 0.1
                }).animate({opacity: 1}, 500);
               return;
            }

            if (clusterSentiment && !hideLegend) {
                width -= 30;
                var pattern = [90];

                for (var ii = 0; ii < height; ii += 10) {
                    // [0, 0.3333]
                    var frac = ii / height,
                        hue = frac * 0.3333;
                    pattern.push('hsb('+hue+',1,1)');
                }

                paper.rect(width + 10, 0, 20, height).attr({fill: pattern.join('-'), stroke: 'lightgray'});
                paper.text(width + 20, 10, '+').attr({font: '13px Verdana', stroke: 'white', 'text-anchor': 'middle'});
                paper.text(width + 20, height - 10, '-').attr({font: '13px Verdana', stroke: 'white', 'text-anchor': 'middle'});
            }

            mesh = new PolyMesh(json, clusterSentiment, width, height);

            for (ii = 4; ii >= 0; --ii) {
                mesh.step();
            }
            mesh.redraw();

            if (skipAnimation) {
                while (!mesh.step()) {}
                mesh.redraw();
                return;
            }

            animateLoop();
        }
        
        function animateLoop() {
            var finished;

            if (!isDragging) {
                finished = mesh.step();
                mesh.redraw();
            }

            if (continuousAnimate && !finished) {
                animateTimeout = setTimeout(animateLoop, 25);
            }
        }
    }
})(jQuery);
