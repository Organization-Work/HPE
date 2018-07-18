<spring:eval expression="@'idol.properties'['discover.version']" var="version"/>
<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="X-UA-Compatible" content="IE=9">
    <title>Force-Directed Layout</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <link rel="stylesheet" href="<%=request.getContextPath()%>/resources/css/cupertino/jquery-ui-1.8.23.custom.css" type="text/css">
    <link rel="stylesheet" href="<%=request.getContextPath()%>/resources/css/reader.css" type="text/css">
    <link rel="stylesheet" href="<%=request.getContextPath()%>/resources/css/tweet.css" type="text/css">
    <link rel="stylesheet" href="<%=request.getContextPath()%>/resources/css/nodegraph.css" type="text/css">
</head>
<body>

<div id="paper"></div>
<form id="controls" class="simpleHash">
    <input name="query" size="10" class="simpleHashText">
    <select name="mindate">
        <option value="0">today</option>
        <option value="1">yesterday</option>
        <option value="7">in last week</option>
        <option value="31">in last month</option>
        <option value="365">in last year</option>
        <option selected value="">at any time</option>
    </select>
    <input id="searchbtn" type="submit" value="Search">
    <span id="load-indicator"></span>
</form>

<script type="text/javascript" src="<%=request.getContextPath()%>/scripts/simplehash-${config.version}.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/resources/js/polyfill.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/resources/js/jquery-ui-1.8.23.custom.min.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/resources/js/jquery.mousewheel.min.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/resources/js/raphael.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/resources/js/d3.patch.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/resources/js/d3.v2.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/resources/js/Autn/reader.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/resources/js/Autn/nodegraph.js"></script>
<script>
    $(function(){
        Autn.nodegraph();
    });
</script>
</body>
</html>

