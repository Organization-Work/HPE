<spring:eval expression="@'idol.properties'['discover.version']" var="version"/>
<!DOCTYPE html>
<html>
<head>
    <title>Visualiser</title>
    <link rel="stylesheet" href="../resources/css/cupertino/jquery-ui-1.8.23.custom.css" type="text/css">
    <link rel="stylesheet" href="../resources/css/reader.css" type="text/css">
    <style type="text/css">
        #paper {
            width: 960px;
            height: 960px;
        }

        body .container {
          font: 10px sans-serif;
        }
        
        .content {
          overflow: scroll;
        }

        .node-nav {
            color: white;
            padding: 2px 6px;
            background-color: steelblue;
            border-radius: 5px;
            cursor: pointer;
            white-space: nowrap;
            margin: 1px;
            display: inline-block;
        }

        .node-nav-query {
            background-color: #07ccd7;
            cursor: default;
        }

        .node-nav-multiple {
            background-color: #462887;
        }
    </style>

</head>
<body>
    <div class="container visualiser">
        <header>
            <h1>Visualiser</h1>
        </header>
        <section>
            <form id="search" class="input-prepend input-append simpleHash">
                <input type="text" name="query" value="*" class="simpleHashText">
                <input type="submit" value="Search" class="btn autoClick">
                <div style="margin: 5px 2px; font-size: 14px;min-height:23px;">
                    <div id="rootpathlabel">&nbsp;</div>
                </div>
            </form>
            <div id="paper"></div>
            <div id="label" style="visibility: hidden; position: absolute; font-size: 14px; padding: 1px 2px; background: lightgoldenrodyellow; border: 1px solid darkgoldenrod;"></div>
        </section>
    </div>

    <script type="text/javascript" src="../resources/js/underscore-min.js"></script>
    <script type="text/javascript" src="../resources/js/polyfill.js"></script>
    <script type="text/javascript" src="../resources/js/underscore-min.js"></script>
    <script type="text/javascript" src="../resources/js/jquery-ui-1.8.23.custom.min.js"></script>
    <script type="text/javascript" src="../resources/js/jquery.mousewheel.min.js"></script>
    <script type="text/javascript" src="../resources/js/raphael.js"></script>
    <script type="text/javascript" src="../resources/js/Autn/wordwrap.js"></script>
    <script type="text/javascript" src="../resources/js/Autn/viewdoc.js"></script>
    <script type="text/javascript" src="../resources/js/d3.patch.js"></script>
    <script type="text/javascript" src="../resources/js/d3.v2.js"></script>
    <script type="text/javascript" src="../resources/js/Autn/visualizer.js"></script>
    <script type="text/javascript" src="<%=request.getContextPath()%>/scripts/simplehash.js"></script>
    <script type="text/javascript" >
         $(function(){
            var queryParam = /[?&]query=([^&]+)(&|$)/.exec(window.location.search);
            queryParam && $('#search input[name=query]').val(decodeURIComponent(queryParam[1]));

            var dom = $('#paper');

            dom.visualizer({
                tooltip: $('#label'),
                navTree: $('#rootpathlabel'),
                handleResize: handleResize,
                onRefClick: function(node) {
                    Autn.viewDoc(node.ref, node.title, {url: 'content.json'});
                }
            });

            function handleResize() {}

            $(window).on('resize', undefined, handleResize);

            $('#search').on('submit', doSearch);
            if (queryParam) {
                doSearch();
            }

            function doSearch() {
                dom.visualizer('search', $('#search input[name=query]').val(), $('#numresults').val() || 6, 'resources/images/ajax-loader.gif', 18, 15);
                return false;
            }
        });
    </script>

	<script src="<%=request.getContextPath()%>/scripts/infonomy/visualiser-${config.version}.js"></script>
</body>
</html>
