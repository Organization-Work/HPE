<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<spring:eval expression="@'idol.properties'['discover.version']" var="version"/>

<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="X-UA-Compatible" content="IE=EmulateIE9" >
    <title>Sunburst</title>
    <link rel="stylesheet" type="text/css" href="<c:url value='/resources/css/idolview.css'/>">
    <link rel="stylesheet" type="text/css" href="<c:url value='/resources/css/cupertino/jquery-ui-1.8.23.custom.css'/>">
    <link rel="stylesheet" type="text/css" href="<c:url value='/resources/css/viewer.css'/>">
    <style>
        #spacer {
            height: 2%;
        }

        #chart {
            width: 95%;
            height: 95%;
        }

        .idolview {
            font: 15px sans-serif;
            color: black;
            width: 100%; height: 100%;
        }
    </style>
</head>
<body>
    <div class="container idolview">
        <div id="spacer"></div>
        <div id="chart"></div>
        <div id="load-indicator"></div>
        <div style="position:absolute;top: 0;right:8px;">
            <div>
                <ul class='draggable-filter draggable-filter-active'>
                    <li class="draggable-filter-label"><spring:message htmlEscape="true" code="idolview.filter.active"/></li>
                </ul>
                <ul class='draggable-filter draggable-filter-inactive'>
                    <li class="draggable-filter-label"><spring:message htmlEscape="true" code="idolview.filter.available"/></li>
                </ul>
            </div>
        </div>
        <div style="position:fixed;bottom: 0;right:0;">
            <div id="timegraph"></div>
        </div>
    </div>

    <script type="text/javascript">
        var locale = '${pageContext.response.locale.language}';
    </script>
   <%--  <script type="text/javascript" src="<c:url value='/resources/js/main.js'/>"></script>
    <script type="text/javascript" src="<c:url value='/resources/js/raphael.js'/>"></script>
    <script src="<%=request.getContextPath()%>/scripts/infonomy/idolview.js"></script>
    <script type="text/javascript" data-main="idolview.js" src="<c:url value='/resources/js/require.js'/>"></script> --%>
   
    <script type="text/javascript" src="<%=request.getContextPath()%>/resources/js/main.js"></script>
	<script type="text/javascript" src="<%=request.getContextPath()%>/resources/js/raphael.js"></script>
	<script type="text/javascript" src="<%=request.getContextPath()%>/scripts/infonomy/idolview.js"></script>
	<script type="text/javascript" data-main="idolview.js" src="<%=request.getContextPath()%>/resources/js/require.js"></script>
</body>
</html>
