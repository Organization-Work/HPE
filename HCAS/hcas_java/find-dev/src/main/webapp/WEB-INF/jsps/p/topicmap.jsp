<spring:eval expression="@'idol.properties'['discover.version']" var="version"/>
<html>
<head>
    <title>Topic Map</title>
    <link rel="stylesheet" href="<%=request.getContextPath()%>/resources/css/cupertino/jquery-ui-1.8.23.custom.css" type="text/css">
    <link rel="stylesheet" href="<%=request.getContextPath()%>/resources/css/msgreader.css" type="text/css">
    <link rel="stylesheet" href="<%=request.getContextPath()%>/resources/css/reader.css" type="text/css">
    <style type="text/css">
        #paper {
            width: 960px;
            height: 500px;
        }

        body .container {
          font: 10px sans-serif;
        }

        .content {
          overflow: scroll;
        }
        
        .debugonly {
            visibility: hidden;
        }
    </style>
</head>
<body>
    <div class="container topicmap">
        <header>
            <h1>Topic Map</h1>
        </header>
        <section>
            <form id="search" class="input-prepend input-append simpleHash">
                <input type="text" name="query" value="*" style="float: left; width:304px; margin-right:5px;" class="simpleHashText">
                <input type="submit" value="Search" class="btn autoClick">
                <div style="clear:both;"></div>
                <label id="clusterCount" style="float: left; margin-top:4px;">Subclusters: </label>
                <div id="slider" style="width:300px; margin:5px 10px;float: left;"></div>
                <div style="clear:both;"></div>
                <input class="debugonly" type="button" name="animate" value="Animate">
                <input class="debugonly" type="checkbox" id="continuousAnimate" name="continuousAnimate" checked="checked">
                <label class="debugonly" for="continuousAnimate">Continuous</label>
            </form>
            <div id="paper"></div>
            <textarea id="debugfield" rows="5" cols="80" class="debugonly"></textarea>
        </section>
    </div>

    <script type="text/javascript" src="<%=request.getContextPath()%>/resources/js/polyfill.js"></script>
    <script type="text/javascript" src="<%=request.getContextPath()%>/resources/js/underscore-min.js"></script>
    <script type="text/javascript" src="<%=request.getContextPath()%>/resources/js/jquery-ui-1.8.23.custom.min.js"></script>
    <script type="text/javascript" src="<%=request.getContextPath()%>/resources/js/jquery.mousewheel.min.js"></script>
    <script type="text/javascript" src="<%=request.getContextPath()%>/resources/js/raphael-min.js"></script>
    <script type="text/javascript" src="<%=request.getContextPath()%>/resources/js/d3.patch.js"></script>
    <script type="text/javascript" src="<%=request.getContextPath()%>/resources/js/d3.v2.min.js"></script>
    <script type="text/javascript" src="<%=request.getContextPath()%>/resources/js/Autn/wordwrap.js"></script>
    <script type="text/javascript" src="<%=request.getContextPath()%>/resources/js/Autn/reader.js"></script>
    <script type="text/javascript" src="<%=request.getContextPath()%>/resources/js/Autn/topicmap.js"></script>
    <script type="text/javascript" src="<%=request.getContextPath()%>/scripts/simplehash.js"></script>
    <script type="text/javascript" >


    
        $(function(){
            var r = 960 / 2;

            function processTerms(terms) {
                // have to remove the 2nd term if we're using the quick query terms
                if (terms.length === 3) {
                    terms.splice(1,1);
                }
                return terms;
            }

            var queryParam = /[?&]query=([^&]+)(&|$)/.exec(window.location.search);
            queryParam && $('#search input[name=query]').val(decodeURIComponent(queryParam[1]));

            //$('input[type=submit],input[type=button]').button();

            var childNodeLimit = /[?&]childLimit=(-?\d+)(&|$)/.exec(window.location.search);
            childNodeLimit = childNodeLimit ? Number(childNodeLimit[1]) : 6;

            $('#slider').slider({
                value: childNodeLimit,
                min: 3,
                max: 20,
                step: 1,
                slide: function(evt, ui) {
                    $('#clusterCount').html('Subclusters: ' + Number(ui.value));
                },
                stop: function(evt, ui) {
                    var newValue = Number(ui.value);
                    if (childNodeLimit !== newValue) {
                        childNodeLimit = Number(ui.value);
                        doSearch();
                        $('#clusterCount').html('Subclusters: ' + newValue);
                    }
                }
            });

            var clusterSentiment = /[?&]clusterSentiment=true(&|$)/.test(window.location.search);

            var debug = window.location.search.match(/[?&]debug=true(&|$)/i);

            if (debug) {
                $('.debugonly').css('visibility', 'visible');
            }

            var skipAnimation = /[?&]skipAnimation=true(&|$)/.test(window.location.search) || /iPad|iPhone|iPod/i.test(navigator.userAgent);

            var reqW = /[?&]w=(\d+)(&|$)/.exec(window.location.search);
            var reqH = /[?&]h=(\d+)(&|$)/.exec(window.location.search);
            var dom = $('#paper');
            reqW && dom.width(reqW[1]);
            reqH && dom.height(reqH[1]);

            var options = {
                debug: debug,
                hideLegend: /[?&]hideLegend=true(&|$)/.test(window.location.search),
                skipAnimation: skipAnimation,
                singleStep: !$('input[name=continuousAnimate]').bind('click', function(){
                    dom.topicmap('animate', this.checked, !this.checked);
                }).prop('checked'),
                onLeafClick: function(node, names, clusterSentiment) {
                    var contentEl = $('.content');
                    Autn.Reader.query(processTerms(names.reverse()), {
                        matchAllTerms: false,
                        highlightColor: node.color,
                        sentiment: clusterSentiment,
                        url: 'query.json',
                        dialogOpts: {
                            height: Math.min(contentEl.height() - 25, 700),
                            position: {
                                my: 'center center',
                                at: 'center center',
                                of: contentEl
                            }
                        }
                    });
                }
            };

            if (debug) {
                options.debug = true;
                options.onMarkerClick = function(vtx) {
                    $('#debugfield').val('Id:' + vtx.vertexId + '\nDepth:' + vtx.depth + '\nPos' + vtx.x + ':' + vtx.y);
                };
            }

            dom.topicmap(options);

            $('#search').on('submit', doSearch);
            doSearch();

            var lastAjax;

            $('input[name=animate]').bind('click.polymesh', function() {
                dom.topicmap('animate', true, !$('input[name=continuousAnimate]').prop('checked'));
            });

            function doSearch() {
                lastAjax && lastAjax.abort();
                dom.topicmap('showLoader', '../resources/images/ajax-loader.gif', 18, 15);
                lastAjax = $.ajax('clusters.json', {
                    contentType: 'application/json',
                    dataType: 'json',
                    data: {
                        childLimit: childNodeLimit,
                        clusterSentiment: clusterSentiment,
                        query: $('#search input[name=query]').val()
                    },
                    success: function(json){
                        dom.topicmap('renderData', json, clusterSentiment);
                    },
                    error: function() {
                        dom.topicmap('clear');
                    }
                });

                return false;
            }

        });
    </script>
	
	<script src="<%=request.getContextPath()%>/scripts/infonomy/topicmap-${config.version}.js"></script>
</body>
</html>
