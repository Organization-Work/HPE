// actually also needs raphael, but raphael doesn't work with require.js, so just assume it's been imported
define(['jquery', 'Autn/i18n', 'jqueryui'], function($, i18n) {
        function Graph(el, data) {
            el = $(el);
            var dom = el[0];
            var width = el.width();
            var height = el.height();
            var paper = Raphael(dom, width, height);
            var plot;
            var fillColour = 'orange';
            var lPad = 4;
            var markerEl;
            var metaMarkerTimeout;
            var dateFilter;
            var period;
            var axisLines;

            var selectRect = paper.rect(0, 0, width, height).attr({fill: 'green', opacity: 0.2}).hide();
            var dragX, lastStartX, lastWidth, lastFilterStart, lastFilterEnd;
            var minDate = Infinity, maxDate = -Infinity, dateRangeScale;
            var glass = paper.rect(0, 0, width, height).attr({opacity: 0, fill: 'white'}).drag(function(dx, dy, x, y, evt) {
                if (dx > 0) {
                    lastStartX = dragX;
                    lastWidth = dx;
                    selectRect.attr({x: lastStartX, width: lastWidth});
                }
                else if (dx < 0) {
                    lastStartX = dragX + dx;
                    lastWidth = -dx;
                    selectRect.attr({x: lastStartX, width: lastWidth});
                }

                if (lastWidth > 5 && isFinite(dateRangeScale)) {
                    var xWithoutPad = lastStartX - lPad;
                    lastFilterStart = minDate + xWithoutPad * dateRangeScale;
                    lastFilterEnd = minDate + (xWithoutPad + lastWidth) * dateRangeScale;
                    showFilterTooltip(lastFilterStart, lastFilterEnd);
                }
                else {
                    lastFilterStart = lastFilterEnd = null;
                    showFilterTooltip();
                }
            }, function(x, y, evt) {
                dateRangeScale = (maxDate - minDate) / (width - lPad);
                lastWidth = lastFilterStart = lastFilterEnd = 0;
                dragX = evt.layerX || (evt.pageX - el.offset().left);
                selectRect.attr({opacity: 0.1, x: dragX, width: 1}).show();
                showFilterTooltip();
            }, function(evt) {
                if (lastFilterStart) {
                    // apply filter
                    selectRect.attr({opacity: 0.2});
                    dateFilter = [lastFilterStart, lastFilterEnd];
                    // firing jQuery events here, consider backbone ones instead?
                    el.trigger('autn.graph.datechange', dateFilter, true);
                }
                else {
                    // clear filter
                    selectRect.hide();

                    if (dateFilter) {
                        dateFilter = null;
                        el.trigger('autn.graph.datechange', dateFilter, true);
                    }
                    else {
                        hideMarker();
                    }
                }
            }).hide();

            this.getDateFilter = function() {
                return dateFilter;
            };

            this.setDateFilter = function(filter) {
                if (filter == null || (filter instanceof Array && filter.length === 2 && typeof(filter[0]) === 'number' && typeof(filter[1]) === 'number')) {
                    if (JSON.stringify(dateFilter) !== JSON.stringify(filter)) {
                        // do we have to update the ui? it'll happen on the next render step
                        dateFilter = filter;
                        el.trigger('autn.graph.datechange', dateFilter, false);
                    }
                }
            };

            // dates should be sorted in ascending order
            // container should have position:relative
            this.render = function(json) {
                if (axisLines) {
                    axisLines.remove();
                    axisLines = null;
                }
                
                if (axisLines) {
                    axisLines.remove();
                    axisLines = null;
                }

                minDate = Infinity,maxDate = -Infinity;
                var timeScale, valScale, numPoints = 0;

                if (json) {
                    var dates = json.dates;
                    period = json.period;
                    var maxVal = -Infinity;

                    for (var date in dates) {
                        var val = dates[date];
                        // note that date as a key in an object is stored as a string, not a number, and needs casting
                        date = Number(date);
                        ++numPoints;
                        if (val > maxVal) {
                            maxVal = val;
                        }
                        if (date > maxDate) {
                            maxDate = date;
                        }
                        if (date < minDate) {
                            minDate = date;
                        }
                    }

                    if (numPoints === 1) {
                        maxDate = minDate + 1;
                        minDate = minDate - 1;
                    }

                    timeScale = (width - lPad) / (maxDate - minDate);
                    valScale = height / maxVal;
                }

                if (!isFinite(timeScale) || !isFinite(minDate) || !isFinite(valScale)) {
                    hideMarker();
                    if (plot) {
                        plot.graphFill.remove();
                        plot.graphPath.remove();
                        plot.markers.remove();
                        plot = null;
                    }
                    if (dateFilter) {
                        glass.show();
                        selectRect.attr({x: 0, width: width}).show();
                    }
                    else {
                        glass.hide();
                        selectRect.hide();
                    }
                    return;
                }

                glass.show();
                showMetaMarker();

                axisLines = paper.set();
                var markers = paper.set();
                var coords = ['M', 0, height, 'R'], dateIdx = 0;

                for (var date in dates) {
                    var x = (date - minDate) * timeScale;
                    var y = dates[date] * valScale;
                    var px = lPad + x;
                    var py = height - y + 2;
                    coords.push(px, py);
                    markers.push(paper.circle(px, py, 2, 2).attr({fill: 'white', stroke: 'orange'}).data('date-key', date).hover(showMarker, scheduleShowMetaMarker).hide());
                    ++dateIdx;
                }

                if (numPoints === 1) {
                    // if there's exactly one point, render as a spike with lines, rather than the 'R' curves
                    coords.splice(3, 1, 'L', px - 2, height);
                    coords.push(px + 2, height);
                }

                var lastX = coords[coords.length - 2];
                var firstX = coords[1];
                var fillCoords = coords.concat('L', lastX, height, firstX, height);
                coords.push('M', lastX, height, firstX, height);

                if (!plot) {
                    plot = {
                        graphFill: paper.path(fillCoords).attr({fill: fillColour, 'fill-opacity': 0.4, stroke: 'none'}),
                        graphPath: paper.path(coords).attr({stroke: fillColour, 'stroke-width': 1.5, 'stroke-linecap': 'round', opacity: 0}),
                        dates: dates,
                        markers: markers
                    };
                    paper.set(plot.graphPath, plot.graphFill).insertBefore(selectRect).animate({opacity: 0.7}, 500);
                }
                else {
                    var anim = Raphael.animation({path: coords}, 500);
                    plot.graphPath.animate(anim);
                    plot.graphFill.animateWith(plot.graphPath, anim, {path: fillCoords}, 500);
                    plot.dates = dates;
                    plot.markers.remove();
                    plot.markers = markers;
                }

                var axisMarkers = calcAxisMarkers();

                axisMarkers.epochs.forEach(function(axisTime) {
                    var xTime = lPad + (axisTime - minDate) * timeScale;
                    axisLines.push(paper.path(['M', xTime, height, 'V', 0]).attr({stroke: 'lightgray', 'stroke-width': 0.5}));
                    axisLines.push(paper.text(xTime, height - 10, axisMarkers.formatter(axisTime)).attr({'text-anchor': 'middle'}));
                });

                axisLines.push(
                    paper.path(['M', lPad, height, 'V', 0]).attr({stroke: 'lightgray', 'stroke-width': 0.5, 'arrow-end': 'classic-wide-long'}),
                    paper.path(['M', lPad, height, 'H', width]).attr({stroke: 'lightgray', 'stroke-width': 0.5, 'arrow-end': 'classic-wide-long'})
                );

                axisLines.toBack();

                setTimeout(function() {
                    markers.show();
                }, 500);

                glass.show();
                if (dateFilter) {
                    // timescale
                    selectRect.attr({
                        x: lPad + (dateFilter[0] - minDate) * timeScale,
                        width: (dateFilter[1] - dateFilter[0]) * timeScale
                    }).show();

                } else {
                    selectRect.hide();
                }

                function showMetaMarker() {
                    clearMetaMarkerShowTimeout();
                    hideMarker();
                    var labelText = i18n('graph.daterange.label', {MINDATE: formatEpoch(minDate), MAXDATE: formatEpoch(maxDate), DOCS: maxVal});
                    markerEl = $('<div class="timegraph-label"></div>').text(labelText).appendTo(el)
                            .position({ my: 'bottom', at: 'center top', of: el, offset: '0 4', collision: 'fit'}).css('opacity', 0).animate({opacity: 1}, 500);
                }

                function scheduleShowMetaMarker() {
                    clearMetaMarkerShowTimeout();

                    metaMarkerTimeout = setTimeout(showMetaMarker, 2000);
                }

                function showMarker() {
                    clearMetaMarkerShowTimeout();
                    hideMarker();

                    var date = this.data('date-key');

                    if (date) {
                        var val = dates[date];
                        markerEl = $('<div class="timegraph-label"></div>').text(i18n('graph.point.label', {DATE: formatEpoch(date), DOCS: val})).appendTo(el)
                                .position({ my: 'bottom', at: 'left top', of: el, offset: this.attr('cx') + ' ' + this.attr('cy'), collision: 'fit'});
                    }
                }
            };

            function showFilterTooltip(minDate, maxDate) {
                hideMarker();
                var labelText = minDate != null ? i18n('graph.filter.set', {MINDATE: formatEpoch(minDate), MAXDATE: formatEpoch(maxDate)})
                                                : dateFilter ? i18n('graph.filter.clear') : i18n('graph.filter.none');
                markerEl = $('<div class="timegraph-label"></div>').text(labelText).appendTo(el)
                        .position({ my: 'bottom', at: 'center top', of: el, offset: '0 4', collision: 'fit'});
            }

            function clearMetaMarkerShowTimeout() {
                if (metaMarkerTimeout) {
                    clearTimeout(metaMarkerTimeout);
                    metaMarkerTimeout = null;
                }
            }

            function hideMarker() {
                if (markerEl) {
                    markerEl.remove();
                    markerEl = null;
                }
            }

            function twoDigitPad(d) {
                return d < 10 ? '0' + d : d;
            }

            function formatEpoch(epoch) {
                var date = new Date(epoch * 1000);
                switch(period) {
                    case 'hour':
                    case 'minute':
                        return $.datepicker.formatDate('d M', date) + ' ' + twoDigitPad(date.getHours()) + ':' + twoDigitPad(date.getMinutes());
                }
                return $.datepicker.formatDate('DD, d M yy', date);
            }

            function hourFormat(epoch) {
                var date = new Date(epoch * 1000);
                return twoDigitPad(date.getHours()) + ':' + twoDigitPad(date.getMinutes());
            }

            function dayMonthFormat(epoch) {
                var date = new Date(epoch * 1000);
                return $.datepicker.formatDate('d M', date);
            }

            function monthYearFormat(epoch) {
                var date = new Date(epoch * 1000);
                return $.datepicker.formatDate('M yy', date);
            }

            function yearFormat(epoch) {
                var date = new Date(epoch * 1000);
                return date.getFullYear();
            }
            
            function calcAxisMarkers(){
                var range = maxDate - minDate;
                
                var datePeriods = {
                    year: [365 * 24 * 3600, yearFormat],
                    month: [31 * 24 * 3600, monthYearFormat],
                    day: [24 * 3600, dayMonthFormat],
                    hour: [3600, hourFormat],
                    minute: [60, hourFormat]
                };

                var stepSizes = [
                    [1, 'minute'], [2, 'minute'], [5, 'minute'], [10, 'minute'], [30, 'minute'],
                    [1, 'hour'], [2, 'hour'], [4, 'hour'], [12, 'hour'],
                    [1, 'day'], [2, 'day'], [5, 'day'], [10, 'day'],
                    [1, 'month'], [3, 'month'], [6, 'month'],
                    [1, 'year'], [2, 'year'], [5, 'year'], [10, 'year']
                ];

                stepSizes.forEach(function(type){
                    type.secs = type[0] * datePeriods[type[1]][0];
                });

                // stepCount should probably be configurable
                var lastFit = stepSizes[0], stepCount = 5, rangeStep = range / stepCount;

                for (var ii = 0; ii < stepSizes.length; ++ii) {
                    var testSize = stepSizes[ii];
                    if (testSize.secs > rangeStep) {
                        break;
                    }
                    lastFit = testSize;
                }

                var multiplier = lastFit[0];
                var datePeriod = lastFit[1];
                var formatter = datePeriods[datePeriod][1];
                var stepSize = lastFit.secs;
                var axisStart = new Date(minDate * 1000);
                var monthCorrection;

                switch (datePeriod) {
                    case 'minute':
                        var rounded = Math.floor(axisStart.getMinutes() / multiplier) * multiplier;
                        axisStart.setMinutes(rounded, 0, 0); break;
                    case 'hour':
                        axisStart.setMinutes(0, 0, 0); break;
                    case 'day':
                        axisStart.setHours(0, 0, 0, 0); break;
                    case 'month':
                        monthCorrection = true;
                        axisStart.setDate(1);
                        axisStart.setHours(0, 0, 0, 0); break;
                    case 'year':
                        axisStart.setMonth(0, 1);
                        axisStart.setHours(0, 0, 0, 0); break;
                }

                var divisions = [];

                for (var currSecs = axisStart * 1e-3; currSecs < maxDate; currSecs += stepSize) {
                    if (monthCorrection) {
                        var tmpDate = new Date(currSecs * 1e3);
                        tmpDate.setDate(1);
                        tmpDate.setHours(0, 0, 0, 0);
                        currSecs = tmpDate.getTime();
                    }

                    if (currSecs > minDate) {
                        divisions.push(currSecs);
                    }
                }

                return {epochs: divisions, formatter: formatter};
            }
        }

        return Graph;
    }
);
