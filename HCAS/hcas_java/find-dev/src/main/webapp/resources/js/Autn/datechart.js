(function($) {
    if (typeof Autn === 'undefined') {
        Autn = {};
    }

    Autn.DateChart = {
        showPopup: showClusterGraph,
        dismissPopup: dismissPopup
    };

    var popup, mask;

    function plural(count, singular, plural) {
        return count + ' ' + (count === 1 ? singular : (arguments[2] || (singular + 's')));
    }

    function dismissPopup() {
        if (popup) {
            var paper = popup.data('paper');
            paper && paper.remove();
            popup.remove();
        }
        if (mask) {
            mask.remove();
        }
        popup = mask = null;
    }

    function showClusterGraph(opts) {
        dismissPopup();

        var applyMask = opts.mask;
        var baseColor = opts.baseColor;
        var fromDate = opts.fromDate;
        var width = opts.width;
        var containerDom = opts.containerDom;
        var dates = opts.dates;
        var title = opts.title;
        var docsFetchCallback = opts.docsFetchCallback;
        var timeStep = opts.timeStep || 86400;
        var timeStepFormat = opts.timeStepFormat || function(epoch) {
            return $.datepicker.formatDate('dd', new Date(epoch * 1000));
        };
        var fullTimeFormat = opts.fullTimeFormat  || function(epoch) {
            return $.datepicker.formatDate('DD, d M yy', new Date(epoch * 1000));
        };
        var docRenderer = opts.docRenderer || function(doc){
            return '<div class="cluster-documents-doc" style="padding-bottom:4px;margin-right:4px;"><a style="font-size:14px; color:white;" target="_blank" href="'
                    + _.escape(doc.ref)
                    + '">' +_.escape(doc.title) +'</a><br>'
                    + _.escape(doc.summary)+'</div>';
        };

        var html = '<div class="cluster-documents"><div class="cluster-documents-title" style="color:'+baseColor+';"><span class="cluster-documents-date">'+fullTimeFormat(fromDate) + '</span>: ' + _.escape(title) + '</div><div class="cluster-documents-body">'
                + '</div><div class="cluster-timeline-docs"></div><div class="cluster-timeline-paper"></div></div>';

        if (applyMask) {
            var bodyEl = $(document.body);
            mask = $('<div></div>').appendTo(bodyEl).css({position:'fixed', left: 0, top: 0, opacity: 0, width: bodyEl.width(), height: bodyEl.height()})
                    .click(function(){
                dismissPopup();
                return false;
            });
        }

        popup = $(html).css({
            width: width,
            'border-color': baseColor
        }).appendTo(containerDom);

        var chartEl = popup.find('div.cluster-timeline-paper');
        var cPaperW = chartEl.width();
        var cPaperH = chartEl.height();
        var cPaper = Raphael(chartEl[0], cPaperW, cPaperH);

        var max = 0;

        if (dates.length) {
            var lPad = isFinite(opts.lPad) ? opts.lPad : 20;
            var vPad = isFinite(opts.vPad) ? opts.vPad : 20;
            var initialResultsIdx = -1;

            for (var ii = 0; ii < dates.length; ++ii) {
                var date = dates[ii];
                if (date.count > max) {
                    max = date.count;
                }

                var dayEnd = date.epoch + timeStep;
                // the clusters are created at 7am or so, so the 'start of day' might not be included in the
                // time spanned by them
                if (date.count && fromDate >= date.epoch && fromDate <= dayEnd) {
                    initialResultsIdx = ii;
                }

                if (ii !== dates.length - 1) {
                    var next = dates[ii + 1];
                    if (next.epoch > dayEnd) {
                        dates.splice(ii + 1, 0, {epoch: dayEnd, count: 0});
                    }
                }
            }

            var chartRight = cPaperW - lPad;
            var chartW = chartRight - lPad;
            var horzStep = chartW / dates.length;
            var halfHorz = horzStep * 0.5;
            var chartBottom = cPaperH - vPad;
            var chartHeight = chartBottom - vPad;
            var vertScale = chartHeight / max;
            var horzX = lPad;

            var tLabel;
            var origFillColor = opts.baseFillColor || 'rgb(39,120,165)',
                origFillOpacity = opts.baseFillOpacity || 0.5,
                origStroke = opts.baseStrokeColor || 'teal';
            var lineColor = opts.highlightStrokeColor || 'orange',
                highlightRectColor = opts.highlightFillColor || '#ffdc72',
                highlightFillOpacity = opts.highlightFillOpacity || origFillOpacity;
            var currentSelected;

            var pts = dates.map(function(a) {
                var blockH = a.count * vertScale;
                var ptY = chartBottom - blockH;
                a.rectColor = origFillColor;
                a.rectEl = cPaper.rect(horzX, ptY, horzStep, blockH, 3).attr({fill: a.rectColor, stroke: origStroke, 'fill-opacity': origFillOpacity})
                        .hover(function(){
                    tLabel && tLabel.remove();
                    tLabel = cPaper.set();
                    tLabel.push(
                            cPaper.circle(horzMidPt, ptY, 5).attr({fill: lineColor, stroke: 'none'}),
                            cPaper.text(horzMidPt, ptY - 12, plural(a.count, 'doc')).attr({'text-anchor': 'middle', 'fill': 'white'})
                            );
                    a.hovered = true;
                    this.attr({fill: highlightRectColor, 'fill-opacity': highlightFillOpacity, stroke: highlightRectColor});

                    currentSelected && (currentSelected !== a) && currentSelected.rectEl.insertBefore(chartLine);
                    $.browser.msie || this.insertBefore(chartLine);
                }, function() {
                    tLabel && tLabel.remove();
                    tLabel = null;
                    a.hovered = false;
                    this.attr({fill: a.rectColor, 'fill-opacity': origFillOpacity, stroke: a.rectColor});
                }).click(function() {
                    fetchDocs(a);
                });
                var horzMidPt = horzX + halfHorz;
                var dayStr = timeStepFormat(a.epoch);
                cPaper.text(horzMidPt, chartBottom + 10, dayStr).attr({'text-anchor': 'middle', 'fill': 'white'});
                var str = horzX + ',' + ptY + ',' + horzMidPt + ',' + ptY;
                horzX += horzStep;
                return str;
            });

            var pathStr = 'M'+pts[0]+ 'S' + pts.join(',') + 'H' + horzX;
            var chartLine = cPaper.path(pathStr).attr({stroke: lineColor, 'stroke-width': 2});
            var pathFill =
                    'M' + horzX + ',' + chartBottom
                            + pathStr
                            + 'L' + chartRight + ',' + chartBottom
                            + 'L' + lPad + ',' + chartBottom
                            + 'Z';
            cPaper.path(pathFill).attr({stroke: 'none', fill: lineColor, 'fill-opacity': 0.2}).toBack();
        }

        function fetchDocs(dateMeta) {
            if (currentSelected === dateMeta) {
                return;
            }

            if (currentSelected) {
                currentSelected.rectColor = origFillColor;
                currentSelected.rectEl.attr({fill: currentSelected.rectColor, 'fill-opacity': origFillOpacity, stroke: currentSelected.rectColor});
            }

            currentSelected = dateMeta;
            dateMeta.rectColor = 'lightblue';
            dateMeta.hovered ||  dateMeta.rectEl.attr({fill: dateMeta.rectColor, 'fill-opacity': 0.5, stroke: dateMeta.rectColor});
            dateMeta.rectEl.insertBefore(chartLine);

            var end = dateMeta.epoch + timeStep;

            var fetchMeta = resultsEl.data('fetchMeta');
            if (fetchMeta) {
                fetchMeta.req.abort();
            }

            fetchMeta = {startEpoch: dateMeta.epoch, endEpoch: end, pageNum: 0, pageSize: 6, fetching: true};
            resultsEl.data('fetchMeta', fetchMeta);

            fetchMeta.req = docsFetchCallback(fetchMeta, function(docsJSON) {
                showTimelineDocs(docsJSON, fetchMeta, false);
            });
        }

        function showTimelineDocs(docsJSON, fetchMeta, append) {
            if (!resultsEl.is(':visible')) {
                return;
            }

            var html = docsJSON.docs.map(docRenderer);

            if (!append) {
                resultsEl.empty();
                popup.find('span.cluster-documents-date').html(fullTimeFormat(fetchMeta.startEpoch));
            }

            resultsEl.append($(html.join('')).css({opacity: 0.1}).fadeTo(500, 1));
            fetchMeta.fetching = false;
            fetchMeta.canFetch = docsJSON.docs.length === fetchMeta.pageSize;

            if (!append) {
                if (resultsEl.prop('scrollHeight') <= resultsEl.prop('offsetHeight')) {
                    onScroll.call(resultsEl);
                }
            }
        }

        function onScroll() {
            var el = $(this);

            if (el.height() + el.scrollTop() > el.prop('scrollHeight') - 50) {
                fetchMore(el);
            }
        }

        function fetchMore(domEl) {
            var fetchMeta = domEl.data('fetchMeta');

            if (!fetchMeta || fetchMeta.fetching || !fetchMeta.canFetch) {
                return;
            }

            fetchMeta.fetching = true;
            fetchMeta.pageNum++;

            fetchMeta.req = docsFetchCallback(fetchMeta, function(docsJSON) {
                showTimelineDocs(docsJSON, fetchMeta, true);
            });
        }

        popup.on('click', 'div.cluster-documents-title', undefined, dismissPopup)
                .data('paper', cPaper);

        var resultsEl = popup.find('div.cluster-timeline-docs').on('scroll', onScroll);

        if (initialResultsIdx >= 0) {
            fetchDocs(dates[initialResultsIdx]);
        }

        return popup;
    }
})(jQuery);
