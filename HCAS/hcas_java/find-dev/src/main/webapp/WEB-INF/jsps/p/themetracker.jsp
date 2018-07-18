<!DOCTYPE html>
<%@ taglib prefix="json" uri="/WEB-INF/tld/json.tld" %>
<spring:eval expression="@'idol.properties'['discover.version']" var="version"/>
<html>
<head>
    <title>Theme Tracker</title>
    <link rel="stylesheet" href="<%=request.getContextPath()%>/resources/css/cupertino/jquery-ui-1.8.23.custom.css" type="text/css">
    <link rel="stylesheet" href="<%=request.getContextPath()%>/resources/css/reader.css" type="text/css">
    <link rel="stylesheet" href="<%=request.getContextPath()%>/resources/css/themetracker.css" type="text/css">
    <style type="text/css">
        .content {
            background: black;
            overflow: scroll;
        }

        .themetracker {
          margin: 0;
        }
    </style>

</head>
<body>

    <div class="container themetracker">
        <div id="themetracker"><div id="paper"></div>
            <ul id="jobname-picker" class="jobname-menu">
            </ul>
        </div>
    </div>

    <script type="text/javascript" src="<%=request.getContextPath()%>/resources/js/polyfill.js"></script>
    <script type="text/javascript" src="<%=request.getContextPath()%>/resources/js/underscore-min.js"></script>
    <script type="text/javascript" src="<%=request.getContextPath()%>/resources/js/jquery-ui-1.8.23.custom.min.js"></script>
    <script type="text/javascript" src="<%=request.getContextPath()%>/resources/js/jquery.ui.datepicker-en-GB.js"></script>
    <script type="text/javascript" src="<%=request.getContextPath()%>/resources/js/jquery.mousewheel.min.js"></script>
    <script type="text/javascript" src="<%=request.getContextPath()%>/resources/js/raphael-min.js"></script>
    <script type="text/javascript" src="<%=request.getContextPath()%>/resources/js/json2-min.js"></script>
    <script type="text/javascript" src="<%=request.getContextPath()%>/resources/js/Autn/datechart.js"></script>
    <script type="text/javascript" >
        $(function(){
            var jobsConf = ${json:toJSON( themeTracker.jobs )};
            var interval = ${themeTracker.defaultTimeSpan};
            var showDatepickers = ${themeTracker.timeSpanUI};

            var jobs = _.map(jobsConf, function(label, jobName){
                return {
                    label: label,
                    jobName: jobName.toUpperCase()
                };
            });

            var imagePath = '../resources/images';
            var jobRegex = /[?&]jobName=([^&]+)(&|$)/i;
            var startDateParam = /[?&]fromdate=(-?\d+)(?:&|$)/i.exec(window.location.search);
            var endDateParam = /[?&]todate=(-?\d+)(?:&|$)/i.exec(window.location.search);
            var jobNameParam = jobRegex.exec(window.location.search);
            var jobName = jobNameParam ? decodeURIComponent(jobNameParam[1]) : jobs[0].jobName;

            $('#jobname-picker').html(_.map(jobs, function(job){
                var css = '', params = { jobname: job.jobName };
                if (jobName.toUpperCase() === job.jobName.toUpperCase()) {
                    css = 'selected';
                }
                if (startDateParam) {
                    params.fromdate = startDateParam[1];
                }
                if (endDateParam) {
                    params.todate = endDateParam[1];
                }

                return '<li class="jobname-menu-item '+css+'"><a href="?'+ $.param(params)+'">'+ _.escape(job.label)+'</a></li>';
            }));

            var dom = $('#paper');
            var containerDom = $('#themetracker');
            var width = dom.width(), height = dom.height();
            var paper = Raphael(dom[0], width, height);
            var popup;

            var sgWidth = 512, sgHeight = 512;
            var datePadding = 40;
            var imageHeight = height - datePadding;
            var clusterDateFormat = 'd/m/y';
            var startDate = startDateParam ? Math.round(startDateParam[1] / 1000)
                                           : Math.round(new Date() / 1000) - interval;

            if (endDateParam) {
                var endDate = Math.round(endDateParam[1] / 1000);
                if (!startDateParam) {
                    startDate = endDate - interval;
                }
                else {
                    interval = endDate - startDate;
                }
            }

            paper.image('themeImage.json?'+ $.param({startDate: startDate, interval: interval, jobName: jobName}), 0, datePadding, width, imageHeight).click(dismissPopup);
            var xScale = width / sgWidth, yScale = imageHeight / sgHeight;

            var debug = window.location.search.match(/[?&]debug=true(&|$)/i);

            var dateEls = {};

            function warn(args) {
                debug && alert(args);
            }

            var datesEl;

            showDatepickers && containerDom.on('click', '.showdates', function(evt){
                var pos = {my: 'top', at: $(this).is('.dateheader') ? 'bottom' : 'top', of: this, collision: 'fit', offset: '0 4'};

                if (datesEl) {
                    datesEl.show().position(pos);
                    return;
                }

                datesEl = $('<form action="" id="datesPicker">From<br><input type="hidden" name="jobname" value="'+ _.escape(jobName)
                            + '"><input type="hidden" name="fromdate"><input type="hidden" name="todate"><input class="from"><br>to<br><input class="to"><br><button type="submit">Search</button></form>')
                        .appendTo(containerDom).position(pos).submit(function(){
                            // Blanking a datepicker() field doesn't always update the altField, so we have to explicitly do a check
                            if (!toDate.val()) {
                                this.todate.value = '';
                            }
                            if (!fromDate.val()) {
                                this.fromdate.value = '';
                            }
                        });

                var minDate = new Date(startDate * 1000);
                var maxDate = new Date((startDate + interval) * 1000);

                var fromDate = datesEl.find('.from').datepicker({
                    altField: datesEl.find('input[name=fromdate]'),
                    altFormat: '@',
                    changeMonth: true,
                    changeYear: true,
                    maxDate: maxDate,
                    onClose: function(date){
                        toDate.datepicker('option', 'minDate', date);
                    }
                }).datepicker('setDate', minDate);

                var toDate = datesEl.find('.to').datepicker({
                    altField: datesEl.find('input[name=todate]'),
                    altFormat: '@',
                    changeMonth: true,
                    changeYear: true,
                    minDate: minDate,
                    onClose: function(date){
                        fromDate.datepicker('option', 'maxDate', date);
                    }
                }).datepicker('setDate', maxDate);

                $(document.body).on('mousedown', bodyListener);

                function bodyListener(evt){
                    if (!$(evt.target).closest('#datesPicker,.ui-datepicker,.showdates').length) {
                        hideDateSelector();
                    }
                }

                function hideDateSelector() {
                    datesEl.hide();
                }

                return false;
            });

            $.ajax('themeClusters.json', {
                data: {startDate: startDate, jobName: jobName, interval: interval},
                success: function(json) {
                    var dedupe = {};
                    var groups = {};

                    json.clusters.forEach(function(cluster){
                        var fromdate = cluster.fromDate;
                        var todate = cluster.toDate;
                        var x1 = cluster.x1;
                        var x2 = cluster.x2;
                        var y1 = cluster.y1;
                        var y2 = cluster.y2;
                        var title = cluster.title;

                        var groupKey = fromdate + ':' + y1;

                        var key = [fromdate,y1].join('_');
                        // based on sgd data, the actual node comes first, continuation links to other nodes come later
                        if (!dedupe[key]) {
                            var group = groups[groupKey];
                            if (!group) {
                                cluster.isNew = true;
                                group = groups[groupKey] = paper.set();
                            }

                            cluster.baseColor = cluster.isNew ? 'orange' : 'teal';

                            // is an actual node, e.g. from the .sgd
                            // 1339828800	CAT_EVERYTHING	19	812	1024	812	1024	violence derailing Observers mission, Moscow, Russia, Syria	26	104	9	20	0	20	3	3	0	1	1
                            var textL = cluster.textL = xScale * (x1);
                            var textW = cluster.textW = xScale * (x2-x1);
                            var textDom = $('<div class="textwrap">'+_.escape(title)+'</div>').css({
//                                'background-color': 'rgba(255,0,0,0.25)' doesn't work in ie, so we use raphael semitransparent rounded rectangles instead
                                left: textL,
                                width: textW
                            }).appendTo(containerDom);

                            if (cluster.isNew) {
                                textDom.css('font-weight', 'bold');
                            }

                            var textH = cluster.textH = textDom.height();
                            var textT = cluster.textT = datePadding + yScale * y1 - textH * 0.5;
                            textDom.css('top', textT);

                            var bgRect = paper.rect(textL, textT, textW, textH, 5).attr({fill: cluster.baseColor, 'fill-opacity': 0.5, stroke: 'black'}).data('cluster', cluster);

                            dedupe[key] = cluster;

                            group.push(bgRect);

                            textDom.hover(function() {
                                group.attr({fill: 'orange', 'fill-opacity': 0.5, stroke: 'orange'});
                            }, function() {
                                group.forEach(function(bgRect){
                                    bgRect.attr({fill: bgRect.data('cluster').baseColor, 'fill-opacity': 0.5, stroke: 'black'});
                                });
                            }).click(function(evt){                                                                                                           
                                evt.preventDefault();
                                evt.stopPropagation();
                                onClusterClick(evt, cluster, group);
                            });

                            cluster.textEl = textDom;
                            cluster.bgRect = bgRect;

                            if (!dateEls[fromdate]) {
                                var day = $.datepicker.formatDate(clusterDateFormat, new Date(fromdate * 1000));
                                dateEls[fromdate] = $('<div class="dateheader showdates">'+day+'</div>').css({
                                    left: textL,
                                    width: textW
                                }).appendTo(containerDom);
                            }
                        }
                        else {
                            // is a continuation node, e.g.
                            // 1339828800	CAT_EVERYTHING	19	812	1024	812	1024	violence derailing Observers mission, Moscow, Russia, Syria	26	104	9	20	12	20	3	14	1	1	1
                            var sourceGroup = groups[groupKey];
                            if (sourceGroup) {
                                var destKey = todate + ':' + y2;
                                var destGroup = groups[destKey];
                                if (destGroup && destGroup !== sourceGroup) {
                                    Array.prototype.push.apply(sourceGroup, destGroup);
                                }

                                groups[destKey] = groups[groupKey];
                            }
                            else {
                                warn('group for y1 should exist, was null');
                            }
                        }
                    });

                    if (!json.clusters.length) {
                        dom.addClass('showdates');
                    }

                    var showSubchildren = true;
                    var detailsAnimating = false;

                    var detailsBtnSize = 16;
                    var btnOpacity = 0.5;
                    var btnX = width - detailsBtnSize - 2;
                    var sourcesBtn = paper.image(imagePath + '/cluster-sources.png', btnX , 2, detailsBtnSize, detailsBtnSize)
                        .attr({opacity: btnOpacity});
                    var hideTimeout;
                    sourcesBtn.hover(function() {
                        var menu = $('#jobname-picker');

                        if (!menu.is(':visible')) {
                            menu.show();
                            menu.css({top: detailsBtnSize + 4, left: width - menu.outerWidth() - 2});
                        }

                        sourcesBtn.attr({opacity: 1});
                        hideTimeout && clearTimeout(hideTimeout);
                    }, function() {
                        queueMenuHide();
                    });

                    $('#jobname-picker').hover(function() {
                        hideTimeout && clearTimeout(hideTimeout);
                    }, queueMenuHide);

                    function queueMenuHide() {
                        hideTimeout && clearTimeout(hideTimeout);
                        hideTimeout = setTimeout(hideMenu, 500);
                    }

                    function hideMenu() {
                        $('#jobname-picker').hide();
                        sourcesBtn.attr({opacity: btnOpacity});
                        hideTimeout && clearTimeout(hideTimeout);
                        hideTimeout = null;
                    }

                    btnX -= detailsBtnSize + 2;

                    var detailsBtn = paper.image(imagePath + '/cluster-detail.png', btnX , 2, detailsBtnSize, detailsBtnSize)
                            .attr({opacity: btnOpacity})
                            .click(function() {
                        var animTime = 200;
                        if (!detailsAnimating) {
                            detailsAnimating = true;
                            showSubchildren = !showSubchildren;

                            for (var key in dedupe) {
                                var cluster = dedupe[key];
                                if (!cluster.isNew) {
                                    if (showSubchildren) {
                                        cluster.bgRect.animate({opacity: 1}, animTime, undefined, $.proxy(cluster.textEl.show, cluster.textEl));
                                    }
                                    else {
                                        cluster.textEl.hide();
                                        cluster.bgRect.animate({opacity: 0}, animTime, undefined);
                                    }
                                }
                            }

                            detailsBtn.animate({opacity: showSubchildren ? btnOpacity : btnOpacity * 0.9}, animTime, undefined, function() {
                                detailsAnimating = false;
                            });
                        }
                    });
                },
                error: function() {
                    dom.addClass('showdates');
                }
            });

            function onClusterClick(evt, cluster, group) {
                showClusterDetails(evt, cluster);
            }

            function dismissPopup() {
                Autn.DateChart.dismissPopup();
                
                if (popup) {
                    var paper = popup.data('paper');
                    paper && paper.remove();
                    popup.remove();
                }
                popup = null;
            }

            function showClusterDetails(evt, cluster) {
                if (popup) {
                    if (popup.data('cluster') === cluster) {
                        dismissPopup();
                        return;
                    }
                }

                dismissPopup();

                var html = '<div class="cluster-popup"><img class="cluster-viewdocs" src="'
                        + imagePath + '/magnifier.png"><img class="cluster-email" src="'
                        + imagePath +'/cluster-email.png"><img class="cluster-chart" src="'
                        + imagePath +'/cluster-chart.png"><div class="cluster-popup-numresults">'
                        + _.escape(plural(cluster.numDocs, 'document')) + '</div></div>';

                var popupTop = cluster.textT + cluster.textH + 1;

                popup = $(html).css({
                    left: cluster.textL,
                    width: cluster.textW,
                    top: popupTop
                }).data('cluster', cluster).appendTo(containerDom).on('click', 'img.cluster-email', cluster, function(evt) {
                    var cluster = evt.data;

                    $.ajax({
                        type: 'POST',
                        contentType: 'application/json',
                        url: 'themeDocuments.json',
                        data: JSON.stringify({id: cluster.id, fromDate: cluster.fromDate, toDate: cluster.toDate, jobName: jobName}),
                        success: function(json) {
                            // IE has a URL limit of 2083 characters, and will unhelpfully give a 'the data area passed to a system is too small' error if exceeded
                            // http://support.microsoft.com/kb/208427
                            var docs = json.map(function(a){return '- ' + a.title + '\n  ' + a.ref;});
                            var removed = 0;

                            while (docs.length > 1) {
                                var body = 'Cluster \''+cluster.title+'\' dated '+ formatClusterDate(cluster.fromDate)
                                   +'\n\nDocuments:\n' + docs.join('\n\n');

                                if (removed > 0) {
                                    body += '\n' + '... and ' + removed + ' more';
                                }

                                var mailTo = 'mailto:?subject=' + encodeURIComponent('Spectrograph cluster: ' + cluster.title) + '&body=' + encodeURIComponent(body);

                                if (!$.browser.msie || mailTo.length <= 2083) {
                                    window.location = mailTo;
                                    break;
                                }

                                docs.pop();
                                removed++;
                            }

                        }
                    });
                }).on('click', 'img.cluster-viewdocs', cluster, function(evt) {
                    var cluster = evt.data;

                    $.ajax({
                        type: 'POST',
                        contentType: 'application/json',
                        url: 'themeDocuments.json',
                        data: JSON.stringify({id: cluster.id, fromDate: cluster.fromDate, toDate: cluster.toDate, jobName: jobName}),
                        success: function(json) {
                            showClusterDocuments(cluster, json);
                        }
                    });
                }).on('click', 'img.cluster-chart', cluster, function(evt) {
                    var cluster = evt.data;

                    var endDate = Math.floor(new Date().getTime() / 1000);
                    var days = Math.max(30, Math.ceil((cluster.toDate - cluster.fromDate) / 86400));
                    var period = 'day', timeStep = 86400;

                    if (days > 31) {
                        period = 'week';
                        timeStep = 86400 * 7;
                    }

                    if ((endDate - cluster.fromDate) / 86400 > 31) {
                        // if we're working on old data, we should focus just on the range spanned by the cluster
                        endDate = cluster.toDate;
                    }

                    $.ajax({
                        type: 'POST',
                        contentType: 'application/json',
                        url: 'themeTimeline.json?' + $.param({days: days, dateperiod: period, endDate: endDate}),
                        data: JSON.stringify({id: cluster.id, fromDate: cluster.fromDate, toDate: cluster.toDate, jobName: jobName}),
                        success: function(json) {
                            showClusterGraph(cluster, json, timeStep);
                        }
                    });
                });

                var popupH = popup.height();

                if (popupTop + popupH > height) {
                    popup.css('top', cluster.textT - popupH - 4);
                }
            }

            function formatClusterDate(fromDate) {
                return $.datepicker.formatDate('DD, d M yy', new Date(fromDate * 1000));
            }

            function showClusterDocuments(cluster, clusterDocs) {
                dismissPopup();

                var pPadding = 10;
                var pWidth = width - 2 * pPadding;

                clusterDocs.sort(function(a, b) {
                    return b.score - a.score;
                });

                var minScore = Infinity, maxScore = 0;

                clusterDocs.forEach(function(doc){
                    var score = doc.score;
                    if (score < minScore) {
                        minScore = score;
                    }
                    if (score > maxScore) {
                        maxScore = score;
                    }
                });

                var scoreDiff = maxScore - minScore;
                var minFont = 10;
                var maxFont = 20;
                var scoreScale = 100 / scoreDiff;
                var fontScale = (maxFont - minFont) / scoreDiff;
                if (scoreDiff == 0) {
                    fontScale = scoreScale = 0;
                }

                var html = '<div class="cluster-documents"><div class="cluster-documents-title" style="color:'
                        + cluster.baseColor+';"><span class="cluster-documents-date">' + formatClusterDate(cluster.fromDate) + '</span>: ' + _.escape(cluster.title)
                    + '</div><div class="cluster-documents-body">'
                    + clusterDocs.map(function(doc){
                        var frac = scoreDiff === 0 ? 255 : 255 * (0.2 + 0.8 * (doc.score - minScore) / scoreDiff);
                        var color = Raphael.rgb(frac, frac, frac);
                        return '<div class="cluster-documents-doc" style="padding-bottom:4px;margin-left:'
                                + (maxFont + Math.round((maxScore - doc.score)*scoreScale))
                                + 'px;"><a style="font-size:'
                                + Math.round(maxFont - (maxScore - doc.score)*fontScale)
                                + 'px; color:'+color+';" target="_blank" href="'+_.escape(doc.ref)+'">'+_.escape(doc.title)+'</a><br>'+_.escape(doc.summary)+'</div>';
                    }).join('')
                    + '</div></div>';

                popup = $(html).css({
                    left: pPadding,
                    width: pWidth,
                    'border-color': cluster.baseColor
                }).data('cluster', cluster).appendTo(containerDom);

                popup.css('top', Math.min(cluster.textT + cluster.textH, height - popup.height() - pPadding));

                popup.on('click', 'div.cluster-documents-title', undefined, dismissPopup);
            }

            function plural(count, singular, plural) {
                return count + ' ' + (count === 1 ? singular : (arguments[2] || (singular + 's')));
            }

            function showClusterGraph(cluster, json, timeStep) {
                dismissPopup();

                var pPadding = 10;
                var pWidth = width - 2 * pPadding;

                var terms = json.terms;

                var popup = Autn.DateChart.showPopup({
                    baseColor: cluster.baseColor,
                    dates: json.dates,
                    fromDate: cluster.fromDate,
                    title: cluster.title,
                    timeStep: timeStep,
                    containerDom: containerDom,
                    width: pWidth,
                    docsFetchCallback: function (fetchMeta, successCallback) {
                        var params = {
                            endEpoch: fetchMeta.endEpoch,
                            startEpoch: fetchMeta.startEpoch,
                            pageNum: fetchMeta.pageNum,
                            pageSize: 6,
                            terms: terms
                        };

                        return $.ajax({
                            contentType: 'application/json',
                            data: JSON.stringify(params),
                            type: 'POST',
                            success: successCallback,
                            url: 'themeTimelineDocs.json'
                        });
                    }
                });

                popup.css({
                    left: pPadding,
                    top: Math.min(cluster.textT + cluster.textH, height - popup.height() - pPadding)
                }).data('cluster', cluster);
            }
        });
    </script>
	
	<script src="<%=request.getContextPath()%>/scripts/infonomy/themetracker-${config.version}.js"></script>
</body>
</html>
